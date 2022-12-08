package eu.pb4.polydex.impl.book;

import eu.pb4.polydex.impl.PolydexImpl;
import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.layered.LayerView;
import eu.pb4.sgui.api.gui.layered.LayeredGui;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;

import java.util.List;
import java.util.Locale;


public class MainIndexGui extends LayeredGui {
    private final ItemLayer mainLayer;
    private final NamespaceLayer indexLayer;
    private final LayerView indexLayerView;
    private PolydexImpl.PackedEntries entries;
    private boolean showAll;

    public MainIndexGui(ServerPlayerEntity player, boolean showAll, int pageItem, int pageSub) {
        super(ScreenHandlerType.GENERIC_9X6, player, false);
        this.showAll = showAll;

        this.mainLayer = new ItemLayer(6);
        this.indexLayer = new NamespaceLayer(6);

        this.entries = PolydexImpl.ITEM_ENTRIES;
        this.mainLayer.setPage(pageItem);
        this.indexLayer.setPage(pageSub);
        this.addLayer(this.mainLayer, 0, 0).setZIndex(1);
        this.indexLayerView = this.addLayer(this.indexLayer, 0, 0);
        this.indexLayerView.setZIndex(0);

        this.setTitle(Text.translatable("text.polydex.index_title"));
    }

    public class ItemLayer extends PagedLayer {
        public ItemLayer(int height) {
            super(MainIndexGui.this.getPlayer(), height, 9, true);
        }

        @Override
        public int getPageAmount() {
            return MathHelper.ceil(((double) MainIndexGui.this.entries.get(MainIndexGui.this.showAll).size()) / this.pageSize);
        }

        @Override
        protected GuiElement getElement(int id) {
            if (id < MainIndexGui.this.entries.get(MainIndexGui.this.showAll).size()) {
                var item = MainIndexGui.this.entries.get(MainIndexGui.this.showAll).get(id);

                return GuiElementBuilder.from(item.stack())
                        .setCallback((x, type, z) -> {
                            if (player.isCreative() && type.isMiddle) {
                                var cursor = this.player.currentScreenHandler.getCursorStack();
                                if (cursor.isItemEqual(item.stack()) && cursor.getCount() < cursor.getMaxCount()) {
                                    cursor.increment(1);
                                } else {
                                    this.player.currentScreenHandler.setCursorStack(item.stack().copy());
                                }
                            } else if ((type.isLeft && item.getVisiblePagesSize(MainIndexGui.this.getPlayer()) > 0) || (type.isRight && item.getVisibleIngredientPagesSize(MainIndexGui.this.getPlayer()) > 0)) {
                                MainIndexGui.this.close(true);
                                new EntryViewerGui(player, item, type.isRight, () -> {
                                    MainIndexGui.this.open();
                                }).open();
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
                        .setName(Text.translatable("text.polydex.button.see_" + (MainIndexGui.this.showAll ? "limited" : "everything")))
                        .setCallback((x, y, z) -> {
                            MainIndexGui.this.showAll = !MainIndexGui.this.showAll;
                            this.setPage(this.getPage());
                            GuiUtils.playClickSound(this.player);
                        }).build();
                case 1 -> new GuiElementBuilder(Items.KNOWLEDGE_BOOK)
                        .setName(Text.translatable("text.polydex.button.select_displayed").formatted(Formatting.WHITE))
                        .setCallback((x, y, z) -> {
                            GuiUtils.playClickSound(this.player);
                            MainIndexGui.this.indexLayerView.setZIndex(2);
                        }).build();
                case 3 -> this.getPageAmount() > 1 ? GuiUtils.previousPage(this.player, this) : GuiUtils.FILLER;
                case 4 -> this.getPageAmount() > 1 ? new GuiElementBuilder(Items.BOOK)
                        .setName(Text.translatable("text.polydex.view.pages",
                                        Text.literal("" + (this.page + 1)).formatted(Formatting.WHITE),
                                        Text.literal("" + this.getPageAmount()).formatted(Formatting.WHITE)
                                ).formatted(Formatting.AQUA)
                        ).build() : GuiUtils.FILLER;
                case 5 -> this.getPageAmount() > 1 ? GuiUtils.nextPage(player, this) : GuiUtils.FILLER;
                case 8 -> GuiUtils.backButton(this.player, () -> MainIndexGui.this.close(), false);
                default -> GuiUtils.FILLER;
            };
        }
    }

    public class NamespaceLayer extends PagedLayer {
        private Type type = Type.NAMESPACES;

        public NamespaceLayer(int height) {
            super(MainIndexGui.this.getPlayer(), height, 9, true);
        }

        @Override
        public int getPageAmount() {
            return MathHelper.ceil(((double) this.type.entries.size() + 1) / this.pageSize);
        }

        @Override
        protected GuiElement getElement(int id) {
            if (id == 0) {
                var builder = new GuiElementBuilder(Items.KNOWLEDGE_BOOK)
                        .setName(Text.translatable("text.polydex.display_all_items"))
                        .hideFlags()
                        .setCallback((x, y, z) -> {
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

            if (id < this.type.entries.size() + 1) {
                var item = this.type.entries.get(id - 1);

                var builder = GuiElementBuilder.from(item.icon())
                        .setName(item.display())
                        .hideFlags()
                        .setCallback((x, y, z) -> {
                            MainIndexGui.this.entries = item.entries();
                            MainIndexGui.this.indexLayer.updateDisplay();
                            MainIndexGui.this.mainLayer.setPage(0);
                            GuiUtils.playClickSound(this.player);
                            MainIndexGui.this.indexLayerView.setZIndex(0);
                        });

                if (item.entries() == MainIndexGui.this.entries) {
                    builder.enchant(Enchantments.LURE, 1);
                }


                return builder.build();
            }
            return GuiUtils.EMPTY;
        }

        @Override
        protected GuiElement getNavElement(int id) {
            return switch (id) {
                case 0 -> new GuiElementBuilder(Items.BOOK)
                        .setName(Text.translatable("text.polydex.category." + this.type.name().toLowerCase(Locale.ROOT)))
                        .setCallback((x, y, z) -> {
                            GuiUtils.playClickSound(this.player);
                            this.type = this.type.getNext();
                            this.updateDisplay();
                        })
                        .build();
                case 3 -> this.getPageAmount() > 1 ? GuiUtils.previousPage(this.player, this) : GuiUtils.FILLER;
                case 4 -> this.getPageAmount() > 1 ? new GuiElementBuilder(Items.BOOK)
                        .setName(Text.translatable("text.polydex.view.pages",
                                        Text.literal("" + (this.page + 1)).formatted(Formatting.WHITE),
                                        Text.literal("" + this.getPageAmount()).formatted(Formatting.WHITE)
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


        private enum Type {
            NAMESPACES(PolydexImpl.NAMESPACED_ENTRIES),
            ITEM_GROUP(PolydexImpl.ITEM_GROUP_ENTRIES),
            ;
            public final List<PolydexImpl.NamespacedEntry> entries;

            Type(List<PolydexImpl.NamespacedEntry> list) {
                this.entries = list;
            }

            public Type getNext() {
                return Type.values()[(this.ordinal() + 1) % Type.values().length];
            }
        }
    }
}
