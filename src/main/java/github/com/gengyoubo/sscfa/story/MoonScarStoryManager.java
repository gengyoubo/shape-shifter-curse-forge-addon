package github.com.gengyoubo.sscfa.story;
import net.minecraftforge.common.MinecraftForge;

import net.minecraft.ChatFormatting;
import net.minecraft.advancements.Advancement;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.FormUtils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * 「月痕之力」剧情链（全程服务端权威，状态由 {@link MoonScarStoryState} 持久化）??
 * <ol>
 *     <li>解??{@code ssc_addon:moon_scar_power} 成就且玩家处?sp 使魔时，悄悄话提示一次，引导其去睡觉?/li>
 *     <li>sp 使魔在诅咒之月夜晚上床睡????真正进入原版睡眠状态（真躺床、真黑屏过渡、真 {@code isSleeping}）??
	 *     ??mixin 仅顶住诅咒之月的强制唤醒（绕?SSC 禁睡、让玩家能真睡）、并阻止跳夜（不推进时间）；
 *     睡满?5 秒后由本模组主动叫醒、瞬间变?red（不播放变身动画，不跳夜）??
 *     若玩家中途下??/ 被取消睡眠（例如手动起床、離线），演出取消??/li>
 *     <li>处于剧??red 的玩家，随时随地用月髓环可免费变?sp 使魔（不消耗物品）??/li>
 * </ol>
 * 形态本身的存??重生由主包形态系统保证（red 死亡重生仍??red，变?sp 后才?sp）??
 */
public final class MoonScarStoryManager {
	public static final ResourceLocation MOON_SCAR_POWER_ADV = new ResourceLocation("ssc_addon", "moon_scar_power");
	/** 未提示玩家的成就检查降频：??20 tick（??1 秒）一次，已提示玩家直接跳过??*/
	private static final int TIP_CHECK_INTERVAL = 20;
	/** 真睡变身所需睡眠时长??00 tick（??5 秒?? vanilla 睡眠阈值）。睡满后由本模组主动叫醒??red??*/
	private static final int STORY_SLEEP_DURATION = 100;
	/**
	 * 正在进行「剧情真睡」的玩????已??tick 计数。仅服务端主线程访问??
	 * 期??{@code SscAddonLivingEntityMixin} 仅顶住诅咒之月「强制唤醒」（让玩家真睡不被弹起）??
	 * {@code SscPlayerMixin} ??{@code canResetTimeBySleeping} 返??false 以阻止跳夜（不推进时间）??
	 * 计数?STORY_SLEEP_DURATION 后??{@link #tickStorySleep} 主动叫醒并变身（不跳夜，故不会卡在床上）??
	 */
	private static final Map<UUID, Integer> STORY_SLEEPING = new HashMap<>();

	private MoonScarStoryManager() {
	}

	/** ??wakeUp 抑??/ canResetTimeBySleeping 抑??mixin 查询：该玩家是否处于「剧情真睡」中?*/
	public static boolean isStorySleeping(UUID playerUuid) {
		return STORY_SLEEPING.containsKey(playerUuid);
	}

