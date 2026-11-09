package github.com.gengyoubo.sscfa.ability;

import io.github.apace100.apoli.power.CooldownPower;
import io.github.apace100.apoli.power.Power;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.FormUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 閸欐绶よぐ銏♀偓浣稿瀼閹广垻顓搁悶鍡楁珤閿涘牏绶ゆ禍鍡曢嚋缂囧﹪鍘ゆ總妤嬬礉閺堝秴濮熺粩顖涙綀婵炰緤绱漥ackcooper閿?026-09-29 閻劍鍩涙稉澶庣枂鐎规氨顭堥敍澶堚偓?
 *
 * <p><b>閺嬭埖鐎幑顫敩</b>閿涙艾褰夌紘濠佺矤閵嗗本鏅ラ弸婊冨竾閸掕翰鈧秵鏁兼稉鎭掆偓灞藉瀼閹广垻鈹栨竟宕囩筏瑜般垺鈧降鈧秮鈧柡鈧梻绶よぐ銏♀偓渚婄礄my_addon:sheep_spell_form閿?
 * 缁屽搫锛撻弮?power閿涘绗夐幐鍌欐崲娴ｆ洘濡ч懗?鐞氼偄濮?姒勬梹纭堕敍灞姐亯閻掕泛鍙忔径杈ㄦ櫏閿涙稒瑕嗛弻鎾舵埛缂侇厾鏁?SheepFormRenderMixin 閻㈣甯悧鍫㈢筏濡€崇€烽敍?
 * SHEEP_FORM 閺佸牊鐏夌紒褏鐢婚幍鑳祰閺冨爼鏆?AI 閸嬫粍婧€/閹广垼顢呴敍鍫ｇ箹婵傛鍑℃宀冪槈閿涘鈧倹妫ゆ鎴濈潌閸斻劎鏁鹃敍鍧昺mediatelyTransform 閻剙褰夐敍?
 * 閻劍鍩涚€规氨顭堥敍澶涚幢閸掔増婀￠崚鍥ф礀閸樼喎鑸伴幀渚婄幢閻楁稑銈舵稉宥呭讲閸戔偓閸栨牓鈧?/p>
 *
 * <p><b>閸樼喎鑸伴幀浣风箽鐎?/b>閿涙艾褰夌紘濠勭仜闂傜顔囪ぐ鏇炲斧 FormID 閸掔増婀扮粻锛勬倞閸ｃ劌鍞寸€涙銆冮敍鍫熷瘻閻溾晛顔?UUID閿涘绱?b>Apoli 閸樼喓鏁?CD 韫囶偆鍙?/b>閿?
 * 閸掑洤鑸伴幀浣风窗闁插秴缂?power 鐎圭偘绶ョ€佃壈鍤?CooldownPower.lastUseTime 濞撳懘娴傞垾鏂衡偓鏂垮綁缂囧﹤澧犺箛顐ゅ弾閹碘偓閺?CooldownPower 閻?
 * 閸撯晙缍?tick閿涘苯鍨忛崶鐐叉倵閹稿鈧苯鎻╅悡褍澧挎担?+ 閸欐绶ょ紒蹇氱箖閺冨爼鏆遍妴宥呮礀閸愭瑱绱機D 濮濓絽鐖剁挧鎷屻€冮敍灞肩瑝闁插秶鐣婚敍娑氭暏閹村嘲鐣剧粙鍖＄礆閵?
 * SSCA 缂佺喍绔撮崘宄板祱閿涘湯killCooldowns閿涘ersistentState 鐠佹媽澶勯敍澶夌瑝閸欐鍨忚ぐ銏♀偓浣稿閸濆稄绱濋弮鐘绘付婢跺嫮鎮婇妴?/p>
 *
 * <p><b>閹镐椒绠欓崠?/b>閿涙艾甯ぐ銏♀偓浣筋唶瑜版洖鍟?NBT 閸掔増婀扮猾鑽ゆ畱閻溾晛顔嶉弫鐗堝祦閿涘牏娅ヨぐ鏇熸閼汇儳甯虹€硅泛顦╂禍搴ｇ筏瑜般垺鈧椒绗栭弮鐘侯唶瑜?閳?閼奉亜濮╅崚鍥ф礀
 * 閸╄櫣顢呮禍鍝勮埌 original_before_enable閿涘矂妲婚崡鈩冾劥缂囧﹤鑸伴幀渚婄礆閿涙稑褰夌紘濠冩埂闂傚瓨甯€缁惧尅绱伴弫鍫熺亯娑撳骸鑸伴幀渚€鍏橀崷銊у负鐎硅埖鏆熼幑顕€鍣烽敍?
 * 闁插秷绻橀崥搴ｆ暠 tickSweep 缂侇叀铔嬮敍鍫熸櫏閺嬫粌婀埆鎺旀埛缂侇厾绶ら敍娑欐櫏閺嬫粍鐥呮禍鍡忓晪閸掑洤娲栭敍澶堚偓?/p>
 */
