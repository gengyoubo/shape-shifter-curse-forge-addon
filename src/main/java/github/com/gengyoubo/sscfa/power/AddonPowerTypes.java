package github.com.gengyoubo.sscfa.power;

import net.onixary.shapeShifterCurseForge.api.registry.SscJavaRegistries;

/**
 * 向 SSC Forge 注册附属的能力类型与技能键（SSC Forge 1.10.0.5 起支持）。在模组构造时调用 {@link #init()}。
 */
public final class AddonPowerTypes {
	/** SP 主/副技能键：能力 JSON 里 {@code "key": {"key": ...}} 用的按键 ID。 */
	public static final String SP_PRIMARY_KEY = "key.ssc_addon.sp_primary";
	public static final String SP_SECONDARY_KEY = "key.ssc_addon.sp_secondary";

	private AddonPowerTypes() {}

	public static void init() {
		SscJavaRegistries.registerPowerKey(SP_PRIMARY_KEY);
		SscJavaRegistries.registerPowerKey(SP_SECONDARY_KEY);
		SscJavaRegistries.registerPowerType(FailAwareActiveSelf.TYPE, new FailAwareActiveSelf());
		SscJavaRegistries.registerPowerType(FamiliarSkill.TYPE, new FamiliarSkill());
		SscJavaRegistries.registerPowerType(BatSonicWaveAbilityPower.TYPE, new BatSonicWaveAbilityPower());
		SscJavaRegistries.registerPowerType(MistFormAbilityPower.TYPE, new MistFormAbilityPower());
		AddonActionsAndConditions.init();
	}
}
