package github.com.gengyoubo.sscfa.item;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.Level;
import github.com.gengyoubo.sscfa.effect.UniversalEnergyEffect;
import github.com.gengyoubo.sscfa.energy.EnergyRestoration;
import github.com.gengyoubo.sscfa.init.AddonEffects;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 通用能量药水（jackcooper）：饮用回复 25 点魔力，判定逻辑与 SSC 压缩能量药水（feed_potion）同源。
 *
 * <p>持有 SSCA 资源条（悦灵 mana / 蝙蝠血 / 阿努比斯灵魂 / 雪狐寒霜）的形态回复对应资源条
 * （恒 25）；寄生果蝠的种子条上限仅 10，固定回复 2 点；使魔 / 蜘蛛系 / 契灵（原版
 * ManaComponent 体系，契灵挂 familiar_fox_mana 的 mana_type_power）回复标准 mana 条——与
 * 压缩能量药水的 familiar_fox_mana 判定同源，但数值恒为 25 且不加饥饿（压缩能量药水饮用
 * 另 +8 饥饿 / +0.6 饱和）。朔望与无能量体系的形态（人类）饮用只消耗药水不回复，并动作栏提示。
 *
 * <p>实现参照项目内 {@code InfiniteEnergyPotionItem}：不继承 PotionItem（避免可堆叠类模组放开叠加），
 * finishUsing 服务端判定后回复，饮毕返还空玻璃瓶。
 * 由能量装瓶器产出（{@code EnergyBottlerBlockEntity.makeEnergyBottle}）；
 * 喷溅 / 滞留型由酿造台转换（饮用+火药→喷溅；喷溅+龙息→滞留，见 BrewingStandInfinitePotionMixin），
 * 投掷时生成携带 {@link UniversalEnergyEffect} 的原版投掷药水弹射物，落点 AOE 回复（距离衰减下限 0.5）。
 */
public class UniversalEnergyPotionItem extends Item {

	/** 回复的 mana 点数。 */
	public static final double MANA_RESTORE = 25.0;

	/** 三种瓶型。 */
	public enum Type {
		DRINK, SPLASH, LINGERING
	}

	/** 饮用读条时长，与原版药水一致（32 tick）。 */
	private static final int DRINK_TIME = 32;

	private final Type type;

	public UniversalEnergyPotionItem(Properties settings, Type type) {
		super(settings);
		this.type = type;
	}

	public Type getType() {
		return type;
	}

	/**
	 * 是否可从此药水受益（判定思路与压缩能量药水同源，不再依赖手动挂的标记 power）：
	 * 持有任意 SSCA 资源条（悦灵 mana / 蝙蝠血 / 阿努比斯灵魂 / 雪狐寒霜 / 果蝠种子），
	 * 或身上存在原版 mana 类型（ManaComponent 非空——使魔系 familiar_fox_mana /
	 * 蜘蛛系 spider_mana 的所有阶段自动覆盖，含原版使魔 2/3 阶、红使魔、进化使魔、
	 * SP 使魔、月织蛛、契灵等）。朔望（ocelot_nova）无能量体系，但切形态后
	 * ManaComponent 可能残留上一形态的 mana_type 导致误判，显式排除。
	 */
	public static boolean canRestore(LivingEntity entity) {
        return entity instanceof Player player && EnergyRestoration.canRestore(player);
    }

    public static void restoreManaScaled(net.minecraft.server.level.ServerPlayer player, double scale) {
        EnergyRestoration.restore(player, scale);
    }

	private static void restoreMana(net.minecraft.server.level.ServerPlayer player) {
		restoreManaScaled(player, 1.0);
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity user) {
		if (!world.isClientSide && user instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
			if (canRestore(user)) {
				// 与压缩能量药水一致：仅回复，无额外完成音效（饮用音由 UseAction.DRINK 原生提供）
				restoreMana(serverPlayer);
			} else {
				// 无门控 power：不回复，动作栏提示（走 lang key）
				serverPlayer.displayClientMessage(Component.translatable("message.ssc_addon.universal_potion.no_effect"), true);
			}
		}
		if (user instanceof Player playerEntity) {
			playerEntity.awardStat(Stats.ITEM_USED.get(this));
			if (!playerEntity.getAbilities().instabuild) {
				stack.shrink(1);
				ItemStack glassBottle = new ItemStack(Items.GLASS_BOTTLE);
				if (!playerEntity.getInventory().add(glassBottle)) {
					playerEntity.drop(glassBottle, false);
				}
			}
		} else {
			stack.shrink(1);
		}
		return stack;
	}

	@Override
	public int getUseDuration(ItemStack stack) {
		return type == Type.DRINK ? DRINK_TIME : 0;
	}

	@Override
	public UseAnim getUseAnimation(ItemStack stack) {
		return type == Type.DRINK ? UseAnim.DRINK : UseAnim.NONE;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
		if (type == Type.DRINK) {
			// 饮用型：起手读条，效果在 finishUsing 服务端结算
			return net.minecraft.world.item.ItemUtils.startUsingInstantly(world, user, hand);
		}
		// 喷溅 / 滞留型：投掷携带通用能量效果的原版药水弹射物，复用原版 AOE / 滞留云机制（仅服务端生成）
		if (!world.isClientSide) {
			spawnThrownPotion(world, user);
		}
		world.playSound(null, user.getX(), user.getY(), user.getZ(),
				SoundEvents.SPLASH_POTION_THROW, SoundSource.PLAYERS,
				0.5F, 0.4F / (world.getRandom().nextFloat() * 0.4F + 0.8F));
		if (!user.getAbilities().instabuild) {
			user.getItemInHand(hand).shrink(1);
		}
		user.awardStat(Stats.ITEM_USED.get(this));
		return InteractionResultHolder.sidedSuccess(user.getItemInHand(hand), world.isClientSide());
	}

	/** 生成携带通用能量效果的原版投掷药水（喷溅 / 滞留）。仅服务端调用。 */
	private void spawnThrownPotion(Level world, Player user) {
		ItemStack thrown = PotionUtils.setPotion(
				new ItemStack(type == Type.LINGERING ? Items.LINGERING_POTION : Items.SPLASH_POTION),
				AddonEffects.UNIVERSAL_ENERGY_POTION.get());
		thrown.getOrCreateTag().putInt("CustomPotionColor", UniversalEnergyEffect.POTION_COLOR);
		ThrownPotion entity = new ThrownPotion(world, user);
		entity.setItem(thrown);
		entity.shootFromRotation(user, user.getXRot(), user.getYRot(), -20.0F, 0.5F, 1.0F);
		world.addFreshEntity(entity);
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context) {
		super.appendHoverText(stack, world, tooltip, context);
		tooltip.add(Component.translatable("tooltip.ssc_addon.universal_potion").withStyle(net.minecraft.ChatFormatting.AQUA));
	}
}
