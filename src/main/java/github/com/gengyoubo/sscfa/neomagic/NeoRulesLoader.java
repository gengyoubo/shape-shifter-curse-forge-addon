package github.com.gengyoubo.sscfa.neomagic;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.Reader;

/**
 * 服务端数据包重载时读取 {@code ssca_neomagic:rules/defaults.json} 并整体替换 {@link NeoRules#current()}。
 * 文件缺失或任一字段非法时重载失败，已生效的规则保持不变。
 */
public final class NeoRulesLoader extends SimplePreparableReloadListener<NeoRules> {
    public static final ResourceLocation FILE = NeoMagic.id("rules/defaults.json");

    @Override
    protected NeoRules prepare(ResourceManager resources, ProfilerFiller profiler) {
        try (Reader reader = resources.getResource(FILE).orElseThrow(() -> new IllegalStateException("Missing " + FILE)).openAsReader()) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            return NeoRules.parse(root);
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot load neomagic rules; retaining previous rules", ex);
        }
    }

    @Override
    protected void apply(NeoRules rules, ResourceManager resources, ProfilerFiller profiler) {
        NeoRules.install(rules);
        NeoMagic.LOG.info("Neomagic loaded {} independent form profiles, rules {}", rules.profiles().size(), rules.rulesVersion());
    }
}
