package github.com.gengyoubo.sscfa.neomagic;

import github.com.gengyoubo.sscfa.ability.BeepSheepFormManager;
import github.com.gengyoubo.sscfa.anomaly.SpellAnomalyContext;
import github.com.gengyoubo.sscfa.init.AddonEffects;
import github.com.gengyoubo.sscfa.network.AddonNetwork;
import github.com.gengyoubo.sscfa.spell.CastRangeGuard;
import github.com.gengyoubo.sscfa.spell.FormSpellStyle;
import github.com.gengyoubo.sscfa.spell.FrostSpellLink;
import github.com.gengyoubo.sscfa.spell.OcelotSpellLink;
import github.com.gengyoubo.sscfa.spell.PlayerSpellMastery;
import github.com.gengyoubo.sscfa.spell.SpellCastManager;
import github.com.gengyoubo.sscfa.spell.SpellChannelManager;
import github.com.gengyoubo.sscfa.spell.SpellCastingRules;
import github.com.gengyoubo.sscfa.util.AbilityUseGuard;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.FormUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static github.com.gengyoubo.sscfa.neomagic.INeoModel.Blueprint;
import static github.com.gengyoubo.sscfa.neomagic.INeoModel.Carrier;
import static github.com.gengyoubo.sscfa.neomagic.INeoModel.Core;
import static github.com.gengyoubo.sscfa.neomagic.INeoModel.Profile;
import static github.com.gengyoubo.sscfa.neomagic.INeoModel.Quote;

/** 服务端权威的新魔法运行时：负担、分期支付、失败/偏移、冷却、存储与 HUD。 */
public final class NeoRuntime {
    public static final Map<UUID, Active> ACTIVE = new HashMap<>();
    private static boolean registered;

    public static final class Active {
        public final NeoSession session;
        public final NeoState.Experiment record;
        public final ItemStack wand;
        public final InteractionHand hand;
        public final NeoEffects.Anchor anchor;
        private final Random random;
        public int fullCooldown;
        public double powerMultiplier = 1;
        public double experienceMultiplier = 1;
        public double rangeMultiplier = 1;
        public boolean legacyBook, legacyReleased;
        public UUID refundCastId;
        public float curseDurationMultiplier = 1;
        public CastRangeGuard.Lock targetLock, guidanceLock;
        public FormSpellStyle style;
        public SpellAnomalyContext anomaly;
        public Set<UUID> chainPrimaries = Set.of();
        public int age, beganTick;
        public boolean settled;

        private Active(NeoSession session, NeoState.Experiment record, ItemStack wand, InteractionHand hand,
                       NeoEffects.Anchor anchor, Random random) {
            this.session = session;
            this.record = record;
            this.wand = wand;
            this.hand = hand;
            this.anchor = anchor;
            this.fullCooldown = NeoRules.current().cooldown(session.blueprint);
            this.random = random;
            record.checks = session.checkpoints;
        }
    }

    private NeoRuntime() {}

    public static NeoState.PlayerData data(ServerPlayer player) {
        return NeoState.get(player.getServer()).player(player.getUUID());
    }

    public static Profile profile(ServerPlayer player) {
        return NeoLegacyCasting.profile(player);
    }

    /** 临时羊形态只继承身体容量；施法特质仍按实际羊形态结算。 */
    public static Profile bodyProfile(ServerPlayer player) {
        String id = form(player);
        Profile actual = NeoRules.current().profile(id);
        if (FormIdentifiers.SHEEP_FORM.toString().equals(id)) {
            String original = BeepSheepFormManager.originalFormId(player);
            if (original != null && !original.equals(id)) {
                Profile base = NeoRules.current().profile(original);
                return new Profile(base.safety(), actual.failure(), actual.offset(),
                        actual.merge(), actual.split(), actual.reference());
            }
        }
        return actual;
    }

    public static String form(ServerPlayer player) {
        var form = FormUtils.getCurrentForm(player);
        return form == null || form.id() == null ? "shape-shifter-curse:original_shifter" : form.id().toString();
    }

