package github.com.gengyoubo.sscfa.ability;

import github.com.gengyoubo.sscfa.util.PowerUtils;
import github.com.gengyoubo.sscfa.util.WhitelistUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Server-owned portable beacon: one payment clock and a whitelisted Haste II aura per owner. */
public final class AllaySPPortableBeacon {
    private static final ResourceLocation ACTIVE = new ResourceLocation("my_addon", "form_allay_sp_beacon_active");
    private static final ResourceLocation MANA = new ResourceLocation("my_addon", "form_allay_sp_mana_resource");
    private static final String SESSION_TAG = "ssca_beacon_session";
    private static final String OWNER_TAG = "ssca_beacon_owner";
    private static final int DRAIN_INTERVAL = 60;
    private static final int EFFECT_INTERVAL = 80;
    private static final double RANGE = 20.0;
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    // Cumulative active ticks belong to the owner, so toggling or swapping beacons cannot reset payment.
    private static final Map<UUID, Integer> DRAIN_TICKS = new HashMap<>();

    private static final class Session {
        final UUID id = UUID.randomUUID();
        ItemStack stack;
        int ticks;

        Session(ItemStack stack) { this.stack = stack; }
    }

    private AllaySPPortableBeacon() { }

    public static void init() {
        github.com.gengyoubo.sscfa.util.SscEvents.useItem(AllaySPPortableBeacon::onUseItem);
        github.com.gengyoubo.sscfa.util.SscEvents.onServerTick(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) tick(player);
        });
        github.com.gengyoubo.sscfa.util.SscEvents.onPlayerJoin(player -> clearPlayer(player));
        github.com.gengyoubo.sscfa.event.PlayerSessionCleanup.register((handler, server) -> clearPlayer(handler.player));
        github.com.gengyoubo.sscfa.util.SscEvents.onServerStopping(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) clearPlayer(player);
            SESSIONS.clear();
            DRAIN_TICKS.clear();
        });
    }

    private static InteractionResultHolder<ItemStack> onUseItem(Player player, Level world, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(Items.BEACON) && !player.isShiftKeyDown()) {
            if (player instanceof ServerPlayer serverPlayer && PowerUtils.isSpAllay(serverPlayer)) {
                toggleBeacon(serverPlayer, stack);
                return InteractionResultHolder.success(stack);
            }
        }
        return InteractionResultHolder.pass(stack);
    }

    public static void toggleBeacon(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (!stack.is(Items.BEACON)) stack = player.getOffhandItem();
        toggleBeacon(player, stack);
    }

    private static void toggleBeacon(ServerPlayer player, ItemStack stack) {
        if (PowerUtils.getResourceValue(player, ACTIVE) == 1 || SESSIONS.containsKey(player.getUUID())) {
            deactivateBeacon(player);
            return;
        }
        if (!PowerUtils.isSpAllay(player) || !stack.is(Items.BEACON)) return;
        if (PowerUtils.getResourceValue(player, MANA) <= 0) {
            player.displayClientMessage(Component.translatable("message.ssc_addon.beacon.no_energy"), true);
            return;
        }
        clearMarkers(player);
        Session session = new Session(stack);
        stack.getOrCreateTag().putUUID(SESSION_TAG, session.id);
        stack.getOrCreateTag().putUUID(OWNER_TAG, player.getUUID());
        SESSIONS.put(player.getUUID(), session);
        PowerUtils.setResourceValueAndSync(player, ACTIVE, 1);
        player.getInventory().setChanged();
        applyAura(player);
        player.playNotifySound(SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.0f);
        player.displayClientMessage(Component.translatable("message.ssc_addon.beacon.activated"), true);
    }

    private static void tick(ServerPlayer player) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null) {
            // Old saved flags and transferred marked stacks never activate a recipient implicitly.
            if (PowerUtils.getResourceValue(player, ACTIVE) != 0) clearPlayer(player);
            else clearMarkers(player);
            return;
        }
        ItemStack carried = findCarried(player, session);
        if (!player.isAlive() || !PowerUtils.isSpAllay(player) || carried.isEmpty()
                || PowerUtils.getResourceValue(player, ACTIVE) != 1
                || PowerUtils.getResourceValue(player, MANA) <= 0) {
            deactivateBeacon(player);
            return;
        }
        if (session.stack != carried) {
            clearStack(session.stack);
            session.stack = carried;
        }
        session.ticks++;
        int drainTicks = DRAIN_TICKS.getOrDefault(player.getUUID(), 0) + 1;
        if (drainTicks >= DRAIN_INTERVAL) {
            DRAIN_TICKS.remove(player.getUUID());
            PowerUtils.changeResourceValueAndSync(player, MANA, -1);
            if (PowerUtils.getResourceValue(player, MANA) <= 0) {
                deactivateBeacon(player);
                return;
            }
        } else DRAIN_TICKS.put(player.getUUID(), drainTicks);
        if (session.ticks % EFFECT_INTERVAL == 0) applyAura(player);
    }

    private static ItemStack findCarried(ServerPlayer player, Session session) {
        Inventory inventory = player.getInventory();
        ItemStack stack = findCarried(inventory.items, player.getUUID(), session);
        if (!stack.isEmpty()) return stack;
        stack = findCarried(inventory.armor, player.getUUID(), session);
        return !stack.isEmpty() ? stack : findCarried(inventory.offhand, player.getUUID(), session);
    }

    private static ItemStack findCarried(java.util.List<ItemStack> stacks, UUID owner, Session session) {
        for (int slot = 0; slot < stacks.size(); slot++) {
            ItemStack stack = stacks.get(slot);
            if (isActiveStack(stack) && stack.getTag().getUUID(OWNER_TAG).equals(owner)
                    && stack.getTag().getUUID(SESSION_TAG).equals(session.id)) return stack;
        }
        return ItemStack.EMPTY;
    }

    private static void applyAura(ServerPlayer player) {
        player.addEffect(haste());
        for (LivingEntity target : player.serverLevel().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(RANGE), target -> target != player && target.isAlive()
                        && !target.isSpectator() && target.distanceToSqr(player) <= RANGE * RANGE
                        && WhitelistUtils.isBuffTarget(player, target))) {
            target.addEffect(haste());
        }
    }

    private static MobEffectInstance haste() {
        return new MobEffectInstance(MobEffects.DIG_SPEED, 100, 1, false, true, true);
    }

    public static void deactivateBeacon(ServerPlayer player) {
        boolean wasActive = SESSIONS.containsKey(player.getUUID()) || PowerUtils.getResourceValue(player, ACTIVE) == 1;
        clearPlayer(player);
        if (wasActive) {
            player.playNotifySound(SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.0f, 1.0f);
            player.displayClientMessage(Component.translatable("message.ssc_addon.beacon.deactivated"), true);
        }
    }

    private static void clearPlayer(ServerPlayer player) {
        Session session = SESSIONS.remove(player.getUUID());
        if (session != null) clearStack(session.stack);
        PowerUtils.setResourceValueAndSync(player, ACTIVE, 0);
        clearMarkers(player);
        if (player.containerMenu != null) clearStack(player.containerMenu.getCarried());
        player.getInventory().setChanged();
    }

    private static void clearMarkers(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        // Keep all 41 slots and their main -> armor -> offhand ordering, without
        // allocating the combinedInventory iterator for every getStack call.
        boolean changed = clearMarkers(inventory.items);
        changed |= clearMarkers(inventory.armor);
        changed |= clearMarkers(inventory.offhand);
        if (changed) inventory.setChanged();
    }

    private static boolean clearMarkers(java.util.List<ItemStack> stacks) {
        boolean changed = false;
        for (int slot = 0; slot < stacks.size(); slot++) {
            ItemStack stack = stacks.get(slot);
            if (isActiveStack(stack)) { clearStack(stack); changed = true; }
        }
        return changed;
    }

    public static boolean isActiveStack(ItemStack stack) {
        return !stack.isEmpty() && stack.is(Items.BEACON) && stack.hasTag()
                && stack.getTag().hasUUID(SESSION_TAG) && stack.getTag().hasUUID(OWNER_TAG);
    }

    /** Clear only our visual marker; real enchantments and unrelated NBT remain intact. */
    public static void clearStack(ItemStack stack) {
        if (stack.is(Items.BEACON) && stack.hasTag()) {
            stack.getTag().remove(SESSION_TAG);
            stack.getTag().remove(OWNER_TAG);
            if (stack.getTag().isEmpty()) stack.setTag(null);
        }
    }
}
