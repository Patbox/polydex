package eu.pb4.polydex.impl.display;

import eu.pb4.polydex.api.DisplayBuilder;
import eu.pb4.polydex.api.PolydexTarget;
import eu.pb4.polydex.api.TargetDisplay;
import eu.pb4.sidebars.api.Sidebar;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;

public class SidebarTargetDisplay extends Sidebar implements TargetDisplay {
    private final PolydexTarget target;

    public SidebarTargetDisplay(PolydexTarget target) {
        super(Priority.HIGH);
        this.target = target;
        this.addPlayer(target.getPlayer());
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

        var build = DisplayBuilder.build(this.target);
        this.setTitle(Text.literal(this.target.getTargetPos().toShortString()).formatted(Formatting.GRAY));

        var lines = new ArrayList<Text>();

        {
            var component = build.getComponent(DisplayBuilder.NAME);
            build.removeComponent(DisplayBuilder.NAME);
            if (component != null) {
                lines.add(
                        Text.translatable("text.polydex.sidebar.target", component.copy().setStyle(Style.EMPTY.withColor(Formatting.WHITE).withBold(false))).formatted(Formatting.YELLOW, Formatting.BOLD)
                );
            }
        }
        {
            boolean shouldAdd = true;
            {
                var component = build.getComponent(DisplayBuilder.INPUT);
                if (component != null) {
                    if (shouldAdd) {
                        shouldAdd = false;
                        lines.add(Text.empty());
                    }

                    lines.add(
                            Text.translatable("text.polydex.sidebar.input", component.copy().setStyle(Style.EMPTY.withColor(Formatting.WHITE).withBold(false))).formatted(Formatting.GREEN, Formatting.BOLD)
                    );
                }
            }
            {
                var component = build.getComponent(DisplayBuilder.FUEL);
                if (component != null) {
                    if (shouldAdd) {
                        shouldAdd = false;
                        lines.add(Text.empty());
                    }

                    lines.add(
                            Text.translatable("text.polydex.sidebar.fuel", component.copy().setStyle(Style.EMPTY.withColor(Formatting.WHITE).withBold(false))).formatted(Formatting.RED, Formatting.BOLD)
                    );
                }
            }
            {
                var component = build.getComponent(DisplayBuilder.OUTPUT);
                if (component != null) {
                    if (shouldAdd) {
                        lines.add(Text.empty());
                    }

                    lines.add(
                            Text.translatable("text.polydex.sidebar.output", component.copy().setStyle(Style.EMPTY.withColor(Formatting.WHITE).withBold(false))).formatted(Formatting.BOLD, Formatting.DARK_GREEN)
                    );
                }
            }
        }

        var progress = build.getComponent(DisplayBuilder.PROGRESS);
        build.removeComponent(DisplayBuilder.PROGRESS);

        {
            var out = build.getOutput();
            if (!out.isEmpty()) {
                lines.add(Text.empty());
                lines.addAll(out);
            }
        }

        if (progress != null) {
            lines.add(Text.empty());
            lines.add(
                    Text.translatable("text.polydex.sidebar.progress", progress.copy().setStyle(Style.EMPTY.withColor(Formatting.WHITE).withBold(false))).formatted(Formatting.GOLD, Formatting.BOLD)
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
