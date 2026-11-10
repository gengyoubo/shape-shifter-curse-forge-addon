package github.com.gengyoubo.sscfa.client;

import github.com.gengyoubo.sscfa.mixin.client.LivingEntityStatusEffectsAccessor;
import github.com.gengyoubo.sscfa.network.AddonNetwork;
import github.com.gengyoubo.sscfa.network.SscAddonNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 食梦魔「恐惧」客户端状态：粉雾强度、隐匿/显形窗口。仅恐惧目标本人持有。
 */
public final class NightmareFearClient {
    private static final int FADE_IN_TICKS = 40;

    private static long endWorldTime = -1L;
    private static long startWorldTime = -1L;
    private static final Map<UUID, Long> HIDDEN = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> REVEALED = new ConcurrentHashMap<>();
    private static Map<MobEffect, MobEffectInstance> blindnessEffects;
    private static MobEffectInstance fearBlindness;

    private NightmareFearClient() {
    }

    public static void register() {
        AddonNetwork.registerClientReceiver(SscAddonNetworking.PACKET_FEAR_STATE, buf -> {
            int duration = buf.readVarInt();
            Minecraft client = Minecraft.getInstance();
            if (client.level == null) return;
            long now = client.level.getGameTime();
            if (duration <= 0) {
                endWorldTime = -1L;
                startWorldTime = -1L;
                HIDDEN.clear();
                return;
            }
            startWorldTime = now;
            endWorldTime = now + duration;
        });
        AddonNetwork.registerClientReceiver(SscAddonNetworking.PACKET_FEAR_HIDE, buf -> {
            UUID nightmareUuid = buf.readUUID();
            int hideTicks = buf.readVarInt();
            Minecraft client = Minecraft.getInstance();
            if (client.level == null) return;
            HIDDEN.put(nightmareUuid, client.level.getGameTime() + hideTicks);
        });
        AddonNetwork.registerClientReceiver(SscAddonNetworking.PACKET_FEAR_REVEAL, buf -> {
            UUID nightmareUuid = buf.readUUID();
            int revealTicks = buf.readVarInt();
            Minecraft client = Minecraft.getInstance();
            if (client.level == null) return;
            HIDDEN.remove(nightmareUuid);
            REVEALED.put(nightmareUuid, client.level.getGameTime() + revealTicks);
        });
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) tick(Minecraft.getInstance());
        });
        MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> {
            clearFearBlindness();
            endWorldTime = -1L;
            startWorldTime = -1L;
            HIDDEN.clear();
            REVEALED.clear();
        });
    }

    public static boolean isFeared() {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return false;
        if (endWorldTime < 0L) return false;
        return client.level.getGameTime() < endWorldTime;
    }

    public static float getFogStrength(float tickDelta) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || endWorldTime < 0L) return 0.0F;
        float now = client.level.getGameTime() + tickDelta;
        if (now >= endWorldTime) return 0.0F;
        float fadeIn = (now - startWorldTime) / FADE_IN_TICKS;
        float fadeOut = (endWorldTime - now) / 10.0F;
        float strength = Math.min(fadeIn, 1.0F);
        strength = Math.min(strength, Math.max(0.0F, fadeOut));
        return Math.max(0.0F, Math.min(1.0F, strength));
    }

    public static boolean isHidden(UUID nightmareUuid) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return false;
        long now = client.level.getGameTime();
        Long revealed = REVEALED.get(nightmareUuid);
        if (revealed != null && revealed > now) return false;
        Long until = HIDDEN.get(nightmareUuid);
        return until != null && until > now;
    }

    public static void tick(Minecraft client) {
        if (client.level == null) {
            clearFearBlindness();
            return;
        }
        long now = client.level.getGameTime();
        if (!HIDDEN.isEmpty()) HIDDEN.values().removeIf(until -> until <= now);
        if (!REVEALED.isEmpty()) REVEALED.values().removeIf(until -> until <= now);
        boolean feared = isFeared();
        if (client.player != null) {
            Map<MobEffect, MobEffectInstance> effects =
                    ((LivingEntityStatusEffectsAccessor) client.player).sscAddon$getActiveStatusEffects();
            updateBlindness(effects, feared);
        } else {
            clearFearBlindness();
        }
    }

    private static void updateBlindness(Map<MobEffect, MobEffectInstance> effects, boolean feared) {
        if (effects != blindnessEffects) {
            clearFearBlindness();
            blindnessEffects = effects;
        }
        if (!feared) {
            clearFearBlindness();
            return;
        }
        MobEffectInstance current = effects.get(MobEffects.BLINDNESS);
        if (current == null || (current == fearBlindness && current.getDuration() < 30)) {
            fearBlindness = new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, false, false, false);
            effects.put(MobEffects.BLINDNESS, fearBlindness);
        } else if (current != fearBlindness) {
            fearBlindness = null;
        }
    }

    private static void clearFearBlindness() {
        if (blindnessEffects != null && fearBlindness != null
                && blindnessEffects.get(MobEffects.BLINDNESS) == fearBlindness) {
            blindnessEffects.remove(MobEffects.BLINDNESS);
        }
        fearBlindness = null;
        blindnessEffects = null;
    }
}
