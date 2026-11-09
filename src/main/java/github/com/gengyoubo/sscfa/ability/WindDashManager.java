package github.com.gengyoubo.sscfa.ability;

import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.compat.WindSpiritPerks;
import github.com.gengyoubo.sscfa.cooldown.SkillCooldowns;
import github.com.gengyoubo.sscfa.network.SscAddonNetworking;
import github.com.gengyoubo.sscfa.util.FormUtils;
import github.com.gengyoubo.sscfa.util.WhitelistUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 椋庣伒銆岄涔嬪啿鍒恒€嶄富鎶€鑳斤紙sp_primary锛夆€斺€?鍥涢樁娈电姸鎬佹満銆?
 *
 * <p>绗?1 娆℃寜涓绘妧鑳介敭锛?
 * <ul>
 *   <li><b>RISE 璧烽</b>锛氬瀭鐩翠笂鍗囪嚦 5 鏍奸珮銆?-3 鏍间繚鎸佽緝蹇€熷害锛堚増0.45 鏍?tick锛夛紝
 *       3-5 鏍肩嚎鎬у噺閫熷埌 0銆傝剼涓嬬伆鐑燂紙CAMPFIRE_COSY_SMOKE锛? 鐑熻姳涓婂崌绮掑瓙锛團IREWORK锛夈€?
 *       涓婃柟鏈夋柟鍧楀垯椤跺ご鍋滃湪鏂瑰潡涓嬫柟锛堜笉绌垮锛夈€?/li>
 *   <li><b>HOVER 鎮诞</b>锛氬埌椤跺悗瀹氫綇 3 绉掞紝鑴氫笅鎸佺画鐏扮儫锛涘悓鏃跺鎴风鏄剧ず缁胯壊钀界偣鎸囩ず鍣?
 *       锛堝噯鏄熸按骞虫柟鍚戞渶杩?16 鏍硷紝鍗婂緞 3 鏍硷級銆?/li>
 * </ul>
 * <p>鎮诞涓 2 娆℃寜涓绘妧鑳介敭锛?
 * <ul>
 *   <li><b>DASH 鍐插埡</b>锛氱洿绾垮揩閫熷啿鍒拌惤鐐逛腑蹇冦€傝捣鐐瑰弽鏂瑰悜鍐掔櫧绮掑瓙锛圕LOUD锛夛紝
 *       璺緞鐧界矑瀛愭嫋灏俱€傚啿鍒鸿繃绋嬩腑涓嶄骇鐢熺伆鐑熴€?/li>
 *   <li><b>LAND 钀藉湴</b>锛氳惤鐐?3 鏍煎崐寰勫唴 12 鐐圭墿鐞嗕激瀹?+ 鏂瑰潡瑙嗚娴姩锛堢矑瀛愭ā鎷燂級锛?
 *       杩涘叆 12 绉?CD銆?/li>
 * </ul>
 * <p>鎮诞 3 绉掕秴鏃舵湭鎸夐敭锛?b>FALL 缂撴參钀戒笅</b>锛? 鏍?绉掞級锛岃惤鍦版棤浼ゅ浣嗚繘鍏?CD銆?
 *
 * <p>鎵€鏈夊垽瀹氬湪鏈嶅姟绔紝绮掑瓙閫氳繃 ServerWorld 骞挎挱锛岃惤鐐归瑙堢函瀹㈡埛绔悇鑷绠椼€?
 * 浼ゅ AOE 榛樿鐧藉悕鍗曪紙{@code my_addon:not_actor_whitelisted} 瀵瑰簲 WhitelistUtils锛夈€?
 */
public final class WindDashManager {