    public static void refreshForm(ServerPlayer player) {
        var data = data(player);
        String current = form(player);
        if (!current.equals(data.form)) {
            FrostSpellLink.clear(player);
            if (ACTIVE.containsKey(player.getUUID())) cancel(player, "form_changed");
            NeoEffects.clearStored(player.getUUID());
            data.burden = 0;
            data.recovery = 0;
            data.form = current;
            dirty(player);
        }
    }

    public static boolean blocked(ServerPlayer player) {
        return player.hasEffect(AddonEffects.PURIFIED.get()) || AbilityUseGuard.blocked(player)
                || player.isSpectator() || !player.isAlive();
    }

    static void start(ServerPlayer player, InteractionHand hand, Blueprint blueprint) {
        refreshForm(player);
        var data = data(player);
        if (blueprint == null) {
            message(player, "need_blueprint");
            return;
        }
        if (blocked(player) || SpellChannelManager.isCasting(player) || NeoEffects.hasStored(player.getUUID())) {
            message(player, "blocked");
            return;
        }
        if (!NeoRules.current().errors(blueprint).isEmpty() || !data.knows(blueprint)) {
            message(player, "invalid");
            return;
        }
        if (data.cooldowns.getOrDefault(blueprint.identity(), 0) > 0) {
            message(player, "cooldown");
            return;
        }
        NeoEffects.Anchor anchor = NeoEffects.capture(player, blueprint);
        if (!NeoEffects.preflight(player, blueprint, anchor)) {
            message(player, "target_invalid");
            return;
        }
        Quote quote = NeoLegacyCasting.blueprintQuote(player, blueprint);
        var style = FormSpellStyle.plan(player,
                blueprint.range() * NeoLegacyCasting.blueprintRangeMultiplier(player, blueprint),
                blueprint.prep(), blueprint.prep(), -1);
        if (!style.pay()) return;
        var experiment = new NeoState.Experiment();
        experiment.blueprint = blueprint;
        experiment.form = data.form;
        experiment.profile = NeoRules.current().profile(data.form);
        experiment.initial = data.burden;
        experiment.normal = quote.normal();
        experiment.actual = quote.actual();
        experiment.profile = profile(player);
        experiment.preparation = style.timing.duration();
        experiment.dimension = player.level().dimension().location().toString();
        experiment.target = anchor.target() == null ? anchor.position().toString() : anchor.target().toString();
        data.add(experiment);
        var active = new Active(new NeoSession(NeoRules.current(), blueprint, quote, style.timing.duration()),
                experiment, player.getItemInHand(hand), hand, anchor, new Random(player.getRandom().nextLong()));
        active.style = style;
        active.anomaly = NeoEffects.anomaly(player, active);
        style.bindAllay(quote.actual(), active.record,
                blueprint.output() == Core.CURSE
                        && Set.of(Carrier.PROJECTILE, Carrier.CONTACT, Carrier.LOCK).contains(blueprint.carrier()));
        style.bindBat(active.record);
        active.record.novaQuickCast = style.novaQuickCast;
        active.record.moistureSpent = style.paidMoisture();
        active.fullCooldown = NeoLegacyCasting.blueprintCooldown(player, blueprint);
        active.powerMultiplier = NeoLegacyCasting.blueprintPower(player, blueprint);
        NeoLegacyCasting.snapshotBlueprintAffinity(player, active);
        active.experienceMultiplier = NeoLegacyCasting.blueprintExperienceMultiplier(player);
        active.rangeMultiplier = NeoLegacyCasting.blueprintRangeMultiplier(player, blueprint) * style.projectileRange;
        if (blueprint.carrier() == Carrier.LOCK && anchor.target() != null) {
            var target = player.serverLevel().getEntity(anchor.target());
            if (target instanceof net.minecraft.world.entity.LivingEntity living) {
                active.targetLock = CastRangeGuard.entity(living, blueprint.range() * active.rangeMultiplier);
            }
        }
        active.guidanceLock = CastRangeGuard.entity(style.target, style.range);
        active.beganTick = player.getServer().getTickCount();
        ACTIVE.put(player.getUUID(), active);
        if (OcelotSpellLink.wind(player)) {
            SpellChannelManager.applyMovement(player, OcelotSpellLink.walkingMultiplier());
        }
        advance(player, active);
    }

