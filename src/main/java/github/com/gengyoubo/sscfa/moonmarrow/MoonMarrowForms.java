package github.com.gengyoubo.sscfa.moonmarrow;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.onixary.shapeShifterCurseForge.form.FormDefinition;
import net.onixary.shapeShifterCurseForge.form.FormManager;
import net.onixary.shapeShifterCurseForge.form.FormRegistry;
import net.onixary.shapeShifterCurseForge.form.TransformManager;
import net.onixary.shapeShifterCurseForge.other.cursedmoon.CursedMoonService;
import org.jetbrains.annotations.Nullable;

/** 月髓环与 SSC Forge 形态系统之间的薄适配层（替代 Fabric 版的 RegPlayerForms / TransformManager / CursedMoon）。 */
public final class MoonMarrowForms {
	private MoonMarrowForms() {}

	@Nullable
	public static ResourceLocation currentFormId(Player player) {
		FormDefinition current = FormManager.current(player);
		return current == null ? null : current.id();
	}

	public static boolean isRegistered(@Nullable ResourceLocation formId) {
		return formId != null && FormRegistry.get(formId) != null;
	}

	public static boolean isCursedMoonNight(Level level) {
		return CursedMoonService.isCursedMoonDay(level) && CursedMoonService.isNight(level);
	}

	public static boolean isTransforming(Player player) {
		return TransformManager.isTransforming(player);
	}

	/** 带黑屏淡入淡出演出的变形；SSC Forge 在演出期间自行锁定移动与跳跃。 */
	public static boolean startTransform(ServerPlayer player, ResourceLocation target) {
		return TransformManager.startTransform(player, target);
	}
}
