package github.com.gengyoubo.sscfa.spell;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 鐟滆埇鍨洪埀顑跨瀹曟宕ョ仦鑺ュ渐闁绘挆宥囩Forge 缂佺姭鍋撻柛鏍ㄧ墪閻ゅ嫰鎮崇敮顔剧闁靛棗鍊风换姘舵偩濞嗘劖鐓㈡繛澶嬫礃缁侊妇绮欑€ｎ偄顣查梻鍥ｅ亾闁?Timing/plan/pay/effect 闁规亽鍎辫ぐ娑㈡晬? * 闁告艾瀚懜浼村箑娓氣偓閹藉ジ骞掗妷顖滅闁诡噯濡囨导?闁撅附鐟ㄥ?闁绘粌顑呭☉?闁告劒鍗冲﹢?閻犵偛婀辩亸?闁哄牆鐗忕划鎰版憘濞戝磭绀嗗☉鎾抽鐢偊鎮介悢鍝ャ€婇柛鏃€绋掗弫顔界濡櫣绐￠柤瀹犳婵繒浠﹂崒婧炩晛顪冨鍛€甸悶娑栧劚濞叉牠濡? */
public final class FormSpellStyle {
    private static final ThreadLocal<FormSpellStyle> EFFECT = new ThreadLocal<>();

    public static java.util.function.ToDoubleFunction<Player> clientRangeSnapshot = player -> Double.NaN;

    public interface HomingProjectile {
        void ssca$home(LivingEntity target, double range);
        Vec3 ssca$guidedVelocity(Vec3 ordinary);
        boolean ssca$isHoming();
        boolean ssca$hasRangeLimit();
        void ssca$flight(double speedMultiplier, double maximumRange);
        double ssca$flightSpeedMultiplier();
        void ssca$syncFlightVelocity();
    }

    public record Timing(int originalDuration, int originalPre, int preparation, int duration, int lockTick) {
        public static Timing of(int original, int adjusted, int lock, double multiplier) {
            original = Math.max(0, original);
            int before = lock < 0 ? original : Math.min(original, Math.max(0, lock));
            int tail = original - before;
            int pre = (int) Math.ceil(Math.max(0, adjusted - tail) * multiplier);
            return new Timing(original, before, pre, pre + tail, lock < 0 ? -1 : pre);
        }
        public int logical(int elapsed) { return (int) logical(elapsed, originalPre, preparation); }
        public static double logical(double elapsed, int originalPre, int preparation) {
            double t = Math.max(0, elapsed);
            return preparation > 0 && t < preparation ? t * originalPre / preparation
                    : originalPre + Math.max(0, t - preparation);
        }
    }

    public final ServerPlayer player;
    public final Timing timing;
    public final LivingEntity target;
    public final double range;
    public final double projectileRange;
    public final double collapseMultiplier;
    public final boolean novaQuickCast;
    private final int manaCost;
    private boolean paid;

    private FormSpellStyle(ServerPlayer player, Timing timing, LivingEntity target, double range,
                           double projectileRange, double collapseMultiplier, boolean novaQuickCast, int manaCost) {
        this.player = player;
        this.timing = timing;
        this.target = target;
        this.range = range;
        this.projectileRange = projectileRange;
        this.collapseMultiplier = collapseMultiplier;
        this.novaQuickCast = novaQuickCast;
        this.manaCost = Math.max(0, manaCost);
    }

    public static FormSpellStyle plan(ServerPlayer player, Spell spell, int level, int original, int adjusted) {
        return plan(player, spell.getProjectileRange(level), original, adjusted, spell.getLockInTick());
    }
    public static FormSpellStyle planTriggered(ServerPlayer player, Spell spell, int level, int original, int adjusted) {
        return new FormSpellStyle(player, Timing.of(original, adjusted, spell.getLockInTick(), 0), null,
                spell.getProjectileRange(level), 1.0, 1.0, false, 0);
    }
    public static FormSpellStyle plan(ServerPlayer player, double range, int original, int adjusted, int lock) {
        return new FormSpellStyle(player, Timing.of(original, adjusted, lock, 1.0), null,
                range, 1.0, 1.0, false, 0);
    }

    public boolean pay() { paid = true; return true; }
    public int paidMoisture() { return 0; }
    public void endCloak() {}
    public void bindAllay(double actual, Object record, boolean directCurse) {}
    public void bindBat(Object record) {}

    public void effect(Runnable action) {
        var previous = EFFECT.get();
        EFFECT.set(this);
        try { action.run(); } finally { if (previous == null) EFFECT.remove(); else EFFECT.set(previous); }
    }

    public static AllaySpellLink.Cast currentAllay() { return null; }
    public static Object currentBat() { return null; }
    public static ServerPlayer currentCaster() {
        var style = EFFECT.get();
        return style == null ? null : style.player;
    }

    public static void heal(ServerPlayer caster, LivingEntity target, float amount) { target.heal(amount); }

    public static double projectileRangeMultiplier(Player player) { return 1.0; }
    public static double projectileSpeedMultiplier(Player player) { return 1.0; }
    public static double projectileCollapseMultiplier(Player player) { return 1.0; }
    public static boolean canAccelerate(Player player) { return false; }
    public static double collapseMultiplier(Player player) { return 1.0; }
    public static double collapseMultiplier(Player player, boolean projectile) { return 1.0; }
    public static double collapseMultiplier(Player player, boolean projectile, boolean ice) { return 1.0; }
    public static double collapseMultiplier(Player player, boolean projectile, boolean ice, boolean debuff) { return 1.0; }
    public static double sheepProjectileRange() { return 40.0; }

    public static boolean legalTarget(ServerPlayer player, LivingEntity target) {
        return target != player && target.isAlive() && !target.isRemoved() && !target.isSpectator()
                && !(target instanceof ArmorStand) && target.level() == player.level()
                && !player.isAlliedTo(target)
                && (!(target instanceof ServerPlayer other) || player.canHarmPlayer(other))
                && !DomainManager.blocksTargeting(player, target);
    }

    public static LivingEntity findTarget(ServerPlayer player, double range) {
        if (!Double.isFinite(range) || range <= 0) return null;
        Vec3 eye = player.getEyePosition(), look = player.getViewVector(1).normalize();
        double bestCos = Math.cos(Math.toRadians(15)), bestDistance = Double.MAX_VALUE;
        LivingEntity best = null;
        for (var candidate : player.level().getEntitiesOfClass(LivingEntity.class, new AABB(eye, eye).inflate(range),
                e -> legalTarget(player, e))) {
            Vec3 delta = candidate.getBoundingBox().getCenter().subtract(eye);
            double distance = delta.lengthSqr();
            if (distance < 1e-8 || distance > range * range) continue;
            double cosine = look.dot(delta) / Math.sqrt(distance);
            if (cosine + 1e-10 < bestCos || Math.abs(cosine - bestCos) < 1e-10 && distance >= bestDistance) continue;
            if (!player.hasLineOfSight(candidate)) continue;
            bestCos = cosine; bestDistance = distance; best = candidate;
        }
        return best;
    }

    public static CourtSpellLink.Cast currentCourt() { return null; }
public static void register() {}
}
