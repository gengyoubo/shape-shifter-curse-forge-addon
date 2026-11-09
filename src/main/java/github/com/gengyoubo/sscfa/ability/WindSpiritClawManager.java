package github.com.gengyoubo.sscfa.ability;

import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.network.SscAddonNetworking;
import github.com.gengyoubo.sscfa.util.FormUtils;
import github.com.gengyoubo.sscfa.util.TrinketUtils;
import github.com.gengyoubo.sscfa.util.WhitelistUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 妞嬪海浼掗敍鍫熸箑妤傛挾骞嗙挒鍦皸閿?- 閵嗗瞼鏌ф搴ょ箾閻栴亗鈧秴涔忛柨顔跨箾閸戠粯濡ч懗鏂ょ礄閺堝秴濮熺粩顖滃Ц閹焦婧€閿涘鈧?
 *
 * 閹稿顒村锕傛暛 閳?鏉╃偟鐢婚悥顏勫毊閿涘矁绉洪幍鎾圭Ш韫囶偓绱欓梻鎾 1.0s 閳?3 缁夋帒鎮?0.34s閿涘绱卞В蹇旑偧閻栴亜鍤€电懓澧犻弬?2.5 閺嶈壈瀵栭崶鎾偓鐘冲灇濡亝澹傛导銈咁唺
 * 閿涘牆鐔€绾偓 8閿涘矂娈㈤弮鍫曟毐鐞涙澘鍣洪敍灞肩瑬閸欑姴濮?MC 閸樼喓澧楅弨璇插毊閸愬嘲宓堥崙蹇庢縺閿涘绱濋崜宥呭暱娑撯偓鐏忓繑顔岄妴浣瑰⒏妤楅亶顥ゆ惔锔衡偓鍌氬櫙閺勭喐娼崡鐐解偓宀冣偓鎰閵嗗稄绱濇潻鐐插毊瀵偓婵妞傚掳鈧?
 * 闂呭繑鐦″▎锛勫焻閸戣绗呴梽宥忕幢閺夋儳绱戦幋鏍ㄥⅵ缁屽搫鎮楁禒搴＄秼閸撳秷绻樻惔锔藉弮閹便垹娲栧鈽呯礄閸ョ偞寮х痪?8 缁夋帪绱濋張鐔兼？瀹革箓鏁?瀵鲸娅橀弨?0%閳?0%閿涘鈧?
 * 缁夊鈧喖娈㈤弮鍫曟毐缁炬寧鈧嗏€滈崙蹇ョ礄-15%/缁夋帪绱濈亸渚€銆?-45%閿涘鈧?
 *
 * 鐎广垺鍩涚粩顖氬櫙閺勭喐鏁鹃崙璇插枎閸楀瓨娼悽?{@code ClawCrosshairMixin} 鐠囪褰囩紒?S2C閿涘湧ACKET_CLAW_STATE閿涘鎮撳銉ф畱
 * {@code ClawClientState} 缂佹ê鍩楅妴鍌涙拱缁褰ч崑姘箛閸旓紕顏崚銈呯暰閿涘奔绻氱拠浣割樋娴滆桨绔撮懛娣偓?
 */
public final class WindSpiritClawManager {

