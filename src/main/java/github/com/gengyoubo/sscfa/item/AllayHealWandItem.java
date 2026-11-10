package github.com.gengyoubo.sscfa.item;

import io.github.apace100.apoli.component.PowerHolderComponent;
import io.github.apace100.apoli.power.Power;
import io.github.apace100.apoli.power.PowerType;
import io.github.apace100.apoli.power.PowerTypeRegistry;
import io.github.apace100.apoli.power.VariableIntPower;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * SP閹妇浼掗崡鏇氱秼濞岃崵鏋熼悧鈺佹惂
 * 閹靛瀵旈弮璺哄櫙閺勭喎顕崙?0閺嶇厧鍞撮悽鐔哄⒖娴兼岸鐝禍顕嗙礄閻у€熷濡楀棴绱氶敍?
 * 閸欐娊鏁▽鑽ゆ灍閻╊喗鐖?鐞涒偓閿?妫版绺鹃敍澶涚礉濞戝牐鈧?2閻愮鍏橀柌蹇ョ礉2.5缁夋帒鍠庨崡?
 */
public class AllayHealWandItem extends Item {

	public static final float HEAL_AMOUNT = 8.0f;
	public static final int COOLDOWN_TICKS = 50; // 2.5 seconds
	public static final double MAX_RANGE = 20.0;
	public static final int MANA_COST = 12;

	private static final ResourceLocation MANA_RESOURCE_ID = github.com.gengyoubo.sscfa.util.FormIdentifiers.ALLAY_MANA_RESOURCE;
	private static final ResourceLocation MANA_COOLDOWN_ID = github.com.gengyoubo.sscfa.util.FormIdentifiers.ALLAY_MANA_COOLDOWN;

	public AllayHealWandItem(Properties settings) {
		super(settings);
	}

	/**
	 * Get the entity the player is looking at within MAX_RANGE
	 */
	@Nullable
	public static LivingEntity getTargetedEntity(Player player) {
		Vec3 eyePos = player.getEyePosition();
		Vec3 lookDir = player.getViewVector(1.0f);
		Vec3 endPos = eyePos.add(lookDir.scale(MAX_RANGE));

		// Get all entities in the range
		AABB searchBox = player.getBoundingBox().inflate(MAX_RANGE);
		Predicate<Entity> predicate = entity -> !entity.isSpectator() && entity.isPickable() && entity instanceof LivingEntity && entity != player;

		double closestDist = MAX_RANGE * MAX_RANGE;
		LivingEntity closestEntity = null;

		for (Entity entity : player.level().getEntities(player, searchBox, predicate)) {
			AABB entityBox = entity.getBoundingBox().inflate(entity.getPickRadius());
			var optional = entityBox.clip(eyePos, endPos);
			if (optional.isPresent()) {
				double dist = eyePos.distanceToSqr(optional.get());
				if (dist < closestDist) {
					closestDist = dist;
					closestEntity = (LivingEntity) entity;
				}
			}
		}

		return closestEntity;
	}

	/**
	 * Check if there's a clear line of sight between player and target (no blocks in the way)
	 */
	public static boolean hasLineOfSight(Player player, LivingEntity target) {
		Vec3 eyePos = player.getEyePosition();
		Vec3 targetPos = target.getEyePosition();

		HitResult blockHit = player.level().clip(new ClipContext(
				eyePos, targetPos,
				ClipContext.Block.COLLIDER,
				ClipContext.Fluid.NONE,
				player
		));

		// If the block hit is beyond the target or missed, we have line of sight
		if (blockHit.getType() == HitResult.Type.MISS) {
			return true;
		}

		double blockDist = eyePos.distanceToSqr(blockHit.getLocation());
		double targetDist = eyePos.distanceToSqr(targetPos);

		return blockDist >= targetDist;
	}

	private static int getManaValue(ServerPlayer player) {
		try {
			var powerHolder = PowerHolderComponent.KEY.get(player);
			PowerType<?> powerType = PowerTypeRegistry.get(MANA_RESOURCE_ID);
			Power power = powerHolder.getPower(powerType);
			if (power instanceof VariableIntPower variablePower) {
				return variablePower.getValue();
			}
		} catch (Exception e) {
			// Resource not found
		}
		return 0;
	}

	// ===== Mana resource read/write =====

	private static void setManaValue(ServerPlayer player, int value) {
		try {
			var powerHolder = PowerHolderComponent.KEY.get(player);
			PowerType<?> powerType = PowerTypeRegistry.get(MANA_RESOURCE_ID);
			Power power = powerHolder.getPower(powerType);
			if (power instanceof VariableIntPower variablePower) {
				variablePower.setValue(Math.max(0, value));
				// 閸欘亜鎮撳顧砤na鏉╂瑤绔存稉鐚祇wer閿涘矂浼╅崗宥呭弿闁插紥ync闁插秶鐤嗘妯昏癁power鐎广垺鍩涚粩顖滄畱ascendProgress
				PowerHolderComponent.syncPower(player, powerType);
			}
		} catch (Exception e) {
			// Resource not found
		}
	}

