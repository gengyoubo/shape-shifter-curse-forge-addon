package github.com.gengyoubo.sscfa.anomaly;

import net.minecraft.world.entity.LivingEntity;

/** 閸愯崵绮ㄩ幒褍鍩楅敍鍦榦rge 缁犫偓閸栨牭绱氶敍姘箽閻ｆ瑥绱撶敮鍛婃櫏閺嬫粌鐪伴幍鈧棁鈧崗銉ュ經閿涙稑鐣弫鏉戝枙缂?鐟欏棜顫楅柨浣哥窡閼宠棄濮忕仦鍌溞╁宥冣偓?*/
public final class FrozenControl {
    public static final int DEFAULT_DURATION_TICKS = 100;
    public static final int SHATTER_SLOW_TICKS = 60;
    private FrozenControl() {}
    public static boolean freeze(LivingEntity target, int durationTicks) { return false; }
    public static void clear(LivingEntity target) {}
    public static boolean blocked(LivingEntity target) { return false; }
    public static boolean blocked(net.minecraft.server.level.ServerPlayer player) { return false; }
    public static void applied(net.minecraft.world.entity.LivingEntity target) {}
    public static void breakFromHit(net.minecraft.world.entity.LivingEntity target) {}
    public static void release(net.minecraft.world.entity.LivingEntity target) {}
    public static net.minecraft.network.protocol.game.ServerboundMovePlayerPacket lockLook(net.minecraft.server.level.ServerPlayer player, net.minecraft.network.protocol.game.ServerboundMovePlayerPacket packet) { return packet; }
    public static void init() {}
}
