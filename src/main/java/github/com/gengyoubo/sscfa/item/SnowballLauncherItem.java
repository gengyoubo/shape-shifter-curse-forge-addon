package github.com.gengyoubo.sscfa.item;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 雪球发射器：按住连发雪球（每 7 tick 一发），弹药存于 NBT {@code Ammo}，上限 {@value #MAX_AMMO}。
 * 原版佩戴便携冰箱时额外的紫水晶音效依赖饰品系统，迁移后补回。
 */
public class SnowballLauncherItem extends Item {
	public static final int MAX_AMMO = 20;

	public SnowballLauncherItem(Properties settings) {
		super(settings);
	}

	public static int getAmmo(ItemStack stack) {
		CompoundTag nbt = stack.getTag();
		return nbt != null ? nbt.getInt("Ammo") : 0;
	}

	public static void setAmmo(ItemStack stack, int ammo) {
		stack.getOrCreateTag().putInt("Ammo", Math.min(ammo, MAX_AMMO));
	}

	@Override
	public int getUseDuration(ItemStack stack) {
		return 72000;
	}

	@Override
	public UseAnim getUseAnimation(ItemStack stack) {
		return UseAnim.BOW;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
		user.startUsingItem(hand);
		return InteractionResultHolder.consume(user.getItemInHand(hand));
	}

	@Override
	public void onUseTick(Level world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
		int usedTicks = getUseDuration(stack) - remainingUseTicks;
		if (usedTicks % 7 == 0) {
			fire(world, user, stack);
		}
	}

	private void fire(Level world, LivingEntity entity, ItemStack stack) {
		int ammo = getAmmo(stack);
		boolean creative = entity instanceof Player player && player.getAbilities().instabuild;

		if (ammo <= 0 && !creative) {
			if (entity instanceof Player player) {
				player.playSound(SoundEvents.DISPENSER_FAIL, 1.0F, 1.2F);
			}
			entity.releaseUsingItem();
			return;
		}

		if (!world.isClientSide) {
			Snowball snowball = new Snowball(world, entity);
			snowball.setItem(new ItemStack(Items.SNOWBALL));
			snowball.shootFromRotation(entity, entity.getXRot(), entity.getYRot(), 0.0F, 2.25F, 1.0F);
			world.addFreshEntity(snowball);
			world.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.DISPENSER_LAUNCH, SoundSource.PLAYERS,
					1.0F, 1.0F / (world.getRandom().nextFloat() * 0.2F + 0.9F));
			if (!creative) {
				setAmmo(stack, ammo - 1);
			}
		}
	}

	@Override
	public boolean isBarVisible(ItemStack stack) {
		return true;
	}

	@Override
	public int getBarWidth(ItemStack stack) {
		return Math.min(13, Math.round((float) getAmmo(stack) / (float) MAX_AMMO * 13.0F));
	}

	@Override
	public int getBarColor(ItemStack stack) {
		return 0xA0E0FF;
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context) {
		tooltip.add(Component.translatable("tooltip.ssc_addon.launcher.ammo", getAmmo(stack), MAX_AMMO).withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.translatable("tooltip.ssc_addon.launcher.usage").withStyle(ChatFormatting.GOLD));
		super.appendHoverText(stack, world, tooltip, context);
	}
}
