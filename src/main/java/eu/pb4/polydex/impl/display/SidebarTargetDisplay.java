package eu.pb4.polydex.impl.display;

import eu.pb4.polydex.api.DisplayBuilder;
import eu.pb4.polydex.api.PolydexTarget;
import eu.pb4.polydex.api.TargetDisplay;
import eu.pb4.sidebars.api.Sidebar;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
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
        this.setTitle(new LiteralText(this.target.getTargetPos().toShortString()).formatted(Formatting.GRAY));

        var lines = new ArrayList<Text>();

        {
            var component = build.getComponent(DisplayBuilder.NAME);
            build.removeComponent(DisplayBuilder.NAME);
            if (component != null) {
                lines.add(
                        new TranslatableText("text.polydex.sidebar.target", component.shallowCopy().setStyle(Style.EMPTY.withColor(Formatting.WHITE).withBold(false))).formatted(Formatting.YELLOW, Formatting.BOLD)
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
                        lines.add(LiteralText.EMPTY);
                    }

                    lines.add(
                            new TranslatableText("text.polydex.sidebar.input", component.shallowCopy().setStyle(Style.EMPTY.withColor(Formatting.WHITE).withBold(false))).formatted(Formatting.GREEN, Formatting.BOLD)
                    );
                }
            }
            {
                var component = build.getComponent(DisplayBuilder.FUEL);
                if (component != null) {
                    if (shouldAdd) {
                        shouldAdd = false;
                        lines.add(LiteralText.EMPTY);
                    }

                    lines.add(
                            new TranslatableText("text.polydex.sidebar.fuel", component.shallowCopy().setStyle(Style.EMPTY.withColor(Formatting.WHITE).withBold(false))).formatted(Formatting.RED, Formatting.BOLD)
                    );
                }
            }
            {
                var component = build.getComponent(DisplayBuilder.OUTPUT);
                if (component != null) {
                    if (shouldAdd) {
                        lines.add(LiteralText.EMPTY);
                    }

                    lines.add(
                            new TranslatableText("text.polydex.sidebar.output", component.shallowCopy().setStyle(Style.EMPTY.withColor(Formatting.WHITE).withBold(false))).formatted(Formatting.BOLD, Formatting.DARK_GREEN)
                    );
                }
            }
        }

        var progress = build.getComponent(DisplayBuilder.PROGRESS);
        build.removeComponent(DisplayBuilder.PROGRESS);

        {
            var out = build.getOutput();
            if (!out.isEmpty()) {
                lines.add(LiteralText.EMPTY);
                lines.addAll(out);
            }
        }

        if (progress != null) {
            lines.add(LiteralText.EMPTY);
            lines.add(
                    new TranslatableText("text.polydex.sidebar.progress", progress.shallowCopy().setStyle(Style.EMPTY.withColor(Formatting.WHITE).withBold(false))).formatted(Formatting.GOLD, Formatting.BOLD)
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
