package github.com.gengyoubo.sscfa.network;

import net.minecraft.world.entity.Entity;
import github.com.gengyoubo.sscfa.network.VisualRecipe.Kind;

/**
 * 服务端授权的持续视觉租约。与 {@link github.com.gengyoubo.sscfa.network.visual.ParametricVisuals}
 * 一样，粒子表现纯客户端；Forge 移植阶段先保留空实现，客户端渲染层移植后接入同步包。
 */
public final class SustainedVisuals {
    private SustainedVisuals() {}

    public static void init() {}

    public static void touch(Entity source, Kind kind, int elapsed, int duration, double radius, double outerRadius) {}

    public static void stop(Entity source, Kind kind) {}
}