    // 娴犮儰绗呭鑼鐠佹澘鐖堕柌蹇撴綆娑撴椽绮拋銈呪偓纭风幢鏉╂劘顢戦弮鏈电矤 balance 韫囶偆鍙庣拠璇插絿閿涘潊bilities.wind_spirit_claw閿?
    private static final float BASE_DAMAGE = 8.0f;
    private static final double RADIUS = 2.5;
    private static final double REACH = 2.0;
    private static final int MAX_INTERVAL = 20;   // 1.0s
    private static final int MIN_INTERVAL = 7;    // 閳?.34s
    private static final int RAMP_TICKS = 60;     // 3 缁夋帟鎻張鈧箛?
    private static final int MAX_CLAW_TICKS = 200; // 鏉╃偛鍤張鈧梹?10 缁夋帒鍘规惔?
    private static final int RECOVER_TICKS = 160;  // 娴?0 閸ョ偞寮?8 缁?
    private static final int RECOVER_TICKS_NECKLACE = 110; // 閹存挳顥撻悘鍏哥瑩鐏炵偤銆嶉柧鎾呯窗閼版劕濮忛崶鐐村姬缁?5.5 缁?
    private static final float DMG_DECAY_PER_SEC = 0.12f;
    private static final float DMG_DECAY_MAX = 0.36f;
    private static final float SPD_DECAY_PER_SEC = 0.15f;
    private static final float SPD_DECAY_MAX = 0.45f;
    private static final float EXHAUSTION_PER_HIT = 0.64f; // 閳?.16 妤楅亶顥ゆ惔锔惧仯
    private static final float FORWARD_LUNGE = 0.28f;
    private static final float PROGRESS_PER_HIT = 1.0f / 25.0f; // 閺堚偓韫囶偊顣堕悳鍥︾瑓缁?10 缁夋帗澧︾粚?

    private static final UUID SPEED_MOD_UUID = UUID.fromString("6a1d3c9e-7b2f-4c8a-9e1d-0f5a2c7b4d33");
    private static final String SPEED_MOD_NAME = "wind_spirit_claw_slow";

    public static final int PHASE_IDLE = 0;
    public static final int PHASE_CLAW = 1;
    public static final int PHASE_OVERHEAT = 2;

    private static final Map<UUID, ClawState> STATES = new ConcurrentHashMap<>();

    // 閸擃垱濡ч懗鏂ょ窗+50% 瀵版帗澧?瑜般垺鈧椒婵€鐎?buff閿涘湑D 2 缁夋帞鏁?power JSON 闁板秶鐤嗛敍灞藉斧 SECONDARY_CD_TICKS 鐢悂鍣哄鎻掑灩閿?
    private static final int BUFF_DURATION = 10;      // 0.5 缁?
    private static final float BUFF_MULT = 1.5f;
    private static final Map<UUID, Integer> BUFF_TICKS = new ConcurrentHashMap<>();

    // 闂冭埖顔?5閿涙碍婀囬崝锛勵伂閺夊啫鈻夎箛顐ゅ弾鐠囪褰囬敍鍫濇彥閻撗勬弓閸掓繂顫愰崠鏍ф礀闁偓姒涙顓荤敮鎼佸櫤閿?
    private static final BalanceReader BAL = new BalanceReader("abilities.wind_spirit_claw");
    /** 缂佺喍绔撮崘宄板祱閺堝秴濮熼惃鍕旂€规碍濡ч懗?ID閿涘牏缍夌紒婊冨瘶鐟欙箑褰傞崹瀣剁礆閵?*/
    private static final String SKILL_ID = "my_addon:form_ocelot_wind_spirit_claw_buff";

    public static final class ClawState {
        boolean holding = false;
        int phase = PHASE_IDLE;
        int holdTicks = 0;
        int sinceLastAttack = 0;
        float progress = 1.0f;
        float recovery = 0.0f;
        int sentPhase = -1;
        float sentProgress = Float.NaN;
        float sentStep = Float.NaN;
        long sentAt = Long.MIN_VALUE;
    }

    private WindSpiritClawManager() {
    }

    /** 鐎广垺鍩涚粩顖欑瑐閹躲儱涔忛柨顔藉瘻娴ｅ繒濮搁幀浣碘偓?*/
    public static void setHolding(ServerPlayer player, boolean holding) {
        if (!FormUtils.isOcelotSP(player)) {
            clear(player);
            return;
        }
        ClawState s = STATES.computeIfAbsent(player.getUUID(), k -> new ClawState());
        s.holding = holding;
        if (holding && s.phase == PHASE_IDLE) {
            s.phase = PHASE_CLAW;
            s.holdTicks = 0;
            s.sinceLastAttack = BAL.i("min_interval", MIN_INTERVAL); // 閹稿绗呯粙宥囩处閸愯绱濋柆鍨帳閹稿绗呴崡鍐叉嚒娑?
        }
    }

