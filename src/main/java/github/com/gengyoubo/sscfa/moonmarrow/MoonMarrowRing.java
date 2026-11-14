package github.com.gengyoubo.sscfa.moonmarrow;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/**
 * 月髓环（ssc_addon:sp_upgrade_thing）的形态映射与扩展点。
 *
 * <p>月髓环本体只依赖 SSC Forge 的形态/变形/诅咒之月 API。尚未迁移的子系统通过这里挂接：</p>
 * <ul>
 *   <li>剧情（月痕之力 / 潮汐之灵）：{@link #registerUseInterceptor} 注册"免费变回"拦截器；</li>
 *   <li>进化树（SSCA 路线分支门控）：{@link #setEvolutionGate} 替换默认门控；</li>
 *   <li>子成就：{@link #registerFormAdvancement}。</li>
 * </ul>
 *
 * <p>目标形态在 {@link github.com.gengyoubo.sscfa.forms.AddonForms} 注册；未注册时月髓环会提示"尚不可用"并且不消耗。</p>
 *
 * <p>形态 ID 使用旧路径（如 my_addon:familiar_fox_sp，而非 Fabric 版 FormIdentifiers 改名后的
 * familiar_fox_spirit_realm_lord）：SSC Forge 强制 Origin 为 {@code form_<path>}，模型配置也按
 * {@code ssc_form_model/origins.origin.<ns>.form_<path>.json} 查找，而现有 Origin、模型、贴图、语言键全部沿用旧路径。</p>
 */
public final class MoonMarrowRing {
	private static final String SSC = "shape-shifter-curse";
	private static final String ADDON = "my_addon";

	public static final ResourceLocation FAMILIAR_FOX_3 = new ResourceLocation(SSC, "familiar_fox_3");
	public static final ResourceLocation FAMILIAR_FOX_SP = new ResourceLocation(ADDON, "familiar_fox_sp");
	public static final ResourceLocation AXOLOTL_SP = new ResourceLocation(ADDON, "axolotl_sp");
	public static final ResourceLocation FAMILIAR_FOX_RED = new ResourceLocation(ADDON, "familiar_fox_red");
	public static final ResourceLocation UPGRADE_FAMILIAR_FOX = new ResourceLocation(ADDON, "upgrade_familiar_fox");
	public static final ResourceLocation UPGRADE_AXOLOTL = new ResourceLocation(ADDON, "upgrade_axolotl");

	/** 起点形态 → 月髓环进化目标形态。 */
	private static final Map<ResourceLocation, ResourceLocation> UPGRADES = new LinkedHashMap<>();
	private static final Map<ResourceLocation, ResourceLocation> FORM_ADVANCEMENTS = new LinkedHashMap<>();
	private static final List<Predicate<ServerPlayer>> USE_INTERCEPTORS = new CopyOnWriteArrayList<>();

	/**
	 * 进化树尚未迁移时的保守门控：SSCA 进化路线起点形态无法确认分支是否已全部解锁，因此一律拒绝，
	 * 避免缺失配置把门控变成免费进化。其余形态不受门控。
	 */
	private static final Predicate<ServerPlayer> DEFAULT_EVOLUTION_GATE = player -> {
		ResourceLocation current = MoonMarrowForms.currentFormId(player);
		return !UPGRADE_FAMILIAR_FOX.equals(current) && !UPGRADE_AXOLOTL.equals(current);
	};
	private static volatile Predicate<ServerPlayer> evolutionGate = DEFAULT_EVOLUTION_GATE;

	public static final float DEFAULT_RED_VARIANT_CHANCE = 0.05f;
	/** 使魔三阶段使用月髓环时的临时红狐概率。 */
	private static volatile float redVariantChance = DEFAULT_RED_VARIANT_CHANCE;

