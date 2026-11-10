package github.com.gengyoubo.sscfa.neomagic;

/** 闁哄唲鍛暭闁哄倽濮ょ涵璺侯浖閵夘垳绀凢orge 闁告濮崇紞鍛存晬婢舵稓绐楀ǎ鍥ㄧ箘閺嗏偓婵炲娲樺﹢铏閿濆洨鍨冲ù锝堟硶閺併倝鎯冮崟顒傚弨閻犲洢鍨归崣鍡涘矗閿濆繒绀夐悗鍦仱濡绢垱顩╅弮鈧涵璺虹暦閳哄嫅鎺撴交閹邦垼鏀介柡鍐硾缁剁喓绮旂紒姗嗘Щ闁?*/
public final class NeoLegacyCasting {
    private NeoLegacyCasting() {}
    public static boolean reversed() { return false; }
    public static boolean shifted() { return false; }
    public static boolean wrongTarget() { return false; }
    public static boolean missed() { return false; }
    public static void miss() {}
    public static int actualCost(int cost) { return cost; }
    public static float power(float damage) { return damage; }
public static int summonCount(int base) { return base; }
public static int formationSafetyBonus(int level) { return 0; }
public static double safetyBonus(net.minecraft.world.item.ItemStack stack) { return 0.0; }
public static double recoveryMultiplier(net.minecraft.world.item.ItemStack stack) { return 1.0; }
}
