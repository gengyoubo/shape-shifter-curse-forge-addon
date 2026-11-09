package github.com.gengyoubo.sscfa.spell;

import github.com.gengyoubo.sscfa.spell.config.SpellConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 榄旀硶娉ㄥ唽琛紙jackcooper锛夛細id 鈫?{@link Spell}銆侸ava 渚у彧娉ㄥ唽琛屼负绫伙紙鍗曚緥锛夛紝鏁板€肩敱 JSON 鏁版嵁鍖呮敞鍏ャ€? * Forge 鐗堢敤 {@link SimplePreparableReloadListener} 鎺ュ叆鏁版嵁鍖呴噸杞斤紝缃戠粶鍚屾璧?{@link github.com.gengyoubo.sscfa.network.AddonNetwork}銆? */
public final class SpellRegistry extends SimplePreparableReloadListener<Void> {
	private static final Map<ResourceLocation, Spell> SPELLS = new LinkedHashMap<>();
	private volatile Map<String, String> rawJson = Map.of();
    private volatile Map<String, SpellConfig> serverConfigs = Map.of();
    private volatile Map<String, SpellConfig> clientConfigs = Map.of();
    private Map<String, String> pendingRaw;
    private Map<String, SpellConfig> pendingConfigs;

    public SpellConfig configFor(String path, SpellConfig fallback) {
        var configs = github.com.gengyoubo.sscfa.balance.BalanceIntegration.isClientThread()
                ? clientConfigs : serverConfigs;
        return configs.getOrDefault(path, fallback);
    }
    public synchronized void commitPending() {
        if (pendingConfigs == null) return;
        rawJson = pendingRaw;
        serverConfigs = pendingConfigs;
        discardPending();
    }
    public synchronized void discardPending() { pendingConfigs = null; pendingRaw = null; }
    public synchronized void clearServer() { discardPending(); serverConfigs = Map.of(); rawJson = Map.of(); }
    public void clearClient() { clientConfigs = Map.of(); }
    public void sendTo(net.minecraft.server.level.ServerPlayer player) {
        sendTo(player, false);
    }
    public void sendTo(net.minecraft.server.level.ServerPlayer player, boolean force) {
        github.com.gengyoubo.sscfa.network.JsonSnapshotSync.send(player,
                github.com.gengyoubo.sscfa.network.SscAddonNetworking.PACKET_SPELL_CONFIG_SYNC, getRawJson(), force);
    }

	public static final SpellRegistry INSTANCE = new SpellRegistry();
	private static final ResourceLocation LISTENER_ID = new ResourceLocation("ssc_addon", "spell_configs");
	private static final String DIR = "spells";

	private SpellRegistry() {
	}

	public static void register(Spell spell) {
		SPELLS.put(spell.getId(), spell);
	}

	public static Spell get(ResourceLocation id) {
		return id == null ? null : SPELLS.get(id);
	}

	public static Spell get(String path) {
		if (path == null || path.isEmpty()) {
			return null;
		}
		return SPELLS.get(new ResourceLocation("ssc_addon", path));
	}

	public static Collection<Spell> all() {
		return SPELLS.values();
	}

