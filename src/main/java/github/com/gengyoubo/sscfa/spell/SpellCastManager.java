package github.com.gengyoubo.sscfa.spell;

import github.com.gengyoubo.sscfa.spell.research.*;
import github.com.gengyoubo.sscfa.util.TrinketUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 月尘魔法书施法服务端核心（jackcooper）。服务端权威：验证佩戴魔法书、读书内卷轴、判冷却/法力?
 * 按卷轴耐久比缩放伤害与冷却（满次数=正常，用过越多越弱），执行魔法、写冷却、累积经验??
 */
public final class SpellCastManager {
	private SpellCastManager() {
	}

	/** 玩家当前佩戴的魔法书（未装备返回 null）??*/
	public static ItemStack getEquippedBook(ServerPlayer player) {
		return TrinketUtils.findFirstEquipped(player, s -> s.getItem() == github.com.gengyoubo.sscfa.init.AddonItems.MOON_DUST_SPELLBOOK.get());
	}

	/** 释放书内指定槽的魔法（正常档）?*/
	public static void cast(ServerPlayer player, int slot) {
		cast(player, slot, 0);
	}

	public static void cast(ServerPlayer player, int slot, int token) {
		castInternal(player, slot, 0, 1.0f, token);
	}

	/**
	 * 临时降档施放（法力不足三连击，用?2026-09-18 定稿）：自动降到付得起的最高档?
	 * 本??CD ??.2 惩罚；不写卷?NBT，下次施法仍按原档。全部档都付不????维持红字提示?
	 */
	public static void castDowngraded(ServerPlayer player, int slot) {
		castDowngraded(player, slot, 0);
	}

	public static void castDowngraded(ServerPlayer player, int slot, int token) {
		// C has no affordability gate; manual level selection remains available.
		castInternal(player, slot, 0, 1.0f, token);
	}

