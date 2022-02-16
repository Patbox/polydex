package eu.pb4.polydex.impl.display;

import eu.pb4.polydex.api.DisplayBuilder;
import eu.pb4.polydex.api.PolydexTarget;
import eu.pb4.polydex.api.PolydexUtils;
import eu.pb4.polydex.api.TargetDisplay;
import eu.pb4.polydex.impl.PolydexImpl;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.network.packet.s2c.play.BossBarS2CPacket;
import net.minecraft.text.LiteralText;

import java.util.UUID;

public class BossbarTargetDisplay extends BossBar implements TargetDisplay {
    private final PolydexTarget target;
    private final boolean forceDisplay;
    private boolean isHidden = true;
    private boolean isEntity = false;

    public BossbarTargetDisplay(PolydexTarget target, boolean forceDisplay) {
        super(UUID.randomUUID(), LiteralText.EMPTY, Color.WHITE, Style.PROGRESS);
        this.setPercent(0);
        this.target = target;
        this.forceDisplay = forceDisplay;

        if (this.forceDisplay) {
            this.target.getPlayer().networkHandler.sendPacket(BossBarS2CPacket.add(this));
        }
    }

    public static BossbarTargetDisplay targetted(PolydexTarget target) {
        return new BossbarTargetDisplay(target, false);
    }

    public static BossbarTargetDisplay always(PolydexTarget target) {
        return new BossbarTargetDisplay(target, true);
    }

    @Override
    public void showDisplay() {
        this.onTargetUpdate();
        if (!this.forceDisplay) {
            this.target.getPlayer().networkHandler.sendPacket(BossBarS2CPacket.add(this));
        }
        this.isHidden = false;
    }

    @Override
    public void hideDisplay() {
        if (this.forceDisplay) {
            this.setName(LiteralText.EMPTY);
            this.setPercent(0);
            this.setColor(Color.WHITE);
            this.isEntity = false;

            this.target.getPlayer().networkHandler.sendPacket(BossBarS2CPacket.updateStyle(this));
            this.target.getPlayer().networkHandler.sendPacket(BossBarS2CPacket.updateName(this));
            this.target.getPlayer().networkHandler.sendPacket(BossBarS2CPacket.updateProgress(this));
        } else {
            this.target.getPlayer().networkHandler.sendPacket(BossBarS2CPacket.remove(this.getUuid()));
        }
        this.isHidden = true;
    }

    @Override
    public void onBreakingStateUpdate() {
        onTargetUpdate();
    }

    @Override
    public void onTargetUpdate() {
        var entity = this.target.getEntity();
        boolean isEntity = entity != null;
        if (this.isEntity != isEntity) {
            this.isEntity = isEntity;

            if (isEntity) {
                this.setColor(Color.RED);
            } else {
                this.setColor(Color.WHITE);
            }
            this.target.getPlayer().networkHandler.sendPacket(BossBarS2CPacket.updateStyle(this));
        }

        float percent = 0;

        if (entity instanceof LivingEntity livingEntity && PolydexImpl.config.displayEntityHealth) {
            percent = livingEntity.getHealth() / livingEntity.getMaxHealth();
        } else if (PolydexImpl.config.displayMiningProgress && this.target.isMining()) {
            percent = this.target.getBreakingProgress();
        }

        this.setName(PolydexUtils.mergeText(DisplayBuilder.buildText(this.target), PolydexUtils.DEFAULT_SEPARATOR));
        this.setPercent(percent);

        this.target.getPlayer().networkHandler.sendPacket(BossBarS2CPacket.updateName(this));
        this.target.getPlayer().networkHandler.sendPacket(BossBarS2CPacket.updateProgress(this));
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
        if (this.forceDisplay || !this.isHidden) {
            this.target.getPlayer().networkHandler.sendPacket(BossBarS2CPacket.remove(this.getUuid()));
        }
    }

    @Override
    public Type getType() {
        return Type.SINGLE_LINE;
    }
}
