package github.com.gengyoubo.sscfa.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 迁移占位：SSCA 的生物（魔女使魔、美西螈变形者、月灵）。保留注册 ID、尺寸、分类与基础属性，
 * 只有游荡/看人的基础 AI；战斗、召唤、渲染待能力与客户端系统迁移后替换为原实体。
 */
public class ShellMob extends PathfinderMob {
	public ShellMob(EntityType<? extends PathfinderMob> type, Level level) {
		super(type, level);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0f));
		goalSelector.addGoal(7, new RandomLookAroundGoal(this));
	}
}
