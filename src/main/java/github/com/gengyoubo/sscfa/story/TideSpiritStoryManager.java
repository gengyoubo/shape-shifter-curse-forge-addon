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
 * 「潮汐之灵」剧情链（阿???Aling 解锁，全程服务端权威，状态???{@link TideSpiritStoryState} 持久化）??
 * <p>机制照搬「月痕之力」（{@link MoonScarStoryManager}），区别??
 * <ol>
 *     <li>前置形态：荧光幼灵（axolotl_fluorescent）而??sp 使魔?/li>
 *     <li>解锁成就：{@code ssc_addon:tide_spirit_power}（集齐蔚蓝??本??+ 荧光幼灵形态成就）?/li>
 *     <li>目标形态：阿澪（axolotl_aling）??b>永??/b>（不自动变回，无月髓环免费还原）??/li>
 *     <li>变身后文案：「这??..哪里？我的家呢？??/li>
 * </ol>
 * <p>剧情真睡的唤醒抑???/ 阻止跳夜复??{@link MoonScarStoryManager#isStorySleeping} 判??
 * （{@code SscPlayerMixin} 据此决定是否顶住诅咒之月强制唤醒、是否阻止跳夜），故本类进入真睡??
 * 同步?uuid 放入 {@code MoonScarStoryManager.STORY_SLEEPING}（通??{@link #isStorySleeping} 统一判断）??
 */
public final class TideSpiritStoryManager {
	/** 解锁成就：集齐蔚蓝??本??+ 荧光幼灵形态?*/
	public static final ResourceLocation TIDE_SPIRIT_POWER_ADV = new ResourceLocation("ssc_addon", "tide_spirit_power");
	private static final int TIP_CHECK_INTERVAL = 20;
	private static final int STORY_SLEEP_DURATION = 100;

	/** 阿澪剧情真睡中的玩家 ??已??tick 计数?*/
	private static final Map<UUID, Integer> ALING_SLEEPING = new HashMap<>();

	private TideSpiritStoryManager() {
	}

	/** ??SscPlayerMixin 统一查询：该玩家是否处于任一剧情真睡中（月痕之????潮汐之灵）??*/
	public static boolean isStorySleeping(UUID playerUuid) {
		return ALING_SLEEPING.containsKey(playerUuid);
	}

	public static void register() {
		// 荧光幼灵在诅咒之月夜晚上???进入「剧情真睡??
		MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.entity.player.PlayerSleepInBedEvent event) -> { var entity = event.getEntity(); var sleepingPos = event.getPos();
			if (entity instanceof ServerPlayer player && !player.level().isClientSide) {
				tryBeginStorySleep(player);
			}
		});

		MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.TickEvent.ServerTickEvent event) -> { if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return; var server = event.getServer();
			if (!ALING_SLEEPING.isEmpty()) {
				tickStorySleep(server);
			}
			if (server.getTickCount() % TIP_CHECK_INTERVAL == 0) {
				TideSpiritStoryState state = TideSpiritStoryState.get(server);
				for (ServerPlayer player : server.getPlayerList().getPlayers()) {
					tickTip(player, state);
				}
			}
		});
	}

	private static void tickTip(ServerPlayer player, TideSpiritStoryState state) {
		if (state.tippedPlayers.contains(player.getUUID())) return;
		if (!FormUtils.isAxolotlFluorescent(player)) return;   // 必须处于荧光幼??
		if (!hasTideSpiritPower(player)) return;               // 必须已解锁潮汐之灵成??
		player.displayClientMessage(
				Component.translatable("message.ssc_addon.tide_spirit.whisper").withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC),
				false);
		state.tippedPlayers.add(player.getUUID());
		state.setDirty();
	}

	/** START_SLEEPING：荧光幼灵解锁成就后、在诅咒之月夜晚上床 ??进入「剧情真睡」??*/
	private static void tryBeginStorySleep(ServerPlayer player) {
		Level world = player.level();
		if (!FormUtils.isAxolotlFluorescent(player)) return;
		if (!hasTideSpiritPower(player)) return;
		if (!(net.onixary.shapeShifterCurseForge.other.cursedmoon.CursedMoonService.isCursedMoonDay(world) && net.onixary.shapeShifterCurseForge.other.cursedmoon.CursedMoonService.isNight(world))) return;
		ALING_SLEEPING.putIfAbsent(player.getUUID(), 0);
	}

	private static void tickStorySleep(MinecraftServer server) {
		Iterator<Map.Entry<UUID, Integer>> it = ALING_SLEEPING.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, Integer> entry = it.next();
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			if (player == null) {
				it.remove();
				continue;
			}
			if (!player.isSleeping()) {
				it.remove();
				continue;
			}
			int slept = entry.getValue() + 1;
			if (slept < STORY_SLEEP_DURATION) {
				entry.setValue(slept);
				continue;
			}
			it.remove();
			finalizeStorySleep(player);
		}
	}

	/** 「剧情真睡」睡满：叫?????瞬间无动画变阿澪（永久）??广播文案??*/
	private static void finalizeStorySleep(ServerPlayer player) {
		if (player.isSleeping()) {
			player.stopSleepInBed(true, true);
		}
		if (!FormUtils.isAxolotlFluorescent(player)) return; // 期间形态已变则不再转??
		var alingForm = net.onixary.shapeShifterCurseForge.form.FormRegistry.get(FormIdentifiers.AXOLOTL_ALING);
		if (alingForm == null) return;
		// 起床即变：immediatelyTransform 瞬间换形态、不播放变身动???
		net.onixary.shapeShifterCurseForge.form.TransformManager.immediatelyTransform(player, FormIdentifiers.AXOLOTL_ALING);
		Level world = player.level();
		player.displayClientMessage(
				Component.translatable("message.ssc_addon.tide_spirit.become_aling").withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC),
				false);
		world.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.AMBIENT_UNDERWATER_EXIT, SoundSource.PLAYERS, 1.0F, 0.8F);
		world.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7F, 1.2F);
	}

	private static boolean hasTideSpiritPower(ServerPlayer player) {
		MinecraftServer server = player.getServer();
		if (server == null) return false;
		Advancement adv = server.getAdvancements().getAdvancement(TIDE_SPIRIT_POWER_ADV);
		if (adv == null) return false;
		return player.getAdvancements().getOrStartProgress(adv).isDone();
	}

	/**
	 * 月髓环触发：若玩家当前为阿澪，免费变回荧光幼灵（不消耗物品），任意时间地点均可?
	 *
	 * <p>阿澪变身后为永久形态且无其它获取途径（仅剧情真睡），故直接以「当前形?阿澪」判定??
	 * 无需持久化标记（与月痕之???red 的标记式判定不同）。还原后若再次于诅咒之月夜入睡，
	 * 仍会重新变身阿澪（可逆循环）?/p>
	 *
	 * @return true 表示已处理（调用方应直接返回、不再走常规升级逻辑、不消耗物品）???
	 */
	public static boolean tryFreeRevertFromAling(Player player) {
		if (player.level().isClientSide) return false;
		if (!(player instanceof ServerPlayer sp)) return false;

		if (!FormUtils.isForm(sp, FormIdentifiers.AXOLOTL_ALING)) return false;

		var fluorescentForm = net.onixary.shapeShifterCurseForge.form.FormRegistry.get(FormIdentifiers.AXOLOTL_FLUORESCENT);
		if (fluorescentForm == null) return false;

		// 月髓环免费变回荧光幼灵：带黑屏淡入淡出动画（startTransform），STUN 在动画期间定???
		net.onixary.shapeShifterCurseForge.form.TransformManager.startTransform(sp, FormIdentifiers.AXOLOTL_FLUORESCENT);
		sp.addEffect(new net.minecraft.world.effect.MobEffectInstance(
				github.com.gengyoubo.sscfa.init.AddonEffects.STUN.get(),
				net.onixary.shapeShifterCurseForge.form.TransformManager.TRANSFORM_FX_DURATION_IN + net.onixary.shapeShifterCurseForge.form.TransformManager.TRANSFORM_FX_DURATION_OUT,
				0, false, false, false));

		Level world = sp.level();
		sp.displayClientMessage(
				Component.translatable("message.ssc_addon.tide_spirit.revert_fluorescent")
						.withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC),
				false);
		world.playSound(null, sp.getX(), sp.getY(), sp.getZ(),
				SoundEvents.AMBIENT_UNDERWATER_EXIT, SoundSource.PLAYERS, 1.0F, 1.2F);
		world.playSound(null, sp.getX(), sp.getY(), sp.getZ(),
				SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7F, 1.0F);
		return true;
	}
}
