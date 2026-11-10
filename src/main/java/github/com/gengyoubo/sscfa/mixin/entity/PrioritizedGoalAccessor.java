package github.com.gengyoubo.sscfa.mixin.entity;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * PrioritizedGoal.goal / priority 閻犱礁娼″Λ鍫曞闯椤帞绀勭紓鍥э梗缁ㄢ剝绋夐鍡欑瓘 AI 濠㈣泛娲ｉ崬?闁搞儳鍋為弬渚€鎮介…鎺旂闁?
 * 濠㈣泛娲ｉ崬銈夊籍閺堢數鐭ら柛鏍ф嚀椤ュ﹪宕抽妸鈺佹闁瑰嘲妫楅崵顓犳啑?Goal 濞戞挸绨肩槐顓㈠礂閸垽鐛撻柨娑樻湰娴狀喗寰勫鍡橆槯闁圭顦敮顐ｅ濡搫甯ョ紒鐙欏啯绀€闁衡偓娣囨墎鍋?
 */
@Mixin(WrappedGoal.class)
public interface PrioritizedGoalAccessor {

	@Accessor("goal")
	Goal ssca$getGoal();

	@Accessor("priority")
	int ssca$getPriority();
}