public final class BeepSheepFormManager {

	private BeepSheepFormManager() {}

	/** 閻溾晛顔?UUID 閳?閸欐绶よ箛顐ゅ弾閿涘牆甯?FormID + Apoli CD 韫囶偆鍙?+ 閸欐绶ら弮璺哄煝閿涘鈧?*/
	private static final Map<UUID, SheepSnapshot> SNAPSHOTS = new HashMap<>();

	private record SheepSnapshot(String originalFormId, Map<String, Integer> cooldowns, long sheepStartTime) {}

	/** Existing persisted transformation snapshot; callers must not treat it as an active form/power. */
	public static String originalFormId(ServerPlayer player) {
		SheepSnapshot snapshot = SNAPSHOTS.get(player.getUUID());
		return snapshot == null ? null : snapshot.originalFormId();
	}

	/** 瑜版挸澧犻弰顖氭儊婢跺嫪绨紘濠傝埌閹緤绱欓幐?FormID 閸掋倕鐣鹃敍灞藉蓟缁旑垰褰查悽顭掔礆閵?*/
	public static boolean isSheepForm(net.onixary.shapeShifterCurseForge.form.FormDefinition form) {
		return form != null && FormIdentifiers.SHEEP_FORM.equals(form.id());
	}

	/** 閸欐绶ゅ鈧慨瀣剁窗韫囶偆鍙庨崢鐔疯埌閹?+ Apoli CD 閳?閻剙鍨忕紘濠傝埌閹緤绱欓弮鐘荤拨鐏炲骏绱濋悽銊﹀煕鐎规氨顭堥敍澶堚偓?*/
	public static void onSheepStart(ServerPlayer player) {
		// 鐟佸懏顒撮幍鎾存焽閿?026-09-29 閻劍鍩涚€圭偞绁撮梻顕€顣?閿涘绱伴崣妯肩筏閻剟妫挎稉顓燁剾鐟佸懏顒撮垾鏂衡偓鏃€绔?PLAYING_DEAD 閸欏﹣鍗抽悽鐔告櫏閺嬫嚎鈧焦浠径宥囩彲婵￥鈧?
		// 閼汇儰绗夊〒鍜冪窗閺佸牊鐏夌紒褏鐢?tick閿涘牓鏀?SWIMMING 婵寧鈧?閹镐胶鐢婚崶鐐额攨閿涘绱濇稉鏃傜筏閺堢喖妫?PlayDeadEndClient 閻劏锛?GLFW
		// 缂佹洝绻冮幐澶愭暛鐏炲繗鏂€娴犲秳绱伴崣鎴炲絹閸撳秶绮ㄩ弶鐔峰瘶 閳?缂囧﹨闊╂稉濠冩￥缂囧氦銈块摶?power 閳?extra() 鏉╂柨娲?0 閳?force(0) 濞撳懐鈹栫憗鍛劥 CD
		//閿涘牆宓嗛梻顕€顣?閵嗗苯褰夐崶鐐叉倵 CD 鐞氼偊鍣哥純顔衡偓宥囨畱閻喎娲滈敍灞炬）韫?20:12:54 skill disabled 鐠€锕€鎲℃稉搴㈩劃閸氳鎮庨敍澶堚偓?
		if (player.hasEffect(github.com.gengyoubo.sscfa.init.AddonEffects.PLAYING_DEAD.get())) {
			player.removeEffect(github.com.gengyoubo.sscfa.init.AddonEffects.PLAYING_DEAD.get());
			player.removeEffect(net.minecraft.world.effect.MobEffects.BLINDNESS);
			player.removeEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN);
			player.setPose(net.minecraft.world.entity.Pose.STANDING);
		}
		if (SNAPSHOTS.containsKey(player.getUUID())) return; // 闁插秴顦查弬鑺ョ《閸欘亜鍩涢弬鐗堟櫏閺嬫粍妞傞梹?
		net.onixary.shapeShifterCurseForge.form.FormDefinition original = FormUtils.getCurrentForm(player);
		String originalId = original != null && original.id() != null
				? original.id().toString() : null;
		if (originalId == null || FormIdentifiers.SHEEP_FORM.toString().equals(originalId)) {
			return; // 瀹稿弶妲哥紘?閺冪姴鑸伴幀渚婄礄閻炲棜顔戞稉宥呭讲鏉堟拝绱氶敍姘瑝韫囶偆鍙庢稉宥呭瀼閹?
		}
		// Apoli 閸樼喓鏁?CD 韫囶偆鍙庨敍鍦昽oldownPower.lastUseTime 闂呭繐鍨忚ぐ銏♀偓渚€鍣稿鐑樼闂嗚绱遍崚鍥ф礀閸氬骸娲栭崘娆戠敾鐠у府绱?
		Map<String, Integer> cds = new HashMap<>();
		for (Power power : io.github.apace100.apoli.component.PowerHolderComponent.KEY.get(player).getPowers()) {
			if (power instanceof CooldownPower cooldown && cooldown.getRemainingTicks() > 0) {
				cds.put(power.getType().getIdentifier().toString(), cooldown.getRemainingTicks());
			}
		}
		long now = player.level().getGameTime();
		SNAPSHOTS.put(player.getUUID(), new SheepSnapshot(originalId, cds, now));

