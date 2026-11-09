package github.com.gengyoubo.sscfa.ability;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.FormUtils;
import github.com.gengyoubo.sscfa.util.PowerUtils;
import github.com.gengyoubo.sscfa.spell.UniversalFormationManager;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 濠靛倹鍨规导鎺撶▔閺勫浚娲ｉ柟鍨涘亾闁?- 闁哄秴娲╅鍥偐閼哥鍋撴担瑙勭皻闁挎稑鐗婂﹢鍥礉閿涘嫷浼傞柡澶婂暙閳诲鏁嶆径鍫氬亾?
 *
 * 濡増绮忔竟濠勭驳婢跺矂鐛撻柨娑欑摏ELLOW闁?24 闁哄秶銆嬬槐姘跺焼?ORANGE闁挎稑鐗冩晶?4 闁哄秶銆嬬槐姘跺焼?RED闁挎稑鐗嗛崙锟犲础閸モ晠鐛撻柨娑橆槶閳?
 * 閻犵儤绻勯‖鍥矗濡搫顕ч柡?RED闁愁偅澹桬LLOW闁挎稑濂旂徊?expireTick 缂備綀鍕棡濞戞挸绉磋ぐ澶愬Υ?
 *
 * 闁告艾濂旂粩鎾籍閸洘锛熸慨锝呯箣闁叉粍绺介幋鐘辩触闁绘壕鏅涢宥夊矗椤忓洤鍘撮柟闀愮劍濠€?1 濞戞搩浜濋悥锝囨媼鐢喚骞㈠☉鎾筹梗缁斿瓨绋夐鍡樼獥闁哄秴娲ㄥ▓鎴﹀礈椤栨瑧绋婇柣顫妿閻濇盯宕氱紒妯碱伕闂傚嫨鍊戦埀?
 *
 * 濡増绮忔竟濠冾殗濡懓鐦ㄩ柨娑欏哺閳ь剚淇虹换?S2C 闁告艾鏈鐐哄礌?+ my_addon:mancianima_mark_color 闁哄鈧弶顐?+ 濞戞挸顦柌?entity_glow power 閻庡湱鍋熼獮鍥晬?
 * 濞寸姴鎳庨〃鏍倶閸偅鎷卞ù婊冩惈椤撳綊骞嬫搴紓闁告瑯鍨甸～鍡涙晬鐏炶偐鐟濇慨鍏夊墲閻撳宕㈤悢鍝勵暭 GLOWING / scoreboard team闁?
 *
 * 缂佷勘鍨兼竟濠囧冀閸ヮ亶鍞堕梺澶哥閻?15 缂佸甯槐鏉啃掕箛搴ㄥ殝閻炴凹鍋嗙€涒晠鎳濋崣澶屽灱閻犱焦濯界换鍐儍閸曨厽绐楅柡宥呮祫缁?5 缂佸甯掗崬瀛樼▔瀹ュ牆鍘撮悶姘煎亜閹挻绋夐埀顒勬偝閳轰緡鍟€闁告劕绉甸鑲╃棯閵忥絽顥忛柡宥呮穿椤斿洭濡?
 */
public final class MancianimaMarkManager {

	private MancianimaMarkManager() {}

	public enum MarkColor { YELLOW, ORANGE, RED }

	public static final int MARK_DURATION_TICKS = 300;        // 15s
	public static final int RANGE_ORANGE = 24;
	public static final int RANGE_RED_KEEP = 24;
	public static final int RED_RELOCK_COOLDOWN_TICKS = 300;  // 15s 闁告劕鎳嶇粭澶愭嚄閽樺鍤犻柛姘缁旀挳鎯勯鐣屽灱闁告劕绉剁€涒晠寮?
	/** 闂傚啳鍩栭宀勫础閸モ晠鐛撻柛鎰槻瀹撳牓鏁嶅鍓佺煁闁哄秴娲犻崯瀣础閸モ晛顒㈤柕鍡曡兌鐎涒晠寮介崶锝呮櫔鐎殿喗娲滈崹搴ㄦ焾閸婄喎娈鹃悘蹇斿灴濞撳墎鎲版担铏规惣鐎?3s */
	public static final int STAGE_GATE_TICKS = 60;            // 3s

	/** 闂傚啳鍩栭宀勫础閸モ晠鐛撻柛鎰槻瀹撳牓鏁嶉崼锝囩閻炴稑鏈鍌濈疀椤愩倕寮鹃悹鍥嚙瑜板洭鏁嶇仦鑲╄繑 MancianimaPrimary 缂佹稑顦板﹢鍥礉閿涘嫷浼傜紒顐ヮ嚙閸欙繝鎮介…鎺斿耿HUD 閻庡箍鍨洪崺娑氱博椤栨瑧鐭濋悹鍥嚙閻栧爼鏌岃箛銉х */
	public static int stageGateTicks() {
		return BAL.i("stage_gate_ticks", STAGE_GATE_TICKS);
	}

