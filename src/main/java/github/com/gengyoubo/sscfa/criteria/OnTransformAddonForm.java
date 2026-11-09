package github.com.gengyoubo.sscfa.criteria;

import com.google.gson.JsonObject;
import github.com.gengyoubo.sscfa.util.FormIdMigration;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * 附属形态变身成就触发器（{@code my_addon:on_transform_addon_form}）。条件可选 {@code form_id}；
 * 两边都按 {@link FormIdMigration#canonical} 换成改名后的 ID 再比较，所以成就 JSON 写新 ID、
 * SSC Forge 实际用旧 ID 也能匹配。
 */
public class OnTransformAddonForm extends SimpleCriterionTrigger<OnTransformAddonForm.Condition> {
    public static final ResourceLocation ID = new ResourceLocation("my_addon", "on_transform_addon_form");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    public void trigger(ServerPlayer player, ResourceLocation formId) {
        ResourceLocation canonical = FormIdMigration.canonical(formId);
        trigger(player, condition -> condition.matches(canonical));
    }

    @Override
    protected Condition createInstance(JsonObject obj, ContextAwarePredicate playerPredicate, DeserializationContext predicateDeserializer) {
        ResourceLocation formId = null;
        if (obj.has("form_id")) {
            formId = FormIdMigration.canonical(new ResourceLocation(obj.get("form_id").getAsString()));
        }
        return new Condition(formId, playerPredicate);
    }

    public static class Condition extends AbstractCriterionTriggerInstance {
        @Nullable
        private final ResourceLocation formId;

        public Condition(@Nullable ResourceLocation formId) {
            this(formId, ContextAwarePredicate.ANY);
        }

        public Condition(@Nullable ResourceLocation formId, ContextAwarePredicate playerPredicate) {
            super(ID, playerPredicate);
            this.formId = formId;
        }

        public boolean matches(ResourceLocation triggered) {
            return formId == null || formId.equals(triggered);
        }
    }
}
