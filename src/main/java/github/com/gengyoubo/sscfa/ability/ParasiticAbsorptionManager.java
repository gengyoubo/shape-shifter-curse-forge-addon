/*
 * Copyright (c) 2026 MangZai-120
 * This file is part of the "shape shifter curse addon" project.
 * Licensed under the GNU Affero General Public License v3.0 (AGPL-3.0).
 */
package github.com.gengyoubo.sscfa.ability;

import github.com.gengyoubo.sscfa.balance.BalanceReader;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/** Parasitic absorption is owned and persisted by its target, including non-player entities. */
public final class ParasiticAbsorptionManager {
    public static final int MAX_DURATION = 300;
    private static final BalanceReader BAL = new BalanceReader("abilities.parasitic_seed_system");
    private ParasiticAbsorptionManager() { }

    public static void init() {
        github.com.gengyoubo.sscfa.event.PlayerSessionCleanup.register((handler, server) -> {
            if (handler.player instanceof ParasiticAbsorptionAccess access) {
                access.ssca$parasiticAbsorption().remove(handler.player);
                handler.player.removeEffect(github.com.gengyoubo.sscfa.init.AddonEffects.BAT_ABSORPTION.get());
            }
        });
    }

    public static void addAbsorption(LivingEntity entity, int amplifier) {
        addAbsorption(entity, amplifier, 1.0f);
    }

    public static void addAbsorption(LivingEntity entity, int amplifier, float durationFactor) {
        if (!(entity.level() instanceof ServerLevel) || !(entity instanceof ParasiticAbsorptionAccess access)) return;
        int duration = Math.max(20, Math.round(BAL.i("absorption_max_duration", MAX_DURATION) * durationFactor));
        entity.addEffect(new MobEffectInstance(github.com.gengyoubo.sscfa.init.AddonEffects.BAT_ABSORPTION.get(), duration, amplifier, false, true, true));
        access.ssca$parasiticAbsorption().grant(entity, (amplifier + 1) * 2.0f);
        access.ssca$parasiticAbsorption().refresh(entity.level().getGameTime() + duration);
    }
}
