package github.com.gengyoubo.sscfa.client.renderer.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import github.com.gengyoubo.sscfa.entity.LunarSpiritEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * 月灵发光 overlay 层（jackcooper）：与本体同 geo，仅换三色变体对应的 *_overlay 贴图。
 * 蜘蛛眼同款 RenderType.eyes()（无光晕 + 加法透明 + 黑暗中可见），
 * 月纹 / 翅脉自发光；写法参照 WitchFamiliarEyesLayer。
 */
public class LunarSpiritOverlayLayer extends GeoRenderLayer<LunarSpiritEntity> {

	private static final ResourceLocation[] OVERLAY_TEXTURES = {
			new ResourceLocation("my_addon", "textures/entity/moon_spirit_pink_overlay.png"),
			new ResourceLocation("my_addon", "textures/entity/moon_spirit_cyan_overlay.png"),
			new ResourceLocation("my_addon", "textures/entity/moon_spirit_green_overlay.png")
	};

	public LunarSpiritOverlayLayer(GeoEntityRenderer<LunarSpiritEntity> renderer) {
		super(renderer);
	}

	@Override
	public void render(PoseStack poseStack, LunarSpiritEntity animatable,
	                   BakedGeoModel bakedModel, RenderType renderType,
	                   MultiBufferSource bufferSource, VertexConsumer buffer,
	                   float partialTick, int packedLight, int packedOverlay) {
		int variant = Math.max(0, Math.min(2, animatable.getVariant()));
		RenderType eyesRenderType = RenderType.eyes(OVERLAY_TEXTURES[variant]);
		VertexConsumer eyesBuffer = bufferSource.getBuffer(eyesRenderType);

		getRenderer().reRender(
				bakedModel,
				poseStack,
				bufferSource,
				animatable,
				eyesRenderType,
				eyesBuffer,
				partialTick,
				15728640, // 全亮度（LightTexture.FULL_BRIGHT）
				LivingEntityRenderer.getOverlayCoords(animatable, 0),
				1.0f, 1.0f, 1.0f, 1.0f
		);
	}
}
