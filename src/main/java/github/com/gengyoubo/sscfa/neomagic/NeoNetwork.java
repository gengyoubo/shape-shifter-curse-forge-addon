package github.com.gengyoubo.sscfa.neomagic;

import github.com.gengyoubo.sscfa.init.AddonItems;
import github.com.gengyoubo.sscfa.item.MagicScrollItem;
import github.com.gengyoubo.sscfa.network.AddonNetwork;
import github.com.gengyoubo.sscfa.spell.FrostSpellLink;
import github.com.gengyoubo.sscfa.spell.OcelotSpellLink;
import github.com.gengyoubo.sscfa.spell.PlayerSpellMastery;
import github.com.gengyoubo.sscfa.spell.ScrollData;
import github.com.gengyoubo.sscfa.spell.SharedSpellCooldowns;
import github.com.gengyoubo.sscfa.spell.Spell;
import github.com.gengyoubo.sscfa.spell.SpellCastManager;
import github.com.gengyoubo.sscfa.spell.SpellNumbers;
import github.com.gengyoubo.sscfa.spell.SpellRegistry;
import github.com.gengyoubo.sscfa.spell.SpellbookData;
import github.com.gengyoubo.sscfa.spell.CollapsePenalties;
import github.com.gengyoubo.sscfa.spell.research.RuneScheme;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.api.distmarker.Dist;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static github.com.gengyoubo.sscfa.neomagic.INeoModel.Blueprint;
import static github.com.gengyoubo.sscfa.neomagic.INeoModel.Profile;
import static github.com.gengyoubo.sscfa.neomagic.INeoModel.Quote;
import static github.com.gengyoubo.sscfa.neomagic.INeoModel.Risk;

/** 新魔法 HUD 同步与崩解度调试指令；不接收旧实验研究协议。 */
public final class NeoNetwork {
    public static final ResourceLocation HUD = new ResourceLocation("ssc_addon", "neomagic_hud");
    public static final ResourceLocation HUD_V2 = new ResourceLocation("ssc_addon", "neomagic_hud_v2");
    public static final ResourceLocation HUD_RESYNC_V2 = new ResourceLocation("ssc_addon", "neomagic_hud_resync_v2");

    private record HudSnapshot(ServerPlayer player, ServerLevel world, Object connection,
                               CompoundTag state, int sentTick, int fullTick) {}

