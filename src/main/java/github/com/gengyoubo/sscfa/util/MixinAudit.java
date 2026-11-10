package github.com.gengyoubo.sscfa.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 开发用自检：强制加载本模组每个 mixin 的目标类，让注入现在就执行，而不是等玩家第一次用到那个类时才崩。
 *
 * <p>mixin 写错（方法名、描述符、static 不匹配等）编译期查不出来，只有目标类加载时才报错；很多目标类
 * （界面、渲染器、少见实体）在启动阶段根本不会加载。服务端由 game test 调用，客户端用
 * {@code -Dssca.mixinAudit=true} 启动后在进入标题界面前执行。</p>
 */
public final class MixinAudit {
    private static final String CONFIG = "ssc_addon.mixins.json";
    private static final String MIXIN_ANNOTATION = "Lorg/spongepowered/asm/mixin/Mixin;";

    private MixinAudit() {}

    /**
     * 加载 mixin 配置里指定分组的全部目标类。
     *
     * @param sections 配置里的分组名：{@code "mixins"}（双端）、{@code "client"}、{@code "server"}
     * @return 每个失败项一行说明；全部正常时为空
     */
    public static List<String> run(String... sections) {
        List<String> failures = new ArrayList<>();
        ClassLoader loader = MixinAudit.class.getClassLoader();
        JsonObject config;
        try (InputStream in = loader.getResourceAsStream(CONFIG)) {
            if (in == null) return List.of("找不到 " + CONFIG);
            config = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception e) {
            return List.of("读取 " + CONFIG + " 失败：" + e);
        }
        String base = config.get("package").getAsString();
        for (String section : sections) {
            JsonArray names = config.getAsJsonArray(section);
            if (names == null) continue;
            for (JsonElement name : names) {
                String mixin = base + "." + name.getAsString();
                Set<String> targets;
                try {
                    targets = targets(loader, mixin);
                } catch (Exception e) {
                    failures.add(mixin + "：读不到 mixin 类（" + e + "）");
                    continue;
                }
                for (String target : targets) {
                    try {
                        // 不触发静态初始化：只要类被定义，mixin 就已经应用
                        Class.forName(target, false, loader);
                    } catch (ClassNotFoundException e) {
                        // 可选依赖的类不在场：该 mixin 本来就不会生效
                    } catch (Throwable e) {
                        Throwable root = e;
                        while (root.getCause() != null && root.getCause() != root) root = root.getCause();
                        failures.add(mixin + " -> " + target + "：" + root);
                    }
                }
            }
        }
        return failures;
    }

    /** 从 mixin 类的字节码读出 {@code @Mixin} 的目标（不加载 mixin 类本身，它们不允许被直接加载）。 */
    private static Set<String> targets(ClassLoader loader, String mixin) throws Exception {
        Set<String> targets = new LinkedHashSet<>();
        try (InputStream in = loader.getResourceAsStream(mixin.replace('.', '/') + ".class")) {
            if (in == null) throw new IllegalStateException("类文件不存在");
            ClassNode node = new ClassNode();
            new ClassReader(in).accept(node, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG);
            List<AnnotationNode> annotations = new ArrayList<>();
            if (node.invisibleAnnotations != null) annotations.addAll(node.invisibleAnnotations);
            if (node.visibleAnnotations != null) annotations.addAll(node.visibleAnnotations);
            for (AnnotationNode annotation : annotations) {
                if (!MIXIN_ANNOTATION.equals(annotation.desc) || annotation.values == null) continue;
                for (int i = 0; i + 1 < annotation.values.size(); i += 2) {
                    String key = (String) annotation.values.get(i);
                    if (!(annotation.values.get(i + 1) instanceof List<?> values)) continue;
                    for (Object value : values) {
                        if ("value".equals(key) && value instanceof Type type) targets.add(type.getClassName());
                        else if ("targets".equals(key) && value instanceof String text) targets.add(text.replace('/', '.'));
                    }
                }
            }
        }
        return targets;
    }
}
