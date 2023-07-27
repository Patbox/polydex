package eu.pb4.polydex.impl.book.ui;

import eu.pb4.polydex.api.v1.recipe.PolydexEntry;
import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

public final class EntryPageViewerGui extends AbstractPageViewerGui {

    private final SubRecipeLayer subPages;

    public EntryPageViewerGui(ServerPlayerEntity player, PolydexEntry entry, boolean ingredients, @Nullable Runnable closeCallback) {
        super(player, closeCallback);
        this.entry = entry;
        this.pages = ingredients ? entry.getVisibleIngredientPages(player) : entry.getVisiblePages(player);
        this.setText(Text.translatable(ingredients ? "text.polydex.recipes_title_input" : "text.polydex.recipes_title_output", entry.stack().getName()));
        this.subPages = new SubRecipeLayer(4);
        this.addLayer(this.subPages, 0, 6);
        this.setupNavigator();
        this.updateDisplay();
        this.subPages.updateDisplay();
    }

    public class SubRecipeLayer extends PagedLayer {
        public SubRecipeLayer(int height) {
            super(EntryPageViewerGui.this.getPlayer(), height, 9, true);
        }

        @Override
        public int getPageAmount() {
            return pages.size() / this.pageSize;
        }

        @Override
        protected GuiElement getElement(int id) {
            if (id < pages.size()) {
                var page = pages.get(id);

                return GuiElementBuilder.from(page.typeIcon(player))
                        .setCallback((x, type, z) -> {
                            EntryPageViewerGui.this.setPage(id);
                            GuiUtils.playClickSound(this.player);
                        })
                        .build();

            }
            return GuiElement.EMPTY;
        }
    }
}