    private static void advance(ServerPlayer player, Active active) {
        if (active.settled) return;
        if ((active.targetLock != null && active.targetLock.exceeded(player))
                || (active.guidanceLock != null && active.guidanceLock.exceeded(player))) {
            failRange(player, active);
            return;
        }
        var data = data(player);
        var session = active.session;
        Profile live = session.needsRisk(active.age) ? profile(player) : null;
        double delta = session.advance(active.age, data.burden, live, active.random);
        data.burden += delta;
        if (delta > 0) {
            data.recovery = NeoRules.current().recoveryDelay();
            data.lastPaymentTick = player.getServer().getTickCount();
        }
        active.record.paid = session.paid;
        active.record.yaw = session.yaw;
        active.record.pitch = session.pitch;
        active.record.reversed = session.reversed;
        active.record.failureBasis = session.failureBasis;
        active.record.failureProbability = session.failureProbability;
        active.record.failureDraws = session.failureDraws;
        active.record.failureBoundary = session.failureBoundary;
        if (session.failed) {
            fail(player, active);
        } else if (session.complete) {
            ACTIVE.remove(player.getUUID());
            if (active.style != null) active.style.endCloak();
            SpellChannelManager.clearSlow(player);
            // 效果只接收一次不可变结果；之后不再对本次施法做崩解检查。
            if (NeoEffects.release(player, active)) {
                active.record.result = session.quote.plan().stored() ? "stored" : "released";
                setCooldown(player, session.blueprint.identity(), active.fullCooldown);
                active.settled = true;
                if (!session.quote.plan().stored()) {
                    data.successful(session.blueprint);
                    grantExperience(player, active);
                }
            } else if (session.hasShifted) {
                fail(player, active);
            } else {
                active.record.result = "target_lost";
                setCooldown(player, session.blueprint.identity(), Math.max(10, active.fullCooldown / 2));
                active.settled = true;
                message(player, "target_invalid");
            }
        }
        dirty(player);
    }

    public static void fail(ServerPlayer player, Active active) {
        if (active.settled) return;
        active.settled = true;
        if (active.style != null) active.style.endCloak();
        ACTIVE.remove(player.getUUID());
        active.record.result = "failed";
        SpellChannelManager.clearSlow(player);
        setCooldown(player, active.session.blueprint.identity(), active.fullCooldown);
        backlash(player, active.session.quote.actual());
    }

    private static void failRange(ServerPlayer player, Active active) {
        if (active.settled) return;
        active.settled = true;
        active.session.failed = true;
        ACTIVE.remove(player.getUUID(), active);
        if (active.style != null) active.style.endCloak();
        active.record.result = "failed";
        active.record.failureReason = "range_exceeded";
        SpellChannelManager.clearSlow(player);
        setCooldown(player, active.session.blueprint.identity(),
                SpellCastingRules.interruptedCooldown(active.fullCooldown));
        sendSuppression(player, NeoLegacyCasting.element(active.session.blueprint.output()).color);
        player.displayClientMessage(NeoMagic.text("range_exceeded"), true);
        dirty(player);
        NeoNetwork.hud(player);
    }

    static void grantExperience(ServerPlayer player, Active active) {
        double amount = active.session.quote.normal() * active.session.blueprint.tier() * active.experienceMultiplier;
        int expTen = (int) Math.min(Integer.MAX_VALUE, Math.round(amount));
        PlayerSpellMastery.grant(player, null, expTen);
        var data = data(player);
        data.spellbookMasteryExpTen = (int) Math.min(Integer.MAX_VALUE, (long) data.spellbookMasteryExpTen + expTen);
        dirty(player);
    }

    public static void backlash(ServerPlayer player, double actualCost) {
        // 连续惩罚按服务器 tick 统一结算，不在失败施法时重复叠加。
        message(player, "failed");
        dirty(player);
    }

    public static void cancel(ServerPlayer player, String reason) {
        Active active = ACTIVE.remove(player.getUUID());
        if (active == null) return;
        if (active.style != null) active.style.endCloak();
        SpellChannelManager.clearSlow(player);
        active.record.result = reason;
        active.settled = true;
        setCooldown(player, active.session.blueprint.identity(), Math.max(10, active.fullCooldown / 2));
        dirty(player);
    }

    public static void releaseOrCancel(ServerPlayer player) {
        if (NeoEffects.hasStored(player.getUUID())) NeoEffects.trigger(player);
        else cancel(player, "cancelled");
    }

