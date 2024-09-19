package eu.pb4.polydex.impl.book.ui;

import eu.pb4.polydex.api.v1.recipe.PolydexEntry;
import eu.pb4.polydex.api.v1.recipe.PolydexPageUtils;
import eu.pb4.polydex.impl.PlayerInterface;
import eu.pb4.polydex.impl.PolydexImpl;
import eu.pb4.polydex.impl.book.InternalPageTextures;
import eu.pb4.sgui.api.elements.GuiElement;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.layered.LayerView;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.List;
import java.util.Locale;


public class MainIndexGui extends ExtendedGui {
    private final ItemLayer mainLayer;
    private final NamespaceLayer indexLayer;
    private final LayerView indexLayerView;
    private PolydexImpl.PackedEntries entries;
    private boolean showAll;

    public MainIndexGui(ServerPlayerEntity player, boolean showAll, int pageItem, int pageSub) {
        super(ScreenHandlerType.GENERIC_9X6, player, true);
        this.showAll = showAll;

        this.mainLayer = new ItemLayer(6);
        this.indexLayer = new NamespaceLayer(4);

        this.entries = PolydexImpl.ITEM_ENTRIES;
        this.mainLayer.setPage(pageItem);
        this.indexLayer.setPage(pageSub);
        this.addLayer(this.mainLayer, 0, 0).setZIndex(1);
        this.indexLayerView = this.addLayer(this.indexLayer, 0, 6);
        this.indexLayerView.setZIndex(0);

        this.setOverlayTexture(InternalPageTextures.MAIN_INVENTORY);
        this.setText(Text.translatable("text.polydex.index_title"));
    }

    @Override
    public void onOpen() {
        super.onOpen();
        this.mainLayer.updateDisplay();
        this.indexLayer.updateDisplay();
    }

    @Override
    public void onTick() {
        if (!PolydexImpl.isReady()) {
            this.close();
            return;
        }
        super.onTick();
    }

    public class ItemLayer extends PagedLayer {
        public ItemLayer(int height) {
            super(MainIndexGui.this.getPlayer(), height, 9, true);
        }

        @Override
        protected int getEntryCount() {
            return MainIndexGui.this.entries.get(MainIndexGui.this.showAll).size();
        }

        @Override
        protected GuiElement getElement(int id) {
            if (id < MainIndexGui.this.entries.get(MainIndexGui.this.showAll).size()) {
                var item = MainIndexGui.this.entries.get(MainIndexGui.this.showAll).get(id);

                return GuiElementBuilder.from(item.stack().toDisplayItemStack(player))
                        .setCallback((x, type, z) -> {
                            /*if (player.isCreative() && type.isMiddle) {
                                var cursor = this.player.currentScreenHandler.getCursorStack();
                                if (ItemStack.areItemsEqual(cursor, item.stack()) && cursor.getCount() < cursor.getMaxCount()) {
                                    cursor.increment(1);
                                } else {
                                    this.player.currentScreenHandler.setCursorStack(item.stack().copy());
                                }
                            } else */if ((type.isLeft && item.getVisiblePagesSize(MainIndexGui.this.getPlayer()) > 0) || (type.isRight && item.getVisibleIngredientPagesSize(MainIndexGui.this.getPlayer()) > 0)) {
                                MainIndexGui.this.close(true);
                                PageViewerGui.openEntry (player, item, type.isRight, MainIndexGui.this::open);
                                GuiUtils.playClickSound(this.player);
                            }
                        })
                        .build();

            }
            return GuiElement.EMPTY;
        }

        @Override
        protected GuiElement getNavElement(int id) {
            return switch (id) {
                case 0 -> new GuiElementBuilder(MainIndexGui.this.showAll ? Items.SLIME_BALL : Items.MAGMA_CREAM)
                        .noDefaults()
                        .hideDefaultTooltip()
                        .setName(Text.translatable("text.polydex.button.see_" + (MainIndexGui.this.showAll ? "limited" : "everything")))
                        .setCallback((x, y, z) -> {
                            MainIndexGui.this.showAll = !MainIndexGui.this.showAll;
                            this.setPage(this.getPage());
                            GuiUtils.playClickSound(this.player);
                        }).build();

                case 1 -> new GuiElementBuilder(Items.KNOWLEDGE_BOOK)
                        .noDefaults()
                        .hideDefaultTooltip()
                        .setName(Text.translatable("text.polydex.category." + MainIndexGui.this.indexLayer.type.name().toLowerCase(Locale.ROOT)))
                        .setCallback((x, y, z) -> {
                            GuiUtils.playClickSound(this.player);
                            MainIndexGui.this.indexLayer.type = MainIndexGui.this.indexLayer.type.getNext();
                            MainIndexGui.this.indexLayer.updateDisplay();
                            this.updateDisplay();
                            MainIndexGui.this.setOverlayTexture(
                                    MainIndexGui.this.indexLayer.type == NamespaceLayer.Type.INVENTORY
                                            ? InternalPageTextures.MAIN_INVENTORY
                                            : InternalPageTextures.MAIN
                                    );
                        })
                        .build();
                /*case 1 -> new GuiElementBuilder(Items.KNOWLEDGE_BOOK)
                        .setName(Text.translatable("text.polydex.button.select_displayed").formatted(Formatting.WHITE))
                        .setCallback((x, y, z) -> {
                            GuiUtils.playClickSound(this.player);
                            MainIndexGui.this.indexLayerView.setZIndex(2);
                        }).build();*/
                case 3 -> this.getPageAmount() > 1 ? GuiUtils.previousPage(this.player, this) : filler();
                case 4 -> this.getPageAmount() > 1 ? GuiUtils.page(this.player,  this.page + 1, this.getPageAmount()).build() : filler();
                case 5 -> this.getPageAmount() > 1 ? GuiUtils.nextPage(player, this) : filler();
                case 8 -> GuiUtils.backButton(this.player, MainIndexGui.this::close, false);
                default -> filler();
            };
        }
    }

