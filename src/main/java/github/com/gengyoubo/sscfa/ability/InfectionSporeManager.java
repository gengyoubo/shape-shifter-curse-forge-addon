/*
 * Copyright (c) 2026 MangZai-120
 * This file is part of the "shape shifter curse addon" project.
 * Licensed under the GNU Affero General Public License v3.0 (AGPL-3.0).
 */
package github.com.gengyoubo.sscfa.ability;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.util.WhitelistUtils;
import org.joml.Vector3f;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 鐎靛嫮鏁撻弸婊嗘浆濞喡ゎ洣閹垛偓閼虫枻绱伴幇鐔哥厠鐎涖垹鐡欓悩鑸碘偓浣侯吀閻炲棗娅掗妴?
 * <p>
 * 閺堝秴濮熺粩顖涙綀婵炰緤绱濇径姘眽閻滎垰顣ㄦ稉瀣閺堝鍨界€规矮绗岀划鎺戠摍閸у洨鏁遍張宥呭缁旑垶鈹嶉崝銊ヨ嫙閹稿娓堕幎鏇⑩偓鎺旂舶鐎广垺鍩涚粩顖ょ礉
 * 娣囨繆鐦夋稉璇差吂閺堣桨绔撮懛缈犵瑢閺冪姴顓归張鍝勬鏉╃喆鈧?
 * <p>
 * 鐞涘奔璐熼敍?
 * - 閹扮喐鐓嬮惄顔界垼濮?25t閿?.25s閿涘奔绗屾稉顓熺槰 I 娑撯偓閼疯揪绱氶崣妤€鍩?1 閻愬湱娲块幒銉ゆ縺鐎圭偨鈧?
 * - 閹扮喐鐓嬮惄顔界垼 1.5 閺嶇厧鍞撮惃鍕弓閹扮喐鐓嬮妴渚€娼惂钘夋倳閸楁洜鏁撻悧鈺€绱扮悮顐＄炊閺屾搫绱濋崜鈺€缍戦弮鍫曟？缂佈勫閺傝棄濮為懓鍛⒖娴ｆ瑦妞傞梻娣偓?
 * - 瀹稿弶鍔呴弻鎾舵窗閺嶅洤鍟€濞喡ゎ潶閸涙垝鑵戦弮璺烘嫹閻ｃ儻绱欐稉宥呭煕閺傝埇鈧椒绗夐崣鐘插閿涘鈧?
 * - 閹扮喐鐓嬮張鐔兼？缁帒鐡欑悰銊у箛閿涙矮绮禍楦款潒鐟欐帗鐦?5t 娑撯偓濞嗭紕璞㈤懝鍙夊妳閺屾挾鐭戠€涙劧绱辩悮顐ｅ妳閺屾挾甯虹€规儼鍤滃杈潒鐟?1/4 妫版垹宸奸妴?
 */
public final class InfectionSporeManager {

    // 娴犮儰绗呴崸鍥﹁礋姒涙顓婚崐纭风幢鏉╂劘顢戦弮鏈电矤 balance 韫囶偆鍙庣拠璇插絿閿涘潊bilities.infection_spore閿涘鈧?
    // 鐢悂鍣烘穱婵堟殌 public 娓?pin 濞村鐦崣宥呯殸闁夸礁鐣炬妯款吇閸婄》绱濇潻鎰攽閺冩湹绗夌憰浣烘纯閹恒儱绱╅悽銊ｂ偓?
    /** 娴肩姵鐓嬮崡濠傜窞閿涘牊鐗搁敍?*/
    public static final double INFECT_SPREAD_RADIUS = 1.5;
    /** 娴肩姵鐓嬮幍顐ｅ伎闂傛挳娈ч敍鍧眎ck閿涘绱伴梼鑼參娴ｆ挻鍔呴弻鎾虫簚閺咁垱鐦?tick N 濞嗏€崇杽娴ｆ挸灏梻瀛樼叀鐠?*/
    public static final int SPREAD_SCAN_INTERVAL = 10;
    /** 娴犳牔姹夌憴鍡氼潡缁帒鐡欓崨銊︽埂閿涘牊婀惂鏄忣唶閿涘奔绗夊鈧弨鎾呯礆 */
    public static final int PARTICLE_INTERVAL_OTHERS = 5;
    /** 缁楊兛绔存禍铏剐炵划鎺戠摍閸涖劍婀￠敍鍫㈠ 1/4 妫版垹宸奸敍娑欐弓閻ф槒顔囬敍灞肩瑝瀵偓閺€鎾呯礆 */
    public static final int PARTICLE_INTERVAL_SELF = 20;
    /** 閺€璇插毊閼板懘鈧姵鍨氭导銈咁唺閸戝繐鍘ゅВ鏂剧伐 */
    public static final float DAMAGE_REDUCTION = 0.15f;

