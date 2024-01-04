package eu.pb4.polydex.impl.display;

import eu.pb4.polydex.api.v1.hover.HoverDisplay;
import eu.pb4.polydex.api.v1.hover.HoverDisplayBuilder;
import eu.pb4.polydex.api.v1.hover.HoverSettings;
import eu.pb4.polydex.api.v1.hover.PolydexTarget;
import eu.pb4.polydex.impl.PolydexImpl;
import eu.pb4.polydex.impl.PolydexImplUtils;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.network.packet.s2c.play.BossBarS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BossbarTargetDisplay extends BossBar implements HoverDisplay {
    private static final UUID ID = new UUID(0x706F6C79646578l, 0l);
    private final PolydexTarget target;
    private final HoverSettings.DisplayMode displayMode;
    private boolean isHidden = true;
    private boolean isEntity = false;

    public BossbarTargetDisplay(PolydexTarget target) {
        super(ID, Text.empty(), Color.WHITE, Style.PROGRESS);
        this.setPercent(0);
        this.target = target;
        this.displayMode = target.settings().displayMode();

        if (this.displayMode == HoverSettings.DisplayMode.ALWAYS) {
            this.target.player().networkHandler.sendPacket(BossBarS2CPacket.add(this));
        }
    }

    @Override
    public void showDisplay() {
        this.onTargetUpdate();
        if (this.displayMode == HoverSettings.DisplayMode.SNEAKING && !this.target.player().isSneaking()) {
            return;
        }
        if (this.displayMode != HoverSettings.DisplayMode.ALWAYS && (this.displayMode == HoverSettings.DisplayMode.TARGET || this.target.player().isSneaking())) {
            this.target.player().networkHandler.sendPacket(BossBarS2CPacket.add(this));
        }
        this.isHidden = false;
    }

    @Override
    public void hideDisplay() {
        if (!this.isHidden) {
            if (this.displayMode == HoverSettings.DisplayMode.ALWAYS) {
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
        if (this.displayMode == HoverSettings.DisplayMode.SNEAKING && !this.target.player().isSneaking()) {
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
            if (!this.isHidden || this.displayMode == HoverSettings.DisplayMode.ALWAYS) {
                this.target.player().networkHandler.sendPacket(BossBarS2CPacket.updateStyle(this));
            }
        }

        float percent = 0;

        if (entity instanceof LivingEntity livingEntity && PolydexImpl.config.displayEntityHealth) {
            percent = Math.min(livingEntity.getHealth() / livingEntity.getMaxHealth(), 1);
        } else if (PolydexImpl.config.displayMiningProgress && this.target.isMining()) {
            percent = Math.min(this.target.breakingProgress(), 1);
        }

        var build = HoverDisplayBuilder.build(this.target);

        List<Text> textList;
        var modName = build.removeAndGetComponent(HoverDisplayBuilder.MOD_SOURCE);
        if (modName != null) {
            textList = new ArrayList<>();
            var name = build.removeAndGetComponent(HoverDisplayBuilder.NAME);
            var t = Text.empty();
            if (name != null) {
                t.append(name);
            }
            t.append(Text.literal(" [").formatted(Formatting.GRAY))
                    .append(Text.empty().append(modName).formatted(Formatting.YELLOW))
                    .append(Text.literal("]").formatted(Formatting.GRAY));
            textList.add(t);
            textList.addAll(build.getOutput());
        } else {
            textList = build.getOutput();
        }

        this.setName(PolydexImplUtils.mergeText(textList, PolydexImplUtils.DEFAULT_SEPARATOR));
        this.setPercent(percent);

        if (!this.isHidden || this.displayMode == HoverSettings.DisplayMode.ALWAYS) {
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
        if (this.displayMode == HoverSettings.DisplayMode.ALWAYS || !this.isHidden) {
            this.target.player().networkHandler.sendPacket(BossBarS2CPacket.remove(this.getUuid()));
        }
    }

    @Override
    public Type getType() {
        return Type.SINGLE_LINE;
    }
}
