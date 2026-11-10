package github.com.gengyoubo.sscfa.item;

import net.onixary.shapeShifterCurseForge.items.accessory.AccessoryItem;
import github.com.gengyoubo.sscfa.util.FormUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class LifesavingCatTailItem extends AccessoryItem {

	public static final int MAX_COOLDOWN = 3600;

	public LifesavingCatTailItem(Properties settings) {
		super(settings);
	}

	public static void registerLootTable() {
		/* loot table registration omitted */
	}

	@Override
	public boolean canEquip(ItemStack stack, LivingEntity entity, AccessoryItem.SlotData slotData) {
		// 闁插海灏楅敍鍫熸箑閸忓鐡嶈ぐ鎲嬬礆娑撳酣顥ゅ锕傜摕閿涘湤ightmare閿涘娼庨崣顖欏兊閹寸繝绻氶崨鐣屽皸鐏忔拝绱欑悮顐㈠З鐎靛綊缍堥敍?
		return github.com.gengyoubo.sscfa.item.AddonAccessoryGuard.canEquip(entity,
				e -> FormUtils.isWildCatSP(e) || FormUtils.isForm(e, github.com.gengyoubo.sscfa.util.FormIdentifiers.WILD_CAT_NIGHTMARE));
	}

	/**
	 * 閼惧嘲褰囪ぐ鎾冲閸愬嘲宓堥崜鈺€缍憈icks (娴犲懎顓归幋椋庮伂閺堝鏅?閵?
	 * 闁俺绻?{@link FabricLoader} 閻滎垰顣ㄧ€瑰牆宕?+ {@link LifesavingCatTailClient} 闂呮梻顬囩猾浼欑礉
	 * 娣囨繆鐦夋稉鎾舵暏閺堝秴濮熺粩顖欑瑝娴兼俺袝閸?{@code MinecraftClient} 閻ㄥ嫮琚柧鐐复閵?
	 */
	private int getCooldownRemaining() {
		if (net.minecraftforge.fml.loading.FMLEnvironment.dist != net.minecraftforge.api.distmarker.Dist.CLIENT) return 0;
		return LifesavingCatTailClient.getCooldownRemaining(this, MAX_COOLDOWN);
	}

	/**
	 * 閸掋倖鏌囬弰顖氭儊閸︺劌鍠庨崡缈犺厬 (娴犲懎顓归幋椋庮伂閺堝鏅?閵?
	 */
	private boolean isOnCooldown() {
		if (net.minecraftforge.fml.loading.FMLEnvironment.dist != net.minecraftforge.api.distmarker.Dist.CLIENT) return false;
		return LifesavingCatTailClient.isOnCooldown(this);
	}

	/**
	 * 閺嶈宓丆D鏉╂稑瀹抽懢宄板絿妫版粏澹?(缁俱垼澹?>濮楁瑨澹?>缂佽儻澹?
	 * progress: 0.0 = 閸掓艾绱戞慨瀣枎閸? 1.0 = 閸愬嘲宓堢€瑰本鍨?
	 */
	private int getColorForProgress(float progress) {
		if (progress < 0.33f) {
			// 缁俱垼澹婇崠鍝勭厵 (0% - 33%)
			return 0xFF5555; // 缁俱垼澹?
		} else if (progress < 0.66f) {
			// 濮楁瑨澹婇崠鍝勭厵 (33% - 66%)
			return 0xFFAA00; // 濮楁瑨澹?
		} else {
			// 缂佽儻澹婇崠鍝勭厵 (66% - 100%)
			return 0x55FF55; // 缂佽儻澹?
		}
	}

	/**
	 * 閺嶈宓丆D鏉╂稑瀹抽懢宄板絿Formatting妫版粏澹?
	 */
	private ChatFormatting getFormattingForProgress(float progress) {
		if (progress < 0.33f) {
			return ChatFormatting.RED;
		} else if (progress < 0.66f) {
			return ChatFormatting.GOLD;
		} else {
			return ChatFormatting.GREEN;
		}
	}

	@Override
	public boolean isEnchantable(ItemStack stack) {
		return false;
	}

	@Override
	public boolean isBarVisible(ItemStack stack) {
		// 閸欘亝婀侀崷銊ュ枎閸楃繝鑵戦幍宥嗘▔缁€楦跨箻鎼达附娼?
		return isOnCooldown();
	}

	@Override
	public int getBarWidth(ItemStack stack) {
		// 鏉╂柨娲?-13閻ㄥ嫬鈧》绱濈悰銊с仛鏉╂稑瀹抽弶锛勬畱闂€鍨
		// 閸愬嘲宓堥崜鈺€缍戠搾濠傜毌閿涘矁绻樻惔锔芥蒋鐡掑﹪鏆?
		int remaining = getCooldownRemaining();
		float progress = 1.0f - ((float) remaining / MAX_COOLDOWN);
		return Math.round(13.0f * progress);
	}

	@Override
	public int getBarColor(ItemStack stack) {
		int remaining = getCooldownRemaining();
		float progress = 1.0f - ((float) remaining / MAX_COOLDOWN);
		return getColorForProgress(progress);
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context) {
		// "娣囨繂鎳￠悮顐㈢啲"
		// 鐟佸懎顦崚鎷屽彏鐢附鐖悽鐔告櫏閿涘本妫ゅ▔鏇氭叏鐞涖儯鈧礁褰旈崝鐘偓渚€妾鏂烩偓浣锋唉閺?
		// 閸︹暞p闁插海灏楅崣妤€鍩岄懛鏉戞嚒娴笺倕顔婇弮璺哄帳閻ゎ偂绔村▎鈩冾劥娴?..
		// tooltip.add(Text.translatable("item.ssc_addon.lifesaving_cat_tail.tooltip.1").formatted(Formatting.GRAY));
		tooltip.add(Component.translatable("item.ssc_addon.lifesaving_cat_tail.tooltip.2").withStyle(ChatFormatting.BLUE));

		// 閺勫墽銇欳D閻樿埖鈧?
		if (isOnCooldown()) {
			int remainingTicks = getCooldownRemaining();
			int remainingSeconds = remainingTicks / 20;
			float progress = 1.0f - ((float) remainingTicks / MAX_COOLDOWN);
			ChatFormatting color = getFormattingForProgress(progress);
			tooltip.add(Component.translatable("item.ssc_addon.lifesaving_cat_tail.cooldown", remainingSeconds).withStyle(color));
		} else {
			tooltip.add(Component.translatable("item.ssc_addon.lifesaving_cat_tail.ready").withStyle(ChatFormatting.GREEN));
		}

		tooltip.add(Component.translatable("item.ssc_addon.lifesaving_cat_tail.tooltip.exclusive").withStyle(ChatFormatting.LIGHT_PURPLE));
		super.appendHoverText(stack, world, tooltip, context);
	}
}