	/** S2C 闁告牕鎷戠槐鎵焊閸℃ɑ鎷遍柛锔藉鐢櫣鈧鍩栫€垫棃寮垫径灞剧暠闁哄秴娲╅鍥触鐏炵虎鍔勯柛鎺撴緲椤撳綊骞嬫搴紓闁挎稑鐬奸弫銈嗙?entity_glow */
	public static final ResourceLocation PACKET_MARK_SYNC = new ResourceLocation("ssc_addon", "mancianima_mark_sync");

	public static final class Mark {
		public final UUID targetUuid;
		public MarkColor color;
		public long expireTick;
		/** 鐟滅増鎸告晶鐘绘⒓閼告鍞界€殿喒鍋撳┑?tick闁挎稒宀哥划宥夊冀?濡絾鐗楅濂稿冀閸ヮ亶鍞?tick闁挎稒绋撶€涒晠寮?闁告娲ㄧ€?tick闁靛棗鍊婚弫銈嗙鎼淬劍鈻夋繛鍫ユ涧閸犲酣宕￠弶鎴濈伈閻庤鍝庨埀?*/
		public long colorSetTick;
		public Mark(UUID t, MarkColor c, long e, long set) {
			targetUuid = t; color = c; expireTick = e; colorSetTick = set;
		}
	}

	/** marker player UUID -> Mark */
	private static final Map<UUID, Mark> MARKS = new ConcurrentHashMap<>();
	/** 婵?(markerUuid + targetUuid) 闁汇劌瀚€涒晠寮介崶顒€娅㈤梺澶哥閸犲酣宕＄€靛摜娉㈤柡?tick */
	private static final Map<String, Long> RED_LOCKOUT = new ConcurrentHashMap<>();
	/** 鐟滅増鎸告晶鐘差潰閿濆懏韬€殿喗娲栭鍗炩枔绾板绐梞arker UUID -> ChannelState */
	public static final Map<UUID, ChannelState> CHANNELING = new ConcurrentHashMap<>();
	/** 鐎垫澘鎳庨幃鎾愁潰閵壯冭礋閻庣懓鐖煎▔锕傚触?*/
	private static final Set<UUID> DIRTY = ConcurrentHashMap.newKeySet();

	/** 濠靛倹鍨规导鎺楁偝閳轰緡鍟€闁哄牃鍋撻柛姘凹缁旀潙鈻庨埄鍐ㄧ仜闁?tick闁挎稑鐗嗚ぐ鍫ュ礄缂佹ê鐏楀☉鎾诡嚙婵晜瀵奸妶鍡樻珪闁?*/
	private static final Map<UUID, Long> LAST_COMBAT = new ConcurrentHashMap<>();
	/** 濞戞挸锕ラ濂稿箮濡炵偓绺洪柛銉у仜椤︽煡鎯?tick */
	private static final Map<UUID, Long> LAST_REGEN = new ConcurrentHashMap<>();
	/** 濞戞挸锕ラ濂告嚇鏉堛劌鐏?mana 闁搞儳鍋涢ˇ鏌ユ儍?tick闁挎稑鐗愮换姗€宕犻弽锕€鈻忓鍕煣缁楁捇鎮介…鎺旂 */
	private static final Map<UUID, Long> LAST_MANA_REGEN = new ConcurrentHashMap<>();
	/** 闂傚牏鍋為崹顒勫棘濡も偓閸ㄧ晫鈧宀稿Σ鍥磹绾绐?s */
	public static final int OUT_OF_COMBAT_TICKS = 100;
	/** 闁硅埖銇炲┑鈧柛銉у仜椤︽煡姊婚幘顔筋吘闁?5s */
	public static final int RESIST_REGEN_INTERVAL_TICKS = 300;
	/** 閺夆晜绋戠€靛弶鎷呴崸妤冩憰闁肩椴搁崹?mana 闁搞儳鍋涢ˇ鏌ユ⒒閹绢喗顓鹃柨?s闁挎稑鐗愰崝姘跺箣濡儤鍊垫慨锝呯箳椤宕?闁绘劗娅㈢槐?*/
	public static final int UPGRADE_FOX_MANA_REGEN_INTERVAL_TICKS = 20;
	/** 閺夆晜绋戠€靛弶鎷呴崸妤冩憰闁肩椴搁崹顒€袙韫囨棏鍋ч柛銉у仜椤︽煡鎯?mana 闂?*/
	public static final double UPGRADE_FOX_MANA_REGEN_AMOUNT = 1.0;
	/** 閺夆晜绋戠€靛弶鎷呴崸妤冩憰 mana 闁搞儳鍋涢ˇ鏌ュ汲閸屾矮绮婚悹浣插墲濡炲倿宕抽妸銊с偒婵?id闁挎稑鐗嗛悥鍫曟煂韫囨搩妲婚柣銏╃厜缁辨繈鏌嗛崹顔煎赋婵?tick 闁绘粍婢樺┃鈧柡瀣閳?Identifier闁?*/
	private static final ResourceLocation UPGRADE_FOX_MANA_REGEN_PAUSE_TIMER =
			new ResourceLocation("my_addon", "form_upgrade_familiar_fox_mana_regen_pause_pause_timer");

