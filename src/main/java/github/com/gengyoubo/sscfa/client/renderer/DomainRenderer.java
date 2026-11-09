package github.com.gengyoubo.sscfa.client.renderer;

import net.minecraft.world.entity.Entity;

/** 领域渲染器（Forge 占位）：客户端边界镜像与渲染待完整客户端层移植。 */
public final class DomainRenderer {
    private DomainRenderer() {}
    public static boolean blocksTargetingClient(Entity target) { return false; }
    public static boolean isCharging(java.util.UUID player) { return false; }
    public static void render(Object context) {}
}
