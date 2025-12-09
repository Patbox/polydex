package eu.pb4.polydex.impl;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import eu.pb4.polydex.api.v1.hover.HoverDisplay;
import eu.pb4.polydex.api.v1.hover.HoverDisplayBuilder;
import eu.pb4.polydex.api.v1.hover.HoverSettings;
import eu.pb4.polydex.api.v1.recipe.PolydexPageUtils;
import eu.pb4.polydex.impl.book.ui.MainIndexGui;
import eu.pb4.polydex.impl.book.ui.SearchGui;
import eu.pb4.polydex.impl.display.PolydexTargetImpl;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import java.util.List;
import java.util.Locale;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class Commands {
    public static final DynamicCommandExceptionType INVALID_ARGUMENT = new DynamicCommandExceptionType((x) -> Component.translatable("argument.enum.invalid", x));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, net.minecraft.commands.Commands.CommandSelection environment) {
        dispatcher.register(literal("polydex")
                .executes((ctx) -> Commands.openIndex(ctx, -1))
                .then(literal("hover")
                        .requires(Permissions.require("polydex.display", 0).and((ctx) -> PolydexImpl.config.displayEnabled))
                        .then(literal("style")
                                .then(argument("style", IdentifierArgument.id())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(PolydexImpl.DISPLAYS.keySet(), builder))
                                        .executes(Commands::changeStyle)
                                )
                        ).then(literal("information")
                                .then(argument("component", IdentifierArgument.id())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(HoverDisplayBuilder.ComponentType.getAllAllowedIds(), builder))
                                        .then(enumArgument("show", HoverDisplayBuilder.ComponentType.Visibility.values())
                                                .executes(Commands::setComponent)
                                        )
                                )
                        ).then(literal("displaymode")
                                .then(enumArgument("type", HoverSettings.DisplayMode.values())
                                        .executes(Commands::setDisplayMode)
                                )
                        )
                )

                .then(literal("page")
                        .requires(Permissions.require("polydex.page", 0))
                        .then(argument("number", IntegerArgumentType.integer(1))
                                .executes((ctx) -> Commands.openIndex(ctx, (IntegerArgumentType.getInteger(ctx, "number") - 1)))
                        )
                )

                .then(literal("search")
                        .requires(Permissions.require("polydex.search", 0).and(x -> PolydexImpl.config.enableSearch))
                        .executes(ctx -> Commands.openSearch(ctx, ""))
                        .then(argument("query", StringArgumentType.greedyString())
                                .executes(ctx -> Commands.openSearch(ctx, StringArgumentType.getString(ctx, "query"))))

                )

                .then(literal("open_page")
                        .requires(Permissions.require("polydex.open_page", 0))
                        .then(argument("page", IdentifierArgument.id())
                                .suggests((context, builder) -> {
                                    for (var id : PolydexImpl.ID_TO_PAGE.keySet()) {
                                        if (id.toString().startsWith(builder.getRemaining()) || id.getPath().startsWith(builder.getRemaining())) {
                                            builder.suggest(id.toString());
                                        }
                                    }

                                    return builder.buildFuture();
                                })
                                .executes(Commands::openPage)
                        )
                )
                .then(literal("entry_usage")
                        .requires(Permissions.require("polydex.open_entry", 0))
                        .then(argument("entry", IdentifierArgument.id())
                                .suggests((context, builder) -> {
                                    for (var id : PolydexImpl.ITEM_ENTRIES.nonEmptyById().keySet()) {
                                        if (id.toString().startsWith(builder.getRemaining()) || id.getPath().startsWith(builder.getRemaining())) {
                                            builder.suggest(id.toString());
                                        }
                                    }

                                    return builder.buildFuture();
                                })
                                .executes(Commands::openEntryUsages)
                        )
                )
                .then(literal("entry_result")
                        .requires(Permissions.require("polydex.open_entry", 0))
                        .then(argument("entry", IdentifierArgument.id())
                                .suggests((context, builder) -> {
                                    for (var id : PolydexImpl.ITEM_ENTRIES.nonEmptyById().keySet()) {
                                        if (id.toString().startsWith(builder.getRemaining()) || id.getPath().startsWith(builder.getRemaining())) {
                                            builder.suggest(id.toString());
                                        }
                                    }

                                    return builder.buildFuture();
                                })
                                .executes(Commands::openEntryResult)
                        )
                )

                .then(literal("category")
                        .requires(Permissions.require("polydex.category", 0))
                        .then(argument("category", IdentifierArgument.id())
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

    @SuppressWarnings("unchecked")
    private static <T extends ArgumentBuilder<CommandSourceStack, T>, G extends Enum<G>> T enumArgument(String name, G[] values) {
        return (T) argument(name, StringArgumentType.word())
                .suggests((context, builder) -> {
                    String string = builder.getRemaining().toLowerCase(Locale.ROOT);
                    for (var value : values) {
                        if (value.name().toLowerCase(Locale.ROOT).startsWith(string)) {
                            builder.suggest(value.name().toLowerCase(Locale.ROOT));
                        }
                    }
                    return builder.buildFuture();
                });
    }

    private static <T extends Enum<T>> T getEnum(CommandContext<?> context, String name, Class<T> tClass) throws CommandSyntaxException {
        var obj = StringArgumentType.getString(context, name);

        try {
            return Enum.valueOf(tClass, obj.toUpperCase(Locale.ROOT));
        } catch (Throwable e) {
            throw INVALID_ARGUMENT.create(obj);
        }
    }

    private static int openCategory(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var id = IdentifierArgument.getId(context, "category");

        var category = PolydexImpl.CATEGORY_BY_ID.get(id);

        if (category != null) {
            PolydexPageUtils.openCategoryUi(context.getSource().getPlayerOrException(), category, null);
        }
        return 0;
    }

    private static int openPage(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var id = IdentifierArgument.getId(context, "page");

        var page = PolydexImpl.ID_TO_PAGE.get(id);

        if (page != null) {
            PolydexPageUtils.openCustomPageUi(context.getSource().getPlayerOrException(), Component.translatable("text.polydex.recipes_title_custom"), List.of(page), true, null);
        }
        return 1;
    }

    private static int openEntryResult(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var id = IdentifierArgument.getId(context, "entry");

        var entry = PolydexImpl.ITEM_ENTRIES.nonEmptyById().get(id);

        if (entry != null) {
            PolydexPageUtils.openRecipeListUi(context.getSource().getPlayerOrException(), entry, null);
        }
        return 1;
    }

    private static int openEntryUsages(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var id = IdentifierArgument.getId(context, "entry");

        var entry = PolydexImpl.ITEM_ENTRIES.nonEmptyById().get(id);

        if (entry != null) {
            PolydexPageUtils.openUsagesListUi(context.getSource().getPlayerOrException(), entry, null);
        }
        return 1;
    }

    private static int reload(CommandContext<CommandSourceStack> context) {
        PolydexImpl.config = PolydexConfigImpl.loadOrCreateConfig(context.getSource().registryAccess());
        context.getSource().sendSuccess(() -> Component.translatable("text.polydex.config_reloaded"), false);
        return 1;
    }

    private static int openIndex(CommandContext<CommandSourceStack> context, int page) throws CommandSyntaxException {
        try {
            new MainIndexGui(context.getSource().getPlayer(), page).open();
        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        }
        return 1;
    }

    private static int openSearch(CommandContext<CommandSourceStack> context, String query) throws CommandSyntaxException {
        try {
            new SearchGui(context.getSource().getPlayerOrException(), query, null);
        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        }
        return 1;
    }

    private static int about(CommandContext<CommandSourceStack> context) {
        for (var text : context.getSource().getEntity() instanceof ServerPlayer ? GenericModInfo.getAboutFull() : GenericModInfo.getAboutConsole()) {
            context.getSource().sendSuccess(() -> text, false);
        }

        return 1;
    }

    private static int changeStyle(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var id = IdentifierArgument.getId(context, "style");

        if (PolydexImpl.DISPLAYS.containsKey(id)) {
            HoverDisplay.set(context.getSource().getPlayer(), id);
            context.getSource().sendSuccess(() -> Component.translatable("text.polydex.changed_style", id.toString()), false);
            return 1;
        } else {
            context.getSource().sendSuccess(() -> Component.translatable("text.polydex.invalid_style", id.toString()).withStyle(ChatFormatting.RED), false);
            return 0;
        }
    }

    private static int setComponent(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var id = IdentifierArgument.getId(context, "component");
        var value = getEnum(context, "show", HoverDisplayBuilder.ComponentType.Visibility.class);

        if (HoverDisplayBuilder.ComponentType.getAllAllowedIds().contains(id)) {
            PolydexTargetImpl.get(context.getSource().getPlayerOrException()).settings().setComponentVisible(id, value);
            context.getSource().sendSuccess(() -> Component.translatable("text.polydex.changed_component_visibility", id.toString(), value.name().toLowerCase(Locale.ROOT)), false);
            return 1;
        } else {
            context.getSource().sendSuccess(() -> Component.translatable("text.polydex.invalid_component_type", id.toString()).withStyle(ChatFormatting.RED), false);
            return 0;
        }
    }

    private static int setDisplayMode(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var value = getEnum(context, "type", HoverSettings.DisplayMode.class);

        PolydexTargetImpl.get(context.getSource().getPlayerOrException()).settings().setDisplayMode(value);
        context.getSource().sendSuccess(() -> Component.translatable("text.polydex.changed_display_mode", value.name().toLowerCase(Locale.ROOT)), false);
        return 1;

    }
}
