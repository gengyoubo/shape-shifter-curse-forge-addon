package github.com.gengyoubo.sscfa.ability;

import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.util.FormUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import java.util.HashSet;
import java.util.Set;

/**
 * 红使?/ SP 形态周期逻辑管理器（jackcooper）??
 *
 * <p>原??{@code RedFormTickMixin} ??{@code ServerPlayerEntity.tick} ??HEAD 注入（每玩家?tick）??
 * 2026-09-09 迁移??{@code ServerTickEvents.START_WORLD_TICK} 世界级事件遍历—??
 * 逻辑本就?{@code player.age % 20 == 0} 一秒门控，行为零差异，删掉最高频注入点??/p>
 *
 * <p>职责（每秒一次，遍历各世界玩家）?/p>
 * <ul>
 *   <li>SP 使魔诅咒之月 5% 概率变身红使魔（10 分钟超时回退，粒子掩护）??/li>
 *   <li>SP 悦灵自动发??回收治疗????)与唱片机(??)??/li>
 *   <li>红使魔自动发?回收药水????)（回收时倒出内容物）?/li>
 *   <li>红使魔超时标签清理与回退?/li>
 * </ul>
 */
public final class RedFormTickManager {
	private RedFormTickManager() {
	}

	// 阶??5：服务端权威快照读取（快照未初始化回退默认值）
	private static final BalanceReader BAL = new BalanceReader("abilities.red_form");

	public static void init() {
		github.com.gengyoubo.sscfa.compat.FormChangeEvents.register((player, previous, current) -> {
			if (!player.level().isClientSide && previous != null && current != null
					&& previous.id().equals(github.com.gengyoubo.sscfa.util.FormIdentifiers.FAMILIAR_FOX_RED)
					&& !current.id().equals(github.com.gengyoubo.sscfa.util.FormIdentifiers.FAMILIAR_FOX_RED)) {
				player.getTags().removeIf(tag -> tag.startsWith("ssc_addon_red_expire:"));
			}
		});
	}

	public static void tick(ServerPlayer player) {
		// 一秒门控（与原 per-player tick 注入一致）
		if (player.tickCount % 20 != 0) {
			return;
		}

		boolean isCursedMoon = net.onixary.shapeShifterCurseForge.other.cursedmoon.CursedMoonService.isCursedMoonDay(player.level());

		// 非诅咒之月清除变身尝试标??
		if (!isCursedMoon && player.getTags().contains("ssc_addon_red_attempted")) {
			player.getTags().remove("ssc_addon_red_attempted");
		}

		// 药水袋逻辑标??
		net.onixary.shapeShifterCurseForge.form.FormDefinition currentForm = FormUtils.getCurrentForm(player);
		boolean isRedForm = currentForm != null && currentForm.id().equals(github.com.gengyoubo.sscfa.util.FormIdentifiers.FAMILIAR_FOX_RED);

		// SP 形??+ 诅咒之月变身红使魔??%??
		if (currentForm != null && currentForm.id().equals(github.com.gengyoubo.sscfa.util.FormIdentifiers.FAMILIAR_FOX_SP) && isCursedMoon && !player.getTags().contains("ssc_addon_red_attempted")) {
			player.addTag("ssc_addon_red_attempted");
			if (player.getRandom().nextFloat() < BAL.d("transform_chance", 0.05)) {
				ResourceLocation redFormId = github.com.gengyoubo.sscfa.util.FormIdentifiers.FAMILIAR_FOX_RED;
				net.onixary.shapeShifterCurseForge.form.FormDefinition redForm = net.onixary.shapeShifterCurseForge.form.FormRegistry.get(redFormId);
				if (redForm != null) {
					net.onixary.shapeShifterCurseForge.form.TransformManager.immediatelyTransform(player, redForm.id());

					// 10 分??= 12000 tick（默认；balance 可覆?duration_ticks??
					long expireTime = player.level().getGameTime() + BAL.i("duration_ticks", 12000);
					player.addTag("ssc_addon_red_expire:" + expireTime);

					player.displayClientMessage(Component.translatable("message.ssc_addon.red_transformation_special").withStyle(ChatFormatting.GREEN), false);
					player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 1.0F, 1.0F);
					return; // 变身成功后直接返??
				}
			}
		}

		// 两轮独立背包扫描合并为单轮（同一次循环内按形态三元处理三类物品）?
		// ??20t 少扫一整轮 41 格；行为等价（三物品互不重叠，回收逻辑照旧）??
		boolean isAllaySp = currentForm != null && currentForm.id().equals(github.com.gengyoubo.sscfa.util.FormIdentifiers.ALLAY_SP);
		if (isAllaySp) {
			placeFormItemSafe(player, 0, github.com.gengyoubo.sscfa.init.AddonItems.ALLAY_HEAL_WAND.get());
			placeFormItemSafe(player, 1, github.com.gengyoubo.sscfa.init.AddonItems.ALLAY_JUKEBOX.get());
		} else {
			for (int i = 0; i < player.getInventory().getContainerSize(); ++i) {
				ItemStack stack = player.getInventory().getItem(i);
				if (stack.is(github.com.gengyoubo.sscfa.init.AddonItems.ALLAY_HEAL_WAND.get())) {
					player.getInventory().setItem(i, ItemStack.EMPTY);
				} else if (stack.is(github.com.gengyoubo.sscfa.init.AddonItems.ALLAY_JUKEBOX.get())) {
					player.getInventory().setItem(i, ItemStack.EMPTY);
				}
			}
		}

