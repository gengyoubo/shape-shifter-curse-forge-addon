package github.com.gengyoubo.sscfa.item;

import net.onixary.shapeShifterCurseForge.items.accessory.AccessoryItem;
import github.com.gengyoubo.sscfa.util.FormUtils;
import github.com.gengyoubo.sscfa.util.TrinketUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 安卡纹???- SP阿努比斯之狼专属饰品（戒指槽?
 * 复活被动触发时：消除凋零和虚弱效果、减???0%冷却、物品消耗并播放碎裂音效
 */
public class AnkhStoneItem extends AccessoryItem {

	public AnkhStoneItem(Properties settings) {
		super(settings);
	}

	/**
	 * 复活触发后的安卡纹石效果处理，??AnkhStoneTotemMixin 调??
	 */
	public static void onRevival(LivingEntity entity) {
		if (!(entity instanceof ServerPlayer player)) return;
		if (!FormUtils.isAnubisWolfSP(player)) return;

		// 检查是否装备了安卡纹石（框架无关：自动适??Trinkets/Curios??
		ItemStack ankhStack = TrinketUtils.findFirstEquipped(player,
				stack -> stack.getItem() == github.com.gengyoubo.sscfa.init.AddonItems.ANKH_STONE.get());
		if (ankhStack == null) return;

		// 消除凋零和虚弱效果（保留火焰抗性??
		player.removeEffect(MobEffects.WITHER);
		player.removeEffect(MobEffects.WEAKNESS);

		// 消耗安卡纹石（只消耗第一个）
		ankhStack.shrink(1);

		// 播放物品碎裂音???
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.ITEM_BREAK, player.getSoundSource(), 1.0f, 1.0f);
	}

	/**
	 * 注册安卡纹石到沙漠神殿战利品表??5%概率?-2个??
	 */
	public static void registerLootTable() {}

	@Override
	public boolean canEquip(ItemStack stack, LivingEntity entity, AccessoryItem.SlotData slotData) {
		return github.com.gengyoubo.sscfa.item.AddonAccessoryGuard.canEquip(entity, FormUtils::isAnubisWolfSP);
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context) {
		tooltip.add(Component.translatable("item.ssc_addon.ankh_stone.tooltip_1").withStyle(ChatFormatting.LIGHT_PURPLE));
		tooltip.add(Component.translatable("item.ssc_addon.ankh_stone.tooltip_2").withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.translatable("item.ssc_addon.ankh_stone.tooltip_3").withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.translatable("item.ssc_addon.ankh_stone.tooltip_4").withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.translatable("item.ssc_addon.ankh_stone.tooltip_5").withStyle(ChatFormatting.RED));
		super.appendHoverText(stack, world, tooltip, context);
	}
}