    private static final Map<UUID, HudSnapshot> HUD_SENT = new HashMap<>();
    private static final Map<UUID, Integer> HUD_RESYNC_REQUESTED = new HashMap<>();
    private static final Map<Profile, String> PROFILE_JSON = new LinkedHashMap<>(32, .75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Profile, String> entry) { return size() > 128; }
    };
    private static boolean registered;

    private NeoNetwork() {}

    public static void close(ServerPlayer player) {
        HUD_SENT.remove(player.getUUID());
        HUD_RESYNC_REQUESTED.remove(player.getUUID());
    }

    public static void clear() {
        HUD_SENT.clear();
        HUD_RESYNC_REQUESTED.clear();
        PROFILE_JSON.clear();
    }

    public static void hud(ServerPlayer player) {
        boolean compact = AddonNetwork.canSend(player, HUD_V2);
        if (!compact && !AddonNetwork.canSend(player, HUD)) return;
        CompoundTag state = hudState(player);
        int now = player.getServer().getTickCount();
        HudSnapshot previous = HUD_SENT.get(player.getUUID());
        boolean sameSession = previous != null && previous.player == player
                && previous.world == player.serverLevel() && previous.connection == player.connection;
        boolean full = !sameSession || now - previous.fullTick < 0 || now - previous.fullTick >= 100;
        if (!full && previous.state.equals(state)) return;
        var buffer = AddonNetwork.buffer();
        // 两种通道都使用完整 NBT；旧客户端与新客户端共用同一套编解码。
        buffer.writeNbt(state);
        AddonNetwork.sendToPlayer(player, compact ? HUD_V2 : HUD, buffer);
        HUD_SENT.put(player.getUUID(),
                new HudSnapshot(player, player.serverLevel(), player.connection, state, now, full ? now : previous.fullTick));
    }

    public static CompoundTag hudState(ServerPlayer player) {
        var data = NeoRuntime.data(player);
        var active = NeoRuntime.ACTIVE.get(player.getUUID());
        var legacy = NeoLegacyCasting.active(player);
        ItemStack book = SpellCastManager.getEquippedBook(player);
        boolean holding = book != null
                || player.getMainHandItem().is(AddonItems.MOON_DUST_SPELLBOOK.get())
                || player.getOffhandItem().is(AddonItems.MOON_DUST_SPELLBOOK.get())
                || player.getMainHandItem().getItem() instanceof MagicScrollItem
                || player.getOffhandItem().getItem() instanceof MagicScrollItem;

        CompoundTag n = new CompoundTag();
        n.putBoolean("Visible", holding || data.burden > 0 || active != null || NeoEffects.hasStored(player.getUUID()));
        n.putInt("PlayerId", player.getId());
        n.putString("Dimension", player.level().dimension().location().toString());

        Profile profile = NeoLegacyCasting.profile(player, book);
        n.putInt("MasteryExpTen", data.spellbookMasteryExpTen);
        n.putBoolean("BookEquipped", book != null && !book.isEmpty());
        n.putInt("CastingLevel", PlayerSpellMastery.tier(data.spellbookMasteryExpTen));
        n.putDouble("DangerMax", profile.safety() * 2);
        n.putDouble("C", data.burden);
        n.putDouble("S", profile.safety());
        n.putDouble("BodyS", NeoRuntime.bodyProfile(player).safety());
        n.putInt("Recovery", data.recovery);
        n.putInt("RecoveryDelay", NeoRules.current().recoveryDelay());
        n.putDouble("FrostSpellMultiplier", FrostSpellLink.multiplier(player, true));
        n.putLong("FrostReadyUntil", FrostSpellLink.readyUntil(player));
        n.putLong("NovaQuickCastUntil", OcelotSpellLink.quickCast(player) ? player.level().getGameTime() + 20 : 0L);

        Blueprint blueprint;
        Quote quote;
        int prep;
        String legacyName = "";
        boolean hasSpell = active != null || legacy != null;
        // 每次只计算最终采用的预览；活动施法直接复用起手快照。
        if (legacy != null) {
            blueprint = legacy.session.blueprint;
            quote = legacy.session.quote;
            prep = legacy.duration;
            legacyName = legacy.record.spell;
        } else if (active != null) {
            blueprint = active.session.blueprint;
            quote = active.session.quote;
            prep = active.record.preparation;
        } else {
            ItemStack scroll = player.getMainHandItem().getItem() instanceof MagicScrollItem ? player.getMainHandItem()
                    : player.getOffhandItem().getItem() instanceof MagicScrollItem ? player.getOffhandItem()
                    : ItemStack.EMPTY;
            boolean solo = !scroll.isEmpty();
            if (!solo && book != null) {
                scroll = SpellbookData.getScroll(book, SpellbookData.getSelectedSlot(book));
            }
            Spell spell = ScrollData.getSpell(scroll);
            if (spell != null) {
                hasSpell = true;
                int level = solo ? ScrollData.getLevel(scroll) : ScrollData.getCastLevel(scroll);
                var modifiers = RuneScheme.modifiers(player, scroll, level);
                int original = spell.getCastingProfile(player, level, solo).ticks();
                prep = FormSpellStyleTiming(original, modifiers.time(original), spell.getLockInTick());
                blueprint = NeoLegacyCasting.descriptor(spell, level, prep);
                int normal = solo
                        ? modifiers.mana(Math.round(spell.getManaCost() * spell.getConfig().manaCostMultiplier(level)))
                        : SpellNumbers.finalManaCost(spell, book, player, level, scroll);
                quote = NeoLegacyCasting.quote(player, spell, level, prep, normal);
                legacyName = spell.getId().toString();
            } else {
                blueprint = data.draft;
                quote = NeoLegacyCasting.blueprintQuote(player, blueprint, book);
                prep = FormSpellStyleTiming(blueprint.prep(), blueprint.prep(), -1);
            }
        }

        Risk risk = NeoRules.current().offsetRisk(blueprint, data.burden, quote.actual(), profile);
        NeoSession session = legacy != null ? legacy.session : active == null ? null : active.session;
        double forecast = session == null
                ? NeoRules.current().failureChance(blueprint, data.burden + quote.actual(), quote.actual(), profile)
                : session.failureProbability;
        int age = legacy != null ? legacy.age : active == null ? -1 : active.age;
        boolean protectedByLock = session != null && prep > 0
                && session.failureBoundary < prep && age >= session.failureBoundary;
        double failure = data.burden <= profile.safety() || protectedByLock ? 0 : forecast;

        n.putDouble("Q", quote.actual());
        n.putDouble("Failure", failure);
        n.putDouble("ForecastFailure", forecast);
        n.putDouble("Offset", risk.offset());
        n.putBoolean("HasSpell", hasSpell);
        n.putInt("LockBoundary", session == null ? prep : session.failureBoundary);
        if (!data.records.isEmpty()) {
            var last = data.records.get(0);
            n.putString("ResultId", last.id);
            n.putString("Result", last.result);
        }
        n.putDouble("FailureBasis", session == null ? data.burden + quote.actual() : session.failureBasis);
        n.putInt("CD", data.cooldowns.getOrDefault(blueprint.identity(), 0));
        n.putBoolean("Stored", NeoEffects.hasStored(player.getUUID()));
        if (!legacyName.isEmpty()) {
            Spell spell = SpellRegistry.get(new ResourceLocation(legacyName));
            if (spell != null) {
                n.putInt("CD", (int) Math.max(0,
                        SharedSpellCooldowns.getCooldownEndOf(player, spell) - player.level().getGameTime()));
            }
        }
        n.putInt("Age", age);
        n.putInt("Prep", prep);
        n.putDouble("Paid", session == null ? 0 : session.paid);
        n.putDouble("Yaw", session == null ? 0 : session.yaw);
        n.putDouble("Pitch", session == null ? 0 : session.pitch);
        n.putDouble("Cumulative", session == null ? 0 : 1 - session.survival);
        n.putString("Spell", legacyName);
        n.putInt("CollapseStage", player.isAlive() && !player.isSpectator()
                ? CollapsePenalties.stage(data.burden, profile.safety()) : 0);
        n.putInt("Blocked", 0);
        n.putString("Form", data.form);
        n.putBoolean("Reference", NeoRuntime.bodyProfile(player).reference());
        n.putString("Profile", PROFILE_JSON.computeIfAbsent(profile, NeoState.JSON::toJson));
        return n;
    }

    private static int FormSpellStyleTiming(int original, int adjusted, int lock) {
        return github.com.gengyoubo.sscfa.spell.FormSpellStyle.Timing.of(original, adjusted, lock, 1.0).duration();
    }

    public static void register() {
        if (registered) return;
        registered = true;
        AddonNetwork.clientbound(HUD);
        AddonNetwork.clientbound(HUD_V2);
        AddonNetwork.registerServerReceiver(HUD_RESYNC_V2, (player, buffer) -> {
            int now = player.getServer().getTickCount();
            Integer last = HUD_RESYNC_REQUESTED.get(player.getUUID());
            if (last != null && now - last >= 0 && now - last < 20) return;
            HUD_RESYNC_REQUESTED.put(player.getUUID(), now);
            HudSnapshot old = HUD_SENT.get(player.getUUID());
            if (old != null) {
                HUD_SENT.put(player.getUUID(), new HudSnapshot(old.player, old.world, old.connection,
                        old.state, old.sentTick, now - 100));
            }
            hud(player);
        });
        MinecraftForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> event.getDispatcher()
                .register(Commands.literal("neomagic")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("reset_burden").executes(context -> {
                            NeoRuntime.relieve(context.getSource().getPlayerOrException(), Double.MAX_VALUE);
                            return 1;
                        }))));
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> github.com.gengyoubo.sscfa.neomagic.client.NeoClient::register);
    }
}
