package github.com.gengyoubo.sscfa.mixin.entity;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * MobEntity 的 goalSelector / targetSelector / initGoals 访问器（羊了个羊 AI 替换用）。
 * 两选择器与 initGoals 均为 protected，附属普通类无法直接触达——顶层 accessor mixin 暴露。
 */
@Mixin(Mob.class)
public interface MobEntityGoalAccessor {

	@Accessor("goalSelector")
	GoalSelector ssca$getGoalSelector();

	@Accessor("targetSelector")
	GoalSelector ssca$getTargetSelector();

	@Invoker("registerGoals")
	void ssca$invokeInitGoals();
}