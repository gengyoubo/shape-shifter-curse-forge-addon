/*
 * Copyright (c) 2026 MangZai-120
 * This file is part of the "shape shifter curse addon" project.
 * Licensed under the GNU Affero General Public License v3.0 (AGPL-3.0).
 */
package github.com.gengyoubo.sscfa.ability;

import org.joml.Vector3f;
import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.compat.NovaPerks;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.FormUtils;
import github.com.gengyoubo.sscfa.util.PowerUtils;
import github.com.gengyoubo.sscfa.util.WhitelistUtils;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 閺堟梹婀滈妴灞肩瘈閸涚晫浼掗悮顐犫偓宥勫瘜/濞嗏€插瘜閸斻劍濡ч懗?+ 闂傤亪浼?缁狅紕鎮婇崳顭掔礄閺堝秴濮熺粩顖涙綀婵炰緤绱氶妴?
 * <p>
 * 娑撳顨滅化鑽ょ埠閿?
 * <ul>
 *   <li><b>闂傤亪浼?/b>閿涙艾鐔€绾偓鐞氼偄濮╅梻顏堜缉 {@link #BASE_DODGE}閿涘瞼浼掔捄鍐╃槨濞?+{@link #DODGE_PER_LEAP}閿涘牓妾洪弮?{@link #DODGE_DURATION}閿涘鈧?
 *       閸欐ぞ婵€閺冭埖瀵滆ぐ鎾冲閸戠姷宸煎鍌滃芳閸忓秶鏌呴敍鍫滅瑝閸欐ぞ婵€閵嗕椒绗夐崙濠氣偓鈧敍澶嗏偓鏂衡偓鏃傛暠 SscAddonLivingEntityMixin 鐠嬪啰鏁?{@link #rollDodge}閵?/li>
 *   <li><b>閻忎絻绌梻顏囬煩</b>閿涘牊顐奸幎鈧懗?sp_secondary閿涘绱伴崥鎴濆櫙閺勭喕鐑﹂崘璇х礉缁岃桨鑵戦崣顖滄暏閿涘苯褰叉潻鐐垫暏 2 濞嗏槄绱欑粭?1閳? 濞嗭紕鐛ラ崣?{@link #LEAP_WINDOW}閿涘绱?
 *       濮ｅ繑顐?+20% 闂傤亪浼╅敍娑氭暏濠?2 濞嗏剝瀵?power JSON cooldown閿涘苯褰ч悽?1 濞嗏€茬瑬鐡掑懏妞傞幐?extra_cooldowns.single_use閵?/li>
 *   <li><b>閼稿秷闊╅悥鍡欏仮</b>閿涘牅瀵岄幎鈧懗?sp_primary閿涘绱伴拑鍕 {@link #CHARGE_TIME}閿涘牆鍣洪柅?70% + 閹舵鈧?III + TNT 闂婅櫕鏅?+ 姒涙垹鍎划鎺戠摍閿涘绱?
 *       閽冨嫭寮ч懛顏嗗瀻閳ユ柡鈧梹绉烽懓?1 娑旀繂鎳￠敍宀冨毀閸涜棄宕愬?{@link #LETHAL_RADIUS} 娴笺倕顔?{@link #MAX_DAMAGE}閿涘本娓舵潻?{@link #MAX_RADIUS} 闂呭繗绐涚粋鏄忊€滈崙蹇ョ礉
 *       娑撳秶鐗崸蹇旀煙閸фぜ鈧焦妫ら崣瀣╂縺閿涘牏娅ч崥宥呭礋/鐎圭姷澧块崗宥勬縺閿涘鈧礁缍婄仦鐐靛负鐎硅绱盋D 鐟?power JSON閵?/li>
 * </ul>
 * 鐟欙箑褰傞幒銉у殠閿涙p_primary 閹稿鏁?閳?{@link #startCharge}閿涙硞p_secondary 閹稿鏁?閳?{@link #tryLeap}閵?
 * 闂団偓閸?power JSON閿涘潊poli:active_self, key.ssc_addon.sp_primary/secondary閿涘鍨ㄩ幐澶愭暛 C2S 閸栧懘鍣风拫鍐暏閺堫剛琚弬瑙勭《閵?
 */
public final class NovaSkillManager {
    // ==== 娴犮儰绗呯敮鎼佸櫤娑撴椽绮拋銈呪偓纭风幢鏉╂劘顢戦弮鏈电矤 balance 韫囶偆鍙庣拠璇插絿閿涘澃cope: abilities.nova閿涘本鏆熼幑顔煎瘶閸欘垵顩惄鏍电礆====
    // 闂傤亪浼?
    private static final float BASE_DODGE = 0.15f;      // 姒涙顓婚敍姘唨绾偓鐞氼偄濮╅梻顏堜缉 15%
    private static final float DODGE_PER_LEAP = 0.20f;  // 姒涙顓婚敍姘变紥鐠哄啯鐦″▎?+20%
    private static final int DODGE_DURATION = 60;       // 姒涙顓婚敍姘变紥鐠哄啴妫柆鍨閹存劖瀵旂紒?3s
    private static final float DODGE_CAP = 0.85f;       // 姒涙顓婚敍姘舵／闁灝鍤戦悳鍥︾瑐闂勬劧绱濋柆鍨帳鐎瑰苯鍙忛弮鐘虫櫕
    // 閻忎絻绌梻顏囬煩
    private static final int LEAP_WINDOW = 100;         // 姒涙顓婚敍姘鳖儑 1閳? 濞嗭紕鐛ラ崣?5s
    private static final double LEAP_POWER = 1.2;       // 姒涙顓婚敍姘崇儲閸愬弶鎸夐獮鎶解偓鐔峰
    private static final int LEAP_INPUT_GAP = 5;        // 鏉堟挸鍙嗛崢缁樺缁愭褰?tick)閿涙艾绻€妞?> leap power 閻?cooldown(3)閿涘苯鎯侀崚娆愬皡娑撳秳缍囬幐澶夌秶閻ㄥ嫰鍣告径宥埿曢崣鎴欌偓鍌涙弓閻ф槒顔?balance閿涘奔绻氶幐浣哥埗闁?
    // 閼稿秷闊╅悥鍡欏仮
    private static final int CHARGE_TIME = 100;         // 姒涙顓婚敍姘虫惈閸?5s
    private static final int LETHAL_RADIUS = 5;         // 姒涙顓婚敍姘冲毀閸涜棄宕愬?5 閺?
    private static final int MAX_RADIUS = 12;           // 姒涙顓婚敍姘付鏉╂粌宕愬?12 閺?
    private static final float MAX_DAMAGE = 50.0f;      // 姒涙顓婚敍姘冲毀閸涙垝婵€鐎?50
    // 閽冨嫬濮忛張鐔跺敩閻礁鍣洪柅鐕傜窗GENERIC_MOVEMENT_SPEED MULTIPLY_TOTAL -0.70 = 缁墽鈥橀崙蹇涒偓?70%閿涘牓娼懡顖涙寜缂傛挻鍙冮敍灞藉讲缁墽鈥橀崚?70%閿涘鈧倿绮拋銈忕幢鏉╂劘顢戦弮鏈电矤 balance 韫囶偆鍙庣拠璇插絿
    private static final UUID CHARGE_SLOW_UUID = UUID.fromString("9f3c1e5a-7b2d-4c8e-a1f6-0d9e8c7b6a54");
    private static final double CHARGE_SLOW_AMOUNT = -0.70;

    /** 閺堝秴濮熺粩顖涙綀婵?balance 韫囶偆鍙庣拠璇插絿閿涘牆鎻╅悡褎婀崚婵嗩潗閸栨牗妞傞崶鐐衡偓鈧稉濠冩煙姒涙顓荤敮鎼佸櫤閿涘鈧?*/
    private static final BalanceReader BAL = new BalanceReader("abilities.nova");
    /** 缂佺喍绔撮崘宄板祱閺堝秴濮熼幎鈧懗?ID閿涘牏缍夌紒婊冨瘶鐟欙箑褰傞崹瀣剁礆閵?*/
    private static final String LEAP_SKILL_ID = "my_addon:form_ocelot_nova_leap";
    private static final String EXPLODE_SKILL_ID = "my_addon:form_ocelot_nova_charge";

    private static final Map<UUID, Float> DODGE = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> DODGE_EXPIRE = new ConcurrentHashMap<>();
    private static final Map<UUID, LeapSession> LEAPS = new ConcurrentHashMap<>();
    /** 閻忎絻绌張鈧潻鎴滅濞喡扳偓灞炬暪閸掓媽袝閸欐垯鈧秶娈?tick閿涙氨鏁ゆ禍搴㈠閸?active_self 閻㈤潧閽╃憴锕€褰傞崷銊ょ濞嗏剝瀵滈柨顔煎敶閻ㄥ嫬顦?tick 闁插秴顦查妴?*/
    private static final Map<UUID, Long> LEAP_LAST_INPUT = new ConcurrentHashMap<>();
    /** 閼稿秷闊╅悥鍡欏仮閽冨嫬濮忔导姘崇樈閿?026-09-27 鐎孤ゎ吀娣囶喖顦查敍澶涚窗鐠ч攱澧滈幎?chargeTime/lethalRadius/maxRadius/maxDamage
     * 韫囶偆鍙庢潻娑楃窗鐠囨績鈧柡鈧?00t 閽冨嫬濮忛崣顖濆厴鐠恒劍鏆熼幑顔煎瘶 reload閿涘矁瀚㈡潻娑樺閸掋倕鐣炬稉?explode 缂佹挾鐣婚崡铏鐠?balance閿?
     * reload 閸氬骸鎮撴稉鈧崣鎴犲瀻閻愰晲绱伴悽銊︽煀閸欏倹鏆熼敍娑欐暭鐠囪绱扮拠婵嗘彥閻撗傜箽鐠囦礁宕熷▎锟犲櫞閺€鎯у弿缁嬪鎮撻崣鍌樷偓?*/
    private static final Map<UUID, ChargeSession> CHARGE_START = new ConcurrentHashMap<>();

    /** 閸楁洘顐奸拑鍕閻ㄥ嫯鎹ｉ幍瀣棘閺佹澘鎻╅悡褝绱欓弸鍕偓鐘叉倵娑撳秴褰夐敍澶堚偓?*/
    private record ChargeSession(long castId, long startTick, int chargeTime, int lethalRadius, int maxRadius, float maxDamage, float npcMaxDamage) {
    }

    /** 缁楊兛绔村▓闈涘嚒娴ｈ法鏁ら惃鍕崶閸欙綇绱遍崘宄板祱闁板秶鐤嗛梾蹇旀拱鏉烆喖鍠曠紒鎿勭礉娑撱垹銇?power / reload 娑撳秵鏁奸張顒冪枂 CD閵?*/
    private record LeapSession(long castId, long firstTick, int singleUseCooldown, boolean enhanced) {
    }

    private NovaSkillManager() {
    }

    public static void init() {
        github.com.gengyoubo.sscfa.util.SscEvents.onServerTick(server -> {
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                tickPlayer(p);
            }
        });
        // 閺傤厾鍤庡〒鍛倞閻樿埖鈧浇銆冮敍宀勪缉閸忓秹鍣告潻鐐叉倵濞堝鏆€閺冄呯崶閸欙絽鎷伴拑鍕鐠ч鍋ｉ妴?
        github.com.gengyoubo.sscfa.event.PlayerSessionCleanup.register((handler, server) -> {
            interruptControl(handler.player);
            UUID id = handler.player.getUUID();
            DODGE.remove(id);
            DODGE_EXPIRE.remove(id);
            LEAPS.remove(id);
            LEAP_LAST_INPUT.remove(id);
            CHARGE_START.remove(id);
        });
    }

    // ==== 闂傤亪浼?====

    /** 閸欐ぞ婵€閺冭泛鍨界€规碍妲搁崥锕傛／闁尅绱欓悽?damage mixin 鐠嬪啰鏁ら敍澶堚偓鍌濈箲閸?true = 閸忓秶鏌呴張顒侇偧娴笺倕顔婇妴?*/
    public static boolean rollDodge(ServerPlayer player) {
        if (!FormUtils.isForm(player, FormIdentifiers.OCELOT_NOVA)) return false;
        float chance = (float) BAL.d("base_dodge", BASE_DODGE);
        Float extra = DODGE.get(player.getUUID());
        Long exp = DODGE_EXPIRE.get(player.getUUID());
        if (extra != null && exp != null && player.level().getGameTime() <= exp) {
            chance += extra;
        }
        chance = Math.min((float) BAL.d("dodge_cap", DODGE_CAP), chance);
        return player.getRandom().nextFloat() < chance;
    }

    private static void addDodge(ServerPlayer player) {
        float cap = (float) BAL.d("dodge_cap", DODGE_CAP);
        float cur = DODGE.getOrDefault(player.getUUID(), 0f);
        DODGE.put(player.getUUID(), Math.min(cap, cur + (float) BAL.d("dodge_per_leap", DODGE_PER_LEAP)));
        DODGE_EXPIRE.put(player.getUUID(), player.level().getGameTime() + BAL.i("dodge_duration", DODGE_DURATION));
    }

    // ==== 閻忎絻绌梻顏囬煩閿涘牊顐奸幎鈧懗?sp_secondary閿?===

    public static void tryLeap(ServerPlayer player) {
        if (!FormUtils.isForm(player, FormIdentifiers.OCELOT_NOVA)) return;
        long now = player.level().getGameTime();
        // 鏉堟挸鍙嗛崢缁樺閿涙ower active_self(cooldown=1) 閺勵垳鏁搁獮瀹犘曢崣鎴礉娑撯偓濞嗭紕澧块悶鍡樺瘻闁款喕绱伴幐浣虹敾婢?tick 闁插秴顦茬憴锕€褰傞張顒佹煙濞夋洏鈧?
        // 鐠佹澘缍嶉張鈧潻鎴滅濞喡扳偓灞炬暪閸掓媽袝閸欐垯鈧秶娈?tick閿涘牊妫ょ拋鍝勵洤娴ｆ洟鍏橀弴瀛樻煀閿涘绱遍懟銉ㄧ獩娑撳﹥顐肩憴锕€褰?< GAP閿涘矁顫嬫稉鍝勬倱娑撯偓濞嗏剝瀵滄担蹇曟畱闁插秴顦?閳?韫囩晫鏆愰妴?
        // 鏉╂瑦鐗遍幐澶夌秶/閸楁洖鍤崣顏囶吇缁楊兛绔村▎鈽呯礉閺夋儳绱戦敍鍫濅粻濮濄垼袝閸欐埊绱氶崥搴″晙閹稿澧犵粻妤佹煀閻ㄥ嫪绔村▎鈽呯礉濮濓絿鈥橀崠鍝勫瀻閸楁洖鍤稉搴ょ箾閹稿鈧?
        Long lastInput = LEAP_LAST_INPUT.put(player.getUUID(), now);
        if (lastInput != null && now - lastInput < LEAP_INPUT_GAP) {
            return;
        }
        LeapSession leap = LEAPS.get(player.getUUID());
        if (leap != null && github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.currentCastId(player, LEAP_SKILL_ID) != leap.castId()) {
            LEAPS.remove(player.getUUID());
            leap = null;
        }
        int count;
        if (leap == null || now - leap.firstTick() > BAL.i("leap_window", LEAP_WINDOW)) {
            // 閺傛壆娈戞稉鈧潪顕嗙窗閻ф槒顔囬弬鑺ユ杹閿涘牅琚卞▓闈涙倱鐏炵偘绔村▎鈩冩煢閺€鎾呯礉CD 閸︺劎顑?2 濞堝灚鍨ㄧ搾鍛崶閺冭埖澧犵挧椋庣暬閿?
            leap = beginLeap(player, now);
            if (leap == null) return;
            count = 1;
            LEAPS.put(player.getUUID(), leap);
        } else {
            count = 2;
        }
        if (count == 1) {
            // 缁?1 濞堢數浼掔捄鍐╁灇閸旂噦绱伴幒銊┾偓浣虹崶閸欙絾妞傞梹璺ㄧ舶鐎广垺鍩涚粩顖ょ礄濞嗏剝濡ч懗鍊熺窡閸斺晜鐖懛顏呭姬閺嶇厧鈧帗鏆熼妴宀€顑?2 濞堜絻绻冮張鐔告闂傛番鈧稄绱?
            github.com.gengyoubo.sscfa.network.SscAddonNetworking.syncNovaLeapWindow(player,
                    BAL.i("leap_window", LEAP_WINDOW));
        }
        // 閸氭垵鍣弰鐔告煙閸氭垼鐑﹂崘璇х礄缁岃桨鑵戦崣顖滄暏閿?
        Vec3 look = player.getLookAngle();
        double leapPower = BAL.d("leap_power", LEAP_POWER);
        double horizontalPower = NovaPerks.horizontalSpeed(leapPower, leap.enhanced());
        player.setDeltaMovement(look.x * horizontalPower, Math.max(0.42, look.y * leapPower + 0.25), look.z * horizontalPower);
        player.hurtMarked = true;
        addDodge(player); // +20% 闂傤亪浼?
        // 閻忎絻绌划鎺戠摍閿涙俺鍓兼稉瀣閸︽壆鍎悳?+ 濞岃儻鐑﹂崘鍙夋煙閸氭垹鐗粚鐑樺珛鐏?+ 闁挎劖鐨甸弳鏉戝毊閿涘牊鏅遍幑椋庝紥閸旑煉绱?
        if (player.level() instanceof ServerLevel sw) {
            // 閼存矮绗呴煫顒€婀撮悜鐔哄箚閿涘牊閮ㄩ懘姘瑓娑撯偓閺佹潙婀€閸у洤瀵戦崚鍡楃閿涘矁瀹崷鎵瀻閸欐垶鍔呴妴渚€娼崶鍝勭暰閻愮櫢绱?
            spawnRing(sw, player, ParticleTypes.CLOUD, player.getX(), player.getY() + 0.08, player.getZ(),
                    0.6, 12, 0.05);
            // 濞岃儻鐑﹂崘鍙夋煙閸氭垹娈戦惍瀵糕敄閹锋牕鐔敍鍫ユ敿濮樻梹绁︾痪鍖＄礉閺佸繑宓庨悘闈涘З閿涙卜wner 閹垫挻鐖ｉ敍姘拱娴滆櫣顑囨稉鈧禍铏剐為柆鑳唨閿?
            for (int i = 1; i <= 6; i++) {
                double t = i / 6.0;
                github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(sw, player, ParticleTypes.CLOUD,
                        player.getX() + look.x * t * 1.6,
                        player.getY() + 0.5 + look.y * t * 1.6,
                        player.getZ() + look.z * t * 1.6,
                        1, 0.05, 0.05, 0.05, 0.02);
            }
            github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(sw, player, ParticleTypes.CRIT, player.getX(), player.getY() + 0.6, player.getZ(),
                    10, 0.3, 0.3, 0.3, 0.35);
        }
        // 閻忎絻绌敍姘瑜伴亶妫悳甯礄楠炵粯婀崇敮鍫ユ殔閸嶅繒鐏涚粔浼欑礆+ 鏉炶崵娉╅惍瀵糕敄閿涘本鏅遍幑椋庝紥閸?
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 0.8F, 1.4F);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.5F, 1.8F);
        if (count >= 2) {
            github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.ended(player, LEAP_SKILL_ID, leap.castId());
            LEAPS.remove(player.getUUID());
            // 缁?2 濞堢數鏁ら幒澶涚窗缁愭褰涢崗鎶芥４閿涘牐绶熼崝鈺傜埉濞戝牆銇戦敍?
            github.com.gengyoubo.sscfa.network.SscAddonNetworking.syncNovaLeapWindow(player, 0);
        }
    }

    // ==== 閼稿秷闊╅悥鍡欏仮閿涘牅瀵岄幎鈧懗?sp_primary閿?===

    public static void startCharge(ServerPlayer player) {
        if (!FormUtils.isForm(player, FormIdentifiers.OCELOT_NOVA)) return;
        long now = player.level().getGameTime();
        if (CHARGE_START.containsKey(player.getUUID())) return; // 瀹告彃婀拑鍕
        if (PowerUtils.getResourceValue(player, FormIdentifiers.OCELOT_NOVA_NINE_LIVES) <= 0) return; // 閺冪姴鎳℃稉宥堝厴閼奉亞鍨?
        if (!github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.begin(player, EXPLODE_SKILL_ID)) return;
        // 鐠ч攱澧滆箛顐ゅ弾閿涙碍婀板▎锟犲櫞閺€鎯у弿缁嬪绱欐潻娑樺閸掋倕鐣?鐟欏棜顫?閻栧棛鍋㈢紒鎾剁暬閿涘顕版导姘崇樈鐎涙顔岄敍灞肩瑝闂?reload 閹广垹寮?
        ChargeSession session = new ChargeSession(github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.currentCastId(player, EXPLODE_SKILL_ID), now,
                BAL.i("charge_time", CHARGE_TIME),
                BAL.i("lethal_radius", LETHAL_RADIUS),
                BAL.i("max_radius", MAX_RADIUS),
                (float) BAL.d("max_damage", MAX_DAMAGE), NpcCombatEnhancements.novaMaxDamage());
        CHARGE_START.put(player.getUUID(), session);
        // 娴狅絿鐖滈崙蹇涒偓?70%閿涘牆鐫橀幀褌鎱ㄩ弨鐟版珤 MULTIPLY_TOTAL -0.70閿涘瞼绨跨涵顕€娼懡顖涙寜缂傛挻鍙冮敍? 閹舵鈧?III
        applyChargeSlow(player);
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,
                session.chargeTime(), 2, false, false, true));
        // 閽冨嫬濮忕挧閿嬪閿涙碍绻佸▽澶庡厴闁插繑鐪归懕姘剧礄鐎佃偐顓稿┑鈧ú璁崇秵鐠嬪啰澧楅敍? TNT 閻愬湱鍣у鏇氫繆婢瑰府绱濇０鍕仛閸楅亶娅撻崗鍛板厴
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.CONDUIT_ACTIVATE, SoundSource.PLAYERS, 1.0F, 0.6F);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.TNT_PRIMED, SoundSource.PLAYERS, 1.2F, 1.0F);
        // 閺嶅洩顔囬拑鍕娑?閳?閸氬本顒炵€广垺鍩涚粩顖ょ礉娓氭盯妫幒褏澧?sneaking_speed_up 閸掋倖鏌囬敍鍫ｆ惈閸旀稒婀＄粋?shift 濞兼粏顢戦崝鐘烩偓鐕傜礆
        PowerUtils.setResourceValueAndSync(player, FormIdentifiers.OCELOT_NOVA_CHARGING, 1);
    }

    /** 閺傝棄濮為拑鍕閺堢喍鍞惍浣稿櫤闁?70%閿涘牆鍘涚粔濠氭珟閸?UUID 閺冄傛叏閺€鐟版珤闂冩彃褰旈崝鐙呯礆閵?*/
    private static void applyChargeSlow(ServerPlayer player) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(CHARGE_SLOW_UUID);
            speed.addTransientModifier(new AttributeModifier(
                    CHARGE_SLOW_UUID, "Nova Charge Slow", BAL.d("charge_slow_amount", CHARGE_SLOW_AMOUNT),
                    AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    /** 缂佹挻娼拑鍕閿涙氨些闂勩倕鍣洪柅鐔舵叏閺€鐟版珤 + 濞撳懓鎼崝娑欑垼鐠佸府绱欑憴锝夋珟缁備胶鏌х捄鎴礆+ 濞撳懓鎼崝娑滎吀閺冭翰鈧倹澧嶉張澶岀波閺夌喕鐭惧鍕埠娑撯偓鐠х増顒濋弬瑙勭《閿涘矂妲婚崙蹇涒偓鐔哥濠曞繈鈧?*/
    public static void interruptControl(ServerPlayer player) {
        if (CHARGE_START.containsKey(player.getUUID())) endCharge(player);
        LeapSession leap = LEAPS.remove(player.getUUID());
        if (leap != null) {
            finishSingleLeap(player, leap);
            github.com.gengyoubo.sscfa.network.SscAddonNetworking.syncNovaLeapWindow(player, 0);
        }
        LEAP_LAST_INPUT.remove(player.getUUID());
    }

    private static void endCharge(ServerPlayer player) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(CHARGE_SLOW_UUID);
        }
        PowerUtils.setResourceValueAndSync(player, FormIdentifiers.OCELOT_NOVA_CHARGING, 0);
        ChargeSession removed = CHARGE_START.remove(player.getUUID());
        // 閼奉亞鍨庣捄顖氱窞瀹告彃鍘?completed閿涘本顒濇径鍕礋缁岀儤鎼锋担婊愮幢閸欐ê鑸?閼奉亝鍓ょ捄顖氱窞閸掓瑦妫ら幆鈺冪稈娑擃厽顒?
        github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.cancelled(player, EXPLODE_SKILL_ID, removed == null ? -1 : removed.castId());
        github.com.gengyoubo.sscfa.network.SustainedVisuals.stop(player,
                github.com.gengyoubo.sscfa.network.VisualRecipe.Kind.NOVA_CHARGE);
    }

    private static void explode(ServerPlayer player, ChargeSession session) {
        if (!(player.level() instanceof ServerLevel sw)) return;
        // 鐠囨槒鎹ｉ幍瀣╃窗鐠囨繂鎻╅悡褝绱欓梼?reload 閸氬海绮ㄧ粻妤佸床閸欏偊绱氶敍姘冲毀閸涜棄宕愬?閺堚偓鏉╂粌宕愬?娴笺倕顔婇崸鍥ф噯鐠ч攱澧滈崐?
        int lethalRadius = session.lethalRadius();
        int maxRadius = session.maxRadius();
        float maxDamage = session.maxDamage();
        // 閼稿秷闊╂禒锝勭幆閿涘牊绉烽懓?1 閸涙枻绱氭稉宥呮躬濮濄倕顦╅惄瀛樺复閹碉綇绱濋弨鍦暠缂佹挸鐔€电鍤滃鍗炵穿閻栧棜袝閸欐垳绡€閸涜棄顦插ú缁樻降閹碉綇绱濋柆鍨帳闁插秴顦查幍锝呮嚒閵?
        List<LivingEntity> targets = sw.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(maxRadius), e -> e != player && e.isAlive());
        for (LivingEntity e : targets) {
            if (WhitelistUtils.isProtected(player, e)) continue; // 閺冪姴寮告导?
            double dist = e.distanceTo(player);
            if (dist > maxRadius) continue;
            float dmg;
            float targetMaxDamage = NpcCombatEnhancements.isEnemyNpc(player, e) ? session.npcMaxDamage() : maxDamage;
            if (dist <= lethalRadius) {
                dmg = targetMaxDamage;
            } else {
                dmg = targetMaxDamage * (1.0f - (float) ((dist - lethalRadius) / (maxRadius - lethalRadius)));
            }
            if (dmg <= 0) continue;
            e.hurt(sw.damageSources().explosion(player, player), dmg); // 瑜版帒鐫橀悳鈺侇啀閵嗕焦妫ら惍鏉戞綎閺傜懓娼?
        }
        // 閻栧棛鍋㈤懠鍐ㄦ纯閸欘垵顫嬮崠鏍电窗娑撳鐪伴崷鍫濆瑎閸曟帒鐤勯梽鍛板瘱閸ョ补鈧柡鈧棁鍤ч崨钘夋箑閿涘牊娈痪顫礉閺堚偓闁辨帞娲伴敍澶嗗晪 鐞涙澘鍣虹敮锔肩礄濮楁瑧浼嗘潻鍥ㄦ诞閿涘鍟?閺堚偓鏉╂粍灏濋崣濠傛箑閿涘牊璐伴悜鐔奉樆濞屽尅绱氶妴?
        // 濮ｅ繐婀€濞屾寧鏆ｉ崷鍫濆З閹礁鍨庣敮鍐跨礄闂呭繑婧€鐠у嘲顫愮憴?+ 鐟欐帒瀹?瀵板嫬鎮滈幎鏍уЗ閿涘绱濋棃鐐叉祼鐎规氨鍋ｉ妴浣哥湴濞嗏剝绔婚弲鎷屸偓灞肩瑝閸掕櫣婧傞妴?
        double ringY = player.getY() + 0.15;
        DustParticleOptions lethalDust = new DustParticleOptions(new Vector3f(0.82f, 0.14f, 0.12f), 1.2f);
        // 閼煎啫娲挎０鍕劅閸﹀牞绱檕wner=null 娑撳秵澧﹂弽鍥风礆閿涙碍婀版禍铏诡儑娑撯偓娴滆櫣袨娑旂喕顩﹂惇瀣閻栧棛鍋㈤懠鍐ㄦ纯閿涘奔绗夐崑姘朵缉鐠?
        spawnRing(sw, null, lethalDust, player.getX(), ringY, player.getZ(), lethalRadius, lethalRadius * 10, 0.25);
        double midRadius = (lethalRadius + maxRadius) / 2.0;
        DustParticleOptions midDust = new DustParticleOptions(new Vector3f(0.62f, 0.36f, 0.22f), 1.0f);
        spawnRing(sw, null, midDust, player.getX(), ringY, player.getZ(), midRadius, (int) (midRadius * 7), 0.35);
        spawnRing(sw, null, ParticleTypes.SMOKE, player.getX(), ringY, player.getZ(), maxRadius, maxRadius * 5, 0.45);
        // 娑擃厼绺鹃悥鍡欏仮娑撹缍?+ 閻戠喍绨敍鍧ner 閹垫挻鐖ｉ敍姘矌閺堫兛姹夌粭顑跨娴滆櫣袨闁儻顔€閿涙稓鍨庨悙绋挎箑閺勵垵瀵栭崶鎾暕鐠€锔跨箽閻ｆ瑥甯弽鍑ょ礆
        github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(sw, player, ParticleTypes.EXPLOSION_EMITTER, player.getX(), player.getY() + 0.5, player.getZ(), 1, 0, 0, 0, 0);
        github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(sw, player, ParticleTypes.LARGE_SMOKE, player.getX(), player.getY() + 0.5, player.getZ(), 30, 1.5, 1.0, 1.5, 0.1);
        // 閼奉亞鍨庡鏇犲瀻閿涙岸鐓堕悥鍡楀暱閸?+ 閸樻岸鍣搁悥鍡欏仮 + 閺堫偄濂栨Λ娆庣稇婵炰緤绱濇稉澶婄湴閸欑姴鍤В浣轰純閹?
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.0F, 0.9F);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.8F, 0.7F);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 0.7F, 1.0F);
        // 閼稿秷闊╅敍姘冲殰瀹稿彉绡冪悮顐ゅ仮閸婃帪绱濇稊婵嗘嚒韫囧懎鐣炬径宥嗘た閿涘牏绮潻鍥ь槻濞?cd 娑撳酣妫柆瑁も偓浣稿櫍閼?1 閸涙枻绱氶垾鏂衡偓鏃€鑽￠悘顐﹀櫢閻?
        NineLivesManager.reviveForSelfDetonate(player);
    }

    // ==== 缁帒鐡欐潏鍛И ====

    /** 濞屽灝宕愬?radius 閻ㄥ嫭鎸夐獮鍐叉妇閸涖劌娼庨崠鈧幘?count 娑擃亞鐭戠€涙劧绱欑挧宄邦潗鐟欐帡娈㈤張?+ 鐟欐帒瀹?瀵板嫬鎮滈幎鏍уЗ閿涘矂浼╅崗宥呮啘閺夊灝娴愮€规氨鍋ｉ敍澶堚偓淇箇ner 闂堢偟鈹栭弮鑸靛ⅵ閺嶅洭浼╃拋鈹库偓?*/
    private static void spawnRing(ServerLevel sw, net.minecraft.world.entity.Entity owner, ParticleOptions particle, double cx, double cy, double cz,
                                  double radius, int count, double yJitter) {
        double start = sw.random.nextDouble() * Math.PI * 2;
        double step = Math.PI * 2 / count;
        for (int i = 0; i < count; i++) {
            double angle = start + step * i + (sw.random.nextDouble() - 0.5) * step * 0.7;
            double r = radius + (sw.random.nextDouble() - 0.5) * 0.5;
            github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(sw, owner, particle, cx + Math.cos(angle) * r, cy + (sw.random.nextDouble() - 0.5) * yJitter,
                    cz + Math.sin(angle) * r, 1, 0, 0, 0, 0);
        }
    }

    private static LeapSession beginLeap(ServerPlayer player, long now) {
        var spec = github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.spec(player, LEAP_SKILL_ID);
        if (spec == null || !github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.begin(player, LEAP_SKILL_ID)) return null;
        boolean enhanced = NovaPerks.hasLongPounce(player);
        long castId = github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.currentCastId(player, LEAP_SKILL_ID);
        if (enhanced) github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.retune(player, LEAP_SKILL_ID, castId,
                NovaPerks.fullCooldown(spec.cooldown(), true));
        return new LeapSession(castId, now, NovaPerks.singleCooldown(spec.extra("single_use"), enhanced), enhanced);
    }

    private static void finishSingleLeap(ServerPlayer player, LeapSession leap) {
        github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.retune(player, LEAP_SKILL_ID, leap.castId(), leap.singleUseCooldown());
        github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.ended(player, LEAP_SKILL_ID, leap.castId());
    }

    // ==== tick ====

    private static void tickPlayer(ServerPlayer player) {
        if (!player.isAlive() || !FormUtils.isForm(player, FormIdentifiers.OCELOT_NOVA)) {
            LeapSession leap = LEAPS.remove(player.getUUID());
            if (leap != null) {
                github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.cancelled(player, LEAP_SKILL_ID, leap.castId());
                github.com.gengyoubo.sscfa.network.SscAddonNetworking.syncNovaLeapWindow(player, 0);
            }
            LEAP_LAST_INPUT.remove(player.getUUID());
            // 閽冨嫬濮忔稉顓⑩偓鏂垮瀼閹广垹鑸伴幀渚婄窗鐏炵偞鈧傛叏閺€鐟版珤娑撳秹娈?apoli power 濞戝牆銇戦敍宀勩€忛幍瀣З鐟欙綁娅庨崙蹇涒偓鐔剁瑢缁備胶鏌х捄鎴炵垼鐠?
            if (CHARGE_START.containsKey(player.getUUID())) {
                endCharge(player);
            }
            return;
        }
        long now = player.level().getGameTime();
        // 闂傤亪浼╅崝鐘冲灇鏉╁洦婀″〒鍛倞
        Long exp = DODGE_EXPIRE.get(player.getUUID());
        if (exp != null && now > exp) {
            DODGE.remove(player.getUUID());
            DODGE_EXPIRE.remove(player.getUUID());
        }
        // 閻忎絻绌敍姘涧閻?1 濞嗏€茬瑬鐡掑懓绻冪粣妤€褰?閳?閺堫剝鐤嗙紒鎾存将閿涘瓔D 閺€鍦暏 extra_cooldowns.single_use
        LeapSession leap = LEAPS.get(player.getUUID());
        if (leap != null && now - leap.firstTick() > BAL.i("leap_window", LEAP_WINDOW)) {
            LEAPS.remove(player.getUUID());
            finishSingleLeap(player, leap);
            // 缁愭褰涙潻鍥ㄦ埂閺堫亞鏁ょ粭?2 濞堢绱扮粣妤€褰涢崗鎶芥４閿涘牐绶熼崝鈺傜埉濞戝牆銇戦敍?
            github.com.gengyoubo.sscfa.network.SscAddonNetworking.syncNovaLeapWindow(player, 0);
        }
        // 閼稿秷闊╅悥鍡欏仮閽冨嫬濮忛敍姘剁拨閻戠喓鐭戠€?+ 閽冨嫭寮ч懛顏嗗瀻閿涘牐绻樻惔锔跨瑢閸楀﹤绶為崗銊╁劥鐠囪绱扮拠婵嗘彥閻撗嶇礉reload 娑撳秵宕查崣鍌︾礆
        ChargeSession session = CHARGE_START.get(player.getUUID());
        if (session != null) {
            long cs = session.startTick();
            int chargeTime = session.chargeTime();
            int lethalRadius = session.lethalRadius();
            int maxRadius = session.maxRadius();
            if (player.level() instanceof ServerLevel) {
                double chargeProgress = (now - cs) / (double) chargeTime; // 0閳? 閽冨嫬濮忔潻娑樺
                github.com.gengyoubo.sscfa.network.SustainedVisuals.touch(player,
                        github.com.gengyoubo.sscfa.network.VisualRecipe.Kind.NOVA_CHARGE,
                        (int) (now - cs), chargeTime, lethalRadius, maxRadius);
                // 閽冨嫬濮忛崗鍛板厴闂婄绱扮€佃偐顓告担搴ㄧ鎼存洖娅旈敍灞剧槨 0.5s 娑撯偓濞喡扳偓渚€娈㈡潻娑樺閸楀洩鐨熼敍鍫ョ叾闁插繐甯囨担搴ゎ唨娴?TNT 瀵洑淇婃竟甯礆
                if ((now - cs) % 10 == 0) {
                    float pitch = 0.7F + 0.8F * (float) chargeProgress;
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.CONDUIT_AMBIENT, SoundSource.PLAYERS, 0.4F, pitch);
                }
                // TNT 瀵洑淇婇妴灞炬樊濠婃番鈧秴锛愰敍姘槨 0.6s 娑撯偓濞嗏槄绱濋梾蹇氱箻鎼达附绗庨崫宥嗙瑤閹儻绱欓悙鍦櫑閸氬孩瀵旂紒顓犲櫑閻懷勫妳閿?
                if ((now - cs) % 12 == 0) {
                    float fuseVol = 0.5F + 0.4F * (float) chargeProgress;
                    float fusePitch = 1.0F + 0.3F * (float) chargeProgress;
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.TNT_PRIMED, SoundSource.PLAYERS, fuseVol, fusePitch);
                }
                // 娑撳鍨庨崜?1 缁夋帪绱伴惄鎴濈暓閼板懎绺剧捄鎶筋暕鐠€锔肩礄濮?0.5s閿涘绱濋幓鎰仛閸楀啿鐨㈠鏇犲瀻
                if (now - cs >= chargeTime - 20 && (now - cs) % 10 == 0) {
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.0F, 0.8F);
                }
            }
            if (now - cs >= chargeTime) {
                explode(player, session);
                github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.completed(player, EXPLODE_SKILL_ID, session.castId());
                endCharge(player); // 閼奉亞鍨庣€瑰本鐦敍姘承掗梽銈呭櫤闁?+ 缁備胶鏌х捄鎴炵垼鐠?+ 濞撳懓鎼崝娑滎吀閺?
            }
        } else if (PowerUtils.getResourceValue(player, FormIdentifiers.OCELOT_NOVA_CHARGING) > 0) {
            // 閺傤厾鍤?闁插秴鎯庨崥搴ゆ惈閸旀稖顓搁弮璁圭礄閸愬懎鐡ㄩ幀渚婄礆娑撱垹銇戞担鍡氭惈閸旀稒鐖ｇ拋甯礄閹镐椒绠欓崠鏍カ濠ф劧绱氬▓瀣殌 閳?閼奉亝鍓ら敍宀勪缉閸忓秵妗堟稊鍛櫤闁喍绗岀粋浣烘煣鐠?
            endCharge(player);
        }
    }
}
