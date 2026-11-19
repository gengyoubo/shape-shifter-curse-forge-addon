package github.com.gengyoubo.sscfa.power;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.onixary.shapeShifterCurseForge.api.SscManaApi;
import net.onixary.shapeShifterCurseForge.api.SscManaMaximumEvent;
import net.onixary.shapeShifterCurseForge.api.registry.SscPowerType;
import net.onixary.shapeShifterCurseForge.power.FormPowerRegistry;
import net.onixary.shapeShifterCurseForge.power.FormPowerRuntime;

/**
 * {@code ssc_addon:conditional_mana_attribute}：条件成立时修改 SSC 法力池上限。
 *
 * <p>Fabric 版把修饰符写入法力组件并每 tick 对账；SSC Forge 的上限按查询重新计算
 * （{@link SscManaMaximumEvent}），因此这里不保存任何状态：每次计算上限时遍历玩家激活的
 * 该类型能力，条件成立者参与计算，条件失效或能力移除后下一次查询即恢复。
 * 计算顺序与 Fabric {@code ModifierList.apply} 一致：先累加全部 {@code add}，再连乘全部
 * {@code multiply}，最后累加全部 {@code add_total}。{@code player_side} 在 Forge 端没有区别。</p>
 *
 * <p>{@code regen_mana_modifier} 尚未支持（附属 JSON 未使用）；SSC Forge 的自然回复没有对应的速率模型。</p>
 */
public final class ConditionalManaAttributePower implements SscPowerType {
    public static final ResourceLocation TYPE = ResourceLocation.fromNamespaceAndPath("ssc_addon", "conditional_mana_attribute");
    private static boolean registered;

    public static synchronized void registerEvents() {
        if (registered) return;
        registered = true;
        MinecraftForge.EVENT_BUS.addListener(ConditionalManaAttributePower::onManaMaximum);
    }

    private static void onManaMaximum(SscManaMaximumEvent event) {
        ServerPlayer player = event.getEntity();
        final double[] add = {0}, multiply = {1}, addTotal = {0};
        final boolean[] any = {false};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (!TYPE.toString().equals(FormPowerRegistry.typeOf(power))
                    || !power.has("max_mana_modifier") || !power.get("max_mana_modifier").isJsonObject()) return;
            JsonObject condition = power.has("condition") && power.get("condition").isJsonObject()
                    ? power.getAsJsonObject("condition") : null;
            if (!FormPowerRuntime.test(player, player, condition)) return;
            JsonObject modifier = power.getAsJsonObject("max_mana_modifier");
            add[0] += FormPowerRuntime.floatValue(modifier, "add", 0);
            multiply[0] *= FormPowerRuntime.floatValue(modifier, "multiply", 1);
            addTotal[0] += FormPowerRuntime.floatValue(modifier, "add_total", 0);
            any[0] = true;
        });
        if (!any[0]) return;
        double maximum = (event.getMaximum() + add[0]) * multiply[0] + addTotal[0];
        if (Double.isFinite(maximum)) event.setMaximum((float) maximum);
    }

    /** 获得或失去能力时立即裁剪并同步，不必等到 SSC 的 tick 检查。 */
    @Override
    public void onAdded(ServerPlayer player, ResourceLocation id, JsonObject data) {
        SscManaApi.synchronizeMana(player);
    }

    @Override
    public void onRemoved(ServerPlayer player, ResourceLocation id, JsonObject data) {
        SscManaApi.synchronizeMana(player);
    }
}
