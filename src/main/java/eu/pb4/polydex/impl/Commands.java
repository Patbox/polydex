package eu.pb4.polydex.impl;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import eu.pb4.polydex.api.v1.hover.HoverDisplay;
import eu.pb4.polydex.api.v1.recipe.PolydexPage;
import eu.pb4.polydex.api.v1.recipe.PolydexPageUtils;
import eu.pb4.polydex.impl.book.ui.CategoryPageViewerGui;
import eu.pb4.polydex.impl.book.ui.MainIndexGui;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class Commands {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(literal("polydex")
                .executes((ctx) -> Commands.openIndex(ctx, 0))
                .then(literal("display")
                        .requires(Permissions.require("polydex.display", 0).and((ctx) -> PolydexImpl.config.displayEnabled))
                        .then(argument("style", IdentifierArgumentType.identifier())
                                .suggests((context, builder) -> CommandSource.suggestIdentifiers(PolydexImpl.DISPLAYS.keySet(), builder))
                                .executes(Commands::changeStyle)
                        )
                        
                )
                .then(literal("page")
                        .requires(Permissions.require("polydex.page", 0))
                        .then(argument("number", IntegerArgumentType.integer(1))
                                .executes((ctx) -> Commands.openIndex(ctx, (IntegerArgumentType.getInteger(ctx, "number") - 1)))
                        )
                )
                .then(literal("category")
                        .requires(Permissions.require("polydex.category", 0))
                        .then(argument("category", IdentifierArgumentType.identifier())
                                .suggests((context, builder) -> {
                                    for (var id : PolydexImpl.CATEGORY_BY_ID.keySet()) {
                                        if (id.toString().startsWith(builder.getRemaining()) || id.getPath().startsWith(builder.getRemaining())) {
                                            builder.suggest(id.toString());
                                        }
                                    }

                                    return builder.buildFuture();
                                })
                                .executes(Commands::openCategory)
                        )
                )
                .then(literal("reload")
                        .requires(Permissions.require("polydex.reload", 3))
                        .executes(Commands::reload)
                )
                .then(literal("about").executes(Commands::about))
        );
    }

    private static int openCategory(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        var id = IdentifierArgumentType.getIdentifier(context, "category");

        var category = PolydexImpl.CATEGORY_BY_ID.get(id);

        if (category != null) {
            var player = context.getSource().getPlayerOrThrow();
            var pages = PolydexPageUtils.getPagesForCategory(category);

            List<PolydexPage> list = new ArrayList<>();
            for (PolydexPage polydexPage : pages) {
                if (polydexPage.canDisplay(null, player)) {
                    list.add(polydexPage);
                }
            }

            if (!list.isEmpty()) {
                new CategoryPageViewerGui(player, category, list, null).open();
            }
        }
        return 0;
    }

    private static int reload(CommandContext<ServerCommandSource> context) {
        PolydexImpl.config = PolydexConfig.loadOrCreateConfig();
        context.getSource().sendFeedback(() -> Text.translatable("text.polydex.config_reloaded"), false);
        return 1;
    }

    private static int openIndex(CommandContext<ServerCommandSource> context, int page) throws CommandSyntaxException {
        try {
            new MainIndexGui(context.getSource().getPlayer(), true, page, 0).open();
        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        }
        return 1;
    }

    private static int about(CommandContext<ServerCommandSource> context) {
        for (var text : context.getSource().getEntity() instanceof ServerPlayerEntity ? GenericModInfo.getAboutFull() : GenericModInfo.getAboutConsole()) {
            context.getSource().sendFeedback(() -> text, false);
        }

        return 1;
    }

    private static int changeStyle(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        var id = IdentifierArgumentType.getIdentifier(context, "style");

        if (PolydexImpl.DISPLAYS.containsKey(id)) {
            HoverDisplay.set(context.getSource().getPlayer(), id);
            context.getSource().sendFeedback(() -> Text.translatable("text.polydex.changed_style", id.toString()), false);
            return 1;
        } else {
            context.getSource().sendFeedback(() -> Text.translatable("text.polydex.invalid_style", id.toString()).formatted(Formatting.RED), false);
            return 0;
        }
    }
}
