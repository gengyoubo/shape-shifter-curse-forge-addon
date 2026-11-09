package github.com.gengyoubo.sscfa.ability;

import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.util.FormUtils;
import github.com.gengyoubo.sscfa.util.WhitelistUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 椋庣伒琚姩銆岃惤鍦伴娑屻€嶃€?
 *
 * <p>鐜╁鏄捐憲婊炵┖鍚庯紙fallDistance 鈮?1.5锛夎惤鍦版椂锛岃嫢鏈婊炵┖鏈熼棿<b>鏈敾鍑诲懡涓换浣曟晫浜?/b>锛?
 * 瑙﹀彂涓€娆¤惤鍦板啿鍑伙細3 鏍煎崐寰勫唴閫犳垚 6 鐐圭墿鐞嗕激瀹?+ 鍑婚€€銆? 绉掑唴缃?CD銆?
 * 鑻ユ粸绌烘湡闂村懡涓繃鏁屼汉锛堝緬鎵嬭繎鎴?/ 鐤鹃杩炵埅 / 寮瑰皠鐗╁懡涓潎绠楋級锛屽垯钀藉湴涓嶈Е鍙戯紙榧撳姳绌轰腑鎴樻枟锛夈€?
 *
 * <p>鏈哄埗锛?
 * <ul>
 *   <li>{@code ServerLivingEntityEvents.ALLOW_DAMAGE}锛氶鐏靛湪绌轰腑閫犳垚浼ゅ 鈫?鏍囪 hitDuringAir</li>
 *   <li>鏈嶅姟绔?tick锛歰nGround 涓嬮檷娌?+ 涓婁竴 tick fallDistance 鈮?1.5 + CD 灏辩华 鈫?瑙﹀彂椋庢秾</li>
 * </ul>
 * 鍒ゅ畾鍏ㄦ湇鍔＄锛岀矑瀛愬箍鎾紝澶氫汉涓€鑷淬€?
 */
public final class WindSpiritLandingSurgeManager {

    // 浠ヤ笅鍧囦负榛樿鍊硷紱杩愯鏃朵粠 balance 蹇収璇诲彇锛坅bilities.wind_landing_surge锛?
    private static final double RADIUS = 3.0;
    private static final float DAMAGE = 6.0f;
    private static final int COOLDOWN_TICKS = 100;          // 5 绉?
    private static final float MIN_FALL_DISTANCE = 1.5f;     // 鏄捐憲婊炵┖闃堝€?

    // 闃舵 5锛氭湇鍔＄鏉冨▉蹇収璇诲彇锛堝揩鐓ф湭鍒濆鍖栧洖閫€榛樿甯搁噺锛?
    private static final BalanceReader BAL = new BalanceReader("abilities.wind_landing_surge");

    /** 姣忕帺瀹朵笂涓€ tick 鐨?onGround 鐘舵€侊紙鐢ㄤ簬妫€娴嬩笅闄嶆部锛夈€?*/
    private static final Map<UUID, Boolean> PREV_ON_GROUND = new ConcurrentHashMap<>();
    /** 姣忕帺瀹朵笂涓€ tick 鐨?fallDistance锛堣惤鍦扮灛闂?MC 浼氭竻闆讹紝鏁呴渶璁?prev锛夈€?*/
    private static final Map<UUID, Float> PREV_FALL_DISTANCE = new ConcurrentHashMap<>();
    /** 姣忕帺瀹朵笅娆″彲瑙﹀彂椋庢秾鐨勬湇鍔＄ tick锛圕D 鏃堕棿鎴筹級銆?*/
    private static final Map<UUID, Long> NEXT_AVAILABLE_TICK = new ConcurrentHashMap<>();
    /** 鏈婊炵┖鏈熼棿鏄惁鏀诲嚮鍛戒腑杩囨晫浜猴紙true 鍒欒惤鍦颁笉瑙﹀彂锛夈€?*/
    private static final Map<UUID, Boolean> HIT_DURING_AIR = new ConcurrentHashMap<>();

    private static boolean registered = false;

    private WindSpiritLandingSurgeManager() {
    }

    /** 鍦?mod 鍒濆鍖栨椂璋冪敤涓€娆★細娉ㄥ唽浼ゅ浜嬩欢鐩戝惉锛堣拷韪┖涓懡涓級銆?*/
    public static synchronized void register() {
        if (registered) return;
        registered = true;
        github.com.gengyoubo.sscfa.util.SscEvents.allowDamage((entity, source, amount) -> {
            handleDamageDealt(entity, source);
            return true; // 涓嶆嫤鎴激瀹?
        });
    }

    /** 浼ゅ缁撶畻鏃讹細鑻ユ敾鍑绘柟鏄鐏典笖鍦ㄧ┖涓紝鏍囪鏈婊炵┖鍛戒腑杩囥€?*/
    private static void handleDamageDealt(LivingEntity target, DamageSource source) {
        Entity attacker = source.getEntity();
        if (!(attacker instanceof ServerPlayer player)) return;
        if (!FormUtils.isOcelotSP(player)) return;
        if (player.onGround()) return; // 涓嶅湪绌轰腑鍒欏拷鐣?
        // 鍦ㄧ┖涓€犳垚浼ゅ 鈫?鏍囪
        HIT_DURING_AIR.put(player.getUUID(), Boolean.TRUE);
    }