	// 闂傚啳鍩栭?5闁挎稒淇虹换宥囨偘鐏炵偓顦ч煫鍥跺亞閸欏海鎷犵拠鎻掔悼闁挎稑娼奲ilities.mancianima_mark闁挎稒绋戦幓鈺呮偂瑜庡﹢顓㈠礆濠靛棭娼楅柛鏍ㄧ墪濞叉牠鏌呴埀顒侇渶濡鍚囬悽顖涙倐閸ｆ椽鏁?
	private static final BalanceReader BAL = new BalanceReader("abilities.mancianima_mark");

	/** 闁哄秴娲╅鍥箣濡粯鐏嶉柛娆愬灩閺佹捇鏁嶉崼婊勭负閻庣懓鐤囩换姗€宕欓崫鍕垫闁诲繒鏁哥敮铏光偓纭呭煐濡炲倻鎷崘顏呮殢闁?*/
	public static void markCombat(UUID playerUuid, long now) {
		if (playerUuid != null) LAST_COMBAT.put(playerUuid, now);
	}

	public static final class ChannelState {
		public final UUID targetUuid;
		public final long endTick;
		/** 1=濞戞挻妲掗々锕傚箮閳ь剟鎳楅悾灞惧焸濞寸鍊栭?2s)闁?=婵炲枴銈庢矗闁瑰灈鍋撻柤宕囨櫕閻忔稓绮旂紒妯荤劦闁哄鍋?1s) */
		public final int type;
        public long castId = -1;
		public ChannelState(UUID target, long endTick, int type) {
			this.targetUuid = target; this.endTick = endTick; this.type = type;
		}
	}

	// ============== 婵炲鍔岄崬?==============
	public static void register() {
		github.com.gengyoubo.sscfa.util.SscEvents.onServerStopping(server -> {
			MARKS.clear(); RED_LOCKOUT.clear(); CHANNELING.clear(); DIRTY.clear();
			LAST_COMBAT.clear(); LAST_REGEN.clear(); LAST_MANA_REGEN.clear();
		});
		github.com.gengyoubo.sscfa.util.SscEvents.onServerTick(MancianimaMarkManager::onTick);
		github.com.gengyoubo.sscfa.event.PlayerSessionCleanup.register((handler, server) -> {
			UUID id = handler.player.getUUID();
			clearMark(server, id);
			CHANNELING.remove(id);
			DIRTY.remove(id);
			LAST_COMBAT.remove(id);
			LAST_REGEN.remove(id);
			LAST_MANA_REGEN.remove(id);
		});
	}

	// ============== Public API ==============

	public static Mark getMark(UUID markerUuid) { return MARKS.get(markerUuid); }

	public static boolean canRedRelock(UUID markerUuid, UUID targetUuid, long now) {
		Long end = RED_LOCKOUT.get(markerUuid + ":" + targetUuid);
		return end == null || now >= end;
	}

	public static String getColorString(UUID markerUuid, UUID targetUuid) {
		Mark m = MARKS.get(markerUuid);
		if (m == null || !m.targetUuid.equals(targetUuid)) return null;
		return colorString(m.color);
	}

	private static String colorString(MarkColor c) {
		return switch (c) { case YELLOW -> "yellow"; case ORANGE -> "orange"; case RED -> "red"; };
	}

