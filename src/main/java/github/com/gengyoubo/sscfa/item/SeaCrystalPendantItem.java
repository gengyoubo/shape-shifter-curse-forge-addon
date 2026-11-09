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
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 海晶荧光坠：荧光幼灵形态专属项链?
 * 装备后强化两个主动技能——潮汐球变一次性爆炸球（加速、范围伤害与减速）?
 * 法阵激光变缩范围三连发（施法期仅半速而非完全定身）?
 * 具体效果?TidalOrbEntity / LaserBeamEntity / 两个 Manager 里通过
 * TrinketUtils.isWearing(owner, github.com.gengyoubo.sscfa.init.AddonItems.SEA_CRYSTAL_PENDANT.get()) 判定生效?
 *
 * <p>饰品后端无关：与附属其余饰品一致继?SSC ?{@link AccessoryItem} 抽象?
 * （Trinkets / Curios 桥接自动适配），不直接依?Trinkets 类——附属对 trinkets 为弱依赖?
 */
public class SeaCrystalPendantItem extends AccessoryItem {
	public SeaCrystalPendantItem(Properties settings) {
		super(settings);
	}

	// 沉船宝藏?+ 藏宝图（埋藏的宝藏）箱，均以 15% 概率掉落；本饰品无法合成，仅此两种途径获得
	private static final ResourceLocation SHIPWRECK_TREASURE_LOOT = new ResourceLocation("minecraft", "chests/shipwreck_treasure");
	private static final ResourceLocation BURIED_TREASURE_LOOT = new ResourceLocation("minecraft", "chests/buried_treasure");

	/** 注册海晶荧光坠到沉船宝藏?+ 藏宝图宝藏箱战利品表（各 15% 概率）；本饰品无法合成，仅此两种途径获得?*/
	public static void registerLootTable() {}

	@Override
	public boolean canEquip(ItemStack stack, LivingEntity entity, AccessoryItem.SlotData slotData) {
		// 专属限制：只有荧光幼灵（含阿澪）能装备?
		// 登录装载瞬间（age==0）宽容放行（防重开世界?Curios/Trinkets 校验吐出），
		// 形态不符的?AddonAccessoryGuard.tick 全局兜底自动卸下归还?
		return AddonAccessoryGuard.canEquip(entity, FormUtils::isAxolotlFluorescent);
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context) {
		tooltip.add(Component.translatable("item.ssc_addon.sea_crystal_pendant.desc").withStyle(ChatFormatting.BLUE));
		tooltip.add(Component.translatable("item.ssc_addon.sea_crystal_pendant.tooltip.exclusive").withStyle(ChatFormatting.LIGHT_PURPLE));
	}
}