	/** 娉ㄥ唽鍐呯疆榄旀硶琛屼负绫伙紙鏁板€肩瓑寰?JSON 娉ㄥ叆锛夈€傛殏缂撲緷璧栬兘鍔涘眰鐨?3 涓硶鏈€?*/
	public static void init() {
		register(new github.com.gengyoubo.sscfa.spell.spells.FireBoltSpell());
		register(new github.com.gengyoubo.sscfa.spell.spells.FlameNovaSpell());
		register(new github.com.gengyoubo.sscfa.spell.spells.MeteorSpell());
		register(new github.com.gengyoubo.sscfa.spell.spells.FrostSpikeSpell());
		register(new github.com.gengyoubo.sscfa.spell.spells.IceBarrageSpell());
		register(new github.com.gengyoubo.sscfa.spell.spells.FrostNovaSpell());
		register(new github.com.gengyoubo.sscfa.spell.spells.FrostArmorSpell());
		register(new github.com.gengyoubo.sscfa.spell.spells.MoonlightArrowSpell());
		register(new github.com.gengyoubo.sscfa.spell.spells.LunarMendSpell());
		register(new github.com.gengyoubo.sscfa.spell.spells.LunarVeilSpell());
		register(new github.com.gengyoubo.sscfa.spell.spells.LunarPhaseSpell());
		register(new github.com.gengyoubo.sscfa.spell.spells.CurseMarkSpell());
		register(new github.com.gengyoubo.sscfa.spell.spells.DreadWhisperSpell());
		register(new github.com.gengyoubo.sscfa.spell.spells.BeepSheepSpell());
		register(new github.com.gengyoubo.sscfa.spell.spells.VoidDevourSpell());
		register(new github.com.gengyoubo.sscfa.spell.spells.VoidErosionSpell());
		register(new github.com.gengyoubo.sscfa.spell.spells.DeathFinaleSpell());
		register(new github.com.gengyoubo.sscfa.spell.spells.SpaceBlinkSpell());
		register(new github.com.gengyoubo.sscfa.spell.spells.SpaceStrideSpell());
		register(new github.com.gengyoubo.sscfa.spell.spells.DomainSpell());
	}

	@Override
	protected Void prepare(ResourceManager manager, ProfilerFiller profiler) {
		return null;
	}

	@Override
	protected void apply(Void unused, ResourceManager manager, ProfilerFiller profiler) {
		onResourceManagerReload(manager);
	}

    public synchronized void onResourceManagerReload(ResourceManager manager) {
        discardPending();
        Map<String, String> loaded = new LinkedHashMap<>();
        Map<String, SpellConfig> configs = new LinkedHashMap<>();
        for (var entry : manager.listResources(DIR, id -> id.getPath().endsWith(".json")).entrySet()) {
            var id = entry.getKey();
            if (!id.getNamespace().equals("ssc_addon")) continue;
            String path = id.getPath();
            String name = path.substring(path.lastIndexOf('/') + 1, path.length() - 5);
            if (name.startsWith("_") || get(name) == null) continue;
            try (var input = entry.getValue().open()) {
                String json = new String(input.readAllBytes(), StandardCharsets.UTF_8);
                configs.put(name, parseConfig(json, name));
                loaded.put(name, json);
            } catch (Exception invalid) {
                throw new IllegalArgumentException("Invalid spell resource " + id, invalid);
            }
        }
        for (Spell spell : SPELLS.values()) {
            if (!configs.containsKey(spell.getId().getPath()))
                throw new IllegalArgumentException("Missing spell config: " + spell.getId());
        }
        pendingRaw = Map.copyOf(loaded);
        pendingConfigs = Map.copyOf(configs);
    }

    private static SpellConfig parseConfig(String json, String path) {
        SpellConfig config = SpellConfig.fromJson(GsonHelper.parse(json));
        if (!config.manaCostConfigured) throw new IllegalArgumentException("Missing/invalid mana_cost: " + path);
        return config;
    }

    public void applyClientSync(Map<String, String> raw) {
        Map<String, SpellConfig> configs = new LinkedHashMap<>();
        for (var entry : raw.entrySet()) {
            if (get(entry.getKey()) == null) throw new IllegalArgumentException("Unknown server spell " + entry.getKey());
            configs.put(entry.getKey(), parseConfig(entry.getValue(), entry.getKey()));
        }
        for (ResourceLocation id : SPELLS.keySet()) {
            if (!configs.containsKey(id.getPath()))
                throw new IllegalArgumentException("Missing server spell " + id);
        }
        clientConfigs = Map.copyOf(configs);
    }

	public Map<String, String> getRawJson() {
		synchronized (this) {
			return Collections.unmodifiableMap(new LinkedHashMap<>(rawJson));
		}
	}

	public interface SpellConfigInjector {
		void ssc_addon$applyConfig(SpellConfig config);
	}
}