    /** balance 韫囶偆鍙庣拠璇插絿閿涘牆鎻╅悡褎婀崚婵嗩潗閸栨牕娲栭柅鈧妯款吇鐢悂鍣洪敍?*/
    private static final BalanceReader BAL = new BalanceReader("abilities.infection_spore");

    /** 鐞氼偅鍔呴弻鎾舵窗閺?UUID -> 閹扮喐鐓嬮弫鐗堝祦 */
    private static final Map<UUID, InfectionData> ENTRIES = new ConcurrentHashMap<>();
    /** 閸欏鏌熷▽鑽ゆ灍鐎涖垹鐡欓惄顔界垼 UUID -> 濞岃崵鏋熼弫鐗堝祦閿涘牊鐦?60t +1HP閿涘本瀵旂紒?durationTicks閿?*/
    private static final Map<UUID, HealData> HEAL_ENTRIES = new ConcurrentHashMap<>();
    /** 閸欏鏌熷▽鑽ゆ灍濮?tick 闁?*/
    public static final float TICK_HEAL = 1.0f;
    /** 濮ｆ帡娴橀弫灞兼眽濮ｅ繑顐奸幒澶庮攨闁?*/
    public static final float TICK_DAMAGE = 1.0f;
    /** 閸欏鏌熷▽鑽ゆ灍闂傛挳娈ч敍鍫滅瑢閹扮喐鐓嬮懞鍌氼殧娑撯偓閼疯揪绱? 缁夋帪绱?*/
    public static final int HEAL_INTERVAL = 60;

    private InfectionSporeManager() {
    }

    public static void init() {
        github.com.gengyoubo.sscfa.util.SscEvents.onWorldTick(InfectionSporeManager::onWorldTick);
        github.com.gengyoubo.sscfa.util.SscEvents.onServerStarting(server -> clearAll());
        github.com.gengyoubo.sscfa.util.SscEvents.onServerStopped(server -> clearAll());
    }

    public static void clearAll() {
        ENTRIES.clear();
        HEAL_ENTRIES.clear();
        CLOUDS.clear();
    }

