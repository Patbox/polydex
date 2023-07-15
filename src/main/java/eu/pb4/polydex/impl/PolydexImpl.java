package eu.pb4.polydex.impl;

import com.google.gson.JsonParser;
import com.mojang.brigadier.StringReader;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import eu.pb4.placeholders.api.TextParserUtils;
import eu.pb4.polydex.api.*;
import eu.pb4.polydex.api.hover.HoverDisplay;
import eu.pb4.polydex.api.hover.HoverDisplayBuilder;
import eu.pb4.polydex.api.hover.PolydexTarget;
import eu.pb4.polydex.api.recipe.PolydexEntry;
import eu.pb4.polydex.api.recipe.PolydexPage;
import eu.pb4.polydex.api.recipe.PolydexStack;
import eu.pb4.polydex.impl.book.view.CustomPage;
import eu.pb4.polydex.impl.book.view.PotionRecipePage;
import eu.pb4.polydex.mixin.BrewingRecipeAccessor;
import eu.pb4.polydex.mixin.BrewingRecipeRegistryAccessor;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.CustomValue;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.command.argument.ItemStringReader;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import net.minecraft.recipe.Recipe;
import net.minecraft.resource.ResourceManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Nameable;
import net.minecraft.util.Util;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.MathHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

public class PolydexImpl {
    public static Codec<ItemStack> ITEM_STACK_CODEC = Codec.either(Identifier.CODEC, ItemStack.CODEC).xmap(
            x -> x.left().isPresent() ? Registries.ITEM.get(x.left().get()).getDefaultStack() : x.right().get(), x -> x.hasNbt() || x.getCount() != 1 ? Either.right(x) : Either.left(Registries.ITEM.getId(x.getItem())));

    public static final Map<Identifier, Function<PolydexTarget, HoverDisplay>> DISPLAYS = new HashMap<>();
    public static final HashMap<Class<?>, Function<Recipe, PolydexPage>> RECIPE_VIEWS = new HashMap<>();
    public static final List<PolydexPage.Creator> VIEWS = new ArrayList<>();
    public static final String ID = "polydex";
    public static final Map<Item, Function<Item, @Nullable Collection<PolydexEntry>>> ITEM_ENTRY_BUILDERS = new HashMap<>();
    public static final PackedEntries ITEM_ENTRIES = PackedEntries.create();
    public static final Map<String, NamespacedEntry> BY_NAMESPACE = new HashMap<>();
    public static final Map<ItemGroup, NamespacedEntry> BY_ITEMGROUP = new HashMap<>();

    public static final List<NamespacedEntry> NAMESPACED_ENTRIES = new ArrayList<>();
    public static final List<NamespacedEntry> ITEM_GROUP_ENTRIES = new ArrayList<>();

    public static final Map<PolydexStack<?>, PolydexEntry> STACK_TO_ENTRY = new Object2ObjectOpenHashMap<>();
    public static final Map<Item, List<PolydexEntry>> ITEM_TO_ENTRIES = new Object2ObjectOpenCustomHashMap<>(Util.identityHashStrategy());

    public static final Logger LOGGER = LogManager.getLogger("Polydex");
    public static final List<Consumer<HoverDisplayBuilder>> DISPLAY_BUILDER_CONSUMERS = new ArrayList<>();
    public static final Map<Identifier, List<CustomPage.ViewData>> CUSTOM_PAGES = new HashMap<>();
    public static Codec<Text> TEXT = Codec.either(Codec.STRING, Codecs.TEXT)
            .xmap(either -> either.map(TextParserUtils::formatTextSafe, Function.identity()), Either::right);

    @Nullable
    private static CompletableFuture<Void> cacheBuilding = null;
    private static boolean isReady;
    public static PolydexConfig config = new PolydexConfig();

    public static boolean isReady() {
        return isReady;
    }

    static {
        PolydexInitializer.init();
    }

    public static Identifier id(String path) {
        return new Identifier(ID, path);
    }

    public static PolydexEntry getEntry(ItemStack stack) {
        if (!isReady) {
            return null;
        }
        var pStack = PolydexStack.of(stack);
        var x = STACK_TO_ENTRY.get(stack);
        if (x == null) {
            var list = ITEM_TO_ENTRIES.getOrDefault(stack.getItem(), Collections.emptyList());

            for (var entry : list) {
                if (entry.isPartOf(pStack)) {
                    return entry;
                }
            }
        }
        return x;
    }

