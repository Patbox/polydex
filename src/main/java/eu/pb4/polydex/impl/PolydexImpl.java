package eu.pb4.polydex.impl;

import com.google.gson.JsonParser;
import com.mojang.brigadier.StringReader;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import eu.pb4.placeholders.TextParser;
import eu.pb4.polydex.api.*;
import eu.pb4.polydex.impl.book.view.CustomView;
import eu.pb4.polydex.impl.book.view.PotionRecipeView;
import eu.pb4.polydex.mixin.BrewingRecipeAccessor;
import eu.pb4.polydex.mixin.BrewingRecipeRegistryAccessor;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.CustomValue;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.command.argument.ItemStringReader;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeType;
import net.minecraft.resource.ResourceManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Nameable;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.registry.Registry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.codecs.MoreCodecs;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

public class PolydexImpl {
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
    public static Codec<Text> TEXT = Codec.either(Codec.STRING, MoreCodecs.TEXT)
            .xmap(either -> either.map(TextParser::parseSafe, Function.identity()), Either::right);


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
        BY_NAMESPACE.put("minecraft", new NamespacedEntry("minecraft", new LiteralText("Minecraft (Vanilla)"), Items.GRASS_BLOCK.getDefaultStack(), PackedEntries.create()));
        NAMESPACED_ENTRIES.clear();
        ITEM_GROUP_ENTRIES.clear();

        for (var item : Registry.ITEM) {
            if (item == Items.AIR) {
                continue;
            }

            var entries = new ArrayList<ItemEntry>();

            var func = ITEM_ENTRY_BUILDERS.get(item);
            if (func != null) {
                var custom = func.apply(item);
                if (custom == null) {
                    continue;
                } else {
                    entries.addAll(custom);
                }
            } else {
                entries.add(ItemEntry.of(item));
            }

            var recipes = server.getRecipeManager().values();

            for (var entry : entries) {
                for (var viewBuilder : VIEWS) {
                    var pageEntries = viewBuilder.createEntries(server, entry, recipes);

                    if (pageEntries != null) {
                        entry.pages().addAll(pageEntries);
                    }
                }

                ITEM_ENTRIES.add(entry);
                BY_NAMESPACE.computeIfAbsent(entry.identifier().getNamespace(), NamespacedEntry::ofMod).entries.add(entry);
                if (entry.item().getGroup() != null) {
                    BY_ITEMGROUP.computeIfAbsent(entry.item().getGroup(), NamespacedEntry::ofItemGroup).entries.add(entry);
                }
                PolydexServerInterface.updateTimeReference(server);
            }

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
            if (view != null && recipe.getOutput().getItem() == entry.item()) {
                list.add(new PageEntry<>(view, recipe));
            }
        }