	/** 内部统一施法：forcedLevel>0 用指定档（临时降档），否则用卷轴档位；cdPenalty 为本?CD 惩罚倍率??*/
	private static void castInternal(ServerPlayer player, int slot, int forcedLevel, float cdPenalty, int token) {
		if (github.com.gengyoubo.sscfa.util.AbilityUseGuard.blocked(player)) return;
		if (SpellChannelManager.isCasting(player)) return;
		// 变羊闸门??026-09-29 用户定稿）：变成羊后禁止施法（羊蹄子翻不动魔法书）；
		// 不给提示??红字——与 STUN 沉默处理一致，防变羊期间刷提??
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
		// 换书稳定期（阶段 C ??5.2，已拍板）：战斗中换书后短期内禁止施法（防多书满蓝连用??
		if (FormCastingStyle.isSwapStabilizing(player)) {
			SpellChannelManager.playNoManaSound(player); // 换书稳定期拒绝：火焰熄灭??
			player.displayClientMessage(SpellCastFeedback.stabilizing(FormCastingStyle.getSwapReadyAt(player) - world.getGameTime()), true);
			return;
		}
		// 阶??B（?5.2）：卷轴 NBT 与玩家共享表双源判定——换?换??同法术第二张卷轴均不能绕 CD??
		// 被共享表拦截时把共享结束时刻回写卷轴 NBT（经饰品同步）：新换入的卷??HUD 遮罩也能正确显示
		if (ScrollData.isOnCooldown(scroll, world)
				|| SharedSpellCooldowns.isOnSharedCooldown(player, spell)) {
			SpellChannelManager.playNoManaSound(player); // CD 中施放：火焰熄灭??
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
		// 施放档位（低阶选档??.4；缺?卷轴档位；三连击降档?forcedLevel 指定临时档??
		int level = Math.max(1, Math.min(spell.getMaxLevel(), forcedLevel > 0 ? forcedLevel : ScrollData.getCastLevel(scroll)));
		// 法阵加成：耗蓝倍率（全魔法每级 +10%?? 形态亲和耗蓝乘区（使魔????.85?? 每级耗蓝倍??
		// （耗蓝按施放档位算——低阶施放省蓝；召唤亲和 +1 只加强施法效果，不推高耗蓝??
		RuneModifiers runeModifiers=RuneScheme.modifiers(player,scroll,level);
        int manaCost = SpellNumbers.finalManaCost(spell, book, player, level, scroll);


		// 施法前置校验（如陨火要求准星命中方块）：失败拒绝施法、不耗法??CD（仿契灵传送失败不消耗??
		if (spell.getCastingMode() == SpellCastingRules.Mode.AUTOMATIC && !RuneCastContext.with(runeModifiers,()->spell.canCast(player))) {
			SpellChannelManager.playFailureSound(player);
			player.displayClientMessage(spell.getCastFailureMessage(player).copy().withStyle(ChatFormatting.RED), true);
			return;
		}

		FormationElement spellElement = spell.getElement();
		float ratio = ScrollData.getDurabilityRatio(scroll);   // 1=满次? 越低惩罚越??
		// 形态亲和等级加成（荧光幼??阿澪：召唤??+1，上?5）——叠加在施放档位??
		// （耗蓝已按档位结算完毕；亲和只加强效果等级，不推高耗蓝，语义与旧版一致）
		level = FormAffinity.bonusSpellLevel(player, spellElement, level);
		// 法阵加成：同系伤 +12%/级、对立系?-12%/级；同??cd -5%/级；空间法阵只缩空间??cd
		// 形态亲和：伤??耗??CD 三乘区（与法阵独立叠加）
		float damage = spell.getBaseDamage() * ratio * spell.getDamageMultiplier(level)
				* FormationData.sumDamageMultiplier(book, spellElement)
				* FormAffinity.damageMultiplier(player, spellElement);
		// 阶??B（?.2）：统一冷却公式——多乘??+ 双层下限（绝??floor + 0.2×等级基准），
		// 与单独使??客户?HUD 共用同一实现（SpellNumbers??
		float formationCdMul = FormationData.sumCooldownMultiplier(book, spellElement);
		float affinityCdMul = FormAffinity.cooldownMultiplier(player, spellElement);
		damage=runeModifiers.power(damage,RuneCapabilities.of(spell.getId().getPath()).contains(RuneModifiers.Stat.DAMAGE),RuneCapabilities.of(spell.getId().getPath()).contains(RuneModifiers.Stat.HEAL));
        final float runeDamage=damage;
        int cd = runeModifiers.cooldown(SpellNumbers.finalCooldownTicks(spell, level, ratio, formationCdMul, affinityCdMul));
		if (cdPenalty != 1.0f) {
			cd = Math.round(cd * cdPenalty); // 三连击降档惩罚：本??CD ??.2（用户定稿??
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
		// 记录耗蓝时刻：自然回?7 秒延迟从此起算（再次施法重置?
		manaCost = github.com.gengyoubo.sscfa.neomagic.NeoLegacyCasting.actualCost(manaCost);
		// 一次施法返还预算（阶??B ??.4）：本次耗????0% 为上限，多弹?AOE 共??
		spell.ssc_addon$setRefundCastId(FormCastingStyle.openRefundBudget(player, manaCost));
		// 经验机制（exp_mode??026-09-15）——基数按「法术基础成本（基础耗蓝×等级倍率）?有效技能等级?
		// （阶?C 对齐计划????6：不用最终耗蓝，防堆加耗蓝法阵变成刷级装备），
		// （蓝色品质中?6.0 exp/次），再乘经验法阵（通用??exp 变体）倍率：每?+10%（Lv5=??.5）??
		//   0 = 释放即得全额? = 命中才得（释放时 0，全额挂起）? = 释放?20%、命中??80%??
		// 挂起部分??Spell 桥??cast 调用前装入：弹射物法术在 cast 内取走存进实体（NBT 持久化，
		// 命中结算时发放）；AOE 法术?damage 成功后取走发放（首目标取全额、后续??0，天然幂等）?
		int immediateTen = prepareExperience(spell, level, book);
		// 统一四参入口：法术内部自行决定是否按等级缩放速??外??范围（无 instanceof 特判?
		try {
			spell.castAtTarget(player, github.com.gengyoubo.sscfa.neomagic.NeoLegacyCasting.power(damage), false, level, scroll, target);
		} finally {
			spell.ssc_addon$clearPendingExp();
			spell.ssc_addon$clearRefundCastId();
		}
		// 风灵「风行」等施放即返类流派（不要求命中）
		if(github.com.gengyoubo.sscfa.neomagic.NeoLegacyCasting.missed())return;
		FormCastingStyle.onSpellCast(player, spellElement, manaCost);
		// 降档施放告知（临时档不写卷轴，玩家需要知道本次按了几档）
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

    /** 跳杀被动：书内原加成保留；白色兜底用虚拟卷轴，不生??消耗物品，也不清除已有CD??*/

    /** 跳杀被动（占位）：依赖月织蛛能力层，待移植后恢复。 */
    public static boolean triggerSpiderCurse(ServerPlayer player, int slot, Spell fallback, net.minecraft.world.entity.LivingEntity victim) { return false; }

	/** 独立卷轴和书内法术共用经验预算；命中经验仍由原实体桥一次性结算??*/
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
			case 1 -> pendingTen = baseExpTen;                // 命中才得：释放时全额挂起
			case 2 -> pendingTen = baseExpTen * 8 / 10;       // 释??20% + 命中?80%（整数截断??
			default -> pendingTen = 0;                        // 释放即得：无挂起
		}
		spell.ssc_addon$setPendingExp(pendingTen);
		return baseExpTen-pendingTen;
	}

	/** 更新当前选中槽（存??NBT，持久化 + 服务端一致）?*/
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
	 * 设置卷轴施放档位（低阶选档，阶??C / 计划????.4；服务端权威）?
	 * castLevel=0 表示重置为跟随卷轴等级??-5 ??{@link ScrollData#setCastLevel} 夹??[0, 卷轴等级]??
	 * 各等级共?CD、耗??伤??CD 按档位结算（施法处已实现）；写回?NBT 经饰品同步客户端??
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
