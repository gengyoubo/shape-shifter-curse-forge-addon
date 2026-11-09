package github.com.gengyoubo.sscfa.event;

import github.com.gengyoubo.sscfa.SscAddonForge;
import github.com.gengyoubo.sscfa.init.AddonEffects;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 变羊期间的行为封锁（服务端权威兜底，双端都会拦）：中招玩家无法对方块右键（放置方块、开门、用物品都不行）? * 无法破坏方块、无法攻击实体? *
 * <p>其余防线：{@code sheep_form} 效果把攻击伤害乘 0（属性层）；客户端输入层拦截与生?AI 停机原先?mixin 实现? * 随变羊能力一起迁移?/p>
 */
@Mod.EventBusSubscriber(modid = SscAddonForge.MOD_ID)
public final class SheepFormGuard {
	private SheepFormGuard() {}

	private static boolean isSheep(Player player) {
		return player.hasEffect(AddonEffects.SHEEP_FORM.get());
	}

	@SubscribeEvent
	public static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
		if (isSheep(event.getEntity())) {
			event.setCanceled(true);
			event.setCancellationResult(InteractionResult.FAIL);
		}
	}

	@SubscribeEvent
	public static void onAttackBlock(PlayerInteractEvent.LeftClickBlock event) {
		if (isSheep(event.getEntity())) event.setCanceled(true);
	}

	@SubscribeEvent
	public static void onAttackEntity(AttackEntityEvent event) {
		if (isSheep(event.getEntity())) event.setCanceled(true);
	}

	/** 空手/非方块物品是否仍可使用（客户端预检用；当前设计：变羊期间全部禁止）?*/
	public static boolean canUseItem(LivingEntity entity, ItemStack stack) {
		return !entity.hasEffect(AddonEffects.SHEEP_FORM.get()) && !(stack.getItem() instanceof BlockItem);
	}

	/** 装备槽位占位检查（预留：变羊期间主副手只读）?*/
	public static boolean isSlotLocked(EquipmentSlot slot) {
		return slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND;
	}
}
