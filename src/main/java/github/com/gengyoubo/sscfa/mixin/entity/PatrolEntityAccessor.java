package github.com.gengyoubo.sscfa.mixin.entity;

import net.minecraft.world.entity.monster.PatrollingMonster;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** 暴露 PatrolEntity#setPatrolling(boolean) 给附属包使用。 */
@Mixin(PatrollingMonster.class)
public interface PatrolEntityAccessor {
    @Invoker("setPatrolling")
    void invokeSetPatrolling(boolean patrolling);
}
