package github.com.gengyoubo.sscfa.mixin.entity;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * MobEntity 闁?goalSelector / targetSelector / initGoals 閻犱礁娼″Λ鍫曞闯椤帞绀勭紓鍥э梗缁ㄢ剝绋夐鍡欑瓘 AI 闁哄洦瀵у畷鏌ユ偨椤帞绀嗛柕?
 * 濞戞挶鍊濋埀顒€顦扮€氥劑宕抽妸銈囩憿 initGoals 闁秆冩矗鐠?protected闁挎稑鐭傚顔句沪閻愬瓨鐝梺顐ｆ皑鐞氼偊寮悩宕囥€婇柣鈺佺摠鐢鎲撮敃浣瑰涧闁炽儲鏌￠埀顒佹閵嗗﹦浠?accessor mixin 闁哄棙鎸冲﹢鍫曞Υ?
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