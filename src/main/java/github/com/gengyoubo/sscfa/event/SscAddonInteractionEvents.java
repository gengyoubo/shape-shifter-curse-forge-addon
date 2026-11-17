package github.com.gengyoubo.sscfa.event;

import github.com.gengyoubo.sscfa.SscAddon;
import github.com.gengyoubo.sscfa.ability.MancianimaPassive;
import github.com.gengyoubo.sscfa.init.AddonEffects;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.FormUtils;
import github.com.gengyoubo.sscfa.util.SscEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Evoker;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BellBlock;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

/**
 * Per-form interaction rules, ported from the Fabric {@code SscAddonInteractionEvents}:
 * <ul>
 *   <li>Mancianima: merchants it kills drop loot, it cannot trade, an evoker plus a nether star gives
 *       two totems, and ringing a bell starts a village assault;</li>
 *   <li>vampire bat in mist form: every left and right click is blocked;</li>
 *   <li>axolotl SP: empty main hand plus an arrow in the off hand "crafts" a water spear;</li>
 *   <li>Netherjudge: a wither rose aimed at a block is eaten instead of planted
 *       ({@code AnubisEatWitherRoseItemMixin} drives the eating).</li>
 * </ul>
 */
public final class SscAddonInteractionEvents {
	private static boolean registered;

	private SscAddonInteractionEvents() {}

	public static synchronized void register() {
		if (registered) return;
		registered = true;

		SscEvents.onLivingDeath((entity, source) -> {
			if (entity.level().isClientSide()) return;
			MancianimaPassive.onAssaultTargetDeath(entity);
			if (entity instanceof AbstractVillager merchant && source.getEntity() instanceof ServerPlayer killer
					&& FormUtils.isForm(killer, FormIdentifiers.FAMILIAR_FOX_MANCIANIMA)) {
				MancianimaPassive.onMerchantKilledByMancianima(merchant, killer);
			}
		});

		MinecraftForge.EVENT_BUS.addListener((PlayerInteractEvent.EntityInteract event) -> {
			InteractionResult result = interactEntity(event.getEntity(), event.getLevel(), event.getHand(), event.getTarget());
			if (result != InteractionResult.PASS) {
				event.setCanceled(true);
				event.setCancellationResult(result);
			}
		});
		MinecraftForge.EVENT_BUS.addListener((PlayerInteractEvent.EntityInteractSpecific event) -> {
			if (inMistForm(event.getEntity())) {
				event.setCanceled(true);
				event.setCancellationResult(InteractionResult.FAIL);
			}
		});

		SscEvents.useBlock((player, world, hand, hit) -> {
			if (inMistForm(player)) return InteractionResult.FAIL;
			// Netherjudge holding a wither rose: do not plant it, let use() start eating.
			if (hand == InteractionHand.MAIN_HAND && player.getMainHandItem().is(Items.WITHER_ROSE)
					&& FormUtils.isForm(player, FormIdentifiers.ANUBIS_WOLF_SP)) return InteractionResult.FAIL;
			// Mancianima rings a bell: try to start a village assault (once per in-game day); the bell still rings.
			if (!world.isClientSide() && player instanceof ServerPlayer sp
					&& FormUtils.isForm(sp, FormIdentifiers.FAMILIAR_FOX_MANCIANIMA)
					&& world.getBlockState(hit.getBlockPos()).getBlock() instanceof BellBlock) {
				MancianimaPassive.tryTriggerAssaultByBell(sp);
			}
			return InteractionResult.PASS;
		});
		SscEvents.useItem((player, world, hand) -> {
			if (inMistForm(player)) return InteractionResultHolder.fail(player.getItemInHand(hand));
			return craftWaterSpear(player, world, hand);
		});

		MinecraftForge.EVENT_BUS.addListener((PlayerInteractEvent.LeftClickBlock event) -> {
			if (inMistForm(event.getEntity())) event.setCanceled(true);
		});
		SscEvents.attackEntity((player, world, hand, entity, hit) ->
				inMistForm(player) ? InteractionResult.FAIL : InteractionResult.PASS);
	}

	private static boolean inMistForm(Player player) {
		return player.hasEffect(AddonEffects.MIST_FORM.get()) && FormUtils.isForm(player, FormIdentifiers.BAT_DESMODUS);
	}

	private static InteractionResult interactEntity(Player player, Level world, InteractionHand hand, Entity entity) {
		if (inMistForm(player)) return InteractionResult.FAIL;
		if (world.isClientSide() || !(player instanceof ServerPlayer sp)
				|| !FormUtils.isForm(sp, FormIdentifiers.FAMILIAR_FOX_MANCIANIMA)) return InteractionResult.PASS;
		// Mancianima cannot trade.
		if (entity instanceof AbstractVillager) return InteractionResult.FAIL;
		if (!(entity instanceof Evoker)) return InteractionResult.PASS;
		ItemStack stack = sp.getItemInHand(hand);
		if (!stack.is(Items.NETHER_STAR)) return InteractionResult.PASS;
		if (!sp.getAbilities().instabuild) stack.shrink(1);
		ItemStack reward = new ItemStack(Items.TOTEM_OF_UNDYING, 2);
		if (!sp.getInventory().add(reward)) sp.drop(reward, false);
		if (sp.level() instanceof ServerLevel sw) {
			sw.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.EVOKER_PREPARE_SUMMON,
					sp.getSoundSource(), 1.0f, 1.2f);
		}
		return InteractionResult.SUCCESS;
	}

	/**
	 * Axolotl SP: with the selected hotbar slot empty and an arrow in the off hand, right-click consumes the
	 * arrow and puts a water spear in that slot (5 s craft cooldown, at most one spear on the player). With
	 * an empty main hand the game only fires the off-hand use, hence the off-hand check.
	 */
	private static InteractionResultHolder<ItemStack> craftWaterSpear(Player player, Level world, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);
		if (hand != InteractionHand.OFF_HAND || !FormUtils.isAxolotlSP(player) || !player.getMainHandItem().isEmpty()
				|| !player.getOffhandItem().is(Items.ARROW) || countSpears(player) > 0) {
			return InteractionResultHolder.pass(held);
		}
		if (world.isClientSide()) {
			return player.getCooldowns().isOnCooldown(Items.ARROW) ? InteractionResultHolder.pass(held)
					: InteractionResultHolder.success(held);
		}
		if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.pass(held);
		Long until = SscAddon.WATER_SPEAR_CRAFT_CD.get(sp.getUUID());
		if (until != null && sp.getServer().getTickCount() < until) return InteractionResultHolder.pass(held);
		int slot = sp.getInventory().selected;
		// Server-side recheck: the selected slot must really be empty.
		if (!sp.getInventory().getItem(slot).isEmpty()) return InteractionResultHolder.pass(held);
		sp.getOffhandItem().shrink(1);
		sp.getInventory().setItem(slot, new ItemStack(SscAddon.WATER_SPEAR));
		sp.getInventory().setChanged();
		if (sp.level() instanceof ServerLevel sw) {
			sw.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.BOTTLE_FILL, sp.getSoundSource(), 0.8f, 1.0f);
			github.com.gengyoubo.sscfa.util.ParticleUtils.spawnWaterBurst(sw, sp.getX(), sp.getY() + 1.0, sp.getZ(), 0.5);
		}
		return InteractionResultHolder.success(sp.getItemInHand(hand));
	}

	private static int countSpears(Player player) {
		int count = 0;
		var inventory = player.getInventory();
		for (int i = 0; i < inventory.getContainerSize(); i++) if (inventory.getItem(i).is(SscAddon.WATER_SPEAR)) count++;
		return count;
	}
}
