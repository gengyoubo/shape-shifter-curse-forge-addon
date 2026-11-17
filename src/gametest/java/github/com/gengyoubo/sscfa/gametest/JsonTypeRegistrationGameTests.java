package github.com.gengyoubo.sscfa.gametest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import github.com.gengyoubo.sscfa.SscAddonForge;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.onixary.shapeShifterCurseForge.api.registry.SscJavaRegistries;
import net.onixary.shapeShifterCurseForge.power.FormPowerDefinition;
import net.onixary.shapeShifterCurseForge.power.FormPowerRegistry;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Every addon-namespaced {@code "type"} in the loaded power JSON must resolve to a registered SSC
 * Forge power type, action or condition. SSC Forge silently treats an unknown condition as false
 * and an unknown action as a no-op, so a missing registration disables the power without an error.
 */
@GameTestHolder(SscAddonForge.MOD_ID)
@PrefixGameTestTemplate(false)
public final class JsonTypeRegistrationGameTests {
    private static final Set<String> NAMESPACES = Set.of("my_addon", "ssc_addon");
    /**
     * Known gaps, each with its reason. Remove an entry when its type is ported.
     * conditional_mana_attribute: SSC Forge has no max-mana / regeneration modifier API.
     */
    private static final Set<String> PENDING = Set.of("ssc_addon:conditional_mana_attribute");

    private JsonTypeRegistrationGameTests() {}

    @GameTest(template = "gametest_floor")
    public static void addonJsonTypesAreRegistered(GameTestHelper helper) {
        Map<String, Set<ResourceLocation>> missing = new TreeMap<>();
        int scanned = 0;
        for (FormPowerDefinition definition : FormPowerRegistry.all().values()) {
            if (definition.data() == null || !NAMESPACES.contains(definition.id().getNamespace())) continue;
            walk(definition.data(), true, definition.id(), missing);
            scanned++;
        }
        // The addon ships ~335 power files; a near-empty scan means the registry was not loaded.
        helper.assertTrue(scanned > 300, "expected the addon's power JSON to be loaded, scanned " + scanned);
        PENDING.forEach(missing::remove);
        helper.assertTrue(missing.isEmpty(), "unregistered addon JSON types: " + missing);
        helper.succeed();
    }

    private static void walk(JsonElement element, boolean topLevel, ResourceLocation power,
                             Map<String, Set<ResourceLocation>> missing) {
        if (element instanceof JsonArray array) {
            for (JsonElement child : array) walk(child, false, power, missing);
            return;
        }
        if (!(element instanceof JsonObject object)) return;
        JsonElement t = object.get("type");
        if (t != null && t.isJsonPrimitive()) {
            ResourceLocation type = ResourceLocation.tryParse(t.getAsString());
            if (type != null && NAMESPACES.contains(type.getNamespace()) && !resolves(type, topLevel)) {
                missing.computeIfAbsent(type.toString(), k -> new TreeSet<>()).add(power);
            }
        }
        for (var entry : object.entrySet()) walk(entry.getValue(), false, power, missing);
    }

    /** A power file's own type is a power type; nested types are actions or conditions. */
    private static boolean resolves(ResourceLocation type, boolean topLevel) {
        if (SscJavaRegistries.powerType(type).isPresent() || SscJavaRegistries.power(type).isPresent()) return true;
        return !topLevel && (SscJavaRegistries.action(type).isPresent() || SscJavaRegistries.condition(type).isPresent());
    }
}
