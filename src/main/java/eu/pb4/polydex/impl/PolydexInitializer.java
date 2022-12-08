package eu.pb4.polydex.impl;

import eu.pb4.polydex.api.DisplayBuilder;
import eu.pb4.polydex.api.ItemEntry;
import eu.pb4.polydex.api.ItemPageView;
import eu.pb4.polydex.api.TargetDisplay;
import eu.pb4.polydex.impl.book.view.AbstractCookingRecipeView;
import eu.pb4.polydex.impl.book.view.CraftingRecipeView;
import eu.pb4.polydex.impl.book.view.SmithingRecipeView;
import eu.pb4.polydex.impl.book.view.StonecuttingRecipeView;
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
import net.minecraft.recipe.RecipeType;
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

        ItemPageView.registerRecipe(RecipeType.CRAFTING, new CraftingRecipeView());
        ItemPageView.registerRecipe(RecipeType.BLASTING, new AbstractCookingRecipeView(Items.BLAST_FURNACE));
        ItemPageView.registerRecipe(RecipeType.SMELTING, new AbstractCookingRecipeView(Items.FURNACE));
        ItemPageView.registerRecipe(RecipeType.CAMPFIRE_COOKING, new AbstractCookingRecipeView(Items.CAMPFIRE));
        ItemPageView.registerRecipe(RecipeType.SMOKING, new AbstractCookingRecipeView(Items.SMOKER));
        ItemPageView.registerRecipe(RecipeType.SMITHING, new SmithingRecipeView());
        ItemPageView.registerRecipe(RecipeType.STONECUTTING, new StonecuttingRecipeView());
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