	/** 閻犱礁澧介悿鍡涘箣閺嶃劍绂岄柟骞垮灪閻栵絿鎷嬬敮顔剧YELLOW 閻犙冨槻椤劙鏁嶆径鍫氬亾閸屾凹娲ら柡瀣矋濞存盯骞戦～顓犵獥闁稿繐鐗婄粩濠氭偠閸℃洜鐟愬☉鎾亾濞戞搩浜炲ú浼村冀閸モ晜鐣遍柛鎿冨灟缂嶆棃鎮介妸锝傚亾?*/
	public static void setMark(ServerPlayer marker, LivingEntity target, MarkColor color) {
		UUID markerId = marker.getUUID();
		Mark old = MARKS.get(markerId);
		if (old != null && (target == null || !old.targetUuid.equals(target.getUUID()))) {
			clearTargetSideEffects(marker.getServer(), old.targetUuid);
		}
		if (target == null) { MARKS.remove(markerId); DIRTY.add(markerId); return; }
		long now = ((ServerLevel) marker.level()).getGameTime();
		long expire = (old != null && old.targetUuid.equals(target.getUUID())) ? old.expireTick : (now + BAL.i("mark_duration_ticks", MARK_DURATION_TICKS));
		// 闁稿繈鍔嶉弻?YELLOW 闁哄秴娲╅?闁?闂佹彃绉堕悿鍡欐嫚?marker+target 闁汇劌瀚€涒晠寮介崶顒€娅㈤梺?CD闁挎稑鐗嗛崢鎴犳媼閸濄儳褰岄柛鎺曨嚙瀹曞瞼鐥～顔剧
		boolean isFreshYellow = color == MarkColor.YELLOW
				&& (old == null || !old.targetUuid.equals(target.getUUID()));
		if (isFreshYellow) {
			RED_LOCKOUT.remove(markerId + ":" + target.getUUID());
		}
		// 闁稿繈鍔嶉弻濠囧冀閸ヮ亶鍞堕柨娑樼墔缁楀宕ュ畝鈧ú浼村冀閸ラ绀嗛柍?colorSetTick 闁?now闁挎稒绋戦幃鎾绘儎椤旂晫鍨肩€点倕澧庨悽?闁?濞ｅ洦绻勯弳鈧柡鍐勫懏鐣?colorSetTick
		long setTick = (old != null && old.targetUuid.equals(target.getUUID())) ? old.colorSetTick : now;
		MARKS.put(markerId, new Mark(target.getUUID(), color, expire, setTick));
		DIRTY.add(markerId);
	}

	/** 闁告娲ㄦ鍥礆閹殿喖顒㈤柤纭咁嚋缁辨瑨绠涢崨娣偓蹇氥亹閹惧啿顤呴柡?ORANGE闁靛棔娴囩换鍐╊渶閸曨剛鍨?3s 闂傚啳鍩栭宀勫礃瀹勬澘绁卞☉鎾存⒐濠€顓㈠捶閵娾晜鏁氶悗瑙勭濠€锟犲礃閸滃啰绀嗛柕?*/
	public static boolean upgradeToRed(ServerPlayer marker, LivingEntity target) {
		Mark m = MARKS.get(marker.getUUID());
		if (m == null || !m.targetUuid.equals(target.getUUID())) return false;
		if (m.color != MarkColor.ORANGE) return false;
		long now = ((ServerLevel) marker.level()).getGameTime();
		if (now - m.colorSetTick < BAL.i("stage_gate_ticks", STAGE_GATE_TICKS)) return false;
		if (!canRedRelock(marker.getUUID(), target.getUUID(), now)) return false;
		m.color = MarkColor.RED;
		m.expireTick = now + BAL.i("mark_duration_ticks", MARK_DURATION_TICKS);
		m.colorSetTick = now; // 闂佹彃绉堕悿鍡涙⒓閼告鍞介悹浣插墲濡炲倿鏁嶅畝鈧€涒晠寮介崶锝呮櫔鐎殿喗娲滈崹搴ㄦ閳ь剟宕樺鍥╂惣 3s
		RED_LOCKOUT.put(marker.getUUID() + ":" + target.getUUID(), now + BAL.i("red_relock_cooldown_ticks", RED_RELOCK_COOLDOWN_TICKS));
		DIRTY.add(marker.getUUID());
		return true;
	}

	public static void clearMark(MinecraftServer server, UUID markerUuid) {
		Mark m = MARKS.remove(markerUuid);
		if (m != null) clearTargetSideEffects(server, m.targetUuid);
		DIRTY.add(markerUuid);
	}

	public static boolean isRedMarkedBy(UUID markerUuid, UUID targetUuid) {
		Mark m = MARKS.get(markerUuid);
		return m != null && m.color == MarkColor.RED && m.targetUuid.equals(targetUuid);
	}

	public static UUID getMarkerOf(UUID targetUuid) {
		for (Map.Entry<UUID, Mark> e : MARKS.entrySet()) {
			if (e.getValue().targetUuid.equals(targetUuid)) return e.getKey();
		}
		return null;
	}

