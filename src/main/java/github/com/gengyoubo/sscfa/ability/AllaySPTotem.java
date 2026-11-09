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
	// 激活图腾归属标记（2026-10-20：离身失效规?R3；离开 SP 悦灵本人背包即清除激活）
	private static final String OWNER_TAG = "ssc_totem_owner";
	// 激活冷却（R1）：默认 20 秒；balance 可覆盖（abilities.allay_sp_totem.activate_cooldown_ticks?
	private static final int ACTIVATE_COOLDOWN_TICKS = 400;
	// 图腾救援范围：默?20 格；运行时从 balance 快照读取（abilities.allay_sp_totem?
	private static final double RANGE = 20.0;
	// 使用UUID追踪持有激活图腾的玩家，避免存储实体引用导致跨维度/重连后引用过?
	private static final Set<UUID> playersWithActiveTotems = ConcurrentHashMap.newKeySet();
	// 激活冷却记录（R1：uuid -> 最近一次激活的世界时间；会话级，断?重启清零仅损失最?0秒冷却）
	private static final java.util.Map<UUID, Long> ACTIVATE_COOLDOWNS = new ConcurrentHashMap<>();

	// 阶段 5：服务端权威快照读取（快照未初始化回退默认常量?
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
		// ?0tick?秒）检查一次，平衡响应性和性能
		long currentTick = server.overworld().getGameTime();
		if (currentTick % 40 != 0) {
			return;
		}

		// 使用Iterator安全遍历并移除，避免ConcurrentModificationException
		Iterator<UUID> it = playersWithActiveTotems.iterator();
		while (it.hasNext()) {
			UUID uuid = it.next();
			ServerPlayer player = server.getPlayerList().getPlayer(uuid);

			// 玩家离线或不存在，移除追?
			if (player == null) {
				it.remove();
				continue;
			}

			// 检查是否仍持有激活的图腾（R3：必须仍在本人的背包/手上?
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

			// 不再是SP悦灵时：关闭图腾并移除追?
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
	 * R2：玩家整个背包（主背?副手）内是否已有任一激活图腾（不看归属——别人的激活图腾也会在?tick 被清）?
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

	/** 旧档兼容：清掉背包内「有 ACTIVE ?OWNER」的旧格式标记（改动前激活的图腾自然降级为普通图腾）?*/
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

		// 旧档兼容：清除背包内遗留的旧格式激活标记（?OWNER_TAG ?ACTIVE 标记不再有效?
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
			// 激活：三条新规则（2026-10-20?
			//   R1 激活冷?20 秒（会话级静?Map，按服务器世界时间计时）
			//   R2 背包全局唯一：已持有任一激活图腾则拒绝激活第二个
			//   R3 离身失效：激活时?owner 标记；掉落物/存容器经 mixin 清除；tick 校验归属
			long now = player.level().getGameTime();
			long cooldownTicks = BAL.i("activate_cooldown_ticks", ACTIVATE_COOLDOWN_TICKS);
			Long lastActivate = ACTIVATE_COOLDOWNS.get(player.getUUID());
			if (lastActivate != null && now - lastActivate < cooldownTicks) {
				player.displayClientMessage(Component.translatable("message.ssc_addon.totem.cooldown"), true);
				player.playNotifySound(SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5f, 1.0f);
				return InteractionResultHolder.fail(stack);
			}

			// R2 背包全局唯一：已持有任一激活图腾则拒绝激活第二个
			if (hasAnyActiveTotem(player)) {
				player.displayClientMessage(Component.translatable("message.ssc_addon.totem.already_active"), true);
				player.playNotifySound(SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5f, 1.0f);
				return InteractionResultHolder.fail(stack);
			}

			// 婵€娲?
			nbt.putBoolean(ACTIVE_TAG, true);
			nbt.putUUID(OWNER_TAG, player.getUUID());

			// Glint is derived from our activation marker; original enchantments and HideFlags remain owned by the item.

			// R1 冷却写入
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

		// Get nearby players within range（balance 快照可覆盖救援范围）
		AABB box = entity.getBoundingBox().inflate(BAL.d("range", RANGE));
		List<Player> nearbyPlayers = entity.level().getEntitiesOfClass(Player.class, box, p -> p instanceof ServerPlayer);

		for (Player player : nearbyPlayers) {
			if (!(player instanceof ServerPlayer serverPlayer)) continue;

			// a. Check if they are SP Allay
			if (!isSpAllay(serverPlayer)) continue;

			// b. Check whitelist：自我救援总是允许；救他人统一?isBuffTarget
			//   - whitelistEnabled = false：仅作用于非怪物/非敌对生?
			//   - whitelistEnabled = true：白名单空时玩家/驯服宠物/owner-tag；非空时仅白名单成员
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
		// R3：只认「本人激活」的图腾（捡?转来的别人激活图腾不会给救援效果?
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

	/** R3：激活图?+ 归属校验（owner 标记必须等于该玩?uuid）?*/
	private static boolean isOwnedActiveTotem(ItemStack stack, Player player) {
		return isActiveTotem(stack)
				&& stack.getTag().hasUUID(OWNER_TAG)
				&& stack.getTag().getUUID(OWNER_TAG).equals(player.getUUID());
	}

	// ---- R3 静态清除入口（mixin 调用：掉落物 tick / 容器 setStack?---

	/** 是否为激活图腾（mixin 用；只看标记，不看归属——离身场景一律清除）?*/
	public static boolean isMarkedActiveTotem(ItemStack stack) {
		return isActiveTotem(stack);
	}

	/** 清除图腾激活标记（mixin 用；同步?owner 与光?NBT）?*/
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
