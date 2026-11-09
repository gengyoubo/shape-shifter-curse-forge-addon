package github.com.gengyoubo.sscfa.balance;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * 按数据包优先级读取 {@code data/ssc_addon/balance/**.json}。非法文件让准备阶段失败；
 * 合法结果交给 {@link Sink} 暂存，由 {@link BalanceIntegration} 在整次重载成功后才发布。
 */
public final class BalanceLoader implements PreparableReloadListener {
    /** balance 文件所在目录（相对命名空间根目录）。 */
    public static final String DIRECTORY = "balance";

    /** 准备开始与 apply 结果回传。 */
    public interface Sink {
        /** 新一次重载开始准备（工作线程）：丢弃上一次未发布的候选。 */
        void onReloadStarted();

        /** apply 阶段（主线程）：合并层列表（低 → 高优先级）。 */
        void onLoaded(List<BalanceMerge.Layer> layers);
    }

    private final Sink sink;

    public BalanceLoader(Sink sink) {
        this.sink = sink;
    }

    @Override
    public CompletableFuture<Void> reload(PreparationBarrier barrier, ResourceManager manager,
                                          ProfilerFiller prepareProfiler, ProfilerFiller applyProfiler,
                                          Executor prepareExecutor, Executor applyExecutor) {
        // 准备阶段（工作线程）：读文件、解析为层，不碰已生效的状态
        return CompletableFuture.supplyAsync(() -> {
                    sink.onReloadStarted();
                    return readLayers(manager);
                }, prepareExecutor)
                .thenCompose(barrier::wait)
                .thenAcceptAsync(sink::onLoaded, applyExecutor);
    }

    @Override
    public String getName() {
        return "ssc_addon:balance";
    }

    /** 资源路径不含命名空间：data/ssc_addon/balance → balance。 */
    List<BalanceMerge.Layer> readLayers(ResourceManager manager) {
        record Ranked(int rank, String id, BalanceMerge.Layer layer) {}
        Map<String, Integer> packOrder = new HashMap<>();
        manager.listPacks().forEach(pack -> packOrder.put(pack.packId(), packOrder.size()));
        List<Ranked> ranked = new ArrayList<>();
        for (ResourceLocation fileId : manager.listResources(DIRECTORY,
                id -> id.getNamespace().equals("ssc_addon") && id.getPath().endsWith(".json")).keySet()) {
            String relative = fileId.getPath().substring(DIRECTORY.length() + 1);
            for (Resource resource : manager.getResourceStack(fileId)) {
                String source = resource.sourcePackId() + ":" + fileId;
                try (var input = resource.open()) {
                    var tree = BalanceDocuments.normalize(relative, source, new String(input.readAllBytes(), StandardCharsets.UTF_8));
                    ranked.add(new Ranked(packOrder.getOrDefault(resource.sourcePackId(), -1),
                            fileId.toString(), new BalanceMerge.Layer(source, tree)));
                } catch (Exception e) {
                    throw new IllegalArgumentException("Invalid balance resource " + source, e);
                }
            }
        }
        // 数据包优先级高于文件名顺序（包括不同文件名的聚合文件）
        ranked.sort(Comparator.comparingInt(Ranked::rank).thenComparing(Ranked::id));
        List<BalanceMerge.Layer> layers = new ArrayList<>();
        layers.add(new BalanceMerge.Layer("builtin", Map.of("schema_version", 1L)));
        ranked.forEach(value -> layers.add(value.layer()));
        return layers;
    }
}
