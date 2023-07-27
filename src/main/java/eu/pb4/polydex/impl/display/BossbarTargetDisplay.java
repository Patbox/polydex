package eu.pb4.polydex.impl.display;

import eu.pb4.polydex.api.v1.hover.HoverDisplay;
import eu.pb4.polydex.api.v1.hover.HoverDisplayBuilder;
import eu.pb4.polydex.api.v1.hover.PolydexTarget;
import eu.pb4.polydex.impl.PolydexImpl;
import eu.pb4.polydex.impl.PolydexImplUtils;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.network.packet.s2c.play.BossBarS2CPacket;
import net.minecraft.text.Text;

import java.util.UUID;

public class BossbarTargetDisplay extends BossBar implements HoverDisplay {
    private static final UUID ID = new UUID(0x706F6C79646578l, 0l);
    private final PolydexTarget target;
    private final DisplayMode displayMode;
    private boolean isHidden = true;
    private boolean isEntity = false;

    public BossbarTargetDisplay(PolydexTarget target, DisplayMode mode) {
        super(ID, Text.empty(), Color.WHITE, Style.PROGRESS);
        this.setPercent(0);
        this.target = target;
        this.displayMode = mode;

        if (mode == DisplayMode.ALWAYS) {
            this.target.player().networkHandler.sendPacket(BossBarS2CPacket.add(this));
        }
    }

    public static BossbarTargetDisplay targetted(PolydexTarget target) {
        return new BossbarTargetDisplay(target, DisplayMode.TARGET);
    }

    public static BossbarTargetDisplay always(PolydexTarget target) {
        return new BossbarTargetDisplay(target, DisplayMode.ALWAYS);
    }

    public static BossbarTargetDisplay sneaking(PolydexTarget target) {
        return new BossbarTargetDisplay(target, DisplayMode.SNEAK);
    }

    @Override
    public void showDisplay() {
        this.onTargetUpdate();
        if (this.displayMode != DisplayMode.ALWAYS && (this.displayMode == DisplayMode.TARGET || this.target.player().isSneaking())) {
            this.target.player().networkHandler.sendPacket(BossBarS2CPacket.add(this));
            this.isHidden = false;
        }
    }

    @Override
    public void hideDisplay() {
        if (!this.isHidden) {
            if (this.displayMode == DisplayMode.ALWAYS) {
                this.setName(Text.empty());
                this.setPercent(0);
                this.setColor(Color.WHITE);
                this.isEntity = false;

                this.target.player().networkHandler.sendPacket(BossBarS2CPacket.updateStyle(this));
                this.target.player().networkHandler.sendPacket(BossBarS2CPacket.updateName(this));
                this.target.player().networkHandler.sendPacket(BossBarS2CPacket.updateProgress(this));
            } else {
                this.target.player().networkHandler.sendPacket(BossBarS2CPacket.remove(this.getUuid()));
            }
            this.isHidden = true;
        }
    }

    @Override
    public void onBreakingStateUpdate() {
        onTargetUpdate();
    }

    @Override
    public void onTargetUpdate() {
        if (this.displayMode == DisplayMode.SNEAK && !this.target.player().isSneaking()) {
            this.hideDisplay();
            return;
        }

        var entity = this.target.entity();
        boolean isEntity = entity != null;
        if (this.isEntity != isEntity) {
            this.isEntity = isEntity;

            if (isEntity) {
                this.setColor(Color.RED);
            } else {
                this.setColor(Color.WHITE);
            }
            if (!this.isHidden || this.displayMode == DisplayMode.ALWAYS) {
                this.target.player().networkHandler.sendPacket(BossBarS2CPacket.updateStyle(this));
            }
        }

        float percent = 0;

        if (entity instanceof LivingEntity livingEntity && PolydexImpl.config.displayEntityHealth) {
            percent = Math.min(livingEntity.getHealth() / livingEntity.getMaxHealth(), 1);
        } else if (PolydexImpl.config.displayMiningProgress && this.target.isMining()) {
            percent = Math.min(this.target.breakingProgress(), 1);
        }

        this.setName(PolydexImplUtils.mergeText(HoverDisplayBuilder.buildText(this.target), PolydexImplUtils.DEFAULT_SEPARATOR));
        this.setPercent(percent);

        if (!this.isHidden || this.displayMode == DisplayMode.ALWAYS) {
            this.target.player().networkHandler.sendPacket(BossBarS2CPacket.updateName(this));
            this.target.player().networkHandler.sendPacket(BossBarS2CPacket.updateProgress(this));
        }
    }

    @Override
    public boolean isHidden() {
        return this.isHidden;
    }

    @Override
    public boolean isSmall() {
        return true;
    }

    @Override
    public void remove() {
        if (this.displayMode == DisplayMode.ALWAYS || !this.isHidden) {
            this.target.player().networkHandler.sendPacket(BossBarS2CPacket.remove(this.getUuid()));
        }
    }

    @Override
    public Type getType() {
        return Type.SINGLE_LINE;
    }

    public enum DisplayMode {
        ALWAYS,
        TARGET,
        SNEAK
    }
}
