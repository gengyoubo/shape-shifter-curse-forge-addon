package github.com.gengyoubo.sscfa.event;

import github.com.gengyoubo.sscfa.SscAddonForge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录血量恢复：修复「带 max_health 修饰符的形态（如 SP 美西螈）重进存档后血量被裸 20 上限钳掉」。
 *
 * <p><b>根因</b>：形态能力提供的最大生命值是运行时临时修饰符，不写入玩家 NBT。登录时先按裸 20 上限读取存档血量
 * （大于 20 被钳到 20），之后形态能力才把最大生命值抬高，此时血量已经停在 20。</p>
 *
 * <p><b>修复</b>：{@code LivingEntityLoginHealthMixin} 在读取存档时记录原始血量快照；之后每个服务端 tick 检查，
 * 一旦最大生命值 ≥ 快照（修饰符已挂上）就把血量补回快照值。期间受击、死亡或离线则作废，不覆盖真实战斗血量。
 * 普通玩家、减血形态、半血退出等场景都是无副作用的空操作。</p>
 */
@Mod.EventBusSubscriber(modid = SscAddonForge.MOD_ID)
public final class LoginHealthRestoreHandler {
	/** 等待最大生命值修饰符挂上的最长 tick 数，超时放弃（不强改血量）。 */
	private static final int MAX_WAIT_TICKS = 200;

	private static final class Pending {
		final float snapshot;
		int waited;

		Pending(float snapshot) {
			this.snapshot = snapshot;
		}
	}

	private static final Map<UUID, Pending> PENDING = new ConcurrentHashMap<>();

	static {
		PlayerSessionCleanup.register((connection, server) -> PENDING.remove(connection.player.getUUID()));
	}

	private LoginHealthRestoreHandler() {}

	/** 由 mixin 在读取存档 NBT 时调用，记录登录血量快照。 */
	public static void recordSnapshot(UUID uuid, float savedHealth) {
		if (savedHealth > 0.0f) {
			PENDING.put(uuid, new Pending(savedHealth));
		}
	}

	@SubscribeEvent
	public static void onServerTick(TickEvent.ServerTickEvent event) {
		if (event.phase != TickEvent.Phase.END || PENDING.isEmpty()) return;
		Iterator<Map.Entry<UUID, Pending>> it = PENDING.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, Pending> entry = it.next();
			Pending p = entry.getValue();
			ServerPlayer player = event.getServer().getPlayerList().getPlayer(entry.getKey());
			if (player == null) {
				// 尚未真正加入玩家列表 / 已离开：只累计等待，超时清理
				if (++p.waited > MAX_WAIT_TICKS) it.remove();
				continue;
			}
			// 已受击 / 已死亡：玩家已进入正常游戏，不能用旧快照覆盖真实血量
			if (player.hurtTime > 0 || !player.isAlive()) {
				it.remove();
				continue;
			}
			if (player.getMaxHealth() >= p.snapshot) {
				if (player.getHealth() < p.snapshot) player.setHealth(p.snapshot);
				it.remove();
			} else if (++p.waited > MAX_WAIT_TICKS) {
				// 超时仍未挂上（形态可能已不存在）：放弃，不强改血量
				it.remove();
			}
		}
	}
}
