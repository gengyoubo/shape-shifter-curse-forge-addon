package github.com.gengyoubo.sscfa.mixin.entity;

import github.com.gengyoubo.sscfa.ability.IronGolemOfferFlowerToAllayGoal;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.FormUtils;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(IronGolem.class)
public abstract class IronGolemAllayFlowerMixin extends Mob {
	protected IronGolemAllayFlowerMixin(EntityType<? extends Mob> entityType, Level world) {
		super(entityType, world);
	}

	@Inject(method = "registerGoals", at = @At("TAIL"))
	private void ssc_addon$addAllayFlowerGoal(CallbackInfo ci) {
		this.goalSelector.addGoal(6, new IronGolemOfferFlowerToAllayGoal((IronGolem) (Object) this));
		// 婵傛垹浼掗敍姘舵惂閸屸偓閸庘€插瘜閸斻劍鏁鹃崙璇差殩閻忕數甯虹€?		
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(
				(IronGolem) (Object) this,
				Player.class,
				10,
				true,
				false,
				p -> p instanceof Player pe && FormUtils.isForm(pe, FormIdentifiers.FAMILIAR_FOX_MANCIANIMA)
		));
	}

	@Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
	private void ssc_addon$convertAllayAttackToHealing(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
		if (!(source.getEntity() instanceof Player player) || !FormUtils.isAllaySP(player)) return;

		IronGolem golem = (IronGolem) (Object) this;
		if (amount > 0.0F && golem.getHealth() < golem.getMaxHealth()) {
			golem.heal(amount);
		}
		if (golem.getTarget() == player) {
			golem.setTarget(null);
		}
		if (player.getUUID().equals(golem.getPersistentAngerTarget())) {
			golem.setPersistentAngerTarget(null);
			golem.setRemainingPersistentAngerTime(0);
		}
		cir.setReturnValue(false);
	}
}