    /** 閺傝棄濮為幇鐔哥厠閵嗗倸鎳℃稉顓炲嚒閹扮喐鐓嬮惄顔界垼閺冨墎娲块幒銉╁櫢缂冾喗妞傞梹鍖＄礄娣囨繄鏆€ caster閿涘鈧?*/
    public static void infect(ServerPlayer caster, LivingEntity target, int durationTicks) {
        if (!(target.level() instanceof ServerLevel serverWorld)) return;
        if (target == caster) return;
        // 閺傝棄濮炴稉鎾崇潣閵嗗奔鑵戝В鎺嬧偓宄泆ff閿涘牆娴橀弽?+ 閸涖劍婀￠幍锝堫攨閿涘本鏅ラ弸婊冩倱娑擃厽鐦ㄩ敍娑欏⒏鐞涒偓閻?buff 鐠愮喕鐭楅敍灞炬拱缁狅紕鎮婇崳銊ょ瑝閸愬秶娲块幒銉ゆ縺鐎圭绱?
        // 鐢?caster source 娓氭盯顥ゅ锕傜摕閵嗗苯鍙嗗锔衡偓宄漞buff 閹凤附鍩呰ぐ鎺戞礈
        target.addEffect(new MobEffectInstance(
                github.com.gengyoubo.sscfa.init.AddonEffects.BAT_POISON.get(), Math.max(20, durationTicks), 0, false, true, true), caster);
        UUID targetUuid = target.getUUID();
        long now = serverWorld.getGameTime();
        long newEnd = now + Math.max(20, durationTicks);
        InfectionData existing = ENTRIES.get(targetUuid);
        if (existing != null) {
            // 閸涙垝鑵戝鍙夊妳閺屾挾娲伴弽?閳?娴犲懎鍩涢弬?endTick閿涙稐绻氶悾娆忓斧 caster
            existing.endTick = newEnd;
            return;
        }
        InfectionData data = new InfectionData(
                caster.getUUID(),
                serverWorld.dimension(),
                newEnd
        );
        ENTRIES.put(targetUuid, data);
    }

    /** 閻劋绨?mixin 閺屻儴顕?*/
    public static boolean isInfected(UUID uuid) {
        return ENTRIES.containsKey(uuid);
    }

    /** 鐎电懓寮搁弬鍦窗閺嶅洦鏌﹂崝鐘充笉閻ゆ顒涚€涙劑鈧倸鎳℃稉顓炲嚒閸︺劍涓嶉悿妞捐厬閻ㄥ嫮娲伴弽鍥ㄦ閻╁瓨甯撮柌宥囩枂閺冨爼鏆遍敍鍫滅箽閻?nextHealTick 閼哄倹濯块敍澶堚偓?*/
    public static void applyFriendHeal(ServerPlayer caster, LivingEntity target, int durationTicks) {
        if (!(target.level() instanceof ServerLevel serverWorld)) return;
        UUID targetUuid = target.getUUID();
        long now = serverWorld.getGameTime();
        long newEnd = now + Math.max(20, durationTicks);
        HealData existing = HEAL_ENTRIES.get(targetUuid);
        if (existing != null) {
            existing.endTick = newEnd;
            return;
        }
        HealData data = new HealData(
                serverWorld.dimension(),
                newEnd,
                now + BAL.i("heal_interval", HEAL_INTERVAL)
        );
        HEAL_ENTRIES.put(targetUuid, data);
    }

    public static int size() {
        return ENTRIES.size();
    }

