package github.com.gengyoubo.sscfa.spell;

import github.com.gengyoubo.sscfa.spell.research.*;
import github.com.gengyoubo.sscfa.util.TrinketUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 閺堝牆鐨规鏃€纭舵稊锔芥煢濞夋洘婀囬崝锛勵伂閺嶇绺鹃敍鍧朼ckcooper閿涘鈧倹婀囬崝锛勵伂閺夊啫鈻夐敍姘剁崣鐠囦椒鍍甸幋鎾摕濞夋洑鍔熼妴浣筋嚢娑旓箑鍞撮崡鐤叡閵嗕礁鍨介崘宄板祱/濞夋洖濮忛妴?
 * 閹稿宓庢潪纾嬧偓鎰畽濮ｆ梻缂夐弨鍙ユ縺鐎瑰厖绗岄崘宄板祱閿涘牊寮у▎鈩冩殶=濮濓絽鐖堕敍宀€鏁ゆ潻鍥Ш婢舵俺绉哄鎲嬬礆閿涘本澧界悰宀勭摕濞夋洏鈧礁鍟撻崘宄板祱閵嗕胶鐤粔顖滅病妤犲被鈧?
 */
public final class SpellCastManager {
	private SpellCastManager() {
	}

	/** 閻溾晛顔嶈ぐ鎾冲娴ｂ晜鍩撻惃鍕摕濞夋洑鍔熼敍鍫熸弓鐟佸懎顦潻鏂挎礀 null閿涘鈧?*/
	public static ItemStack getEquippedBook(ServerPlayer player) {
		return TrinketUtils.findFirstEquipped(player, s -> s.getItem() == github.com.gengyoubo.sscfa.init.AddonItems.MOON_DUST_SPELLBOOK.get());
	}

	/** 闁插﹥鏂佹稊锕€鍞撮幐鍥х暰濡茬晫娈戞鏃€纭堕敍鍫燁劀鐢憡銆傞敍澶堚偓?*/
	public static void cast(ServerPlayer player, int slot) {
		cast(player, slot, 0);
	}

	public static void cast(ServerPlayer player, int slot, int token) {
		castInternal(player, slot, 0, 1.0f, token);
	}

	/**
	 * 娑撳瓨妞傞梽宥嗐€傞弬鑺ユ杹閿涘牊纭堕崝娑楃瑝鐡掑厖绗佹潻鐐插毊閿涘瞼鏁ら幋?2026-09-18 鐎规氨顭堥敍澶涚窗閼奉亜濮╅梽宥呭煂娴犳ê绶辩挧椋庢畱閺堚偓妤傛ɑ銆傞敍?
	 * 閺堫剚顐?CD 鑴?.2 閹晝缍掗敍娑楃瑝閸愭瑥宓庢潪?NBT閿涘奔绗呭▎鈩冩煢濞夋洑绮涢幐澶婂斧濡楋絻鈧倸鍙忛柈銊︺€傞柈鎴掔帛娑撳秷鎹?閳?缂佸瓨瀵旂痪銏犵摟閹绘劗銇氶妴?
	 */
	public static void castDowngraded(ServerPlayer player, int slot) {
		castDowngraded(player, slot, 0);
	}

	public static void castDowngraded(ServerPlayer player, int slot, int token) {
		// C has no affordability gate; manual level selection remains available.
		castInternal(player, slot, 0, 1.0f, token);
	}

