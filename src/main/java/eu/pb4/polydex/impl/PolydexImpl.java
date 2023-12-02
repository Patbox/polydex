package eu.pb4.polydex.impl;

import com.google.gson.JsonParser;
import com.mojang.brigadier.StringReader;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import eu.pb4.placeholders.api.TextParserUtils;
import eu.pb4.polydex.api.v1.recipe.PolydexPageUtils;
import eu.pb4.polydex.api.v1.hover.HoverDisplay;
import eu.pb4.polydex.api.v1.hover.HoverDisplayBuilder;
import eu.pb4.polydex.api.v1.hover.PolydexTarget;
import eu.pb4.polydex.api.v1.recipe.PolydexCategory;
import eu.pb4.polydex.api.v1.recipe.PolydexEntry;
import eu.pb4.polydex.api.v1.recipe.PolydexPage;
import eu.pb4.polydex.api.v1.recipe.PolydexStack;
import eu.pb4.polydex.impl.book.view.CustomPage;
import eu.pb4.polydex.impl.book.view.PotionRecipePage;
import eu.pb4.polydex.mixin.BrewingRecipeAccessor;
import eu.pb4.polydex.mixin.BrewingRecipeRegistryAccessor;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.CustomValue;
import net.minecraft.block.Block;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityType;
import net.minecraft.item.*;
import net.minecraft.potion.PotionUtil;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.registry.Registries;
import net.minecraft.command.argument.ItemStringReader;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.Potion;
import net.minecraft.recipe.Recipe;
import net.minecraft.resource.ResourceManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
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
    public static final Map<Identifier, Function<PolydexTarget, HoverDisplay>> DISPLAYS = new HashMap<>();
    public static final HashMap<Class<?>, Function<RecipeEntry<?>, PolydexPage>> RECIPE_VIEWS = new HashMap<>();
    public static final List<PolydexPage.EntryModifier> ENTRY_MODIFIERS = new ArrayList<>();
    public static final List<PolydexPage.PageCreator> PAGE_CREATORS = new ArrayList<>();
    public static final String ID = "polydex";
    public static final Map<Item, Function<Item, @Nullable Collection<PolydexEntry>>> ITEM_ENTRY_BUILDERS = new HashMap<>();
    public static final PackedEntries ITEM_ENTRIES = PackedEntries.create();
    public static final Map<String, NamespacedEntry> BY_NAMESPACE = new HashMap<>();
    public static final Map<ItemGroup, NamespacedEntry> BY_ITEMGROUP = new HashMap<>();
    public static final List<NamespacedEntry> NAMESPACED_ENTRIES = new ArrayList<>();
    public static final List<NamespacedEntry> ITEM_GROUP_ENTRIES = new ArrayList<>();
    public static final Map<PolydexCategory, List<PolydexPage>> CATEGORY_TO_PAGES = new Object2ObjectOpenHashMap<>();
    public static final Map<PolydexStack<?>, PolydexEntry> STACK_TO_ENTRY = new Object2ObjectOpenHashMap<>();
    public static final Map<Identifier, PolydexPage> ID_TO_PAGE = new Object2ObjectOpenHashMap<>();
    public static final Map<Item, List<PolydexEntry>> ITEM_TO_ENTRIES = new Reference2ObjectOpenHashMap<>();
    public static final Logger LOGGER = LogManager.getLogger("Polydex");
    public static final List<Consumer<HoverDisplayBuilder>> DISPLAY_BUILDER_CONSUMERS = new ArrayList<>();
    public static final Map<Block, List<Consumer<HoverDisplayBuilder>>> DISPLAY_BUILDER_CONSUMERS_BLOCK = new IdentityHashMap<>();
    public static final Map<EntityType<?>, List<Consumer<HoverDisplayBuilder>>> DISPLAY_BUILDER_CONSUMERS_ENTITY_TYPE = new IdentityHashMap<>();
    public static final Map<Identifier, List<CustomPage.ViewData>> CUSTOM_PAGES = new HashMap<>();
    public static final Map<Identifier, PolydexCategory> CATEGORY_BY_ID = new HashMap<>();
    public static final Map<Item, Function<ItemStack, @Nullable PolydexEntry>> ITEM_ENTRY_CREATOR = new HashMap<>();
    private static final Comparator<PolydexPage> PAGE_SORT = Comparator.<PolydexPage>comparingInt((x) -> -x.priority()).thenComparing(PolydexPage::sortingId).thenComparing(PolydexPage::identifier);
    private static final Map<String, Text> MOD_NAMES = new HashMap<>();
    public static Codec<ItemStack> ITEM_STACK_CODEC = Codec.either(Identifier.CODEC, ItemStack.CODEC).xmap(
            x -> x.left().isPresent() ? Registries.ITEM.get(x.left().get()).getDefaultStack() : x.right().get(), x -> x.hasNbt() || x.getCount() != 1 ? Either.right(x) : Either.left(Registries.ITEM.getId(x.getItem())));
    public static Codec<Text> TEXT = Codec.either(Codec.STRING, TextCodecs.CODEC)
            .xmap(either -> either.map(TextParserUtils::formatTextSafe, Function.identity()), Either::right);
    public static PolydexConfigImpl config = new PolydexConfigImpl();
    @Nullable
    private static CompletableFuture<Void> cacheBuilding = null;
    private static boolean isReady;

    static {
        PolydexInitializer.init();
    }

    public static boolean isReady() {
        return isReady;
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
            try {
                updateCaches(server, recipes);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }).thenAcceptAsync((x) -> {
            LOGGER.info("[Polydex] Done! It took {} ms", (System.currentTimeMillis() - time));
            isReady = true;
            cacheBuilding = null;
        }, server);
    }

    public static void updateCaches(MinecraftServer server, Collection<RecipeEntry<?>> recipes) {
        ITEM_ENTRIES.clear();
        BY_NAMESPACE.clear();
        BY_ITEMGROUP.clear();
        STACK_TO_ENTRY.clear();
        ITEM_TO_ENTRIES.clear();
        CATEGORY_TO_PAGES.clear();
        CATEGORY_BY_ID.clear();
        ID_TO_PAGE.clear();
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
                    var x = ITEM_ENTRY_CREATOR.get(item.getItem());

                    if (x != null) {
                        var y = x.apply(item);
                        if (y != null) {
                            groupEntries.add(y);
                        }
                    } else {
                        groupEntries.add(PolydexEntry.of(item));
                    }
                }
                for (var item : group.getSearchTabStacks()) {
                    var x = ITEM_ENTRY_CREATOR.get(item.getItem());

                    if (x != null) {
                        var y = x.apply(item);
                        if (y != null) {
                            groupEntries.add(y);
                        }
                    } else {
                        groupEntries.add(PolydexEntry.of(item));
                    }
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
                if (custom != null) {
                    entries.addAll(custom);
                }
            }
        }

        var globalPages = new ArrayList<PolydexPage>(recipes.size());
        for (var recipe : recipes) {
            var baseClass = (Class<?>) recipe.value().getClass();

            while (baseClass != Object.class) {
                var view = PolydexImpl.RECIPE_VIEWS.get(baseClass);

                if (view != null) {
                    var page = view.apply(recipe);
                    if (page != null) {
                        globalPages.add(page);
                        ID_TO_PAGE.put(page.identifier(), page);
                    }
                    break;
                }
                baseClass = baseClass.getSuperclass();
            }
        }

        for (var creator : PAGE_CREATORS) {
            creator.createPages(server, globalPages::add);
        }

        for (var globalPage : globalPages) {
            for (var entry : entries) {
                if (globalPage.isOwner(server, entry)) {
                    entry.outputPages().add(globalPage);
                }

                for (var ingredient : globalPage.ingredients()) {
                    if (ingredient.matches(entry.stack(), false)) {
                        entry.ingredientPages().add(globalPage);
                        break;
                    }
                }
            }

            for (var category : globalPage.categories()) {
                CATEGORY_TO_PAGES.computeIfAbsent(category, (x) -> {
                    CATEGORY_BY_ID.put(x.identifier(), x);
                    return new ArrayList<>();
                }).add(globalPage);
            }
        }

        for (var entry : entries) {
            for (var viewBuilder : ENTRY_MODIFIERS) {
                viewBuilder.entryModifier(server, entry);
            }
        }

        for (var entry : entries) {
            entry.outputPages().sort(PAGE_SORT);
            entry.ingredientPages().sort(PAGE_SORT);

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

        for (var x : CATEGORY_TO_PAGES.values()) {
            x.sort(PAGE_SORT);
        }

        NAMESPACED_ENTRIES.addAll(BY_NAMESPACE.values());
        NAMESPACED_ENTRIES.sort(Comparator.comparing((s) -> s.namespace));
        ITEM_GROUP_ENTRIES.addAll(BY_ITEMGROUP.values());
        ITEM_GROUP_ENTRIES.sort(Comparator.comparing((s) -> s.namespace));

        config = PolydexConfigImpl.loadOrCreateConfig();
    }

    public static void potionRecipe(MinecraftServer server, Consumer<PolydexPage> consumer) {
        var itemRecipes = BrewingRecipeRegistryAccessor.getITEM_RECIPES();
        var potionRecipes = BrewingRecipeRegistryAccessor.getPOTION_RECIPES();

        for (var recipe : itemRecipes) {
            var access = (BrewingRecipeAccessor<Item>) recipe;

            consumer.accept(new PotionRecipePage.ItemBase(new Identifier("minecraft:brewing/item/"
                    + Registries.ITEM.getId(access.getInput()).toUnderscoreSeparatedString() + "/" + Registries.ITEM.getId(access.getOutput()).toUnderscoreSeparatedString()), recipe));
        }

        for (var recipe : potionRecipes) {
            var access = (BrewingRecipeAccessor<Potion>) recipe;

            consumer.accept(new PotionRecipePage.PotionBase(new Identifier("minecraft:brewing/potion/"
                    + Registries.POTION.getId(access.getInput()).toUnderscoreSeparatedString() + "/" + Registries.POTION.getId(access.getOutput()).toUnderscoreSeparatedString()),
                    recipe));
        }


    }

    public static void defaultBuilder(HoverDisplayBuilder displayBuilder) {
        var target = displayBuilder.getTarget();
        var entity = target.entity();
        if (entity != null) {
            displayBuilder.setComponent(HoverDisplayBuilder.NAME, entity.getDisplayName());
            displayBuilder.setComponent(HoverDisplayBuilder.RAW_ID, Text.literal(Registries.ENTITY_TYPE.getId(entity.getType()).toString()));
            if (PolydexImpl.config.displayModSource) {
                displayBuilder.setComponent(HoverDisplayBuilder.MOD_SOURCE, getMod(Registries.ENTITY_TYPE.getId(entity.getType())));
            }
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
                        displayBuilder.setComponent(HoverDisplayBuilder.EFFECTS, PolydexImplUtils.mergeText(effects, PolydexImplUtils.SPACE_SEPARATOR));
                    }
                }
            }

            var list = DISPLAY_BUILDER_CONSUMERS_ENTITY_TYPE.get(entity.getType());

            if (list != null) {
                for (var a : list) {
                    a.accept(displayBuilder);
                }
            }
        } else if (target.blockState() != null) {
            displayBuilder.setComponent(HoverDisplayBuilder.RAW_ID, Text.literal(Registries.BLOCK.getId(target.blockState().getBlock()).toString()));
            if (target.blockEntity() instanceof Nameable nameable) {
                if (nameable.hasCustomName()) {
                    displayBuilder.setComponent(HoverDisplayBuilder.NAME, nameable.getCustomName());
                } else {
                    displayBuilder.setComponent(HoverDisplayBuilder.NAME, nameable.getDisplayName());
                }
            } else if (target.blockEntity() instanceof SkullBlockEntity skull) {
                var owner = skull.getOwner();
                if (owner != null && owner.getName() != null && !owner.getName().isEmpty()) {
                    displayBuilder.setComponent(HoverDisplayBuilder.NAME, Text.translatable("block.minecraft.player_head.named", owner.getName()));
                } else {
                    displayBuilder.setComponent(HoverDisplayBuilder.NAME, target.blockState().getBlock().getName());
                }
            } else {
                displayBuilder.setComponent(HoverDisplayBuilder.NAME, target.blockState().getBlock().getName());
            }
            if (PolydexImpl.config.displayModSource) {
                displayBuilder.setComponent(HoverDisplayBuilder.MOD_SOURCE, getMod(Registries.BLOCK.getId(target.blockState().getBlock())));
            }

            if (PolydexImpl.config.displayCantMine && (!target.player().canHarvest(target.blockState()) || target.blockState().calcBlockBreakingDelta(target.player(), target.player().getWorld(), target.pos()) <= 0)) {
                var text = Text.literal("⛏").formatted(Formatting.DARK_RED);
                if (!displayBuilder.isSmall()) {
                    text.append(" ").append(Text.translatable("text.polydex.cant_mine").formatted(Formatting.RED));
                }
                displayBuilder.setComponent(HoverDisplayBuilder.EFFECTS, text);
            }

            if (config.displayAdditional && target.blockEntity() instanceof AbstractFurnaceBlockEntity furnace) {
                displayBuilder.setComponent(HoverDisplayBuilder.INPUT, PolydexPageUtils.createText(furnace.getStack(0)));
                displayBuilder.setComponent(HoverDisplayBuilder.FUEL, PolydexPageUtils.createText(furnace.getStack(1)));
                displayBuilder.setComponent(HoverDisplayBuilder.OUTPUT, PolydexPageUtils.createText(furnace.getStack(2)));
            }

            if (PolydexImpl.config.displayMiningProgress && target.isMining()) {
                displayBuilder.setComponent(HoverDisplayBuilder.PROGRESS, Text.literal("" + (int) (target.breakingProgress() * 100))
                        .append(Text.literal("%").formatted(Formatting.GRAY)));
            }

            var list = DISPLAY_BUILDER_CONSUMERS_BLOCK.get(target.blockState().getBlock());

            if (list != null) {
                for (var a : list) {
                    a.accept(displayBuilder);
                }
            }
        }
    }

    public static Text getMod(Identifier id) {
        return MOD_NAMES.computeIfAbsent(id.getNamespace(), PolydexImpl::createModName);
    }

    private static Text createModName(String s) {
        var container = FabricLoader.getInstance().getModContainer(s);

        if (container.isPresent()) {
            return Text.literal(container.get().getMetadata().getName());
        }

        return Text.literal(s);
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

    public static PolydexEntry seperateCustomEnchantments(ItemStack stack) {
        var ench = EnchantmentHelper.get(stack);
        var string = new StringBuilder();

        ench.forEach((a, b) -> {
            string.append(Registries.ENCHANTMENT.getId(a).toUnderscoreSeparatedString()).append("/");
        });


        var baseId = Registries.ITEM.getId(stack.getItem());

        return PolydexEntry.of(baseId.withSuffixedPath("//" + string), stack);
    }

    public static PolydexEntry seperateCustomPotion(ItemStack stack) {
        var potion = PotionUtil.getPotion(stack);
        var baseId = Registries.ITEM.getId(stack.getItem());
        return PolydexEntry.of(baseId.withSuffixedPath("/" + Registries.POTION.getId(potion).toUnderscoreSeparatedString()), stack);
    }

    public record PackedEntries(List<PolydexEntry> all, List<PolydexEntry> nonEmpty, Map<Identifier, PolydexEntry> nonEmptyById) {
        public static PackedEntries create() {
            return new PackedEntries(new ArrayList<>(), new ArrayList<>(), new HashMap<>());
        }

        public List<PolydexEntry> get(boolean all) {
            return all ? this.all : this.nonEmpty;
        }

        public void clear() {
            this.all.clear();
            this.nonEmpty.clear();
            this.nonEmptyById.clear();
        }

        public void add(PolydexEntry entry) {
            this.all.add(entry);
            if (!entry.outputPages().isEmpty() || !entry.ingredientPages().isEmpty()) {
                this.nonEmpty.add(entry);
                this.nonEmptyById.put(entry.identifier(), entry);
            }
        }

        public void recalculateEmpty() {
            this.nonEmpty.clear();
            this.nonEmptyById.clear();
            for (var entry : this.all) {
                if (!entry.outputPages().isEmpty() || !entry.ingredientPages().isEmpty()) {
                    this.nonEmpty.add(entry);
                    this.nonEmptyById.put(entry.identifier(), entry);
                }
            }
        }

        public void addAll(Collection<PolydexEntry> groupEntries) {
            this.all.addAll(groupEntries);

            for (var entry : groupEntries) {
                if (!entry.outputPages().isEmpty() || !entry.ingredientPages().isEmpty()) {
                    this.nonEmpty.add(entry);
                    this.nonEmptyById.put(entry.identifier(), entry);
                }
            }
        }
    }

    public record NamespacedEntry(String namespace, Text display, Function<ServerPlayerEntity, ItemStack> icon,
                                  PackedEntries entries) {
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
