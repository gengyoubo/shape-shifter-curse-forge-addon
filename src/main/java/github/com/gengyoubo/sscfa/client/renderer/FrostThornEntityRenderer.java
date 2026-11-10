package github.com.gengyoubo.sscfa.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import github.com.gengyoubo.sscfa.SscAddon;
import github.com.gengyoubo.sscfa.entity.FrostThornEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** 冰锥 3D 渲染器：按 CustomModelData 阶段切换材质。 */
public class FrostThornEntityRenderer extends EntityRenderer<FrostThornEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("textures/atlas/blocks.png");
    private static final ItemStack[] CACHED_STACKS = new ItemStack[3];

    private final ItemRenderer itemRenderer;

    public FrostThornEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(FrostThornEntity entity, float yaw, float tickDelta, PoseStack matrices,
                       MultiBufferSource buffers, int light) {
        matrices.pushPose();
        float scale = entity.getLevel() > 0 ? 0.25f * (1.0f + entity.getLevel()) : 0.48f;
        matrices.scale(scale, scale, scale);
        matrices.mulPose(Axis.YP.rotationDegrees(-entity.getYRot() + 180.0F));
        matrices.mulPose(Axis.XP.rotationDegrees(-entity.getXRot()));
        matrices.translate((8.0 - 1.5) / 16.0, (8.0 - 0.5) / 16.0, (8.0 - 3.0) / 16.0);

        int stage = Math.max(0, Math.min(2, entity.getStage()));
        ItemStack stack = CACHED_STACKS[stage];
        if (stack == null) {
            stack = new ItemStack(SscAddon.FROST_THORN);
            stack.getOrCreateTag().putInt("CustomModelData", stage + 1);
            CACHED_STACKS[stage] = stack;
        }
        this.itemRenderer.renderStatic(stack, ItemDisplayContext.GROUND, light, OverlayTexture.NO_OVERLAY,
                matrices, buffers, entity.level(), entity.getId());
        matrices.popPose();
        super.render(entity, yaw, tickDelta, matrices, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(FrostThornEntity entity) {
        return TEXTURE;
    }
}
