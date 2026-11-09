package github.com.gengyoubo.sscfa.ability;

import github.com.gengyoubo.sscfa.util.PowerUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AllaySPTotem {

	private static final String ACTIVE_TAG = "ssc_totem_active";
	// 婵€娲诲浘鑵惧綊灞炴爣璁帮紙2026-10-20锛氱韬け鏁堣鍒?R3锛涚寮€ SP 鎮︾伒鏈汉鑳屽寘鍗虫竻闄ゆ縺娲伙級
	private static final String OWNER_TAG = "ssc_totem_owner";
	// 婵€娲诲喎鍗达紙R1锛夛細榛樿 20 绉掞紱balance 鍙鐩栵紙abilities.allay_sp_totem.activate_cooldown_ticks锛?
	private static final int ACTIVATE_COOLDOWN_TICKS = 400;
	// 鍥捐吘鏁戞彺鑼冨洿锛氶粯璁?20 鏍硷紱杩愯鏃朵粠 balance 蹇収璇诲彇锛坅bilities.allay_sp_totem锛?
	private static final double RANGE = 20.0;
	// 浣跨敤UUID杩借釜鎸佹湁婵€娲诲浘鑵剧殑鐜╁锛岄伩鍏嶅瓨鍌ㄥ疄浣撳紩鐢ㄥ鑷磋法缁村害/閲嶈繛鍚庡紩鐢ㄨ繃鏈?
	private static final Set<UUID> playersWithActiveTotems = ConcurrentHashMap.newKeySet();
	// 婵€娲诲喎鍗磋褰曪紙R1锛歶uid -> 鏈€杩戜竴娆℃縺娲荤殑涓栫晫鏃堕棿锛涗細璇濈骇锛屾柇绾?閲嶅惎娓呴浂浠呮崯澶辨渶闀?0绉掑喎鍗达級
	private static final java.util.Map<UUID, Long> ACTIVATE_COOLDOWNS = new ConcurrentHashMap<>();

	// 闃舵 5锛氭湇鍔＄鏉冨▉蹇収璇诲彇锛堝揩鐓ф湭鍒濆鍖栧洖閫€榛樿甯搁噺锛?
	private static final github.com.gengyoubo.sscfa.balance.BalanceReader BAL =
			new github.com.gengyoubo.sscfa.balance.BalanceReader("abilities.allay_sp_totem");

	private AllaySPTotem() {
		// Utility class
	}

	public static void init() {
		github.com.gengyoubo.sscfa.util.SscEvents.useItem(AllaySPTotem::onUseItem);
		// Register to server tick event but only check players with active totems
		github.com.gengyoubo.sscfa.util.SscEvents.onServerTick(AllaySPTotem::onServerTick);
	}

	private static void onServerTick(MinecraftServer server) {
		// 姣?0tick锛?绉掞級妫€鏌ヤ竴娆★紝骞宠　鍝嶅簲鎬у拰鎬ц兘
		long currentTick = server.overworld().getGameTime();
		if (currentTick % 40 != 0) {
			return;
		}

		// 浣跨敤Iterator瀹夊叏閬嶅巻骞剁Щ闄わ紝閬垮厤ConcurrentModificationException
		Iterator<UUID> it = playersWithActiveTotems.iterator();
		while (it.hasNext()) {
			UUID uuid = it.next();
			ServerPlayer player = server.getPlayerList().getPlayer(uuid);

			// 鐜╁绂荤嚎鎴栦笉瀛樺湪锛岀Щ闄よ拷韪?
			if (player == null) {
				it.remove();
				continue;
			}

			// 妫€鏌ユ槸鍚︿粛鎸佹湁婵€娲荤殑鍥捐吘锛圧3锛氬繀椤讳粛鍦ㄦ湰浜虹殑鑳屽寘/鎵嬩笂锛?
			boolean stillHasActiveTotem = false;

			for (ItemStack stack : player.getInventory().items) {
				if (isOwnedActiveTotem(stack, player)) {
					stillHasActiveTotem = true;
					break;
				}
			}

			if (!stillHasActiveTotem) {
				for (ItemStack stack : player.getInventory().offhand) {
					if (isOwnedActiveTotem(stack, player)) {
						stillHasActiveTotem = true;
						break;
					}
				}
			}

			if (!stillHasActiveTotem) {
				it.remove();
				continue;
			}

			// 涓嶅啀鏄疭P鎮︾伒鏃讹細鍏抽棴鍥捐吘骞剁Щ闄よ拷韪?
			if (!isSpAllay(player)) {
				deactivateAllTotems(player);
				it.remove();
			}
		}
	}

	private static void deactivateAllTotems(ServerPlayer player) {
		// Check main inventory and offhand
		boolean deactivatedAny = false;

		// Check main inventory
		for (ItemStack stack : player.getInventory().items) {
			if (isActiveTotem(stack)) {
				deactivateTotem(stack);
				deactivatedAny = true;
			}
		}

		// Check offhand
		for (ItemStack stack : player.getInventory().offhand) {
			if (isActiveTotem(stack)) {
				deactivateTotem(stack);
				deactivatedAny = true;
			}
		}

		if (deactivatedAny) {
			player.displayClientMessage(Component.translatable("message.ssc_addon.totem.deactivated"), true);
			player.playNotifySound(SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 1.0f, 0.5f);
		}
	}

	/**
	 * R2锛氱帺瀹舵暣涓儗鍖咃紙涓昏儗鍖?鍓墜锛夊唴鏄惁宸叉湁浠讳竴婵€娲诲浘鑵撅紙涓嶇湅褰掑睘鈥斺€斿埆浜虹殑婵€娲诲浘鑵句篃浼氬湪鏈?tick 琚竻锛夈€?
	 */
	private static boolean hasAnyActiveTotem(Player player) {
		for (ItemStack stack : player.getInventory().items) {
			if (isActiveTotem(stack)) return true;
		}
		for (ItemStack stack : player.getInventory().offhand) {
			if (isActiveTotem(stack)) return true;
		}
		return false;
	}

	/** 鏃ф。鍏煎锛氭竻鎺夎儗鍖呭唴銆屾湁 ACTIVE 鏃?OWNER銆嶇殑鏃ф牸寮忔爣璁帮紙鏀瑰姩鍓嶆縺娲荤殑鍥捐吘鑷劧闄嶇骇涓烘櫘閫氬浘鑵撅級銆?*/
	private static void purgeLegacyMarks(Player player) {
		for (ItemStack stack : player.getInventory().items) {
			if (isActiveTotem(stack) && !stack.getTag().hasUUID(OWNER_TAG)) deactivateTotem(stack);
		}
		for (ItemStack stack : player.getInventory().offhand) {
			if (isActiveTotem(stack) && !stack.getTag().hasUUID(OWNER_TAG)) deactivateTotem(stack);
		}
	}

	private static void deactivateTotem(ItemStack stack) {
		CompoundTag nbt = stack.getOrCreateTag();
		nbt.remove(ACTIVE_TAG);
		nbt.remove(OWNER_TAG);
	}

	private static InteractionResultHolder<ItemStack> onUseItem(Player player, net.minecraft.world.level.Level world, InteractionHand hand) {
		if (world.isClientSide) return InteractionResultHolder.pass(player.getItemInHand(hand));

		ItemStack stack = player.getItemInHand(hand);

		// Only function for Totem of Undying
		if (!stack.is(Items.TOTEM_OF_UNDYING)) {
			return InteractionResultHolder.pass(stack);
		}

		// Must be SP Allay locally checked
		if (!isSpAllay(player)) {
			return InteractionResultHolder.pass(stack);
		}

		// Toggle Active State
		CompoundTag nbt = stack.getOrCreateTag();
		boolean isActive = nbt.getBoolean(ACTIVE_TAG);

		// 鏃ф。鍏煎锛氭竻闄よ儗鍖呭唴閬楃暀鐨勬棫鏍煎紡婵€娲绘爣璁帮紙鏃?OWNER_TAG 鐨?ACTIVE 鏍囪涓嶅啀鏈夋晥锛?
		purgeLegacyMarks(player);

		if (isActive) {
			// Deactivate
			deactivateTotem(stack);

			player.displayClientMessage(Component.translatable("message.ssc_addon.totem.deactivated"), true);
			player.playNotifySound(SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 1.0f, 0.5f);

			if (player instanceof ServerPlayer serverPlayer) {
				updateActiveTotemTracking(serverPlayer, false);
			}
		} else {
			// 婵€娲伙細涓夋潯鏂拌鍒欙紙2026-10-20锛?
			//   R1 婵€娲诲喎鍗?20 绉掞紙浼氳瘽绾ч潤鎬?Map锛屾寜鏈嶅姟鍣ㄤ笘鐣屾椂闂磋鏃讹級
			//   R2 鑳屽寘鍏ㄥ眬鍞竴锛氬凡鎸佹湁浠讳竴婵€娲诲浘鑵惧垯鎷掔粷婵€娲荤浜屼釜
			//   R3 绂昏韩澶辨晥锛氭縺娲绘椂鎵?owner 鏍囪锛涙帀钀界墿/瀛樺鍣ㄧ粡 mixin 娓呴櫎锛泃ick 鏍￠獙褰掑睘
			long now = player.level().getGameTime();
			long cooldownTicks = BAL.i("activate_cooldown_ticks", ACTIVATE_COOLDOWN_TICKS);
			Long lastActivate = ACTIVATE_COOLDOWNS.get(player.getUUID());
			if (lastActivate != null && now - lastActivate < cooldownTicks) {
				player.displayClientMessage(Component.translatable("message.ssc_addon.totem.cooldown"), true);
				player.playNotifySound(SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5f, 1.0f);
				return InteractionResultHolder.fail(stack);
			}

			// R2 鑳屽寘鍏ㄥ眬鍞竴锛氬凡鎸佹湁浠讳竴婵€娲诲浘鑵惧垯鎷掔粷婵€娲荤浜屼釜
			if (hasAnyActiveTotem(player)) {
				player.displayClientMessage(Component.translatable("message.ssc_addon.totem.already_active"), true);
				player.playNotifySound(SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5f, 1.0f);
				return InteractionResultHolder.fail(stack);
			}

			// 婵€娲?
			nbt.putBoolean(ACTIVE_TAG, true);
			nbt.putUUID(OWNER_TAG, player.getUUID());

			// Glint is derived from our activation marker; original enchantments and HideFlags remain owned by the item.

			// R1 鍐峰嵈鍐欏叆
			ACTIVATE_COOLDOWNS.put(player.getUUID(), now);

			player.displayClientMessage(Component.translatable("message.ssc_addon.totem.activated"), true);
			player.playNotifySound(SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 1.0f, 2.0f);

			if (player instanceof ServerPlayer serverPlayer) {
				updateActiveTotemTracking(serverPlayer, true);
			}
		}

		return InteractionResultHolder.success(stack);
	}

	/**
	 * Called by Mixin when an entity would die or use a totem.
	 *
	 * @param entity The entity attempting to use a totem.
	 * @return true if an Allay SP totem was used and prevented death.
	 */
	public static boolean tryUseAllayTotem(LivingEntity entity) {
		if (entity.level().isClientSide) return false;

		// Get nearby players within range锛坆alance 蹇収鍙鐩栨晳鎻磋寖鍥达級
		AABB box = entity.getBoundingBox().inflate(BAL.d("range", RANGE));
		List<Player> nearbyPlayers = entity.level().getEntitiesOfClass(Player.class, box, p -> p instanceof ServerPlayer);

		for (Player player : nearbyPlayers) {
			if (!(player instanceof ServerPlayer serverPlayer)) continue;

			// a. Check if they are SP Allay
			if (!isSpAllay(serverPlayer)) continue;

			// b. Check whitelist锛氳嚜鎴戞晳鎻存€绘槸鍏佽锛涙晳浠栦汉缁熶竴璧?isBuffTarget
			//   - whitelistEnabled = false锛氫粎浣滅敤浜庨潪鎬墿/闈炴晫瀵圭敓鐗?
			//   - whitelistEnabled = true锛氱櫧鍚嶅崟绌烘椂鐜╁/椹湇瀹犵墿/owner-tag锛涢潪绌烘椂浠呯櫧鍚嶅崟鎴愬憳
			if (entity != serverPlayer) {
				if (!github.com.gengyoubo.sscfa.util.WhitelistUtils.isBuffTarget(serverPlayer, entity)) {
					continue;
				}
			}

			// c. Check inventory for Active Totem
			ItemStack activeTotem = findActiveTotem(serverPlayer);

			if (!activeTotem.isEmpty()) {
				// d. Consume totem
				activeTotem.shrink(1);

				// e. Trigger Effect on DYING ENTITY
				entity.setHealth(1.0F);
				entity.removeAllEffects();
				entity.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1)); // 5 seconds
				entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1)); // 45 seconds
				entity.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0)); // 40 seconds

				// Visuals: Totem of Undying particle/sound
				entity.level().broadcastEntityEvent(entity, (byte) 35);

				// Notify SP Allay
				serverPlayer.displayClientMessage(Component.translatable("message.ssc_addon.totem.triggered", entity.getDisplayName()), true);

				return true; // Prevent death
			}
		}

		return false; // Did not prevent death
	}

	private static ItemStack findActiveTotem(ServerPlayer player) {
		// R3锛氬彧璁ゃ€屾湰浜烘縺娲汇€嶇殑鍥捐吘锛堟崱鍒?杞潵鐨勫埆浜烘縺娲诲浘鑵句笉浼氱粰鏁戞彺鏁堟灉锛?
		if (isOwnedActiveTotem(player.getMainHandItem(), player)) return player.getMainHandItem();
		if (isOwnedActiveTotem(player.getOffhandItem(), player)) return player.getOffhandItem();

		// Check inventory main
		for (ItemStack stack : player.getInventory().items) {
			if (isOwnedActiveTotem(stack, player)) return stack;
		}

		return ItemStack.EMPTY;
	}

	private static boolean isActiveTotem(ItemStack stack) {
		// Check if item is Totem and has active tag
		if (stack.getTag() != null) {
			return !stack.isEmpty() && stack.is(Items.TOTEM_OF_UNDYING) && stack.hasTag() && stack.getTag().getBoolean(ACTIVE_TAG);
		}
		return false;
	}

	/** R3锛氭縺娲诲浘鑵?+ 褰掑睘鏍￠獙锛坥wner 鏍囪蹇呴』绛変簬璇ョ帺瀹?uuid锛夈€?*/
	private static boolean isOwnedActiveTotem(ItemStack stack, Player player) {
		return isActiveTotem(stack)
				&& stack.getTag().hasUUID(OWNER_TAG)
				&& stack.getTag().getUUID(OWNER_TAG).equals(player.getUUID());
	}

	// ---- R3 闈欐€佹竻闄ゅ叆鍙ｏ紙mixin 璋冪敤锛氭帀钀界墿 tick / 瀹瑰櫒 setStack锛?---

	/** 鏄惁涓烘縺娲诲浘鑵撅紙mixin 鐢紱鍙湅鏍囪锛屼笉鐪嬪綊灞炩€斺€旂韬満鏅竴寰嬫竻闄わ級銆?*/
	public static boolean isMarkedActiveTotem(ItemStack stack) {
		return isActiveTotem(stack);
	}

	/** 娓呴櫎鍥捐吘婵€娲绘爣璁帮紙mixin 鐢紱鍚屾娓?owner 涓庡厜鏁?NBT锛夈€?*/
	public static void clearTotemMark(ItemStack stack) {
		if (stack.is(Items.TOTEM_OF_UNDYING) && stack.hasTag()) {
			deactivateTotem(stack);
		}
	}

	private static boolean isSpAllay(Player player) {
		return player instanceof ServerPlayer sp && PowerUtils.isSpAllay(sp);
	}

	private static void updateActiveTotemTracking(ServerPlayer player, boolean hasActiveTotem) {
		if (hasActiveTotem) {
			playersWithActiveTotems.add(player.getUUID());
		} else {
			playersWithActiveTotems.remove(player.getUUID());
		}
	}

	public static void clearPlayer(ServerPlayer player) {
		playersWithActiveTotems.remove(player.getUUID());
		ACTIVATE_COOLDOWNS.remove(player.getUUID());
	}

	public static void clearAll() {
		playersWithActiveTotems.clear();
		ACTIVATE_COOLDOWNS.clear();
	}
}
