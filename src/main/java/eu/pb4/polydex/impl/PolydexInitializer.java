package eu.pb4.polydex.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import eu.pb4.polydex.api.*;
import eu.pb4.polydex.impl.display.BossbarTargetDisplay;
import eu.pb4.polydex.impl.display.NoopTargetDisplay;
import eu.pb4.polydex.impl.display.SidebarTargetDisplay;
import eu.pb4.polydex.impl.book.view.CraftingRecipeView;
import eu.pb4.polydex.impl.book.view.AbstractCookingRecipeView;
import eu.pb4.polydex.impl.book.view.SmithingRecipeView;
import eu.pb4.polydex.impl.book.view.StonecuttingRecipeView;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.Items;
import net.minecraft.recipe.RecipeType;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collection;
import java.util.function.BiFunction;

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

        ItemEntry.registerBuilder(Items.POTION, PolydexImpl::potionBuilder);
        ItemEntry.registerBuilder(Items.SPLASH_POTION, PolydexImpl::potionBuilder);
        ItemEntry.registerBuilder(Items.LINGERING_POTION, PolydexImpl::potionBuilder);

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