    public static void rebuild(MinecraftServer server) {
        try {
            ItemGroups.updateDisplayContext(server.getOverworld().getEnabledFeatures(), false, server.getRegistryManager());
        } catch (Throwable e) {
            e.printStackTrace();
        }

        if (cacheBuilding != null) {
            LOGGER.warn("[Polydex] Cancelling unfinished matching");
            cacheBuilding.cancel(true);
        }
        isReady = false;
        LOGGER.info("[Polydex] Started matching recipes and items...");
        var time = System.currentTimeMillis();

        var recipes = server.getRecipeManager().values();
        cacheBuilding = CompletableFuture.runAsync(() -> {
            updateCaches(server, recipes);
        }).thenAcceptAsync((x) -> {
            LOGGER.info("[Polydex] Done! It took {} ms", (System.currentTimeMillis() - time));
            isReady = true;
        }, server);
    }

    public static void updateCaches(MinecraftServer server, Collection<Recipe<?>> recipes) {
        ITEM_ENTRIES.clear();
        BY_NAMESPACE.clear();
        BY_ITEMGROUP.clear();
        STACK_TO_ENTRY.clear();
        ITEM_TO_ENTRIES.clear();
        BY_NAMESPACE.put("minecraft", new NamespacedEntry("minecraft", Text.literal("Minecraft (Vanilla)"), (p) -> Items.GRASS_BLOCK.getDefaultStack(), PackedEntries.create()));
        NAMESPACED_ENTRIES.clear();
        ITEM_GROUP_ENTRIES.clear();

        var entries = new ObjectLinkedOpenHashSet<PolydexEntry>();

        var allGroups = new ArrayList<>(ItemGroups.getGroups());

        for (var group : allGroups) {
            if (group.getType() == ItemGroup.Type.CATEGORY) {
                var groupEntries = new ObjectLinkedOpenHashSet<PolydexEntry>();
                var namespacedEntry = NamespacedEntry.ofItemGroup(group);

                for (var item : group.getDisplayStacks()) {
                    groupEntries.add(PolydexEntry.of(item));
                }
                for (var item : group.getSearchTabStacks()) {
                    groupEntries.add(PolydexEntry.of(item));
                }

                entries.addAll(groupEntries);
                namespacedEntry.entries.addAll(groupEntries);
                BY_ITEMGROUP.put(group, namespacedEntry);
            }
        }

        for (var item : Registries.ITEM) {
            if (item == Items.AIR) {
                continue;
            }

            var func = ITEM_ENTRY_BUILDERS.get(item);
            if (func != null) {
                var custom = func.apply(item);
                if (custom == null) {
                    continue;
                } else {
                    entries.addAll(custom);
                }
            }
        }

        var list = new ArrayList<PolydexPage>(recipes.size());
        for (var recipe : recipes) {
            var baseClass = (Class<?>) recipe.getClass();

            while (baseClass != Object.class) {
                var view = PolydexImpl.RECIPE_VIEWS.get(baseClass);

                if (view != null) {
                    var page = view.apply(recipe);
                    if (page != null) {
                        list.add(page);
                    }
                    break;
                }
                baseClass = baseClass.getSuperclass();
            }
        }

        for (var recipe : list) {
            for (var entry : entries) {
                if (recipe.isOwner(server, entry)) {
                    entry.recipeOutput().add(recipe);
                }

                for (var ingredient : recipe.getIngredients()) {
                    if (ingredient.matches(entry.stack(), false)) {
                        entry.ingredients().add(recipe);
                        break;
                    }
                }
            }
        }

        var copy = new ArrayList<>(ITEM_ENTRIES.all);
        for (var entry : ITEM_ENTRIES.all) {
            for (var page : entry.recipeOutput()) {
                for (var entry2 : copy) {
                    for (var ingredient : page.getIngredients()) {
                        if (ingredient.matches(entry2.stack(), false)) {
                            entry2.ingredients().add(page);
                            break;
                        }
                    }
                }
            }
            entry.recipeOutput().sort(Comparator.comparingInt((x) -> -x.priority()));
        }

        var map = new HashMap<Item, Collection<Recipe<?>>>();

        for (var item : Registries.ITEM) {
            map.put(item, new ArrayList<>());
        }

        for (var entry : entries) {
            for (var viewBuilder : VIEWS) {
                var pageEntries = viewBuilder.createPages(server, entry);

                if (pageEntries != null) {
                    entry.recipeOutput().addAll(pageEntries);
                }
            }
        }

        for (var entry : entries) {
            ITEM_ENTRIES.add(entry);
            STACK_TO_ENTRY.put(entry.stack(), entry);
            if (entry.stack().getBackingClass() == ItemStack.class) {
                ITEM_TO_ENTRIES.computeIfAbsent(((ItemStack) entry.stack().getBacking()).getItem(), (a) -> new ArrayList<>()).add(entry);
            }
            BY_NAMESPACE.computeIfAbsent(entry.identifier().getNamespace(), (x) -> NamespacedEntry.ofMod(x, entry.stack()::toItemStack)).entries.add(entry);
        }


        for (var x : BY_NAMESPACE.values()) {
            x.entries.recalculateEmpty();
        }

        for (var x : BY_ITEMGROUP.values()) {
            x.entries.recalculateEmpty();
        }

        NAMESPACED_ENTRIES.addAll(BY_NAMESPACE.values());
        NAMESPACED_ENTRIES.sort(Comparator.comparing((s) -> s.namespace));
        ITEM_GROUP_ENTRIES.addAll(BY_ITEMGROUP.values());
        ITEM_GROUP_ENTRIES.sort(Comparator.comparing((s) -> s.namespace));
        config = PolydexConfig.loadOrCreateConfig();
    }