	public static void register() {
		github.com.gengyoubo.sscfa.moonmarrow.MoonMarrowRing.registerUseInterceptor(MoonScarStoryManager::tryFreeRevertFromStoryRed);
		// sp 使魔在诅咒之月夜晚上???进入「剧情真睡」（真睡眠、仅顶住 SSC 禁睡、阻止跳夜，睡满??5 秒后叫醒??red）??
		MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.entity.player.PlayerSleepInBedEvent event) -> { var entity = event.getEntity(); var sleepingPos = event.getPos();
			if (entity instanceof ServerPlayer player && !player.level().isClientSide) {
				tryBeginStorySleep(player);
			}
		});

		MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.TickEvent.ServerTickEvent event) -> { if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return; var server = event.getServer();
			// 「剧情真睡」计时：?tick 推进，睡满后叫醒并变身（仅在有玩家真睡时执行）??
			if (!STORY_SLEEPING.isEmpty()) {
				tickStorySleep(server);
			}
			// 低语提示：解锁成就且??sp 使????发一次；降频?20 tick 检查??
			if (server.getTickCount() % TIP_CHECK_INTERVAL == 0) {
				MoonScarStoryState state = MoonScarStoryState.get(server);
				for (ServerPlayer player : server.getPlayerList().getPlayers()) {
					tickTip(player, state);
				}
			}
		});
	}

	private static void tickTip(ServerPlayer player, MoonScarStoryState state) {
		if (state.tippedPlayers.contains(player.getUUID())) return; // 已提示过，永不再??
		if (!FormUtils.isFamiliarFoxSP(player)) return;             // 必须处于 sp 使??
		if (!hasMoonScarPower(player)) return;                      // 必须已解锁月痕之力成??
		player.displayClientMessage(
				Component.translatable("message.ssc_addon.moon_scar.whisper").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC),
				false);
		state.tippedPlayers.add(player.getUUID());
		state.setDirty();
	}

	/** START_SLEEPING：sp 使魔解锁成就后、在诅咒之月夜晚上床 ??进入「剧情真睡」（不立即变身，让玩家真正睡一觉）?*/
	private static void tryBeginStorySleep(ServerPlayer player) {
		Level world = player.level();
		if (!FormUtils.isFamiliarFoxSP(player)) return;
		if (!hasMoonScarPower(player)) return;
		if (!(net.onixary.shapeShifterCurseForge.other.cursedmoon.CursedMoonService.isCursedMoonDay(world) && net.onixary.shapeShifterCurseForge.other.cursedmoon.CursedMoonService.isNight(world))) return;
		// 加入真睡追踪：mixin 据此仅顶?SSC 的诅咒之月强制唤醒（让玩家能真睡不被弹起）、并阻止跳夜?
		// 玩家保持原版睡眠状态（原版睡眠本身锁住视角/移动、呈现睡姿与黑屏过渡），不需 STUN??
		STORY_SLEEPING.putIfAbsent(player.getUUID(), 0);
	}

	/** ??tick 推进「剧情真睡」计时：睡满后叫醒变身；玩家中途下??離线则取消演出?*/
	private static void tickStorySleep(MinecraftServer server) {
		Iterator<Map.Entry<UUID, Integer>> it = STORY_SLEEPING.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, Integer> entry = it.next();
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			if (player == null) {
				it.remove(); // 玩家離线：清理，待下次重新尝?
				continue;
			}
			// 玩家已不在睡眠状态（被其它原因唤??下床）：取消演出，不变身??
			// （SSC 的诅咒之月唤醒已被抑制，所以这里不在睡?= 玩家自主下床或受到不可抑制的唤醒。??
			if (!player.isSleeping()) {
				it.remove();
				continue;
			}
			int slept = entry.getValue() + 1;
			if (slept < STORY_SLEEP_DURATION) {
				entry.setValue(slept);
				continue;
			}
			// 睡满：先移除标记（解除唤醒抑??/ 恢??canResetTimeBySleeping），再主动叫醒并变??
			it.remove();
			finalizeStorySleep(server, player);
		}
	}

	/** 「剧情真睡」睡满：主动把玩家从床上叫????瞬间无动画??red ??记入剧??red 并广播低语??*/
	private static void finalizeStorySleep(MinecraftServer server, ServerPlayer player) {
		if (player.isSleeping()) {
			// 主动起床（此时标记已移除，唤醒不再被抑制）；演出期间已阻??canResetTimeBySleeping，故不跳??
			player.stopSleepInBed(true, true);
		}
		if (!FormUtils.isFamiliarFoxSP(player)) return; // 期间形态已变则不再转??
		var redForm = net.onixary.shapeShifterCurseForge.form.FormRegistry.get(FormIdentifiers.FAMILIAR_FOX_RED);
		if (redForm == null) return;
		// 起床即变：setFormDirectly 瞬间换形态、不播放变身动画
		net.onixary.shapeShifterCurseForge.form.TransformManager.immediatelyTransform(player, FormIdentifiers.FAMILIAR_FOX_RED);
		MoonScarStoryState state = MoonScarStoryState.get(server);
		state.storyRedPlayers.add(player.getUUID());
		state.setDirty();
		Level world = player.level();
		player.displayClientMessage(
				Component.translatable("message.ssc_addon.moon_scar.become_red").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC),
				false);
		world.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1.0F, 0.6F);
	}

	/**
	 * 月髓环触发：若玩家处?剧情 red"，免费变??sp 使魔（不消耗物品），任意时间地点均可??
	 *
	 * @return true 表示已处理（调用方应直接返回、不再走常规升级逻辑、不消耗物品）?
	 */
	public static boolean tryFreeRevertFromStoryRed(Player player) {
		if (player.level().isClientSide) return false;
		if (!(player instanceof ServerPlayer sp)) return false;
		MinecraftServer server = sp.getServer();
		if (server == null) return false;

		MoonScarStoryState state = MoonScarStoryState.get(server);
		if (!state.storyRedPlayers.contains(sp.getUUID())) return false;

		if (!FormUtils.isFamiliarFoxRed(sp)) {
			// 标记与实际形态不一致（异常情况）：清理脏标记并放行常规逻辑，避免卡??
			state.storyRedPlayers.remove(sp.getUUID());
			state.setDirty();
			return false;
		}

		var spForm = net.onixary.shapeShifterCurseForge.form.FormRegistry.get(FormIdentifiers.FAMILIAR_FOX_SP);
		if (spForm == null) return false;

		// 月髓环免费变?sp 使魔：带黑屏淡入淡出动画（原 handleDirectTransform(...,false)??
		if (!net.onixary.shapeShifterCurseForge.form.TransformManager.startTransform(sp, FormIdentifiers.FAMILIAR_FOX_SP)) return true;
		state.storyRedPlayers.remove(sp.getUUID());
		state.setDirty();

		Level world = sp.level();
		sp.displayClientMessage(
				Component.translatable("message.ssc_addon.moon_scar.revert_sp").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC),
				false);
		world.playSound(null, sp.getX(), sp.getY(), sp.getZ(),
				SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.0F);
		return true;
	}

	private static boolean hasMoonScarPower(ServerPlayer player) {
		MinecraftServer server = player.getServer();
		if (server == null) return false;
		Advancement adv = server.getAdvancements().getAdvancement(MOON_SCAR_POWER_ADV);
		if (adv == null) return false;
		return player.getAdvancements().getOrStartProgress(adv).isDone();
	}
}
