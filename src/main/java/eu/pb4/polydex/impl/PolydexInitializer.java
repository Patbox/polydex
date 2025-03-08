package eu.pb4.polydex.impl;

import eu.pb4.polydex.api.v1.hover.HoverDisplay;
import eu.pb4.polydex.api.v1.hover.HoverDisplayBuilder;
import eu.pb4.polydex.api.v1.recipe.PolydexEntry;
import eu.pb4.polydex.api.v1.recipe.PolydexPage;
import eu.pb4.polydex.impl.book.ui.GuiUtils;
import eu.pb4.polydex.impl.book.view.*;
import eu.pb4.polydex.impl.book.view.crafting.ShapedCraftingRecipePage;
import eu.pb4.polydex.impl.book.view.crafting.ShapelessCraftingRecipePage;
import eu.pb4.polydex.impl.book.view.crafting.ShulkerBoxColoringRecipePage;
import eu.pb4.polydex.impl.book.view.crafting.TippedArrowRecipePage;
import eu.pb4.polydex.impl.book.view.smithing.SmithingTransformRecipeView;
import eu.pb4.polydex.impl.book.view.smithing.SmithingTrimRecipePage;
import eu.pb4.polydex.impl.compat.LibMultiPartCompatibility;
import eu.pb4.polydex.impl.display.BossbarTargetDisplay;
import eu.pb4.polydex.impl.display.NoopTargetDisplay;
import eu.pb4.polydex.impl.display.SidebarTargetDisplay;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.Items;
import net.minecraft.recipe.*;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

import static eu.pb4.polydex.impl.PolydexImpl.id;

public class PolydexInitializer implements ModInitializer {
    private static boolean initialized;

    private static final Identifier EVENT_ID = id("run_reload");

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        HoverDisplay.register(id("disabled"), NoopTargetDisplay::create);
        HoverDisplay.register(id("bossbar"), BossbarTargetDisplay::new);
        HoverDisplay.register(id("sidebar"), SidebarTargetDisplay::new);

        PolydexPage.register(PolydexImpl::potionRecipe);
        PolydexPage.register(PolydexImpl::blockInteractions);

        PolydexPage.registerRecipeViewer(ShapedRecipe.class, ShapedCraftingRecipePage::new);
        PolydexPage.registerRecipeViewer(ShapelessRecipe.class, ShapelessCraftingRecipePage::new);
        PolydexPage.registerRecipeViewer(ShulkerBoxColoringRecipe.class, ShulkerBoxColoringRecipePage::new);
        PolydexPage.registerRecipeViewer(TippedArrowRecipe.class, TippedArrowRecipePage::new);

        PolydexPage.registerRecipeViewer(BlastingRecipe.class, AbstractCookingRecipePage.of(Items.BLAST_FURNACE));
        PolydexPage.registerRecipeViewer(SmeltingRecipe.class, AbstractCookingRecipePage.of(Items.FURNACE));
        PolydexPage.registerRecipeViewer(CampfireCookingRecipe.class, AbstractCookingRecipePage.of(Items.CAMPFIRE));
        PolydexPage.registerRecipeViewer(SmokingRecipe.class, AbstractCookingRecipePage.of(Items.SMOKER));
        PolydexPage.registerRecipeViewer(SmithingTrimRecipe.class, SmithingTrimRecipePage::new);
        PolydexPage.registerRecipeViewer(SmithingTransformRecipe.class, SmithingTransformRecipeView::new);
        PolydexPage.registerRecipeViewer(StonecuttingRecipe.class, StonecuttingRecipePage::new);

        PolydexPage.register(PolydexImpl::addCustomPages);
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            PolydexPage.register(PolydexImpl::addDebugPage);
        }

        PolydexEntry.registerProvider(PolydexImpl::defaultEntries);
        PolydexEntry.registerEntryCreator(Items.ENCHANTED_BOOK, PolydexImpl::seperateCustomEnchantments);
        PolydexEntry.registerEntryCreator(Items.POTION, PolydexImpl::seperateCustomPotion);
        PolydexEntry.registerEntryCreator(Items.SPLASH_POTION, PolydexImpl::seperateCustomPotion);
        PolydexEntry.registerEntryCreator(Items.LINGERING_POTION, PolydexImpl::seperateCustomPotion);
        PolydexEntry.registerEntryCreator(Items.TIPPED_ARROW, PolydexImpl::seperateCustomPotion);

        HoverDisplayBuilder.register(PolydexImpl::defaultBuilder);

        if (FabricLoader.getInstance().isModLoaded("libmultipart")) {
            LibMultiPartCompatibility.register();
        }
    }

    @Override
    public void onInitialize() {
        GenericModInfo.build(FabricLoader.getInstance().getModContainer("polydex2").get());
        CommandRegistrationCallback.EVENT.register(Commands::register);
        ServerLifecycleEvents.SERVER_STARTED.addPhaseOrdering(Event.DEFAULT_PHASE, EVENT_ID);
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.addPhaseOrdering(Event.DEFAULT_PHASE, EVENT_ID);
        ServerLifecycleEvents.SERVER_STARTED.register(EVENT_ID, PolydexImpl::rebuild);
        ServerLifecycleEvents.SERVER_STARTED.register((s) -> CardboardWarning.checkAndAnnounce());
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register(EVENT_ID, (server, manager, b) -> PolydexImpl.rebuild(server));
        ResourceManagerHelper serverData = ResourceManagerHelper.get(ResourceType.SERVER_DATA);
        PolymerResourcePackUtils.addModAssets("polydex2");
        GuiUtils.register();

        serverData.registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public Identifier getFabricId() {
                return Identifier.of(PolydexImpl.ID, "polydex_page");
            }

            @Override
            public void reload(ResourceManager manager) {
                PolydexImpl.onReload(manager);
            }
        });

        init();
    }
}
