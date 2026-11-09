package github.com.gengyoubo.sscfa.util;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

/** Form names may change; the existing Origins, powers and their saved state must not. */
public final class FormIdMigration {
    private static final Map<ResourceLocation, ResourceLocation> ALIASES;
    private static final Map<ResourceLocation, ResourceLocation> LEGACY;
    private static final Map<String, String> STRINGS;
    private static final Map<String, String> PATHS;
    private static final String[] COMPONENT_FIELDS = {
            "nowFormID", "currentForm", "fallbackFormID", "BeforeCursedMoonAppliedForm",
            "AfterCursedMoonAppliedForm", "transformTargetForm", "customPotionFormID"
    };
    private static final String[] COMPONENT_LISTS = {"formHistory", "free_perk_forms"};

    static {
        Map<ResourceLocation, ResourceLocation> aliases = new LinkedHashMap<>();
        alias(aliases, "familiar_fox_sp", "familiar_fox_spirit_realm_lord");
        alias(aliases, "snow_fox_sp", "snow_fox_frost_spirit");
        alias(aliases, "allay_sp", "allay_crystal_sound");
        alias(aliases, "axolotl_sp", "axolotl_abyssal_overlord");
        alias(aliases, "wild_cat_sp", "wild_cat_moonlight_phantom");
        alias(aliases, "fallen_allay_sp", "allay_spirit_of_the_fallen");
        alias(aliases, "anubis_wolf_sp", "anubis_wolf_netherjudge");
        alias(aliases, "golden_sandstorm_sp", "anubis_wolf_golden_sandstorm");
        alias(aliases, "upgrade_familiar_fox", "familiar_fox_upgrade");
        alias(aliases, "upgrade_axolotl", "axolotl_upgrade");
        ALIASES = Map.copyOf(aliases);
        Map<ResourceLocation, ResourceLocation> legacy = new LinkedHashMap<>();
        Map<String, String> strings = new LinkedHashMap<>();
        Map<String, String> paths = new LinkedHashMap<>();
        aliases.forEach((oldId, newId) -> {
            legacy.put(newId, oldId);
            strings.put(oldId.toString(), newId.toString());
            paths.put(oldId.getPath(), newId.getPath());
        });
        LEGACY = Map.copyOf(legacy);
        STRINGS = Map.copyOf(strings);
        PATHS = Map.copyOf(paths);
    }

    private FormIdMigration() { }

    private static void alias(Map<ResourceLocation, ResourceLocation> aliases, String oldPath, String newPath) {
        aliases.put(new ResourceLocation("my_addon", oldPath), new ResourceLocation("my_addon", newPath));
    }

    public static Map<ResourceLocation, ResourceLocation> aliases() {
        return ALIASES;
    }

    public static ResourceLocation canonical(ResourceLocation id) {
        return id == null ? null : ALIASES.getOrDefault(id, id);
    }

    /** Only complete, exact old IDs are rewritten; unrelated or malformed strings pass through. */
    public static String canonical(String id) {
        return id == null ? null : STRINGS.getOrDefault(id, id);
    }

    /** For SSCA's own path-keyed keybind/color settings, never for other namespaces. */
    public static String canonicalPath(String path) {
        return path == null ? null : PATHS.getOrDefault(path, path);
    }

    /** Stable resource identity: existing Origins/powers, models and cooldowns retain their IDs. */
    public static ResourceLocation legacy(ResourceLocation id) {
        return id == null ? null : LEGACY.getOrDefault(id, id);
    }

    /** Copy on change: callers may also retain the original save or packet for another consumer. */
    public static CompoundTag migrateComponentNbt(CompoundTag source) {
        CompoundTag result = source;
        for (String key : COMPONENT_FIELDS) {
            if (!source.contains(key, Tag.TAG_STRING)) continue;
            String oldId = source.getString(key);
            String newId = canonical(oldId);
            if (oldId.equals(newId)) continue;
            if (result == source) result = source.copy();
            result.putString(key, newId);
        }
        for (String key : COMPONENT_LISTS) {
            if (!source.contains(key, Tag.TAG_LIST)) continue;
            ListTag list = source.getList(key, Tag.TAG_STRING);
            ListTag changed = null;
            for (int index = 0; index < list.size(); index++) {
                String oldId = list.getString(index);
                String newId = canonical(oldId);
                if (oldId.equals(newId)) continue;
                if (changed == null) changed = list.copy();
                changed.set(index, StringTag.valueOf(newId));
            }
            if (changed != null) {
                if (result == source) result = source.copy();
                result.put(key, changed);
            }
        }
        return result;
    }
}