    /** 濮ｅ繑婀囬崝锛勵伂 tick 鐎佃鐦℃稉顏勬躬缁捐法甯虹€规儼鐨熼悽銊ｂ偓?*/
    public static void tick(ServerPlayer player) {
        // 閸擃垱濡ч懗?buff 鐠佲剝妞傞敍鍫㈠缁斿绨潻鐐插毊閻樿埖鈧緤绱?
        Integer buff = BUFF_TICKS.get(player.getUUID());
        if (buff != null) {
            if (buff <= 1 || !FormUtils.isOcelotSP(player)) BUFF_TICKS.remove(player.getUUID());
            else BUFF_TICKS.put(player.getUUID(), buff - 1);
        }

        ClawState s = STATES.get(player.getUUID());
        if (s == null) return;

        if (player.isDeadOrDying() || !FormUtils.isOcelotSP(player)) {
            clear(player);
            return;
        }

        switch (s.phase) {
            case PHASE_CLAW -> tickClaw(player, s);
            case PHASE_OVERHEAT -> tickOverheat(player, s);
            default -> {
            }
        }

        // 閻栴亜鍤幍锝堚偓鎰閹稿绨ㄦ禒璺烘倱濮濄儻绱辨潻鍥╁劰閺夆剝瀵滈張宥呭缁旑垰娲栨径宥夆偓鐔峰閺堫剙婀撮幒銊ㄧ箻閿涘本鐦＄粔鎺撶墡閸戝棎鈧?
        if (STATES.containsKey(player.getUUID())) {
            float progress = crosshairProgress(s);
            float step = s.phase == PHASE_OVERHEAT ? recoveryStep(player) : 0;
            long now = player.level().getGameTime();
            boolean legacy = !github.com.gengyoubo.sscfa.network.AddonNetwork.canSend(player,
                    github.com.gengyoubo.sscfa.network.CountdownSync.STATE);
            if (legacy || s.sentPhase != s.phase || s.sentStep != step
                    || s.phase == PHASE_CLAW && s.sentProgress != progress || now - s.sentAt >= 20) {
                SscAddonNetworking.syncClawState(player, s.phase, progress, step);
                s.sentPhase = s.phase; s.sentProgress = progress; s.sentStep = step; s.sentAt = now;
            }
        }
    }

    private static void tickClaw(ServerPlayer player, ClawState s) {
        s.holdTicks++;
        applySpeedSlow(player, slowFactor(s.holdTicks));

        if (!s.holding) {
            enterOverheat(s);
            removeSpeedSlow(player);
            return;
        }

        s.sinceLastAttack++;
        int interval = currentInterval(s.holdTicks);
        if (s.sinceLastAttack >= interval) {
            performClawAttack(player, s);
            s.sinceLastAttack = 0;
            s.progress -= PROGRESS_PER_HIT;
            player.resetAttackStrengthTicker(); // 鐠佲晛甯悧鍫濆櫤娴?閸戝棙妲﹂崗鍛板厴閸欏秵妲ч悥顏勫毊閼哄倸顨?
        }

        if (s.progress <= 0.0f || s.holdTicks >= BAL.i("max_claw_ticks", MAX_CLAW_TICKS)) {
            s.progress = 0.0f;
            enterOverheat(s);
            removeSpeedSlow(player);
        }
    }

