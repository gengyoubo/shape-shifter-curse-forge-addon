package github.com.gengyoubo.sscfa.particle.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * 「汇聚冰晶」粒子客户端实现：匀速直线飞向中心，抵达（寿命尽）即移除，末段淡出。
 *
 * <p><b>为什么必须自定义粒子</b>（反编译 {@code Particle} 6 参构造器实证）：原版粒子构造器会给初速加
 * ±0.4 随机抖动并乘 0.15~0.45 随机系数——本粒子 0.05/tick 的汇聚速度会被随机量完全淹没（视觉即「四散」）；
 * WhiteAsh/Snowflake 还带正重力会下坠。故本粒子：
 * <ul>
 *   <li>走 3 参构造器（零速度）+ 手动赋精确速度——绕开随机抖动；</li>
 *   <li>零物理：gravityStrength=0、velocityMultiplier=1（无重力、无摩擦）→ 匀速直线；</li>
 *   <li>固定寿命 {@value #INWARD_ICE_LIFETIME} tick——服务端按「初速 = 距离/寿命」发速度，
 *       寿命尽头 = 抵达中心 = 自动移除（「汇聚到中心就消失」精确成立）；</li>
 *   <li>末 {@value #FADE_TICKS} tick alpha 平滑淡出。</li>
 * </ul></p>
 */
public class InwardIceParticle extends TextureSheetParticle {

	/** 固定寿命（tick）：服务端按此值计算初速（距离/寿命），保证恰好抵达中心。 */
	public static final int INWARD_ICE_LIFETIME = 20;
	/** 末段淡出时长（tick）。 */
	private static final int FADE_TICKS = 5;

	private InwardIceParticle(ClientLevel world, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites) {
		// 3 参构造器：速度零初始化（绕开 6 参构造器的 ±0.4 随机抖动 + 随机缩放）
		super(world, x, y, z);
		// 单帧贴图：构造时取一次 sprite 即可，无需保留 provider 引用
		this.setSprite(sprites.get(0, 1));
		// 精确赋服务端算好的向心速度（字段直赋，无任何随机化）
		this.xd = vx;
		this.yd = vy;
		this.zd = vz;
		// 零物理：无重力、无摩擦 → 匀速直线
		this.gravity = 0.0F;
		this.friction = 1.0F;
		this.hasPhysics = false;
		this.lifetime = INWARD_ICE_LIFETIME;
		// 小冰晶颗粒（渲染尺寸 0.12 格）；白色乘色保持贴图冰蓝白原色
		this.scale(0.12f);
		this.rCol = 1.0F;
		this.gCol = 1.0F;
		this.bCol = 1.0F;
	}

	@Override
	public ParticleRenderType getRenderType() {
		return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
	}

	@Override
	public void tick() {
		super.tick(); // gravity=0、multiplier=1 → 匀速直线；寿命尽自动 markDead
	}

	@Override
	protected int getLightColor(float tickDelta) {
		// 全亮度：冰晶在暗处也清晰可见（同 EndRod/Snowflake 自发光处理）
		return 15728880;
	}

	@Override
	public void render(com.mojang.blaze3d.vertex.VertexConsumer vertexConsumer, net.minecraft.client.Camera camera, float tickDelta) {
		// 末段淡出：按剩余寿命压 alpha（alpha 为实例字段，安全）
		int remain = this.lifetime - this.age;
		if (remain <= FADE_TICKS) {
			this.alpha = Math.max(0.0F, remain / (float) FADE_TICKS);
		}
		super.render(vertexConsumer, camera, tickDelta);
	}

	/** 粒子工厂：服务端速度精确交给粒子（无随机化）。 */
	public static class Factory implements ParticleProvider<SimpleParticleType> {
		private final SpriteSet spriteProvider;

		public Factory(SpriteSet sprites) {
			this.spriteProvider = sprites;
		}

		@Override
		public InwardIceParticle createParticle(SimpleParticleType type, ClientLevel world, double x, double y, double z, double vx, double vy, double vz) {
			return new InwardIceParticle(world, x, y, z, vx, vy, vz, spriteProvider);
		}
	}
}
