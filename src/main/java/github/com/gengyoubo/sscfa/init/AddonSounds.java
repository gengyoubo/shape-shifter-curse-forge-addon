package github.com.gengyoubo.sscfa.init;

import github.com.gengyoubo.sscfa.SscAddonForge;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** 对应 assets/ssc_addon/sounds.json 的音效事件。 */
public final class AddonSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, SscAddonForge.MOD_ID);

    public static final RegistryObject<SoundEvent> SHAPE_SHIFTERS_DREAM = sound("shape_shifters_dream");
    public static final RegistryObject<SoundEvent> ALLAY_HEAL_MUSIC = sound("allay_heal_music");
    public static final RegistryObject<SoundEvent> ALLAY_SPEED_MUSIC = sound("allay_speed_music");
    public static final RegistryObject<SoundEvent> EXPLOSION_THEME = sound("explosion_theme");

    private AddonSounds() {}

    private static RegistryObject<SoundEvent> sound(String id) {
        return SOUNDS.register(id, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(SscAddonForge.MOD_ID, id)));
    }
}
