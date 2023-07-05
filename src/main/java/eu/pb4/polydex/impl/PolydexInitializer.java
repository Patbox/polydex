package eu.pb4.polydex.impl;

import eu.pb4.polydex.api.hover.HoverDisplay;
import eu.pb4.polydex.api.hover.HoverDisplayBuilder;
import eu.pb4.polydex.api.PageView;
import eu.pb4.polydex.impl.book.view.*;
import eu.pb4.polydex.impl.book.view.crafting.AbstractCraftingRecipeView;
import eu.pb4.polydex.impl.book.view.crafting.ShapedCraftingRecipeView;
import eu.pb4.polydex.impl.book.view.crafting.ShapelessCraftingRecipeView;
import eu.pb4.polydex.impl.book.view.crafting.ShulkerBoxColoringRecipeView;
import eu.pb4.polydex.impl.book.view.smithing.SmithingTransformRecipeView;
import eu.pb4.polydex.impl.book.view.smithing.SmithingTrimRecipeView;
import eu.pb4.polydex.impl.display.BossbarTargetDisplay;
import eu.pb4.polydex.impl.display.NoopTargetDisplay;
import eu.pb4.polydex.impl.display.SidebarTargetDisplay;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
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

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        HoverDisplay.register(id("disabled"), NoopTargetDisplay::create);
        HoverDisplay.register(id("bossbar"), BossbarTargetDisplay::targetted);
        HoverDisplay.register(id("bossbar_always"), BossbarTargetDisplay::always);
        HoverDisplay.register(id("bossbar_sneak"), BossbarTargetDisplay::sneaking);
        HoverDisplay.register(id("sidebar"), SidebarTargetDisplay::new);

        PageView.register(PolydexImpl::buildRecipes);
        PageView.register(PolydexImpl::potionRecipe);

        PageView.registerRecipeViewer(ShapedRecipe.class, new ShapedCraftingRecipeView());
        PageView.registerRecipeViewer(ShapelessRecipe.class, new ShapelessCraftingRecipeView());
        PageView.registerRecipeViewer(ShulkerBoxColoringRecipe.class, new ShulkerBoxColoringRecipeView());

        PageView.registerRecipeViewer(BlastingRecipe.class, new AbstractCookingRecipeView(Items.BLAST_FURNACE));
        PageView.registerRecipeViewer(SmeltingRecipe.class, new AbstractCookingRecipeView(Items.FURNACE));
        PageView.registerRecipeViewer(CampfireCookingRecipe.class, new AbstractCookingRecipeView(Items.CAMPFIRE));
        PageView.registerRecipeViewer(SmokingRecipe.class, new AbstractCookingRecipeView(Items.SMOKER));
        PageView.registerRecipeViewer(SmithingTrimRecipe.class, new SmithingTrimRecipeView());
        PageView.registerRecipeViewer(SmithingTransformRecipe.class, new SmithingTransformRecipeView());
        PageView.registerRecipeViewer(StonecuttingRecipe.class, new StonecuttingRecipeView());

        PageView.register(PolydexImpl::addCustomPages);

        HoverDisplayBuilder.register(PolydexImpl::defaultBuilder);
    }

    @Override
    public void onInitialize() {
        GenericModInfo.build(FabricLoader.getInstance().getModContainer("polydex2").get());
        CommandRegistrationCallback.EVENT.register(Commands::register);
        ServerLifecycleEvents.SERVER_STARTED.register(PolydexImpl::rebuild);
        ServerLifecycleEvents.SERVER_STARTED.register((s) -> CardboardWarning.checkAndAnnounce());
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, manager, b) -> PolydexImpl.rebuild(server));
        ResourceManagerHelper serverData = ResourceManagerHelper.get(ResourceType.SERVER_DATA);

        serverData.registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public Identifier getFabricId() {
                return new Identifier(PolydexImpl.ID, "polydex_page");
            }

            @Override
            public void reload(ResourceManager manager) {
                PolydexImpl.onReload(manager);
            }
        });


        init();
    }
}
