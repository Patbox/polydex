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
import eu.pb4.polydex.impl.book.view.PotionRecipePage;
import eu.pb4.polydex.impl.book.view.ToolUseOnBlockPage;
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
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.block.entity.BannerBlockEntity;
import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.CreakingEntity;
import net.minecraft.item.*;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.registry.Registries;
import net.minecraft.command.argument.ItemStringReader;
import net.minecraft.entity.LivingEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Nameable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
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
    public static final HashMap<Class<?>, Function<RecipeEntry<?>, PolydexPage>> RECIPE_VIEWS = new HashMap<>();
    public static final List<PolydexPage.EntryModifier> ENTRY_MODIFIERS = new ArrayList<>();
    public static final List<PolydexPage.PageCreator> PAGE_CREATORS = new ArrayList<>();
    public static final String ID = "polydex";
    public static final Map<Item, Function<Item, @Nullable Collection<PolydexEntry>>> ITEM_ENTRY_BUILDERS = new HashMap<>();
    public static final List<BiConsumer<MinecraftServer, PolydexEntry.EntryConsumer>> ENTRY_PROVIDERS = new ArrayList<>();
    public static final PackedEntries ITEM_ENTRIES = PackedEntries.create();
    public static final Map<String, NamespacedEntry> BY_NAMESPACE = new HashMap<>();
    public static final Map<ItemGroup, NamespacedEntry> BY_ITEMGROUP = new HashMap<>();
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
    public static final Map<Identifier, List<CustomPage.ViewData>> CUSTOM_PAGES = new HashMap<>();
    public static final Map<Identifier, PolydexCategory> CATEGORY_BY_ID = new HashMap<>();
    public static final Map<Item, Function<ItemStack, @Nullable PolydexEntry>> ITEM_ENTRY_CREATOR = new HashMap<>();
    private static final Comparator<PolydexPage> PAGE_SORT = Comparator.<PolydexPage>comparingInt((x) -> -x.priority()).thenComparing(PolydexPage::sortingId).thenComparing(PolydexPage::identifier);
    private static final Map<String, Text> MOD_NAMES = new HashMap<>();
    public static Codec<ItemStack> ITEM_STACK_CODEC = ItemStack.VALIDATED_CODEC;
    public static Codec<Text> TEXT = Codec.either(Codec.STRING, TextCodecs.CODEC)
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
        return Identifier.of(ID, path);
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
            PolydexPageUtils.AFTER_PAGE_LOADING.invoker().accept(server);
        }, server);
    }

    private static List<ItemGroupData> getItemGroupEntries(MinecraftServer server) {
        var list = new ArrayList<ItemGroupData>();
        var context = new ItemGroup.DisplayContext(server.getOverworld().getEnabledFeatures(), false, server.getRegistryManager());
        for (var group : Registries.ITEM_GROUP) {
            var data = new ItemGroupData(Registries.ITEM_GROUP.getId(group), group, context.enabledFeatures());
            ((ItemGroupAccessor) group).getEntryCollector().accept(context, data);
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
                var data = new ItemGroupData(PolymerItemGroupUtils.REGISTRY.getId(group), group, context.enabledFeatures());
                ((ItemGroupAccessor) group).getEntryCollector().accept(context, data);
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

    public static void callItemGroupEvents(Identifier id, ItemGroup itemGroup, List<ItemStack> parentTabStacks, List<ItemStack> searchTabStacks, ItemGroup.DisplayContext context) {
        FabricItemGroupEntries fabricCollector = new FabricItemGroupEntries(context, parentTabStacks, searchTabStacks);
        try {
            ItemGroupEvents.modifyEntriesEvent(RegistryKey.of(RegistryKeys.ITEM_GROUP, id)).invoker().modifyEntries(fabricCollector);
        } catch (Throwable ignored) {}

        try {
            ItemGroupEvents.MODIFY_ENTRIES_ALL.invoker().modifyEntries(itemGroup, fabricCollector);
        } catch (Throwable e) {

        }
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

        var polydexEntries = new ObjectLinkedOpenHashSet<PolydexEntry>();
        var consumer = new PolydexEntry.EntryConsumer() {
            @Override
            public void accept(PolydexEntry entry) {
                polydexEntries.add(entry);
            }

            @Override
            public void accept(PolydexEntry entry, ItemGroup group) {
                BY_ITEMGROUP.computeIfAbsent(group, NamespacedEntry::ofItemGroup).entries.add(entry);
                polydexEntries.add(entry);
            }

            @Override
            public void acceptAll(Collection<PolydexEntry> entries) {
                polydexEntries.addAll(entries);
            }

            @Override
            public void acceptAll(Collection<PolydexEntry> entries, ItemGroup group) {
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

        config = PolydexConfigImpl.loadOrCreateConfig(server.getRegistryManager());
    }

    public static void defaultEntries(MinecraftServer server, PolydexEntry.EntryConsumer consumer) {
        var allGroups = getItemGroupEntries(server);

        for (var group : allGroups) {
            if (group.group.getType() == ItemGroup.Type.CATEGORY) {
                var groupEntries = new ObjectLinkedOpenHashSet<PolydexEntry>();
                for (var item : group.stacks) {
                    if (item.isIn(ConventionalItemTags.HIDDEN_FROM_RECIPE_VIEWERS)) {
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

        for (var item : Registries.ITEM) {
            //noinspection deprecation
            if (item == Items.AIR || item.getRegistryEntry().isIn(ConventionalItemTags.HIDDEN_FROM_RECIPE_VIEWERS)) {
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
        var itemRecipes = ((BrewingRecipeRegistryAccessor) server.getBrewingRecipeRegistry()).getItemRecipes();
        var potionRecipes = ((BrewingRecipeRegistryAccessor) server.getBrewingRecipeRegistry()).getPotionRecipes();

        for (var recipe : itemRecipes) {
            consumer.accept(new PotionRecipePage.ItemBase(Identifier.of("minecraft:brewing/item/"
                    + recipe.from().getKey().get().getValue().toUnderscoreSeparatedString() + "/" + recipe.to().getKey().get().getValue().toUnderscoreSeparatedString()), recipe));
        }

        for (var recipe : potionRecipes) {
            consumer.accept(new PotionRecipePage.PotionBase(Identifier.of("minecraft:brewing/potion/"
                    + recipe.from().getKey().get().getValue().toUnderscoreSeparatedString() + "/" + recipe.to().getKey().get().getValue().toUnderscoreSeparatedString()),
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
                    if (entity instanceof CreakingEntity creaking && creaking.getHomePos() != null) {
                        displayBuilder.setComponent(HoverDisplayBuilder.HEALTH, Text.literal("").append(Text.literal("❤ ").formatted(Formatting.RED))
                                .append("??")
                                .append(Text.literal("/").formatted(Formatting.GRAY))
                                .append("??"));
                    } else {
                        displayBuilder.setComponent(HoverDisplayBuilder.HEALTH, Text.literal("").append(Text.literal("❤ ").formatted(Formatting.RED))
                                .append("" + Math.min(MathHelper.ceil(livingEntity.getHealth()), MathHelper.ceil(livingEntity.getMaxHealth())))
                                .append(PolydexImpl.config.displayEntityAbsorption && livingEntity.getAbsorptionAmount() > 0
                                        ? Text.literal("+" + MathHelper.ceil(Math.min(livingEntity.getAbsorptionAmount(), livingEntity.getMaxAbsorption()))).formatted(Formatting.YELLOW) : Text.empty())
                                .append(Text.literal("/").formatted(Formatting.GRAY))
                                .append("" + MathHelper.ceil(livingEntity.getMaxHealth())));
                    }

                    var value = livingEntity.getAttributeValue(EntityAttributes.ARMOR);

                    if (value > 0) {
                        displayBuilder.setComponent(HoverDisplayBuilder.ARMOR, Text.literal("").append(Text.literal("\uD83D\uDEE1 ").formatted(Formatting.GRAY))
                                .append("" + MathHelper.ceil(value)));
                    }
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
                        effects.add(Text.literal("⚗").setStyle(net.minecraft.text.Style.EMPTY.withColor(effect.getEffectType().value().getColor())));
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
                } else if (!(nameable instanceof BannerBlockEntity)) {
                    displayBuilder.setComponent(HoverDisplayBuilder.NAME, nameable.getDisplayName());
                } else {
                    displayBuilder.setComponent(HoverDisplayBuilder.NAME, target.blockState().getBlock().getName());
                }
            } else if (target.blockEntity() instanceof SkullBlockEntity skull) {
                var owner = skull.getOwner();
                if (owner != null && owner.name().isPresent()) {
                    displayBuilder.setComponent(HoverDisplayBuilder.NAME, Text.translatable("block.minecraft.player_head.named", owner.name().get()));
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
        var ench = EnchantmentHelper.getEnchantments(stack);
        var string = new StringBuilder();

        ench.getEnchantments().forEach((e) -> {
            string.append(e.getKey().get().getValue().toUnderscoreSeparatedString()).append("/");
        });


        var baseId = Registries.ITEM.getId(stack.getItem());

        return PolydexEntry.of(baseId.withSuffixedPath("//" + string), stack);
    }

    public static PolydexEntry seperateCustomPotion(ItemStack stack) {
        var potion = stack.getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT);
        var baseId = Registries.ITEM.getId(stack.getItem());
        return PolydexEntry.of(baseId.withSuffixedPath("/" + potion.potion().get().getKey().map(RegistryKey::getValue).orElse(Identifier.of("unknown")).toUnderscoreSeparatedString()), stack);
    }

    public static void blockInteractions(MinecraftServer server, Consumer<PolydexPage> polydexPageConsumer) {
        var axes = PolydexIngredient.of(Ingredient.fromTag(ItemTags.AXES));
        var shovels = PolydexIngredient.of(Ingredient.fromTag(ItemTags.SHOVELS));
        var hoes = PolydexIngredient.of(Ingredient.fromTag(ItemTags.HOES));
        {
            var map = new Reference2ObjectOpenHashMap<Item, LinkedHashSet<Item>>();
            for (var entry : AxeItemAccessor.getSTRIPPED_BLOCKS().entrySet()) {
                var a = entry.getKey().asItem();
                var b = entry.getValue().asItem();
                if (a != null && b != null) {
                    map.computeIfAbsent(b, (c) -> new LinkedHashSet<>()).add(a);
                }
            }

            for (var entry : map.entrySet()) {
                polydexPageConsumer.accept(new ToolUseOnBlockPage(Identifier.of("stripping/"
                        + Registries.ITEM.getId(entry.getKey()).toShortTranslationKey() + "/" + Registries.ITEM.getId(entry.getValue().getFirst()).toShortTranslationKey()),
                        axes, PolydexIngredient.of(Ingredient.ofItems(entry.getValue().toArray(new ItemConvertible[0]))), PolydexStack.of(entry.getKey())));
            }
        }
        {
            var map = new Reference2ObjectOpenHashMap<Item, LinkedHashSet<Item>>();
            for (var entry : ShovelItemAccessor.getPATH_STATES().entrySet()) {
                var a = entry.getKey().asItem();
                var b = entry.getValue().getBlock().asItem();
                if (a != null && b != null) {
                    map.computeIfAbsent(b, (c) -> new LinkedHashSet<>()).add(a);
                }

            }

            for (var entry : map.entrySet()) {
                polydexPageConsumer.accept(new ToolUseOnBlockPage(Identifier.of("flattening/"
                        + Registries.ITEM.getId(entry.getKey()).toShortTranslationKey() + "/" + Registries.ITEM.getId(entry.getValue().getFirst()).toShortTranslationKey()),
                        shovels, PolydexIngredient.of(Ingredient.ofItems(entry.getValue().toArray(new ItemConvertible[0]))), PolydexStack.of(entry.getKey())));
            }
        }

        try {
            var world = new FakeWorld(server) {
                BlockState state = Blocks.AIR.getDefaultState();
                @Override
                public boolean setBlockState(BlockPos pos, BlockState state, int flags, int maxUpdateDepth) {
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
            var placement = new AutomaticItemPlacementContext(world, BlockPos.ORIGIN, Direction.DOWN, Items.DIAMOND_HOE.getDefaultStack(), Direction.DOWN);
            var map = new Reference2ObjectOpenHashMap<Item, LinkedHashSet<Item>>();

            for (var entry : HoeItemAccessor.getTILLING_ACTIONS().entrySet()) {
                try {
                    var a = entry.getKey().asItem();
                    world.state = entry.getKey().getDefaultState();
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
                polydexPageConsumer.accept(new ToolUseOnBlockPage(Identifier.of("tilling/"
                        + Registries.ITEM.getId(entry.getKey()).toShortTranslationKey() + "/" + Registries.ITEM.getId(entry.getValue().getFirst()).toShortTranslationKey()),
                        hoes, PolydexIngredient.of(Ingredient.ofItems(entry.getValue().toArray(new ItemConvertible[0]))), PolydexStack.of(entry.getKey())));
            }
        } catch (Throwable e) {

        }


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
            if (!entry.outputPages().isEmpty() || !entry.ingredientPages().isEmpty()) {
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

    public record NamespacedEntry(String namespace, Text display, Function<ServerPlayerEntity, ItemStack> icon,
                                  PackedEntries entries) {
        public static NamespacedEntry ofMod(String namespace, MinecraftServer server) {
            return ofMod(namespace, PackedEntries.create(), server);
        }

        public static NamespacedEntry ofMod(String namespace, ItemStack defaultIcon, MinecraftServer server) {
            return ofMod(namespace, (p) -> defaultIcon, server);
        }

        public static NamespacedEntry ofMod(String namespace, Function<ServerPlayerEntity, ItemStack> defaultIcon, MinecraftServer server) {
            return ofMod(namespace, PackedEntries.create(), defaultIcon, server);
        }

        public static NamespacedEntry ofMod(String namespace, PackedEntries entries, MinecraftServer server) {
            ItemStack icon = Items.BOOK.getDefaultStack();

            {
                var id = Registries.ITEM.getIds().stream().filter((idx) -> idx.getNamespace().equals(namespace)).findFirst();

                if (id.isPresent()) {
                    icon = Registries.ITEM.get(id.get()).getDefaultStack();
                }
            }

            ItemStack finalIcon = icon;
            return ofMod(namespace, entries, (p) -> finalIcon, server);
        }

        public static NamespacedEntry ofMod(String namespace, PackedEntries entries, Function<ServerPlayerEntity, ItemStack> defaultIcon, MinecraftServer server) {
            var icon = defaultIcon;
            for (var mod : FabricLoader.getInstance().getAllMods()) {
                try {
                    var val = mod.getMetadata().getCustomValue("polydex:entry/" + namespace);
                    if (val != null && val.getType() != CustomValue.CvType.NULL) {
                        var obj = val.getAsObject();
                        Text display;
                        if (obj.containsKey("name")) {
                            display = PARSER.parseText(obj.get("name").getAsString(), ParserContext.of());
                        } else {
                            display = Text.literal(mod.getMetadata().getName());
                        }

                        if (obj.containsKey("icon")) {
                            try {
                                var itemStringReader = new ItemStringReader(server.getRegistryManager()).consume(new StringReader(obj.get("icon").getAsString()));

                                var iconStack = itemStringReader.item().value().getDefaultStack();
                                if (itemStringReader.components() != null) {
                                    iconStack.applyChanges(itemStringReader.components());
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

    public static class ItemGroupData implements ItemGroup.Entries {
        public final Set<ItemStack> stacks = ItemStackSet.create();
        public final ItemGroup group;
        private final FeatureSet enabledFeatures;
        public final Identifier id;

        public ItemGroupData(Identifier id, ItemGroup group, FeatureSet enabledFeatures) {
            this.group = group;
            this.id = id;
            this.enabledFeatures = enabledFeatures;
        }

        public void add(ItemStack stack, ItemGroup.StackVisibility visibility) {
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
