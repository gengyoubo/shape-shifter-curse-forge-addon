package github.com.gengyoubo.sscfa.init;

import github.com.gengyoubo.sscfa.criteria.OnTransformAddonForm;
import net.minecraft.advancements.CriteriaTriggers;

/** 自定义成就触发器。原版注册表不是线程安全的，{@link #init()} 只能在 enqueueWork 里调用。 */
public final class AddonCriteria {
    public static final OnTransformAddonForm ON_TRANSFORM_ADDON_FORM = CriteriaTriggers.register(new OnTransformAddonForm());

    private AddonCriteria() {}

    /** 触发类加载，完成注册。 */
    public static void init() {}
}
