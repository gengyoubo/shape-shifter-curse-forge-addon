package github.com.gengyoubo.sscfa.anomaly;

import java.util.Objects;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

/** The accepted spell identity and anomaly amount travel with its delayed effects. */
public record SpellAnomalyContext(ResourceLocation spellId, String element, int level, double amount,
                                  UUID casterUUID, UUID castUUID) {
    public SpellAnomalyContext {
        Objects.requireNonNull(spellId);
        Objects.requireNonNull(element);
        Objects.requireNonNull(casterUUID);
        Objects.requireNonNull(castUUID);
        if (element.isBlank() || level < 1 || !Double.isFinite(amount) || amount < 0)
            throw new IllegalArgumentException("Invalid spell anomaly snapshot");
    }

    public CompoundTag write() {
        var nbt = new CompoundTag();
        nbt.putString("Spell", spellId.toString());
        nbt.putString("Element", element);
        nbt.putInt("Level", level);
        nbt.putDouble("Amount", amount);
        nbt.putUUID("Caster", casterUUID);
        nbt.putUUID("Cast", castUUID);
        return nbt;
    }

    /** Invalid/absent older snapshots produce no anomaly event. */
    public static SpellAnomalyContext read(CompoundTag nbt) {
        if (!nbt.contains("Spell", Tag.TAG_STRING)
                || !nbt.contains("Element", Tag.TAG_STRING)
                || !nbt.contains("Level", Tag.TAG_ANY_NUMERIC)
                || !nbt.contains("Amount", Tag.TAG_ANY_NUMERIC)
                || !nbt.hasUUID("Caster") || !nbt.hasUUID("Cast")) return null;
        try {
            return new SpellAnomalyContext(new ResourceLocation(nbt.getString("Spell")), nbt.getString("Element"),
                    nbt.getInt("Level"), nbt.getDouble("Amount"), nbt.getUUID("Caster"), nbt.getUUID("Cast"));
        } catch (IllegalArgumentException invalid) {
            return null;
        }
    }
}
