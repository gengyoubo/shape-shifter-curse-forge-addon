package github.com.gengyoubo.sscfa.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import github.com.gengyoubo.sscfa.util.FormUtils;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import java.util.List;

public class WaterSpearItem extends TridentItem {
	private static final int TICKS_PER_DURABILITY = 20;

	public WaterSpearItem(Properties settings) {
		super(settings);
	}

	@Override
	public void releaseUsing(ItemStack stack, Level world, LivingEntity user, int remainingUseTicks) {
		if (user instanceof Player playerEntity) {
			int i = this.getUseDuration(stack) - remainingUseTicks;
			if (i >= 10) {
				float f = playerEntity.getYRot();
				float g = playerEntity.getXRot();
				float h = -Mth.sin(f * ((float) Math.PI / 180)) * Mth.cos(g * ((float) Math.PI / 180));
				float j = -Mth.sin(g * ((float) Math.PI / 180));
				float k = Mth.cos(f * ((float) Math.PI / 180)) * Mth.cos(g * ((float) Math.PI / 180));
				float l = Mth.sqrt(h * h + j * j + k * k);
				float m = 2.5F;
				h *= m / l;
				j *= m / l;
				k *= m / l;

				if (!world.isClientSide) {
					WaterSpearEntity waterSpear = new WaterSpearEntity(world, playerEntity, stack);
					waterSpear.shootFromRotation(playerEntity, playerEntity.getXRot(), playerEntity.getYRot(), 0.0F, m, 1.0F);
					stack.shrink(1);
					world.addFreshEntity(waterSpear);
					world.playSound(null, playerEntity.getX(), playerEntity.getY(), playerEntity.getZ(), SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.0F, 1.0F);
					world.playSound(null, playerEntity.getX(), playerEntity.getY(), playerEntity.getZ(), SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 0.5F, 1.2F);
				}

				playerEntity.awardStat(Stats.ITEM_USED.get(this));
			}
		}
	}

	@Override
	public int getUseDuration(ItemStack stack) {
		return 72000;
	}

	@Override
	public UseAnim getUseAnimation(ItemStack stack) {
		return UseAnim.SPEAR;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
		ItemStack itemStack = user.getItemInHand(hand);

		if (FormUtils.isAxolotlSP(user)) {
			user.startUsingItem(hand);
			return InteractionResultHolder.success(itemStack);
		}

		return InteractionResultHolder.fail(itemStack);
	}

	@Override
	public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		// 甯︽敾鍑昏€?source 渚涘叆姊︽嫤鎴綊鍥?
		target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1), attacker);

		if (!target.level().isClientSide) {
			Level world = target.level();
			double x = target.getX();
			double y = target.getY() + target.getBbHeight() / 2;
			double z = target.getZ();

			List<Entity> nearbyEntities = world.getEntities(attacker, new AABB(x - 1.5, y - 1.5, z - 1.5, x + 1.5, y + 1.5, z + 1.5));
			for (Entity entity : nearbyEntities) {
				if (entity instanceof LivingEntity living && entity != attacker && entity != target) {
					living.hurt(world.damageSources().mobAttack(attacker), 4.0f);
				}
			}

			world.playSound(null, x, y, z, SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 1.0F, 0.8F);
		}

		stack.hurtAndBreak(1, attacker, e -> e.broadcastBreakEvent(attacker.getUsedItemHand()));

		return true;
	}

	@Override
	public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected) {
		super.inventoryTick(stack, world, entity, slot, selected);

		if (!world.isClientSide) {
			if (entity instanceof Player player) {
				boolean isSpAxolotl = player.isCreative() || FormUtils.isAxolotlSP(player);

				if (!isSpAxolotl) {
					stack.setCount(0);
					return;
				}

			} else {
				stack.setCount(0);
				return;
			}
		}

		if (!world.isClientSide && entity instanceof LivingEntity livingEntity && world.getGameTime() % TICKS_PER_DURABILITY == 0) {
			stack.hurtAndBreak(1, livingEntity, e -> {
			});
		}
	}

	@Override
	public int getEnchantmentValue() {
		return 0;
	}

	@Override
	public boolean isValidRepairItem(ItemStack stack, ItemStack ingredient) {
		return false;
	}

	@Override
	public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
		if (slot == EquipmentSlot.MAINHAND) {
			ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
			builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Tool modifier", 8.0, AttributeModifier.Operation.ADDITION));
			builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Tool modifier", -3.0, AttributeModifier.Operation.ADDITION));
			return builder.build();
		}
		return super.getDefaultAttributeModifiers(slot);
	}
}
