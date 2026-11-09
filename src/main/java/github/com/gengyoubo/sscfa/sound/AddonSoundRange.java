package github.com.gengyoubo.sscfa.sound;

import github.com.gengyoubo.sscfa.util.FormUtils;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import org.apache.commons.lang3.tuple.Pair;

/** A direct sound entry carries its doubled broadcast radius through the vanilla sound packet. */
public final class AddonSoundRange {
    private static final String PREFIX = "sound_range_x2/";
    private AddonSoundRange() {}

    public static boolean isAddonActionTarget(Object target) {
        if (target instanceof LivingEntity living) {
            var form = FormUtils.getCurrentForm(living);
            return form != null && form.id() != null && form.id().getNamespace().equals("my_addon");
        }
        return target instanceof Pair<?, ?> pair
                && (isAddonActionTarget(pair.getLeft()) || isAddonActionTarget(pair.getRight()));
    }

    public static boolean isExtended(SoundEvent event, float callVolume) {
        return !originalId(event.getLocation()).equals(event.getLocation());
    }

    /** Explicit packet marker avoids mistaking another mod's fixed-range sound for an SSCA sound. */
    public static ResourceLocation originalId(ResourceLocation id) {
        if (!id.getNamespace().equals("ssc_addon") || !id.getPath().startsWith(PREFIX)) return id;
        String encoded = id.getPath().substring(PREFIX.length());
        int slash = encoded.indexOf('/');
        if (slash <= 0 || slash == encoded.length() - 1) return id;
        ResourceLocation decoded = ResourceLocation.tryParse(encoded.substring(0, slash) + ":" + encoded.substring(slash + 1));
        return decoded != null ? decoded : id;
    }

    public static SoundEvent extend(SoundEvent event, float callVolume) {
        if (isExtended(event, callVolume)) return event;
        ResourceLocation id = event.getLocation();
        ResourceLocation marker = new ResourceLocation("ssc_addon", PREFIX + id.getNamespace() + "/" + id.getPath());
        return SoundEvent.createFixedRangeEvent(marker, (float) SoundRangeRules.distance(event.getRange(callVolume)));
    }

    public static Holder<SoundEvent> extendPlayback(Holder<SoundEvent> event, float callVolume) {
        if (!SoundRangeRules.isAddonPlayback() || isExtended(event.value(), callVolume)) return event;
        return Holder.direct(extend(event.value(), callVolume));
    }
}