    private static void tickOverheat(ServerPlayer player, ClawState s) {
        removeSpeedSlow(player);

        // 閸嬫粍澧滈崥搴ｇ彌閸楀厖绮犺ぐ鎾冲鏉╂稑瀹抽幈銏″弮閸ョ偞寮ч敍鍫熸￥瀵ゆ儼绻滈敍澶涚幢閺堢喖妫垮锕傛暛 = 瀵鲸娅橀弨浼欑礄getRecoveryMultiplier 缂傗晜鏂?0閳?0%閿涘鈧?
        // 閹存挳顥撻悘鍏哥瑩鐏炵偤銆嶉柧淇扁偓灞藉敖妞嬪海鍩呴柧鍐︹偓宥嗘閼版劕濮忛崶鐐差槻閺囨潙鎻╅敍?s 閳?5.5s閿涘鈧?
        s.recovery += recoveryStep(player);
        if (s.recovery >= 1.0f) {
            // 閸ョ偞寮ч敍姘矝閹稿缍?閳?閺冪姷绱抽柌宥呮儙鏉╃偛鍤敍娑樻儊閸掓瑧绮ㄩ弶鐔粹偓浣稿櫙閺勭喐娼☉鍫濄亼
            if (s.holding) {
                s.phase = PHASE_CLAW;
                s.holdTicks = 0;
                s.sinceLastAttack = BAL.i("min_interval", MIN_INTERVAL);
                s.progress = 1.0f;
                s.recovery = 0.0f;
            } else {
                clear(player);
            }
        }
    }

    private static void enterOverheat(ClawState s) {
        s.phase = PHASE_OVERHEAT;
        s.recovery = Mth.clamp(s.progress, 0.0f, 1.0f); // 娴犲骸缍嬮崜宥呭⒖娴ｆ瑨绻樻惔锕€绱戞慨瀣礀濠娾槄绱欓幍鎾崇繁鐡掑﹣绠欓崜鈺勭Ш鐏忔垯鈧礁娲栫搾濠佺畽閿?
    }

    private static float recoveryStep(ServerPlayer player) {
        int recoverTicks = TrinketUtils.isWearing(player,
                github.com.gengyoubo.sscfa.init.AddonItems.WIND_SPIRIT_STAMINA_NECKLACE.get())
                ? BAL.i("recover_ticks_necklace", RECOVER_TICKS_NECKLACE)
                : BAL.i("recover_ticks", RECOVER_TICKS);
        return 1.0f / recoverTicks;
    }

