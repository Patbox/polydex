package eu.pb4.polydex.impl.compat;

import alexiil.mc.lib.multipart.api.AbstractPart;
import alexiil.mc.lib.multipart.api.MultipartUtil;
import alexiil.mc.lib.multipart.impl.LibMultiPart;
import eu.pb4.polydex.api.v1.hover.HoverDisplayBuilder;
import eu.pb4.polydex.impl.PolydexImpl;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Vec3d;

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

            var container = MultipartUtil.get(target.player().getServerWorld(), target.pos());

            if (container == null) {
                return;
            }

            Vec3d vec = target.hitResult().getPos().subtract(Vec3d.of(target.pos()));
            var part = container.getFirstPart(partx -> doesContain(partx, vec));
            if (part == null) {
                return;
            }

            var name = part.getName(target.hitResult() instanceof BlockHitResult blockHitResult ? blockHitResult : null);
            hoverDisplayBuilder.setComponent(HoverDisplayBuilder.NAME, name);
            hoverDisplayBuilder.setComponent(HoverDisplayBuilder.MOD_SOURCE, PolydexImpl.getMod(part.definition.identifier));
            hoverDisplayBuilder.setComponent(HoverDisplayBuilder.RAW_ID, Text.literal(part.definition.identifier.toString()));
        } catch (Throwable e) {

        }
    }

    private static boolean doesContain(AbstractPart part, Vec3d vec) {
        var shape = part.getOutlineShape();
        for (var box : shape.getBoundingBoxes()) {
            if (box.expand(0.01).contains(vec)) {
                return true;
            }
        }
        return false;
    }
}
