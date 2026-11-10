package github.com.gengyoubo.sscfa.client.particle;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.lwjgl.opengl.GL11;

/** Faded particles blend with the scene without becoming opaque occluders in its depth buffer. */
public final class FadedParticleBuffer {
    private final BufferBuilder vertices = new BufferBuilder(1024);
    private boolean building;

    public BufferBuilder vertices() {
        if (!building) {
            vertices.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_LIGHTMAP_COLOR);
            building = true;
        }
        return vertices;
    }

    public BufferBuilder.RenderedBuffer end() {
        if (!building) return null;
        building = false;
        return vertices.end();
    }

    public void draw() {
        var built = end();
        if (built == null) return;
        if (built.isEmpty()) {
            built.release();
            return;
        }
        boolean writeDepth = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        RenderSystem.depthMask(false);
        try {
            BufferUploader.drawWithShader(built);
        } finally {
            RenderSystem.depthMask(writeDepth);
        }
    }

    public void discard() {
        var built = end();
        if (built != null) built.release();
    }
}
