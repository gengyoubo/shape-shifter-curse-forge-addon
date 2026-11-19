package github.com.gengyoubo.sscfa.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 配置访问统一入口。客户端配置只影响本地显示，保存在 {@code config/ssc_addon-client.json}；
 * 首次访问时加载，缺失字段取默认值，文件损坏时备份为 {@code .broken} 后使用默认配置。
 */
public final class SSCAddonConfig {
    private static final Logger LOG = LoggerFactory.getLogger("ssc_addon/config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String CLIENT_FILE = "ssc_addon-client.json";
    private static SSCAddonClientConfig client;

    private SSCAddonConfig() {}

    public static synchronized SSCAddonClientConfig client() {
        if (client == null) client = load();
        return client;
    }

    /**
     * 写回磁盘；失败只记日志，不影响游戏。不触发 {@link ConfigChangeManager}：其监听者（如故事书战利品）
     * 属于服务端配置，客户端显示设置的保存不应让它们重载。
     */
    public static synchronized void saveClient() {
        SSCAddonClientConfig config = client();
        Path path = clientPath();
        try {
            Files.createDirectories(path.getParent());
            Path temp = path.resolveSibling(CLIENT_FILE + ".tmp");
            try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                GSON.toJson(config, writer);
            }
            try {
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicUnsupported) {
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException | RuntimeException failure) {
            LOG.warn("Could not save {}", path, failure);
        }
    }

    /** 从磁盘重新读取（例如手动编辑文件后）。 */
    public static synchronized void reloadClient() {
        client = load();
    }

    private static SSCAddonClientConfig load() {
        Path path = clientPath();
        SSCAddonClientConfig loaded = null;
        boolean exists = Files.isRegularFile(path);
        if (exists) {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                loaded = GSON.fromJson(reader, SSCAddonClientConfig.class);
            } catch (IOException | JsonParseException | IllegalStateException malformed) {
                LOG.warn("Ignoring invalid {}; defaults will be used", path, malformed);
                try {
                    Files.move(path, path.resolveSibling(CLIENT_FILE + ".broken"), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException ignored) {
                    // 备份失败不影响使用默认配置。
                }
                exists = false;
            }
        }
        if (loaded == null) loaded = new SSCAddonClientConfig();
        loaded.sanitize();
        loaded.migrateSkillHudLayout();
        client = loaded;
        if (!exists) saveClient();
        return loaded;
    }

    private static Path clientPath() {
        return FMLPaths.CONFIGDIR.get().resolve(CLIENT_FILE);
    }
}