	/** 閸愬懘鍎寸紒鐔剁閺傝姤纭堕敍姝爋rcedLevel>0 閻劍瀵氱€规碍銆傞敍鍫滃閺冨爼妾峰锝忕礆閿涘苯鎯侀崚娆戞暏閸楃柉閰卞锝勭秴閿涙矞dPenalty 娑撶儤婀板▎?CD 閹晝缍掗崐宥囧芳閵?*/
	private static void castInternal(ServerPlayer player, int slot, int forcedLevel, float cdPenalty, int token) {
		if (github.com.gengyoubo.sscfa.util.AbilityUseGuard.blocked(player)) return;
		if (SpellChannelManager.isCasting(player)) return;
		// 閸欐绶ら梻鎼佹，閿?026-09-29 閻劍鍩涚€规氨顭堥敍澶涚窗閸欐ɑ鍨氱紘濠傛倵缁備焦顒涢弬鑺ョ《閿涘牏绶ら煫鍕摍缂堣绗夐崝銊╃摕濞夋洑鍔熼敍澶涚幢
		// 娑撳秶绮伴幓鎰仛闂?缁俱垹鐡ч垾鏂衡偓鏂剧瑢 STUN 濞屽绮径鍕倞娑撯偓閼疯揪绱濋梼鎻掑綁缂囧﹥婀￠梻鏉戝煕閹绘劗銇?
		if (player.hasEffect(github.com.gengyoubo.sscfa.init.AddonEffects.SHEEP_FORM.get())) return;
		ItemStack book = getEquippedBook(player);
		if (book == null || book.isEmpty()) {
			return;
		}
		int count = SpellbookData.getSlotCount(book);
		if (slot < 0 || slot >= count) {
			return;
		}
		ItemStack scroll = SpellbookData.getScroll(book, slot);
		if (scroll.isEmpty()) {
			return;
		}
		if (ArcaneAnalysis.isUnanalyzed(scroll)) {
			player.displayClientMessage(Component.translatable("message.ssc_addon.analysis.required"), true);
			return;
		}
		Spell spell = ScrollData.getSpell(scroll);
		if (spell == null) {
			return;
		}
		if(!RuneScheme.validScroll(player,scroll,spell))return;
        Level world = player.level();
		// 閹诡澀鍔熺粙鍐茬暰閺堢噦绱欓梼鑸殿唽 C 鎼?5.2閿涘苯鍑￠幏宥嗘緲閿涘绱伴幋妯绘灍娑擃厽宕叉稊锕€鎮楅惌顓熸埂閸愬懐顩﹀銏℃煢濞夋洩绱欓梼鎻掝樋娑旓附寮ч拑婵婄箾閻㈩煉绱?
		if (FormCastingStyle.isSwapStabilizing(player)) {
			SpellChannelManager.playNoManaSound(player); // 閹诡澀鍔熺粙鍐茬暰閺堢喐瀚嗙紒婵撶窗閻忣偆鍔欓悢鍕純闂?
			player.displayClientMessage(SpellCastFeedback.stabilizing(FormCastingStyle.getSwapReadyAt(player) - world.getGameTime()), true);
			return;
		}
		// 闂冭埖顔?B閿涘煪?5.2閿涘绱伴崡鐤叡 NBT 娑撳海甯虹€硅泛鍙℃禍顐ャ€冮崣灞剧爱閸掋倕鐣鹃垾鏂衡偓鏃€宕插Σ?閹诡澀鍔?閸氬本纭堕張顖滎儑娴滃苯绱堕崡鐤叡閸у洣绗夐懗鐣岀搏 CD閵?
		// 鐞氼偄鍙℃禍顐ャ€冮幏锔藉焻閺冭埖濡搁崗鍙橀煩缂佹挻娼弮璺哄煝閸ョ偛鍟撻崡鐤叡 NBT閿涘牏绮℃鏉挎惂閸氬本顒為敍澶涚窗閺傜増宕查崗銉ф畱閸楃柉閰?HUD 闁喚鍍垫稊鐔诲厴濮濓絿鈥橀弰鍓с仛
		if (ScrollData.isOnCooldown(scroll, world)
				|| SharedSpellCooldowns.isOnSharedCooldown(player, spell)) {
			SpellChannelManager.playNoManaSound(player); // CD 娑擃厽鏌﹂弨鎾呯窗閻忣偆鍔欓悢鍕純闂?
			player.displayClientMessage(SpellCastFeedback.cooldown(SharedSpellCooldowns.getEffectiveCooldownEnd(player, scroll) - world.getGameTime()), true);
			if (!ScrollData.isOnCooldown(scroll, world)) {
				long sharedEnd = SharedSpellCooldowns.getCooldownEndOf(player, spell);
				if (sharedEnd > ScrollData.getCooldownEnd(scroll)) {
					ScrollData.setCooldownEnd(scroll, sharedEnd);
					SpellbookData.setScroll(book, slot, scroll);
				}
			}
			return;
		}
		// 閺傝姤鏂佸锝勭秴閿涘牅缍嗛梼鍫曗偓澶嬨€傞敍灞?.4閿涙稓宸遍惇?閸楃柉閰卞锝勭秴閿涙稐绗佹潻鐐插毊闂勫秵銆傞悽?forcedLevel 閹稿洤鐣炬稉瀛樻濡楋綇绱?
		int level = Math.max(1, Math.min(spell.getMaxLevel(), forcedLevel > 0 ? forcedLevel : ScrollData.getCastLevel(scroll)));
		// 濞夋洟妯€閸旂姵鍨氶敍姘斥偓妤勬憫閸婂秶宸奸敍鍫濆弿姒勬梹纭跺В蹇曢獓 +10%閿? 瑜般垺鈧椒缈伴崪宀冣偓妤勬憫娑旀ê灏敍鍫滃▏姒勬梻閮?鑴?.85閿? 濮ｅ繒楠囬懓妤勬憫閸婂秶宸?
		// 閿涘牐鈧鎽戦幐澶嬫煢閺€鐐€傛担宥囩暬閳ユ柡鈧柧缍嗛梼鑸垫煢閺€鍓ф阜閽冩繐绱遍崣顒€鏁滄禍鎻掓嫲 +1 閸欘亜濮炲鐑樻煢濞夋洘鏅ラ弸婊愮礉娑撳秵甯规妯库偓妤勬憫閿?
		RuneModifiers runeModifiers=RuneScheme.modifiers(player,scroll,level);
        int manaCost = SpellNumbers.finalManaCost(spell, book, player, level, scroll);


		// 閺傝姤纭堕崜宥囩枂閺嶏繝鐛欓敍鍫濐洤闂勩劎浼€鐟曚焦鐪伴崙鍡樻Е閸涙垝鑵戦弬鐟版健閿涘绱版径杈Е閹锋帞绮烽弬鑺ョ《閵嗕椒绗夐懓妤佺《閸?CD閿涘牅璞㈡總鎴犱紥娴肩娀鈧礁銇戠拹銉ょ瑝濞戝牐鈧绱?
		if (spell.getCastingMode() == SpellCastingRules.Mode.AUTOMATIC && !RuneCastContext.with(runeModifiers,()->spell.canCast(player))) {
			SpellChannelManager.playFailureSound(player);
			player.displayClientMessage(spell.getCastFailureMessage(player).copy().withStyle(ChatFormatting.RED), true);
			return;
		}

		FormationElement spellElement = spell.getElement();
		float ratio = ScrollData.getDurabilityRatio(scroll);   // 1=濠娾剝顐奸弫? 鐡掑﹣缍嗛幆鈺冪稈鐡掑﹤銇?
		// 瑜般垺鈧椒缈伴崪宀€鐡戠痪褍濮為幋鎰剁礄閼窖冨帨楠炶偐浼?闂冩寧鎯婇敍姘将閸炪倗閮?+1閿涘奔绗傞梽?5閿涘鈧柡鈧柨褰旈崝鐘叉躬閺傝姤鏂佸锝勭秴娑?
		// 閿涘牐鈧鎽戝鍙夊瘻濡楋絼缍呯紒鎾剁暬鐎瑰本鐦敍娑楃堪閸滃苯褰ч崝鐘插繁閺佸牊鐏夌粵澶岄獓閿涘奔绗夐幒銊╃彯閼版鎽戦敍宀冾嚔娑斿绗岄弮褏澧楁稉鈧懛杈剧礆
		level = FormAffinity.bonusSpellLevel(player, spellElement, level);
		// 濞夋洟妯€閸旂姵鍨氶敍姘倱缁婵€ +12%/缁狙佲偓浣割嚠缁斿閮存导?-12%/缁狙嶇幢閸氬瞼閮?cd -5%/缁狙嶇幢缁屾椽妫垮▔鏇㈡█閸欘亞缂夌粚娲？缁?cd
		// 瑜般垺鈧椒缈伴崪宀嬬窗娴笺倕顔?閼版鎽?CD 娑撳绠婚崠鐚寸礄娑撳孩纭堕梼鐢靛缁斿褰旈崝鐙呯礆
		float damage = spell.getBaseDamage() * ratio * spell.getDamageMultiplier(level)
				* FormationData.sumDamageMultiplier(book, spellElement)
				* FormAffinity.damageMultiplier(player, spellElement);
		// 闂冭埖顔?B閿涘煪?.2閿涘绱扮紒鐔剁閸愬嘲宓堥崗顒€绱￠垾鏂衡偓鏂款樋娑旀ê灏?+ 閸欏苯鐪版稉瀣閿涘牏绮风€?floor + 0.2鑴崇粵澶岄獓閸╁搫鍣敍澶涚礉
		// 娑撳骸宕熼悪顑垮▏閻?鐎广垺鍩涚粩?HUD 閸忚京鏁ら崥灞肩鐎圭偟骞囬敍鍦玴ellNumbers閿?
		float formationCdMul = FormationData.sumCooldownMultiplier(book, spellElement);
		float affinityCdMul = FormAffinity.cooldownMultiplier(player, spellElement);
		damage=runeModifiers.power(damage,RuneCapabilities.of(spell.getId().getPath()).contains(RuneModifiers.Stat.DAMAGE),RuneCapabilities.of(spell.getId().getPath()).contains(RuneModifiers.Stat.HEAL));
        final float runeDamage=damage;
        int cd = runeModifiers.cooldown(SpellNumbers.finalCooldownTicks(spell, level, ratio, formationCdMul, affinityCdMul));
		if (cdPenalty != 1.0f) {
			cd = Math.round(cd * cdPenalty); // 娑撳绻涢崙濠氭濡楋絾鍎电純姘剧窗閺堫剚顐?CD 鑴?.2閿涘牏鏁ら幋宄扮暰缁嬪尅绱?
		}

		net.minecraft.nbt.CompoundTag previousNbt = scroll.getTag() == null ? null : scroll.getTag().copy();
		if (!spell.prepareScroll(player, scroll)) {
			SpellChannelManager.playFailureSound(player);
			return;
		}
		if (!java.util.Objects.equals(previousNbt, scroll.getTag())) {
			SpellbookData.setScroll(book, slot, scroll);
		}
		final int castLevel = level;
		final int castMana = manaCost;
		// Book bonuses quote collapse; preparation pays the player's C every 2 ticks.
		SpellChannelManager.start(player, spell, scroll, level, false, token, manaCost, cd,
				() -> getEquippedBook(player) == book && ItemStack.matches(SpellbookData.getScroll(book, slot), scroll),
				target -> finishCast(player, book, scroll, spell, runeDamage, castLevel, castMana, forcedLevel, target),
				duration -> {
					ItemStack current = SpellbookData.getScroll(book, slot);
					long end = player.level().getGameTime() + duration;
					boolean unchanged = ItemStack.matches(current, scroll);
					scroll.getOrCreateTag().putInt("RuneCdTotal",duration);
                    ScrollData.setCooldownEnd(scroll, end);
					if (unchanged) SpellbookData.setScroll(book, slot, scroll);
					SharedSpellCooldowns.record(player, spell, end);
				}, ()->{}, runeModifiers);
	}

