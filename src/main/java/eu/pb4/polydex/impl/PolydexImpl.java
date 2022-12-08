package eu.pb4.polydex.impl;

import com.google.gson.JsonParser;
import com.mojang.brigadier.StringReader;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import eu.pb4.placeholders.api.TextParserUtils;
import eu.pb4.polydex.api.*;
import eu.pb4.polydex.impl.book.view.CustomView;
import eu.pb4.polydex.impl.book.view.PotionRecipeView;
import eu.pb4.polydex.mixin.BrewingRecipeAccessor;
import eu.pb4.polydex.mixin.BrewingRecipeRegistryAccessor;
import eu.pb4.polymer.core.impl.InternalServerRegistry;
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
import net.minecraft.recipe.RecipeType;
import net.minecraft.resource.ResourceManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Nameable;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.MathHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

public class PolydexImpl {
    public static Codec<ItemStack> ITEM_STACK_CODEC = Codec.either(Identifier.CODEC, ItemStack.CODEC).xmap(
            x -> x.left().isPresent() ? Registries.ITEM.get(x.left().get()).getDefaultStack() : x.right().get(), x -> x.hasNbt() || x.getCount() != 1 ? Either.right(x) : Either.left(Registries.ITEM.getId(x.getItem())));

    public static final Map<Identifier, Function<PolydexTarget, TargetDisplay>> DISPLAYS = new HashMap<>();
    public static final HashMap<RecipeType<?>, ItemPageView<Recipe<?>>> RECIPE_VIEWS = new HashMap<>();
    public static final List<ItemPageView.PageEntryCreator> VIEWS = new ArrayList<>();
    public static final String ID = "polydex";
    public static final Map<Item, Function<Item, @Nullable Collection<ItemEntry>>> ITEM_ENTRY_BUILDERS = new HashMap<>();
    public static final PackedEntries ITEM_ENTRIES = PackedEntries.create();
    public static final Map<String, NamespacedEntry> BY_NAMESPACE = new HashMap<>();
    public static final Map<ItemGroup, NamespacedEntry> BY_ITEMGROUP = new HashMap<>();

    public static final List<NamespacedEntry> NAMESPACED_ENTRIES = new ArrayList<>();
    public static final List<NamespacedEntry> ITEM_GROUP_ENTRIES = new ArrayList<>();

    public static final Logger LOGGER = LogManager.getLogger("Polydex");
    public static final List<Consumer<DisplayBuilder>> DISPLAY_BUILDER_CONSUMERS = new ArrayList<>();
    public static final Map<Identifier, List<CustomView.ViewData>> CUSTOM_PAGES = new HashMap<>();
    public static Codec<Text> TEXT = Codec.either(Codec.STRING, Codecs.TEXT)
            .xmap(either -> either.map(TextParserUtils::formatTextSafe, Function.identity()), Either::right);


    public static PolydexConfig config = new PolydexConfig();

    static {
        PolydexInitializer.init();
    }

    public static Identifier id(String path) {
        return new Identifier(ID, path);
    }

