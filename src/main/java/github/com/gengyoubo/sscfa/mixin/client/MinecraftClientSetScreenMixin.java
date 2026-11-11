package github.com.gengyoubo.sscfa.mixin.client;

import github.com.gengyoubo.sscfa.client.colorpicker.AdvancedColorScreen;
import github.com.gengyoubo.sscfa.config.SSCAddonConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 当 SSCA 颜色编辑开关 ON 时，拦截 setScreen(FormColorSelectMenu)，替换为 AdvancedColorScreen。
 * 用类名字符串判断避免对 SSC 的硬依赖。
 */
@Mixin(Minecraft.class)
public abstract class MinecraftClientSetScreenMixin {

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true, require = 0)
    private void ssc_addon$replaceColorMenu(Screen screen, CallbackInfo ci) {
        if (screen == null) return;
        if (screen instanceof AdvancedColorScreen) return;
        if (!SSCAddonConfig.client().enableColorEditor) return;

        String name = screen.getClass().getName();
        if (!"net.onixary.shapeShifterCurseForge.client.codex.FormColorSelectMenu".equals(name)
                && !"net.onixary.shapeShifterCurseForge.client.codex.FormColorSelectMenuV2".equals(name)) {
            return;
        }

        // FormColorSelectMenu 构造时已经置位 SSC 的静态/临时状态；直接 cancel 而不清理会导致下次再开时
        // 触发 “already in use”。先反射还原，再切换到自己的界面。
        cleanupSscColorMenuState(screen);
        Screen parent = ((Minecraft) (Object) this).screen;
        ((Minecraft) (Object) this).setScreen(new AdvancedColorScreen(parent));
        ci.cancel();
    }

    /**
     * 反射清理 SSC 的 FormColorSelectMenu / FormTextureUtils 状态，使其表现得像 close() 已经调用。
     */
    @Unique
    private static void cleanupSscColorMenuState(Screen formColorSelectMenu) {
        try {
            Class<?> menuCls = formColorSelectMenu.getClass();
            setBooleanField(menuCls, formColorSelectMenu, "isUsingTempTexture", false);
            setBooleanField(menuCls, formColorSelectMenu, "isUsingTempModel", false);
            setBooleanField(menuCls, formColorSelectMenu, "isUsingSkinOverrider", false);
            setStaticField(menuCls, "instance", null);

            Class<?> texUtils = Class.forName("net.onixary.shapeShifterCurseForge.client.render.FormTextureUtils");
            setStaticBoolean(texUtils, "useTempFormTexture", false);
            setStaticField(texUtils, "tempFormTextureProcessor", null);
            setStaticField(texUtils, "tempFormModelProcessor", null);
            setStaticField(texUtils, "tempCustomSkinConfigOverrider", null);
        } catch (Throwable ignored) {
            // 反射失败不应阻断界面切换。
        }
    }

    @Unique
    private static void setBooleanField(Class<?> owner, Object target, String name, boolean value) {
        try {
            java.lang.reflect.Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            field.setBoolean(target, value);
        } catch (Throwable ignored) {
        }
    }

    @Unique
    private static void setStaticBoolean(Class<?> owner, String name, boolean value) {
        try {
            java.lang.reflect.Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            field.setBoolean(null, value);
        } catch (Throwable ignored) {
        }
    }

    @Unique
    private static void setStaticField(Class<?> owner, String name, Object value) {
        try {
            java.lang.reflect.Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            field.set(null, value);
        } catch (Throwable ignored) {
        }
    }
}