	private static void finishCast(ServerPlayer player, ItemStack book, ItemStack scroll, Spell spell,
			float damage, int level, int manaCost, int forcedLevel, net.minecraft.world.phys.Vec3 target) {
		FormationElement spellElement = spell.getElement();
		// 鐠佹澘缍嶉懓妤勬憫閺冭泛鍩㈤敍姘冲殰閻掕泛娲栨径?7 缁夋帒娆㈡潻鐔剁矤濮濄倛鎹ｇ粻妤嬬礄閸愬秵顐奸弬鑺ョ《闁插秶鐤嗛敍?
		manaCost = github.com.gengyoubo.sscfa.neomagic.NeoLegacyCasting.actualCost(manaCost);
		// 娑撯偓濞嗏剝鏌﹀▔鏇＄箲鏉╂﹢顣╃粻妤嬬礄闂冭埖顔?B 鎼?.4閿涘绱伴張顒侇偧閼版鎽?鑴?0% 娑撹桨绗傞梽鎰剁礉婢舵艾鑴婃稉?AOE 閸忓彉闊?
		spell.ssc_addon$setRefundCastId(FormCastingStyle.openRefundBudget(player, manaCost));
		// 缂佸繘鐛欓張鍝勫煑閿涘潒xp_mode閿?026-09-15閿涘鈧柡鈧柨鐔€閺佺増瀵滈妴灞剧《閺堫垰鐔€绾偓閹存劖婀伴敍鍫濈唨绾偓閼版鎽戣劤缁涘楠囬崐宥囧芳閿涘?閺堝鏅ラ幎鈧懗鐣岀搼缁狙佲偓?
		// 閿涘牓妯佸▓?C 鐎靛綊缍堢拋鈥冲灊娑?鎼?6閿涙矮绗夐悽銊︽付缂佸牐鈧鎽戦敍宀勬Щ閸棗濮為懓妤勬憫濞夋洟妯€閸欐ɑ鍨氶崚椋庨獓鐟佸懎顦敍澶涚礉
		// 閿涘牐鎽戦懝鎻掓惂鐠愩劋鑵戞担?6.0 exp/濞嗏槄绱氶敍灞藉晙娑旀绮℃灞剧《闂冪绱欓柅姘辨暏缁?exp 閸欐ü缍嬮敍澶娾偓宥囧芳閿涙碍鐦＄痪?+10%閿涘湢v5=鑴?.5閿涘绱?
		//   0 = 闁插﹥鏂侀崡鍐茬繁閸忋劑顤傞敍? = 閸涙垝鑵戦幍宥呯繁閿涘牓鍣撮弨鐐 0閿涘苯鍙忔０婵囧瘯鐠у嚖绱氶敍? = 闁插﹥鏂佸?20%閵嗕礁鎳℃稉顓∷?80%閵?
		// 閹稿倽鎹ｉ柈銊ュ瀻缂?Spell 濡椼儱婀?cast 鐠嬪啰鏁ら崜宥堫棅閸忋儻绱板鐟扮殸閻椻晜纭堕張顖氭躬 cast 閸愬懎褰囩挧鏉跨摠鏉╂稑鐤勬担鎿勭礄NBT 閹镐椒绠欓崠鏍电礉
		// 閸涙垝鑵戠紒鎾剁暬閺冭泛褰傞弨鎾呯礆閿涙睔OE 濞夋洘婀抽崷?damage 閹存劕濮涢崥搴″絿鐠ф澘褰傞弨鎾呯礄妫ｆ牜娲伴弽鍥у絿閸忋劑顤傞妴浣告倵缂侇厼褰?0閿涘苯銇夐悞璺虹畵缁涘绱氶妴?
		int immediateTen = prepareExperience(spell, level, book);
		// 缂佺喍绔撮崶娑樺棘閸忋儱褰涢敍姘《閺堫垰鍞撮柈銊ㄥ殰鐞涘苯鍠呯€规碍妲搁崥锔藉瘻缁涘楠囩紓鈺傛杹闁喎瀹?婢舵牞顫?閼煎啫娲块敍鍫熸￥ instanceof 閻楃懓鍨介敍?
		try {
			spell.castAtTarget(player, github.com.gengyoubo.sscfa.neomagic.NeoLegacyCasting.power(damage), false, level, scroll, target);
		} finally {
			spell.ssc_addon$clearPendingExp();
			spell.ssc_addon$clearRefundCastId();
		}
		// 妞嬪海浼掗妴宀勵棑鐞涘被鈧秶鐡戦弬鑺ユ杹閸楀疇绻戠猾缁樼ウ濞叉拝绱欐稉宥堫洣濮瑰倸鎳℃稉顓ㄧ礆
		if(github.com.gengyoubo.sscfa.neomagic.NeoLegacyCasting.missed())return;
		FormCastingStyle.onSpellCast(player, spellElement, manaCost);
		// 闂勫秵銆傞弬鑺ユ杹閸涘﹦鐓￠敍鍫滃閺冭埖銆傛稉宥呭晸閸楃柉閰遍敍宀€甯虹€瑰爼娓剁憰浣虹叀闁挻婀板▎鈩冨瘻娴滃棗鍤戝锝忕礆
		if (forcedLevel > 0) {
			player.displayClientMessage(Component.translatable("message.ssc_addon.spell.downgrade_cast",
					Component.translatable(spell.getNameKey()), forcedLevel), true);
		}
		PlayerSpellMastery.grant(player, book, immediateTen);
		if (SpellNumbers.isAtFloor(spell, level, ScrollData.getDurabilityRatio(scroll),
				FormationData.sumCooldownMultiplier(book, spellElement), FormAffinity.cooldownMultiplier(player, spellElement))) {
			player.displayClientMessage(Component.translatable("message.ssc_addon.spell.cd_floor")
					.withStyle(ChatFormatting.GRAY), true);
		}
	}

