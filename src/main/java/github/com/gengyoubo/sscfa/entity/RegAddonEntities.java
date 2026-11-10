package github.com.gengyoubo.sscfa.entity;

import net.minecraft.world.entity.EntityType;

/** Fabric 实体注册兼容 shim（Forge 侧实际注册为 ShellProjectile 占位）。 */
public final class RegAddonEntities {
    private RegAddonEntities() {}

    public static final EntityType<BridgeWebBullet> BRIDGE_WEB_BULLET = null;
    public static final EntityType<WebMembraneBullet> WEB_MEMBRANE_BULLET = null;
    public static final EntityType<SpiderSwingBullet> SPIDER_SWING_BULLET = null;

    public static void init() {
    }
}