    public static Collection<PolydexPage> potionRecipe(MinecraftServer server, PolydexEntry entry) {
        if (entry.stack().getBackingClass() != ItemStack.class) {
            return List.of();
        }
        var stack = (ItemStack) entry.stack().getBacking();

        var list = new ArrayList<PolydexPage>();
        var itemRecipes = BrewingRecipeRegistryAccessor.getITEM_RECIPES();
        var potionRecipes = BrewingRecipeRegistryAccessor.getPOTION_RECIPES();

        for (var recipe : itemRecipes) {
            var access = (BrewingRecipeAccessor<Item>) recipe;

            if (access.getOutput() == stack.getItem()) {
                list.add(new PotionRecipePage.ItemBase(new Identifier("minecraft:brewing/item/"
                        + Registries.ITEM.getId(access.getInput()).toUnderscoreSeparatedString() + "/" + Registries.ITEM.getId(access.getOutput()).toUnderscoreSeparatedString()), recipe));
            }
        }

        var potion = PotionUtil.getPotion(stack);
        if (potion != Potions.EMPTY) {
            for (var recipe : potionRecipes) {
                var access = (BrewingRecipeAccessor<Potion>) recipe;

                if (access.getOutput() == potion) {
                    list.add(new PotionRecipePage.PotionBase(new Identifier("minecraft:brewing/potion/"
                            + Registries.POTION.getId(access.getInput()).toUnderscoreSeparatedString() + "/" + Registries.POTION.getId(access.getOutput()).toUnderscoreSeparatedString()),
                            recipe));
                }
            }
        }

        return list;
    }

