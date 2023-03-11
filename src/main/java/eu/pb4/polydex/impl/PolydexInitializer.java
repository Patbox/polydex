package eu.pb4.polydex.impl;

import eu.pb4.polydex.api.DisplayBuilder;
import eu.pb4.polydex.api.ItemPageView;
import eu.pb4.polydex.api.TargetDisplay;
import eu.pb4.polydex.impl.book.view.*;
import eu.pb4.polydex.impl.book.view.crafting.ShapedCraftingRecipeView;
import eu.pb4.polydex.impl.book.view.crafting.ShapelessCraftingRecipeView;
import eu.pb4.polydex.impl.book.view.smithing.LegacySmithingRecipeView;
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
        TargetDisplay.register(id("disabled"), NoopTargetDisplay::create);
        TargetDisplay.register(id("bossbar"), BossbarTargetDisplay::targetted);
        TargetDisplay.register(id("bossbar_always"), BossbarTargetDisplay::always);
        TargetDisplay.register(id("bossbar_sneak"), BossbarTargetDisplay::sneaking);
        TargetDisplay.register(id("sidebar"), SidebarTargetDisplay::new);

        ItemPageView.register(PolydexImpl::buildRecipes);
        ItemPageView.register(PolydexImpl::potionRecipe);

        ItemPageView.registerRecipeViewer(ShapedRecipe.class, new ShapedCraftingRecipeView());
        ItemPageView.registerRecipeViewer(ShapelessRecipe.class, new ShapelessCraftingRecipeView());
        ItemPageView.registerRecipeViewer(BlastingRecipe.class, new AbstractCookingRecipeView(Items.BLAST_FURNACE));
        ItemPageView.registerRecipeViewer(SmeltingRecipe.class, new AbstractCookingRecipeView(Items.FURNACE));
        ItemPageView.registerRecipeViewer(CampfireCookingRecipe.class, new AbstractCookingRecipeView(Items.CAMPFIRE));
        ItemPageView.registerRecipeViewer(SmokingRecipe.class, new AbstractCookingRecipeView(Items.SMOKER));
        ItemPageView.registerRecipeViewer(LegacySmithingRecipe.class, new LegacySmithingRecipeView());
        ItemPageView.registerRecipeViewer(SmithingTrimRecipe.class, new SmithingTrimRecipeView());
        ItemPageView.registerRecipeViewer(SmithingTransformRecipe.class, new SmithingTransformRecipeView());
        ItemPageView.registerRecipeViewer(StonecuttingRecipe.class, new StonecuttingRecipeView());

        ItemPageView.register(PolydexImpl::addCustomPages);

        DisplayBuilder.register(PolydexImpl::defaultBuilder);
    }

    @Override
    public void onInitialize() {
        GenericModInfo.build(FabricLoader.getInstance().getModContainer("polydex").get());
        CommandRegistrationCallback.EVENT.register(Commands::register);
        ServerLifecycleEvents.SERVER_STARTED.register(PolydexImpl::updateCaches);
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, manager, b) -> PolydexImpl.updateCaches(server));
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
