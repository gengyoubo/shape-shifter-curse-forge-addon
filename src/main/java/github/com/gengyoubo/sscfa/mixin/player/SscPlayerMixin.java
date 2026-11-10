package github.com.gengyoubo.sscfa.mixin.player;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import github.com.gengyoubo.sscfa.SscAddon;
import github.com.gengyoubo.sscfa.ability.MancianimaAggroTracker;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.FormUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import github.com.gengyoubo.sscfa.compat.MoistureTickSnapshot;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.UUID;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class SscPlayerMixin implements MoistureTickSnapshot {

    @Unique private long ssca$moistureEpoch = Long.MIN_VALUE;
    @Unique private int ssca$moistureBefore = Integer.MIN_VALUE;

    @Override public int ssca$moistureBefore(long epoch) {
        return ssca$moistureEpoch == epoch ? ssca$moistureBefore : Integer.MIN_VALUE;
    }

    @Override public void ssca$moistureBefore(long epoch, int air) {
        ssca$moistureEpoch = epoch;
        ssca$moistureBefore = air;
    }

	/**
	 * 「月痕之力」剧情真睡：sp 使魔上床真正睡眠期间，顶住 SSC {@code CursedMoonWorldMixin} 每 tick
	 * 对其的「诅咒之月强制唤醒」（SSC 唯一禁睡点），让玩家能真正睡着、不被弹起。
	 * <p>
	 * <b>关键</b>：SSC 调的是 {@code player.wakeUp()}（无参），而 {@code PlayerEntity} 重写了无参
	 * {@code wakeUp()} → 它覆盖了 {@code LivingEntity.wakeUp()}，所以必须注入 <b>PlayerEntity 层</b>的
	 * {@code wakeUp()V} 才拦得住（注入 LivingEntity 层会被多态绕过，这正是之前「假睡」抖动的根因）。
	 * <p>
	 * 仅对「处于剧情真睡的服务端玩家」生效（{@code instanceof ServerPlayerEntity} 短路，客户端实体不读状态、
	 * 无跨线程访问）；睡满后由 {@code MoonScarStoryManager.tickStorySleep} 移除标记后主动叫醒。
	 */
	@Inject(method = "stopSleeping()V", at = @At("HEAD"), cancellable = true)
	private void ssc_addon$keepMoonScarStorySleeping(CallbackInfo ci) {
		if ((Object) this instanceof ServerPlayer sp) {
			UUID uuid = sp.getUUID();
			if (github.com.gengyoubo.sscfa.story.MoonScarStoryManager.isStorySleeping(uuid)
					|| github.com.gengyoubo.sscfa.story.TideSpiritStoryManager.isStorySleeping(uuid)) {
				ci.cancel();
			}
		}
	}


	@ModifyVariable(method = "attack", at = @At(value = "STORE", ordinal = 0), ordinal = 2)
	private boolean forceCrit(boolean isCritical) {
		if (((Player) (Object) this).hasEffect(SscAddon.GUARANTEED_CRIT)) {
			return true;
		}
		return isCritical;
	}

	/**
	 * 契灵基础攻击设定（用 @WrapOperation 兼容 SSC 主包同位置的 @Redirect）：
	 * - 无法攻击劫掠阵营生物
	 * - 空手：固定 1 颗心（2.0F），无视护甲（indirectMagic 伤害源）
	 * - 持武器：总伤害 -20%（暴击通过 base*0.8*1.5 = base*1.2 自动 -20%）
	 * - 主动攻击 mob → 激怒该 mob（之后 mob 才能将契灵设为目标）
	 */
	@WrapOperation(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
	private boolean ssc_addon$mancianimaAttackScale(Entity target, DamageSource source, float amount, Operation<Boolean> original) {
		Player self = (Player) (Object) this;
		if (!FormUtils.isForm(self, FormIdentifiers.FAMILIAR_FOX_MANCIANIMA)) {
			return original.call(target, source, amount);
		}
		// 无法攻击劫掠阵营生物
		if (target instanceof Raider) {
			if (!self.level().isClientSide()) {
				self.playSound(SoundEvents.FIRE_EXTINGUISH, 0.5f, 0.8f);
			}
			return false;
		}
		boolean bareHand = self.getMainHandItem().isEmpty();
		boolean result;
		if (bareHand) {
			DamageSource bypassSrc = self.damageSources().indirectMagic(self, self);
			result = original.call(target, bypassSrc, github.com.gengyoubo.sscfa.ability.NpcCombatEnhancements.fixedDamage(self, target, 2.0F));
		} else {
			result = original.call(target, source, amount * 0.8F);
		}
		// 激怒被攻击的 mob（仅在伤害命中时记录；坚守者/铁傀儡 本就主动攻击，仍记录无副作用）
		if (result && target instanceof Mob mob && !self.level().isClientSide()) {
			MancianimaAggroTracker.provoke(mob.getUUID(), self.getUUID());
		}
		return result;
	}
	/**
	 * 死亡时掉落药水袋内物品（仅当 keepInventory 关闭时）。（原 PotionBagDeathDropMixin 合并至此；行为不变。）
	 * 1.20.1 yarn：PlayerEntity 覆写了 dropInventory()，在 keepInventory 关闭时执行 inventory.dropAll()；
	 * 在 HEAD 注入：此时物品栏尚未 dropAll，可遍历找到药水袋并掉落其内物品。
	 */
	@Inject(method = "dropEquipment()V", at = @At("HEAD"))
	private void ssc_addon$dropPotionBagItems(CallbackInfo ci) {
		Player player = (Player) (Object) this;
		boolean keepInventory = player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY);
		if (keepInventory) {
			return;
		}
		ItemStack potionBag = ItemStack.EMPTY;
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack stack = player.getInventory().getItem(i);
			if (stack.is(SscAddon.POTION_BAG)) {
				potionBag = stack;
				break;
			}
		}
		if (!potionBag.isEmpty() && potionBag.hasTag()) {
			CompoundTag nbt = potionBag.getTag();
			if (nbt != null && nbt.contains("Items", 9)) {
				ListTag list = nbt.getList("Items", 10);
				for (int i = 0; i < list.size(); ++i) {
					CompoundTag itemTag = list.getCompound(i);
					ItemStack stack = ItemStack.of(itemTag);
					if (!stack.isEmpty()) {
						player.drop(stack, true, false);
					}
				}
				nbt.remove("Items");
			}
		}
	}

	/**
	 * 禁止用 Q 键或任何方式丢弃药水袋。（原 PreventPotionBagDropMixin 合并至此；行为不变。）
	 */
	@Inject(method = "drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;", at = @At("HEAD"), cancellable = true)
	private void ssc_addon$preventPotionBagDrop(ItemStack stack, boolean throwRandomly, boolean retainOwnership, CallbackInfoReturnable<?> cir) {
		if (stack.is(SscAddon.POTION_BAG)) {
			cir.setReturnValue(null);
		}
	}}
