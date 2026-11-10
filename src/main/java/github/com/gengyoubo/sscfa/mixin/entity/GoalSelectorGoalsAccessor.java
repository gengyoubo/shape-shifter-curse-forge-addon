package github.com.gengyoubo.sscfa.mixin.entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;

/**
 * GoalSelector.goals 閻犱礁娼″Λ鍫曞闯椤帞绀勭紓鍥э梗缁ㄢ剝绋夐鍡欑瓘 AI 濠㈣泛娲ｉ崬?闁搞儳鍋為弬渚€鎮介…鎺旂闁挎稒顒歰als 濞?private Set&lt;PrioritizedGoal&gt;闁?
 * 濠㈣泛娲ｉ崬銈夊矗濡偐绛忛柛鎾崇Ф濞堟垿宕㈤悢鐑樻櫢 Goal 闂傚棗妫庨埀顑跨劍娴狀喗寰勫鍡橆槯闁搞儳鍋為弬渚€鏌堟禒瀣粯閻熸洑鑳跺ú鍧楀箳閵夘煈鍤㈤悗鐟板暔閳?
 */
@Mixin(GoalSelector.class)
public interface GoalSelectorGoalsAccessor {

	@Accessor("availableGoals")
	Set<WrappedGoal> ssca$getGoals();
}
