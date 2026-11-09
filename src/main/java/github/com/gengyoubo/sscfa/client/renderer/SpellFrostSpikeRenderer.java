package github.com.gengyoubo.sscfa.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import github.com.gengyoubo.sscfa.entity.SpellFrostSpikeEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * 月尘魔法·冰锥渲染器（jackcooper）。按魔法等级切换两种外观?
 * <ul>
 *   <li><b>L1-3</b>：维持现??—??委??{@link ThrownItemRenderer} 渲染雪球物品精灵??/li>
 *   <li><b>L4+</b>：寒棘狐「冰刺」同??3D 冰锥模型（{@code frost_thorn_model}，CustomModelData=1 ??
 *       stage0 材质），保??0.9 缩放与原有中心补偿，尖端朝当前飞行方向??/li>
 * </ul>
 * 等级?DataTracker 同步，多人客机自动一致；3D 模型直接按客机当前飞行速度对正尖端??
 */
public class SpellFrostSpikeRenderer extends EntityRenderer<SpellFrostSpikeEntity> {
	// L1-3 委托的雪球渲染器（复用其逻辑，避免自己重写物品精灵渲染）
	private final ThrownItemRenderer<SpellFrostSpikeEntity> snowballRenderer;
	// L4+ 渲染用（CustomModelData ??1，静态复用，避免每??new + NBT 写入?
	private static ItemStack CACHED_STACK;
	// 冰锥渲染大小：模型本体长??0.6 格??.9 缩放后全长约 0.55 ??—??比雪球大一圈的显眼冰锥?
	// 仍小于寒棘狐凝棘满级强化锥??.5），保持视觉层级：魔法书投射?< SP 形态技能体
	private static final float SCALE = 0.9f;

	private final ItemRenderer itemRenderer;

	public SpellFrostSpikeRenderer(EntityRendererProvider.Context ctx) {
		super(ctx);
		this.snowballRenderer = new ThrownItemRenderer<>(ctx, 1.0F, true);
		this.itemRenderer = ctx.getItemRenderer();
	}

	@Override
	public void render(SpellFrostSpikeEntity entity, float yaw, float tickDelta, PoseStack matrices,
			MultiBufferSource vertexConsumers, int light) {
		if (entity.getSpellLevel() < 4) {
			// L1-3：维持现状（雪球物品精灵??
			snowballRenderer.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
			return;
		}
		// L4+閿涙艾鐦ㄥΛ妯煎珓閸氬本顑?3D 閸愪即鏁?
		matrices.pushPose();
		matrices.scale(SCALE, SCALE, SCALE);
		applyFlightRotation(matrices, entity.getDeltaMovement(), entity.getYRot(), entity.getXRot());
		// 模型中心补偿：frost_thorn 模型几何中心在网??(1.5, 0.5, 3) 而非标准 (8,8,8)??
		// 在旋转后的局部空间里平移，把模型中心对回实体锚点（与旋转无关，始终居中）
		matrices.translate((8.0 - 1.5) / 16.0, (8.0 - 0.5) / 16.0, (8.0 - 3.0) / 16.0);
		if (CACHED_STACK == null) {
			CACHED_STACK = new ItemStack(github.com.gengyoubo.sscfa.init.AddonItems.FROST_THORN.get());
			CACHED_STACK.getOrCreateTag().putInt("CustomModelData", 1);
		}
		this.itemRenderer.renderStatic(CACHED_STACK, ItemDisplayContext.GROUND, light, OverlayTexture.NO_OVERLAY,
				matrices, vertexConsumers, entity.level(), entity.getId());
		matrices.popPose();
		super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
	}

	/** Use flight data rather than stale spawn/tracker angles, including guided and spread projectiles. */
	static void applyFlightRotation(PoseStack matrices, Vec3 velocity, float fallbackYaw, float fallbackPitch) {
		float yaw = fallbackYaw;
		float pitch = fallbackPitch;
		if (velocity.lengthSqr() > 1.0e-12) {
			yaw = (float) Math.toDegrees(Math.atan2(-velocity.x, velocity.z));
			pitch = (float) Math.toDegrees(Math.atan2(-velocity.y, velocity.horizontalDistance()));
		}
		// The actual model's narrow tip is -Z. Yaw turns it horizontally; pitch lifts that tip.
		matrices.mulPose(Axis.YP.rotationDegrees(-yaw + 180.0F));
		matrices.mulPose(Axis.XP.rotationDegrees(-pitch));
	}

	@Override
	public ResourceLocation getTextureLocation(SpellFrostSpikeEntity entity) {
		// L1-3 委托雪球渲染器（内部自带贴图）；此方法仅基类外部调用（如阴影），给安全默认??
		return snowballRenderer.getTextureLocation(entity);
	}
}
