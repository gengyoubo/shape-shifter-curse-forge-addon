package github.com.gengyoubo.sscfa.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 迁移占位：法术/技能投射物与场地实体（火球、冰刺、激光、潮汐球、蛛丝弹等）。
 * 保留注册 ID、尺寸与追踪范围，使旧存档与 /summon 可用；沿当前速度直线飞行，10 秒后自行消失，不造成任何效果。
 * 原实体依赖能力、法术、平衡、网络等系统，待其迁移后逐个替换。
 */
public class ShellProjectile extends Projectile {
	private static final int LIFETIME = 200;

	public ShellProjectile(EntityType<? extends ShellProjectile> type, Level level) {
		super(type, level);
	}

	@Override
	protected void defineSynchedData() {
	}

	@Override
	public void tick() {
		super.tick();
		Vec3 motion = getDeltaMovement();
		setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
		if (!level().isClientSide && tickCount > LIFETIME) discard();
	}
}
