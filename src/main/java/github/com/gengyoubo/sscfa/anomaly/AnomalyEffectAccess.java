package github.com.gengyoubo.sscfa.anomaly;

import github.com.gengyoubo.sscfa.init.AddonAnomalyEffects;

/** Lazy, entity-local combat cache. Unaffected entities keep a null reference. */
public interface AnomalyEffectAccess {
    AddonAnomalyEffects.Cache ssca$anomalyEffects();
    void ssca$anomalyEffects(AddonAnomalyEffects.Cache cache);
    long ssca$lastAnomalyZoneHitTick();
    void ssca$lastAnomalyZoneHitTick(long tick);
}
