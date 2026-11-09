package github.com.gengyoubo.sscfa.power;

import com.google.gson.JsonObject;
import github.com.gengyoubo.sscfa.balance.BalanceIntegration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * 使魔主/副键技能 {@code ssc_addon:familiar_skill}：冷却字段同 {@link FailAwareActiveSelf}，另加 {@code "primary"}。
 * 击杀赋能就绪时走赋能释放（火环/吐息），否则按统一技能处理。
 */
public final class FamiliarSkill extends FailAwareActiveSelf {
	public static final ResourceLocation TYPE = new ResourceLocation("ssc_addon", "familiar_skill");

	/** 击杀赋能释放入口：已处理（赋能就绪并尝试释放）返回 true。由击杀赋能系统注册。 */
	@FunctionalInterface
	public interface EmpoweredCast {
		boolean tryCast(ServerPlayer player, boolean primary);
	}

	private static volatile EmpoweredCast empowered = (player, primary) -> false;

	public static void setEmpoweredCast(EmpoweredCast cast) {
		empowered = cast;
	}

	@Override
	protected void use(ServerPlayer player, ResourceLocation powerId, JsonObject data) {
		if (!player.isAlive() || !BalanceIntegration.isPlayerReady(player)) return;
		boolean primary = data.has("primary") && data.get("primary").getAsBoolean();
		if (empowered.tryCast(player, primary)) return;
		super.use(player, powerId, data);
	}
}
