/*
 * Copyright (c) 2026 MangZai-120
 * This file is part of the "shape shifter curse addon" project.
 * Licensed under the GNU Affero General Public License v3.0 (AGPL-3.0).
 */
package github.com.gengyoubo.sscfa.ability;

import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.PowerUtils;
import net.minecraft.server.level.ServerPlayer;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 鐎靛嫮鏁撻弸婊嗘浆缁夊秴鐡欓柌蹇氬厴闁插繐娲栨径宥囶吀閻炲棗娅掗妴?
 * <p>
 * 閹稿姘﹂幋妯煎Ц閹礁鍠呯€规艾娲栨径宥夋？闂呮棑绱伴張顏冩唉閹存ɑ鐦?5s閿?00t閿涘娲?1 閻愮櫢绱濇禍銈嗗灛濮?8s閿?60t閿涘娲?1 閻愮櫢绱?
 * 濠?10 閻愯妞傛稉宥呭晙閸ョ偛顦查妴鍌欐唉閹存ê鍨界€规矮绗岀粔宥呯摍鐎靛灝鎳℃稉鈧懛杈剧礄{@link ParasiticCombatTracker}閿涘鈧?
 * 閺堝秴濮熺粩顖涙綀婵炰緤绱濋弴澶稿敩閸?JSON action_over_time閿涘牆鍙鹃弮鐘崇《閸掋倕鐣炬禍銈嗗灛閿涘鈧?
 */
public final class ParasiticSeedEnergyRegen {
    // 娴犮儰绗呴崸鍥﹁礋姒涙顓婚崐纭风幢鏉╂劘顢戦弮鏈电矤 balance 韫囶偆鍙庣拠璇插絿閿涘潊bilities.parasitic_seed_system閿?
    /** 閺堫亙姘﹂幋妯烘礀婢跺秹妫块梾鏃撶礄tick閿涘绱?s */
    private static final int INTERVAL_PEACE = 100;
    /** 娴溿倖鍨崶鐐差槻闂傛挳娈ч敍鍧眎ck閿涘绱?s */
    private static final int INTERVAL_COMBAT = 160;
    /** 閼充粙鍣烘稉濠囨 */
    private static final int MAX_ENERGY = 10;

    /** balance 韫囶偆鍙庣拠璇插絿閿涘牆鎻╅悡褎婀崚婵嗩潗閸栨牕娲栭柅鈧妯款吇鐢悂鍣洪敍?*/
    private static final BalanceReader BAL = new BalanceReader("abilities.parasitic_seed_system");

    /** 閻溾晛顔?UUID -> 鐠烘繀绗傚▎鈥虫礀婢跺秴鍑＄槐顖濐吀 tick 閺?*/
    private static final Map<UUID, Integer> ACCUM = new ConcurrentHashMap<>();

    private ParasiticSeedEnergyRegen() {
    }

    public static void init() {
        github.com.gengyoubo.sscfa.util.SscEvents.onServerTick(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                tickPlayer(player);
            }
        });
    }

    private static void tickPlayer(ServerPlayer player) {
        // 娴犲懏鐏夐摝鐘茶埌閹焦瀵旈張澶岊潚鐎涙劙鍣洪懗浠嬪櫤鐠у嫭绨敍娑㈡姜鐠囥儱鑸伴幀浣烘纯閹恒儲绔婚悶鍡欑柈鐠佲€宠嫙鐠哄疇绻冮妴?
        // 濞夈劍鍓伴敍姘箑妞よ崵鏁?hasPowerId閿涘牊妲搁崥锔藉瘮閺?power閿涘绱濇稉宥堝厴閻?hasResource(...,0)閳ユ柡鈧?
        // 閸氬氦鈧懎顕弮?power 閻溾晛顔?= 0>=0 閹帞婀￠敍灞界暓閸楊偄鐣崗銊ャ亼閺佸牞绱濇导姘嚤閼锋潙鍙忛悳鈺侇啀濮?tick 閻у€熺獓
        // 娑撴柨鎳嗛張鐔糕偓褏娅ч崣?PowerHolderComponent.sync 閸忋劑鍣洪崥灞绢劄閸栧懌鈧?
        // 閸忓牏鏁ゆ笟鍨杹閻ㄥ嫬鑸伴幀浣虹矋娴犳儼顕伴敍鍫濆礋濞嗏槄绱氶幐鈩冨竴缂佹繂銇囨径姘殶闂堢偞鐏夐摝鐘靛负鐎硅绱?
        // 閸愬秴浠?O(power閺? 閻?power 濡偓濞村绱遍弸婊嗘浆瑜般垺鈧焦澧犻崣顖濆厴閹稿倽顕?power閿涘奔琚辩仦鍌氬灲鐎规氨绮ㄩ弸婊呯搼娴犳灚鈧?
        if (!github.com.gengyoubo.sscfa.util.FormUtils.isBatParasiticFruit(player)
                || !github.com.gengyoubo.sscfa.resource.ResourceBars.hasPowerId(
                        player, FormIdentifiers.BAT_PARASITIC_FRUIT_SEED_ENERGY)) {
            ACCUM.remove(player.getUUID());
            return;
        }
        int current = PowerUtils.getResourceValue(player, FormIdentifiers.BAT_PARASITIC_FRUIT_SEED_ENERGY);
        if (current >= BAL.i("max_energy", MAX_ENERGY)) {
            ACCUM.put(player.getUUID(), 0);
            return;
        }
        int interval = ParasiticCombatTracker.isInCombat(player)
                ? BAL.i("regen_interval_combat", INTERVAL_COMBAT)
                : BAL.i("regen_interval_peace", INTERVAL_PEACE);
        int acc = ACCUM.getOrDefault(player.getUUID(), 0) + 1;
        if (acc >= interval) {
            PowerUtils.changeResourceValueAndSync(player, FormIdentifiers.BAT_PARASITIC_FRUIT_SEED_ENERGY, 1);
            acc = 0;
        }
        ACCUM.put(player.getUUID(), acc);
    }
}
