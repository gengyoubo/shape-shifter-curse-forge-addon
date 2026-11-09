package github.com.gengyoubo.sscfa.mixin.entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;

/**
 * GoalSelector.goals 访问器（羊了个羊 AI 备份/回放用）：goals 为 private Set&lt;PrioritizedGoal&gt;，
 * 备份变羊前的原生 Goal 集、恢复时回放都需要直接读它。
 */
@Mixin(GoalSelector.class)
public interface GoalSelectorGoalsAccessor {

	@Accessor("availableGoals")
	Set<WrappedGoal> ssca$getGoals();
}
