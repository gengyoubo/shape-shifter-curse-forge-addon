package github.com.gengyoubo.sscfa.client.model;

import github.com.gengyoubo.sscfa.entity.GhostCatEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/** 幽灵野猫：复用野猫形态的 geo 与贴图。 */
public class GhostCatModel extends GeoModel<GhostCatEntity> {

    @Override
    public ResourceLocation getModelResource(GhostCatEntity entity) {
        return new ResourceLocation("my_addon", "geo/form_wild_cat_sp.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(GhostCatEntity entity) {
        return new ResourceLocation("my_addon", "textures/form_wild_cat_sp/form_wild_cat_sp.png");
    }

    @Override
    public ResourceLocation getAnimationResource(GhostCatEntity entity) {
        return new ResourceLocation("ssc_addon", "animations/axolotl_shifter.animation.json");
    }
}
