package github.com.gengyoubo.sscfa.client.particle;

import net.minecraft.client.Camera;

/** Only explicitly identified, nonessential effects opt into first-person avoidance. */
public interface OwnedDecoration extends ParticleOwnership {
    double ssca$cameraDistance(Camera camera, float tickDelta);

    float ssca$cameraVisibility(Camera camera, float tickDelta);

    float ssca$getRenderAlpha();

    void ssca$setRenderAlpha(float alpha);
}
