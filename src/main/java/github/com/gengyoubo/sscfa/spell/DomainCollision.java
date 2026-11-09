package github.com.gengyoubo.sscfa.spell;

import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** 方块碰撞、潜行和自动上台阶处理后的球壳终检，客户端与服务端共用。 */
public final class DomainCollision {
	private DomainCollision() {}

	public static Vec3 finishMovement(Entity entity, Vec3 movement, List<DomainRules.Shell> shells) {
		Vec3 clipped = DomainRules.clipMovement(entity.position(), movement, entity.getBbWidth() * 0.5, shells);
		if (clipped.equals(movement)) return movement;
		// 缩短上台阶位移也会降低落点；不能因此把碰撞箱放进台阶侧面。
		if (!entity.level().noCollision(entity, entity.getBoundingBox().move(clipped))) return Vec3.ZERO;
		return clipped;
	}
}
