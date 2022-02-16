package eu.pb4.polydex.impl.book;

import eu.pb4.polydex.impl.PolydexImpl;
import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.layered.LayerView;
import eu.pb4.sgui.api.gui.layered.LayeredGui;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;


public class MainIndexGui extends LayeredGui {
    private final ItemLayer mainLayer;
    private final NamespaceLayer indexLayer;
    private final LayerView indexLayerView;
    private PolydexImpl.PackedEntries entries;
    private String namespace;
    private boolean showAll;

    public MainIndexGui(ServerPlayerEntity player, boolean showAll, String namespace, int pageItem, int pageSub) {
        super(ScreenHandlerType.GENERIC_9X6, player,  false);
        this.showAll = showAll;

        this.mainLayer = new ItemLayer(6);
        this.indexLayer = new NamespaceLayer(6);

        this.entries = PolydexImpl.BY_NAMESPACE.containsKey(namespace) ? PolydexImpl.BY_NAMESPACE.get(namespace).entries() : PolydexImpl.ITEM_ENTRIES;
        this.namespace = namespace;
        this.mainLayer.setPage(pageItem);
        this.indexLayer.setPage(pageSub);
        this.addLayer(this.mainLayer, 0, 0).setZIndex(1);
        this.indexLayerView = this.addLayer(this.indexLayer, 0, 0);
        this.indexLayerView.setZIndex(0);

        this.setTitle(new TranslatableText("text.polydex.index_title"));
    }

    public class ItemLayer extends PagedLayer {
        public ItemLayer(int height) {
            super(MainIndexGui.this.getPlayer(), height, 9, true);
        }

        @Override
        public int getPageAmount() {
            return MathHelper.ceil(((double) MainIndexGui.this.entries.get(MainIndexGui.this.showAll).size()) / this.pageSize);
        }

        public static int getPageCount(boolean all) {
            return MathHelper.ceil(((double) PolydexImpl.ITEM_ENTRIES.get(all).size()) / EntryViewerGui.PAGE_SIZE);
        }

        @Override
        protected GuiElement getElement(int id) {
            if (id < MainIndexGui.this.entries.get(MainIndexGui.this.showAll).size()) {
                var item = MainIndexGui.this.entries.get(MainIndexGui.this.showAll).get(id);

                return GuiElementBuilder.from(item.stack())
                        .setCallback((x, y, z) -> {
                            if (item.pages().size() > 0) {
                                MainIndexGui.this.close();
                                new EntryViewerGui(player, item, MainIndexGui.this::open).open();
                                GuiUtils.playClickSound(this.player);

                            }
                        })
                        .build();

            }
            return GuiUtils.EMPTY;
        }

        @Override
        protected GuiElement getNavElement(int id) {
            return switch (id) {
                case 0 -> new GuiElementBuilder(MainIndexGui.this.showAll ? Items.SLIME_BALL : Items.MAGMA_CREAM)
                        .setName(new TranslatableText("text.polydex.button.see_" + (MainIndexGui.this.showAll ? "limited" : "everything")))
                        .setCallback((x, y, z) -> {
                            MainIndexGui.this.showAll = !MainIndexGui.this.showAll;
                            this.setPage(this.getPage());
                            GuiUtils.playClickSound(this.player);
                        }).build();
                case 1 -> new GuiElementBuilder(Items.KNOWLEDGE_BOOK)
                        .setName(new TranslatableText("text.polydex.button.select_displayed").formatted(Formatting.WHITE))
                        .setCallback((x, y, z) -> {
                            GuiUtils.playClickSound(this.player);
                            MainIndexGui.this.indexLayerView.setZIndex(2);
                        }).build();
                case 3 -> this.getPageAmount() > 1 ? GuiUtils.previousPage(this.player, this) : GuiUtils.FILLER;
                case 4 -> this.getPageAmount() > 1 ? new GuiElementBuilder(Items.BOOK)
                        .setName(new TranslatableText("text.polydex.view.pages",
                                        new LiteralText("" + (this.page + 1)).formatted(Formatting.WHITE),
                                        new LiteralText("" + this.getPageAmount()).formatted(Formatting.WHITE)
                                ).formatted(Formatting.AQUA)
                        ).build() : GuiUtils.FILLER;
                case 5 -> this.getPageAmount() > 1 ? GuiUtils.nextPage(player, this) : GuiUtils.FILLER;
                case 8 -> GuiUtils.backButton(this.player, () -> MainIndexGui.this.close(), false);
                default -> GuiUtils.FILLER;
            };
        }
    }

