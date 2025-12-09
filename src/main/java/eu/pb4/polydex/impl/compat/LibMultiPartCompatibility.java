package eu.pb4.polydex.impl.compat;

import alexiil.mc.lib.multipart.api.AbstractPart;
import alexiil.mc.lib.multipart.api.MultipartUtil;
import alexiil.mc.lib.multipart.impl.LibMultiPart;
import eu.pb4.polydex.api.v1.hover.HoverDisplayBuilder;
import eu.pb4.polydex.impl.PolydexImpl;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class LibMultiPartCompatibility {
    public static void register() {
        try {
            HoverDisplayBuilder.register(LibMultiPart.BLOCK, LibMultiPartCompatibility::changeData);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private static void changeData(HoverDisplayBuilder hoverDisplayBuilder) {
        try {
            var target = hoverDisplayBuilder.getTarget();

            var container = MultipartUtil.get(target.player().level(), target.pos());

            if (container == null) {
                return;
            }

            Vec3 vec = target.hitResult().getLocation().subtract(Vec3.atLowerCornerOf(target.pos()));
            var part = container.getFirstPart(partx -> doesContain(partx, vec));
            if (part == null) {
                return;
            }

            var name = part.getName(target.hitResult() instanceof BlockHitResult blockHitResult ? blockHitResult : null);
            hoverDisplayBuilder.setComponent(HoverDisplayBuilder.NAME, name);
            hoverDisplayBuilder.setComponent(HoverDisplayBuilder.MOD_SOURCE, PolydexImpl.getMod(part.definition.identifier));
            hoverDisplayBuilder.setComponent(HoverDisplayBuilder.RAW_ID, Component.literal(part.definition.identifier.toString()));
        } catch (Throwable e) {

        }
    }

    private static boolean doesContain(AbstractPart part, Vec3 vec) {
        var shape = part.getOutlineShape();
        for (var box : shape.toAabbs()) {
            if (box.inflate(0.01).contains(vec)) {
                return true;
            }
        }
        return false;
    }
}