	private static void triggerManaCooldown(ServerPlayer player) {
		try {
			var powerHolder = PowerHolderComponent.KEY.get(player);
			PowerType<?> powerType = PowerTypeRegistry.get(MANA_COOLDOWN_ID);
			Power power = powerHolder.getPower(powerType);
			if (power instanceof VariableIntPower variablePower) {
				variablePower.setValue(70); // 3.5 seconds cooldown
				// 閸欘亜鎮撳顧﹐oldown鏉╂瑤绔存稉鐚祇wer
				PowerHolderComponent.syncPower(player, powerType);
			}
		} catch (Exception e) {
			// Resource not found
		}
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
		ItemStack stack = user.getItemInHand(hand);

		if (!world.isClientSide && user instanceof ServerPlayer serverPlayer) {
			// Find the targeted entity
			LivingEntity target = getTargetedEntity(serverPlayer);

			if (target != null) {
				// 缂佺喍绔村鍝勫閻╊喗鐖ｉ崚銈呯暰閿涙艾褰堥張宥呭缁旑垳娅ч崥宥呭礋閹绱戦崗铏付閸?
				// - 瀵偓閸忓啿鍙ч梻顓ㄧ窗娴犲懍缍旈悽銊ょ艾闂堢偞鈧亞澧?闂堢偞鏅€靛湱鏁撻悧?
				// - 瀵偓閸忓啿绱戦崥顖ょ窗閻ц棄鎮曢崡鏇犫敄閺冩湹绮庡▽鑽ゆ灍閻溾晛顔?妞诡垱婀囩€圭姷澧?owner-tag閿涙盯娼粚鐑樻娴犲懏涓嶉悿妤冩閸氬秴宕熼崘鍛嚠鐠?
				if (!github.com.gengyoubo.sscfa.util.WhitelistUtils.isBuffTarget(serverPlayer, target)) {
					serverPlayer.displayClientMessage(Component.translatable("item.ssc_addon.allay_heal_wand.no_target").withStyle(ChatFormatting.RED), true);
					return InteractionResultHolder.fail(stack);
				}
				// Check line of sight (no block obstruction)
				boolean hasLineOfSight = hasLineOfSight(serverPlayer, target);

				if (hasLineOfSight) {
					// Check mana
					int currentMana = getManaValue(serverPlayer);
					if (currentMana < MANA_COST) {
						serverPlayer.displayClientMessage(Component.translatable("item.ssc_addon.allay_heal_wand.no_mana").withStyle(ChatFormatting.RED), true);
						return InteractionResultHolder.fail(stack);
					}

					// Consume mana
					setManaValue(serverPlayer, currentMana - MANA_COST);

					// Trigger mana cooldown
					triggerManaCooldown(serverPlayer);

					// Heal the target
					target.heal(HEAL_AMOUNT);

					// Spawn heal particles
					ServerLevel serverWorld = (ServerLevel) world;
					github.com.gengyoubo.sscfa.util.ParticleUtils.spawnParticles(serverWorld, ParticleTypes.HEART,
							target.getX(), target.getY() + target.getBbHeight() + 0.5, target.getZ(),
							5, 0.3, 0.3, 0.3, 0.01);

					// Play heal sound
					// User hears private sound
					serverPlayer.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0f, 1.5f);
					// Target and nearby players hear positional sound (exclude user to avoid double sound)
					world.playSound(serverPlayer, target.getX(), target.getY(), target.getZ(),
							SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0f, 1.5f);

					// Set cooldown
					user.getCooldowns().addCooldown(this, COOLDOWN_TICKS);

					return InteractionResultHolder.success(stack);
				} else {
					// Target is behind a wall
					serverPlayer.displayClientMessage(Component.translatable("item.ssc_addon.allay_heal_wand.blocked").withStyle(ChatFormatting.RED), true);
				}
			} else {
				serverPlayer.displayClientMessage(Component.translatable("item.ssc_addon.allay_heal_wand.no_target").withStyle(ChatFormatting.GRAY), true);
			}
		}

		return InteractionResultHolder.pass(stack);
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context) {
		tooltip.add(Component.translatable("item.ssc_addon.allay_heal_wand.tooltip").withStyle(ChatFormatting.AQUA));
		tooltip.add(Component.translatable("item.ssc_addon.allay_heal_wand.tooltip.exclusive").withStyle(ChatFormatting.LIGHT_PURPLE));
		super.appendHoverText(stack, world, tooltip, context);
	}
}