		net.onixary.shapeShifterCurseForge.form.FormDefinition sheep = net.onixary.shapeShifterCurseForge.form.FormRegistry.get(FormIdentifiers.SHEEP_FORM);
		if (sheep != null) {
			net.onixary.shapeShifterCurseForge.form.TransformManager.immediatelyTransform(player, sheep.id()); // 閻剙褰夐敍鍫熸￥姒涙垵鐫嗛崝銊ф暰閿涘瞼鏁ら幋宄扮暰缁嬪尅绱?
			// fallback 娣囶喖顦查敍?026-09-29 鐎圭偞绁撮梻顕€顣?閻喎娲滈敍澶涚窗immediatelyTransform 鐏忛箖鍎?_setForm 閳?
			// NormalForm.onTransform_Finish 閳?setFallbackForm(null)閳ユ柡鈧攺ull 閺屻儰绗夐崚?form 娴兼碍濡?
			// fallbackFormID 濮光剝鐓嬮幋?InitialForm閿?瀵偓娑旓箑澧犳禍鍝勮埌閿涘鈧倻绶ら張鐔兼？娑撯偓閺冿缚鎹㈡担鏇＄熅瀵板嫯铔?
			// FormUtils.applyFallback閿涘牆顩ч柌宥堢箻閻ц缍嶉柧鎯х磽鐢鍘规惔鏇礆閿涘瞼甯虹€硅泛姘ㄦ导姘愁潶闁挎瑨顕ら崣妯烘礀娴滆櫣琚妴?
			// 閸欐绶ら崥搴ｇ彌閸掔粯濡?fallback 娣囶喖娲栭妴灞藉綁缂囧﹤澧犻惃鍕斧瑜般垺鈧降鈧稄绱濇穱婵婄槈娴犺缍?fallback 闁棄娲栭崚鏉垮斧瑜般垺鈧降鈧?
			net.onixary.shapeShifterCurseForge.form.FormDefinition originalForm = net.onixary.shapeShifterCurseForge.form.FormRegistry.get(ResourceLocation.tryParse(originalId));
			if (originalForm != null) {
				
			}
		}
	}

	/** 閸欐绶ょ紒鎾存将閿涙艾鍨忛崶鐐插斧瑜般垺鈧?+ 閸ョ偛鍟?Apoli CD閿涘牆鎻╅悡褍澧挎担?+ 閸欐绶ょ紒蹇氱箖閺冨爼鏆遍敍澶堚偓?*/
	public static void onSheepEnd(ServerPlayer player) {
		SheepSnapshot snapshot = SNAPSHOTS.remove(player.getUUID());
		net.onixary.shapeShifterCurseForge.form.FormDefinition original = snapshot != null
				? net.onixary.shapeShifterCurseForge.form.FormRegistry.get(ResourceLocation.tryParse(snapshot.originalFormId())) : null;
		if (original == null) {
			original = net.onixary.shapeShifterCurseForge.form.FormRegistry.get(net.onixary.shapeShifterCurseForge.form.FormRegistry.ORIGINAL_BEFORE_ENABLE); // 鐠佹澘缍嶇紓鍝勩亼閸忔粌绨抽敍姘眽瑜?
		}
		net.onixary.shapeShifterCurseForge.form.TransformManager.immediatelyTransform(player, original.id());
		// fallback 娣囶喖顦查敍鍫濇倱 onSheepStart閿涘绱伴崚鍥ф礀閻?_setForm 鐏忛箖鍎?setFallbackForm(null) 閸欏牅绱伴幎?
		// fallbackFormID 濮光剝鐓嬮幋?InitialForm閳ユ柡鈧柨鍨忛崶鐐叉倵缁斿鍩㈡穱顔兼礀閸樼喎鑸伴幀渚婄礉闂冩彃鎮楃紒顓濇崲娴?applyFallback
		// 閹跺﹦甯虹€瑰爼鏁婄拠顖氬綁閹存劕绱戞稊锕€澧犳禍鍝勮埌閿?026-09-29 鐎圭偞绁撮妴宀勨偓鈧崙铏圭筏瑜般垺鈧礁鎮楅崣妯规眽缁眹鈧秶婀￠崶鐙呯礆閵?
		
		// CD 閸ョ偛鍟撻敍姘瀼瑜般垺鈧礁鍑￠柌宥呯紦 power 鐎圭偘绶ラ敍鍦旸 濞撳懘娴傞敍澶涚礉閹稿鈧苯鎻╅悡褍澧挎担?- 閸欐绶ょ紒蹇氱箖閵嗗秶鐢婚崘娆欑礉
		// 娴?CD 鐞涖劎骞囨稉搴涒偓灞肩矤閺堫亜褰夌紘濞库偓宥呯暚閸忋劋绔撮懛杈剧礄濮濓絽鐖剁拋锛勭暬閿涘瞼鏁ら幋宄扮暰缁嬭￥鈧苯鍩嗛崗銊╁櫢閺傛媽顓哥粻妞尖偓宥忕礆閵?
		// 闁秴宸婚悳鈺侇啀瑜版挸澧?power 閹?type id 閸栧綊鍘よ箛顐ゅ弾閿涘湧owerTypeRegistry 閺?get(Identifier) API閿涘奔绗夋笟婵婄鐎瑰喛绱?
		if (snapshot != null && !snapshot.cooldowns().isEmpty()) {
			long elapsed = Math.max(0, player.level().getGameTime() - snapshot.sheepStartTime());
			var holder = io.github.apace100.apoli.component.PowerHolderComponent.KEY.get(player);
			for (Power power : holder.getPowers()) {
				if (!(power instanceof CooldownPower cooldown)) continue;
				Integer snap = snapshot.cooldowns().get(power.getType().getIdentifier().toString());
				if (snap == null) continue;
				int remaining = (int) Math.max(0, snap - elapsed);
				if (remaining > 0) {
					cooldown.setCooldown(remaining);
					io.github.apace100.apoli.component.PowerHolderComponent.syncPower(player, power.getType());
				}
			}
		}
	}

	/** 閸忔粌绨抽幍顐ｅ伎閿涘牊瀵?END_SERVER_TICK閿涘绱伴弫鍫熺亯濞屸€茬啊娴ｅ棜绻曢崡鈥虫躬缂囧﹤鑸伴幀?閳?閸掑洤娲栭妴?
	 *  鐟曞棛娲婇敍姘卞婵傝泛顦婚惃鍕閸掑洭娼锝呯埗鐠侯垰绶為敍鍫燁劥娴滐繝鍣搁悽鐔哥閺佸牊鐏?閹稿洣鎶ゅ〒鍛櫏閺?reload 濞撳懏鏅ラ弸婊呯搼閿涘鈧?*/
	public static void tickSweep(net.minecraft.server.MinecraftServer server) {
		for (var world : server.getAllLevels()) {
			for (ServerPlayer sp : world.players()) {
				net.onixary.shapeShifterCurseForge.form.FormDefinition form = FormUtils.getCurrentForm(sp);
				if (isSheepForm(form) && !sp.hasEffect(github.com.gengyoubo.sscfa.init.AddonEffects.SHEEP_FORM.get())) {
					onSheepEnd(sp); // 閺佸牊鐏夊鍙夋￥娴ｅ棗鑸伴幀浣界箷閺勵垳绶ら敍姘划婢?
				}
			}
		}
	}

	/** 閻溾晛顔嶉弫鐗堝祦閹镐椒绠欓崠鏍电礄PersistentState 鐠囶厺绠熼敍澶涚窗閸?NBT閿涘牊鏌囩痪?閸嬫粍婀囨稉宥勬丢閸樼喎鑸伴幀浣筋唶瑜版洩绱氶妴?*/
	public static void writePlayerNbt(ServerPlayer player, CompoundTag nbt) {
		SheepSnapshot snapshot = SNAPSHOTS.get(player.getUUID());
		if (snapshot == null) return;
		CompoundTag tag = new CompoundTag();
		tag.putString("OriginalForm", snapshot.originalFormId());
		tag.putLong("SheepStart", snapshot.sheepStartTime());
		CompoundTag cds = new CompoundTag();
		snapshot.cooldowns().forEach(cds::putInt);
		tag.put("Cooldowns", cds);
		nbt.put("SSCA.SheepForm", tag);
	}

	/** 閻ц缍嶉幁銏狀槻閿涙俺顕?NBT 闁插秴缂撹箛顐ゅ弾閿涘牆褰夌紘濠佽厬閹哄鍤庨柌宥堢箾缂侇叀铔嬮敍澶堚偓?*/
	public static void readPlayerNbt(ServerPlayer player, CompoundTag nbt) {
		if (!nbt.contains("SSCA.SheepForm")) return;
		CompoundTag tag = nbt.getCompound("SSCA.SheepForm");
		Map<String, Integer> cds = new HashMap<>();
		CompoundTag cdTag = tag.getCompound("Cooldowns");
		for (String key : cdTag.getAllKeys()) cds.put(key, cdTag.getInt(key));
		SNAPSHOTS.put(player.getUUID(), new SheepSnapshot(
				github.com.gengyoubo.sscfa.util.FormIdMigration.canonical(tag.getString("OriginalForm")),
				cds, tag.getLong("SheepStart")));
	}

	/** 閻溾晛顔嶈ぐ璇茬俺闁偓閸戠尨绱欐稉宥囨殌鐏忛晲缍嬮弫鐗堝祦閿涘绱板〒鍛敶鐎涙顔囪ぐ鏇樷偓?*/
	public static void onPlayerRemoved(ServerPlayer player) {
		SNAPSHOTS.remove(player.getUUID());
	}

	// ==================== 娴滃娆㈤幐鍌濇祰閿涘湯scAddon.registerApoliSystems 鐠嬪啰鏁ら敍?====================

	/** 娴滃娆㈠▔銊ュ斀閿涙氨甯虹€?NBT 鐠囪鍟撻敍鍫熷瘮娑斿懎瀵查敍? 閻ц缍嶉惌顐ｎ劀 + 閺傤厾鍤庡〒鍛倞閵?*/
	public static void init() {
		// JOIN閿涙艾褰夌紘濠佽厬闁插秷绻橀敍鍫熸櫏閺嬫粈绗岃ぐ銏♀偓渚€鍏橀崷銊у负鐎硅埖鏆熼幑顕€鍣烽敍灞姐亯閻掑墎鐢荤挧甯礆閿涙稒顒濇径鍕幑鎼存洑琚辩粔宥呯磽鐢潻绱?
		// 閳?閸楋紕绶よぐ銏♀偓浣风稻閺佸牊鐏夊鍙夋￥閿涘牆浠犻張宥嗘埂闂傛潙鍩岄張鐔告弓鐠?onRemoved閿涘鍟?閸掑洤娲栭崢鐔疯埌閹緤绱?
		// 閳?韫囶偆鍙庢稉銏犮亼娴ｅ棗顦╂禍搴ｇ筏瑜般垺鈧緤绱欓弮褍鐡ㄥ锝忕礆閳?閸掑洤娲栨禍鍝勮埌闂冩彃宕卞姹団偓?
		github.com.gengyoubo.sscfa.util.SscEvents.onPlayerJoin(player -> {
					net.onixary.shapeShifterCurseForge.form.FormDefinition form = FormUtils.getCurrentForm(player);
					if (isSheepForm(form)) {
						// 闁插秷绻橀崥搴ｇ彌閸掕鎱?fallback閿涙艾鐡ㄥ锝夊櫡閻?fallbackFormID 閸欘垵鍏橀弰顖欑瑐娑撯偓娴兼俺鐦?
						// onTransform_Finish 濮光剝鐓嬮惃鍕眽瑜般垹鈧》绱遍悽銊ユ彥閻撗冨斧瑜般垺鈧浇顩惄鏍电礉闂冭尙娅ヨぐ鏇㈡懠娴犺缍?
						// applyFallback 閹跺﹤褰夌紘濠佽厬閻ㄥ嫮甯虹€硅泛褰夋禍铏硅閵?
						SheepSnapshot snap = SNAPSHOTS.get(player.getUUID());
						net.onixary.shapeShifterCurseForge.form.FormDefinition original = snap != null
								? net.onixary.shapeShifterCurseForge.form.FormRegistry.get(ResourceLocation.tryParse(snap.originalFormId())) : null;
						if (original != null) {
							
						}
						if (!player.hasEffect(github.com.gengyoubo.sscfa.init.AddonEffects.SHEEP_FORM.get())) {
							onSheepEnd(player); // 閺冪姴鎻╅悡褎妞傞崘鍛村劥閸忔粌绨抽崚鍥︽眽瑜?
						}
					}
			});
		// 濞夈劍鍓伴敍?*娑撳秷鍏?*濞夈劌鍞?DISCONNECT 濞撳懐鈹栬箛顐ゅ弾閿?026-09-29 鐎圭偞绁撮梻顕€顣?閻喎娲滈敍澶涚窗Fabric DISCONNECT
		// 娴滃娆㈤崷?onDisconnected HEAD 鐟欙箑褰傞敍灞炬－娴?PlayerManager.remove 閳?savePlayerData 閳?
		// writeCustomDataToNbt 鐎涙娲忛垾鏂衡偓鏂垮帥濞撳懎鍞寸€涙銆冩导姘愁唨 SSCA.SheepForm 娴犲孩婀崘娆掔箻 NBT閿涘矂鍣告潻娑樻倵
		// readPlayerNbt 閺冪姵鏆熼幑?閳?閸掔増婀?onSheepEnd 鐠ч姹夎ぐ銏犲幑鎼存洩绱欓妴灞藉綁缂囧﹪鈧偓閸戝搫娲栭弶銉ф纯閹恒儱褰夋禍鎭掆偓宥忕礆閵?
		// 閸愬懎鐡ㄧ悰銊︽￥濞夊嫭绱℃搴ㄦ珦閿涙岸鍣告潻娑欐 readPlayerNbt 閹?UUID 鐟曞棛娲婇妴浣稿煂閺?onSheepEnd 瀹?remove閵?
		// 閸嬫粍婀囨潻娑氣柤缂佹挻娼懛顏嗗姧濞撳懐鈹栭妴?
	}

	/** NBT 閸愭瑱绱伴悽?mixin閿涘湧layerEntity.writeCustomDataToNbt 閺堫偄鐔敍澶庣殶閻劊鈧?*/
	public static void onWriteNbt(ServerPlayer player, CompoundTag nbt) {
		writePlayerNbt(player, nbt);
	}

	/** NBT 鐠囦紮绱伴悽?mixin閿涘湧layerEntity.readCustomDataFromNbt 閺堫偄鐔敍澶庣殶閻劊鈧?*/
	public static void onReadNbt(ServerPlayer player, CompoundTag nbt) {
		readPlayerNbt(player, nbt);
	}
}
