package github.com.gengyoubo.sscfa.client.renderer;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import github.com.gengyoubo.sscfa.util.FormIdMigration;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.onixary.shapeShifterCurseForge.api.PlayerSkinData;
import net.onixary.shapeShifterCurseForge.api.SscApi;
import net.onixary.shapeShifterCurseForge.client.render.FormTextureUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/** Tint exposed vanilla skin below the two SSCA allay overlays, including first-person cuffs. */
public final class AllaySkinColor {
    private static final Map<ResourceLocation, Source> SOURCES = new LinkedHashMap<>(8, 0.75f, true);
    private static final Map<Key, ResourceLocation> TINTED = new LinkedHashMap<>(64, 0.75f, true);

    private record Source(NativeImage image, int average) {}

    private record Key(ResourceLocation texture, int color, boolean reverse) {}

    private AllaySkinColor() {
    }

    public static void init(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new SimplePreparableReloadListener<Void>() {
            @Override
            protected Void prepare(ResourceManager manager, ProfilerFiller profiler) {
                return null;
            }

            @Override
            protected void apply(Void unused, ResourceManager manager, ProfilerFiller profiler) {
                clear();
            }
        });
        MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut logout) -> clear());
    }

    public static ResourceLocation texture(AbstractClientPlayer player, ResourceLocation original) {
        if (!RenderSystem.isOnRenderThread()) return original;
        ResourceLocation formId = SscApi.currentForm(player)
                .map(form -> ResourceLocation.tryParse(form.getFormId())).orElse(null);
        if (formId != null) formId = FormIdMigration.canonical(formId);
        if (formId == null || !formId.equals(FormIdentifiers.ALLAY_SP)
                && !formId.equals(FormIdentifiers.FALLEN_ALLAY_SP)) {
            return original;
        }
        PlayerSkinData skin = SscApi.currentSkin(player).orElse(null);
        if (skin == null || !skin.isEnableFormColor() || skin.isKeepOriginalSkin()) return original;
        FormTextureUtils.ColorSetting colors = skin.getFormColor();
        if (colors == null || colors.accentColor2() >>> 24 == 0) return original;
        Key key = new Key(original, colors.accentColor2(), colors.accent2GreyReverse());
        ResourceLocation cached = TINTED.get(key);
        if (cached != null) return cached;
        Source source = source(original);
        if (source == null) return original;
        NativeImage recolored = new NativeImage(source.image().getWidth(), source.image().getHeight(), false);
        try {
            for (int y = 0; y < recolored.getHeight(); y++) {
                for (int x = 0; x < recolored.getWidth(); x++) {
                    int pixel = source.image().getPixelRGBA(x, y);
                    int tinted = FormTextureUtils.processMaskChannel(pixel, 255, key.color(),
                            source.average(), key.reverse());
                    recolored.setPixelRGBA(x, y, (pixel & 0xff000000) | (tinted & 0xffffff));
                }
            }
            ResourceLocation result = Minecraft.getInstance().getTextureManager()
                    .register("ssca_allay_skin", new DynamicTexture(recolored));
            recolored = null;
            TINTED.put(key, result);
            while (TINTED.size() > 64) {
                var iterator = TINTED.entrySet().iterator();
                var oldest = iterator.next();
                Minecraft.getInstance().getTextureManager().release(oldest.getValue());
                iterator.remove();
            }
            return result;
        } finally {
            if (recolored != null) recolored.close();
        }
    }

    private static Source source(ResourceLocation id) {
        Source cached = SOURCES.get(id);
        if (cached != null) return cached;
        Minecraft client = Minecraft.getInstance();
        NativeImage image;
        var resource = client.getResourceManager().getResource(id);
        if (resource.isPresent()) {
            try (var stream = resource.get().open()) {
                image = NativeImage.read(stream);
            } catch (IOException ignored) {
                return null;
            }
        } else {
            int active = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
            RenderSystem.activeTexture(GL13.GL_TEXTURE0);
            int previous = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            try {
                var texture = client.getTextureManager().getTexture(id);
                if (texture == null) return null;
                RenderSystem.bindTexture(texture.getId());
                int width = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
                int height = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
                if (width < 64 || height < 32 || width > 4096 || height > 4096) return null;
                image = new NativeImage(width, height, false);
                image.downloadTexture(0, false);
            } finally {
                RenderSystem.bindTexture(previous);
                RenderSystem.activeTexture(active);
            }
        }
        long sum = 0;
        int count = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = image.getPixelRGBA(x, y);
                if (pixel >>> 24 != 0) {
                    sum += FormTextureUtils.getGreyScale(pixel);
                    count++;
                }
            }
        }
        Source result = new Source(image, count == 0 ? 255 : (int) (sum / count));
        SOURCES.put(id, result);
        while (SOURCES.size() > 8) {
            var iterator = SOURCES.entrySet().iterator();
            var oldest = iterator.next();
            oldest.getValue().image().close();
            iterator.remove();
        }
        return result;
    }

    private static void clear() {
        for (Source source : SOURCES.values()) source.image().close();
        SOURCES.clear();
        for (ResourceLocation id : TINTED.values()) Minecraft.getInstance().getTextureManager().release(id);
        TINTED.clear();
    }
}
