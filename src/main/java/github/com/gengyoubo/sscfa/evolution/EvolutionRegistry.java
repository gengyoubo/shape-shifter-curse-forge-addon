package github.com.gengyoubo.sscfa.evolution;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** SSCA 进化路线数据驱动加载器（Forge 版 datapack reload listener）。 */
public final class EvolutionRegistry extends SimplePreparableReloadListener<Void> {
    public static final EvolutionRegistry INSTANCE = new EvolutionRegistry();
    private static final ResourceLocation ID = new ResourceLocation("ssc_addon", "ssca_evolution_routes");
    private static final String DIR = "ssca_evolution/routes";

    private record Snapshot(Map<String, EvolutionRoute> routes, Map<String, String> rawJson) {}
    private volatile Snapshot snapshot = new Snapshot(Map.of(), Map.of());

    private EvolutionRegistry() {}

    @Override
    protected Void prepare(ResourceManager manager, ProfilerFiller profiler) {
        return null;
    }

    @Override
    protected void apply(Void unused, ResourceManager manager, ProfilerFiller profiler) {
        onResourceManagerReload(manager);
    }

    public void onResourceManagerReload(ResourceManager manager) {
        Map<String, EvolutionRoute> loaded = new LinkedHashMap<>();
        Map<String, String> loadedRaw = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, Resource> entry :
                manager.listResources(DIR, path -> path.getPath().endsWith(".json")).entrySet()) {
            ResourceLocation fileId = entry.getKey();
            String path = fileId.getPath();
            String fileName = path.substring(path.lastIndexOf('/') + 1, path.length() - ".json".length());
            if (fileName.startsWith("_")) continue;
            try (InputStream is = entry.getValue().open()) {
                String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                JsonObject o = GsonHelper.parse(content);
                if (loaded.putIfAbsent(fileName, EvolutionRoute.fromJson(fileName, o)) != null) {
                    throw new IllegalArgumentException("Duplicate evolution route id: " + fileName);
                }
                loadedRaw.put(fileName, content);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to load evolution route: " + fileId
                        + "; retaining the previous registry", e);
            }
        }
        snapshot = new Snapshot(Collections.unmodifiableMap(loaded), Collections.unmodifiableMap(loadedRaw));
    }

    public void applyClientSync(Map<String, String> raw) {
        Map<String, EvolutionRoute> rebuilt = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : raw.entrySet()) {
            try {
                JsonObject o = GsonHelper.parse(e.getValue());
                rebuilt.put(e.getKey(), EvolutionRoute.fromJson(e.getKey(), o));
            } catch (Exception ex) {
                System.err.println("[ssc_addon] Failed to apply synced evolution route: " + e.getKey() + " - " + ex);
                return;
            }
        }
        snapshot = new Snapshot(Collections.unmodifiableMap(rebuilt), Collections.unmodifiableMap(new LinkedHashMap<>(raw)));
    }

    public EvolutionRoute getRoute(String routeId) {
        return routeId == null ? null : snapshot.routes.get(routeId);
    }
    public Map<String, EvolutionRoute> all() { return snapshot.routes; }
    public Map<String, String> getRawJson() { return snapshot.rawJson; }

    public EvolutionRoute getRouteByStartForm(ResourceLocation formId) {
        if (formId == null) return null;
        formId = github.com.gengyoubo.sscfa.util.FormIdMigration.canonical(formId);
        for (EvolutionRoute r : snapshot.routes.values()) {
            if (formId.equals(r.startForm)) return r;
        }
        return null;
    }

    public EvolutionNode getNode(String routeId, String nodeId) {
        EvolutionRoute r = getRoute(routeId);
        return r == null ? null : r.getNode(nodeId);
    }
}