    /** 鐠鸿櫕娼冪悮顐㈠З閿涙矮鍔熼崘鍛斧閸旂姵鍨氭穱婵堟殌閿涙稓娅ч懝鎻掑幑鎼存洜鏁ら搹姘珯閸楃柉閰遍敍灞肩瑝閻㈢喐鍨?濞戝牐鈧澧块崫渚婄礉娑旂喍绗夊〒鍛存珟瀹稿弶婀丆D閵?*/

    /** 跳杀被动（占位）：依赖月织蛛能力层，待移植后恢复。 */
    public static boolean triggerSpiderCurse(ServerPlayer player, int slot, Spell fallback, net.minecraft.world.entity.LivingEntity victim) { return false; }

	/** 閻欘剛鐝涢崡鐤叡閸滃奔鍔熼崘鍛《閺堫垰鍙￠悽銊х病妤犲矂顣╃粻妤嬬幢閸涙垝鑵戠紒蹇涚崣娴犲秶鏁遍崢鐔风杽娴ｆ挻藟娑撯偓濞嗏剝鈧呯波缁犳ぜ鈧?*/
	public static void castSoloEffect(ServerPlayer player, Spell spell, ItemStack scroll, int level,
			float damage, net.minecraft.world.phys.Vec3 target) {
		int immediateTen=prepareExperience(spell,level,null);
		try { spell.castAtTarget(player,github.com.gengyoubo.sscfa.neomagic.NeoLegacyCasting.power(damage),true,level,scroll,target); }
		finally { spell.ssc_addon$clearPendingExp(); }
		if(!github.com.gengyoubo.sscfa.neomagic.NeoLegacyCasting.missed())PlayerSpellMastery.grant(player,null,immediateTen);
	}

