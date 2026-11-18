package github.com.gengyoubo.sscfa.client.hud;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4f;

/** Runs without an OpenGL context; verifies the stack shared by HUD and screen rendering. */
public final class SpellbookHudLayerTest {
    public static void main(String[] args) {
        PoseStack pose = new PoseStack();
        pose.translate(12, 34, 56);
        PoseStack.Pose caller = pose.last();
        Matrix4f original = new Matrix4f(caller.pose());

        for (int frame = 0; frame < 300; frame++) {
            drawHud(pose, false);
            require(pose.clear(), "HUD must restore the original stack depth");
            require(pose.last() == caller, "HUD must preserve the caller's pose");
            require(pose.last().pose().equals(original), "HUD transforms must not leak into screens");
        }

        // Forge or another overlay may already have pushed its own frame.
        pose.pushPose();
        PoseStack.Pose overlay = pose.last();
        try {
            drawHud(pose, true);
            throw new AssertionError("Expected the simulated render failure");
        } catch (RenderFailure expected) {
            require(pose.last() == overlay, "Failure cleanup must preserve the surrounding overlay");
        } finally {
            pose.popPose();
        }
        require(pose.clear() && pose.last() == caller, "Screen rendering must retain a usable base pose");
        require(pose.last().pose().equals(original), "Failure cleanup must preserve the base transform");
        System.out.println("Spellbook HUD pose stack regression checks passed");
    }

    private static void drawHud(PoseStack pose, boolean fail) {
        SpellbookHudLayer.push(pose);
        try {
            pose.translate(20, 40, 0);
            pose.pushPose();
            try {
                pose.scale(1.2f, 1.2f, 1);
                if (fail) throw new RenderFailure();
            } finally {
                pose.popPose();
            }
        } finally {
            pose.popPose();
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class RenderFailure extends RuntimeException {}
}