		// 红使魔：自动发放药水?8)；非该形态则回收（倒出内容物后移除??
		if (isRedForm) {
			placeFormItemSafe(player, 8, github.com.gengyoubo.sscfa.init.AddonItems.POTION_BAG.get());
		} else {
			for (int i = 0; i < player.getInventory().getContainerSize(); ++i) {
				ItemStack stack = player.getInventory().getItem(i);
				if (stack.is(github.com.gengyoubo.sscfa.init.AddonItems.POTION_BAG.get())) {
					if (stack.getTag() != null && stack.hasTag() && stack.getTag().contains("Items", 9)) {
						ListTag list = stack.getTag().getList("Items", 10);
						for (int j = 0; j < list.size(); ++j) {
							CompoundTag itemTag = list.getCompound(j);
							ItemStack contentStack = ItemStack.of(itemTag);
							if (!contentStack.isEmpty()) {
								player.drop(contentStack, false, true);
							}
						}
					}
					player.getInventory().setItem(i, ItemStack.EMPTY);
				}
			}
		}

		// 红使魔超时回??
		Set<String> tagsToRemove = new HashSet<>();
		boolean shouldRevert = false;
		long currentTime = player.level().getGameTime();

		for (String tag : player.getTags()) {
			if (tag.startsWith("ssc_addon_red_expire:")) {
				boolean pendingRed = net.onixary.shapeShifterCurseForge.form.TransformManager.isTransforming(player);
				if (!isRedForm) {
					if (!pendingRed) tagsToRemove.add(tag);
					continue;
				}
				try {
					long expireTime = Long.parseLong(tag.split(":")[1]);
					if (currentTime >= expireTime) {
						shouldRevert = true;
						tagsToRemove.add(tag);
					}
				} catch (NumberFormatException ignored) {
					tagsToRemove.add(tag); // 无效标签直接移除
				}
			}
		}

		if (!tagsToRemove.isEmpty()) {
			for (String tag : tagsToRemove) {
				player.getTags().remove(tag);
			}
		}

		if (shouldRevert) {
			ResourceLocation spFormId = github.com.gengyoubo.sscfa.util.FormIdentifiers.FAMILIAR_FOX_SP;
			net.onixary.shapeShifterCurseForge.form.FormDefinition spForm = net.onixary.shapeShifterCurseForge.form.FormRegistry.get(spFormId);
			if (spForm != null) {
				// ??immediatelyTransform 避免动画
				net.onixary.shapeShifterCurseForge.form.TransformManager.immediatelyTransform(player, spForm.id());

				// 白色粒子大量覆盖玩家（owner 打标：仅本人第一人称避让，变身演出他人视角原样）
				if (player.level() instanceof ServerLevel serverWorld) {
					github.com.gengyoubo.sscfa.network.DecorationParticles.forced(serverWorld, player, ParticleTypes.CLOUD, player.getX(), player.getY() + 1.0, player.getZ(), 100, 0.5, 1.0, 0.5, 0.1, 512);
					github.com.gengyoubo.sscfa.network.DecorationParticles.forced(serverWorld, player, ParticleTypes.POOF, player.getX(), player.getY() + 1.0, player.getZ(), 50, 0.5, 1.0, 0.5, 0.1, 512);
				}

				player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
				player.removeEffect(MobEffects.JUMP);

				player.displayClientMessage(Component.translatable("message.ssc_addon.red_revert_timeout").withStyle(ChatFormatting.GREEN), false);
			}
		}
	}

	/**
	 * 安全地把指定形态物品放进固定槽位，避免覆盖玩家原有物品??
	 * ①目标槽已是该物品则不动；②原物品合并进背包同种堆；③剩余找空槽；④仍剩余丢地上，绝不静默删除?
	 */
	private static void placeFormItemSafe(ServerPlayer player, int targetSlot, Item formItem) {
		Inventory inv = player.getInventory();
		ItemStack existing = inv.getItem(targetSlot);

		if (existing.is(formItem)) {
			return;
		}

		ItemStack moved = existing.copy();
		inv.setItem(targetSlot, ItemStack.EMPTY);

		if (!moved.isEmpty()) {
			for (int i = 0; i < inv.items.size() && !moved.isEmpty(); ++i) {
				if (i == targetSlot) continue;
				ItemStack slotStack = inv.items.get(i);
				if (slotStack.isEmpty()) continue;
				if (!ItemStack.isSameItemSameTags(slotStack, moved)) continue;
				int room = slotStack.getMaxStackSize() - slotStack.getCount();
				if (room <= 0) continue;
				int merge = Math.min(room, moved.getCount());
				slotStack.grow(merge);
				moved.shrink(merge);
			}

			if (!moved.isEmpty()) {
				for (int i = 0; i < inv.items.size() && !moved.isEmpty(); ++i) {
					if (i == targetSlot) continue;
					if (!inv.items.get(i).isEmpty()) continue;
					inv.items.set(i, moved.copy());
					moved.setCount(0);
				}
			}

			if (!moved.isEmpty()) {
				player.drop(moved, false, true);
			}
		}

		inv.setItem(targetSlot, new ItemStack(formItem));
	}
}
