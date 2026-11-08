package github.com.gengyoubo.sscfa.util;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** 服务端主动发放 minecraft:impossible 触发的成就；成就不存在时静默忽略。 */
public final class AdvancementUtils {
	private AdvancementUtils() {}

	public static void grant(Player player, ResourceLocation advId) {
		if (player == null || player.level().isClientSide || !(player instanceof ServerPlayer sp)) return;
		MinecraftServer server = sp.getServer();
		if (server == null) return;
		Advancement adv = server.getAdvancements().getAdvancement(advId);
		if (adv == null) return;
		AdvancementProgress progress = sp.getAdvancements().getOrStartProgress(adv);
		if (progress.isDone()) return;
		for (String criterion : progress.getRemainingCriteria()) {
			sp.getAdvancements().award(adv, criterion);
		}
	}
}
