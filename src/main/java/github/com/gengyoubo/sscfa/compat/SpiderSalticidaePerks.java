package github.com.gengyoubo.sscfa.compat;

import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.cooldown.SkillCastManager;
import github.com.gengyoubo.sscfa.cooldown.SkillCooldowns;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.FormUtils;
import github.com.gengyoubo.sscfa.resource.ResourceBars;
import github.com.gengyoubo.sscfa.util.PowerUtils;
import github.com.gengyoubo.sscfa.util.WhitelistUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** 跳蛛共用原蜘蛛购买记录，效果接入跳杀和毒液，不引入蛛网法力。 */
public final class SpiderSalticidaePerks {
    public static final ResourceLocation FEEDING_1 = addon("perk_spider_salticidae_feeding_1");
    public static final ResourceLocation FEEDING_2 = addon("perk_spider_salticidae_feeding_2");
    public static final ResourceLocation CHARGE = addon("perk_spider_salticidae_charge");
    public static final ResourceLocation SENSE = addon("perk_spider_salticidae_sense");
    public static final ResourceLocation SENSE_CHARGING = addon("perk_spider_salticidae_sense_charging");
    public static final ResourceLocation RAPID = addon("perk_spider_salticidae_rapid");
    public static final ResourceLocation STABLE = addon("perk_spider_salticidae_stable");
    public static final ResourceLocation RECOVERY_1 = addon("perk_spider_salticidae_recovery_1");
    public static final ResourceLocation RECOVERY_2 = addon("perk_spider_salticidae_recovery_2");
    private static final double KILL_HEAL_1 = 2.0, KILL_HEAL_2 = 4.0;
    private static final double CHARGE_TIME_MULTIPLIER = 0.7;
    private static final double JUMP_COOLDOWN_MULTIPLIER_1 = 0.85, JUMP_COOLDOWN_MULTIPLIER_2 = 0.75;
    private static final double VENOM_COOLDOWN_MULTIPLIER = 0.7;
    private static final double VENOM_DAMAGE_MULTIPLIER = 1.25;
    private static final int STRENGTH_TICKS = 60;
    private static final BalanceReader BAL = new BalanceReader("abilities.salticidae_perks");

    private SpiderSalticidaePerks() { }

    /**
     * Jumping spider form. The Fabric version also remapped SSC's spider perk purchases to the
     * addon's perk powers ({@code loadMapped}); SSC Forge applies perk powers per perk tree and no
     * addon tree is registered for this form yet, so until then only the base values apply.
     */
    private static boolean isSupported(Player player) {
        return player != null && FormUtils.isForm(player, FormIdentifiers.SPIDER_SALTICIDAE);
    }
    public static boolean enabled(Player player, ResourceLocation power) {
        return isSupported(player) && ResourceBars.hasPowerId(player, power);
    }

    public static int chargeTicks(Player player, int base) {
        return Math.max(1, scaleTicks(base, enabled(player, CHARGE) ? BAL.d("charge_time_multiplier", CHARGE_TIME_MULTIPLIER) : 1.0));
    }

    public static float killHealing(Player player) {
        return (float) (enabled(player, FEEDING_2) ? BAL.d("kill_heal_2", KILL_HEAL_2)
                : enabled(player, FEEDING_1) ? BAL.d("kill_heal_1", KILL_HEAL_1) : 0.0);
    }

    public static int strengthTicks(Player player) {
        return enabled(player, SENSE) ? BAL.i("strength_ticks", STRENGTH_TICKS) : 0;
    }

    public static double venomMultiplier(Player player) {
        return enabled(player, STABLE) ? BAL.d("venom_damage_multiplier", VENOM_DAMAGE_MULTIPLIER) : 1.0;
    }

    /** 在统一服务中按本次购买状态冻结成功／失败冷却，保留JSON起算模式。 */
    public static boolean beginSkill(ServerPlayer player, String skill, boolean venom) {
        if (!isSupported(player)) return false;
        var spec = SkillCooldowns.spec(player, skill);
        if (spec == null) return false;
        double multiplier = venom ? (enabled(player, RAPID) ? BAL.d("venom_cooldown_multiplier", VENOM_COOLDOWN_MULTIPLIER) : 1.0)
                : enabled(player, RECOVERY_2) ? BAL.d("jump_cooldown_multiplier_2", JUMP_COOLDOWN_MULTIPLIER_2)
                : enabled(player, RECOVERY_1) ? BAL.d("jump_cooldown_multiplier_1", JUMP_COOLDOWN_MULTIPLIER_1) : 1.0;
        var original = spec.resolve();
        var config = new SkillCastManager.ResolvedConfig(scaleTicks(original.cooldown, multiplier),
                scaleTicks(original.failCooldown, multiplier), original.cooldownStart);
        return SkillCastManager.get(player.serverLevel()).begin(player, skill, config) >= 0;
    }

    /** 仅同步蓄力状态变化；也会清除重连后留下的旧资源值。 */
    public static void setSensing(ServerPlayer player, boolean charging) {
        if (!enabled(player, SENSE_CHARGING)) return;
        int value = charging ? 1 : 0;
        if (PowerUtils.getResourceValue(player, SENSE_CHARGING) != value) PowerUtils.setResourceValueAndSync(player, SENSE_CHARGING, value);
    }

    public static void rewardDirectHit(ServerPlayer player, LivingEntity target, float healthLost, float healing, int strengthTicks) {
        if (healthLost <= 0 || !isSupported(player) || WhitelistUtils.isProtected(player, target)) return;
        if (strengthTicks > 0) player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, strengthTicks, 1));
        if (healing > 0 && !target.isAlive()) player.heal(healing);
    }

    private static int scaleTicks(int base, double multiplier) { return Math.max(0, (int) Math.ceil(base * multiplier)); }
    private static ResourceLocation addon(String path) { return new ResourceLocation("my_addon", path); }
}
