package github.com.gengyoubo.sscfa.energy;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.api.SscApi;
import net.onixary.shapeShifterCurseForge.power.FormActivePowerService;
import net.onixary.shapeShifterCurseForge.power.FormPowerRegistry;

/** 通用能量药水对前置模组的资源读写适配。 */
public final class EnergyRestoration {
    private static final ResourceLocation[] RESOURCES = {
            new ResourceLocation("my_addon", "form_allay_sp_mana_resource"),
            new ResourceLocation("my_addon", "form_bat_desmodus_blood_resource"),
            new ResourceLocation("my_addon", "form_anubis_wolf_sp_soul_energy"),
            new ResourceLocation("my_addon", "form_snow_fox_sp_resource"),
            new ResourceLocation("my_addon", "form_bat_parasitic_fruit_seed_energy")
    };

    private EnergyRestoration() {}

    private static ResourceLocation activeResource(Player player) {
        // 果蝠种子条优先；该资源上限较低，每瓶回复量也不同。
        if (FormPowerRegistry.has(player, RESOURCES[4]) || hasActiveChild(player, RESOURCES[4])) return RESOURCES[4];
        for (int i = 0; i < RESOURCES.length - 1; i++) {
            ResourceLocation id = RESOURCES[i];
            if (FormPowerRegistry.has(player, id) || hasActiveChild(player, id)) return id;
        }
        return null;
    }

    private static boolean hasActiveChild(Player player, ResourceLocation id) {
        String path = id.getPath();
        for (int cut = path.lastIndexOf('_'); cut > 0; cut = path.lastIndexOf('_', cut - 1)) {
            ResourceLocation parent = new ResourceLocation(id.getNamespace(), path.substring(0, cut));
            if (!FormPowerRegistry.has(player, parent)) continue;
            var definition = FormPowerRegistry.get(parent);
            if (definition == null || !"apoli:multiple".equals(FormPowerRegistry.typeOf(definition.data()))) continue;
            var child = definition.data().get(path.substring(cut + 1));
            if (child != null && child.isJsonObject()
                    && "apoli:resource".equals(FormPowerRegistry.typeOf(child.getAsJsonObject()))) return true;
        }
        return false;
    }

    private static boolean hasMana(Player player) {
        boolean[] active = {false};
        FormPowerRegistry.visitActive(player, (id, data) -> {
            if ("shape-shifter-curse:mana_type_power".equals(FormPowerRegistry.typeOf(data))) active[0] = true;
        });
        return active[0];
    }

    public static boolean canRestore(Player player) {
        String form = SscApi.currentForm(player).map(data -> data.getFormId()).orElse("");
        if (form.equals("my_addon:ocelot_nova")) return false;
        return activeResource(player) != null || hasMana(player);
    }

    public static void restore(ServerPlayer player, double scale) {
        if (!canRestore(player)) return;
        ResourceLocation resource = activeResource(player);
        if (resource != null) {
            int base = resource.equals(RESOURCES[4]) ? 2 : 25;
            int amount = Math.max(1, (int) Math.round(base * scale));
            JsonObject modifier = new JsonObject();
            modifier.addProperty("operation", "addition");
            modifier.addProperty("value", amount);
            FormActivePowerService.modifyResource(player, resource, modifier);
        } else {
            FormActivePowerService.gainMana(player, (float) (25 * scale));
        }
    }
}
