package eu.pb4.polydex.impl.book.ui;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import eu.pb4.polydex.impl.PlayerInterface;
import eu.pb4.polydex.impl.search.SearchQuery;
import eu.pb4.polydex.impl.search.SearchResult;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.AnvilInputGui;
import it.unimi.dsi.fastutil.objects.ReferenceSortedSets;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class SearchGui extends AnvilInputGui implements PageAware {
    private static final int PAGE_SIZE = 9 * 3;
    private final MainIndexState state;
    private final Runnable openPrevious;

    private int page = 0;
    @Nullable
    private CompletableFuture<SearchResult> searching = null;
    private SearchResult entries;
    private int searchDelayTimer = -1;
    private String currentInput = "";
    private int searchTime = -1;
    private boolean hasSearchIcon = false;

    public SearchGui(ServerPlayer player, String search, @Nullable Runnable openPrevious) {
        super(player, true);
        this.setTitle(ExtendedGui.formatTexturedTextAnvil(player, Component.literal("o"), Component.translatable("text.polydex.recipes_title_search")));
        this.setSlot(0, GuiUtils.fillerStack(player));
        this.setSlot(1, GuiUtils.fillerStack(player));
        this.setSlot(2, GuiUtils.fillerStack(player));
        this.state = ((PlayerInterface) player.connection).polydex_mainIndexState();
        this.openPrevious = openPrevious;
        if (search.isEmpty()) {
            this.entries = SearchResult.global();
            this.updateDisplay();
        } else {
            this.entries = SearchResult.EMPTY;
            this.onInput(search);
        }
        this.open();
    }

    public void updateSearching() {
        if (this.searching == null && this.searchDelayTimer < 0) {
            if (hasSearchIcon) {
                this.setSlot(1, GuiUtils.fillerStack(player));
                if (this.screenHandler != null) {
                    this.screenHandler.setRemoteSlot(2, ItemStack.EMPTY);
                }
            }
            hasSearchIcon = false;
            return;
        }

        this.setSlot(1, new GuiElementBuilder(Items.COMPASS)
                .noDefaults()
                .setComponent(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(Optional.empty(), true))
                .glow(false)
                .hideDefaultTooltip()
                .setName(Component.translatable("text.polydex.searching").append(".".repeat(this.searchTime / 5))));

        if (!hasSearchIcon && this.screenHandler != null) {
            this.screenHandler.setRemoteSlot(2, ItemStack.EMPTY);
        }
        hasSearchIcon = true;
    }

    @Override
    public void onInput(String input) {
        super.onInput(input);
        if (this.screenHandler != null) {
            this.screenHandler.setRemoteSlot(2, ItemStack.EMPTY);
        }


        var itemStack = GuiUtils.fillerStack(player).getItemStack().copy();
        itemStack.set(DataComponents.CUSTOM_NAME, Component.literal(input));
        itemStack.set(DataComponents.TOOLTIP_DISPLAY, new TooltipDisplay(true, ReferenceSortedSets.emptySet()));
        this.setSlot(0, itemStack, Objects.requireNonNull(this.getSlot(0)).getGuiCallback());
        if (this.screenHandler != null) {
            this.screenHandler.setRemoteSlot(0, itemStack.copy());
        }
        if (!input.isEmpty() && input.equals(this.currentInput)) {
            return;
        }
        this.currentInput = "";
        if (this.searching != null) {
            this.searching.cancel(true);
            this.searching = null;
        }
        this.searchTime = 0;

        if (input.isEmpty()) {
            this.entries = SearchResult.global();
            this.searchDelayTimer = -1;
        } else {
            this.entries = SearchResult.EMPTY;
            this.searchDelayTimer = 8;
        }
        this.updateDisplay();
    }

    @Override
    public void onTick() {
        if (this.searching != null) {
            this.searchTime++;
        }
        this.updateSearching();
        if (--this.searchDelayTimer == 0) {
            if (this.getInput().equals(this.currentInput)) {
                return;
            }
            this.currentInput = this.getInput();
            if (this.searching != null && !this.searching.isDone()) {
                this.searching.cancel(true);
                this.searching = null;
            }

            try {
                var query = SearchQuery.parse(this.getInput());
                this.searching = SearchResult.getAsync(query, player);
                this.searching.thenAcceptAsync((searchResult -> {
                    this.entries = searchResult;
                    this.searching = null;
                    this.searchTime = 0;
                    this.setPage(0);
                }), this.getPlayer().level().getServer());
            } catch (CommandSyntaxException e) {
                return;
            }
        }
        super.onTick();
    }

    @Override
    public void onClose() {
        if (this.searching != null) {
            this.searching.cancel(true);
            this.searching = null;
        }
        super.onClose();
    }

    @Override
    public void setDefaultInputValue(String input) {
        super.setDefaultInputValue(input);
        var itemStack = GuiUtils.fillerStack(player).getItemStack().copy();
        itemStack.set(DataComponents.CUSTOM_NAME, Component.literal(input));
        itemStack.set(DataComponents.TOOLTIP_DISPLAY, new TooltipDisplay(true, ReferenceSortedSets.emptySet()));
        this.setSlot(0, itemStack, Objects.requireNonNull(this.getSlot(0)).getGuiCallback());
    }

    public void updateDisplay() {
        this.updateSearching();
        {
            var entries = this.entries.get(this.state.showAll);
            int i = 0;
            int offset = this.page * PAGE_SIZE;
            int size = Math.min(PAGE_SIZE, entries.size() - offset);
            for (; i < size; i++) {
                var entry = entries.get(i + offset);
                var b = GuiElementBuilder.from(entry.stack().toDisplayItemStack(player))
                        .setCallback((x, type, z) -> {
                            if ((type.isLeft && entry.getVisiblePagesSize(player) > 0) || (type.isRight && entry.getVisibleIngredientPagesSize(player) > 0)) {
                                this.close(true);
                                PageViewerGui.openEntry(player, entry, type.isRight, this::open);
                                GuiUtils.playClickSound(this.player);
                            }
                        });
                this.setSlot(i + 3, b);
            }

            for (; i < PAGE_SIZE; i++) {
                this.setSlot(i + 3, GuiUtils.EMPTY);
            }
        }

        var filler = GuiUtils.hasTexture(player) ? GuiUtils.EMPTY : GuiUtils.FILLER;
        for (int i = 0; i < 9; i++) {
            this.setSlot(PAGE_SIZE + 3 + i, filler);
        }


        this.setSlot(PAGE_SIZE + 3, new GuiElementBuilder(this.state.showAll ? Items.SLIME_BALL : Items.MAGMA_CREAM)
                .noDefaults()
                .hideDefaultTooltip()
                .setName(Component.translatable("text.polydex.button.see_" + (this.state.showAll ? "limited" : "everything")))
                .setCallback((x, y, z) -> {
                    this.state.showAll = !this.state.showAll;
                    this.setPage(this.getPage());
                    GuiUtils.playClickSound(this.player);
                }));

        this.setSlot(PAGE_SIZE + 3 + 3, this.getPageAmount() > 1 ? GuiUtils.previousPage(this.player, this) : filler);
        this.setSlot(PAGE_SIZE + 3 + 4, this.getPageAmount() > 1 ? GuiUtils.page(this.player, this.page + 1, this.getPageAmount()).build() : filler);
        this.setSlot(PAGE_SIZE + 3 + 5, this.getPageAmount() > 1 ? GuiUtils.nextPage(player, this) : filler);
        this.setSlot(PAGE_SIZE + 3 + 8, GuiUtils.backButton(this.player, this::closeAndReopenPrevious, this.openPrevious != null));
    }

    private void closeAndReopenPrevious() {
        if (this.openPrevious != null) {
            this.close(true);
            this.openPrevious.run();
        } else {
            this.close();
        }
    }

    protected int getEntryCount() {
        return this.entries.get(this.state.showAll).size();
    }

    @Override
    public int getPageAmount() {
        return (this.getEntryCount() - 1) / PAGE_SIZE + 1;
    }

    @Override
    public int getPage() {
        return this.page;
    }

    @Override
    public void setPage(int i) {
        this.page = i;
        this.updateDisplay();
    }
}
