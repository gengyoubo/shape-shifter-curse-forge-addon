package github.com.gengyoubo.sscfa.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * 食梦魔「惊吓」幽灵野猫实体 —— 真实体 + 野猫形态 geo 模型。
 *
 * <p>由 {@code NightmareSpookManager} 在目标攻击幽灵苦力怕后生成（NoAI/无敌/对他人隐身，
 * 仅目标客户端经 SpookGhostVisibleMixin 显形）。服务端按阶段手动驱动位移：
 * 追跑 → 4 格起跳扑脸 → 贴身挥爪（12 魔法伤）→ 烟雾消散。
 * 客户端由 {@code GhostCatRenderer} 用 form_wild_cat_sp.geo 模型渲染
 * （程序化四足骨骼驱动，等效 SSC feral 姿态）。</p>
 */
public class GhostCatEntity extends PathfinderMob implements GeoAnimatable {

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

	public GhostCatEntity(EntityType<? extends PathfinderMob> type, Level world) {
		super(type, world);
	}

	public static AttributeSupplier.Builder createGhostCatAttributes() {
		return PathfinderMob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 20.0)
				.add(Attributes.MOVEMENT_SPEED, 0.3);
	}

	@Override
	protected void registerGoals() {
		// NoAI 使用：目标由 NightmareSpookManager 服务端手动驱动，不注册任何 AI
	}

	// ========== GeoEntity 实现 ==========

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		// 占位控制器（骨骼变换由渲染器程序化驱动）
		controllers.add(new AnimationController<>(this, "movement", 3, state -> PlayState.CONTINUE));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}

	@Override
	public double getTick(Object object) {
		return this.tickCount;
	}
}
