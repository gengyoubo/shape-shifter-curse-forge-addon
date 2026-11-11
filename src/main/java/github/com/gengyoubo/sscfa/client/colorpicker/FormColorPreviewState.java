package github.com.gengyoubo.sscfa.client.colorpicker;

import net.onixary.shapeShifterCurseForge.client.render.FormTextureUtils;

/** Read the working palette without writing preview values to player components. */
public interface FormColorPreviewState {
    boolean ssca$colorEnabled();
    boolean ssca$keepOriginalSkin();
    FormTextureUtils.ColorSetting ssca$previewColors();
}
