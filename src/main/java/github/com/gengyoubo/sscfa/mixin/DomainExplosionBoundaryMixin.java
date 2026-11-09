package github.com.gengyoubo.sscfa.mixin;

import github.com.gengyoubo.sscfa.spell.DomainManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 领域爆炸隔离（jackcooper，2026-09-22 需求核实）：爆炸射线不认识黑色球壳（壳非方块），
 * 爆炸波及范围内的跨界方块会被照常炸毁——外界 TNT 能炸掉领域内方块、内部爆炸也能炸外界。
 *
 * <p>实体伤害已由 {@code SscAddonLivingEntityMixin.damage} 的跨界拦截覆盖（爆炸
 * DamageSource.getPosition 无显式坐标时回退爆源实体坐标，已反编译核实）；
 * 本 mixin 只补方块面：{@code collectBlocksAndDamageEntities} TAIL 过滤
 * {@code affectedBlocks} 中与爆心分属壳内外的方块（复用 {@link
 * DomainManager#blocksCrossBoundary}，壳间算内，与传送/声音语义一致）。</p>
 *
 * <p>仅服务端（方块破坏只在服务端结算）；被过滤的方块不炸不掉落，爆炸视觉粒子
 * 照常（客户端表现，无破坏语义）。</p>
 */
@Mixin(Explosion.class)
public abstract class DomainExplosionBoundaryMixin {
	@Shadow @Final private double x;
	@Shadow @Final private double y;
	@Shadow @Final private double z;
	@Shadow @Final private Level level;

	@Inject(method = "explode", at = @At("TAIL"))
	private void ssca$filterCrossBoundaryBlocks(CallbackInfo ci) {
		if (!(level instanceof ServerLevel serverWorld)) return;
		Vec3 center = new Vec3(x, y, z);
		((Explosion) (Object) this).getToBlow()
				.removeIf(pos -> DomainManager.blocksCrossBoundary(serverWorld, center, Vec3.atCenterOf(pos)));
	}
}
