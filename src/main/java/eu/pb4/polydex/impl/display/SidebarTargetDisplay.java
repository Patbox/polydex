package eu.pb4.polydex.impl.display;

import eu.pb4.polydex.api.v1.hover.HoverDisplayBuilder;
import eu.pb4.polydex.api.v1.hover.PolydexTarget;
import eu.pb4.polydex.api.v1.hover.HoverDisplay;
import eu.pb4.polydex.impl.PolydexImpl;
import eu.pb4.sidebars.api.Sidebar;
import java.util.ArrayList;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

public class SidebarTargetDisplay extends Sidebar implements HoverDisplay {
    private final PolydexTarget target;

    public SidebarTargetDisplay(PolydexTarget target) {
        super(Priority.HIGH);
        this.target = target;
        this.addPlayer(target.player());
    }

    @Override
    public void showDisplay() {
        this.onTargetUpdate();
        this.show();
    }

    @Override
    public void hideDisplay() {
        this.hide();
    }

    @Override
    public void onBreakingStateUpdate() {
        onTargetUpdate();
    }

    @Override
    public void onTargetUpdate() {
        this.clearLines();

        var build = HoverDisplayBuilder.build(this.target);
        this.setTitle(Component.literal(this.target.pos().toShortString()).withStyle(ChatFormatting.GRAY));

        var lines = new ArrayList<Component>();

        {
            var component = build.removeAndGetComponent(HoverDisplayBuilder.NAME);
            if (component != null) {
                lines.add(
                        Component.translatable("text.polydex.sidebar.target", component.copy().setStyle(Style.EMPTY.withColor(ChatFormatting.WHITE).withBold(false))).withStyle(ChatFormatting.GRAY, ChatFormatting.BOLD)
                );
            }
        }

        if (PolydexImpl.config.displayModSource) {
            var component = build.removeAndGetComponent(HoverDisplayBuilder.MOD_SOURCE);
            if (component != null) {
                lines.add(
                        Component.translatable("text.polydex.sidebar.mod", component.copy().setStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW).withBold(false))).withStyle(ChatFormatting.DARK_GRAY)
                );
            }
        }
        {
            boolean shouldAdd = true;
            {
                var component = build.removeAndGetComponent(HoverDisplayBuilder.INPUT);
                if (component != null) {
                    shouldAdd = false;
                    lines.add(Component.empty());

                    lines.add(
                            Component.translatable("text.polydex.sidebar.input", component.copy().setStyle(Style.EMPTY.withColor(ChatFormatting.WHITE).withBold(false))).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
                    );
                }
            }
            {
                var component = build.removeAndGetComponent(HoverDisplayBuilder.FUEL);
                if (component != null) {
                    if (shouldAdd) {
                        shouldAdd = false;
                        lines.add(Component.empty());
                    }

                    lines.add(
                            Component.translatable("text.polydex.sidebar.fuel", component.copy().setStyle(Style.EMPTY.withColor(ChatFormatting.WHITE).withBold(false))).withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
                    );
                }
            }
            {
                var component = build.removeAndGetComponent(HoverDisplayBuilder.OUTPUT);
                if (component != null) {
                    if (shouldAdd) {
                        lines.add(Component.empty());
                    }

                    lines.add(
                            Component.translatable("text.polydex.sidebar.output", component.copy().setStyle(Style.EMPTY.withColor(ChatFormatting.WHITE).withBold(false))).withStyle(ChatFormatting.BOLD, ChatFormatting.DARK_GREEN)
                    );
                }
            }
        }

        var progress = build.removeAndGetComponent(HoverDisplayBuilder.PROGRESS);

        {
            var out = build.getOutput();
            if (!out.isEmpty()) {
                lines.add(Component.empty());
                lines.addAll(out);
            }
        }

        if (progress != null) {
            lines.add(Component.empty());
            lines.add(
                    Component.translatable("text.polydex.sidebar.progress", progress.copy().setStyle(Style.EMPTY.withColor(ChatFormatting.WHITE).withBold(false))).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
            );
        }

        int i = lines.size();
        for (var text : lines) {
            this.setLine(i--, text);
        }
    }

    @Override
    public boolean isHidden() {
        return !this.isActive();
    }

    @Override
    public boolean isSmall() {
        return false;
    }

    @Override
    public void remove() {
        this.hide();
    }

    @Override
    public Type getType() {
        return Type.MULTI_LINE;
    }
}
