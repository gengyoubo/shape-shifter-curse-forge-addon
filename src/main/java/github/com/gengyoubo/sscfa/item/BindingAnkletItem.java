package github.com.gengyoubo.sscfa.item;

import net.onixary.shapeShifterCurseForge.items.accessory.AccessoryItem;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.FormUtils;
import github.com.gengyoubo.sscfa.util.TrinketUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.monster.Evoker;
import net.minecraft.world.entity.monster.Illusioner;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 绑定脚环（Binding Anklet）—?契灵首个专属饰品?
 *
 * 槽位：复?SSC 守御脚环?trinkets:feet/aglet 槽（与守御脚环互斥）?
 * 装备限制：仅契灵形态可装备，其他形态拒绝?
 * 被动效果：在 16 格范围内为其?*劫掠阵营 NPC**（pillager / vindicator / evoker /
 *           illusioner / ravager / witch）提?+20% 造成伤害加成?
 *           佩戴者本人（玩家/契灵）不享受此加成?
 * 获取途径：仅 25% 概率出现在劫掠者哨塔战利品箱中?
 *
 * 加成的伤害侧?{@link github.com.gengyoubo.sscfa.mixin.entity.BindingAnkletAuraMixin}
 * ?LivingEntity#damage 入口 ModifyVariable，调用本类静态方法判定?
 */
public class BindingAnkletItem extends AccessoryItem {

	/** 灵气范围（格?*/
	public static final double AURA_RADIUS = 16.0D;
	/** 加成倍数 */
	public static final float DAMAGE_MULTIPLIER = 1.20F;

	public BindingAnkletItem(Properties settings) {
		super(settings);
	}

	/* ------------------------------------------------------------ */
	/*  装备限制                                                       */
	/* ------------------------------------------------------------ */

	@Override
	public boolean canEquip(ItemStack stack, LivingEntity entity, AccessoryItem.SlotData slotData) {
		// 仅契灵形态可装备；登录装载瞬间（age==0）宽容放行，?AddonAccessoryGuard.tick 兜底卸下
		return github.com.gengyoubo.sscfa.item.AddonAccessoryGuard.canEquip(entity,
				e -> FormUtils.isForm(e, FormIdentifiers.FAMILIAR_FOX_MANCIANIMA));
	}

	/* ------------------------------------------------------------ */
	/*  灵气加成判定（供 Mixin 调用?                                   */
	/* ------------------------------------------------------------ */

	/**
	 * 判定 attacker 是否属于劫掠阵营 NPC（不含玩家、不含玩家的召唤?/ 宠物）?
	 */
	public static boolean isRaiderFaction(LivingEntity attacker) {
		if (attacker instanceof Player) return false;
		if (attacker instanceof Raider) return true; // ?pillager / vindicator / evoker / illusioner / ravager / witch（注：witch ?1.20 也是 RaiderEntity 子类?
		// 兜底：直接列举（避免某些 mod 替换继承链时漏判?
		return attacker instanceof Pillager
				|| attacker instanceof Vindicator
				|| attacker instanceof Evoker
				|| attacker instanceof Illusioner
				|| attacker instanceof Witch
				|| attacker instanceof AbstractIllager;
	}

	/**
	 * 检?attacker 周围 AURA_RADIUS 格内是否存在佩戴绑定脚环的契灵玩家?
	 * 多人环境下：必须在服务器线程调用；佩戴者自身的攻击不在此函数判定（attacker
	 * 必为劫掠 NPC，玩家本人天然不满足 {@link #isRaiderFaction}）?
	 */
	public static boolean hasAnkletAuraNearby(LivingEntity attacker) {
		Level world = attacker.level();
		if (world.isClientSide) return false;
		AABB box = attacker.getBoundingBox().inflate(AURA_RADIUS);
		// getEntitiesByClass 已自?box 过滤，再补距离平方过滤保证球形范?
		double r2 = AURA_RADIUS * AURA_RADIUS;
		List<Player> players = world.getEntitiesOfClass(Player.class, box, p -> !p.isSpectator());
		for (Player p : players) {
			if (p.distanceToSqr(attacker) > r2) continue;
			// 必须是契灵形态（否则装备早就被拒绝，但热切换形态时双保险）
			if (!FormUtils.isForm(p, FormIdentifiers.FAMILIAR_FOX_MANCIANIMA)) continue;
			if (isAnkletEquipped(p)) return true;
		}
		return false;
	}

	private static boolean isAnkletEquipped(Player player) {
		return TrinketUtils.isWearing(player, github.com.gengyoubo.sscfa.init.AddonItems.BINDING_ANKLET.get());
	}

	/* ------------------------------------------------------------ */
	/*  战利品注入：劫掠者哨?25%                                       */
	/* ------------------------------------------------------------ */

	private static final ResourceLocation PILLAGER_OUTPOST_LOOT = new ResourceLocation("minecraft", "chests/pillager_outpost");

	public static void registerLootTable() {}

	/* ------------------------------------------------------------ */
	/*  Tooltip                                                       */
	/* ------------------------------------------------------------ */

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context) {
		tooltip.add(Component.translatable("item.ssc_addon.binding_anklet.tooltip_1").withStyle(ChatFormatting.LIGHT_PURPLE));
		tooltip.add(Component.translatable("item.ssc_addon.binding_anklet.tooltip_2").withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.translatable("item.ssc_addon.binding_anklet.tooltip_3").withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.translatable("item.ssc_addon.binding_anklet.tooltip_4").withStyle(ChatFormatting.DARK_GRAY));
		super.appendHoverText(stack, world, tooltip, context);
	}
}