        return list;
    }

    public static Collection<ItemEntry> potionBuilder(Item item) {
        var list = new ArrayList<ItemEntry>();
        for (var potion : Registry.POTION) {
            if (potion != Potions.EMPTY) {
                var id = Registry.POTION.getId(potion);
                list.add(ItemEntry.of(new Identifier(id.getNamespace(), "polydex/potion/" + id.getPath()), item, PotionUtil.setPotion(item.getDefaultStack(), potion)));
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
                    displayBuilder.setComponent(DisplayBuilder.HEALTH, new LiteralText("").append(new LiteralText("♥ ").formatted(Formatting.RED))
                            .append("" + Math.min(MathHelper.ceil(livingEntity.getHealth()), MathHelper.ceil(livingEntity.getMaxHealth())))
                            .append(new LiteralText("/").formatted(Formatting.GRAY))
                            .append("" + MathHelper.ceil(livingEntity.getMaxHealth())));
                }

                if (PolydexImpl.config.displayAdditional) {
                    var effects = new ArrayList<Text>();

                    if (livingEntity.isOnFire()) {
                        effects.add(new LiteralText("\uD83D\uDD25" + (livingEntity.isFrozen() ? " " : "")).formatted(Formatting.GOLD));
                    }

                    if (livingEntity.isFrozen()) {
                        effects.add(new LiteralText("❄").formatted(Formatting.AQUA));
                    }

                    for (var effect : livingEntity.getStatusEffects()) {
                        effects.add(new LiteralText("⚗").setStyle(net.minecraft.text.Style.EMPTY.withColor(effect.getEffectType().getColor())));
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
                var text = new LiteralText("⛏").formatted(Formatting.DARK_RED);
                if (!displayBuilder.isSmall()) {
                    text.append(" ").append(new TranslatableText("text.polydex.cant_mine").formatted(Formatting.RED));
                }
                displayBuilder.setComponent(DisplayBuilder.EFFECTS, text);
            }

            if (config.displayAdditional && target.getBlockEntity() instanceof AbstractFurnaceBlockEntity furnace) {
                displayBuilder.setComponent(DisplayBuilder.INPUT, PolydexUtils.createText(furnace.getStack(0)));
                displayBuilder.setComponent(DisplayBuilder.FUEL, PolydexUtils.createText(furnace.getStack(1)));
                displayBuilder.setComponent(DisplayBuilder.OUTPUT, PolydexUtils.createText(furnace.getStack(2)));
            }

            if (PolydexImpl.config.displayMiningProgress && target.isMining()) {
                displayBuilder.setComponent(DisplayBuilder.PROGRESS, new LiteralText("" + (int) (target.getBreakingProgress() * 100))
                        .append(new LiteralText("%").formatted(Formatting.GRAY)));
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
        var resources = manager.findResources("polydex_page", path -> path.endsWith(".json"));

        for (Identifier path : resources) {
            try {
                var resource = manager.getResource(path);
                try (var reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
                    var json = JsonParser.parseReader(reader);

                    var result = CustomView.ViewData.CODEC.parse(JsonOps.INSTANCE, json);

                    result.result().ifPresent(viewData -> CUSTOM_PAGES.computeIfAbsent(viewData.entryId(), (e) -> new ArrayList<>()).add(viewData));

                    result.error().ifPresent(error -> LOGGER.error("Failed to parse page at {}: {}", path, error.toString()));
                }
            } catch (Exception e) {
                LOGGER.error("Failed to read page at {}", path, e);
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
            if (!entry.pages().isEmpty()) {
                this.nonEmpty.add(entry);
            }
        }
    }

    public record NamespacedEntry(String namespace, Text display, ItemStack icon, PackedEntries entries) {
        public static NamespacedEntry ofMod(String namespace) {
            return ofMod(namespace, PackedEntries.create());
        }

        public static NamespacedEntry ofMod(String namespace, PackedEntries entries) {
            ItemStack icon = Items.BOOK.getDefaultStack();

            {
                var id = Registry.ITEM.getIds().stream().filter((idx) -> idx.getNamespace().equals(namespace)).findFirst();

                if (id.isPresent()) {
                    icon = Registry.ITEM.get(id.get()).getDefaultStack();
                }
            }

            for (var mod : FabricLoader.getInstance().getAllMods()) {
                try {
                    var val = mod.getMetadata().getCustomValue("polydex:entry/" + namespace);
                    if (val != null && val.getType() != CustomValue.CvType.NULL) {
                        var obj = val.getAsObject();
                        Text display;
                        if (obj.containsKey("name")) {
                            display = TextParser.parse(obj.get("name").getAsString());
                        } else {
                            display = new LiteralText(mod.getMetadata().getName());
                        }

                        if (obj.containsKey("icon")) {
                            try {
                                var itemStringReader = (new ItemStringReader(new StringReader(obj.get("icon").getAsString()), true)).consume();

                                icon = itemStringReader.getItem().getDefaultStack();
                                if (itemStringReader.getNbt() != null) {
                                    icon.setNbt(itemStringReader.getNbt());
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
                    return new NamespacedEntry(namespace, new LiteralText(mod.getMetadata().getName()), icon, entries);
                }
            }

            return new NamespacedEntry(namespace, new LiteralText(namespace), icon, entries);
        }

        public static NamespacedEntry ofItemGroup(ItemGroup group) {
            return new NamespacedEntry(group.getName(), group.getDisplayName(), group.getIcon().copy(), PackedEntries.create());
        }
    }
}
