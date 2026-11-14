package github.com.gengyoubo.sscfa.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import github.com.gengyoubo.sscfa.SscAddon;
import github.com.gengyoubo.sscfa.entity.ThrownWaterSpearEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class ThrownWaterSpearEntityRenderer extends EntityRenderer<ThrownWaterSpearEntity> {
    private static final ItemStack CACHED_THROWING_STACK = createThrowingStack();
    private final ItemRenderer itemRenderer;

    private static ItemStack createThrowingStack() {
        ItemStack stack = new ItemStack(SscAddon.WATER_SPEAR);
        stack.getOrCreateTag().putInt("CustomModelData", 1);
        return stack;
    }

    public ThrownWaterSpearEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(ThrownWaterSpearEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        float yaw = entity.getYRot();
        float pitch = entity.getXRot();
        Vec3 velocity = entity.getDeltaMovement();
        double horizontal = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        if (Math.abs(yaw) < 1.0e-4 && Math.abs(pitch) < 1.0e-4 && (horizontal > 1.0e-6 || velocity.y != 0)) {
            yaw = (float) (Mth.atan2(velocity.x, velocity.z) * (180.0 / Math.PI));
            pitch = (float) (Mth.atan2(velocity.y, horizontal) * (180.0 / Math.PI));
        }
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(pitch - 90.0F));
        this.itemRenderer.renderStatic(CACHED_THROWING_STACK, ItemDisplayContext.GROUND, packedLight,
                OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.getId());
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(ThrownWaterSpearEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
