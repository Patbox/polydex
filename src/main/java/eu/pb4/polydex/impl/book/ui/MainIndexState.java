package eu.pb4.polydex.impl.book.ui;

import eu.pb4.polydex.impl.PolydexImpl;

public class MainIndexState {
    public PolydexImpl.PackedEntries entries = PolydexImpl.ITEM_ENTRIES;
    public MainIndexGui.NamespaceLayer.Type type = MainIndexGui.NamespaceLayer.Type.INVENTORY;
    public boolean showAll = true;
    public int page = 0;
    public int subPage = 0;

    public void reset() {
        this.entries = PolydexImpl.ITEM_ENTRIES;
        this.page = 0;
        this.subPage = 0;
    }
}