	static {
		registerUpgrade(FAMILIAR_FOX_3, FAMILIAR_FOX_SP);
		registerUpgrade(new ResourceLocation(SSC, "axolotl_3"), AXOLOTL_SP);
		registerUpgrade(new ResourceLocation(SSC, "allay_sp"), new ResourceLocation(ADDON, "fallen_allay_sp"));
		registerUpgrade(new ResourceLocation(SSC, "anubis_wolf_3"), new ResourceLocation(ADDON, "anubis_wolf_sp"));
		// 吸血蝙蝠：原版蝙蝠三阶段永久态 → desmodus
		registerUpgrade(new ResourceLocation(SSC, "bat_3"), new ResourceLocation(ADDON, "bat_desmodus"));
		// 风灵：原版豹猫永久形态 ocelot_3 → 风灵
		registerUpgrade(new ResourceLocation(SSC, "ocelot_3"), new ResourceLocation(ADDON, "ocelot_wind_spirit"));
		// 月织蛛：原版蜘蛛三阶段永久态 → 月织蛛
		registerUpgrade(new ResourceLocation(SSC, "spider_3"), new ResourceLocation(ADDON, "spider_moon_weaver"));
		// 食梦魔：原版野猫 SP 形态 → 食梦魔
		registerUpgrade(new ResourceLocation(SSC, "feral_cat_sp"), new ResourceLocation(ADDON, "wild_cat_nightmare"));
		// 寒棘狐：原版雪狐三阶段永久态 → 寒棘狐
		registerUpgrade(new ResourceLocation(SSC, "snow_fox_3"), new ResourceLocation(ADDON, "snow_fox_frostspine"));
		// SSCA 进化路线起点 → SP 形态：需解锁全部分支（见 evolutionGate）
		registerUpgrade(UPGRADE_FAMILIAR_FOX, FAMILIAR_FOX_SP);
		registerUpgrade(UPGRADE_AXOLOTL, AXOLOTL_SP);
	}

	private MoonMarrowRing() {}

	public static synchronized void registerUpgrade(ResourceLocation from, ResourceLocation to) {
		UPGRADES.put(from, to);
	}

	@Nullable
	public static synchronized ResourceLocation targetOf(@Nullable ResourceLocation from) {
		return from == null ? null : UPGRADES.get(github.com.gengyoubo.sscfa.util.FormIdMigration.legacy(from));
	}

	/** 当前形态是否已经是某条月髓环路线的终点（即"已是 SP"）。 */
	public static synchronized boolean isUpgradeTarget(@Nullable ResourceLocation formId) {
		return formId != null && UPGRADES.containsValue(github.com.gengyoubo.sscfa.util.FormIdMigration.legacy(formId));
	}

	public static synchronized Map<ResourceLocation, ResourceLocation> upgrades() {
		return Collections.unmodifiableMap(new LinkedHashMap<>(UPGRADES));
	}

	public static synchronized Set<ResourceLocation> targets() {
		return Set.copyOf(UPGRADES.values());
	}

	/**
	 * 注册使用前拦截器（服务端）。返回 true 表示已处理（例如剧情免费变回），月髓环不再执行常规逻辑且不消耗。
	 */
	public static void registerUseInterceptor(Predicate<ServerPlayer> interceptor) {
		USE_INTERCEPTORS.add(interceptor);
	}

	static boolean runInterceptors(ServerPlayer player) {
		for (Predicate<ServerPlayer> interceptor : USE_INTERCEPTORS) {
			if (interceptor.test(player)) return true;
		}
		return false;
	}

	/** 进化树迁移后用真实的分支解锁检查替换默认门控。 */
	public static void setEvolutionGate(Predicate<ServerPlayer> gate) {
		evolutionGate = gate == null ? DEFAULT_EVOLUTION_GATE : gate;
	}

	static boolean passesEvolutionGate(ServerPlayer player) {
		return evolutionGate.test(player);
	}

	/** 红狐形态及其到期回退迁移后调用（通常传 {@link #DEFAULT_RED_VARIANT_CHANCE}）。 */
	public static void setRedVariantChance(float chance) {
		redVariantChance = Math.max(0.0f, Math.min(1.0f, chance));
	}

	static float redVariantChance() {
		return redVariantChance;
	}

	/** 目标形态 → 子成就（成功变形后发放；成就不存在时静默跳过）。 */
	public static synchronized void registerFormAdvancement(ResourceLocation targetForm, ResourceLocation advancement) {
		FORM_ADVANCEMENTS.put(targetForm, advancement);
	}

	@Nullable
	static synchronized ResourceLocation formAdvancement(ResourceLocation targetForm) {
		return FORM_ADVANCEMENTS.get(targetForm);
	}
}
