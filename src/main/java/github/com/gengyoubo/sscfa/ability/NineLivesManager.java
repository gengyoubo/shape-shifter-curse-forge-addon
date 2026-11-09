/*
 * Copyright (c) 2026 MangZai-120
 * This file is part of the "shape shifter curse addon" project.
 * Licensed under the GNU Affero General Public License v3.0 (AGPL-3.0).
 */
package github.com.gengyoubo.sscfa.ability;

import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.FormUtils;
import github.com.gengyoubo.sscfa.util.PowerUtils;
import github.com.gengyoubo.sscfa.util.TrinketUtils;
import github.com.gengyoubo.sscfa.util.WhitelistUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 閺堟梹婀滈妴灞肩瘈閸涘鈧秵鐗宠箛鍐吀閻炲棗娅掗敍鍫熸箛閸旓紕顏弶鍐ㄢ枆閿涘鈧?
 * <p>
 * 閺堝搫鍩楅敍?b>鐞氼偄濮╁璁抽鐟欙箑褰?/b>閳ユ柡鈧棁鍤у璁虫縺鐎硅櫕妞傞懟銉ュ⒖娴ｆ瑥鎳￠弫?&gt; 0閿涘本绉烽懓?1 閸涜棄鑻熼崢鐔锋勾婢跺秵妞?
 * 閿涘牆娲?6 鐞涒偓 + 6 妫版绮嶈箛鍐ㄦ儧閺€?30s + 1s 閺冪姵鏅?+ 閸忋劌鎲抽崣顖炴閻ㄥ嫬顦插ú濠氱叾閺?閻楄鏅ラ敍澶涚幢鐟欙箑褰傞崥?3s 閸愬懎鍟€濮濊楠告稉宥埿曢崣鎴礄閻喐顒撮敍澶堚偓?
 * 閸涜姤鏆熸稉濠囨 9閿?b>閼磋京顬囬幋妯绘灍</b>閿?0s 閺堫亜褰堟导?閺堫亝鏁鹃崙浼欑礆閸氬孩鐦?20s 閸?1 閸涙枻绱遍惇鐔割劥闁插秶鏁撻崥搴℃礀濠?9 閸涘鈧?
 * 閸忋劑鍎撮崚銈呯暰閸︺劍婀囬崝锛勵伂閿涘苯顦挎禍杞扮閼锋番鈧?
 */
public final class NineLivesManager {
    // ==== 娴犮儰绗呯敮鎼佸櫤娑撴椽绮拋銈呪偓纭风幢鏉╂劘顢戦弮鏈电矤 balance 韫囶偆鍙庣拠璇插絿閿涘澃cope: abilities.nine_lives閿涘本鏆熼幑顔煎瘶閸欘垵顩惄鏍电礆====
    private static final int MAX_LIVES = 8;             // 姒涙顓婚崨鑺ユ殶娑撳﹪妾?
    private static final int REGEN_INTERVAL = 400;      // 姒涙顓婚敍姘冲姎閹存ɑ鐦?20s 閸?1 閸?
    private static final int OUT_OF_COMBAT_TICKS = 200; // 姒涙顓婚敍?0s 閺堫亝鍨弬妤冪暬閼磋鲸鍨?
    private static final int REVIVE_CD_TICKS = 60;      // 姒涙顓婚敍姘槻濞茶鎮?3s 閸愬懍绗夐崘宥埿曢崣鎴礄閻喐顒寸粣妤€褰涢敍?
    private static final int INVULN_TICKS = 20;         // 姒涙顓婚敍姘槻濞茶鎮?1s 閺冪姵鏅?
    private static final float REVIVE_HEAL = 6.0f;      // 姒涙顓婚敍姘槻濞茶娲栫悰鈧?
    private static final int ABSORB_DURATION = 600;     // 姒涙顓婚敍? 妫版绮嶈箛鍐ㄦ儧閺€鑸靛瘮缂?30s
    private static final int ABSORB_AMPLIFIER = 2;      // 姒涙顓婚敍娆癰sorption III = 6 妫版绮嶈箛?
    private static final float REVIVE_HEAL_NECKLACE = 8.0f; // 姒涙顓婚敍姘煋閺堟梹婀滄稉鎾崇潣妞ゅ綊鎽兼径宥嗘た閸ョ偠顢?8
    private static final int INVULN_TICKS_NECKLACE = 36;    // 姒涙顓婚敍姘煋閺堟梹婀滄稉鎾崇潣妞ゅ綊鎽兼径宥嗘た閺冪姵鏅?1.8s

