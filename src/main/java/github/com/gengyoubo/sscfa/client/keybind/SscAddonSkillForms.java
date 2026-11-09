package github.com.gengyoubo.sscfa.client.keybind;

import github.com.gengyoubo.sscfa.util.FormIdMigration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * 提供「特殊键位设置」二级菜单要列出?SSCA 形态清单（自动检测，非硬编码）??
 * <p>
 * 来??= {@link RegPlayerForms#playerForms} 中所有命名空间为 {@code my_addon} 的已注册形??
 * （SSCA 注册的形态全部如此），按注册顺序排列；如此新?数据包形态会自动出现?
 * 显示名走 SSC 图鉴使用的形态名称，与形态改变卷轴一致?
 */
public final class SscAddonSkillForms {

	/**
	 * 始终显示的额外形态：数据驱动形态（如契?ssc_form）在数据加载前不在注册表里，
	 * 列在这里可保证它和静态注册??SP 形态一样、即使在主菜单也能显示?
	 */
	private static final List<String> ALWAYS_SHOWN_FORMS = List.of(
			"familiar_fox_mancianima"
	);

	private SscAddonSkillForms() {
	}

	/** 自动检测要展示的形??path 列表（去重、保序）??*/
	public static List<String> getFormPaths() {
		LinkedHashSet<String> paths = new LinkedHashSet<>();
		try {
			for (net.onixary.shapeShifterCurseForge.form.FormDefinition form : net.onixary.shapeShifterCurseForge.form.FormRegistry.forms().values()) {
				if (form == null) {
					continue;
				}
				ResourceLocation id = form.id();
				if (id != null && "my_addon".equals(id.getNamespace())) {
					paths.add(id.getPath());
				}
			}
		} catch (Throwable ignored) {
			// 注册表异常时仅返回始终显示??
		}
		paths.addAll(ALWAYS_SHOWN_FORMS);
		return new ArrayList<>(paths);
	}

	/** 形态显示名（复用游戏内形态名 lang 键）?*/
	public static Component displayName(String formPath) {
		return Component.translatable("codex.form.my_addon." + FormIdMigration.canonicalPath(formPath) + ".name");
	}
}
