package github.com.gengyoubo.sscfa.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.form.FormDefinition;
import net.onixary.shapeShifterCurseForge.form.FormManager;
import org.jetbrains.annotations.Nullable;

/**
 * 形态判断工具。SSC Forge 的附属形态沿用旧 ID（如 {@code my_addon:axolotl_sp}），{@link FormIdentifiers} 用改名后的新 ID；
 * 两边都先经 {@link FormIdMigration#canonical} 换成新 ID 再比较。
 */
public final class FormUtils {
	// 通过实体类型 ID 判断，避免直接引用 SSC 的实体类
	private static final ResourceLocation TRANSFORMATIVE_WOLF_TYPE_ID = new ResourceLocation("shape-shifter-curse", "t_wolf");

	private FormUtils() {}

	@Nullable
	public static FormDefinition getCurrentForm(LivingEntity entity) {
		return entity instanceof Player player ? FormManager.current(player) : null;
	}

	/** 当前形态的新 ID（旧 ID 已映射）；非玩家或无形态返回 null。 */
	@Nullable
	public static ResourceLocation currentFormId(LivingEntity entity) {
		FormDefinition form = getCurrentForm(entity);
		return form == null ? null : FormIdMigration.canonical(form.id());
	}

	public static boolean isForm(LivingEntity entity, ResourceLocation formId) {
		ResourceLocation current = currentFormId(entity);
		return current != null && current.equals(FormIdMigration.canonical(formId));
	}

	public static boolean isAnyForm(LivingEntity entity, ResourceLocation... formIds) {
		ResourceLocation current = currentFormId(entity);
		if (current == null) return false;
		for (ResourceLocation formId : formIds) {
			if (current.equals(FormIdMigration.canonical(formId))) return true;
		}
		return false;
	}

	public static boolean isFamiliarFoxSP(LivingEntity entity) { return isForm(entity, FormIdentifiers.FAMILIAR_FOX_SP); }
	public static boolean isFamiliarFoxRed(LivingEntity entity) { return isForm(entity, FormIdentifiers.FAMILIAR_FOX_RED); }

	/** 使魔系形态（使魔SP / 进化使魔 / 契灵 / 红堕落使魔）。 */
	public static boolean isFamiliarFoxFamily(LivingEntity entity) {
		return isAnyForm(entity, FormIdentifiers.FAMILIAR_FOX_SP, FormIdentifiers.UPGRADE_FAMILIAR_FOX,
				FormIdentifiers.FAMILIAR_FOX_RED, FormIdentifiers.FAMILIAR_FOX_MANCIANIMA);
	}

	public static boolean isSnowFoxSP(LivingEntity entity) { return isForm(entity, FormIdentifiers.SNOW_FOX_SP); }
	public static boolean isAllaySP(LivingEntity entity) { return isForm(entity, FormIdentifiers.ALLAY_SP); }
	public static boolean isWildCatSP(LivingEntity entity) { return isForm(entity, FormIdentifiers.WILD_CAT_SP); }
	public static boolean isOcelotSP(LivingEntity entity) { return isForm(entity, FormIdentifiers.OCELOT_SP); }
	public static boolean isAxolotlSP(LivingEntity entity) { return isForm(entity, FormIdentifiers.AXOLOTL_SP); }

	/** 进化美西螈（SSCA 进化路线起点形态）。 */
	public static boolean isUpgradeAxolotl(LivingEntity entity) { return isForm(entity, FormIdentifiers.UPGRADE_AXOLOTL); }

	/** 荧光幼灵（含阿澪，技能一致）。 */
	public static boolean isAxolotlFluorescent(LivingEntity entity) {
		return isForm(entity, FormIdentifiers.AXOLOTL_FLUORESCENT) || isForm(entity, FormIdentifiers.AXOLOTL_ALING);
	}

	/** 需要保湿的形态（SP 美西螈 / 进化美西螈 / 荧光幼灵），用于加湿器生效判定。 */
	public static boolean isMoistureDependent(LivingEntity entity) {
		return isAxolotlSP(entity) || isUpgradeAxolotl(entity) || isAxolotlFluorescent(entity);
	}

	public static boolean isAnubisWolfSP(LivingEntity entity) { return isForm(entity, FormIdentifiers.ANUBIS_WOLF_SP); }
	public static boolean isGoldenSandstormSP(LivingEntity entity) { return isForm(entity, FormIdentifiers.GOLDEN_SANDSTORM_SP); }

	public static boolean isFamiliarFoxForm(LivingEntity entity) {
		return isAnyForm(entity, FormIdentifiers.FAMILIAR_FOX_SP, FormIdentifiers.FAMILIAR_FOX_RED);
	}

	public static boolean isBatDesmodus(LivingEntity entity) { return isForm(entity, FormIdentifiers.BAT_DESMODUS); }
	public static boolean isBatParasiticFruit(LivingEntity entity) { return isForm(entity, FormIdentifiers.BAT_PARASITIC_FRUIT); }

	/** 是否为咒文胡狼（按实体类型注册 ID 判断）。 */
	public static boolean isTransformativeWolf(LivingEntity entity) {
		return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).equals(TRANSFORMATIVE_WOLF_TYPE_ID);
	}
}