    public static void updateCaches(MinecraftServer server) {
        ITEM_ENTRIES.clear();
        BY_NAMESPACE.clear();
        BY_ITEMGROUP.clear();
        BY_NAMESPACE.put("minecraft", new NamespacedEntry("minecraft", Text.literal("Minecraft (Vanilla)"), Items.GRASS_BLOCK.getDefaultStack(), PackedEntries.create()));
        NAMESPACED_ENTRIES.clear();
        ITEM_GROUP_ENTRIES.clear();

        var map = new HashMap<Item, Collection<Recipe<?>>>();

        for (var item : Registries.ITEM) {
            map.put(item, new ArrayList<>());
        }

        var recipes = server.getRecipeManager().values();
        PolydexServerInterface.updateTimeReference(server);

        for (var recipe : recipes) {
            if (recipe.getOutput() != null) {
                map.get(recipe.getOutput().getItem()).add(recipe);
            }
            PolydexServerInterface.updateTimeReference(server);
        }
        PolydexServerInterface.updateTimeReference(server);

        var entries = new ObjectLinkedOpenHashSet<ItemEntry>();

        try {
            ItemGroups.updateDisplayParameters(server.getOverworld().getEnabledFeatures(), false);

        } catch (Throwable e) {
            e.printStackTrace();
        }

        var allGroups = new ArrayList<>(ItemGroups.getGroups());

        if (FabricLoader.getInstance().isModLoaded("polymer-core")) {
            try {
                InternalServerRegistry.ITEM_GROUPS.entries().forEach(x -> allGroups.add(x.getValue()));
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

        for (var group : allGroups) {
            if (group.getType() == ItemGroup.Type.CATEGORY) {
                var groupEntries = new ObjectLinkedOpenHashSet<ItemEntry>();
                var namespacedEntry = NamespacedEntry.ofItemGroup(group);

                for (var item : group.getDisplayStacks()) {
                    groupEntries.add(ItemEntry.of(item));
                }
                for (var item : group.getSearchTabStacks()) {
                    groupEntries.add(ItemEntry.of(item));
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
            PolydexServerInterface.updateTimeReference(server);
        }

        for (var entry : entries) {
            for (var viewBuilder : VIEWS) {
                var pageEntries = viewBuilder.createEntries(server, entry, map.get(entry.item()));

                if (pageEntries != null) {
                    entry.pages().addAll(pageEntries);
                }
            }
        }

        for (var entry : entries) {
            ITEM_ENTRIES.add(entry);

            BY_NAMESPACE.computeIfAbsent(entry.identifier().getNamespace(), (x) -> NamespacedEntry.ofMod(x, entry.stack().copy())).entries.add(entry);
            PolydexServerInterface.updateTimeReference(server);
        }


        var copy = new ArrayList<>(ITEM_ENTRIES.all);
        for (var entry : ITEM_ENTRIES.all) {
            for (var page : entry.pages()) {
                for (var entry2 : copy) {
                    for (var ingredient : page.getIngredients()) {
                        if (ingredient.test(entry2.stack())) {
                            entry2.ingredients().add(page);
                            break;
                        }
                    }
                }
                PolydexServerInterface.updateTimeReference(server);
            }
        }

        ITEM_ENTRIES.recalculateEmpty();

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

    public static Collection<PageEntry<?>> buildRecipes(MinecraftServer server, ItemEntry entry, Collection<Recipe<?>> recipes) {
        var list = new ArrayList<PageEntry<?>>();
        for (var recipe : recipes) {
            var view = PolydexImpl.RECIPE_VIEWS.get(recipe.getType());
            if (view != null) {
                list.add(new PageEntry<>(view, recipe));
            }
        }

        return list;
    }

    public static Collection<PageEntry<?>> potionRecipe(MinecraftServer server, ItemEntry entry) {
        var list = new ArrayList<PageEntry<?>>();
        var itemRecipes = BrewingRecipeRegistryAccessor.getITEM_RECIPES();
        var potionRecipes = BrewingRecipeRegistryAccessor.getPOTION_RECIPES();

        for (var recipe : itemRecipes) {
            var access = (BrewingRecipeAccessor<Item>) recipe;

            if (access.getOutput() == entry.item()) {
                list.add(PotionRecipeView.ITEM.toEntry(recipe));
            }
        }

        var potion = PotionUtil.getPotion(entry.stack());
        if (potion != Potions.EMPTY) {
            for (var recipe : potionRecipes) {
                var access = (BrewingRecipeAccessor<Potion>) recipe;

                if (access.getOutput() == potion) {
                    list.add(PotionRecipeView.POTION.toEntry(recipe));
                }
            }
        }

        return list;
    }

    public static void defaultBuilder(DisplayBuilder displayBuilder) {
        var target = displayBuilder.getTarget();
        var entity = target.getEntity();
        if (entity != null) {
            displayBuilder.setComponent(DisplayBuilder.NAME, entity.getDisplayName());

            if (entity instanceof LivingEntity livingEntity) {
                if (PolydexImpl.config.displayEntityHealth) {
                    displayBuilder.setComponent(DisplayBuilder.HEALTH, Text.literal("").append(Text.literal("♥ ").formatted(Formatting.RED))
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
                        displayBuilder.setComponent(DisplayBuilder.EFFECTS, PolydexUtils.mergeText(effects, PolydexUtils.SPACE_SEPARATOR));
                    }
                }

            }
        } else {
            if (target.getBlockEntity() instanceof Nameable nameable) {
                if (nameable.hasCustomName()) {
                    displayBuilder.setComponent(DisplayBuilder.NAME, nameable.getCustomName());
                } else {
                    displayBuilder.setComponent(DisplayBuilder.NAME, nameable.getDisplayName());
                }
            } else {
                displayBuilder.setComponent(DisplayBuilder.NAME, target.getBlockState().getBlock().getName());
            }

            if (PolydexImpl.config.displayCantMine && (!target.getPlayer().canHarvest(target.getBlockState()) || target.getBlockState().calcBlockBreakingDelta(target.getPlayer(), target.getPlayer().world, target.getTargetPos()) <= 0)) {
                var text = Text.literal("⛏").formatted(Formatting.DARK_RED);
                if (!displayBuilder.isSmall()) {
                    text.append(" ").append(Text.translatable("text.polydex.cant_mine").formatted(Formatting.RED));
                }
                displayBuilder.setComponent(DisplayBuilder.EFFECTS, text);
            }

            if (config.displayAdditional && target.getBlockEntity() instanceof AbstractFurnaceBlockEntity furnace) {
                displayBuilder.setComponent(DisplayBuilder.INPUT, PolydexUtils.createText(furnace.getStack(0)));
                displayBuilder.setComponent(DisplayBuilder.FUEL, PolydexUtils.createText(furnace.getStack(1)));
                displayBuilder.setComponent(DisplayBuilder.OUTPUT, PolydexUtils.createText(furnace.getStack(2)));
            }

            if (PolydexImpl.config.displayMiningProgress && target.isMining()) {
                displayBuilder.setComponent(DisplayBuilder.PROGRESS, Text.literal("" + (int) (target.getBreakingProgress() * 100))
                        .append(Text.literal("%").formatted(Formatting.GRAY)));
            }
        }
    }

    public static Collection<PageEntry<?>> addCustomPages(MinecraftServer server, ItemEntry entry) {
        var list = new ArrayList<PageEntry<?>>();

        for (var custom : CUSTOM_PAGES.getOrDefault(entry.identifier(), Collections.emptyList())) {
            list.add(CustomView.INSTANCE.toEntry(custom));
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

                    var result = CustomView.ViewData.CODEC.parse(JsonOps.INSTANCE, json);

                    result.result().ifPresent(viewData -> CUSTOM_PAGES.computeIfAbsent(viewData.entryId(), (e) -> new ArrayList<>()).add(viewData));

                    result.error().ifPresent(error -> LOGGER.error("Failed to parse page at {}: {}", resource.getKey(), error.toString()));
                }
            } catch (Exception e) {
                LOGGER.error("Failed to read page at {}", resource.getKey(), e);
            }
        }
    }

    public record PackedEntries(List<ItemEntry> all, List<ItemEntry> nonEmpty) {
        public static PackedEntries create() {
            return new PackedEntries(new ArrayList<>(), new ArrayList<>());
        }

        public List<ItemEntry> get(boolean all) {
            return all ? this.all : this.nonEmpty;
        }

        public void clear() {
            this.all.clear();
            this.nonEmpty.clear();
        }

        public void add(ItemEntry entry) {
            this.all.add(entry);
            if (!entry.pages().isEmpty() || !entry.ingredients().isEmpty()) {
                this.nonEmpty.add(entry);
            }
        }

        public void recalculateEmpty() {
            this.nonEmpty.clear();
            for (var entry : this.all) {
                if (!entry.pages().isEmpty() || !entry.ingredients().isEmpty()) {
                    this.nonEmpty.add(entry);
                }
            }
        }

        public void addAll(Collection<ItemEntry> groupEntries) {
            this.all.addAll(groupEntries);

            for (var entry : groupEntries) {
                if (!entry.pages().isEmpty() || !entry.ingredients().isEmpty()) {
                    this.nonEmpty.add(entry);
                }
            }
        }
    }

    public record NamespacedEntry(String namespace, Text display, ItemStack icon, PackedEntries entries) {
        public static NamespacedEntry ofMod(String namespace) {
            return ofMod(namespace, PackedEntries.create());
        }

        public static NamespacedEntry ofMod(String namespace, ItemStack defaultIcon) {
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

            return ofMod(namespace, entries, icon);
        }
        public static NamespacedEntry ofMod(String namespace, PackedEntries entries, ItemStack defaultIcon) {
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

                                icon = itemStringReader.item().value().getDefaultStack();
                                if (itemStringReader.nbt() != null) {
                                    icon.setNbt(itemStringReader.nbt());
                                }
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
            return new NamespacedEntry(group.getDisplayName().getString(), group.getDisplayName(), group.getIcon().copy(), PackedEntries.create());
        }
    }
}
