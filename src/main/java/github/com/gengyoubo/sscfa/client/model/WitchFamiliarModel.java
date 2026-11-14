package github.com.gengyoubo.sscfa.client.model;

import github.com.gengyoubo.sscfa.entity.WitchFamiliarEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * 女巫使魔模型 - 复用原版使魔的3D模型，使用专属纹理
 */
public class WitchFamiliarModel extends GeoModel<WitchFamiliarEntity> {

	private static final ResourceLocation MODEL = new ResourceLocation("ssc_addon", "geo/witch_familiar.geo.json");
	private static final ResourceLocation TEXTURE = new ResourceLocation("ssc_addon", "textures/entity/witch_familiar.png");
	private static final ResourceLocation ANIMATION = new ResourceLocation("ssc_addon", "animations/witch_familiar.animation.json");

	@Override
	public ResourceLocation getModelResource(WitchFamiliarEntity entity) {
		// 使用专属Geo模型（带正确骨骼层级的使魔模型）
		return MODEL;
	}

	@Override
	public ResourceLocation getTextureResource(WitchFamiliarEntity entity) {
		return TEXTURE;
	}

	@Override
	public ResourceLocation getAnimationResource(WitchFamiliarEntity entity) {
		// 使用附属包女巫使魔专属动画（基于原版使魔四足动作）
		return ANIMATION;
	}
}