	private static int prepareExperience(Spell spell,int level,ItemStack book) {
		int expMode = spell.getExpMode();
		int baseExpTen = Math.round(spell.getManaCost()
				* spell.getConfig().manaCostMultiplier(level)) * level;
		int bestExpFormation = book == null ? 0 : FormationData.getBestUniversalVariantLevel(book, FormationData.VARIANT_EXP);
		if (bestExpFormation > 0) {
			baseExpTen = Math.round(baseExpTen * FormationData.universalExpMultiplier(bestExpFormation));
		}
		int pendingTen;
		switch (expMode) {
			case 1 -> pendingTen = baseExpTen;                // 閸涙垝鑵戦幍宥呯繁閿涙岸鍣撮弨鐐閸忋劑顤傞幐鍌濇崳
			case 2 -> pendingTen = baseExpTen * 8 / 10;       // 闁插﹥鏂?20% + 閸涙垝鑵戠悰?80%閿涘牊鏆ｉ弫鐗堝焻閺傤叏绱?
			default -> pendingTen = 0;                        // 闁插﹥鏂侀崡鍐茬繁閿涙碍妫ら幐鍌濇崳
		}
		spell.ssc_addon$setPendingExp(pendingTen);
		return baseExpTen-pendingTen;
	}

	/** 閺囧瓨鏌婅ぐ鎾冲闁鑵戝Σ鏂ょ礄鐎涙ü鍔?NBT閿涘本瀵旀稊鍛 + 閺堝秴濮熺粩顖欑閼疯揪绱氶妴?*/
	public static void setSelected(ServerPlayer player, int slot) {
		ItemStack book = getEquippedBook(player);
		if (book == null || book.isEmpty()) {
			return;
		}
		if (slot < 0 || slot >= SpellbookData.getSlotCount(book)) return;
		SpellChannelManager.cancelSelf(player);
		SpellbookData.setSelectedSlot(book, slot);
	}

