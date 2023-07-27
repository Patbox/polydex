package eu.pb4.polydex.impl.book.ui;

import eu.pb4.polydex.api.v1.recipe.PolydexCategory;
import eu.pb4.polydex.api.v1.recipe.PolydexPage;
import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class CategoryPageViewerGui extends AbstractPageViewerGui {
    private final SubRecipeLayer subPages;

    public CategoryPageViewerGui(ServerPlayerEntity player, PolydexCategory category, List<PolydexPage> pages, @Nullable Runnable closeCallback) {
        super(player, closeCallback);
        this.pages = pages;
        this.setText(Text.translatable("text.polydex.recipes_title_category", category.name()));
        this.subPages = new SubRecipeLayer(4);
        this.addLayer(this.subPages, 0, 6);
        this.setupNavigator();
        this.updateDisplay();
        this.subPages.updateDisplay();
    }

    public class SubRecipeLayer extends PagedLayer {
        public SubRecipeLayer(int height) {
            super(CategoryPageViewerGui.this.getPlayer(), height, 9, true);
        }

        @Override
        public int getPageAmount() {
            return pages.size() / this.pageSize;
        }

        @Override
        protected GuiElement getElement(int id) {
            if (id < pages.size()) {
                var page = pages.get(id);

                return GuiElementBuilder.from(page.entryIcon(null, player))
                        .setCallback((x, type, z) -> {
                            CategoryPageViewerGui.this.setPage(id);
                            GuiUtils.playClickSound(this.player);
                        })
                        .build();

            }
            return GuiElement.EMPTY;
        }
    }
}
