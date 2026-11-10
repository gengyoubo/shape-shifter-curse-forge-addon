package github.com.gengyoubo.sscfa.item;

import github.com.gengyoubo.sscfa.spell.ScrollData;
import github.com.gengyoubo.sscfa.spell.research.*;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import github.com.gengyoubo.sscfa.spell.ArcaneAnalysis;
import github.com.gengyoubo.sscfa.spell.Spell;
import github.com.gengyoubo.sscfa.spell.SpellRarity;
import github.com.gengyoubo.sscfa.spell.SpellChannelManager;
import github.com.gengyoubo.sscfa.spell.SpellCastingRules;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * 榄旀硶鍗疯酱锛坖ackcooper锛夈€備竴涓€氱敤鐗╁搧锛岄€氳繃 NBT锛坽@link ScrollData}锛夌粦瀹氬叿浣撻瓟娉曚笌绋€鏈夊害銆?
 *
 * <p><b>鍗曠嫭浣跨敤</b>锛堢洿鎺ュ彸閿級锛氭寜鍥哄畾瑁哥敤鎯╃綒閲婃斁锛堜激瀹?脳0.5銆佸喎鍗?脳2锛夛紝姣忔娑堣€?1 娆℃暟锛岃€楀敖閿€姣侊紱
 * 绾㈣壊鍗疯酱绂佹鍗曠嫭浣跨敤銆?b>鏀惧叆榄旀硶涔?/b>锛氭棤娆℃暟闄愬埗銆佸彧鑰椾功娉曞姏銆佹晥鏋滄寜鍓╀綑娆℃暟姣斾緥缂╂斁锛堣榄旀硶涔﹂€昏緫锛夈€?/p>
 */
public class MagicScrollItem extends Item {

