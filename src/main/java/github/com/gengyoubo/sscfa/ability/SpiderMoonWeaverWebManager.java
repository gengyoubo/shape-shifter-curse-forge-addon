package github.com.gengyoubo.sscfa.ability;

import github.com.gengyoubo.sscfa.entity.ShellProjectile;

import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.compat.SpiderMoonWeaverPerks;
import github.com.gengyoubo.sscfa.init.AddonBlocks;
import github.com.gengyoubo.sscfa.state.RegSpiderMoonWeaverStateComponent;
import github.com.gengyoubo.sscfa.state.SpiderMoonWeaverStateComponent;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.FormUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 闁哄牆鐗忕划鎰版憘濞戞牑鍋撳畝鈧划鎰磾閹寸偞閽╅柕鍡楃С鐎靛矂骞庨埀顒勬嚄?- 闁哄牆绉存慨鐔虹博椤栨粌笑闁诡兛鐒﹀┃鈧柕?
 *
 * <p>闁告娲戠€靛矂骞庨埀顒勬嚄娴犲鏆涢柨娑樻緝p_primary闁挎稑顧€缁辨繄鈧箍鍨洪崺娑氱博椤栨繄鐝舵繛灞惧椤ュ懎霉鐎ｎ亜绲洪柛鏍ф嚇閳瑰秹宕濋…鎺旂獥
 * <ul>
 *   <li><b>婵炲吋绮忛、鎴﹀矗鐏炶棄姣婂☉鎾愁煼閺?/b> 闁?{@link #toggleMode} 闁告帒娲﹀畷?闁圭⒈鍙€閻?/ 闁衡偓鐠囨彃姣?婵☆垪鈧磭纭€闁挎稑鐗嗛褰掑箣妞嬪寒浼傞柟闈涱儏婵炲秹宕氶妶鍛毎闁挎稑鐭侀幖顐﹀礉濞戞鍘紒鍌欑劍椤掓盯宕氶崶銊ュ簥闁挎稑顦埀?/li>
 *   <li><b>闁圭顦紞鍥ㄧ▔婵犳碍鏆涢柦鍐ㄥ婵?闁?闁哄鍎崇槐?/b> 闁?{@link #start} / {@link #release} 闁告瑦鍨垫慨鈺勩亹閹惧啿顤呮俊顖椻偓宕囩闁?/li>
 *   <ul>
 *     <li>闁圭⒈鍙€閻儳螣閳ュ磭纭€闁挎稒鐭粭澶婎焽濠婂棭鏀?闁?闁告瑦鍨甸惃鐘绘⒔閸曨偆娼?{@link BridgeWebBullet} 闁炬梹绋愮粭锝咁嚕閻у摜绀勯柛娑欏灊閼垫垿寮悷鐗堝仴鐎点倝缂氬ú鎾寸▔濠靛浂娼介柨娑樻湰椤亪宕稿Δ鈧ぐ鏌ュ即婢剁鏁╅柛鎴濈箻閳ь剛鍠撶紞澶愭嚉濠婃劗骞?
 *         闁告稒鍨濋懙鎴犫偓鍦仒缂嶅寮悾宀€鐐婄紓浣规穿缁辨岸鏁?
 *         婵炲吋绮忛、?闁?闁煎瓨鐭粭鍛寸嵁閹惰姤鎳?{@link AddonWebBridgeAction} 闁炬梹绋愮粭锝咁浖閵夘垳绀勯柛姘湰閻楅亶宕ｉ娑欑濞寸媴绲跨紞澶愭嚉濠婃劗绀嗛柕?/li>
 *     <li>闁衡偓鐠囨彃姣婃俊顖椻偓宕囩 闁?闁?{@link WebMembraneBullet}闁挎稑鑻幊鈩冪▔椤撶偞鍊甸柟绋款槹閵嗗倿宕烽妸銉ョ９鐎?3/5/6 闁哄秶鍘ч崬瀵告嫻閹绢喗妗ㄩ梺鎯ф惈閸ｆ椽鏌呴悢缁樼＝缂傚啯鍨埀?/li>
 *   </ul>
 * </ul>
 * 闁藉啫瀚慨蹇涘礆?3 婵℃缍囩槐?20t? 闁?tier1闁靛棔璁叉晶?0t 闁?tier2闁靛棔璁叉晶?0t 闁?tier3闁挎稑鐗婂?3 缂佸甯掗惃婵囥亜鐠佸湱绀嗛柨娑欑缁旂喖鎷戦崟顐㈩潝閺夊牏顢婇埀?mana闁?
 * mana 濞戞挸绉烽崘濠氭嚊椤忓嫬袟闂佹彃锕ラ弬渚€濡?
 *
 * <p>婵☆垪鈧磭纭€閻庢稒眉缁剟鎮鍌滃綄 CCA 缂備礁瀚▎?{@link SpiderMoonWeaverStateComponent}闁?=闁圭⒈鍙€閻?/ 1=闁衡偓鐠囨彃姣婇柨娑樺缁楀骞?origin闁靛棔娴囧▔鏇熷濮樺磭妯?閻犳亽鍔岄懜浼村箑?婵繆顔婃鎼佹煂瀹ュ洦鏅稿ǎ鍥ㄧ箘閺嗏偓闁挎稑顧€缁遍亶鎷戦崟顐㈩潝婵℃绲肩紞鍛偓娑櫭肩花?
 * 闁哄牜鍓涚悮顐﹀嫉瀹ュ懎顫ょ紒?map闁挎稑鐗忛悘娑㈠箑娓氬﹦绀嗛柕鍡曟櫙D 闁?power form_spider_moon_weaver_web 闁?JSON 閻庢稒顨嗛宀勬煀瀹ュ洨鏋傞柕?
 */
public final class SpiderMoonWeaverWebManager {

	private static final int MODE_BRIDGE = SpiderMoonWeaverStateComponent.MODE_BRIDGE;
	private static final int MODE_ATTACK = SpiderMoonWeaverStateComponent.MODE_ATTACK;

	// 濞寸姰鍎扮粭鍛啅閼碱剚顏㈤悹浣规緲閻栧爼鏌岃箛鎾寸秵濞戞挻妞界划顖滄媼閵堝應鍋撶涵椋庡耿閺夆晜鍔橀、鎴﹀籍閺堢數鐭?balance 闊浂鍋嗛崣搴ｆ嫚鐠囨彃绲块柨娑樻綂bilities.moon_weaver_web闁?
	private static final int MAX_TICKS = 60;        // 婵犲ň鍓濋妴鍌炴嫅閸曨偄顫?3 缂?
	private static final int TIER1_TICKS = 20;      // 闁? 缂佸甯楁慨?tier1
	private static final int TIER2_TICKS = 40;      // 闁? 缂佸甯熺换?tier2
	// 闂佹彃锕ラ弬渚€宕?CD 闁?power JSON 闂佹澘绉堕悿鍡涙晬閸綆娼岀紒顐ょ帛閺佺偤鏌屾繝蹇曠闁挎稑鑻敮?CD_TICKS_PER_TIER 閻㈩垱鎮傞崳鍝勵啅閹绘帒鐏?
	private static final double START_MANA = 6.0;   // 閻犙囨敱婢ф粓妫侀埀?6 mana闁挎稑鐗婇柈銊╂偨閵娿儱鏂ч柣妤€鐗愬﹢妤呮憘濞戝磭绀?
	private static final double MANA_PER_TICK = 0.25;
	private static final double PERK_CHARGE_TIME_MULTIPLIER = 0.7;

	// 闂傚啳鍩栭?5闁挎稒纰嶅﹢鍥礉閿涘嫷浼傞柡澶婂暙閳诲绠涢銈呭季閻犲洩顕цぐ鍥晬閸繃褰ラ柣鎾楀嫭寮撻柛鎺撶箓椤劙宕犻弽褎绀€闂侇偀鍋撳娑欘焾椤撹崵鏁幖浣告闁?
	private static final BalanceReader BAL = new BalanceReader("abilities.moon_weaver_web");
	/** 缂備胶鍠嶇粩鎾礃瀹勬澘绁遍柡鍫濈Т婵喖鎯冮崟顓€鏃傗偓瑙勭婵⊙囨嚄?ID闁挎稑鐗忕紞澶岀磼濠婂啫鐦堕悷娆欑畱瑜板倿宕圭€ｅ墎绀嗛柕?*/
	private static final String SKILL_ID = "my_addon:form_spider_moon_weaver_web";

	/** HUD 闂傚倶鍔嶉～鐘诲触鐏炲墽鐖遍柨娑欐皑缁劗绱旈幋锝嗗闯闁归潧顑嗙涵鍫曞礉濞戝磭绀刡alance 闁告瑯鍨甸惃鐔兼晬濮圭悢D 閻忕偞娲滈妵姘舵偨椤帞绀嗛柕?*/
	public static double startManaForHud() { return BAL.d("start_mana", START_MANA); }

	/** UUID -> {鐎规瓕灏幖顐﹀礉?tick 闁轰胶娈柕鍡楀€瑰﹢鍥礉閿涘嫷浼傞柡澶婂暙閳诲鏁嶇仦绛嬫▼濞存粏妗ㄧ粩鎾嚊濞ｎ兘鍋?*/
	private static final Map<UUID, ChargeState> CHARGING = new ConcurrentHashMap<>();
	/** 妤犵偞濞婇幗鐢稿箹椤撶啎澶愭嫅閸曨偄顫斿☉鎿冨幘濞堟垿鎮抽埡渚囧晙闁挎稑鐗嗗濠氬礄婵犳碍姣愰柟?/ 婵炲吋绮忛、鎴︽⒐閹稿骸鐦婚悷娆欑畱瑜板倿鏁嶆径娑氱release 闁哄啯鍎奸摂瀣嚇濮橆偆鐟撴鐐村▕閹界敻鎳撳畝鍕闁炬梹绋愮粭锝咁嚕楠炲簱鍋?*/
	private static final class ChargeState {
		int ticks;
		double paidMana;
		final long castId;
		final int cooldownTicks;
		final int mode, tier1Ticks, tier2Ticks, maxTicks;
		final double tier1Mana, tier2Mana, totalMana;

		ChargeState(ServerPlayer player) {
			castId = github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.currentCastId(player, SKILL_ID);
			cooldownTicks = github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.cooldown(player, SKILL_ID);
			mode = getMode(player);
			int originalMax = BAL.i("max_ticks", MAX_TICKS);
			int originalTier1 = Math.min(originalMax, BAL.i("tier1_ticks", TIER1_TICKS));
			int originalTier2 = Math.max(originalTier1, Math.min(originalMax, BAL.i("tier2_ticks", TIER2_TICKS)));
			double scale = SpiderMoonWeaverPerks.acceleratesCharge(player, mode)
					? BAL.d("perk_charge_time_multiplier", PERK_CHARGE_TIME_MULTIPLIER) : 1.0;
			tier1Ticks = Math.max(1, (int) Math.ceil(originalTier1 * scale));
			tier2Ticks = Math.max(1, (int) Math.ceil(originalTier2 * scale));
			maxTicks = Math.max(1, (int) Math.ceil(originalMax * scale));
			double rate = BAL.d("mana_per_tick", MANA_PER_TICK);
			tier1Mana = originalTier1 * rate;
			tier2Mana = originalTier2 * rate;
			totalMana = originalMax * rate;
		}

		/** 闁圭顦板﹢鏉库枎闄囬幖顐﹀礉濞戞粎绠婚幖杈惧缁劎绮诲Δ鍐焾閻犱籍銈呯€柣銏╃厜缁辨繄绱撻埡鍐彋闁哄啫鐖煎Λ鎸庣▔瀹ュ顎栧ù锝呴閹洤顩奸敐鍡忓亾閺勫繆鍋撳Δ浣恒€婇柕?*/
		double manaDue(int progress) {
			if (progress >= maxTicks) return totalMana;
			if (progress <= tier1Ticks) return tier1Mana * progress / tier1Ticks;
			if (progress <= tier2Ticks) return tier1Mana + (tier2Mana - tier1Mana) * (progress - tier1Ticks) / (tier2Ticks - tier1Ticks);
			return tier2Mana + (totalMana - tier2Mana) * (progress - tier2Ticks) / (maxTicks - tier2Ticks);
		}
	}
	private static final java.util.Set<UUID> FLAT_CHARGING = ConcurrentHashMap.newKeySet();

	private SpiderMoonWeaverWebManager() {}

	private static boolean isSpiderMoonWeaver(ServerPlayer player) {
		return FormUtils.isForm(player, FormIdentifiers.SPIDER_MOON_WEAVER);
	}

	private static github.com.gengyoubo.sscfa.compat.ManaComponent mana(ServerPlayer player) {
		return github.com.gengyoubo.sscfa.compat.ManaUtils.getManaComponent(player);
	}

	/** 閻犲洩顕цぐ鍥偝閳轰緡鍟€鐟滅増鎸告晶鐘参熼垾宕囩闁?=闁圭⒈鍙€閻?/ 1=闁衡偓鐠囨彃姣婇柨娑橆槶閳?*/
	private static int getMode(ServerPlayer player) {
		return RegSpiderMoonWeaverStateComponent.SPIDER_MOON_WEAVER_STATE.get(player).getMode();
	}

	/** 婵炲吋绮忛、鎴﹀矗鐏炶棄姣婂☉鎾愁煼閺侇參鏁嶅顒€鐎奸柟?闁圭⒈鍙€閻?/ 闁衡偓鐠囨彃姣?婵☆垪鈧磭纭€闁挎稑鐗嗛褰掑箣妞嬪寒浼傞柟闈涱儏婵炲秹宕氶妶鍛毎闁挎稑鐭侀幖顐﹀礉濞戞鍘紒鍌欑劍椤掓盯宕氶崶銊ュ簥闁挎稑顦埀?*/
	public static void toggleMode(ServerPlayer player) {
		if (!isSpiderMoonWeaver(player)) return;
		if (CHARGING.containsKey(player.getUUID())) return;
		int next = (getMode(player) == MODE_ATTACK) ? MODE_BRIDGE : MODE_ATTACK;
		RegSpiderMoonWeaverStateComponent.SPIDER_MOON_WEAVER_STATE.get(player).setMode(next);
		ServerLevel sw = (ServerLevel) player.level();
		sw.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.WOOL_PLACE, SoundSource.PLAYERS, 0.8f, next == MODE_ATTACK ? 1.4f : 0.9f);
		player.displayClientMessage(Component.translatable(next == MODE_ATTACK
				? "message.my_addon.spider_moon_weaver.mode.attack"
				: "message.my_addon.spider_moon_weaver.mode.bridge"), true);
	}

	/** 濞戞挸顭烽弫顓㈠箰婢跺绉剁€殿喒鍋撳┑顔碱儓閹碱偊宕濆☉宕囩闁哄牆绉存慨鐔虹博椤栫偛娅㈤柡宥忕節閻?form / CD / mana闁挎稑顦埀?*/
	public static void start(ServerPlayer player) {
		if (CHARGING.containsKey(player.getUUID())) return;
		if (!isSpiderMoonWeaver(player)) return;
		// 缂備胶鍠嶇粩鎾嫉瀹ュ懎顫ら梻鍌樺妿椤?+ 鐎点倗鍎ら妴鍌炴晬閸ь櫞_release闁挎稒纰嶅妤呮煥椤旇棄鐦绘俊妤嬬秬閹癸絿绮诲Δ瀣
		if (!beginCharge(player)) return;
		CHARGING.put(player.getUUID(), new ChargeState(player));
		FLAT_CHARGING.remove(player.getUUID()); // 闁哄拋鍣ｉ埀顒佷亢閹碱偊宕?闁?闁炬梹绋愮粭锝咁嚕?
		ServerLevel sw = (ServerLevel) player.level();
		sw.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.WOOL_HIT, SoundSource.PLAYERS, 0.7f, 1.2f);
	}

	/** 濞戞挸顭烽弫顓㈠箰婢跺绉剁€殿喒鍋撳┑顔碱儍閳ь剙鑻柦鈺呮煣閻戞ɑ鍎楁俊妞煎劘閳ь剙绉烽幖顐﹀礉濞戝磭绀勯柛娆忚嫰閸ゎ噣姊归幐搴＄樆 / 婵炲吋绮忛、鎴︽⒐閹稿骸鐦婚悷娆欑畱瑜板倿鏁嶅☉娆愮疀闁告棑绱曢顒勬煂瀹ュ棛澧″Δ?form / CD / mana闁挎稑顦埀?*/
	public static void startFlat(ServerPlayer player) {
		if (CHARGING.containsKey(player.getUUID())) return;
		if (!isSpiderMoonWeaver(player)) return;
		if (!beginCharge(player)) return;
		CHARGING.put(player.getUUID(), new ChargeState(player));
		FLAT_CHARGING.add(player.getUUID()); // 妤犵偞濞婇幗鐢告嫅閸曨偄顫?闁?闁煎瓨鐭粭鍛寸嵁閹惰姤鎳?
		ServerLevel sw = (ServerLevel) player.level();
		sw.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.WOOL_HIT, SoundSource.PLAYERS, 0.7f, 1.0f);
	}

	/** 闁谎勬椤斿洭寮懞銉︽澒闁挎稒鍩爋wer JSON cooldown = 婵犲ň鍓濋妴鍌炴晬? 婵℃缍囩槐娆矰闁挎稑鏈妤呭箥鐎ｎ偅顦ч柟绋款槹閵嗗倹鎷呭鍡欐Х濞撴艾顑嗘慨宀€绮诲灏栧亾?*/
	private static boolean beginCharge(ServerPlayer player) {
		if (!github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.ready(player, SKILL_ID)) return false;
		if (mana(player).getMana() < BAL.d("start_mana", START_MANA)) return false;
		return github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.begin(player, SKILL_ID);
	}

	/** 婵絽绻戝﹢鍥礉閿涘嫷浼?tick 閻庝絻顫夐惁鈩冪▔椤忓嫭韬紒鎹愭硶鐢櫣鈧鍎奸惃鐔兼偨椤帞绀勯柟绋垮€稿﹢?SscAddonServerEvents 濞戞挻鐗滈弲?tick 鐎甸偊浜為獮鍡涙晬婢跺牃鍋?*/
	public static void tick(ServerPlayer player) {
		ChargeState s = CHARGING.get(player.getUUID());
		if (s == null) return;
		if (player.isDeadOrDying() || !isSpiderMoonWeaver(player)) {
			cancel(player); // 婵繆顔婃?/ 鐟滆埇鍨洪埀顑挎濞戭亝寰?闁?闁告瑦鐗楃粔鐑芥晬鐏炶偐鐟濈紓浣规尵閻?
			return;
		}
		if (s.ticks < s.maxTicks) {
			double due = s.manaDue(s.ticks + 1);
			double manaPerTick = Math.max(0.0, due - s.paidMana);
			github.com.gengyoubo.sscfa.compat.ManaComponent m = mana(player);
			if (m.getMana() + 1.0E-9 < manaPerTick) {
				release(player); // mana 闁肩増顨呴弫?闁?闁煎浜滄慨鈺呮煂婵犲啯鏉圭憸鐗堟尭婢х姴顩?
				return;
			}
			m.consumeMana(manaPerTick);
			s.paidMana = due;
			s.ticks++;
			ServerLevel sw = (ServerLevel) player.level();
			float chime = tierChimePitch(s);
			if (chime > 0f) {
				// 閻犳亽鍔嶉妴鍌炴儔椤掑嫭锛熼柨娑欏哺濞碱剚顬囬幇顒€鏂ч柣?SSC 闁藉啫瀚慨蹇曗偓鐟版湰閸ㄦ岸妫呴搹顐ｆ珡闁挎稑娼眔te_block hat+snare 闁告娲╅惃鐔兼晬? cloud 缂侇喗甯掗悺?
				sw.playSound(null, player.getX(), player.getY(), player.getZ(),
						SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.PLAYERS, 0.8f, chime);
				sw.playSound(null, player.getX(), player.getY(), player.getZ(),
						SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.PLAYERS, 0.8f, chime);
				github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(sw, player, ParticleTypes.CLOUD, player.getX(), player.getY() + 1.0, player.getZ(),
						10, 0.5, 0.5, 0.5, 0.0);
			} else if (s.ticks % 4 == 0) {
				github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(sw, player, ParticleTypes.CLOUD, player.getX(), player.getY() + 1.0, player.getZ(),
						4, 0.4, 0.5, 0.4, 0.0);
			}
		}
		// 閺夊牊鍎抽崺灞筋煥閳╁啨鈧倿宕ユ惔婵堢闁?tier3闁靛棔妞掔粭澶愬礃瀹ュ牃鍋?mana闁挎稑鐬煎ú鍧楀礆閻楀牊绶遍梺娆惧櫍閸ｆ挳寮?
	}

	/** 濞达綀娉曢弫銈夊嫉椤掍緡鍋ч柡鍌濆Г閺備焦绌卞┑鍡欐憼闁汇劌瀚板Σ鍥磹閸忕厧袚闂傚﹦顒茬槐婵囶渶濡鍚?0/40/60t闁靛棔绀侀顔芥償閹跨棜rk濞?4/28/42t闁?*/
	private static float tierChimePitch(ChargeState state) {
		if (state.ticks == state.tier1Ticks) return 1.0f;
		if (state.ticks == state.tier2Ticks) return 1.19f;
		if (state.ticks == state.maxTicks) return 1.414f;
		return 0f;
	}

	/** 闁哄鍎崇槐鎴炵▔婵犳碍鏆?/ 闁煎浜滄慨鈺呮煂婵犲啯鏉归柨娑欑鐎垫粏銇愰幘鍐差枀婵?+ 婵☆垪鈧磭纭€ + 妤犵偞濞婇幗鐢稿冀閸パ呯闁告瑦鍨垫慨鈺呮晬閸喆鈧倹鎷?CD = tier閼寸牚d_per_tier闁挎稑鐬肩划鐑樼▔閳ь剟寮靛鍛潳缂備焦鎸鹃悾濠氭晬婢跺牃鍋?*/
	public static void release(ServerPlayer player) {
		ChargeState s = CHARGING.remove(player.getUUID());
		boolean flat = FLAT_CHARGING.remove(player.getUUID());
		if (s == null) return;
		if (!player.isAlive() || !isSpiderMoonWeaver(player)
				|| github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.currentCastId(player, SKILL_ID) != s.castId
				|| github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.spec(player, SKILL_ID) == null) {
			github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.cancelled(player, SKILL_ID, s.castId);
			return;
		}
		int ticks = s.ticks;
		int tier = ticks >= s.maxTicks ? 3 : (ticks >= s.tier2Ticks ? 2 : 1);
		completeCharge(player, s, tier);
		if (s.mode == MODE_ATTACK) {
			fireAttack(player, tier);
		} else if (flat) {
			fireBridgeFlat(player, tier); // 闁告瑥鑻崵顕€姊归幐搴＄樆 / 婵炲吋绮忛、鎴︽⒐閹稿骸鐦?闁?闁煎瓨鐭粭鍛寸嵁閹惰姤鎳?
		} else {
			fireBridgeShoot(player, tier); // 闁告娲栭崵顕€姊归幐搴＄樆 闁?闁告瑦鍨甸惃鐘绘憘濞戞顐剧€?
		}
	}

	private static void completeCharge(ServerPlayer player, ChargeState state, int tier) {
		github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.retune(player, SKILL_ID, state.castId,
				(int) Math.ceil((double) state.cooldownTicks * tier / 3.0));
		github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.completed(player, SKILL_ID, state.castId);
	}

	/** 闁告瑦鐗楃粔鐑芥嫅閸曨偄顫旈柨娑樼墔缁楀绱掗幘鍓佹毈闁靛棔妞掔粭澶嬫交?CD闁挎稒姘ㄧ划鐑樼▔閳ь剟寮靛鍛潳濠㈡儼绮剧憴锕€顩?0 婵炴挸鎳撻鍥亹閺囶亞绀嗛柕?*/
	public static void cancel(ServerPlayer player) {
		ChargeState s = CHARGING.remove(player.getUUID());
		FLAT_CHARGING.remove(player.getUUID());
		github.com.gengyoubo.sscfa.cooldown.SkillCooldowns.cancelled(player, SKILL_ID, s == null ? -1 : s.castId);
	}

	/** 闁绘壕鏅涢宥夊箳婢跺苯娈犳繛鎾虫噽閹﹪鏁嶅畝鍕╅柛宥夋涧濡?UUID 婵炲牆顑囬弳鈧柕?*/
	public static void onDisconnect(ServerPlayer player) {
        cancel(player);
	}

	// 闁衡偓鐠囨彃姣婃俊顖椻偓宕囩闁挎稒鑹捐ぐ鍌滀焊閸曨喗纾藉☉鎾寸箓閼村﹪鏁嶉崼婵囧殥濞戞搩鍙冮幗鐢稿礄韫囨稈鍋撻悢铏圭Ч闁?
	private static void fireAttack(ServerPlayer player, int tier) {
		ShellProjectile bullet = new ShellProjectile(github.com.gengyoubo.sscfa.init.AddonEntities.WEB_MEMBRANE_BULLET.get(), player.level()); bullet.setOwner(player);
		bullet.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, 1.6f, 1.0f);
		player.level().addFreshEntity(bullet);
		ServerLevel sw = (ServerLevel) player.level();
		sw.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.9f, 0.7f);
	}

	// 闁圭⒈鍙€閻儳螣閳ュ磭纭€-妤犵偞濞婇幗鐢告晬濮橆剚韬柣婧炬櫅椤斿秹宕滃鍡樼厵闁煎瓨鑹鹃幃鎾搭殗濡粯瀵滄鐐村▕濞间即鏌фウ鎸庣＝濞戞挻绻冭棢闁挎稑鐗嗛柦鈺呭捶妫颁胶鐦嶉柤瀹犳濠€顏堝礈瀹ュ棙鐓欓梺鎯ф惈閸ゎ參鏁嶆径娑氬耿婵℃ぜ鍎卞锟犲矗椤栨稒绂屽ù鐙呯到閸ｆ椽鏌呴悢铏圭Ч闁?
	private static void fireBridgeFlat(ServerPlayer player, int tier) {
		int length = tier >= 3 ? 18 : (tier >= 2 ? 14 : 10);
		BlockPos pos = player.blockPosition(); // 闁煎瓨鑹鹃幃鎾搭殗濡粯瀵滄鐐村▕濞间即鏁嶉崼鈶╂晞婵ɑ鏌ㄩ惇浼存晬婢舵稓绀夊ù鐘查婢х娀寮崷顓у剳濞戞挴鍋撻柡宥囧帶缁辨垶鎱ㄧ€ｎ厼鍘撮梺鎹愭〃缁?
		Direction dir = player.getDirection();
		AddonWebBridgeAction.BuildWebBridge(player.level(), pos, dir,
				new AddonWebBridgeAction.WebBridgeConfig(length, 0), AddonBlocks.TEMP_WEB_BRIDGE.get());
		ServerLevel sw = (ServerLevel) player.level();
		sw.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.GRASS_PLACE, SoundSource.PLAYERS, 0.6f, 0.8f);
		github.com.gengyoubo.sscfa.network.DecorationParticles.spawn(sw, player, ParticleTypes.CLOUD, player.getX(), player.getY() + 0.5, player.getZ(),
				40, 1.0, 0.5, 1.0, 0.0);
	}

	// 闁圭⒈鍙€閻儳螣閳ュ磭纭€-闁告瑦鍨甸惃鐘绘晬濮橆剙绲洪悘蹇撳濡绢喚浠?BridgeWebBullet 闁炬梹绋愮粭锝咁嚕閻у摜绀勯柛娑欏灊閼垫垿寮悷鐗堝仴閻犙勫浜涙俊鐐茬Ф婢?BuildWebLadder 鐎点倝缂氬ú鎾寸▔濠靛浂娼介柍銉︽煛閳ь剚姊归～顏堝锤濡も偓瑜版煡寮存径绋挎暕缂傚啯鍨奸崯姗€鏁嶅☉妯诲殥濞戞搩鍘奸悿鍕媴閹惧瓨鐓㈢紓鍌滃Х缁偊鏁?
	private static void fireBridgeShoot(ServerPlayer player, int tier) {
		ShellProjectile bullet = new ShellProjectile(github.com.gengyoubo.sscfa.init.AddonEntities.BRIDGE_WEB_BULLET.get(), player.level()); bullet.setOwner(player);
		bullet.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, 2.0f, 1.0f); // 闁告鍠撴晶?speed=2, divergence=1
		player.level().addFreshEntity(bullet);
	}
}