    // ===== 甯搁噺锛堝潎涓洪粯璁ゅ€硷紱杩愯鏃朵粠 balance 蹇収璇诲彇 abilities.wind_dash锛?=====
    private static final float TARGET_HEIGHT = 5.0f;
    private static final double RISE_FAST_SPEED = 0.45;      // 0-3 鏍奸樁娈甸€熷害锛堟牸/tick锛?
    private static final int RISE_FAST_TICKS = 7;            // 0-3 鏍肩害 7 tick
    private static final int RISE_SLOW_TICKS = 10;           // 3-5 鏍煎噺閫熼樁娈电害 10 tick
    private static final int HOVER_TICKS = 60;               // 鎮诞 3 绉?
    private static final double DASH_SPEED = 1.5;            // 鍐插埡閫熷害锛堟牸/tick锛屾瀬蹇級
    private static final double MAX_DASH_RANGE = 16.0;
    private static final double LANDING_RADIUS = 3.0;
    private static final float LANDING_DAMAGE = 12.0f;
    // CD锛?2 绉掞級鐢辩粺涓€鍐峰嵈鏈嶅姟 + power JSON 閰嶇疆绠＄悊锛屽師 COOLDOWN_TICKS 甯搁噺宸插垹
    private static final double FALL_SPEED = 0.15;           // 3 鏍?绉?鈮?0.15 鏍?tick

    // 闃舵 5锛氭湇鍔＄鏉冨▉蹇収璇诲彇锛堝揩鐓ф湭鍒濆鍖栧洖閫€榛樿甯搁噺锛?
    private static final BalanceReader BAL = new BalanceReader("abilities.wind_dash");
    /** 缁熶竴鍐峰嵈鏈嶅姟鐨勭ǔ瀹氭妧鑳?ID锛堢綉缁滃寘瑙﹀彂鍨嬶級銆?*/
    private static final String SKILL_ID = "my_addon:form_ocelot_wind_spirit_wind_dash";

    // ===== 闃舵 =====
    public static final int PHASE_NONE = 0;
    public static final int PHASE_RISE = 1;
    public static final int PHASE_HOVER = 2;
    public static final int PHASE_DASH = 3;
    public static final int PHASE_FALL = 4;

    private static final Map<UUID, DashState> STATES = new ConcurrentHashMap<>();

    public static final class DashState {
        boolean enhanced;
        double maxRange, landingRadius;
        float landingDamage;
        long castId = -1;
        int phase = PHASE_NONE;
        double startY;          // 璧烽鍓嶈剼閮?Y
        double targetY;         // 鐩爣鎮诞 Y锛堝彲鑳藉洜椤跺ご鑰屼綆浜?startY+5锛?
        int riseTick = 0;       // 璧烽闃舵 tick 璁℃暟
        int hoverTick = 0;      // 鎮诞宸叉寔缁?tick
        Vec3 dashTarget = null; // 鍐插埡钀界偣
        double dashTraveled = 0.0;
        double dashTotal = 0.0;
        Vec3 dashStart = null;
        Vec3 dashDir = null;
        boolean dashed = false; // 鏄惁宸茶繘鍏ヨ繃鍐插埡锛堝尯鍒?鍐插埡钀藉湴"涓?瓒呮椂钀戒笅钀藉湴"锛?
    }

    private WindDashManager() {
    }

    /** 瀹㈡埛绔寜涓绘妧鑳介敭锛堟棤 payload锛夈€傛湇鍔＄鏍规嵁褰撳墠闃舵鍒嗘敮銆?*/
    public static void onKeyPress(ServerPlayer player) {
        if (!FormUtils.isOcelotSP(player)) {
            clear(player);
            return;
        }
        DashState s = STATES.get(player.getUUID());
        if (s == null || s.phase == PHASE_NONE) {
            if (!SkillCooldowns.begin(player, SKILL_ID)) return;
            startRise(player);
            SkillCooldowns.released(player, SKILL_ID);
        } else if (s.phase == PHASE_HOVER) {
            // 鎮诞涓 2 娆℃寜閿?鈫?鍐插埡
            startDash(player, s);
        }
        // RISE/DASH/FALL 鏈熼棿鎸夐敭蹇界暐
    }