    private static void performClawAttack(ServerPlayer player, ClawState s) {
        ServerLevel sw = (ServerLevel) player.level();

        Vec3 look = player.getViewVector(1.0f);
        double randSide = (player.getRandom().nextDouble() - 0.5) * 1.2;
        double randUp = (player.getRandom().nextDouble() - 0.5) * 0.6;
        Vec3 side = new Vec3(-look.z, 0, look.x).normalize();
        double reach = BAL.d("reach", REACH);
        Vec3 center = player.getEyePosition()
                .add(look.scale(reach))
                .add(side.scale(randSide))
                .add(0, randUp, 0);

        // MC 閸樼喓澧楅弨璇插毊閸愬嘲宓堥崙蹇庢縺缁粯鏆熼敍鍫㈡埂鐎圭偛鍘栭懗鍊熺箻鎼达讣绱氳劤 閻栴亜鍤弮鍫曟毐鐞涙澘鍣?
        float g = player.getAttackStrengthScale(0.5f);
        float vanillaFactor = 0.2f + g * g * 0.8f;
        float clawFactor = 1.0f - Math.min((float) BAL.d("dmg_decay_max", DMG_DECAY_MAX),
                (float) BAL.d("dmg_decay_per_sec", DMG_DECAY_PER_SEC) * (s.holdTicks / 20.0f));
        float dmg = (float) BAL.d("base_damage", BASE_DAMAGE) * vanillaFactor * clawFactor;

        double radius = BAL.d("radius", RADIUS);
        AABB box = new AABB(center.subtract(radius, radius, radius), center.add(radius, radius, radius));
        for (Entity e : sw.getEntities(player, box)) {
            if (!(e instanceof LivingEntity living)) continue;
            if (WhitelistUtils.isProtected(player, living)) continue; // 姒涙顓婚惂钘夋倳閸?
            // 鐟欏棛鍤庡Λ鈧弻銉窗婢ф瑥鎮楅惄顔界垼娑撳秴鎳℃稉顓ㄧ礄娴犲灝甯悧?PR #523 鐠炲湱灏楅崘鎻掑煛缁屽灝顣炬穱顔碱槻閿?
            if (!github.com.gengyoubo.sscfa.util.LineOfSightUtils.hasLineOfSight(sw, player, living)) continue;
            living.hurt(player.damageSources().playerAttack(player), dmg);
            Vec3 push = living.position().subtract(player.position());
            if (push.lengthSqr() < 1.0e-4) push = look;
            push = push.normalize();
            living.knockback(0.35, -push.x, -push.z);
        }

        sw.sendParticles(ParticleTypes.SWEEP_ATTACK, center.x, center.y, center.z, 1, 0, 0, 0, 0);

        Vec3 flat = new Vec3(look.x, 0, look.z).normalize().scale((float) BAL.d("forward_lunge", FORWARD_LUNGE));
        player.push(flat.x, 0.0, flat.z);
        player.hurtMarked = true;

        player.causeFoodExhaustion((float) BAL.d("exhaustion_per_hit", EXHAUSTION_PER_HIT));

        sw.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.7f, 1.4f);
    }

    private static float slowFactor(int holdTicks) {
        return Math.min((float) BAL.d("spd_decay_max", SPD_DECAY_MAX),
                (float) BAL.d("spd_decay_per_sec", SPD_DECAY_PER_SEC) * (holdTicks / 20.0f));
    }

    private static void applySpeedSlow(ServerPlayer player, float factor) {
        AttributeInstance attr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attr == null) return;
        double target = -factor; // MULTIPLY_TOTAL閿?0.15 = 缁夊鈧唻?.85
        AttributeModifier existing = attr.getModifier(SPEED_MOD_UUID);
        if (existing != null) {
            if (Math.abs(existing.getAmount() - target) < 1.0e-4) return;
            attr.removeModifier(SPEED_MOD_UUID);
        }
        attr.addTransientModifier(new AttributeModifier(
                SPEED_MOD_UUID, SPEED_MOD_NAME, target,
                AttributeModifier.Operation.MULTIPLY_TOTAL));
    }

    private static void removeSpeedSlow(ServerPlayer player) {
        AttributeInstance attr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attr != null && attr.getModifier(SPEED_MOD_UUID) != null) {
            attr.removeModifier(SPEED_MOD_UUID);
        }
    }

    /** 閹稿缍囬弮鍫曟毐 閳?瑜版挸澧犻弨璇插毊闂傛挳娈ч敍鍧眎ck閿涘绱?0 閳?3 缁夋帒鎮?7閵?*/
    private static int currentInterval(int holdTicks) {
        int rampTicks = BAL.i("ramp_ticks", RAMP_TICKS);
        float t = Math.min(holdTicks, rampTicks) / (float) rampTicks;
        int maxInterval = BAL.i("max_interval", MAX_INTERVAL);
        int minInterval = BAL.i("min_interval", MIN_INTERVAL);
        return Math.round(maxInterval - (maxInterval - minInterval) * t);
    }

    /** 閸戝棙妲﹂弶?閼版劕濮忛敍姘卞焻閸戠粯婀?閸撯晙缍戞潻娑樺(闂呭繑鐦″▎锛勫焻閸戣绗呴梽?閿涙稖绻冮悜顓熸埂=娴犲骸缍嬮崜宥堢箻鎼达箑娲栧?0閳?)閿涙稓鈹栭梻?1閵?*/
    public static float crosshairProgress(ClawState s) {
        return switch (s.phase) {
            case PHASE_CLAW -> Mth.clamp(s.progress, 0.0f, 1.0f);
            case PHASE_OVERHEAT -> Mth.clamp(s.recovery, 0.0f, 1.0f);
            default -> 1.0f;
        };
    }

    /** 瀵版帗澧?闂堢偞顒熼崳銊ㄧ箮閹存ü婵€鐎瑰啿鈧秶宸?= 鏉╁洨鍎归崶鐐差槻缂傗晜鏂?0-0.9) 鑴?閸擃垱濡ч懗?buff(1.5)閵嗗倻绮?ClawDamageBoostMixin 閻劊鈧?*/
    public static float getNormalMeleeMultiplier(ServerPlayer player) {
        return getNormalMeleeMultiplier(player, null);
    }

    public static float getNormalMeleeMultiplier(ServerPlayer player, LivingEntity target) {
        float recovery = 1.0f;
        ClawState s = STATES.get(player.getUUID());
        if (s != null && s.phase == PHASE_OVERHEAT) {
            recovery = s.recovery * 0.9f; // 閸ョ偛顦叉潻娑樺 0閳? 閺勭姴鐨犳导銈咁唺 0閳?0%
        }
        float buff = BUFF_TICKS.containsKey(player.getUUID())
                ? (float) BAL.d("buff_mult", BUFF_MULT) : 1.0f;
        if (BUFF_TICKS.containsKey(player.getUUID()) && target != null && NpcCombatEnhancements.isEnemyNpc(player, target))
            buff *= (float) NpcCombatEnhancements.value("wind_secondary_mul", 2.0);
        return recovery * buff;
    }

    /** 閺勵垰鎯侀幍瀣瘮濮濓箑娅掗敍鍫滅瑢娑撹瀵?is_weapon 娑撯偓閼疯揪绱版稉缁樺閺€璇插毊閸旀稑濮為幋?&gt; 1閿涘鈧倹瀣佸锕€娅掗弮鏈电瑝閸氬啰鏌ф搴ょ箾閻?閸擃垱濡ч懗钘夊娴笺倧绱欓悘顐ｅΩ/妞嬬喓澧?閺傜懓娼＄粵澶愭姜濮濓箑娅掓禒宥呮倖閿涘鈧?*/
    public static boolean isHoldingWeapon(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) return false;
        double totalAdd = 0.0;
        for (AttributeModifier m : stack
                .getAttributeModifiers(EquipmentSlot.MAINHAND)
                .get(Attributes.ATTACK_DAMAGE)) {
            if (m.getOperation() == AttributeModifier.Operation.ADDITION) {
                totalAdd += m.getAmount();
            }
        }
        return totalAdd > 1.0;
    }

    /** 閸擃垱濡ч懗鏂ょ礄sp_secondary閿涘绱?.5 缁夋帒鍞村鎺撳/瑜般垺鈧椒婵€鐎?+50%閿涘畱d 2 缁夋帇鈧?*/
    public static void activateSecondaryBuff(ServerPlayer player) {
        if (!FormUtils.isOcelotSP(player)) return;
        if (!github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.ready(player, SKILL_ID)) return; // CD 娑?
        github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.instant(player, SKILL_ID);
        BUFF_TICKS.put(player.getUUID(), BAL.i("buff_duration", BUFF_DURATION));
        ServerLevel sw = (ServerLevel) player.level();
        sw.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 0.8f, 1.6f);
        // owner 閹垫挻鐖ｉ敍姘矌閺堫兛姹夌粭顑跨娴滆櫣袨闁儻顔€
        github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(sw, player, ParticleTypes.CRIT, player.getX(), player.getY() + 1.0, player.getZ(), 20, 0.4, 0.5, 0.4, 0.2);
    }

    /** 瑜拌绨冲〒鍛倞閿涙氨些闂勩倗濮搁幀?+ 缁夊鈧喍鎱ㄦ鎵儊閿涘苯鑻熼柅姘辩叀鐎广垺鍩涚粩顖氬櫙閺勭喐娼☉鍫濄亼閵?*/
    public static void clear(ServerPlayer player) {
        removeSpeedSlow(player);
        if (STATES.remove(player.getUUID()) != null) {
            SscAddonNetworking.syncClawState(player, PHASE_IDLE, 1.0f);
        }
    }

    public static void onPlayerDisconnect(ServerPlayer player) {
        removeSpeedSlow(player);
        STATES.remove(player.getUUID());
        BUFF_TICKS.remove(player.getUUID());
    }
}
