package github.com.gengyoubo.sscfa.mixin.entity;

import net.minecraft.world.entity.monster.PatrollingMonster;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** 闁哄棙鎸冲﹢?PatrolEntity#setPatrolling(boolean) 缂備焦鐟╁顔句沪閻愭彃鐦跺ù锝堟硶閺併倝濡?*/
@Mixin(PatrollingMonster.class)
public interface PatrolEntityAccessor {
    @Invoker("setPatrolling")
    void invokeSetPatrolling(boolean patrolling);
}