    /** 寮€濮嬭捣椋炪€?*/
    private static void startRise(ServerPlayer player) {
        DashState s = new DashState();
        s.castId = SkillCooldowns.currentCastId(player, SKILL_ID);
        s.enhanced = WindSpiritPerks.hasLongPounce(player);
        s.maxRange = WindSpiritPerks.dashRange(BAL.d("max_dash_range", MAX_DASH_RANGE), s.enhanced);
        s.landingRadius = WindSpiritPerks.landingRadius(BAL.d("landing_radius", LANDING_RADIUS), s.enhanced);
        s.landingDamage = WindSpiritPerks.landingDamage((float) BAL.d("landing_damage", LANDING_DAMAGE), s.enhanced);
        s.phase = PHASE_RISE;
        double targetHeight = BAL.d("target_height", TARGET_HEIGHT);
        s.startY = player.getY();
        s.targetY = s.startY + targetHeight;
        // 椤跺ご妫€娴嬶細浠庤剼閮ㄥ悜涓?raycast锛堢洰鏍囬珮 +0.5 鏍硷級锛岀鍒扮殑鏂瑰潡涓嬫柟鍗充负鍙揪鏈€楂樼偣
        double headClear = checkVerticalClearance(player, targetHeight + 0.5);
        if (headClear < targetHeight) {
            // 涓婃柟鏈夋柟鍧楅樆鎸★紝鐩爣闄嶄负纰伴《澶勶紙鏂瑰潡涓嬫柟锛?
            s.targetY = s.startY + Math.max(0.5, headClear - 0.05);
        }
        STATES.put(player.getUUID(), s);
        ServerLevel sw = (ServerLevel) player.level();
        // 璧烽闊虫晥锛堝叏鍛樺彲鍚級锛氱敤鐭績涓婂崌闊筹紝閬垮厤寰幆/闀块煶鍦ㄨ惤鍦板悗娈嬬暀 1-2 绉?
        sw.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 0.9f, 0.8f);
        sw.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 0.5f, 1.8f);
        // 鍚屾闃舵缁欏鎴风锛堥┍鍔ㄨ惤鐐归瑙?闊虫晥锛?
        SscAddonNetworking.syncDashState(player, s.phase, s.targetY, s.maxRange, s.landingRadius);
    }

    /** 鎮诞涓Е鍙戝啿鍒猴細3D 鍑嗘槦灏勭嚎鎵捐惤鐐癸紙鐬勮繎澶勫湴闈?杩戣惤鐐癸紝鐬勮繙澶?杩滆惤鐐癸紝鐪嬪ぉ绌洪€€鍒?6鏍硷級銆?*/
    private static void startDash(ServerPlayer player, DashState s) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        ServerLevel sw = (ServerLevel) player.level();
        // 3D 鍑嗘槦灏勭嚎鎵捐惤鐐癸紙涓庡鎴风棰勮 computeHorizontalLanding 瀹屽叏涓€鑷达紝淇濊瘉鎵€瑙佸嵆鎵€寰楋級
        s.dashTarget = computeDashLanding(sw, eye, look, player, s.maxRange);
        s.dashStart = new Vec3(player.getX(), player.getY(), player.getZ());
        s.dashDir = s.dashTarget.subtract(s.dashStart).normalize();
        s.dashTotal = Math.max(1.0, s.dashStart.distanceTo(s.dashTarget));
        s.dashTraveled = 0.0;
        s.phase = PHASE_DASH;
        s.dashed = true;

        // 璧风偣鍙嶆柟鍚戠櫧绮掑瓙锛堝悜鍐插埡鍙嶆柟鍚戝柗鍑猴級
        Vec3 back = s.dashDir.scale(-1);
        github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(sw, player, ParticleTypes.CLOUD, player.getX(), player.getY() + 1.0, player.getZ(),
                25, 0.2, 0.3, 0.2, 0.02);
        // 缁欏弽鏂瑰悜绮掑瓙涓€涓€熷害锛堢敤 spawnParticles 鐨勯€熷害鍒嗛噺杩戜技锛?
        for (int i = 0; i < 15; i++) {
            github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(sw, player, ParticleTypes.CLOUD,
                    player.getX() + back.x * 0.5, player.getY() + 1.0, player.getZ() + back.z * 0.5,
                    1, 0.1, 0.1, 0.1, 0.0);
        }
        // 鍐插埡闊虫晥
        sw.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.8f);
        sw.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 0.6f, 1.4f);
        SscAddonNetworking.syncDashState(player, s.phase, 0, s.maxRange, s.landingRadius);
    }

    /** 姣忔湇鍔＄ tick 瀵规瘡涓湪绾跨帺瀹惰皟鐢ㄣ€?*/
    public static void tick(ServerPlayer player) {
        DashState s = STATES.get(player.getUUID());
        if (s == null) return;

        // 姝讳骸 / 闈為鐏?鈫?娓呯悊骞惰鐜╁鑷劧钀戒笅
        if (player.isDeadOrDying() || !FormUtils.isOcelotSP(player)) {
            clear(player);
            return;
        }

        switch (s.phase) {
            case PHASE_RISE -> tickRise(player, s);
            case PHASE_HOVER -> tickHover(player, s);
            case PHASE_DASH -> tickDash(player, s);
            case PHASE_FALL -> tickFall(player, s);
            default -> {}
        }
    }

    private static void tickRise(ServerPlayer player, DashState s) {
        s.riseTick++;
        ServerLevel sw = (ServerLevel) player.level();

        // 璁＄畻鏈?tick 搴斿埌杈剧殑 Y
        double curY = player.getY();
        double dy = s.targetY - curY;
        double stepY;
        if (dy <= 0) {
            // 宸插埌椤?
            enterHover(player, s);
            return;
        }
        double traveledFromStart = curY - s.startY;
        double riseFastSpeed = BAL.d("rise_fast_speed", RISE_FAST_SPEED);
        if (traveledFromStart < 3.0) {
            // 0-3 鏍煎揩閫?
            stepY = Math.min(riseFastSpeed, dy);
        } else {
            // 3-5 鏍肩嚎鎬у噺閫燂細浠?rise_fast_speed 绾挎€у埌 0
            float t = (float) Mth.clamp((traveledFromStart - 3.0) / 2.0, 0.0, 1.0);
            stepY = riseFastSpeed * (1.0f - t);
            stepY = Math.min(stepY, dy);
            if (stepY < 0.02) stepY = dy; // 鏀跺熬锛岄伩鍏嶆姈鍔?
        }

        // 搴旂敤浣嶇Щ锛堝瀭鐩达紝淇濇寔 x/z锛?
        player.setDeltaMovement(0, stepY, 0);
        player.hurtMarked = true;
        player.setPos(player.getX(), curY + stepY, player.getZ());

        // 鑴氫笅鐏扮儫 + 鐑熻姳涓婂崌绮掑瓙
        spawnRiseParticles(sw, player);

        if (curY + stepY >= s.targetY - 0.05
                || s.riseTick > BAL.i("rise_fast_ticks", RISE_FAST_TICKS) + BAL.i("rise_slow_ticks", RISE_SLOW_TICKS) + 5) {
            enterHover(player, s);
        }
    }

    private static void enterHover(ServerPlayer player, DashState s) {
        // 绮剧‘钀藉埌 targetY
        player.setPos(player.getX(), s.targetY, player.getZ());
        player.setDeltaMovement(0, 0, 0);
        player.hurtMarked = true;
        s.phase = PHASE_HOVER;
        s.hoverTick = 0;
        SscAddonNetworking.syncDashState(player, s.phase, s.targetY, s.maxRange, s.landingRadius);
    }

    private static void tickHover(ServerPlayer player, DashState s) {
        s.hoverTick++;
        ServerLevel sw = (ServerLevel) player.level();
        // 瀹氫綇浣嶇疆锛堝鎶楅噸鍔涳級
        player.setDeltaMovement(0, 0, 0);
        player.hurtMarked = true;
        // 鑴氫笅鎸佺画鐏扮儫
        github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(sw, player, ParticleTypes.CAMPFIRE_COSY_SMOKE,
                player.getX(), player.getY() - 0.1, player.getZ(),
                3, 0.25, 0.0, 0.25, 0.005);

        if (s.hoverTick >= BAL.i("hover_ticks", HOVER_TICKS)) {
            // 瓒呮椂 鈫?缂撴參钀戒笅
            s.phase = PHASE_FALL;
            SscAddonNetworking.syncDashState(player, s.phase, 0, s.maxRange, s.landingRadius);
        }
    }

    private static void tickDash(ServerPlayer player, DashState s) {
        ServerLevel sw = (ServerLevel) player.level();
        double dashSpeed = BAL.d("dash_speed", DASH_SPEED);
        double step = Math.min(dashSpeed, s.dashTotal - s.dashTraveled);
        if (step <= 0) {
            enterLand(player, s, true);
            return;
        }
        s.dashTraveled += step;
        Vec3 next = s.dashStart.add(s.dashDir.scale(s.dashTraveled));
        // 鐩寸嚎鍐插埡涓€旂鎾炴娴嬶細鑻ヤ腑閫旈亣澧欙紝鎻愬墠钀藉湴
        if (!isClearPath(sw, new Vec3(player.getX(), player.getY(), player.getZ()), next)) {
            enterLand(player, s, true);
            return;
        }
        player.setDeltaMovement(s.dashDir.x * dashSpeed, s.dashDir.y * dashSpeed, s.dashDir.z * dashSpeed);
        player.hurtMarked = true;
        player.setPos(next.x, next.y, next.z);

        // 璺緞鐧界矑瀛愭嫋灏撅紙鍐插埡涓棤鐏扮儫锛?
        github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(sw, player, ParticleTypes.CLOUD,
                player.getX(), player.getY() + 0.5, player.getZ(),
                4, 0.15, 0.15, 0.15, 0.0);
        github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(sw, player, ParticleTypes.CLOUD,
                player.getX(), player.getY() + 1.2, player.getZ(),
                3, 0.1, 0.1, 0.1, 0.0);

        if (s.dashTraveled >= s.dashTotal - 0.05) {
            enterLand(player, s, true);
        }
    }

    private static void tickFall(ServerPlayer player, DashState s) {
        ServerLevel sw = (ServerLevel) player.level();
        // 3 鏍?绉?= 0.15 鏍?tick 缂撴參涓嬭惤
        double stepY = BAL.d("fall_speed", FALL_SPEED);
        double curY = player.getY();
        // 妫€娴嬭剼涓嬫柟鍧楋紝钀藉湴鍒欑粨鏉?
        double groundY = findGroundY(sw, player.getX(), player.getZ(), curY);
        if (curY - stepY <= groundY + 0.01) {
            // 钀藉湴锛堣秴鏃惰惤涓嬶細鏃犱激瀹充絾鏈?CD锛?
            player.setPos(player.getX(), groundY, player.getZ());
            player.setDeltaMovement(0, 0, 0);
            player.hurtMarked = true;
            // 钀藉湴灏忕矑瀛?
            github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(sw, player, ParticleTypes.CLOUD, player.getX(), player.getY(), player.getZ(),
                    10, 0.3, 0.0, 0.3, 0.02);
            sw.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.SAND_PLACE, SoundSource.PLAYERS, 0.5f, 1.2f);
            enterCooldown(player);
            clear(player);
            return;
        }
        player.setDeltaMovement(0, -stepY, 0);
        player.hurtMarked = true;
        player.setPos(player.getX(), curY - stepY, player.getZ());
        // 涓嬭惤鏃惰剼涓嬬伆鐑?
        github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(sw, player, ParticleTypes.CAMPFIRE_COSY_SMOKE,
                player.getX(), player.getY() - 0.1, player.getZ(),
                2, 0.2, 0.0, 0.2, 0.005);
    }

    /** 鍐插埡钀藉湴锛氶€犳垚浼ゅ + 鏂瑰潡瑙嗚娴姩 + 杩?CD銆?*/
    private static void enterLand(ServerPlayer player, DashState s, boolean withDamage) {
        ServerLevel sw = (ServerLevel) player.level();
        Vec3 land = s.dashTarget != null ? s.dashTarget : player.position();
        player.setPos(land.x, land.y, land.z);
        player.setDeltaMovement(0, 0, 0);
        player.hurtMarked = true;

        // 钀藉湴闊虫晥锛堝叏鍛樺彲鍚級
        sw.playSound(null, land.x, land.y, land.z,
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.7f, 1.4f);
        sw.playSound(null, land.x, land.y, land.z,
                SoundEvents.PLAYER_BIG_FALL, SoundSource.PLAYERS, 0.8f, 0.8f);

        if (withDamage) {
            // AOE 浼ゅ锛堥粯璁ょ櫧鍚嶅崟锛?
            double landingRadius = s.landingRadius;
            AABB box = new AABB(land.subtract(landingRadius, landingRadius, landingRadius),
                    land.add(landingRadius, landingRadius, landingRadius));
            for (Entity e : sw.getEntities(player, box)) {
                if (!(e instanceof LivingEntity living)) continue;
                if (WhitelistUtils.isProtected(player, living)) continue; // 榛樿鐧藉悕鍗?
                // 瑙嗙嚎妫€鏌ワ細澧欏悗鐩爣涓嶅懡涓紙浠垮師鐗?PR #523 璞圭尗鍐插埡绌垮淇锛?
                if (!github.com.gengyoubo.sscfa.util.LineOfSightUtils.hasLineOfSight(sw, player, living)) continue;
                boolean hit = living.hurt(player.damageSources().playerAttack(player),
                        NpcCombatEnhancements.scale(player, living, s.landingDamage, "wind_primary_mul", 1.5));
                if (hit && s.enhanced) living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 0), player);
                Vec3 push = living.position().subtract(land);
                if (push.lengthSqr() < 1.0e-4) push = new Vec3(0, 1, 0);
                push = push.normalize();
                living.knockback(0.6, -push.x, -push.z);
            }
            // 钀藉湴鐖嗗彂绮掑瓙锛堢敤蹇秷/鐬秷绮掑瓙锛岄伩鍏嶇櫧闆鹃暱鏃堕棿娈嬬暀锛?
            sw.sendParticles(ParticleTypes.POOF, land.x, land.y + 0.2, land.z,
                    25, landingRadius * 0.5, 0.1, landingRadius * 0.5, 0.08);
            sw.sendParticles(ParticleTypes.SWEEP_ATTACK, land.x, land.y + 0.5, land.z,
                    10, landingRadius * 0.4, 0.3, landingRadius * 0.4, 0.0);
            sw.sendParticles(ParticleTypes.EXPLOSION, land.x, land.y + 0.3, land.z,
                    2, landingRadius * 0.3, 0.1, landingRadius * 0.3, 0.0);
            // 鏂瑰潡瑙嗚娴姩锛堢矑瀛愭ā鎷燂級锛氳惤鐐瑰崐寰勫唴鏂瑰潡鎸夎繙杩戝啋鐮村潖绮掑瓙
            spawnBlockFloatParticles(sw, land, landingRadius);
        }

        enterCooldown(player);
        clear(player);
    }

    /** 杩涘叆 CD锛?2 绉掞紝on_end 缁撶畻鐐?= 钀藉湴锛夈€傝秴鏃惰惤涓嬩篃杩?CD銆?*/
    private static void enterCooldown(ServerPlayer player) {
        DashState s = STATES.get(player.getUUID());
        if (s != null) SkillCooldowns.ended(player, SKILL_ID, s.castId);
    }

    /** 璧烽闃舵鑴氫笅绮掑瓙锛氱伆鐑?+ 鐑熻姳涓婂崌銆?*/
    private static void spawnRiseParticles(ServerLevel sw, ServerPlayer player) {
        // 鐏扮儫锛堣剼涓嬶級
        github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(sw, player, ParticleTypes.CAMPFIRE_COSY_SMOKE,
                player.getX(), player.getY() - 0.1, player.getZ(),
                4, 0.3, 0.0, 0.3, 0.01);
        // 鐑熻姳鍚屾涓婂崌绮掑瓙锛團IREWORK锛屽甫鍚戜笂閫熷害锛?
        github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(sw, player, ParticleTypes.FIREWORK,
                player.getX(), player.getY() - 0.2, player.getZ(),
                3, 0.25, 0.0, 0.25, 0.05);
    }

    /**
     * 鏂瑰潡瑙嗚娴姩锛堢矑瀛愭ā鎷燂級锛氳惤鍦板崐寰?3 鏍煎唴鏂瑰潡锛屾寜璺濊惤鐐硅繙杩戝啋鏂瑰潡鐮村潖绮掑瓙锛?
     * 杩戝瀵嗛泦銆佽繙澶勭█鐤忥紝鍛堢幇"琚啿鍑绘尝闇囪捣"鐨勮瑙夈€備笉鍔ㄧ湡瀹炴柟鍧楋紝閬垮厤鍏煎闂銆?
     */
    private static void spawnBlockFloatParticles(ServerLevel sw, Vec3 land, double radius) {
        BlockPos center = BlockPos.containing(land.x, land.y, land.z);
        int r = (int) Math.ceil(radius);
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > radius) continue;
                // 鎵捐鍒楀湴琛ㄦ柟鍧楋紙浠庤惤鐐归珮搴﹀悜涓嬫壘绗竴涓疄蹇冿級
                for (int dy = 0; dy >= -2; dy--) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    BlockState st = sw.getBlockState(pos);
                    if (st.isAir()) continue;
                    // 杩戝绮掑瓙澶氥€佽繙澶勫皯
                    float intensity = (float) (1.0 - dist / radius);
                    int count = Math.max(1, (int) (4 * intensity));
                    sw.sendParticles(new net.minecraft.core.particles.BlockParticleOption(
                            net.minecraft.core.particles.ParticleTypes.BLOCK, st),
                            pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                            count, 0.3, 0.05, 0.3, 0.1);
                    break;
                }
            }
        }
    }

    // ===== 宸ュ叿鏂规硶 =====

    /** 鍨傜洿鍚戜笂灏勭嚎锛岃繑鍥炰粠鑴氶儴鍒颁笂鏂圭涓€涓疄蹇冩柟鍧楀簳闈㈢殑璺濈锛堟棤闃绘尅杩斿洖 maxCheck锛夈€?*/
    private static double checkVerticalClearance(ServerPlayer player, double maxCheck) {
        ServerLevel sw = (ServerLevel) player.level();
        Vec3 feet = new Vec3(player.getX(), player.getY(), player.getZ());
        Vec3 top = feet.add(0, maxCheck, 0);
        BlockHitResult bhr = sw.clip(new ClipContext(feet, top,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (bhr.getType() == HitResult.Type.MISS) return maxCheck;
        return bhr.getLocation().y - feet.y;
    }

    /**
     * 璁＄畻鍐插埡钀界偣锛氱敤瀹屾暣 3D 鍑嗘槦灏勭嚎锛堝惈淇话瑙掞級锛屽懡涓柟鍧楀垯鍙栬澶勫湴闈紙姘村钩璺濈鈮AX_DASH_RANGE锛夛紱
     * 鎵撶┖锛堢湅澶╃┖锛夋墠閫€鍒版按骞虫柟鍚戞渶杩?MAX_DASH_RANGE 澶勭殑鍦伴潰銆傝繖鏍风帺瀹剁瀯杩戝鍦伴潰=杩戣惤鐐癸紝鐬勮繙澶?杩滆惤鐐广€?
     */
    private static Vec3 computeDashLanding(ServerLevel world, Vec3 eye, Vec3 look, ServerPlayer player, double maxDashRange) {
        Vec3 flatLook = new Vec3(look.x, 0, look.z);
        double flatLen = flatLook.length();
        // 3D 灏勭嚎闀垮害涓婇檺锛氫繚璇佹按骞虫姇褰变笉瓒呰繃 max_dash_range
        double max3D = flatLen > 1.0e-4 ? maxDashRange / flatLen : maxDashRange;
        Vec3 end3D = eye.add(look.scale(max3D));
        BlockHitResult bhr = world.clip(new ClipContext(eye, end3D,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (bhr.getType() != HitResult.Type.MISS) {
            Vec3 hit = bhr.getLocation();
            double groundY = findGroundY(world, hit.x, hit.z, Math.min(hit.y, player.getY()));
            return new Vec3(hit.x, groundY, hit.z);
        }
        // 鎵撶┖锛氶€€鍒版按骞虫柟鍚戞渶杩?MAX_DASH_RANGE 澶勭殑鍦伴潰
        Vec3 flatDir;
        if (flatLen > 1.0e-4) {
            flatDir = flatLook.normalize();
        } else {
            float yaw = player.getYRot();
            flatDir = new Vec3(-Mth.sin(yaw * 0.017453292f), 0, Mth.cos(yaw * 0.017453292f));
        }
        double hx = player.getX() + flatDir.x * maxDashRange;
        double hz = player.getZ() + flatDir.z * maxDashRange;
        double gy = findGroundY(world, hx, hz, player.getY());
        return new Vec3(hx, gy, hz);
    }

    /** 浠庣┖涓按骞崇偣 (x,z) 鍚戜笅鎵惧湴闈㈢珯绔嬮潰 Y銆俧romY 涓鸿捣濮嬮珮搴︺€?*/
    private static double findGroundY(ServerLevel world, double x, double z, double fromY) {
        BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();
        int startY = (int) Math.floor(fromY) + 1;
        for (int y = startY; y >= startY - 20 && y >= world.getMinBuildHeight(); y--) {
            mpos.set(x, y, z);
            BlockState st = world.getBlockState(mpos);
            if (!st.isAir() && !st.getCollisionShape(world, mpos).isEmpty()) {
                return y + 1; // 鏂瑰潡椤堕潰
            }
        }
        return fromY; // 鎵句笉鍒板湴闈紝缁存寔鍘熼珮搴?
    }

    /** 鐩寸嚎璺緞鏄惁鐣呴€氾紙鏃犲疄蹇冩柟鍧楅樆鎸★級銆?*/
    private static boolean isClearPath(ServerLevel world, Vec3 from, Vec3 to) {
        double dist = from.distanceTo(to);
        int steps = Math.max(1, (int) Math.ceil(dist / 0.5));
        Vec3 step = to.subtract(from).scale(1.0 / steps);
        BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();
        for (int i = 1; i < steps; i++) {
            Vec3 p = from.add(step.scale(i));
            mpos.set(p.x, p.y, p.z);
            BlockState st = world.getBlockState(mpos);
            if (!st.isAir() && !st.getCollisionShape(world, mpos).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** 褰诲簳娓呯悊鐘舵€佸苟閫氱煡瀹㈡埛绔€?*/
    public static void clear(ServerPlayer player) {
        // 姝讳骸/鍙樺舰鎸夊疄闄呮柦鏀鹃樁娈电粨绠楋紱钀藉湴璺緞宸插厛 ended锛屾澶勪负绌烘搷浣溿€?
        DashState s = STATES.get(player.getUUID());
        if (s != null) SkillCooldowns.cancelled(player, SKILL_ID, s.castId);
        if (STATES.remove(player.getUUID()) != null) {
            SscAddonNetworking.syncDashState(player, PHASE_NONE, 0);
        }
    }

    public static void onPlayerDisconnect(ServerPlayer player) {
        clear(player);
    }
}