    /** 閺堝秴濮熺粩顖涙綀婵?balance 韫囶偆鍙庣拠璇插絿閿涘牆鎻╅悡褎婀崚婵嗩潗閸栨牗妞傞崶鐐衡偓鈧稉濠冩煙姒涙顓荤敮鎼佸櫤閿涘鈧?*/
    private static final BalanceReader BAL = new BalanceReader("abilities.nine_lives");

    private static final Map<UUID, Long> LAST_COMBAT = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> REVIVE_TICK = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> INVULN_END = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> REGEN_ACC = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> RESPAWN_REFILL = new ConcurrentHashMap<>();

    private NineLivesManager() {
    }

    public static void init() {
        github.com.gengyoubo.sscfa.util.SscEvents.onServerTick(server -> {
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                tickPlayer(p);
            }
        });
        // 閺傤厾鍤庡〒鍛倞閻樿埖鈧浇銆冮敍姘舵Щ濮濄垻顬囩痪璺ㄥ负鐎硅埖娼惄顔界暙閻ｆ瑱绱欑紓鎾村弮濞夊嫭绱￠敍澶涚礉娑旂喖浼╅崗宥夊櫢鏉╃偛鎮楀▓瀣殌閺?CD/閽冨嫬濮忕挧椋庡仯
        github.com.gengyoubo.sscfa.event.PlayerSessionCleanup.register((handler, server) -> {
            UUID id = handler.player.getUUID();
            LAST_COMBAT.remove(id);
            REVIVE_TICK.remove(id);
            INVULN_END.remove(id);
            REGEN_ACC.remove(id);
            RESPAWN_REFILL.remove(id);
        });
        // 閻喐顒撮柌宥囨晸閸氬骸娲栧?9 閸涙枻绱欏鎯扮箿閸?tick 闁插矁顔曢敍宀€鈥樻穱婵嬪櫢閻㈢喎鎮?power 瀹稿弶宸挎禍鍫礆
        github.com.gengyoubo.sscfa.util.SscEvents.afterRespawn((oldPlayer, newPlayer, alive) ->
                RESPAWN_REFILL.put(newPlayer.getUUID(), Boolean.TRUE));
    }

    /** 閺嶅洩顔囨潻娑樺弳閹存ɑ鏋熼敍鍫濆綀娴笺倖鍨ㄩ弨璇插毊閺冩儼鐨熼悽顭掔礆閵?*/
    public static void markCombat(ServerPlayer player) {
        LAST_COMBAT.put(player.getUUID(), player.level().getGameTime());
    }

    /** 閺勵垰鎯侀懘杈╊瀲閹存ɑ鏋熼敍鍫ｇТ鏉?10s 閺堫亝鍨弬妤嬬礆閵?*/
    public static boolean isOutOfCombat(ServerPlayer player) {
        long last = LAST_COMBAT.getOrDefault(player.getUUID(), Long.MIN_VALUE / 2);
        return player.level().getGameTime() - last > BAL.i("out_of_combat_ticks", OUT_OF_COMBAT_TICKS);
    }

    /** 婢跺秵妞块崥搴㈡￥閺佸瞼鐛ラ崣锝忕礄濮濄倖婀￠梻?mixin 閸欐牗绉锋稉鈧崚鍥︽縺鐎圭绱氶敍娑欐闂€璺ㄦ暠婢跺秵妞块弮鑸垫Ц閸氾附鍩撻張鏃€婀滄稉鎾崇潣妞ゅ綊鎽奸崘鍐茬暰閿?s / 1.8s閿涘鈧?*/
    public static boolean isInvulnerable(ServerPlayer player) {
        long end = INVULN_END.getOrDefault(player.getUUID(), Long.MIN_VALUE / 2);
        return player.level().getGameTime() < end;
    }

    /**
     * 閼峰瓨顒存导銈咁唺閺冭泛鐨剧拠鏇犳暏娑旀繂鎳℃径宥嗘た閵嗗倽绻戦崶?true 鐞涖劎銇氬鎻掝槻濞蹭紮绱欑拫鍐暏閺傜懓绨查崣鏍ㄧХ閺堫剚顐煎璁抽閿涘鈧?
     */
    public static boolean tryRevive(ServerPlayer player) {
        long now = player.level().getGameTime();
        long rev = REVIVE_TICK.getOrDefault(player.getUUID(), Long.MIN_VALUE / 2);
        if (now - rev < BAL.i("revive_cd_ticks", REVIVE_CD_TICKS)) {
            return false; // 婢跺秵妞块崥?3s cd 閸愬懍绗夌憴锕€褰傞敍鍫㈡埂濮濅紮绱?
        }
        int lives = PowerUtils.getResourceValue(player, FormIdentifiers.OCELOT_NOVA_NINE_LIVES);
        if (lives <= 0) {
            return false; // 閺冪姴鎳￠崣顖滄暏閿涘瞼婀″?
        }
        doRevive(player);
        return true;
    }

    /**
     * 閼稿秷闊╅懛顏嗗瀻娑撴挾鏁ゆ径宥嗘た閿涙氨绮潻鍥ь槻濞?cd閿涘牅绻氱拠浣碘偓灞界箑鐎规艾顦插ú姹団偓宥忕礆閿涘奔绮庣憰浣圭湴鐏忔碍婀侀崨鑺ユ殶閵?
     * 娓?{@link NovaSkillManager} 瀵洜鍨庨弮璺侯嚠閼奉亜绻佺拫鍐暏閳ユ柡鈧棁鍤滃鍙樼瘍鐞氼偆鍋㈤崐鎺炵礉娴ｅ棔绡€閸涜棄绻€鐎规碍甯存担蹇ョ礉閸戔偓濞戝牐鈧?1 閸涘鈧?
     */
    public static void reviveForSelfDetonate(ServerPlayer player) {
        int lives = PowerUtils.getResourceValue(player, FormIdentifiers.OCELOT_NOVA_NINE_LIVES);
        if (lives <= 0) {
            return; // 閸欐垵濮╅梻銊﹀付瀹歌弓绻氱拠浣规箒閸涙枻绱辨稉鍥︾閺冪姴鎳￠崚娆庣瑝婢跺秵妞块敍鍫滅瘍娑撳秶婀″浼欑礉閼奉亜绻佹稉宥呭綀閺堫剚顐奸懛顏嗗瀻瑜板崬鎼烽敍?
        }
        doRevive(player);
    }

    /**
     * 婢跺秵妞块弽绋跨妇閿涙碍绉烽懓?1 閸?+ 閸ョ偠顢?+ 6 妫版绮嶈箛?30s + 閺冪姵鏅?+ 閸忋劌鎲抽崣顖炴閻ㄥ嫬娴橀懙楣冪叾閺?閻楄鏅ラ妴?
     * 閹村瓨婀愰張娑楃瑩鐏炵偤銆嶉柧淇扁偓宀冪枂閸ョ偟灏楅惉鐐解偓宥嗘閿涙艾娲栫悰鈧?6閳?閵嗕焦妫ら弫?1s閳?.8s閿涘苯鑻熼崷銊ヮ槻濞茶崵鐏涢梻鎾缚闁偓+閸戝繘鈧喎鎳嗛崶瀛樻櫕娴滄亽鈧?
     */
    private static void doRevive(ServerPlayer player) {
        boolean hasNecklace = TrinketUtils.isWearing(player, github.com.gengyoubo.sscfa.init.AddonItems.NOVA_REVIVE_NECKLACE.get());
        float heal = hasNecklace
                ? (float) BAL.d("revive_heal_necklace", REVIVE_HEAL_NECKLACE)
                : (float) BAL.d("revive_heal", REVIVE_HEAL);
        int invuln = hasNecklace
                ? BAL.i("invuln_ticks_necklace", INVULN_TICKS_NECKLACE)
                : BAL.i("invuln_ticks", INVULN_TICKS);
        long now = player.level().getGameTime();
        PowerUtils.changeResourceValueAndSync(player, FormIdentifiers.OCELOT_NOVA_NINE_LIVES, -1);
        player.setHealth(heal);
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION,
                BAL.i("absorb_duration", ABSORB_DURATION), BAL.i("absorb_amplifier", ABSORB_AMPLIFIER), false, false, true));
        REVIVE_TICK.put(player.getUUID(), now);
        INVULN_END.put(player.getUUID(), now + invuln);
        github.com.gengyoubo.sscfa.spell.OcelotSpellLink.onNineLivesRevive(player);
        markCombat(player);
        // 婢跺秵妞块棅铏櫏閿涙艾鍙忛崨妯哄讲闂備紮绱欑粭顑跨閸?null閿涘绱濋棅鎶藉櫤闁倷鑵戦敍?.6閿涘浼╅崗宥堢箖閸?
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.6F, 1.2F);
        if (player.level() instanceof ServerLevel sw) {
            github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(sw, player, ParticleTypes.TOTEM_OF_UNDYING,
                    player.getX(), player.getY() + 1.0, player.getZ(), 40, 0.4, 0.6, 0.4, 0.2);
            if (hasNecklace) {
                reviveBurst(player, sw);
            }
        }
    }

    /** 閺堟梹婀滄稉鎾崇潣妞ゅ綊鎽兼稉鎾崇潣閿涙艾顦插ú鑽ょ仜闂傛潙顕崨銊ユ纯 4 閺嶇厧鍞撮棃鐐垫閸氬秴宕熼弫灞兼眽闂囧洭鈧偓 + 閸戝繘鈧噦绱濈紒娆掑殰瀹稿崬鏋犻幁顖溾敄闂傛番鈧?*/
    private static void reviveBurst(ServerPlayer player, ServerLevel sw) {
        for (LivingEntity e : sw.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(4.0), e -> e != player && e.isAlive())) {
            if (WhitelistUtils.isProtected(player, e)) {
                continue;
            }
            double dx = e.getX() - player.getX();
            double dz = e.getZ() - player.getZ();
            if (dx * dx + dz * dz < 1.0e-4) {
                dx = player.getLookAngle().x;
                dz = player.getLookAngle().z;
            }
            e.knockback(0.8, -dx, -dz);
            e.hurtMarked = true;
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1, false, true, true), player);
        }
        // owner 閹垫挻鐖ｉ敍姘矌閺堫兛姹夌粭顑跨娴滆櫣袨闁儻顔€閿涘奔绮禍楦款潒鐟欐帒甯弽?
        github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(sw, player, ParticleTypes.SWEEP_ATTACK, player.getX(), player.getY() + 0.5, player.getZ(), 8, 1.5, 0.3, 1.5, 0.0);
        sw.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 0.8F);
    }

    private static void tickPlayer(ServerPlayer player) {
        boolean isNova = FormUtils.isForm(player, FormIdentifiers.OCELOT_NOVA);
        int maxLives = BAL.i("max_lives", MAX_LIVES);
        // 闁插秶鏁撻崶鐐村姬 9 閸?
        if (Boolean.TRUE.equals(RESPAWN_REFILL.get(player.getUUID()))) {
            if (isNova && PowerUtils.hasResource(player, FormIdentifiers.OCELOT_NOVA_NINE_LIVES, 0)) {
                PowerUtils.setResourceValueAndSync(player, FormIdentifiers.OCELOT_NOVA_NINE_LIVES, maxLives);
                RESPAWN_REFILL.remove(player.getUUID());
            } else if (!isNova) {
                RESPAWN_REFILL.remove(player.getUUID());
            }
        }
        int lives = PowerUtils.getResourceValue(player, FormIdentifiers.OCELOT_NOVA_NINE_LIVES);
        if (!isNova || lives <= 0) {
            REGEN_ACC.remove(player.getUUID());
            return;
        }
        // 娑撳﹥鏌熺€瑰牆宕奸崢鐔惰礋 hasResource(...,0)閿涘牆鍞撮柈銊ュ嫉鐠囪绔村▎鈥虫倱娑撯偓鐠у嫭绨敍澶涚礉
        // 閻滅増鏁兼稉杞扮濞?getResourceValue 閸氬本妞傞崗鍛秼鐎涙ê婀幀褝绱?=0 鐟欏棔璐熼弮鐘虹カ濠?閺冪姴鎳￠敍澶夌瑢閺佹澘鈧厧鍨界€规熬绱濆В?tick 鐏忔垳绔村▎鈩冨閹诲繈鈧?
        if (lives >= maxLives) {
            REGEN_ACC.put(player.getUUID(), 0);
            return;
        }
        if (!isOutOfCombat(player)) {
            return; // 閹存ɑ鏋熸稉顓濈瑝閹垹顦?
        }
        int acc = REGEN_ACC.getOrDefault(player.getUUID(), 0) + 1;
        if (acc >= BAL.i("regen_interval", REGEN_INTERVAL)) {
            PowerUtils.changeResourceValueAndSync(player, FormIdentifiers.OCELOT_NOVA_NINE_LIVES, 1);
            acc = 0;
        }
        REGEN_ACC.put(player.getUUID(), acc);
    }
}
