package github.com.gengyoubo.sscfa.power;

import com.google.gson.JsonObject;
import github.com.gengyoubo.sscfa.balance.BalanceIntegration;
import github.com.gengyoubo.sscfa.util.PowerUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.onixary.shapeShifterCurseForge.api.registry.SscPowerType;
import net.onixary.shapeShifterCurseForge.power.FormPowerRuntime;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 雪狐 SP 姿态移速 {@code ssc_addon:snow_fox_sp_form_speed}：按 {@code resource} 指向的姿态资源挂移速修饰符。
 *
 * <p>资源值 0 = 近战姿态（{@code melee_speed_bonus}），1 = 远程姿态（{@code ranged_speed_penalty}）。
 * 两个数值可被 balance 快照（forms.snow_fox_sp）覆盖；快照重载后同姿态也会重挂。</p>
 */
public final class SnowFoxSpFormSpeedPower implements SscPowerType {
	public static final ResourceLocation TYPE = new ResourceLocation("ssc_addon", "snow_fox_sp_form_speed");

	private static final UUID MELEE_SPEED_UUID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
	private static final UUID RANGED_SPEED_UUID = UUID.fromString("b2c3d4e5-f6a7-8901-bcde-f12345678901");

	/** 一个玩家上次挂修饰符时的姿态与 balance 配置代数。 */
	private static final class State {
		int lastSwitchState = -1;
		long lastBalanceRevision = Long.MIN_VALUE;
	}

	private final Map<UUID, State> states = new ConcurrentHashMap<>();

	@Override
	public void tick(ServerPlayer player, ResourceLocation powerId, JsonObject data) {
		State state = states.computeIfAbsent(player.getUUID(), ignored -> new State());
		if (!FormPowerRuntime.test(player, player, data.getAsJsonObject("condition"))) {
			removeModifiers(player);
			state.lastSwitchState = Integer.MIN_VALUE;
			return;
		}

		int currentState = switchState(player, data);
		long balanceRevision = BalanceIntegration.serverRevision();
		// 重生后玩家实体是新的，临时修饰符随旧实体丢失：姿态没变也要重挂
		if (currentState != state.lastSwitchState || balanceRevision != state.lastBalanceRevision
				|| modifierMissing(player, currentState)) {
			updateSpeedModifier(player, data, currentState);
			state.lastSwitchState = currentState;
			state.lastBalanceRevision = balanceRevision;
		}
	}

	@Override
	public void onRemoved(ServerPlayer player, ResourceLocation powerId, JsonObject data) {
		states.remove(player.getUUID());
		removeModifiers(player);
	}

	/** 姿态资源缺失时按近战姿态处理。 */
	private static int switchState(ServerPlayer player, JsonObject data) {
		ResourceLocation resourceId = ResourceLocation.tryParse(FormPowerRuntime.stringValue(data, "resource", ""));
		return resourceId == null ? 0 : PowerUtils.getResourceValue(player, resourceId);
	}

	private static boolean modifierMissing(ServerPlayer player, int state) {
		AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed == null || state < 0 || state > 1) return false;
		return speed.getModifier(state == 0 ? MELEE_SPEED_UUID : RANGED_SPEED_UUID) == null;
	}

	private static void removeModifiers(ServerPlayer player) {
		AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed == null) return;
		speed.removeModifier(MELEE_SPEED_UUID);
		speed.removeModifier(RANGED_SPEED_UUID);
	}

	private static void updateSpeedModifier(ServerPlayer player, JsonObject data, int state) {
		AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed == null) return;
		speed.removeModifier(MELEE_SPEED_UUID);
		speed.removeModifier(RANGED_SPEED_UUID);

		// 加成值每次从 balance 快照读取（power JSON 字段是未覆盖时的默认）
		var balance = BalanceIntegration.serverSnapshot();
		if (state == 0) {
			double bonus = balance != null && balance.overridesPower("forms.snow_fox_sp", "melee_speed_bonus")
					? balance.getDouble("forms.snow_fox_sp", "melee_speed_bonus")
					: FormPowerRuntime.doubleValue(data, "melee_speed_bonus", 0.1);
			speed.addTransientModifier(new AttributeModifier(MELEE_SPEED_UUID, "Snow Fox SP Melee Speed",
					bonus, AttributeModifier.Operation.MULTIPLY_TOTAL));
		} else if (state == 1) {
			double penalty = balance != null && balance.overridesPower("forms.snow_fox_sp", "ranged_speed_penalty")
					? balance.getDouble("forms.snow_fox_sp", "ranged_speed_penalty")
					: FormPowerRuntime.doubleValue(data, "ranged_speed_penalty", -0.1);
			speed.addTransientModifier(new AttributeModifier(RANGED_SPEED_UUID, "Snow Fox SP Ranged Speed",
					penalty, AttributeModifier.Operation.MULTIPLY_TOTAL));
		}
	}
}