    public static void defaultBuilder(HoverDisplayBuilder displayBuilder) {
        var target = displayBuilder.getTarget();
        var entity = target.getEntity();
        if (entity != null) {
            displayBuilder.setComponent(HoverDisplayBuilder.NAME, entity.getDisplayName());

            if (entity instanceof LivingEntity livingEntity) {
                if (PolydexImpl.config.displayEntityHealth) {
                    displayBuilder.setComponent(HoverDisplayBuilder.HEALTH, Text.literal("").append(Text.literal("♥ ").formatted(Formatting.RED))
                            .append("" + Math.min(MathHelper.ceil(livingEntity.getHealth()), MathHelper.ceil(livingEntity.getMaxHealth())))
                            .append(Text.literal("/").formatted(Formatting.GRAY))
                            .append("" + MathHelper.ceil(livingEntity.getMaxHealth())));
                }

                if (PolydexImpl.config.displayAdditional) {
                    var effects = new ArrayList<Text>();

                    if (livingEntity.isOnFire()) {
                        effects.add(Text.literal("\uD83D\uDD25" + (livingEntity.isFrozen() ? " " : "")).formatted(Formatting.GOLD));
                    }

                    if (livingEntity.isFrozen()) {
                        effects.add(Text.literal("❄").formatted(Formatting.AQUA));
                    }

                    for (var effect : livingEntity.getStatusEffects()) {
                        effects.add(Text.literal("⚗").setStyle(net.minecraft.text.Style.EMPTY.withColor(effect.getEffectType().getColor())));
                    }

                    if (!effects.isEmpty()) {
                        displayBuilder.setComponent(HoverDisplayBuilder.EFFECTS, PolydexUtils.mergeText(effects, PolydexUtils.SPACE_SEPARATOR));
                    }
                }

            }
        } else {
            if (target.getBlockEntity() instanceof Nameable nameable) {
                if (nameable.hasCustomName()) {
                    displayBuilder.setComponent(HoverDisplayBuilder.NAME, nameable.getCustomName());
                } else {
                    displayBuilder.setComponent(HoverDisplayBuilder.NAME, nameable.getDisplayName());
                }
            } else {
                displayBuilder.setComponent(HoverDisplayBuilder.NAME, target.getBlockState().getBlock().getName());
            }

            if (PolydexImpl.config.displayCantMine && (!target.getPlayer().canHarvest(target.getBlockState()) || target.getBlockState().calcBlockBreakingDelta(target.getPlayer(), target.getPlayer().getWorld(), target.getTargetPos()) <= 0)) {
                var text = Text.literal("⛏").formatted(Formatting.DARK_RED);
                if (!displayBuilder.isSmall()) {
                    text.append(" ").append(Text.translatable("text.polydex.cant_mine").formatted(Formatting.RED));
                }
                displayBuilder.setComponent(HoverDisplayBuilder.EFFECTS, text);
            }

            if (config.displayAdditional && target.getBlockEntity() instanceof AbstractFurnaceBlockEntity furnace) {
                displayBuilder.setComponent(HoverDisplayBuilder.INPUT, PolydexUtils.createText(furnace.getStack(0)));
                displayBuilder.setComponent(HoverDisplayBuilder.FUEL, PolydexUtils.createText(furnace.getStack(1)));
                displayBuilder.setComponent(HoverDisplayBuilder.OUTPUT, PolydexUtils.createText(furnace.getStack(2)));
            }

            if (PolydexImpl.config.displayMiningProgress && target.isMining()) {
                displayBuilder.setComponent(HoverDisplayBuilder.PROGRESS, Text.literal("" + (int) (target.getBreakingProgress() * 100))
                        .append(Text.literal("%").formatted(Formatting.GRAY)));
            }
        }
    }

    public static Collection<PolydexPage> addCustomPages(MinecraftServer server, PolydexEntry entry) {
        var list = new ArrayList<PolydexPage>();

        for (var custom : CUSTOM_PAGES.getOrDefault(entry.identifier(), Collections.emptyList())) {
            list.add(new CustomPage(custom.entryId(), custom));
        }

        return list;
    }

    public static void onReload(ResourceManager manager) {
        CUSTOM_PAGES.clear();
        var resources = manager.findResources("polydex_page", path -> path.getPath().endsWith(".json"));

        for (var resource : resources.entrySet()) {
            try {
                try (var reader = new BufferedReader(new InputStreamReader(resource.getValue().getInputStream()))) {
                    var json = JsonParser.parseReader(reader);

                    var result = CustomPage.ViewData.CODEC.parse(JsonOps.INSTANCE, json);

                    result.result().ifPresent(viewData -> CUSTOM_PAGES.computeIfAbsent(viewData.entryId(), (e) -> new ArrayList<>()).add(viewData));

                    result.error().ifPresent(error -> LOGGER.error("Failed to parse page at {}: {}", resource.getKey(), error.toString()));
                }
            } catch (Exception e) {
                LOGGER.error("Failed to read page at {}", resource.getKey(), e);
            }
        }
    }

