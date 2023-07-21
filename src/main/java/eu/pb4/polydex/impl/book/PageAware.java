package eu.pb4.polydex.impl.book;

import eu.pb4.sgui.api.gui.SlotGuiInterface;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

public interface PageAware {
    default void nextPage() {
        var page = this.getPage() + 1;

        if (page < this.getPageAmount()) {
            this.setPage(page);
        } else {
            this.setPage(0);
        }
    }

    default void previousPage() {
        var page = this.getPage() - 1;

        if (page < 0) {
            this.setPage(this.getPageAmount() - 1);
        } else {
            this.setPage(page);
        }
    }

    int getPage();

    void setPage(int i);

    int getPageAmount();
}
