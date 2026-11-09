package github.com.gengyoubo.sscfa.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import github.com.gengyoubo.sscfa.entity.SpellMoonlightArrowEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * 月尘魔法·月光箭渲染器（jackcooper）??D 光灵箭模型（{@code moonlight_arrow_render}
 * 隐藏渲染物????{@code moonlight_arrow_projectile} ??elements JSON，贴图复用原?
 * spectral_arrow），沿实体速度朝向摆正（箭头朝飞行方向）。与冰??L4 渲染同范式??
 *
 * <p>实??tick 已按速度自??yaw/pitch；模型箭头朝 +Z（网?z??4.5 端）?
 * 几何中心在标?(8,8,8)，无需中心补偿?/p>
 */
public class MoonlightArrowRenderer extends EntityRenderer<SpellMoonlightArrowEntity> {
	/** 渲染大小：全长约 0.85 格的显眼箭矢（大于雪球、小于寒棘狐强化锥，保持投射物视觉层级）??*/
	private static final float SCALE = 0.85f;
	/** 渲染用物品栈（静态复用，避免每帧 new）??*/
	private static ItemStack CACHED_STACK;

	private final ItemRenderer itemRenderer;

	public MoonlightArrowRenderer(EntityRendererProvider.Context ctx) {
		super(ctx);
		this.itemRenderer = ctx.getItemRenderer();
	}

	@Override
	public void render(SpellMoonlightArrowEntity entity, float yaw, float tickDelta, PoseStack matrices,
	                   MultiBufferSource vertexConsumers, int light) {
		if (CACHED_STACK == null) {
			CACHED_STACK = new ItemStack(github.com.gengyoubo.sscfa.init.AddonItems.MOONLIGHT_ARROW_RENDER.get());
		}
		matrices.pushPose();
		matrices.scale(SCALE, SCALE, SCALE);
		// 模型箭头??+Z：??Y ??-yaw 对准水平朝向；绕 X ??-pitch（与寒棘狐冰锥同款惯例：实??pitch=atan2(-v.y) 存负值）
		matrices.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
		matrices.mulPose(Axis.XP.rotationDegrees(-entity.getXRot()));
		this.itemRenderer.renderStatic(CACHED_STACK, ItemDisplayContext.GROUND, light, OverlayTexture.NO_OVERLAY,
				matrices, vertexConsumers, entity.level(), entity.getId());
		matrices.popPose();
		super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
	}

	@Override
	public ResourceLocation getTextureLocation(SpellMoonlightArrowEntity entity) {
		return new ResourceLocation("minecraft", "textures/item/spectral_arrow.png");
	}
}