	/**
	 * 鐠佸墽鐤嗛崡鐤叡閺傝姤鏂佸锝勭秴閿涘牅缍嗛梼鍫曗偓澶嬨€傞敍宀勬▉濞?C / 鐠佲€冲灊娑?鎼?.4閿涙稒婀囬崝锛勵伂閺夊啫鈻夐敍澶堚偓?
	 * castLevel=0 鐞涖劎銇氶柌宥囩枂娑撻缚绐￠梾蹇撳祹鏉炲鐡戠痪褝绱?-5 閻?{@link ScrollData#setCastLevel} 婢剁懓鍩?[0, 閸楃柉閰辩粵澶岄獓]閵?
	 * 閸氬嫮鐡戠痪褍鍙℃禍?CD閵嗕浇鈧鎽?娴笺倕顔?CD 閹稿銆傛担宥囩波缁犳绱欓弬鑺ョ《婢跺嫬鍑＄€圭偟骞囬敍澶涚幢閸愭瑥娲栨稊?NBT 缂佸繘銈伴崫浣告倱濮濄儱顓归幋椋庮伂閵?
	 */
	public static void setCastLevel(ServerPlayer player, int slot, int castLevel) {
		if (SpellChannelManager.isCasting(player)) return;
		ItemStack book = getEquippedBook(player);
		if (book == null || book.isEmpty() || slot < 0 || slot >= SpellbookData.getSlotCount(book)) {
			return;
		}
		ItemStack scroll = SpellbookData.getScroll(book, slot);
		Spell spell = ScrollData.getSpell(scroll);
		if (spell == null) {
			return;
		}
		ScrollData.setCastLevel(scroll, castLevel);
		SpellbookData.setScroll(book, slot, scroll);
		player.displayClientMessage(Component.translatable("message.ssc_addon.spell.cast_level_set",
				Component.translatable(spell.getNameKey()),
				ScrollData.getCastLevel(scroll)), true);
	}
}
