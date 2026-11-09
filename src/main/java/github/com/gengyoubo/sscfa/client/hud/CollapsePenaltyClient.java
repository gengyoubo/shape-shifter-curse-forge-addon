package github.com.gengyoubo.sscfa.client.hud;

import java.util.Map;
import github.com.gengyoubo.sscfa.mixin.client.LivingEntityStatusEffectsAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import github.com.gengyoubo.sscfa.neomagic.client.NeoClient;

/** Visual carriers only: health loss is exclusively authoritative on the server. */
public final class CollapsePenaltyClient {
    private static Map<MobEffect, MobEffectInstance> effects;
    private static final MobEffect[] TYPES = {MobEffects.CONFUSION, MobEffects.BLINDNESS, MobEffects.WITHER};
    private static final MobEffectInstance[] OWNED = new MobEffectInstance[3];
    private CollapsePenaltyClient() {}

    public static void tick(Minecraft client) {
        if (client.level == null || client.player == null || !client.player.isAlive() || client.player.isSpectator()) {
            clear(); return;
        }
        var current = ((LivingEntityStatusEffectsAccessor) client.player).sscAddon$getActiveStatusEffects();
        if (effects != current) { clear(); effects = current; }
        var hud = NeoClient.hud;
        int stage = hud.getInt("PlayerId") == client.player.getId()
                && hud.getString("Dimension").equals(client.level.dimension().location().toString())
                ? hud.getInt("CollapseStage") : 0;
        for (int i = 0; i < TYPES.length; i++) {
            boolean active = stage >= (i == 0 ? 4 : 5);
            MobEffectInstance effect = effects.get(TYPES[i]);
            if (!active) {
                if (effect != null && effect == OWNED[i]) effects.remove(TYPES[i]);
                OWNED[i] = null;
            } else if (effect == null || effect == OWNED[i] && effect.getDuration() < 40) {
                OWNED[i] = new MobEffectInstance(TYPES[i], 200, i == 2 ? 2 : 0, false, false, false);
                effects.put(TYPES[i], OWNED[i]);
            } else if (effect != OWNED[i]) {
                OWNED[i] = null; // Real server potions and nightmare effects retain their ownership.
            }
        }
    }

    public static void clear() {
        for (int i = 0; i < TYPES.length; i++) {
            if (effects != null && OWNED[i] != null && effects.get(TYPES[i]) == OWNED[i]) effects.remove(TYPES[i]);
            OWNED[i] = null;
        }
        effects = null;
    }
}
