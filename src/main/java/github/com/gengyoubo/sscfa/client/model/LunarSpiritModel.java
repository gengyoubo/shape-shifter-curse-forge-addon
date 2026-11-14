package github.com.gengyoubo.sscfa.client.model;

import github.com.gengyoubo.sscfa.entity.LunarSpiritEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * 月灵模型（jackcooper）：引用 Blockbench 生成的一套资源（my_addon 命名空间）——
 * geo（球身 + 四片翅膀 + 月冠，7 骨骼）+ 三段动画（summon/fly/idle）+ 三色变体贴图。
 * 贴图按实体 VARIANT 切换（粉/蓝/绿）。
 */
public class LunarSpiritModel extends GeoModel<LunarSpiritEntity> {
	private static final ResourceLocation MODEL = new ResourceLocation("my_addon", "geo/moon_spirit.geo.json");
	private static final ResourceLocation ANIMATION = new ResourceLocation("my_addon", "animations/moon_spirit.animation.json");
	private static final ResourceLocation PINK = new ResourceLocation("my_addon", "textures/entity/moon_spirit_pink.png");
	private static final ResourceLocation CYAN = new ResourceLocation("my_addon", "textures/entity/moon_spirit_cyan.png");
	private static final ResourceLocation GREEN = new ResourceLocation("my_addon", "textures/entity/moon_spirit_green.png");

	@Override
	public ResourceLocation getModelResource(LunarSpiritEntity entity) {
		return MODEL;
	}

	@Override
	public ResourceLocation getTextureResource(LunarSpiritEntity entity) {
		// 三色变体贴图（0 粉 / 1 蓝 / 2 绿）
		return switch (entity.getVariant()) {
			case LunarSpiritEntity.VARIANT_PINK -> PINK;
			case LunarSpiritEntity.VARIANT_CYAN -> CYAN;
			default -> GREEN;
		};
	}

	@Override
	public ResourceLocation getAnimationResource(LunarSpiritEntity entity) {
		return ANIMATION;
	}
}