    public record PackedEntries(List<PolydexEntry> all, List<PolydexEntry> nonEmpty) {
        public static PackedEntries create() {
            return new PackedEntries(new ArrayList<>(), new ArrayList<>());
        }

        public List<PolydexEntry> get(boolean all) {
            return all ? this.all : this.nonEmpty;
        }

        public void clear() {
            this.all.clear();
            this.nonEmpty.clear();
        }

        public void add(PolydexEntry entry) {
            this.all.add(entry);
            if (!entry.recipeOutput().isEmpty() || !entry.ingredients().isEmpty()) {
                this.nonEmpty.add(entry);
            }
        }

        public void recalculateEmpty() {
            this.nonEmpty.clear();
            for (var entry : this.all) {
                if (!entry.recipeOutput().isEmpty() || !entry.ingredients().isEmpty()) {
                    this.nonEmpty.add(entry);
                }
            }
        }

        public void addAll(Collection<PolydexEntry> groupEntries) {
            this.all.addAll(groupEntries);

            for (var entry : groupEntries) {
                if (!entry.recipeOutput().isEmpty() || !entry.ingredients().isEmpty()) {
                    this.nonEmpty.add(entry);
                }
            }
        }
    }

    public record NamespacedEntry(String namespace, Text display, Function<ServerPlayerEntity, ItemStack> icon, PackedEntries entries) {
        public static NamespacedEntry ofMod(String namespace) {
            return ofMod(namespace, PackedEntries.create());
        }

        public static NamespacedEntry ofMod(String namespace, ItemStack defaultIcon) {
            return ofMod(namespace, (p) -> defaultIcon);
        }
        public static NamespacedEntry ofMod(String namespace, Function<ServerPlayerEntity, ItemStack> defaultIcon) {
            return ofMod(namespace, PackedEntries.create(), defaultIcon);
        }

        public static NamespacedEntry ofMod(String namespace, PackedEntries entries) {
            ItemStack icon = Items.BOOK.getDefaultStack();

            {
                var id = Registries.ITEM.getIds().stream().filter((idx) -> idx.getNamespace().equals(namespace)).findFirst();

                if (id.isPresent()) {
                    icon = Registries.ITEM.get(id.get()).getDefaultStack();
                }
            }

            ItemStack finalIcon = icon;
            return ofMod(namespace, entries, (p) -> finalIcon);
        }
        public static NamespacedEntry ofMod(String namespace, PackedEntries entries, Function<ServerPlayerEntity, ItemStack> defaultIcon) {
            var icon = defaultIcon;
            for (var mod : FabricLoader.getInstance().getAllMods()) {
                try {
                    var val = mod.getMetadata().getCustomValue("polydex:entry/" + namespace);
                    if (val != null && val.getType() != CustomValue.CvType.NULL) {
                        var obj = val.getAsObject();
                        Text display;
                        if (obj.containsKey("name")) {
                            display = TextParserUtils.formatText(obj.get("name").getAsString());
                        } else {
                            display = Text.literal(mod.getMetadata().getName());
                        }

                        if (obj.containsKey("icon")) {
                            try {
                                var itemStringReader = (ItemStringReader.item(Registries.ITEM.getReadOnlyWrapper(), new StringReader(obj.get("icon").getAsString())));

                                var iconStack = itemStringReader.item().value().getDefaultStack();
                                if (itemStringReader.nbt() != null) {
                                    iconStack.setNbt(itemStringReader.nbt());
                                }
                                icon = (p) -> iconStack;
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        return new NamespacedEntry(namespace, display, icon, entries);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            for (var mod : FabricLoader.getInstance().getAllMods()) {
                if (mod.getMetadata().getId().equals(namespace)) {
                    return new NamespacedEntry(namespace, Text.literal(mod.getMetadata().getName()), icon, entries);
                }
            }

            return new NamespacedEntry(namespace, Text.literal(namespace), icon, entries);
        }

        public static NamespacedEntry ofItemGroup(ItemGroup group) {
            return new NamespacedEntry(group.getDisplayName().getString(), group.getDisplayName(), (p) -> group.getIcon(), PackedEntries.create());
        }
    }
}
