package eu.pb4.polydex.impl.display;

import eu.pb4.polydex.api.v1.hover.HoverDisplay;
import eu.pb4.polydex.api.v1.hover.HoverDisplayBuilder;
import eu.pb4.polydex.api.v1.hover.HoverSettings;
import eu.pb4.polydex.api.v1.hover.PolydexTarget;
import eu.pb4.polydex.impl.PolydexImpl;
import eu.pb4.polydex.impl.PolydexImplUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBossEventPacket;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.LivingEntity;

public class BossbarTargetDisplay extends BossEvent implements HoverDisplay {
    private static final UUID ID = new UUID(0x706F6C79646578l, 0l);
    private final PolydexTarget target;
    private final HoverSettings.DisplayMode displayMode;
    private boolean isHidden = true;
    private boolean isEntity = false;

    public BossbarTargetDisplay(PolydexTarget target) {
        super(ID, Component.empty(), BossBarColor.WHITE, BossBarOverlay.PROGRESS);
        this.setProgress(0);
        this.target = target;
        this.displayMode = target.settings().displayMode();

        if (this.displayMode == HoverSettings.DisplayMode.ALWAYS) {
            this.target.player().connection.send(ClientboundBossEventPacket.createAddPacket(this));
        }
    }

    @Override
    public void showDisplay() {
        this.onTargetUpdate();
        if (this.displayMode == HoverSettings.DisplayMode.SNEAKING && !this.target.player().isShiftKeyDown()) {
            return;
        }
        if (this.displayMode != HoverSettings.DisplayMode.ALWAYS && (this.displayMode == HoverSettings.DisplayMode.TARGET || this.target.player().isShiftKeyDown())) {
            this.target.player().connection.send(ClientboundBossEventPacket.createAddPacket(this));
        }
        this.isHidden = false;
    }

    @Override
    public void hideDisplay() {
        if (!this.isHidden) {
            if (this.displayMode == HoverSettings.DisplayMode.ALWAYS) {
                this.setName(Component.empty());
                this.setProgress(0);
                this.setColor(BossBarColor.WHITE);
                this.isEntity = false;

                this.target.player().connection.send(ClientboundBossEventPacket.createUpdateStylePacket(this));
                this.target.player().connection.send(ClientboundBossEventPacket.createUpdateNamePacket(this));
                this.target.player().connection.send(ClientboundBossEventPacket.createUpdateProgressPacket(this));
            } else {
                this.target.player().connection.send(ClientboundBossEventPacket.createRemovePacket(this.getId()));
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
        if (this.displayMode == HoverSettings.DisplayMode.SNEAKING && !this.target.player().isShiftKeyDown()) {
            this.hideDisplay();
            return;
        }

        var entity = this.target.entity();
        boolean isEntity = entity != null;
        if (this.isEntity != isEntity) {
            this.isEntity = isEntity;

            if (isEntity) {
                this.setColor(BossBarColor.RED);
            } else {
                this.setColor(BossBarColor.WHITE);
            }
            if (!this.isHidden || this.displayMode == HoverSettings.DisplayMode.ALWAYS) {
                this.target.player().connection.send(ClientboundBossEventPacket.createUpdateStylePacket(this));
            }
        }

        float percent = 0;

        if (entity instanceof LivingEntity livingEntity && PolydexImpl.config.displayEntityHealth) {
            percent = Math.min(livingEntity.getHealth() / livingEntity.getMaxHealth(), 1);
        } else if (PolydexImpl.config.displayMiningProgress && this.target.isMining()) {
            percent = Math.min(this.target.breakingProgress(), 1);
        }

        var build = HoverDisplayBuilder.build(this.target);

        List<Component> textList;
        var modName = build.removeAndGetComponent(HoverDisplayBuilder.MOD_SOURCE);
        if (modName != null) {
            textList = new ArrayList<>();
            var name = build.removeAndGetComponent(HoverDisplayBuilder.NAME);
            var t = Component.empty();
            if (name != null) {
                t.append(name);
            }
            t.append(Component.literal(" [").withStyle(ChatFormatting.GRAY))
                    .append(Component.empty().append(modName).withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal("]").withStyle(ChatFormatting.GRAY));
            textList.add(t);
            textList.addAll(build.getOutput());
        } else {
            textList = build.getOutput();
        }

        this.setName(PolydexImplUtils.mergeText(textList, PolydexImplUtils.DEFAULT_SEPARATOR));
        this.setProgress(percent);

        if (!this.isHidden || this.displayMode == HoverSettings.DisplayMode.ALWAYS) {
            this.target.player().connection.send(ClientboundBossEventPacket.createUpdateNamePacket(this));
            this.target.player().connection.send(ClientboundBossEventPacket.createUpdateProgressPacket(this));
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
        if (this.displayMode == HoverSettings.DisplayMode.ALWAYS || !this.isHidden) {
            this.target.player().connection.send(ClientboundBossEventPacket.createRemovePacket(this.getId()));
        }
    }

    @Override
    public Type getType() {
        return Type.SINGLE_LINE;
    }
}