	public MagicScrollItem(Properties settings) {
		super(settings);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
		ItemStack stack = user.getItemInHand(hand);
		if (github.com.gengyoubo.sscfa.util.AbilityUseGuard.blocked(user)) return InteractionResultHolder.fail(stack);
		if (ArcaneAnalysis.isUnanalyzed(stack)) {
			if (!world.isClientSide) user.displayClientMessage(Component.translatable("message.ssc_addon.analysis.required").withStyle(ChatFormatting.RED), true);
			return InteractionResultHolder.fail(stack);
		}
		Spell spell = ScrollData.getSpell(stack);
		if (spell == null) {
			return InteractionResultHolder.pass(stack);
		}
		if (user.isShiftKeyDown()) {
			if (!world.isClientSide && user instanceof ServerPlayer sp) {
				sp.displayClientMessage(Component.translatable("research.ssc_addon.slotted.analyze_scroll"), true);
			}
			return InteractionResultHolder.success(stack);
		}
		// 绾㈣壊鍗疯酱涓嶅彲鍗曠嫭浣跨敤锛堟寜绛夌骇瀵瑰簲鐨勬湁鏁堝搧璐ㄥ垽瀹氾級
		SpellRarity effectiveRarity = spell.getRarity(ScrollData.getLevel(stack));
		if (!effectiveRarity.canUseSolo()) {
			if (user instanceof ServerPlayer player) SpellChannelManager.playFailureSound(player);
			if (world.isClientSide) {
				user.displayClientMessage(Component.translatable("message.ssc_addon.scroll.cannot_solo").withStyle(ChatFormatting.RED), true);
			}
			return InteractionResultHolder.fail(stack);
		}
		// 鍗曠嫭浣跨敤鍐峰嵈涓紙鍗疯酱 NBT + 鐜╁鍏变韩琛ㄥ弻婧愬垽瀹氾紝闃舵 B 搂15.2锛?
		if (world.getGameTime() < github.com.gengyoubo.sscfa.spell.SharedSpellCooldowns.getEffectiveCooldownEnd(user, stack)) {
			if (user instanceof ServerPlayer player) SpellChannelManager.playFailureSound(player);
			return InteractionResultHolder.fail(stack);
		}
		if (ScrollData.getUses(stack) <= 0) {
			if (user instanceof ServerPlayer player) SpellChannelManager.playFailureSound(player);
			return InteractionResultHolder.fail(stack);
		}
		if (!world.isClientSide && user instanceof ServerPlayer sp) {
			if (SpellChannelManager.isCasting(sp)||!RuneScheme.validScroll(sp,stack,spell)) return InteractionResultHolder.fail(stack);
            int level = ScrollData.getLevel(stack);
            RuneModifiers runeModifiers=RuneScheme.modifiers(sp,stack,level);
			if (spell.getCastingMode() == SpellCastingRules.Mode.AUTOMATIC && !RuneCastContext.with(runeModifiers,()->spell.canCast(sp))) {
				SpellChannelManager.playFailureSound(sp);
				sp.displayClientMessage(spell.getCastFailureMessage(sp), true);
				return InteractionResultHolder.fail(stack);
			}
			if (!spell.prepareScroll(sp, stack)) {
				SpellChannelManager.playFailureSound(sp);
				return InteractionResultHolder.fail(stack);
			}
			float damage = runeModifiers.power(spell.getBaseDamage() * spell.getSoloDamageMultiplier() * spell.getDamageMultiplier(level),RuneCapabilities.of(spell.getId().getPath()).contains(RuneModifiers.Stat.DAMAGE),RuneCapabilities.of(spell.getId().getPath()).contains(RuneModifiers.Stat.HEAL));
			// 闃舵 B锛埪?.2锛夛細缁熶竴鍐峰嵈鍏紡锛坰olo 鎯╃綒鍊嶇巼骞跺叆绛夌骇鍩哄噯锛涘弻灞備笅闄愪笌涔﹀唴涓€鑷达級
			int cd = runeModifiers.cooldown(github.com.gengyoubo.sscfa.spell.SpellNumbers.finalSoloCooldownTicks(spell, level));
			ItemStack snapshot = stack.copy();
			boolean started = SpellChannelManager.start(sp, spell, snapshot, level, true, hand.ordinal(), 0, cd,
					() -> sp.getItemInHand(hand) == stack && ItemStack.matches(stack, snapshot),
					target -> github.com.gengyoubo.sscfa.spell.SpellCastManager.castSoloEffect(sp, spell, snapshot, level, damage, target),
					duration -> {
						long end = sp.level().getGameTime() + duration;
						stack.getOrCreateTag().putInt("RuneCdTotal",duration);
                    ScrollData.setCooldownEnd(stack, end);
						github.com.gengyoubo.sscfa.spell.SharedSpellCooldowns.record(sp, spell, end);
					},
					() -> { if (ScrollData.consumeSoloUse(stack)) stack.shrink(1); }, runeModifiers);
			if (!started) return InteractionResultHolder.fail(stack);
		}
		user.startUsingItem(hand);
		return InteractionResultHolder.consume(stack);
	}

	@Override
	public int getUseDuration(ItemStack stack) {
		return 72000;
	}

	@Override
	public void releaseUsing(ItemStack stack, Level world, net.minecraft.world.entity.LivingEntity user, int remainingUseTicks) {
		if (user instanceof ServerPlayer player) {
			SpellChannelManager.release(player, player.getUsedItemHand().ordinal(), true);
		}
	}

	@Override
	public boolean isBarVisible(ItemStack stack) {
		int max = ScrollData.getMaxUses(stack);
		return max > 0 && ScrollData.getUses(stack) < max;
	}

	@Override
	public int getBarWidth(ItemStack stack) {
		int max = ScrollData.getMaxUses(stack);
		if (max <= 0) {
			return 0;
		}
		return Math.round(13.0f * ScrollData.getUses(stack) / max);
	}

