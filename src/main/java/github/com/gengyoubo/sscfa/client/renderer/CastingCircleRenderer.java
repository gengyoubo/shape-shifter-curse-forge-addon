package github.com.gengyoubo.sscfa.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import github.com.gengyoubo.sscfa.client.CastingVisualState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class CastingCircleRenderer {
	private CastingCircleRenderer() {}

	/** 零旋转圆环的角度恒定，独立初始化便于脱离渲染环境核对原公式?*/
	static final class RingTrig {
		private static final double[] COS = new double[65];
		private static final double[] SIN = new double[65];
		static {
			for (int point = 0; point <= 64; point++) {
				double angle = 0 + point * Math.PI / 32;
				COS[point] = Math.cos(angle);
				SIN[point] = Math.sin(angle);
			}
		}
		private RingTrig() {}
		static double cos(int point) { return COS[point]; }
		static double sin(int point) { return SIN[point]; }
	}

	public static void render(RenderLevelStageEvent context) {
		var bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
		var client = Minecraft.getInstance();
		if (client.level == null || bufferSource == null) return;
		Vec3 camera = context.getCamera().getPosition();
		for (var player : client.level.players()) {
			if (!player.isAlive() || player.isSpectator() || player.isInvisible()
					|| !CastingVisualState.hasCircle(player.getUUID()) || DomainRenderer.isCharging(player.getUUID())
					|| CastingVisualState.isExplosion(player.getUUID()) || CastingVisualState.isDeathFinale(player.getUUID())) continue;
			Vec3 feet = player.getPosition(context.getPartialTick());
			if (feet.distanceToSqr(camera) > 32 * 32) continue;
			var ground = client.level.clip(new ClipContext(feet.add(0, 0.15, 0),
					feet.add(0, -0.35, 0), ClipContext.Block.COLLIDER,
					ClipContext.Fluid.NONE, player));
			double circleY = ground.getType() == HitResult.Type.BLOCK
					&& ground.getDirection() == net.minecraft.core.Direction.UP
					&& Math.abs(ground.getLocation().y - feet.y) <= 0.16 ? ground.getLocation().y : feet.y;
			var matrices = context.getPoseStack();
			matrices.pushPose();
			matrices.translate(feet.x - camera.x, circleY + 0.025 - camera.y, feet.z - camera.z);
			VertexConsumer vertices = bufferSource.getBuffer(RenderType.debugQuads());
			// 2026-09-23 用户定稿：普通施法脚下法阵改用爆裂魔法同款地面设计（等比缩放到原 0.72 外径），
			// 主色随卷轴稀有度、辅色恒紫（groundStrokes 内置换色并按色缓存）；旋转速度与爆裂地面圈一致?
			int color = CastingVisualState.circleColor(player.getUUID());
			float age = player.tickCount + context.getPartialTick();
			float alpha = CastingVisualState.circleAlpha(player.getUUID()) * (0.88F + 0.06F * (float) Math.sin(age * 0.1));
			drawGround(vertices, matrices, 0.72, 0, age * 0.02, alpha, color);
			matrices.popPose();
		}
		for(var cancelled:CastingVisualState.cancellations()) {
			Vec3 position=cancelled.position();if(position.distanceToSqr(camera)>32*32)continue;
			var matrices=context.getPoseStack();matrices.pushPose();
			matrices.translate(position.x-camera.x,position.y+.025-camera.y,position.z-camera.z);
			drawCancellation(bufferSource.getBuffer(RenderType.debugQuads()),matrices,
					client.level.getGameTime()-cancelled.started()+context.getPartialTick(),cancelled.color());
			matrices.popPose();
		}
	}

	/** Existing small circles use the same hold, upward acceleration and fade as the explosion finale. */
	public static void drawCancellation(VertexConsumer vertices,PoseStack matrices,float age,int color) {
		float since=Math.max(0,age-github.com.gengyoubo.sscfa.spell.ExplosionRules.FINALE_HOLD_TICKS);
		float progress=since/github.com.gengyoubo.sscfa.spell.ExplosionRules.FINALE_RISE_TICKS;
		if(age<0||progress>=1)return;
		drawGround(vertices,matrices,.72,github.com.gengyoubo.sscfa.spell.ExplosionRules.finaleLift(0,since),age*.02,1-progress,color);
	}

	public static void ring(VertexConsumer vertices, Matrix4f matrix, double radius, double width,
	                         double rotation, int color, float alpha) {
		if (rotation == 0) {
			for (int segment = 0; segment < 64; segment++) {
				line(vertices, matrix, RingTrig.cos(segment) * radius, RingTrig.sin(segment) * radius,
						RingTrig.cos(segment + 1) * radius, RingTrig.sin(segment + 1) * radius, width, color, alpha);
			}
			return;
		}
		for (int segment = 0; segment < 64; segment++) {
			double start = rotation + segment * Math.PI / 32;
			double end = rotation + (segment + 1) * Math.PI / 32;
			line(vertices, matrix, Math.cos(start) * radius, Math.sin(start) * radius,
					Math.cos(end) * radius, Math.sin(end) * radius, width, color, alpha);
		}
	}

	/** Canonical ground circle: ordinary casting, domain and explosion all use this drawing. */
	public static void drawGround(VertexConsumer vertices, PoseStack matrices, double radius, double height,
	                              double rotation, float alpha, int primaryColor) {
		drawPattern(vertices, matrices, radius, height, rotation, alpha,
				MagicCircleGeometry.groundStrokes(primaryColor), primaryColor);
	}

	/** Shared cached-geometry path for ordinary casting, domain and explosion circles. */
	public static void drawPattern(VertexConsumer vertices, PoseStack matrices, double radius, double height,
	                               double rotation, float alpha, java.util.List<MagicCircleGeometry.Stroke> strokes,
	                               int glowColor) {
		if (radius <= 0.05 || alpha <= 0) return;
		matrices.pushPose();
		matrices.translate(0, height, 0);
		matrices.scale((float) radius, 1, (float) radius);
		matrices.mulPose(com.mojang.math.Axis.YP.rotation((float) rotation));
		Matrix4f matrix = matrices.last().pose();
		MagicCircleGeometry.Mesh mesh = MagicCircleGeometry.mesh(strokes);
		// 频闪修复?026-09-23）：getDebugQuads ?translucent 层（反编译确?buffer.setSorter 逐帧按相?
		// 距离排序四边形），发光层与主线同平面且到相机距离大量相等（排序平局），相机微动即翻转混合顺?
		// ?亮度逐帧交替。两层保险：?发光层下?0.002 格打破共面（确定的主线在上关系，排序稳定）；
		// ?小尺寸法阵（半径 < 1 格，如施法者脚?0.72 格）跳过发光层——小尺寸下发光本就糊成一片，
		// 且贡献了绝大多数重叠四边形。爆?领域的大法阵（≥2 格）保留发光层，仅受益于 ①?
		boolean glow = radius >= 1.0;
		if (glow) {
			// 发光层矩阵：在当前矩阵基础上再向下平移 0.002 格（除以半径抵消上方 scale?
			matrices.pushPose();
			matrices.translate(0, -0.002 / radius, 0);
			Matrix4f glowMatrix = matrices.last().pose();
			drawStrokes(vertices, glowMatrix, strokes, mesh, true, glowColor, alpha);
			matrices.popPose();
		}
		drawStrokes(vertices, matrix, strokes, mesh, false, glowColor, alpha);
		matrices.popPose();
	}

	private static void drawStrokes(VertexConsumer vertices, Matrix4f matrix,
	                                java.util.List<MagicCircleGeometry.Stroke> strokes,
	                                MagicCircleGeometry.Mesh mesh, boolean glow, int glowColor, float alpha) {
		if (mesh == null) {
			for (var stroke : strokes) {
				float opacity = alpha * stroke.alpha();
				if (glow) opacity *= MagicCircleGeometry.GLOW_ALPHA;
				drawStroke(vertices, matrix, stroke, glow ? stroke.width() * MagicCircleGeometry.GLOW_WIDTH : stroke.width(),
						glow ? glowColor : stroke.color(), opacity);
			}
			return;
		}
		if (CircleVertexBatch.draw(vertices, matrix, mesh, glow, glowColor, alpha)) return;
		for (int i = 0; i < mesh.size(); i++) {
			var quad = mesh.quad(i, glow);
			float opacity = alpha * quad.alpha();
			if (glow) opacity *= MagicCircleGeometry.GLOW_ALPHA;
			int color = glow ? glowColor : quad.color();
			float red = ((color >> 16) & 255) / 255F;
			float green = ((color >> 8) & 255) / 255F;
			float blue = (color & 255) / 255F;
			vertex(vertices, matrix, quad.x1(), quad.z1(), red, green, blue, opacity);
			vertex(vertices, matrix, quad.x2(), quad.z2(), red, green, blue, opacity);
			vertex(vertices, matrix, quad.x3(), quad.z3(), red, green, blue, opacity);
			vertex(vertices, matrix, quad.x4(), quad.z4(), red, green, blue, opacity);
		}
	}

	private static void vertex(VertexConsumer vertices, Matrix4f matrix, float x, float z,
	                           float red, float green, float blue, float alpha) {
		vertices.vertex(matrix, x, 0, z).color(red, green, blue, alpha).endVertex();
	}

	private static void drawStroke(VertexConsumer vertices, Matrix4f matrix,
	                               MagicCircleGeometry.Stroke stroke, double width, int color, float alpha) {
		double half = width / 2;
		vertex(vertices, matrix, stroke.x1() + stroke.nx1() * half, stroke.z1() + stroke.nz1() * half, color, alpha);
		vertex(vertices, matrix, stroke.x2() + stroke.nx2() * half, stroke.z2() + stroke.nz2() * half, color, alpha);
		vertex(vertices, matrix, stroke.x2() - stroke.nx2() * half, stroke.z2() - stroke.nz2() * half, color, alpha);
		vertex(vertices, matrix, stroke.x1() - stroke.nx1() * half, stroke.z1() - stroke.nz1() * half, color, alpha);
	}

	public static void line(VertexConsumer vertices, Matrix4f matrix, double startX, double startZ,
	                         double endX, double endZ, double width, int color, float alpha) {
		double length = Math.hypot(endX - startX, endZ - startZ);
		double offsetX = -(endZ - startZ) / length * width * 0.5;
		double offsetZ = (endX - startX) / length * width * 0.5;
		vertex(vertices, matrix, startX + offsetX, startZ + offsetZ, color, alpha);
		vertex(vertices, matrix, endX + offsetX, endZ + offsetZ, color, alpha);
		vertex(vertices, matrix, endX - offsetX, endZ - offsetZ, color, alpha);
		vertex(vertices, matrix, startX - offsetX, startZ - offsetZ, color, alpha);
	}

	private static void vertex(VertexConsumer vertices, Matrix4f matrix, double horizontal, double depth, int color, float alpha) {
		vertices.vertex(matrix, (float) horizontal, 0, (float) depth)
				.color(((color >> 16) & 255) / 255F, ((color >> 8) & 255) / 255F, (color & 255) / 255F, alpha).endVertex();
	}
}