    public static void relieve(ServerPlayer player, double amount) {
        refreshForm(player);
        data(player).burden = Math.max(0, data(player).burden - amount);
        dirty(player);
    }

    public static void dirty(ServerPlayer player) {
        NeoState.get(player.getServer()).setDirty();
    }

    public static void message(ServerPlayer player, String message) {
        player.displayClientMessage(NeoMagic.text(message), true);
    }

    private static void setCooldown(ServerPlayer player, String identity, int ticks) {
        var data = data(player);
        data.cooldowns.put(identity, ticks);
        data.cooldownStarted.put(identity, player.getServer().getTickCount());
    }

    private static void sendSuppression(ServerPlayer player, int color) {
        if (!AddonNetwork.canSend(player, SpellChannelManager.SUPPRESSION)) return;
        FriendlyByteBuf buf = AddonNetwork.buffer();
        buf.writeResourceLocation(player.level().dimension().location());
        buf.writeUUID(player.getUUID());
        buf.writeDouble(player.getX());
        buf.writeDouble(player.getY());
        buf.writeDouble(player.getZ());
        buf.writeInt(color);
        buf.writeBoolean(false);
        AddonNetwork.sendToPlayer(player, SpellChannelManager.SUPPRESSION, buf);
    }

    private static void tick(MinecraftServer server) {
        int now = server.getTickCount();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            refreshForm(player);
            var data = data(player);
            double prior = data.burden;
            int priorRecovery = data.recovery;
            boolean hadCooldowns = !data.cooldowns.isEmpty();
            var active = ACTIVE.get(player.getUUID());
            var iterator = data.cooldowns.entrySet().iterator();
            while (iterator.hasNext()) {
                var entry = iterator.next();
                int started = data.cooldownStarted.getOrDefault(entry.getKey(), Integer.MIN_VALUE);
                if (started == now) continue;
                int ticks = Math.max(0, entry.getValue() - 1);
                if (ticks == 0) iterator.remove();
                else entry.setValue(ticks);
            }
            data.cooldownStarted.keySet().retainAll(data.cooldowns.keySet());
            if (active != null) {
                if (blocked(player) || OcelotSpellLink.rushing(player)
                        || player.getItemInHand(active.hand) != active.wand
                        || !active.record.dimension.equals(player.level().dimension().location().toString())) {
                    cancel(player, "interrupted");
                } else if (now > active.beganTick) {
                    active.age++;
                    advance(player, active);
                }
            } else if (player.isAlive() && !SpellChannelManager.isCasting(player)) {
                if (now > data.lastPaymentTick) {
                    if (data.recovery > 0) data.recovery--;
                    else if (data.burden > 0) {
                        data.burden = Math.max(0, data.burden - NeoRules.current().recoveryRate()
                                * NeoLegacyCasting.recoveryMultiplier(SpellCastManager.getEquippedBook(player)));
                    }
                }
            }
            if (now % 5 == 0) NeoNetwork.hud(player);
            if (prior != data.burden || priorRecovery != data.recovery || hadCooldowns) dirty(player);
        }
        NeoEffects.tick(server);
    }

    public static void register() {
        if (registered) return;
        registered = true;
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) tick(event.getServer());
        });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> {
            if (!(event.getEntity() instanceof ServerPlayer player)) return;
            cancel(player, "disconnected");
            NeoEffects.clearStored(player.getUUID());
            dirty(player);
            NeoNetwork.close(player);
        });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.Clone event) -> {
            if (event.getOriginal() instanceof ServerPlayer old) cancel(old, "death");
            if (event.getEntity() instanceof ServerPlayer player) {
                var data = data(player);
                data.burden = 0;
                data.recovery = 0;
                NeoEffects.clearStored(player.getUUID());
                dirty(player);
            }
        });
        MinecraftForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> {
            var server = event.getServer();
            for (ServerPlayer player : server.getPlayerList().getPlayers()) cancel(player, "server_stopped");
            NeoEffects.clear();
            for (ServerPlayer player : server.getPlayerList().getPlayers()) dirty(player);
            NeoNetwork.clear();
            ACTIVE.clear();
        });
    }
}
