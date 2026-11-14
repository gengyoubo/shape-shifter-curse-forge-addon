package github.com.gengyoubo.sscfa.client.model;

import github.com.gengyoubo.sscfa.entity.AxolotlShifterEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class AxolotlShifterModel extends GeoModel<AxolotlShifterEntity> {

	private static final ResourceLocation MODEL = new ResourceLocation("ssc_addon", "geo/axolotl_shifter.geo.json");
	private static final ResourceLocation TEXTURE = new ResourceLocation("ssc_addon", "textures/entity/axolotl_shifter.png");
	private static final ResourceLocation ANIMATION = new ResourceLocation("ssc_addon", "animations/axolotl_shifter.animation.json");

	@Override
	public ResourceLocation getModelResource(AxolotlShifterEntity entity) {
		return MODEL;
	}

	@Override
	public ResourceLocation getTextureResource(AxolotlShifterEntity entity) {
		return TEXTURE;
	}

	@Override
	public ResourceLocation getAnimationResource(AxolotlShifterEntity entity) {
		return ANIMATION;
	}
}
