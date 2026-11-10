package github.com.gengyoubo.sscfa.client.renderer;

import net.minecraft.world.entity.Entity;

/** 妫板棗鐓欏〒鍙夌厠閸ｎ煉绱橣orge 閸楃姳缍呴敍澶涚窗鐎广垺鍩涚粩顖濈珶閻ｅ矂鏆呴崓蹇庣瑢濞撳弶鐓嬪鍛暚閺佹潙顓归幋椋庮伂鐏炲倻些濡炲秲鈧?*/
public final class DomainRenderer {
    private DomainRenderer() {}
    public static boolean blocksTargetingClient(Entity target) { return false; }
    public static boolean isCharging(java.util.UUID player) { return false; }
    public static void render(Object context) {}

    public static boolean blocksSoundForListener(double x, double y, double z) { return false; }

    public static net.minecraft.world.phys.Vec3 clampCamera(net.minecraft.world.phys.Vec3 from, net.minecraft.world.phys.Vec3 target) { return target; }
    public static net.minecraft.world.phys.Vec3 projectInside(net.minecraft.world.phys.Vec3 eye, net.minecraft.world.phys.Vec3 center) { return eye; }
}