    public class NamespaceLayer extends PagedLayer {
        private Type type = Type.INVENTORY;
        public NamespaceLayer(int height) {
            super(MainIndexGui.this.getPlayer(), height, 9, true);
        }

        @Override
        protected int getEntryCount() {
            return switch (this.type) {
                case INVENTORY -> 1;
                case LAST_VIEW -> ((PlayerInterface) player.networkHandler).polydex_lastViewed().size();
                default -> this.type.entries.size() + 1;
            };
        }

        @Override
        public int getPageAmount() {
            return this.type == Type.INVENTORY ? 1 : MathHelper.ceil(((double) getEntryCount()) / this.pageSize);
        }

        @Override
        protected void updateDisplay() {
            if (type == Type.INVENTORY) {
                var inventory = this.player.getInventory();
                for (var i = 0; i < 3; ++i) {
                    for (var j = 0; j < 9; ++j) {
                        this.setSlot(i * 9 + j, createDirect(inventory.getStack(j + (i + 1) * 9)));
                    }
                }

                for (var i = 0; i < 9; ++i) {
                    this.setSlot(i + 3 * 9, createDirect(inventory.getStack(i)));
                }
            } else {
                super.updateDisplay();
            }
        }

        private GuiElement createDirect(ItemStack stack) {
            return new GuiElement(stack, (x, type, z) -> {
                var page = PolydexPageUtils.getItemEntryFor(stack);
                if (page != null && ((type.isLeft && page.getVisiblePagesSize(MainIndexGui.this.getPlayer()) > 0) || (type.isRight && page.getVisibleIngredientPagesSize(MainIndexGui.this.getPlayer()) > 0))) {
                    MainIndexGui.this.close(true);
                    PageViewerGui.openEntry(player, page, type.isRight, MainIndexGui.this::open);
                    GuiUtils.playClickSound(this.player);
                }
            });
        }

        private GuiElement createDirect(PolydexEntry page) {
            return new GuiElement(page.stack().toItemStack(player), (x, type, z) -> {
                if ((type.isLeft && page.getVisiblePagesSize(MainIndexGui.this.getPlayer()) > 0) || (type.isRight && page.getVisibleIngredientPagesSize(MainIndexGui.this.getPlayer()) > 0)) {
                    MainIndexGui.this.close(true);
                    PageViewerGui.openEntry(player, page, type.isRight, MainIndexGui.this::open);
                    GuiUtils.playClickSound(this.player);
                }
            });
        }

        @Override
        protected GuiElement getElement(int id) {
            return switch (this.type) {
                case LAST_VIEW -> {
                    var list = ((PlayerInterface) player.networkHandler).polydex_lastViewed();
                    if (id >= list.size()) {
                        yield GuiElement.EMPTY;
                    }
                    var x = PolydexPageUtils.getEntry(list.get(id));
                    if (x != null) {
                        yield createDirect(x);
                    }
                    yield GuiElement.EMPTY;
                }
                case INVENTORY -> GuiElement.EMPTY;
                default -> getElementTypeSelector(id);
            };
        }

        private GuiElement getElementTypeSelector(int id) {
            if (id == 0) {
                var builder = new GuiElementBuilder(Items.KNOWLEDGE_BOOK)
                        .setName(Text.translatable("text.polydex.display_all_items"))
                        .noDefaults()
                        .hideDefaultTooltip()
                        .setCallback((x, y, z) -> {
                            MainIndexGui.this.entries = PolydexImpl.ITEM_ENTRIES;
                            MainIndexGui.this.indexLayer.updateDisplay();
                            MainIndexGui.this.mainLayer.setPage(0);
                            GuiUtils.playClickSound(this.player);
                            MainIndexGui.this.indexLayerView.setZIndex(0);
                        });

                if (MainIndexGui.this.entries == PolydexImpl.ITEM_ENTRIES) {
                    builder.glow();
                }

                return builder.build();
            }

            if (id < this.type.entries.size() + 1) {
                var item = this.type.entries.get(id - 1);

                var builder = GuiElementBuilder.from(item.icon().apply(player))
                        .setName(item.display())
                        .noDefaults()
                        .hideDefaultTooltip()
                        .setCallback((x, y, z) -> {
                            MainIndexGui.this.entries = item.entries();
                            MainIndexGui.this.indexLayer.updateDisplay();
                            MainIndexGui.this.mainLayer.setPage(0);
                            GuiUtils.playClickSound(this.player);
                            MainIndexGui.this.indexLayerView.setZIndex(0);
                        });

                if (item.entries() == MainIndexGui.this.entries) {
                    builder.glow();
                }


                return builder.build();
            }
            return GuiElement.EMPTY;
        }

        @Override
        protected GuiElement getNavElement(int id) {
            return switch (id) {
                case 3 -> this.getPageAmount() > 1 ? GuiUtils.previousPage(this.player, this) : filler();
                case 4 -> this.getPageAmount() > 1 ? GuiUtils.page(this.player,  this.page + 1, this.getPageAmount()).build() : filler();
                case 5 -> this.getPageAmount() > 1 ? GuiUtils.nextPage(player, this) : filler();
                default -> filler();
            };
        }


        private enum Type {
            INVENTORY(null),
            LAST_VIEW(null),
            ITEM_GROUP(PolydexImpl.ITEM_GROUP_ENTRIES),
            NAMESPACES(PolydexImpl.NAMESPACED_ENTRIES),
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
