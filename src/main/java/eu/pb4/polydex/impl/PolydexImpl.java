package eu.pb4.polydex.impl;

import com.google.gson.JsonParser;
import com.mojang.brigadier.StringReader;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import eu.pb4.placeholders.api.ParserContext;
import eu.pb4.placeholders.api.parsers.NodeParser;
import eu.pb4.polydex.api.v1.recipe.*;
import eu.pb4.polydex.api.v1.hover.HoverDisplay;
import eu.pb4.polydex.api.v1.hover.HoverDisplayBuilder;
import eu.pb4.polydex.api.v1.hover.PolydexTarget;
import eu.pb4.polydex.impl.book.view.CustomPage;
import eu.pb4.polydex.impl.book.view.DebugPage;
import eu.pb4.polydex.impl.book.view.PotionRecipePage;
import eu.pb4.polydex.impl.book.view.ToolUseOnBlockPage;
import eu.pb4.polydex.impl.search.VanillaLanguageDownloader;
import eu.pb4.polydex.mixin.*;
import eu.pb4.polymer.core.api.item.PolymerItemGroupUtils;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroupEntries;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.CustomValue;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.creaking.Creaking;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackLinkedSet;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class PolydexImpl {
    private static final boolean POLYMER_CORE_LOADED = FabricLoader.getInstance().isModLoaded("polymer-core");
    public static final NodeParser PARSER = NodeParser.builder().simplifiedTextFormat().quickText().staticPreParsing().build();
    public static final Map<Identifier, Function<PolydexTarget, HoverDisplay>> DISPLAYS = new HashMap<>();
    public static final HashMap<Class<?>, Function<RecipeHolder<?>, PolydexPage>> RECIPE_VIEWS = new HashMap<>();
    public static final List<PolydexPage.EntryModifier> ENTRY_MODIFIERS = new ArrayList<>();
    public static final List<PolydexPage.PageCreator> PAGE_CREATORS = new ArrayList<>();
    public static final String ID = "polydex";
    public static final Map<Item, Function<Item, @Nullable Collection<PolydexEntry>>> ITEM_ENTRY_BUILDERS = new HashMap<>();
    public static final List<BiConsumer<MinecraftServer, PolydexEntry.EntryConsumer>> ENTRY_PROVIDERS = new ArrayList<>();
    public static final PackedEntries ITEM_ENTRIES = PackedEntries.create();
    public static final Map<String, NamespacedEntry> BY_NAMESPACE = new HashMap<>();
    public static final Map<CreativeModeTab, NamespacedEntry> BY_ITEMGROUP = new HashMap<>();
    public static final List<NamespacedEntry> NAMESPACED_ENTRIES = new ArrayList<>();
    public static final List<NamespacedEntry> ITEM_GROUP_ENTRIES = new ArrayList<>();
    public static final Map<PolydexCategory, List<PolydexPage>> CATEGORY_TO_PAGES = new Object2ObjectOpenHashMap<>();
    public static final Map<PolydexStack<?>, PolydexEntry> STACK_TO_ENTRY = new Object2ObjectOpenCustomHashMap<>(new Hash.Strategy<>() {
        @Override
        public int hashCode(PolydexStack<?> o) {
            return o.getSourceHashCode();
        }

        @Override
        public boolean equals(PolydexStack<?> a, PolydexStack<?> b) {
            return a != null && b != null && a.matches(b, true);
        }
    });
    public static final Map<Identifier, PolydexPage> ID_TO_PAGE = new Object2ObjectOpenHashMap<>();
    public static final Map<Item, List<PolydexEntry>> ITEM_TO_ENTRIES = new Reference2ObjectOpenHashMap<>();
    public static final Logger LOGGER = LogManager.getLogger("Polydex");
    public static final List<Consumer<HoverDisplayBuilder>> DISPLAY_BUILDER_CONSUMERS = new ArrayList<>();
    public static final Map<Block, List<Consumer<HoverDisplayBuilder>>> DISPLAY_BUILDER_CONSUMERS_BLOCK = new IdentityHashMap<>();
    public static final Map<EntityType<?>, List<Consumer<HoverDisplayBuilder>>> DISPLAY_BUILDER_CONSUMERS_ENTITY_TYPE = new IdentityHashMap<>();
    public static final List<CustomPage> CUSTOM_PAGES = new ArrayList<>();
    public static final Map<Identifier, PolydexCategory> CATEGORY_BY_ID = new HashMap<>();
    public static final Map<Item, Function<ItemStack, @Nullable PolydexEntry>> ITEM_ENTRY_CREATOR = new HashMap<>();
    private static final Comparator<PolydexPage> PAGE_SORT = Comparator.<PolydexPage>comparingInt((x) -> -x.priority()).thenComparing(PolydexPage::sortingId).thenComparing(PolydexPage::identifier);
    private static final Map<String, Component> MOD_NAMES = new HashMap<>();
    public static Codec<ItemStack> ITEM_STACK_CODEC = ItemStack.STRICT_CODEC;
    public static Codec<Component> TEXT = Codec.either(Codec.STRING, ComponentSerialization.CODEC)
            .xmap(either -> either.map(x -> PARSER.parseText(x, ParserContext.of()), Function.identity()), Either::right);
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
        return Identifier.fromNamespaceAndPath(ID, path);
    }

    public static PolydexEntry getEntry(ItemStack stack) {
        if (!isReady) {
            return null;
        }
        var pStack = PolydexStack.of(stack);
        var x = STACK_TO_ENTRY.get(pStack);
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
        if (cacheBuilding != null) {
            LOGGER.warn("[Polydex] Cancelling unfinished matching");
            cacheBuilding.cancel(true);
        }
        isReady = false;
        LOGGER.info("[Polydex] Started matching recipes and items...");
        var time = System.currentTimeMillis();
        PolydexPageUtils.BEFORE_PAGE_LOADING.invoker().accept(server);
        var recipes = server.getRecipeManager().getRecipes();
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
            PolydexPageUtils.AFTER_PAGE_LOADING.invoker().accept(server);
        }, server);
    }

    private static List<ItemGroupData> getItemGroupEntries(MinecraftServer server) {
        var list = new ArrayList<ItemGroupData>();
        var context = new CreativeModeTab.ItemDisplayParameters(server.overworld().enabledFeatures(), false, server.registryAccess());
        for (var group : BuiltInRegistries.CREATIVE_MODE_TAB) {
            var data = new ItemGroupData(BuiltInRegistries.CREATIVE_MODE_TAB.getKey(group), group, context.enabledFeatures());
            ((CreativeModeTabAccessor) group).getDisplayItemsGenerator().accept(context, data);
            var tmp = new ArrayList<>(data.stacks);
            data.stacks.clear();
            callItemGroupEvents(data.id, group, tmp, tmp, context);
            data.stacks.addAll(tmp);
            if (!data.stacks.isEmpty()) {
                list.add(data);
            }
        }

        if (POLYMER_CORE_LOADED) {
            for (var group : PolymerItemGroupUtils.REGISTRY) {
                var data = new ItemGroupData(PolymerItemGroupUtils.getId(group), group, context.enabledFeatures());
                ((CreativeModeTabAccessor) group).getDisplayItemsGenerator().accept(context, data);
                var tmp = new ArrayList<>(data.stacks);
                data.stacks.clear();
                callItemGroupEvents(data.id, group, tmp, tmp, context);
                data.stacks.addAll(tmp);
                if (!data.stacks.isEmpty()) {
                    list.add(data);
                }
            }
        }
        return list;
    }

    public static void callItemGroupEvents(Identifier id, CreativeModeTab itemGroup, List<ItemStack> parentTabStacks, List<ItemStack> searchTabStacks, CreativeModeTab.ItemDisplayParameters context) {
        FabricItemGroupEntries fabricCollector = new FabricItemGroupEntries(context, parentTabStacks, searchTabStacks);
        try {
            ItemGroupEvents.modifyEntriesEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB, id)).invoker().modifyEntries(fabricCollector);
        } catch (Throwable ignored) {}

        try {
            ItemGroupEvents.MODIFY_ENTRIES_ALL.invoker().modifyEntries(itemGroup, fabricCollector);
        } catch (Throwable e) {

        }
    }


    public static void updateCaches(MinecraftServer server, Collection<RecipeHolder<?>> recipes) {
        config = PolydexConfigImpl.loadOrCreateConfig(server.registryAccess());
        if (config.enableLanguageSearch) {
            VanillaLanguageDownloader.setup();
        }

        ITEM_ENTRIES.clear();
        BY_NAMESPACE.clear();
        BY_ITEMGROUP.clear();
        STACK_TO_ENTRY.clear();
        ITEM_TO_ENTRIES.clear();
        CATEGORY_TO_PAGES.clear();
        CATEGORY_BY_ID.clear();
        ID_TO_PAGE.clear();
        BY_NAMESPACE.put("minecraft", new NamespacedEntry("minecraft", Component.literal("Minecraft (Vanilla)"), (p) -> Items.GRASS_BLOCK.getDefaultInstance(), PackedEntries.create()));
        NAMESPACED_ENTRIES.clear();
        ITEM_GROUP_ENTRIES.clear();

        var polydexEntries = new ObjectLinkedOpenHashSet<PolydexEntry>();
        var consumer = new PolydexEntry.EntryConsumer() {
            @Override
            public void accept(PolydexEntry entry) {
                polydexEntries.add(entry);
            }

            @Override
            public void accept(PolydexEntry entry, CreativeModeTab group) {
                BY_ITEMGROUP.computeIfAbsent(group, NamespacedEntry::ofItemGroup).entries.add(entry);
                polydexEntries.add(entry);
            }

            @Override
            public void acceptAll(Collection<PolydexEntry> entries) {
                polydexEntries.addAll(entries);
            }

            @Override
            public void acceptAll(Collection<PolydexEntry> entries, CreativeModeTab group) {
                BY_ITEMGROUP.computeIfAbsent(group, NamespacedEntry::ofItemGroup).entries.addAll(entries);
                polydexEntries.addAll(entries);
            }
        };

        for (var provider : ENTRY_PROVIDERS) {
            provider.accept(server, consumer);
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
            creator.createPages(server, (page) -> {
                globalPages.add(page);
                ID_TO_PAGE.put(page.identifier(), page);
            });
        }

        for (var globalPage : globalPages) {
            for (var entry : polydexEntries) {
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

        for (var entry : polydexEntries) {
            for (var viewBuilder : ENTRY_MODIFIERS) {
                viewBuilder.entryModifier(server, entry);
            }
        }

        for (var entry : polydexEntries) {
            entry.outputPages().sort(PAGE_SORT);
            entry.ingredientPages().sort(PAGE_SORT);

            ITEM_ENTRIES.add(entry);
            STACK_TO_ENTRY.put(entry.stack(), entry);
            if (entry.stack().getBackingClass() == ItemStack.class) {
                ITEM_TO_ENTRIES.computeIfAbsent(((ItemStack) entry.stack().getBacking()).getItem(), (a) -> new ArrayList<>()).add(entry);
            }
            BY_NAMESPACE.computeIfAbsent(entry.identifier().getNamespace(), (x) -> NamespacedEntry.ofMod(x, entry.stack()::toTypeDisplayItemStack, server)).entries.add(entry);
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
    }

    public static void defaultEntries(MinecraftServer server, PolydexEntry.EntryConsumer consumer) {
        var allGroups = getItemGroupEntries(server);

        for (var group : allGroups) {
            if (group.group.getType() == CreativeModeTab.Type.CATEGORY) {
                var groupEntries = new ObjectLinkedOpenHashSet<PolydexEntry>();
                for (var item : group.stacks) {
                    if (item.is(ConventionalItemTags.HIDDEN_FROM_RECIPE_VIEWERS)) {
                        continue;
                    }

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

                consumer.acceptAll(groupEntries, group.group);
            }
        }

        for (var item : BuiltInRegistries.ITEM) {
            //noinspection deprecation
            if (item == Items.AIR || item.builtInRegistryHolder().is(ConventionalItemTags.HIDDEN_FROM_RECIPE_VIEWERS)) {
                continue;
            }

            var func = ITEM_ENTRY_BUILDERS.get(item);
            if (func != null) {
                var custom = func.apply(item);
                if (custom != null) {
                    consumer.acceptAll(custom);
                }
            }
        }
    }

    public static void potionRecipe(MinecraftServer server, Consumer<PolydexPage> consumer) {
        var itemRecipes = ((PotionBrewingAccessor) server.potionBrewing()).getContainerMixes();
        var potionRecipes = ((PotionBrewingAccessor) server.potionBrewing()).getPotionMixes();

        for (var recipe : itemRecipes) {
            consumer.accept(new PotionRecipePage.ItemBase(Identifier.parse("minecraft:brewing/item/"
                    + recipe.from().unwrapKey().get().identifier().toDebugFileName() + "/" + recipe.to().unwrapKey().get().identifier().toDebugFileName()), recipe));
        }

        for (var recipe : potionRecipes) {
            consumer.accept(new PotionRecipePage.PotionBase(Identifier.parse("minecraft:brewing/potion/"
                    + recipe.from().unwrapKey().get().identifier().toDebugFileName() + "/" + recipe.to().unwrapKey().get().identifier().toDebugFileName()),
                    recipe));
        }


    }

    public static void defaultBuilder(HoverDisplayBuilder displayBuilder) {
        var target = displayBuilder.getTarget();
        var entity = target.entity();
        if (entity != null) {
            displayBuilder.setComponent(HoverDisplayBuilder.NAME, entity.getDisplayName());
            displayBuilder.setComponent(HoverDisplayBuilder.RAW_ID, Component.literal(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString()));
            if (PolydexImpl.config.displayModSource) {
                displayBuilder.setComponent(HoverDisplayBuilder.MOD_SOURCE, getMod(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType())));
            }
            if (entity instanceof LivingEntity livingEntity) {
                if (PolydexImpl.config.displayEntityHealth) {
                    if (entity instanceof Creaking creaking && creaking.getHomePos() != null) {
                        displayBuilder.setComponent(HoverDisplayBuilder.HEALTH, Component.literal("").append(Component.literal("❤ ").withStyle(ChatFormatting.RED))
                                .append("??")
                                .append(Component.literal("/").withStyle(ChatFormatting.GRAY))
                                .append("??"));
                    } else {
                        displayBuilder.setComponent(HoverDisplayBuilder.HEALTH, Component.literal("").append(Component.literal("❤ ").withStyle(ChatFormatting.RED))
                                .append("" + Math.min(Mth.ceil(livingEntity.getHealth()), Mth.ceil(livingEntity.getMaxHealth())))
                                .append(PolydexImpl.config.displayEntityAbsorption && livingEntity.getAbsorptionAmount() > 0
                                        ? Component.literal("+" + Mth.ceil(Math.min(livingEntity.getAbsorptionAmount(), livingEntity.getMaxAbsorption()))).withStyle(ChatFormatting.YELLOW) : Component.empty())
                                .append(Component.literal("/").withStyle(ChatFormatting.GRAY))
                                .append("" + Mth.ceil(livingEntity.getMaxHealth())));
                    }

                    var value = livingEntity.getAttributeValue(Attributes.ARMOR);

                    if (value > 0) {
                        displayBuilder.setComponent(HoverDisplayBuilder.ARMOR, Component.literal("").append(Component.literal("\uD83D\uDEE1 ").withStyle(ChatFormatting.GRAY))
                                .append("" + Mth.ceil(value)));
                    }
                }

                if (PolydexImpl.config.displayAdditional) {
                    var effects = new ArrayList<Component>();

                    if (livingEntity.isOnFire()) {
                        effects.add(Component.literal("\uD83D\uDD25" + (livingEntity.isFullyFrozen() ? " " : "")).withStyle(ChatFormatting.GOLD));
                    }

                    if (livingEntity.isFullyFrozen()) {
                        effects.add(Component.literal("❄").withStyle(ChatFormatting.AQUA));
                    }

                    for (var effect : livingEntity.getActiveEffects()) {
                        effects.add(Component.literal("⚗").setStyle(net.minecraft.network.chat.Style.EMPTY.withColor(effect.getEffect().value().getColor())));
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
            displayBuilder.setComponent(HoverDisplayBuilder.RAW_ID, Component.literal(BuiltInRegistries.BLOCK.getKey(target.blockState().getBlock()).toString()));
            if (target.blockEntity() instanceof Nameable nameable) {
                if (nameable.hasCustomName()) {
                    displayBuilder.setComponent(HoverDisplayBuilder.NAME, nameable.getCustomName());
                } else if (!(nameable instanceof BannerBlockEntity)) {
                    displayBuilder.setComponent(HoverDisplayBuilder.NAME, nameable.getDisplayName());
                } else {
                    displayBuilder.setComponent(HoverDisplayBuilder.NAME, target.blockState().getBlock().getName());
                }
            } else if (target.blockEntity() instanceof SkullBlockEntity skull) {
                var owner = skull.getOwnerProfile();
                if (owner != null && owner.name().isPresent()) {
                    displayBuilder.setComponent(HoverDisplayBuilder.NAME, Component.translatable("block.minecraft.player_head.named", owner.name().get()));
                } else {
                    displayBuilder.setComponent(HoverDisplayBuilder.NAME, target.blockState().getBlock().getName());
                }
            } else {
                displayBuilder.setComponent(HoverDisplayBuilder.NAME, target.blockState().getBlock().getName());
            }
            if (PolydexImpl.config.displayModSource) {
                displayBuilder.setComponent(HoverDisplayBuilder.MOD_SOURCE, getMod(BuiltInRegistries.BLOCK.getKey(target.blockState().getBlock())));
            }

            if (PolydexImpl.config.displayCantMine && (!target.player().hasCorrectToolForDrops(target.blockState()) || target.blockState().getDestroyProgress(target.player(), target.player().level(), target.pos()) <= 0)) {
                var text = Component.literal("⛏").withStyle(ChatFormatting.DARK_RED);
                if (!displayBuilder.isSmall()) {
                    text.append(" ").append(Component.translatable("text.polydex.cant_mine").withStyle(ChatFormatting.RED));
                }
                displayBuilder.setComponent(HoverDisplayBuilder.EFFECTS, text);
            }

            if (config.displayAdditional && target.blockEntity() instanceof AbstractFurnaceBlockEntity furnace) {
                displayBuilder.setComponent(HoverDisplayBuilder.INPUT, PolydexPageUtils.createText(furnace.getItem(0)));
                displayBuilder.setComponent(HoverDisplayBuilder.FUEL, PolydexPageUtils.createText(furnace.getItem(1)));
                displayBuilder.setComponent(HoverDisplayBuilder.OUTPUT, PolydexPageUtils.createText(furnace.getItem(2)));
            }

            if (PolydexImpl.config.displayMiningProgress && target.isMining()) {
                displayBuilder.setComponent(HoverDisplayBuilder.PROGRESS, Component.literal("" + (int) (target.breakingProgress() * 100))
                        .append(Component.literal("%").withStyle(ChatFormatting.GRAY)));
            }

            var list = DISPLAY_BUILDER_CONSUMERS_BLOCK.get(target.blockState().getBlock());

            if (list != null) {
                for (var a : list) {
                    a.accept(displayBuilder);
                }
            }
        }
    }

    public static Component getMod(Identifier id) {
        return MOD_NAMES.computeIfAbsent(id.getNamespace(), PolydexImpl::createModName);
    }

    private static Component createModName(String s) {
        var container = FabricLoader.getInstance().getModContainer(s);

        if (container.isPresent()) {
            return Component.literal(container.get().getMetadata().getName());
        }

        return Component.literal(s);
    }

    public static void addCustomPages(MinecraftServer server, Consumer<PolydexPage> pageConsumer) {
        CUSTOM_PAGES.forEach(pageConsumer);
    }

    public static void onReload(ResourceManager manager) {
        CUSTOM_PAGES.clear();
        var resources = manager.listResources("polydex_page", path -> path.getPath().endsWith(".json"));

        for (var resource : resources.entrySet()) {
            try {
                try (var reader = new BufferedReader(new InputStreamReader(resource.getValue().open()))) {
                    var json = JsonParser.parseReader(reader);
                    var result = CustomPage.ViewData.CODEC.parse(JsonOps.INSTANCE, json);

                    result.result().ifPresent(x -> CUSTOM_PAGES.add(new CustomPage(resource.getKey(), x)));

                    result.error().ifPresent(error -> LOGGER.error("Failed to parse page at {}: {}", resource.getKey(), error.toString()));
                }
            } catch (Exception e) {
                LOGGER.error("Failed to read page at {}", resource.getKey(), e);
            }
        }
    }

    public static PolydexEntry seperateCustomEnchantments(ItemStack stack) {
        var ench = EnchantmentHelper.getEnchantmentsForCrafting(stack);
        var string = new StringBuilder();

        ench.keySet().forEach((e) -> {
            string.append(e.unwrapKey().get().identifier().toDebugFileName()).append("/");
        });


        var baseId = BuiltInRegistries.ITEM.getKey(stack.getItem());

        return PolydexEntry.of(baseId.withSuffix("//" + string), stack);
    }

    public static PolydexEntry seperateCustomPotion(ItemStack stack) {
        var potion = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        var baseId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return PolydexEntry.of(baseId.withSuffix("/" + potion.potion().get().unwrapKey().map(ResourceKey::identifier).orElse(Identifier.parse("unknown")).toDebugFileName()), stack);
    }

    public static void blockInteractions(MinecraftServer server, Consumer<PolydexPage> polydexPageConsumer) {
        var items = server.registryAccess().lookupOrThrow(Registries.ITEM);
        var axes = PolydexIngredient.of(Ingredient.of(items.getOrThrow(ItemTags.AXES)));
        var shovels = PolydexIngredient.of(Ingredient.of(items.getOrThrow(ItemTags.SHOVELS)));
        var hoes = PolydexIngredient.of(Ingredient.of(items.getOrThrow(ItemTags.HOES)));
        {
            var map = new Reference2ObjectOpenHashMap<Item, LinkedHashSet<Item>>();
            for (var entry : AxeItemAccessor.getSTRIPPABLES().entrySet()) {
                var a = entry.getKey().asItem();
                var b = entry.getValue().asItem();
                if (a != null && b != null) {
                    map.computeIfAbsent(b, (c) -> new LinkedHashSet<>()).add(a);
                }
            }

            for (var entry : map.entrySet()) {
                polydexPageConsumer.accept(new ToolUseOnBlockPage(Identifier.parse("stripping/"
                        + BuiltInRegistries.ITEM.getKey(entry.getKey()).toShortLanguageKey() + "/" + BuiltInRegistries.ITEM.getKey(entry.getValue().getFirst()).toShortLanguageKey()),
                        axes, PolydexIngredient.of(Ingredient.of(entry.getValue().toArray(new ItemLike[0]))), PolydexStack.of(entry.getKey())));
            }
        }
        {
            var map = new Reference2ObjectOpenHashMap<Item, LinkedHashSet<Item>>();
            for (var entry : ShovelItemAccessor.getFLATTENABLES().entrySet()) {
                var a = entry.getKey().asItem();
                var b = entry.getValue().getBlock().asItem();
                if (a != null && b != null) {
                    map.computeIfAbsent(b, (c) -> new LinkedHashSet<>()).add(a);
                }

            }

            for (var entry : map.entrySet()) {
                polydexPageConsumer.accept(new ToolUseOnBlockPage(Identifier.parse("flattening/"
                        + BuiltInRegistries.ITEM.getKey(entry.getKey()).toShortLanguageKey() + "/" + BuiltInRegistries.ITEM.getKey(entry.getValue().getFirst()).toShortLanguageKey()),
                        shovels, PolydexIngredient.of(Ingredient.of(entry.getValue().toArray(new ItemLike[0]))), PolydexStack.of(entry.getKey())));
            }
        }

        try {
            var world = new FakeWorld(server) {
                BlockState state = Blocks.AIR.defaultBlockState();
                @Override
                public boolean setBlock(BlockPos pos, BlockState state, int flags, int maxUpdateDepth) {
                    if (state != this.state) {
                        this.state = state;
                        return true;
                    }
                    return false;
                }

                @Override
                public BlockState getBlockState(BlockPos pos) {
                    return this.state;
                }
            };
            var placement = new DirectionalPlaceContext(world, BlockPos.ZERO, Direction.DOWN, Items.DIAMOND_HOE.getDefaultInstance(), Direction.DOWN);
            var map = new Reference2ObjectOpenHashMap<Item, LinkedHashSet<Item>>();

            for (var entry : HoeItemAccessor.getTILLABLES().entrySet()) {
                try {
                    var a = entry.getKey().asItem();
                    world.state = entry.getKey().defaultBlockState();
                    var of = world.state;
                    entry.getValue().getSecond().accept(placement);
                    var b = world.state.getBlock().asItem();
                    if (a != null && of.getBlock() != world.state.getBlock()) {
                        map.computeIfAbsent(b, (c) -> new LinkedHashSet<>()).add(a);
                    }
                } catch (Throwable e) {

                }
            }

            for (var entry : map.entrySet()) {
                polydexPageConsumer.accept(new ToolUseOnBlockPage(Identifier.parse("tilling/"
                        + BuiltInRegistries.ITEM.getKey(entry.getKey()).toShortLanguageKey() + "/" + BuiltInRegistries.ITEM.getKey(entry.getValue().getFirst()).toShortLanguageKey()),
                        hoes, PolydexIngredient.of(Ingredient.of(entry.getValue().toArray(new ItemLike[0]))), PolydexStack.of(entry.getKey())));
            }
        } catch (Throwable e) {

        }


    }

    public static List<PolydexPage> addDebugPage(MinecraftServer server, PolydexEntry entry) {
        if (Objects.requireNonNull(entry.stack().getId()).equals(Identifier.withDefaultNamespace("oak_planks"))) {

        }
        return List.of();
    }

    public static void addDebugPage(MinecraftServer server, Consumer<PolydexPage> polydexPageConsumer) {
        polydexPageConsumer.accept(new DebugPage(Identifier.parse("polydex:debug"), Identifier.withDefaultNamespace("oak_planks")));
    }

    public record PackedEntries(List<PolydexEntry> all, List<PolydexEntry> nonEmpty, Map<Identifier, PolydexEntry> byId, Map<Identifier, PolydexEntry> nonEmptyById) {
        public static PackedEntries create() {
            return new PackedEntries(new ArrayList<>(), new ArrayList<>(), new HashMap<>(), new HashMap<>());
        }

        public List<PolydexEntry> get(boolean all) {
            return all ? this.all : this.nonEmpty;
        }
        @Nullable
        public PolydexEntry get(Identifier identifier, boolean all) {
            return all ? this.byId.get(identifier) : this.nonEmptyById.get(identifier);
        }

        public void clear() {
            this.all.clear();
            this.byId.clear();
            this.nonEmpty.clear();
            this.nonEmptyById.clear();
        }

        public void add(PolydexEntry entry) {
            this.all.add(entry);
            this.byId.put(entry.identifier(), entry);
            if (entry.hasPages()) {
                this.nonEmpty.add(entry);
                this.nonEmptyById.put(entry.identifier(), entry);
            }
        }

        public void recalculateEmpty() {
            this.nonEmpty.clear();
            this.nonEmptyById.clear();
            this.byId.clear();
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
                this.byId.put(entry.identifier(), entry);
                if (!entry.outputPages().isEmpty() || !entry.ingredientPages().isEmpty()) {
                    this.nonEmpty.add(entry);
                    this.nonEmptyById.put(entry.identifier(), entry);
                }
            }
        }
    }

    public record NamespacedEntry(String namespace, Component display, Function<ServerPlayer, ItemStack> icon,
                                  PackedEntries entries) {
        public static NamespacedEntry ofMod(String namespace, MinecraftServer server) {
            return ofMod(namespace, PackedEntries.create(), server);
        }

        public static NamespacedEntry ofMod(String namespace, ItemStack defaultIcon, MinecraftServer server) {
            return ofMod(namespace, (p) -> defaultIcon, server);
        }

        public static NamespacedEntry ofMod(String namespace, Function<ServerPlayer, ItemStack> defaultIcon, MinecraftServer server) {
            return ofMod(namespace, PackedEntries.create(), defaultIcon, server);
        }

        public static NamespacedEntry ofMod(String namespace, PackedEntries entries, MinecraftServer server) {
            ItemStack icon = Items.BOOK.getDefaultInstance();

            {
                var id = BuiltInRegistries.ITEM.keySet().stream().filter((idx) -> idx.getNamespace().equals(namespace)).findFirst();

                if (id.isPresent()) {
                    icon = BuiltInRegistries.ITEM.getValue(id.get()).getDefaultInstance();
                }
            }

            ItemStack finalIcon = icon;
            return ofMod(namespace, entries, (p) -> finalIcon, server);
        }

        public static NamespacedEntry ofMod(String namespace, PackedEntries entries, Function<ServerPlayer, ItemStack> defaultIcon, MinecraftServer server) {
            var icon = defaultIcon;
            for (var mod : FabricLoader.getInstance().getAllMods()) {
                try {
                    var val = mod.getMetadata().getCustomValue("polydex:entry/" + namespace);
                    if (val != null && val.getType() != CustomValue.CvType.NULL) {
                        var obj = val.getAsObject();
                        Component display;
                        if (obj.containsKey("name")) {
                            display = PARSER.parseText(obj.get("name").getAsString(), ParserContext.of());
                        } else {
                            display = Component.literal(mod.getMetadata().getName());
                        }

                        if (obj.containsKey("icon")) {
                            try {
                                var itemStringReader = new ItemParser(server.registryAccess()).parse(new StringReader(obj.get("icon").getAsString()));

                                var iconStack = itemStringReader.item().value().getDefaultInstance();
                                if (itemStringReader.components() != null) {
                                    iconStack.applyComponentsAndValidate(itemStringReader.components());
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
                    return new NamespacedEntry(namespace, Component.literal(mod.getMetadata().getName()), icon, entries);
                }
            }

            return new NamespacedEntry(namespace, Component.literal(namespace), icon, entries);
        }

        public static NamespacedEntry ofItemGroup(CreativeModeTab group) {
            return new NamespacedEntry(group.getDisplayName().getString(), group.getDisplayName(), (p) -> group.getIconItem(), PackedEntries.create());
        }
    }

    public static class ItemGroupData implements CreativeModeTab.Output {
        public final Set<ItemStack> stacks = ItemStackLinkedSet.createTypeAndComponentsSet();
        public final CreativeModeTab group;
        private final FeatureFlagSet enabledFeatures;
        public final Identifier id;

        public ItemGroupData(Identifier id, CreativeModeTab group, FeatureFlagSet enabledFeatures) {
            this.group = group;
            this.id = id;
            this.enabledFeatures = enabledFeatures;
        }

        public void accept(ItemStack stack, CreativeModeTab.TabVisibility visibility) {
            if (stack.getCount() != 1) {
                throw new IllegalArgumentException("Stack size must be exactly 1");
            } else {
                if (stack.getItem().isEnabled(this.enabledFeatures)) {
                    this.stacks.add(stack);
                }
            }
        }
    }

}
