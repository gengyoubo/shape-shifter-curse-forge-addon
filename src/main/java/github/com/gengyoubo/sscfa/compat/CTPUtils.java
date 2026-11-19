package github.com.gengyoubo.sscfa.compat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

/**
 * 形态药水（CTP）识别。SSC Forge 的动态酿造配方在产物上写入字符串
 * {@code targetForm}（与 Fabric 版 {@code CTPUtils} 相同的键），饮用时据此变形。
 */
public final class CTPUtils {
    /** 与 {@code net.onixary.shapeShifterCurseForge.other.brew.BrewingRecipeUtils.TARGET_FORM_KEY} 一致。 */
    public static final String TARGET_FORM_KEY = "targetForm";

    private CTPUtils() {}

    /** 药水携带的目标形态；无标签、键缺失、类型不符或 ID 无法解析时返回 null（SSC 同样不会据此变形）。 */
    public static ResourceLocation getCTPFormIDFromNBT(CompoundTag tag) {
        if (tag == null || !tag.contains(TARGET_FORM_KEY, Tag.TAG_STRING)) return null;
        String raw = tag.getString(TARGET_FORM_KEY);
        return raw.isBlank() ? null : ResourceLocation.tryParse(raw);
    }
}