	@Override
	public int getBarColor(ItemStack stack) {
		Spell spell = ScrollData.getSpell(stack);
		if (spell == null) {
			return 0xFFFFFF;
		}
		Integer cv = spell.getRarity(ScrollData.getLevel(stack)).color.getColor();
		return cv == null ? 0xFFFFFF : cv;
	}

	@Override
	public Component getName(ItemStack stack) {
		if (ArcaneAnalysis.isUnanalyzed(stack)) return Component.translatable("item.ssc_addon.unfamiliar_spell");
		Spell spell = ScrollData.getSpell(stack);
		if (spell == null) {
			return super.getName(stack);
		}
		// 妯℃澘绗竴琛岋細銆屾硶鏈悕 鍗疯酱 路 绯诲悕绯汇€嶏紙鏃犵郴鍒殑娉曟湳鍥為€€涓嶅甫绯诲悕鐨勬棫鏍煎紡锛?
		var element = spell.getElement();
		net.minecraft.network.chat.MutableComponent name = element == null
				? Component.translatable("item.ssc_addon.magic_scroll.format", Component.translatable(spell.getNameKey()))
				: Component.translatable("item.ssc_addon.magic_scroll.format_element",
						Component.translatable(spell.getNameKey()), Component.translatable(element.getNameKey()));
		if (RuneScheme.isModified(stack)) {
			name.append(Component.translatable("research.ssc_addon.runes.modified_suffix"));
		}
		return name.withStyle(spell.getRarity(ScrollData.getLevel(stack)).color);
	}

	@Override
	public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
		if (ArcaneAnalysis.isUnanalyzed(stack)) return Optional.empty();
		Spell spell = ScrollData.getSpell(stack);
		return spell == null
				? Optional.empty()
				: Optional.of(new SpellIconTooltipData(spell.getIconTexture()));
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context) {
		if (ArcaneAnalysis.isUnanalyzed(stack)) {
			tooltip.add(Component.translatable("message.ssc_addon.analysis.required").withStyle(ChatFormatting.GRAY));
			return;
		}
		Spell spell = ScrollData.getSpell(stack);
		if (spell == null) {
			tooltip.add(Component.translatable("item.ssc_addon.magic_scroll.tip_empty").withStyle(ChatFormatting.DARK_GRAY));
			return;
		}
		// 妯℃澘绗簩琛岋細鍝佽川 锝?妗ｄ綅锛堝搧璐ㄨ壊闅忕瓑绾х櫧鈫掓锛沜ustom 妗ｆ樉绀恒€岀壒娈娿€嶏級
		SpellRarity r = spell.getRarity(ScrollData.getLevel(stack));
		var tier = spell.getConfig().spellTier;
		tooltip.add(Component.translatable("item.ssc_addon.magic_scroll.rarity_tier",
				Component.translatable(r.getTranslationKey()).withStyle(r.color),
				Component.translatable("spell.ssc_addon.tier." + tier.name().toLowerCase(java.util.Locale.ROOT)).withStyle(ChatFormatting.GRAY)));
		// 妯℃澘绗笁琛岋細涓昏鏁堟灉 + 鏁堟灉闄愬埗锛坙ang 鍐?\n 鎵嬪姩鎹㈣锛屼腑鏂囨瘡琛?鈮?0 瀛楋級
		for (String line : Component.translatable(spell.getDescKey()).getString().split("\n")) {
			tooltip.add(Component.literal(line).withStyle(ChatFormatting.GRAY));
		}
		// 绾㈣壊鍗疯酱鐨勪娇鐢ㄩ檺鍒讹紙蹇呰鏁堟灉闄愬埗锛屼繚鐣欑孩瀛楁彁绀猴級
		RuneTooltips.append(stack,tooltip);
        if (!r.canUseSolo()) {
			tooltip.add(Component.translatable("item.ssc_addon.magic_scroll.tip_no_solo").withStyle(ChatFormatting.RED));
		}
	}

	public record SpellIconTooltipData(ResourceLocation texture) implements TooltipComponent {
	}
}
