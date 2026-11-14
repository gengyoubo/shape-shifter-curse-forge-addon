package github.com.gengyoubo.sscfa.forms;

import net.minecraft.resources.ResourceLocation;
import net.onixary.shapeShifterCurseForge.api.registry.SscForm;
import net.onixary.shapeShifterCurseForge.api.registry.SscJavaRegistries;
import net.onixary.shapeShifterCurseForge.api.registry.SscRegistrar;
import net.onixary.shapeShifterCurseForge.api.registry.SscRegistryObject;
import net.onixary.shapeShifterCurseForge.form.FormBodyType;

import java.util.function.UnaryOperator;

/**
 * SSCA 形态在 SSC Forge 上的注册（对应 Fabric 版 forms/SscAddonForms）。
 *
 * <p>使用 Java 注册而不是 data/&lt;ns&gt;/ssc_form 动态形态：SSC Forge 1.10.0.4 校验动态形态的 Origin 时
 * 直接以 {@code my_addon:form_x} 查资源（应为 {@code origins/form_x.json}），数据包里的动态形态都会校验失败。
 * Java 形态不经过该校验，能力仍按 {@code <ns>:form_<path>} 从 data/my_addon/origins/ 加载，
 * 模型按 assets/my_addon/ssc_form_model/ 加载。</p>
 *
 * <p>形态 ID 沿用旧路径（familiar_fox_sp 等），与现有 Origin、模型、贴图、语言键一致。
 * 尺寸与体型取自 Fabric 版的 applyScaleFunc / bodyType。</p>
 *
 * <p>注册月髓环、进化石的目标和进化树起点；各形态的具体能力仍需对应的 Forge 能力实现。</p>
 */
public final class AddonForms {
	public static final String NAMESPACE = "my_addon";
	private static final SscRegistrar FORMS = SscJavaRegistries.registrar(NAMESPACE);

	// 月髓环进化目标
	public static final SscRegistryObject<SscForm> FAMILIAR_FOX_SP = sp("familiar_fox_sp", FormBodyType.FERAL, 0.6f, 0.6f, b -> b);
	public static final SscRegistryObject<SscForm> AXOLOTL_SP = sp("axolotl_sp", FormBodyType.NORMAL, 1.0f, 1.0f, b -> b);
	public static final SscRegistryObject<SscForm> FALLEN_ALLAY_SP = sp("fallen_allay_sp", FormBodyType.NORMAL, 0.55f, 1.0f, b -> b);
	public static final SscRegistryObject<SscForm> ANUBIS_WOLF_SP = sp("anubis_wolf_sp", FormBodyType.FERAL, 0.8f, 0.6f, b -> b);
	// 复用原版 bat_3：缓慢下落
	public static final SscRegistryObject<SscForm> BAT_DESMODUS = sp("bat_desmodus", FormBodyType.NORMAL, 0.6f, 0.7f,
			b -> b.addFlags("slow_fall", "slow_fall_animation"));
	// 复用原版 ocelot_3：跳跃加成与 ocelot_3 一致
	public static final SscRegistryObject<SscForm> OCELOT_WIND_SPIRIT = sp("ocelot_wind_spirit", FormBodyType.FERAL, 0.75f, 0.6f,
			b -> b.jumpVelocityAddition(0.25f));
	// 复用原版 spider_3：夜视、攀爬、免疫中毒、摔落保护
	public static final SscRegistryObject<SscForm> SPIDER_MOON_WEAVER = sp("spider_moon_weaver", FormBodyType.NORMAL, 0.81f, 1.0f,
			b -> b.addFlags("night_vision", "climb", "poison_immune").fallProtectionDistance(4.0f));
	public static final SscRegistryObject<SscForm> WILD_CAT_NIGHTMARE = sp("wild_cat_nightmare", FormBodyType.FERAL, 0.55f, 0.6f, b -> b);
	// 复用原版 snow_fox_3：摔落保护
	public static final SscRegistryObject<SscForm> SNOW_FOX_FROSTSPINE = sp("snow_fox_frostspine", FormBodyType.FERAL, 0.55f, 0.6f,
			b -> b.fallProtectionDistance(2.5f));
	// 进化石目标及进化树起点；使用资源文件对应的旧路径注册。
	public static final SscRegistryObject<SscForm> WILD_CAT_SP = sp("wild_cat_sp", FormBodyType.FERAL, 0.55f, 0.6f, b -> b);
	public static final SscRegistryObject<SscForm> SNOW_FOX_SP = sp("snow_fox_sp", FormBodyType.FERAL, 0.55f, 0.6f,
			b -> b.fallProtectionDistance(2.5f));
	public static final SscRegistryObject<SscForm> ALLAY_SP = sp("allay_sp", FormBodyType.NORMAL, 0.55f, 1.0f, b -> b);
	public static final SscRegistryObject<SscForm> GOLDEN_SANDSTORM_SP = sp("golden_sandstorm_sp", FormBodyType.FERAL, 0.8f, 0.6f, b -> b);
	public static final SscRegistryObject<SscForm> FAMILIAR_FOX_MANCIANIMA = sp("familiar_fox_mancianima", FormBodyType.FERAL, 0.55f, 0.6f, b -> b);
	public static final SscRegistryObject<SscForm> FAMILIAR_FOX_RED = sp("familiar_fox_red", FormBodyType.FERAL, 0.65f, 0.6f, b -> b);
	public static final SscRegistryObject<SscForm> BAT_PARASITIC_FRUIT = sp("bat_parasitic_fruit", FormBodyType.NORMAL, 0.6f, 0.7f,
			b -> b.addFlags("slow_fall", "slow_fall_animation"));
	public static final SscRegistryObject<SscForm> AXOLOTL_FLUORESCENT = sp("axolotl_fluorescent", FormBodyType.NORMAL, 0.75f, 0.75f, b -> b);
	public static final SscRegistryObject<SscForm> OCELOT_NOVA = sp("ocelot_nova", FormBodyType.FERAL, 0.75f, 0.6f,
			b -> b.jumpVelocityAddition(0.25f));
	public static final SscRegistryObject<SscForm> SPIDER_SALTICIDAE = sp("spider_salticidae", FormBodyType.NORMAL, 0.6f, 1.0f,
			b -> b.addFlags("night_vision", "climb", "poison_immune").fallProtectionDistance(4.0f));
	public static final SscRegistryObject<SscForm> UPGRADE_FAMILIAR_FOX = sp("upgrade_familiar_fox", FormBodyType.FERAL, 0.55f, 0.6f, b -> b);
	public static final SscRegistryObject<SscForm> UPGRADE_AXOLOTL = sp("upgrade_axolotl", FormBodyType.NORMAL, 1.0f, 1.0f, b -> b);

	private AddonForms() {}

	public static void init() {
		FORMS.init();
	}

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(NAMESPACE, path);
	}

	/** SSCA 的 SP 形态：独立分组、阶段 1、无本能、免疫诅咒之月与抑制剂（Fabric 版 NoInstinct/NoCursedMoonEffect/SpecialForm/InhibitorImmune）。 */
	private static SscRegistryObject<SscForm> sp(String path, FormBodyType body, float scale, float eyeScale,
	                                             UnaryOperator<SscForm.Builder> extra) {
		return FORMS.form(() -> extra.apply(SscForm.builder(id(path))
						.group(id("group_" + path))
						.stage(1)
						.bodyType(body)
						.widthScale(scale)
						.heightScale(scale)
						.eyeScale(eyeScale)
						.specialForm()
						.inhibitorImmune()
						.disablesInstinct()
						.immuneToCursedMoon())
				.build());
	}
}