    public class NamespaceLayer extends PagedLayer {
        public NamespaceLayer(int height) {
            super(MainIndexGui.this.getPlayer(), height, 9, true);
        }

        @Override
        public int getPageAmount() {
            return MathHelper.ceil(((double) PolydexImpl.NAMESPACED_ENTRIES.size() + 1) / this.pageSize);
        }

        @Override
        protected GuiElement getElement(int id) {
            if (id == 0) {
                var builder = new GuiElementBuilder(Items.KNOWLEDGE_BOOK)
                        .setName(new TranslatableText("text.polydex.display_all_items"))
                        .hideFlags()
                        .setCallback((x, y, z) -> {
                            MainIndexGui.this.namespace = "";
                            MainIndexGui.this.entries = PolydexImpl.ITEM_ENTRIES;
                            MainIndexGui.this.indexLayer.updateDisplay();
                            MainIndexGui.this.mainLayer.setPage(0);
                            GuiUtils.playClickSound(this.player);
                            MainIndexGui.this.indexLayerView.setZIndex(0);
                        });

                if (MainIndexGui.this.entries == PolydexImpl.ITEM_ENTRIES) {
                    builder.enchant(Enchantments.LURE, 1);
                }

                return builder.build();
            }

            if (id < PolydexImpl.NAMESPACED_ENTRIES.size() + 1) {
                var item = PolydexImpl.NAMESPACED_ENTRIES.get(id - 1);

                var builder = GuiElementBuilder.from(item.icon())
                        .setName(item.display())
                        .hideFlags()
                        .setCallback((x, y, z) -> {
                            MainIndexGui.this.namespace = item.namespace();
                            MainIndexGui.this.entries = item.entries();
                            MainIndexGui.this.indexLayer.updateDisplay();
                            MainIndexGui.this.mainLayer.setPage(0);
                            GuiUtils.playClickSound(this.player);
                            MainIndexGui.this.indexLayerView.setZIndex(0);
                        });

                if (item.namespace().equals(MainIndexGui.this.namespace)) {
                    builder.enchant(Enchantments.LURE, 1);
                }


                return builder.build();
            }
            return GuiUtils.EMPTY;
        }

        @Override
        protected GuiElement getNavElement(int id) {
            return switch (id) {
                case 3 -> this.getPageAmount() > 1 ? GuiUtils.previousPage(this.player, this) : GuiUtils.FILLER;
                case 4 -> this.getPageAmount() > 1 ? new GuiElementBuilder(Items.BOOK)
                        .setName(new TranslatableText("text.polydex.view.pages",
                                        new LiteralText("" + (this.page + 1)).formatted(Formatting.WHITE),
                                        new LiteralText("" + this.getPageAmount()).formatted(Formatting.WHITE)
                                ).formatted(Formatting.AQUA)
                        ).build() : GuiUtils.FILLER;
                case 5 -> this.getPageAmount() > 1 ? GuiUtils.nextPage(player, this) : GuiUtils.FILLER;
                case 8 -> GuiUtils.backButton(this.player, () -> {
                    GuiUtils.playClickSound(this.player);
                    MainIndexGui.this.indexLayerView.setZIndex(0);
                }, false);
                default -> GuiUtils.FILLER;
            };
        }
    }

    /*private void reopen() {
        new MainIndexGui(this.getPlayer(), this.showAll, this.namespace, this.mainLayer.page, this.indexLayer.page).open();
    }*/
}