	// ============== Tick ==============
	private static void onTick(MinecraftServer server) {
		long now = server.overworld().getGameTime();
		RED_LOCKOUT.entrySet().removeIf(e -> e.getValue() <= now);

		Iterator<Map.Entry<UUID, Mark>> it = MARKS.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, Mark> e = it.next();
			UUID markerId = e.getKey();
			Mark m = e.getValue();
			ServerPlayer marker = server.getPlayerList().getPlayer(markerId);
			if (marker == null) { removeAndCleanup(server, it, m, markerId); continue; }
			net.onixary.shapeShifterCurseForge.form.FormDefinition form = FormUtils.getCurrentForm(marker);
			if (form == null || !FormIdentifiers.FAMILIAR_FOX_MANCIANIMA.equals(form.id())) {
				removeAndCleanup(server, it, m, markerId); continue;
			}
			if (now >= m.expireTick) { removeAndCleanup(server, it, m, markerId); continue; }
			Entity tgt = findEntity(server, m.targetUuid);
			if (!(tgt instanceof LivingEntity living) || !living.isAlive()) { removeAndCleanup(server, it, m, markerId); continue; }

			double dist = marker.distanceTo(living);
			MarkColor newColor = m.color;
			int rangeOrange = BAL.i("range_orange", RANGE_ORANGE);
			if (m.color == MarkColor.YELLOW) {
				if (dist <= rangeOrange) newColor = MarkColor.ORANGE;
			} else if (m.color == MarkColor.ORANGE) {
				if (dist > rangeOrange) newColor = MarkColor.YELLOW;
			} else { // RED
				if (dist > BAL.i("range_red_keep", RANGE_RED_KEEP)) newColor = MarkColor.YELLOW;
			}
			if (newColor != m.color) {
				m.color = newColor;
				DIRTY.add(markerId);
				if (newColor == MarkColor.YELLOW && living instanceof ServerPlayer sp) {
					playSoundToPlayer(sp, SoundEvents.PLAYER_LEVELUP, 1.0f, 1.0f);
				}
			}
			// 闁归晲鑳堕悽缁樻綇閸涱厼袠闁轰礁鐗婇悘澶愭晬濮橆偆鐭?SLOWNESS闁挎稑鐗撻·渚€鎳濋弻銉у蒋濞存粠鍠氶弫?entity_glow 闁圭粯鍔掔欢鐢告晬婢舵稓骞㈤悽顖ら檮閻栵絿鎷嬮幏灞稿亾?source 濞撴碍绋戦崣鍡楊潖閿旇棄顏￠柟鎼簻缂嶅﹪宕?
			if (now % 20 == 0) {
				github.com.gengyoubo.sscfa.spell.DomainManager.runAttachedEffect(marker, living,
						() -> living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 25, 0, false, false, false), marker));
			}
		}

		// 鐎殿喗娲栭閬嶅箑?tick
		Iterator<Map.Entry<UUID, ChannelState>> cit = CHANNELING.entrySet().iterator();
		while (cit.hasNext()) {
			Map.Entry<UUID, ChannelState> e = cit.next();
			UUID markerId = e.getKey();
			ChannelState cs = e.getValue();
			ServerPlayer marker = server.getPlayerList().getPlayer(markerId);
			if (marker == null) { cit.remove(); continue; }
			Mark m = MARKS.get(markerId);
			boolean valid = m != null && m.color == MarkColor.RED && m.targetUuid.equals(cs.targetUuid);
			Entity tgt = findEntity(server, cs.targetUuid);
			boolean targetAlive = tgt instanceof LivingEntity le && le.isAlive();
			if (!valid || !targetAlive || github.com.gengyoubo.sscfa.spell.DomainManager.blocksTargeting(marker, tgt)) {
				cit.remove();
				marker.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.ssc_addon.mancianima.channel_fail"), true);
				// 鐎殿喗娲栭杈ㄧ▔椤撶喐鐒?= 闁艰鲸鏌ㄦ慨鈺傚緞鏉堫偉袝闁挎稒纰嶉濂稿箮閳ь剟鎳楅挊澶岀┛閻庣數銆嬬槐妾泍pe=2闁挎稑顦崇换姗€宕?3.5s 濠㈡儼绮剧憴?CD闁挎稒绋愮€靛矂骞庨埀顒勬嚄閽樺绌块悗鐢点€嬬槐妾泍pe=1闁挎稑顦卞ǎ顕€骞愭担绋挎枾閻炴稑濂旂拹鐔哥▔瀹ュ牜鍟?CD
				if (cs.type == 2) {
					github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.failed(marker, MancianimaTeleport.SKILL_ID, cs.castId);
				}
				continue;
			}
			marker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5, 3, false, false, false));
			// 闁藉啫瀚慨蹇涘嫉閻斿吋锛熺紒妤婂厸缁椾焦绂嶉搹鍓愮偤鎮х憴鍕珡闁挎稒顒rker 闁告稏鍔屽ú?+ target 闁告稏鍔屽ú?+ 濞戞搩鍙冨Λ璺ㄦ崉椤栨氨绐?
			ServerLevel sw = (ServerLevel) marker.level();
			LivingEntity le = (LivingEntity) tgt;
			double mx = marker.getX(), my = marker.getY() + marker.getEyeHeight() * 0.6, mz = marker.getZ();
			double tx = le.getX(), ty = le.getY() + le.getBbHeight() * 0.5, tz = le.getZ();
			// marker 闁告稏鍔屽ú鍧楀籍鐎ｎ厽绁紒顔藉笒閻℃瑩鎮抽銈囩缂侀硸鍋夋竟濠冾洨閸涱厼顫旈柨?
			for (int i = 0; i < 4; i++) {
				double angle = (now * 0.3 + i * Math.PI / 2.0);
				double rx = mx + Math.cos(angle) * 0.8;
				double rz = mz + Math.sin(angle) * 0.8;
				sw.sendParticles(net.minecraft.core.particles.ParticleTypes.WITCH, rx, my, rz, 1, 0, 0, 0, 0);
			}
			// target 闁告稏鍔屽ú鍧楀籍鐎ｎ厽绁紒顔藉笒閻℃瑩鎮抽銈囩缂佷勘鍨兼竟濠囧础闁秵鐝﹂柨?
			for (int i = 0; i < 4; i++) {
				double angle = (-now * 0.3 + i * Math.PI / 2.0);
				double rx = tx + Math.cos(angle) * 1.0;
				double rz = tz + Math.sin(angle) * 1.0;
				sw.sendParticles(net.minecraft.core.particles.ParticleTypes.SCULK_SOUL, rx, ty, rz, 1, 0, 0, 0, 0);
			}
			// marker 闁?target 閻犱警鍨扮欢鐐寸▔婵犲啰妲?4 tick 闁圭偓甯婄粩瀛橈紣濡ゅ啩绱ュ鍕€绘导鈧柛?
			if (now % 4 == 0) {
				int steps = 6;
				for (int s = 1; s < steps; s++) {
					double t = s / (double) steps;
					double px = mx + (tx - mx) * t;
					double py = my + (ty - my) * t;
					double pz = mz + (tz - mz) * t;
					sw.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME, px, py, pz, 1, 0.05, 0.05, 0.05, 0.0);
				}
			}
			if (now >= cs.endTick) {
				cit.remove();
				if (cs.type == 1) {
					MancianimaPrimary.executeChannelComplete(marker, (LivingEntity) tgt);
				} else if (cs.type == 2) {
					MancianimaTeleport.executeRedMarkChannelComplete(marker, (LivingEntity) tgt, cs.castId);
				}
			}
		}

		// 闁告艾鏈鐐烘嚇韫囨梹娈堕柟?
		if (!DIRTY.isEmpty()) {
			Set<UUID> snapshot = new HashSet<>(DIRTY);
			DIRTY.clear();
			for (UUID id : snapshot) {
				ServerPlayer p = server.getPlayerList().getPlayer(id);
				if (p != null) sendSyncPacket(p);
			}
		}

		// 濠靛倹鍨规导鎺楀箮濡炵偓绺洪柛銉у仜椤?/ 閺夆晜绋戠€靛弶鎷呴崸妤冩憰闁肩椴搁崹?mana 闁搞儳鍋涢ˇ鏌ユ晬濮橆偉鈷堢憸鑸灪閳ь兛妞掔花浼村棘閵夘垳绀夐柛姘墕閼荤喐绋夋潪鎵伇婵炲棌鈧啿寮块柣婧炬櫅椤斿秹鏌嗗鍛潑闁挎稑鐗婇惁?tick 婵絽绻掔敮铏光偓纭呮硾閻垱绋夐埀顒€鈻?getCurrentForm闁?
		for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
			net.onixary.shapeShifterCurseForge.form.FormDefinition form = FormUtils.getCurrentForm(sp);
			if (form == null) continue;
			net.minecraft.resources.ResourceLocation formId = form.id();
			if (FormIdentifiers.FAMILIAR_FOX_MANCIANIMA.equals(formId)) {
				// 闁硅埖銇炲┑鈧柛銉у仜椤︽煡鏁?5s 闁告劕鎳忓﹢顓㈠矗濡も偓閸╁本绂掔拋宕囩Э濞寸鍊曢濠囧触鎼搭垳绀夋慨?15s 闁?1 闁硅埖銇炲┑鈧柨娑樼墔鐎靛矂宕濋妸锔芥毎闁告垹绮弲顐ｇ鏉炴壆鐟濋柟鍨尰閺屽洨鎷嬮埄鍐╊槯闁?
				UUID id = sp.getUUID();
				long lastCombat = LAST_COMBAT.getOrDefault(id, 0L);
				long resistRegenInterval = BAL.i("resist_regen_interval_ticks", RESIST_REGEN_INTERVAL_TICKS);
				if (now - lastCombat < resistRegenInterval) continue;
				long lastRegen = LAST_REGEN.getOrDefault(id, 0L);
				if (now - lastRegen < resistRegenInterval) continue;
				int cur = PowerUtils.getResourceValue(sp, FormIdentifiers.MANCIANIMA_RESISTANCE);
				int max = PowerUtils.getResourceMax(sp, FormIdentifiers.MANCIANIMA_RESISTANCE);
				if (max <= 0) max = 2;
				if (cur >= max) { LAST_REGEN.put(id, now); continue; }
				PowerUtils.changeResourceValueAndSync(sp, FormIdentifiers.MANCIANIMA_RESISTANCE, 1);
				LAST_REGEN.put(id, now);
			} else if (FormIdentifiers.UPGRADE_FAMILIAR_FOX.equals(formId)) {
				// 閺夆晜绋戠€靛弶鎷呴崸妤冩憰闁肩椴搁崹?mana 闁搞儳鍋涢ˇ鏌ユ晬濮樺啿濮庨柟?5s 闁告艾瀛╅惁?1s 闁?1 闁?mana闁挎稑鐗撳〒璺侯啅閼肩紟鎺楁煥?mana_system 闁煎搫鍊婚崑锝夋晬?
				// 闁哄啫鐖煎Λ鍧楁⒒閵娿儱顤呯紓鍐惧櫙缁变即鎳樻潏銊ョ仜缂佸娅曢埀顑挎缁楀懘宕楅崼锝庢蕉 20t 闁哄啫鐖煎Λ鍧楁⒒閵婏箑顏″☉鎾愁儜缁辨繈宕楀鍕笡闁靛棗鐭佺换姗€宕犻弽顐ょ煁濞寸姵鍎奸?+
				// isUnlocked + 閻犙冨缁噣骞嶉锝呬紟闁靛棗绋勭槐杈ㄧ▔閵堝澹曢梻鍌樺妼濞煎孩绋夐搹鐟板毐閻犲洩顕цぐ鍥晬瀹€鍐閹兼潙绻嬬粭澶愬绩閻熸澘缍侀悶娑樺鐠愮喖鏁嶉崼婵愬殸闁绘挆鍐炬闁诲繘娼ч崹搴ㄥ绩椤栫偑鈧孩鎯旇箛銉х闁?
				UUID id = sp.getUUID();
				long lastCombat = LAST_COMBAT.getOrDefault(id, 0L);
				if (now - lastCombat < BAL.i("out_of_combat_ticks", OUT_OF_COMBAT_TICKS)) continue;
				long lastRegen = LAST_MANA_REGEN.getOrDefault(id, 0L);
				if (now - lastRegen < BAL.i("upgrade_fox_mana_regen_interval_ticks", UPGRADE_FOX_MANA_REGEN_INTERVAL_TICKS)) continue;
				if (UniversalFormationManager.isCharging(sp)) continue;
				// 濞寸姴鎳庡﹢顏勵啅閼肩紟鎺楁煥?mana_system 闁煎搫鍊婚崑锝夊籍閸撲焦鏅搁柡浣哥墳缁辨獡ana 闁哄鍓濆Ο澶岀矆濞差亝锛岄柟璨夊倻顏遍柤鐤彧缁?
				if (!github.com.gengyoubo.sscfa.evolution.RegEvolutionComponent.EVOLUTION
						.get(sp).isUnlocked(github.com.gengyoubo.sscfa.evolution.FamiliarFoxTree.NODE_MANA)) continue;
				// 婵炴垵鐗愰埀?mana 闁?5s 闁告劕鎳忓▓蹇涘磻濠婂棗娈伴柛鏂诲妼濞叉牗寰勫蹇曠regen_pause_timer 閻犙冨缁?> 0 閻炴稏鍔庨妵姘跺捶閵婏附鐣柛瀣矌閻涖儵宕ｉ敐蹇曠
				// 闁诡儸鍡楀幋闁挎稒鑹鹃悥鍫曟煂韫囨搩妲婚柣銏╃厜缁辨瑩宕㈤悢鍝ユЖ tick 婵絽绻楃换姗€宕犻弽锕€鈻忓鍕⒒楠炲洭宕?new Identifier闁挎稑鏈悗顖炴焻閻樺弶鍎撴慨婵撶到閸垶寮介敓鐘靛矗闁?
				int pauseTimer = PowerUtils.getResourceValue(sp, UPGRADE_FOX_MANA_REGEN_PAUSE_TIMER);
				if (pauseTimer > 0) continue;
				double curMana = github.com.gengyoubo.sscfa.compat.ManaUtils.getPlayerMana(sp);
				double maxMana = github.com.gengyoubo.sscfa.compat.ManaUtils.getPlayerMaxMana(sp);
				if (maxMana <= 0 || curMana >= maxMana) { LAST_MANA_REGEN.put(id, now); continue; }
				// 闁诲繋绲婚～瀣嚍閸屾粌浠柨娑欘劎ana 闁煎浜滄慨鈺呭炊閻愬樊妲婚梺顐ゅ枔瀹?+10%
				double regenAmount = BAL.d("upgrade_fox_mana_regen_amount", UPGRADE_FOX_MANA_REGEN_AMOUNT);
				github.com.gengyoubo.sscfa.evolution.EvolutionComponent svComp =
							github.com.gengyoubo.sscfa.evolution.RegEvolutionComponent.EVOLUTION.get(sp);
				if (svComp.isUnlocked(github.com.gengyoubo.sscfa.evolution.FamiliarFoxTree.NODE_SPIRIT_VISION)) {
					regenAmount *= 1.1;
				}
				// 闁诲浚鍋嗛獮鍡涙嚍閸屾粌浠柨娑欘劎ana 闁煎浜滄慨鈺呭炊閻愬樊妲婚梺顐ゅ枔瀹?+20%
				if (svComp.isUnlocked(github.com.gengyoubo.sscfa.evolution.FamiliarFoxTree.NODE_FIRE_RING)) {
					regenAmount *= 1.2;
				}
				github.com.gengyoubo.sscfa.compat.ManaUtils.gainPlayerMana(sp, regenAmount);
				LAST_MANA_REGEN.put(id, now);
			}
		}
	}

	private static void removeAndCleanup(MinecraftServer server, Iterator<Map.Entry<UUID, Mark>> it, Mark m, UUID markerId) {
		it.remove();
		clearTargetSideEffects(server, m.targetUuid);
		DIRTY.add(markerId);
	}

	private static void clearTargetSideEffects(MinecraftServer server, UUID targetUuid) {
		if (server == null) return;
		Entity ent = findEntity(server, targetUuid);
		if (ent instanceof LivingEntity le) {
			le.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
		}
	}

	// ============== Helpers ==============
	private static Entity findEntity(MinecraftServer server, UUID uuid) {
		for (ServerLevel w : server.getAllLevels()) {
			Entity e = w.getEntity(uuid);
			if (e != null) return e;
		}
		return null;
	}

	private static void sendSyncPacket(ServerPlayer player) {
		Mark m = MARKS.get(player.getUUID());
		FriendlyByteBuf buf = github.com.gengyoubo.sscfa.network.AddonNetwork.buffer();
		if (m == null) {
			buf.writeInt(0);
		} else {
			buf.writeInt(1);
			buf.writeUUID(m.targetUuid);
			buf.writeUtf(colorString(m.color));
		}
		buf.writeInt(m == null ? 0 : (int) Math.max(0,
				BAL.i("stage_gate_ticks", STAGE_GATE_TICKS) - (player.level().getGameTime() - m.colorSetTick)));
		try {
			github.com.gengyoubo.sscfa.network.AddonNetwork.sendToPlayer(player, PACKET_MARK_SYNC, buf);
		} catch (Exception ignored) {}
	}

	/** 闂佹彃绉风换?闁瑰箍鍨诲ǎ顔芥償閿曗偓閹顕ｉ崫鍕厬闁硅泛锕ょ紞瀣礈?mark 闁绘鍩栭埀顑跨閹挸顫㈤妷褏鑸堕悹鍥ュ劤鐢櫣鈧顔愮槐婵囩┍濠靛﹦妲堥悗骞垮灪閸╂稓绮?HUD/闁哄秴娲╅鍥ь潰閿濆洠鈧?*/
	public static void resyncToPlayer(ServerPlayer player) {
		sendSyncPacket(player);
	}

	public static void playSoundToPlayer(ServerPlayer player, SoundEvent sound, float volume, float pitch) {
		Holder<SoundEvent> entry = BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound);
		player.connection.send(new ClientboundSoundPacket(
				entry, SoundSource.PLAYERS,
				player.getX(), player.getY(), player.getZ(),
				volume, pitch, player.getRandom().nextLong()));
	}

	public static void broadcastSoundAtEntity(ServerLevel world, Entity entity, SoundEvent sound, float volume, float pitch) {
		world.playSound(null, entity.getX(), entity.getY(), entity.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
	}
}
