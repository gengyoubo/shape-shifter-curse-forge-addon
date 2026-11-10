package github.com.gengyoubo.sscfa.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import github.com.gengyoubo.sscfa.entity.TidalOrbEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** 荧光幼灵潮汐球渲染器：非拴人走原版飞行物品，拴人时绘制激活态潮涌核心。 */
public class TidalOrbRenderer extends ThrownItemRenderer<TidalOrbEntity> {
    private static final ResourceLocation WIND_TEX = new ResourceLocation("textures/entity/conduit/wind.png");
    private static final ResourceLocation WIND_VERTICAL_TEX = new ResourceLocation("textures/entity/conduit/wind_vertical.png");
    private static final ResourceLocation OPEN_EYE_TEX = new ResourceLocation("textures/entity/conduit/open_eye.png");

    private static final Vector3f C_CYAN = new Vector3f(0.20f, 0.78f, 0.92f);
    private static final Vector3f C_BLUE = new Vector3f(0.25f, 0.45f, 0.95f);
    private static final Vector3f C_LIGHT = new Vector3f(0.55f, 0.80f, 1.0f);

    private final ModelPart eye;
    private final ModelPart wind;

    public TidalOrbRenderer(EntityRendererProvider.Context context) {
        super(context, 1.0F, false);
        this.eye = context.bakeLayer(ModelLayers.CONDUIT_EYE);
        this.wind = context.bakeLayer(ModelLayers.CONDUIT_WIND);
    }

    @Override
    public void render(TidalOrbEntity entity, float yaw, float tickDelta, PoseStack matrices,
                       MultiBufferSource buffers, int light) {
        spawnHoverParticlesClient(entity);
        if (!entity.isTetherActive()) {
            super.render(entity, yaw, tickDelta, matrices, buffers, light);
            return;
        }
        long time = entity.level().getGameTime();
        int fullBright = 0xF000F0;

        matrices.pushPose();
        matrices.translate(0.0, 0.15, 0.0);
        matrices.scale(0.75f, 0.75f, 0.75f);

        int frame = (int) (time / 22) % 3;
        matrices.pushPose();
        matrices.mulPose(Axis.YP.rotationDegrees(((float) time + tickDelta) * 2.5f));
        if (frame == 1) {
            matrices.mulPose(Axis.XP.rotationDegrees(90f));
        } else if (frame == 2) {
            matrices.mulPose(Axis.ZP.rotationDegrees(90f));
        }
        ResourceLocation windTexture = (frame == 0) ? WIND_TEX : WIND_VERTICAL_TEX;
        this.wind.render(matrices, buffers.getBuffer(RenderType.entityCutoutNoCull(windTexture)),
                fullBright, OverlayTexture.NO_OVERLAY);
        matrices.popPose();

        matrices.pushPose();
        matrices.mulPose(this.entityRenderDispatcher.cameraOrientation());
        matrices.scale(0.5f, 0.5f, 0.5f);
        this.eye.render(matrices, buffers.getBuffer(RenderType.entityTranslucent(OPEN_EYE_TEX)),
                fullBright, OverlayTexture.NO_OVERLAY);
        matrices.popPose();

        matrices.popPose();
    }

    private void spawnHoverParticlesClient(TidalOrbEntity entity) {
        if (entity.clientParticleGate == entity.tickCount) return;
        entity.clientParticleGate = entity.tickCount;
        if (!entity.isTetherActive()) return;

        java.util.Random random = new java.util.Random();
        int t = entity.tickCount;
        double x = entity.getX();
        double y = entity.getY();
        double z = entity.getZ();
        var level = entity.level();

        if (level.getFluidState(entity.blockPosition()).is(FluidTags.WATER)) {
            for (int arm = 0; arm < 3; arm++) {
                double angle = t * 0.16 + arm * Math.PI * 2 / 3;
                double vx = x + Math.cos(angle) * 0.9;
                double vz = z + Math.sin(angle) * 0.9;
                BlockPos pos = BlockPos.containing(vx, y, vz);
                if (level.getFluidState(pos).is(FluidTags.WATER)) {
                    level.addParticle(ParticleTypes.CURRENT_DOWN, vx, y, vz, 0, -0.02, 0);
                }
            }
        }

        double rotation = t * 0.12;
        double orbitRadius = 1.1;
        for (int k = 0; k < 3; k++) {
            double angle = rotation + k * (Math.PI * 2 / 3);
            double ox = x + Math.cos(angle) * orbitRadius;
            double oz = z + Math.sin(angle) * orbitRadius;
            double oy = y + Math.sin(t * 0.15 + k * 2.0) * 0.25;
            Vector3f color = (k == 0) ? C_CYAN : (k == 1) ? C_BLUE : C_LIGHT;
            for (int i = 0; i < 4; i++) {
                Vec3 offset = randomInSphere(0.22, random);
                level.addParticle(new DustParticleOptions(color, 1.5f),
                        ox + offset.x, oy + offset.y, oz + offset.z, 0, 0, 0);
            }
        }
        if (t % 2 == 0) {
            double dx = (random.nextDouble() - 0.5) * 0.5;
            double dz = (random.nextDouble() - 0.5) * 0.5;
            level.addParticle(ParticleTypes.FALLING_WATER, x + dx, y + 0.15, z + dz, 0, 0, 0);
        }

        double rotation2 = t * 0.06;
        int boundaryCount = 6;
        double softRadius = TidalOrbEntity.tetherSoftRadius();
        for (int k = 0; k < boundaryCount; k++) {
            double angle = rotation2 + k * (Math.PI * 2 / boundaryCount);
            double ox = x + Math.cos(angle) * softRadius;
            double oz = z + Math.sin(angle) * softRadius;
            double oy = y + Math.sin(t * 0.12 + k) * 0.3;
            Vector3f color = (k % 3 == 0) ? C_CYAN : (k % 3 == 1) ? C_BLUE : C_LIGHT;
            for (int i = 0; i < 3; i++) {
                Vec3 offset = randomInSphere(0.2, random);
                level.addParticle(new DustParticleOptions(color, 1.5f),
                        ox + offset.x, oy + offset.y, oz + offset.z, 0, 0, 0);
            }
        }
    }

    private static Vec3 randomInSphere(double radius, java.util.Random random) {
        double r = radius * Math.cbrt(random.nextDouble());
        double theta = random.nextDouble() * 2 * Math.PI;
        double phi = Math.acos(2 * random.nextDouble() - 1);
        double sinPhi = Math.sin(phi);
        return new Vec3(r * sinPhi * Math.cos(theta), r * Math.cos(phi), r * sinPhi * Math.sin(theta));
    }
}