    private static void onWorldTick(ServerLevel world) {
        if (!HEAL_ENTRIES.isEmpty()) {
            tickHeals(world);
        }
        if (!CLOUDS.isEmpty()) {
            tickClouds(world);
        }
        if (ENTRIES.isEmpty()) return;
        long now = world.getGameTime();
        MinecraftServer server = world.getServer();

        Iterator<Map.Entry<UUID, InfectionData>> it = ENTRIES.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, InfectionData> e = it.next();
            InfectionData data = e.getValue();
            // 娴犲懎顦╅悶鍡曠瑢閺堫兛绗橀悾灞藉爱闁板秶娈戦弶锛勬窗閿涘矁娉曠紒鏉戝閻?world tick 閸氬嫯鍤滅紒瀛樺Б
            if (!data.worldKey.equals(world.dimension())) continue;

            Entity entity = world.getEntity(e.getKey());
            if (!(entity instanceof LivingEntity target) || !target.isAlive()) {
                it.remove();
                continue;
            }
            if (now >= data.endTick) {
                it.remove();
                continue;
            }

            // 1) 閸涖劍婀￠幍锝堫攨閻?BAT_POISON buff 鐠愮喕鐭楅敍鍫ｎ潌 infect/spreadFrom閿涘绱濆銈咁槱娑撳秴鍟€閻╁瓨甯存导銈咁唺

            // 2) 娴肩姵鐓嬮敍鍫滅矌瑜版挻绨弬钘夊閼板懍绮涢崷銊у殠閺冭泛鎯庨悽顭掔礉娓氬じ绨紒褎澹?閸撯晙缍戦弮鍫曟？"鐠囶厺绠熼敍澶涚幢
            // 閼哄倹绁﹂敍姘槨 10t 閹殿偂绔村▎鈽呯礄閸氬本鏋冩禒鑸电槰闂嗗彞绨鍙夋箒 30t 閼哄倹绁﹂懠鍐х伐閿涘绱濇导鐘崇厠瀵ゆ儼绻?閳?.5s 閺冪姵鍔呴惌?
            if (now >= data.nextScanTick) {
                data.nextScanTick = now + BAL.i("spread_scan_interval", SPREAD_SCAN_INTERVAL);
                spreadFrom(server, target, data, now);
            }

            // 3) 缁帒鐡欑悰銊у箛閿涙矮绮禍?1鑴? 鐞氼偅鍔呴弻鎾跺负鐎规儼鍤滃?1/4
            emitInfectionParticles(world, target, now);
        }
    }

    /** 閸?1.5 閺嶇厧鍞寸€电粯澹橀張顏呭妳閺屾挾娲伴弽鍥风礉娴ｈ法鏁ゅ┃鎰煢閸旂姾鈧懐娈戦惂钘夋倳閸楁洝绻樼悰宀冪箖濠娿們鈧?*/
    private static void spreadFrom(MinecraftServer server, LivingEntity host, InfectionData data, long now) {
        ServerPlayer caster = server.getPlayerList().getPlayer(data.casterUuid);
        if (caster == null) return; // 閺傝棄濮為懓鍛竴缁惧灝鍨稉宥勭炊閺屾搫绱欐穱婵堟殌閻滅増婀侀幇鐔哥厠鐠佲剝妞傞敍?

        ServerLevel world = (ServerLevel) host.level();
        double spreadRadius = BAL.d("infect_spread_radius", INFECT_SPREAD_RADIUS);
        AABB box = host.getBoundingBox().inflate(spreadRadius);
        double sqRadius = spreadRadius * spreadRadius;
        List<LivingEntity> candidates = world.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != host && e.isAlive() && host.distanceToSqr(e) <= sqRadius
                        && !ENTRIES.containsKey(e.getUUID()));

        if (candidates.isEmpty()) return;
        int remaining = (int) Math.max(20, data.endTick - now);
        for (LivingEntity candidate : candidates) {
            if (WhitelistUtils.isProtected(caster, candidate)) continue;
            // 閺冨爼妫跨紒褎澹欓懛顏勭秼閸撳秴顔栨稉?
            InfectionData child = new InfectionData(
                    data.casterUuid,
                    world.dimension(),
                    now + remaining
            );
            ENTRIES.put(candidate.getUUID(), child);
            candidate.addEffect(new MobEffectInstance(
                    github.com.gengyoubo.sscfa.init.AddonEffects.BAT_POISON.get(), Math.max(20, remaining), 0, false, true, true), caster);
        }
    }

    /** 閺堝秴濮熺粩顖涚€柅?ParticleS2CPacket 楠炶埖瀵滅憴鍌氱檪閼板懓闊╂禒钘夊瀻閸欐埊绱濈€圭偟骞?閼奉亜绻侀惇瀣毌娑撯偓娴?閵?*/
    private static void emitInfectionParticles(ServerLevel world, LivingEntity target, long now) {
        boolean othersTick = now % PARTICLE_INTERVAL_OTHERS == 0;
        boolean selfTick = now % PARTICLE_INTERVAL_SELF == 0;
        if (!othersTick && !selfTick) return;

        Vec3 pos = target.position();
        // 閸︺劎娲伴弽鍥ф噯閸ユ潙褰傞崙铏硅雹閼瑰弶鍔呴弻鎾剁煈鐎涙劧绱橦APPY_VILLAGER 姒涙顓荤紒鑳閿?
        ClientboundLevelParticlesPacket pkt = new ClientboundLevelParticlesPacket(
                ParticleTypes.HAPPY_VILLAGER,
                false,
                pos.x, pos.y + target.getBbHeight() * 0.6, pos.z,
                0.4f, 0.5f, 0.4f, 0.0f, 2);

        UUID targetUuid = target.getUUID();
        for (ServerPlayer viewer : world.players()) {
            // 鐠烘繄顬囬崜鏃堟珟閿涘牅绗岄崢鐔哄楠炴寧鎸遍崡濠傜窞娑撯偓閼?32 閺嶇》绱?
            if (viewer.distanceToSqr(target) > 32 * 32) continue;
            boolean isSelf = viewer.getUUID().equals(targetUuid);
            if (isSelf ? selfTick : othersTick) {
                viewer.connection.send(pkt);
            }
        }
    }

    /** 閻劋绨?mixin閿涙艾婀弨璇插毊閼板懓顫﹂幇鐔哥厠閺冭泛鍣虹亸鎴濆従娴笺倕顔婇妴?*/
    public static float reduceDamageIfInfected(LivingEntity attacker, float amount) {
        if (attacker == null) return amount;
        if (!ENTRIES.containsKey(attacker.getUUID())) return amount;
        return amount * (1.0f - (float) BAL.d("damage_reduction", DAMAGE_REDUCTION));
    }

    private static final class InfectionData {
        final UUID casterUuid;
        final ResourceKey<Level> worldKey;
        long endTick;
        /** 娑撳顐兼导鐘崇厠閹殿偅寮?tick閿涙矮绱堕弻鎾寸槨 SPREAD_SCAN_INTERVAL 閹殿偂绔村▎鈽呯礄閸樼喐鐦?tick 娑撯偓閹殿偓绱濈紘銈勭秼閹扮喐鐓嬮崷鐑樻珯 N 娴?= 濮?tick N 濞嗏€崇杽娴ｆ挻鐓＄拠顫礆 */
        long nextScanTick;

        InfectionData(UUID casterUuid, ResourceKey<Level> worldKey, long endTick) {
            this.casterUuid = casterUuid;
            this.worldKey = worldKey;
            this.endTick = endTick;
        }
    }

    private static void tickHeals(ServerLevel world) {
        long now = world.getGameTime();
        Iterator<Map.Entry<UUID, HealData>> it = HEAL_ENTRIES.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, HealData> e = it.next();
            HealData data = e.getValue();
            if (!data.worldKey.equals(world.dimension())) continue;
            Entity entity = world.getEntity(e.getKey());
            if (!(entity instanceof LivingEntity target) || !target.isAlive()) {
                it.remove();
                continue;
            }
            if (now >= data.endTick) {
                it.remove();
                continue;
            }
            if (now >= data.nextHealTick) {
                data.nextHealTick = now + BAL.i("heal_interval", HEAL_INTERVAL);
                target.heal((float) BAL.d("tick_heal", TICK_HEAL));
                // 鐟欏棜顫庨崣宥夘洯閿涙艾绺捐箛鍐煈鐎?
                world.sendParticles(ParticleTypes.HEART,
                        target.getX(), target.getY() + target.getBbHeight() * 0.8, target.getZ(),
                        2, 0.3, 0.2, 0.3, 0.0);
            }
        }
    }

    private static final class HealData {
        final ResourceKey<Level> worldKey;
        long endTick;
        long nextHealTick;

        HealData(ResourceKey<Level> worldKey, long endTick, long nextHealTick) {
            this.worldKey = worldKey;
            this.endTick = endTick;
            this.nextHealTick = nextHealTick;
        }
    }

    // ============================ 濠婄偟鏆€濮ｆ帡娴樻禍鎴濈摍缁崵绮?============================
    /** 濮ｆ帡娴樻禍鎴濆磹瀵板嫸绱欓弽纭风礆閿涙氨娲垮?4 閺?閳?閸楀﹤绶?2 閺嶇》绱欐妯款吇閸婄》绱辨潻鎰攽閺冨墎绮?{@link #cloudRadius()} 鐠?balance閿?*/
    public static final double CLOUD_RADIUS = 2.0;
    /** 濮ｆ帡娴樻禍鎴炲閹诲繒鏁撻悧鈺冩畱闂傛挳娈ч敍鍧眎ck閿涘绱板В?1.5s 娑撯偓濞喡ゅ瘱閸ユ潙鍞撮弫鍫熺亯 */
    public static final int CLOUD_SCAN_INTERVAL = 30;
    /** 濮ｆ帡娴樻禍鎴犵煈鐎涙劙妫块梾鏃撶礄tick閿涙稒婀惂鏄忣唶閿涘奔绗夊鈧弨鎾呯礆 */
    public static final int CLOUD_PARTICLE_INTERVAL = 2;
    /** 閸戔偓閸栨牠鈹嶉弫锝嗙槰闂嗗彞绨惃鍕灲鐎规艾宕愬鍕剁礄閺嶇》绱氶敍姘愁潶閸戔偓閸栨牔閲滄担鎾搭劃閼煎啫娲块崘鍛畱娴滄垳绱扮悮顐︹攳閺侊綇绱欐妯款吇閸婄》绱辨潻鎰攽閺冨墎绮?{@link #cloudPurifyReach()} 鐠?balance閿?*/
    public static final double CLOUD_PURIFY_REACH = 8.0;

    /** 濮ｆ帡娴樻禍鎴濆磹瀵板嫸绱檅alance 韫囶偆鍙庣拠璇插絿閿涘奔绶电€涖垹鐡欏纭呮儰閸︽澘缂撴禍鎴犵搼婢舵牠鍎寸拫鍐暏閿涘鈧?*/
    public static double cloudRadius() {
        return BAL.d("cloud_radius", CLOUD_RADIUS);
    }

    /** 閸戔偓閸栨牠鈹嶉弫锝呭灲鐎规艾宕愬鍕剁礄balance 韫囶偆鍙庣拠璇插絿閿涘奔绶甸幃锔句紥閸戔偓閸栨牜鐡戞径鏍劥鐠嬪啰鏁ら敍澶堚偓?*/
    public static double cloudPurifyReach() {
        return BAL.d("cloud_purify_reach", CLOUD_PURIFY_REACH);
    }
    /** 婢с劎璞㈤懝韫厬濮ｆ帞鐭戠€涙劧绱欐稉搴㈠妳閺屾挸顒涚€涙劕鑴婄憴鍡氼潕娑撯偓閼疯揪绱?*/
    private static final DustParticleOptions CLOUD_POISON_DUST = new DustParticleOptions(new Vector3f(0.30f, 0.50f, 0.10f), 1.2f);
    /** 濞叉槒绌惃鍕槰闂嗗彞绨崚妤勩€冮敍鍫熸箛閸旓紕顏弶鍐ㄢ枆閿?*/
    private static final List<CloudData> CLOUDS = new CopyOnWriteArrayList<>();

    /**
     * 閸︺劍瀵氱€规矮缍呯純顔炬晸閹存劒绔撮崶銏＄哺閻ｆ瑦鐦ㄩ梿鍙ョ隘閿涘牊鍔呴弻鎾愁剾鐎涙劕鑴婇拃钘夋勾閺冩儼鐨熼悽顭掔礆閵?
     *
     * @param caster        閺傝姤鏂侀懓鍜冪礄閻劋绨惂钘夋倳閸楁洖鍨界€规矮绗岄幇鐔哥厠瑜版帒鐫橀敍?
     * @param world         閺堝秴濮熺粩顖欑瑯閻?
     * @param center        娴滄垳鑵戣箛鍐跨礄閽€鐣屽仯閿?
     * @param radius        娴滄垵宕愬鍕剁礄閺嶇》绱?
     * @param durationTicks 娴滄垵鐡ㄥú缁樻闂€鍖＄礄tick閿涘绱濋崥灞炬娑旂喐妲告潻娑樺弳閼板懎褰查懢宄扮繁閻ㄥ嫭娓舵径褎鍔呴弻鎾存闂€?
     */
    public static void spawnCloud(ServerPlayer caster, ServerLevel world, Vec3 center, double radius, int durationTicks) {
        long now = world.getGameTime();
        // 濮ｆ帡娴樻稉宥呭讲閸欑姴濮為敍姘冲閽€鐣屽仯闂勫嫯绻庨敍鍫滆⒈娴滄垵宕愬鍕閸滃苯鍞撮敍澶婂嚒閺堝鎮撶紒鏉戝濮ｆ帡娴橀敍灞藉灟娴犲懎鍩涢弬鏉垮従鐎靛灝鎳￠敍灞肩瑝閺傛澘顤?
        for (CloudData c : CLOUDS) {
            if (!c.worldKey.equals(world.dimension())) continue;
            double merge = radius + c.radius;
            if (c.squaredDistanceTo(center.x, center.y, center.z) <= merge * merge) {
                c.endTick = Math.max(c.endTick, now + Math.max(20, durationTicks));
                return;
            }
        }
        CLOUDS.add(new CloudData(
                caster.getUUID(),
                world.dimension(),
                center.x, center.y, center.z,
                radius,
                now + Math.max(20, durationTicks),
                now + BAL.i("cloud_scan_interval", CLOUD_SCAN_INTERVAL)
        ));
    }

    /** 缁夊娅庨惄顔界垼闊偂绗傞惃鍕妳閺屾搫绱欐笟娑欏亰閻忛潧鍣ｉ崠鏍殶閻㈩煉绱氶妴?*/
    public static void cureInfection(UUID uuid) {
        ENTRIES.remove(uuid);
    }

    /** 妞硅鲸鏆庣紒娆忕暰娴ｅ秶鐤?reach 閼煎啫娲块崘鍛偓浣告倱缂佹潙瀹抽惃鍕閺堝鐦ㄩ梿鍙ョ隘閿涘牅绶甸幃锔句紥閸戔偓閸栨牞鐨熼悽顭掔礆閵?*/
    public static void dissipateCloudsNear(ServerLevel world, Vec3 pos, double reach) {
        if (CLOUDS.isEmpty()) return;
        ResourceKey<Level> key = world.dimension();
        double sq = reach * reach;
        CLOUDS.removeIf(c -> c.worldKey.equals(key) && c.squaredDistanceTo(pos.x, pos.y, pos.z) <= sq);
    }

    private static void tickClouds(ServerLevel world) {
        long now = world.getGameTime();
        MinecraftServer server = world.getServer();
        for (CloudData cloud : CLOUDS) {
            if (!cloud.worldKey.equals(world.dimension())) continue;
            if (now >= cloud.endTick) {
                CLOUDS.remove(cloud);
                continue;
            }
            // 1) 缁帒鐡欑悰銊у箛閿涘牊婀囬崝锛勵伂楠炴寧鎸遍敍灞筋樋娴滆桨绔撮懛杈剧礆
            if (now % CLOUD_PARTICLE_INTERVAL == 0) {
                emitCloudParticles(world, cloud);
            }
            // 2) 閸涖劍婀￠幍顐ｅ伎閿涙俺瀵栭崶鏉戝敶濮?1.5s 閻╁瓨甯撮弬钘夊閺佸牊鐏夐敍鍫㈢彲閻偓閹靛秶鏁撻弫鍫礉缁傝绱戦崡鍐蹭粻閿?
            if (now >= cloud.nextScanTick) {
                cloud.nextScanTick = now + BAL.i("cloud_scan_interval", CLOUD_SCAN_INTERVAL);
                ServerPlayer caster = server.getPlayerList().getPlayer(cloud.casterUuid);
                if (caster == null) continue; // 閺傝姤鏂侀懓鍛竴缁惧灝鍨禒鍛殌缁帒鐡欓敍灞肩瑝閻㈢喐鏅ラ敍鍫ユ付閸忓墎娅ч崥宥呭礋閸掋倕鐣鹃敍?
                AABB box = new AABB(
                        cloud.x - cloud.radius, cloud.y - cloud.radius, cloud.z - cloud.radius,
                        cloud.x + cloud.radius, cloud.y + cloud.radius, cloud.z + cloud.radius);
                double sqRadius = cloud.radius * cloud.radius;
                List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, box,
                        e -> e.isAlive() && e != caster
                                && cloud.squaredDistanceTo(e.getX(), e.getY(), e.getZ()) <= sqRadius);
                for (LivingEntity target : targets) {
                    if (WhitelistUtils.isProtected(caster, target)) {
                        // 閸欏鍟楅敍姘礀 1 鐞涒偓 + 閸旂娀鈧?I閿涘牏鐓弮璁圭礉娴犲懓瀵栭崶鏉戝敶缂佸瓨瀵旈敍?
                        target.heal((float) BAL.d("tick_heal", TICK_HEAL));
                        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 0, false, true, true));
                        world.sendParticles(ParticleTypes.HEART,
                                target.getX(), target.getY() + target.getBbHeight() * 0.8, target.getZ(),
                                1, 0.3, 0.2, 0.3, 0.0);
                    } else {
                        // 閺佸奔姹夐敍姘竴 1 鐞涒偓 + 閸戝繘鈧?I閿涘牏鐓弮璁圭礉娴犲懓瀵栭崶鏉戝敶缂佸瓨瀵旈敍澶涚幢鐢附鏌﹂弨鎹愨偓?source 娓氭稑鍙嗗锔藉閹搭亜缍婇崶?
                        target.hurt(target.damageSources().magic(), (float) BAL.d("tick_damage", TICK_DAMAGE));
                        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, true, true), caster);
                    }
                }
            }
        }
    }

    private static void emitCloudParticles(ServerLevel world, CloudData cloud) {
        // 娑撹缍嬪В鎺楁禈閿涙俺鍒涢崷浼存懙瀵偓閻ㄥ嫬鈪风紒鑳娑擃厽鐦ㄧ划鎺戠摍
        world.sendParticles(CLOUD_POISON_DUST,
                cloud.x, cloud.y + 0.5, cloud.z,
                8, cloud.radius * 0.6, 0.45, cloud.radius * 0.6, 0.0);
        // 娴ｅ酣顣舵妯绘殠閻ㄥ嫬顒涚€涙劧绱濇晶鐐插繁"闂?閻ㄥ嫪缍嬬粔顖涘妳
        world.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR,
                cloud.x, cloud.y + 0.3, cloud.z,
                2, cloud.radius * 0.5, 0.3, cloud.radius * 0.5, 0.0);
    }

    private static final class CloudData {
        final UUID casterUuid;
        final ResourceKey<Level> worldKey;
        final double x;
        final double y;
        final double z;
        final double radius;
        long endTick;
        long nextScanTick;

        CloudData(UUID casterUuid, ResourceKey<Level> worldKey, double x, double y, double z,
                  double radius, long endTick, long nextScanTick) {
            this.casterUuid = casterUuid;
            this.worldKey = worldKey;
            this.x = x;
            this.y = y;
            this.z = z;
            this.radius = radius;
            this.endTick = endTick;
            this.nextScanTick = nextScanTick;
        }

        double squaredDistanceTo(double ox, double oy, double oz) {
            double dx = x - ox;
            double dy = y - oy;
            double dz = z - oz;
            return dx * dx + dy * dy + dz * dz;
        }
    }
}