    /** 姣忔湇鍔＄ tick 瀵规瘡涓湪绾跨帺瀹惰皟鐢紙鐢?SscAddon tick 寰幆椹卞姩锛夈€?*/
    public static void tick(ServerPlayer player) {
        // 褰㈡€侀棬鎺у墠缃細闈為鐏电帺瀹朵笉鍐嶆瘡 tick 璇诲啓鍥涘紶鍏ㄥ憳宸ヨ〃锛?
        // 鍒囧嚭褰㈡€佹椂椤哄甫娓呯悊鏈帺瀹剁姸鎬侊紙鍒囧洖鏃?prev 鐘舵€佺己澶?= 棣栨媿涓嶈Е鍙戯紝瀹夊叏锛夈€?
        if (!FormUtils.isOcelotSP(player)) {
            UUID uuid = player.getUUID();
            PREV_ON_GROUND.remove(uuid);
            PREV_FALL_DISTANCE.remove(uuid);
            HIT_DURING_AIR.remove(uuid);
            return;
        }
        UUID uuid = player.getUUID();
        boolean curOnGround = player.onGround();
        Boolean prevOnGroundObj = PREV_ON_GROUND.get(uuid);
        float curFall = player.fallDistance;
        Float prevFallObj = PREV_FALL_DISTANCE.get(uuid);

        // 璧疯烦鐬棿锛堜粠鍦伴潰鍒扮┖涓級锛氭竻闄ゅ懡涓爣璁帮紝寮€濮嬫柊涓€杞粸绌鸿拷韪?
        if (prevOnGroundObj != null && prevOnGroundObj && !curOnGround) {
            HIT_DURING_AIR.remove(uuid);
        }

        // 钀藉湴涓嬮檷娌匡細prev 鍦ㄧ┖涓?鈫?cur 鍦ㄥ湴闈?
        if (prevOnGroundObj != null && !prevOnGroundObj && curOnGround) {
            float prevFall = prevFallObj != null ? prevFallObj : 0f;
            if (prevFall >= (float) BAL.d("min_fall_distance", MIN_FALL_DISTANCE)) {
                tryTriggerSurge(player, uuid);
            }
            // 钀藉湴鍚庢竻鐞嗘湰娆℃粸绌虹姸鎬?
            HIT_DURING_AIR.remove(uuid);
        }

        // 璁板綍鏈?tick 鐘舵€佷緵涓嬩竴 tick 鐢?
        PREV_ON_GROUND.put(uuid, curOnGround);
        PREV_FALL_DISTANCE.put(uuid, curFall);
    }

    /** 灏濊瘯瑙﹀彂钀藉湴椋庢秾锛堝惈 CD / 绌轰腑鍛戒腑妫€鏌ワ級銆?*/
    private static void tryTriggerSurge(ServerPlayer player, UUID uuid) {
        // 闈為鐏典笉瑙﹀彂
        if (!FormUtils.isOcelotSP(player)) return;

        ServerLevel world = (ServerLevel) player.level();
        long now = world.getGameTime();

        // CD 妫€鏌?
        Long next = NEXT_AVAILABLE_TICK.get(uuid);
        if (next != null && now < next) return;

        // 鏈婊炵┖鏈熼棿鍛戒腑杩囨晫浜?鈫?涓嶈Е鍙戯紙榧撳姳绌轰腑鎴樻枟锛?
        if (Boolean.TRUE.equals(HIT_DURING_AIR.get(uuid))) return;

        // 瑙﹀彂椋庢秾锛欰OE
        double radius = BAL.d("radius", RADIUS);
        Vec3 center = player.position();
        AABB box = new AABB(center.subtract(radius, radius, radius), center.add(radius, radius, radius));
        float damage = (float) BAL.d("damage", DAMAGE);
        for (Entity e : world.getEntities(player, box)) {
            if (!(e instanceof LivingEntity living)) continue;
            if (living == player) continue;
            if (WhitelistUtils.isProtected(player, living)) continue; // 榛樿鐧藉悕鍗?
            // 瑙嗙嚎妫€鏌ワ細澧欏悗鐩爣涓嶅懡涓紙浠垮師鐗?PR #523 璞圭尗鍐插埡绌垮淇锛?
            if (!github.com.gengyoubo.sscfa.util.LineOfSightUtils.hasLineOfSight(world, player, living)) continue;
            living.hurt(player.damageSources().playerAttack(player), damage);
            Vec3 push = living.position().subtract(center);
            if (push.lengthSqr() < 1.0e-4) push = new Vec3(0, 1, 0);
            push = push.normalize();
            living.knockback(0.5, -push.x, -push.z);
        }

        // 绮掑瓙锛氱幆褰㈠啿鍑绘尝 + 鍚戜笂鎵皹
        github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(world, player, ParticleTypes.POOF, center.x, center.y + 0.1, center.z,
                20, radius * 0.5, 0.1, radius * 0.5, 0.08);
        github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(world, player, ParticleTypes.CLOUD, center.x, center.y + 0.2, center.z,
                16, radius * 0.6, 0.15, radius * 0.6, 0.06);
        github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(world, player, ParticleTypes.SWEEP_ATTACK, center.x, center.y + 0.5, center.z,
                4, radius * 0.3, 0.2, radius * 0.3, 0.0);

        // 闊虫晥锛堝叏鍛樺彲鍚級
        world.playSound(null, center.x, center.y, center.z,
                SoundEvents.PLAYER_BIG_FALL, SoundSource.PLAYERS, 0.6f, 1.3f);
        world.playSound(null, center.x, center.y, center.z,
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.4f, 1.5f);

        // 璁剧疆 CD
        NEXT_AVAILABLE_TICK.put(uuid, now + BAL.i("cooldown_ticks", COOLDOWN_TICKS));
    }

    public static void onPlayerDisconnect(ServerPlayer player) {
        UUID uuid = player.getUUID();
        PREV_ON_GROUND.remove(uuid);
        PREV_FALL_DISTANCE.remove(uuid);
        NEXT_AVAILABLE_TICK.remove(uuid);
        HIT_DURING_AIR.remove(uuid);
    }
}
