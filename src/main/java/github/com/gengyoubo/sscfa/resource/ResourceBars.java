package github.com.gengyoubo.sscfa.resource;

import github.com.gengyoubo.sscfa.SscAddonForge;
import github.com.gengyoubo.sscfa.power.PowerJson;
import github.com.gengyoubo.sscfa.util.FormUtils;
import github.com.gengyoubo.sscfa.util.PowerUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.onixary.shapeShifterCurseForge.power.FormActivePowerService;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 统一资源条门面：所有资源条读写的唯一入口。
 *
 * <p>clamp 到 [0,max] → 写资源（{@link PowerUtils}，SSC Forge 自动同步）→ 触发该条全部 {@link BarTrigger}；
 * SSC 自带法力（{@link BarKeys#VANILLA_MANA}）直通 SSC Forge 的法力接口。写路径仅服务端；{@link #get} 双端可读。
 * 每个服务端 tick 按各条的 {@link RegenRule} 回复/衰减，并对命中的 {@link ThresholdEffect} 段施加效果。</p>
 */
@Mod.EventBusSubscriber(modid = SscAddonForge.MOD_ID)
public final class ResourceBars {
	/** 形态 → 该形态唯一绑定的资源条（由 {@link BarKeys#ALL} 的 ownerFormId 构建）。 */
	private static final Map<ResourceLocation, ResourceBarDef> FORM_TO_BAR;

	static {
		Map<ResourceLocation, ResourceBarDef> map = new HashMap<>();
		for (ResourceBarDef bar : BarKeys.ALL) {
			if (bar.ownerFormId != null) map.put(bar.ownerFormId, bar);
		}
		FORM_TO_BAR = Collections.unmodifiableMap(map);
	}

	private ResourceBars() {}

	// ==================== 写路径（服务端） ====================

	/** 回复 n 点（clamp 到 max），返回实际值。 */
	public static int gain(ServerPlayer player, ResourceBarDef bar, int n) {
		if (bar == BarKeys.VANILLA_MANA) {
			FormActivePowerService.gainMana(player, n);
			return (int) FormActivePowerService.mana(player);
		}
		int oldV = get(player, bar);
		int newV = Math.max(0, Math.min(maxOf(player, bar), oldV + n));
		apply(player, bar, oldV, newV);
		return newV;
	}

	/** 消耗 n 点：不足返回 false 且不扣；足够则扣并返回 true。 */
	public static boolean consume(ServerPlayer player, ResourceBarDef bar, int n) {
		if (bar == BarKeys.VANILLA_MANA) {
			if (!FormActivePowerService.hasMana(player, n)) return false;
			FormActivePowerService.consumeMana(player, n);
			return true;
		}
		int oldV = get(player, bar);
		if (oldV < n) return false;
		apply(player, bar, oldV, oldV - n);
		return true;
	}

	/** 直接设值（clamp），触发回调。 */
	public static void set(ServerPlayer player, ResourceBarDef bar, int value) {
		if (bar == BarKeys.VANILLA_MANA) {
			FormActivePowerService.gainMana(player, value - FormActivePowerService.mana(player));
			return;
		}
		int oldV = get(player, bar);
		int newV = Math.max(0, Math.min(maxOf(player, bar), value));
		apply(player, bar, oldV, newV);
	}

	/** 写资源 + 触发回调（唯一落点）。 */
	private static void apply(ServerPlayer player, ResourceBarDef bar, int oldV, int newV) {
		if (oldV == newV) return;
		PowerUtils.setResourceValueAndSync(player, bar.id, newV);
		for (BarTrigger t : bar.triggers()) t.onChange(player, oldV, newV, maxOf(player, bar));
	}

	// ==================== 读路径（双端） ====================

	public static int get(Player player, ResourceBarDef bar) {
		if (bar == BarKeys.VANILLA_MANA) return (int) FormActivePowerService.mana(player);
		return PowerUtils.getResourceValue(player, bar.id);
	}

	/** 上限：资源以 JSON 的 max 为准（缺失回退 {@link ResourceBarDef#maxDefault}）；SSC 法力由当前值与百分比推算。 */
	public static int maxOf(Player player, ResourceBarDef bar) {
		if (bar == BarKeys.VANILLA_MANA) {
			float percent = FormActivePowerService.manaPercent(player);
			return percent > 0 ? Math.round(FormActivePowerService.mana(player) / percent) : bar.maxDefault;
		}
		int max = PowerUtils.getResourceMax(bar.id);
		return max > 0 ? max : bar.maxDefault;
	}

	// ==================== 判定 ====================

	/** 玩家是否持有该条（资源能力存在 / 当前形态有 SSC 法力池）。 */
	public static boolean has(Player player, ResourceBarDef bar) {
		if (bar == BarKeys.VANILLA_MANA) {
			return FormActivePowerService.hasFamiliarFoxMana(player) || FormActivePowerService.hasWebMana(player);
		}
		return hasPowerId(player, bar.id);
	}

	/** 当前形态是否持有该 ID 的能力（含 apoli:multiple 子能力）。 */
	public static boolean hasPowerId(LivingEntity entity, ResourceLocation id) {
		return entity instanceof Player player && PowerJson.has(player, id);
	}

	// ==================== 统一 tick 调度（服务端） ====================

	@SubscribeEvent
	public static void onServerTick(TickEvent.ServerTickEvent event) {
		if (event.phase == TickEvent.Phase.END) serverTick(event.getServer());
	}

	/**
	 * 对每个在线玩家持有的条，按各 RegenRule 的 interval 轮询回复/衰减，并对命中的 ThresholdEffect 段逐 tick 施加效果。
	 * 每条资源严格绑定单一形态，按当前形态直接定位，再用 {@link #has} 实测兜底。
	 */
	public static void serverTick(MinecraftServer server) {
		int tick = server.getTickCount();
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			ResourceLocation form = FormUtils.currentFormId(player);
			ResourceBarDef bar = form == null ? null : FORM_TO_BAR.get(form);
			if (bar == null || !has(player, bar)) continue;
			int current = get(player, bar);
			int max = maxOf(player, bar);
			for (RegenRule rule : bar.regenRules()) {
				if (tick % Math.max(1, rule.interval()) == 0) {
					int delta = rule.tickRegen(player, bar);
					if (delta != 0) gain(player, bar, delta);
				}
			}
			for (ThresholdEffect te : bar.thresholds()) {
				if (te.isInSegment(current, max)) te.applyTick(player, current, max);
			}
		}
	}
}
