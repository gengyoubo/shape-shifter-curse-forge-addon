package github.com.gengyoubo.sscfa.config;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** 配置变更通知：保存或重载客户端配置后调用 {@link #notifyChange()}。 */
public final class ConfigChangeManager {
    private static final List<ConfigChangeListener> LISTENERS = new CopyOnWriteArrayList<>();

    private ConfigChangeManager() {}

    public static void register(ConfigChangeListener listener) {
        if (listener != null) LISTENERS.add(listener);
    }

    public static void notifyChange() {
        for (ConfigChangeListener listener : LISTENERS) listener.onConfigChanged();
    }
}
