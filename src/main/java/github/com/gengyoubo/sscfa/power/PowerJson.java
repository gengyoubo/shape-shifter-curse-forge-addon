package github.com.gengyoubo.sscfa.power;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.power.FormPowerDefinition;
import net.onixary.shapeShifterCurseForge.power.FormPowerRegistry;
import org.jetbrains.annotations.Nullable;

/**
 * 按 ID 找 SSC Forge 加载的 power JSON，沿用 Apoli 的子能力命名：{@code apoli:multiple} 里键为 {@code activate}
 * 的子能力，ID 是 {@code <父 ID>_activate}（例如 {@code my_addon:form_axolotl_sp_play_dead_activate}）。
 * SSC Forge 1.10.0.6 起回调给 {@code SscPowerType} 的也是这个 ID。技能、冷却、资源条都按这个 ID 查。
 */
public final class PowerJson {
	private static final String MULTIPLE = "apoli:multiple";

	private PowerJson() {}

	/** 该 ID 的定义（顶层能力或子能力），不看玩家是否拥有；不存在返回 null。 */
	@Nullable
	public static JsonObject definition(ResourceLocation id) {
		return lookup(null, id);
	}

	/** 玩家当前形态上该 ID 的 JSON（顶层能力或子能力）；没有返回 null。 */
	@Nullable
	public static JsonObject find(Player player, String id) {
		ResourceLocation parsed = ResourceLocation.tryParse(id);
		return parsed == null ? null : lookup(player, parsed);
	}

	/** 玩家当前形态是否拥有该 ID 的能力（顶层能力或子能力）。 */
	public static boolean has(Player player, ResourceLocation id) {
		return lookup(player, id) != null;
	}

	private static JsonObject lookup(@Nullable Player player, ResourceLocation id) {
		if (player == null || FormPowerRegistry.has(player, id)) {
			FormPowerDefinition direct = FormPowerRegistry.get(id);
			if (direct != null) return direct.data();
		}
		String path = id.getPath();
		for (int cut = path.lastIndexOf('_'); cut > 0; cut = path.lastIndexOf('_', cut - 1)) {
			ResourceLocation parentId = new ResourceLocation(id.getNamespace(), path.substring(0, cut));
			if (player != null && !FormPowerRegistry.has(player, parentId)) continue;
			FormPowerDefinition parent = FormPowerRegistry.get(parentId);
			if (parent == null || !MULTIPLE.equals(FormPowerRegistry.typeOf(parent.data()))) continue;
			JsonElement child = parent.data().get(path.substring(cut + 1));
			if (child != null && child.isJsonObject()) return child.getAsJsonObject();
		}
		return null;
	}
}
