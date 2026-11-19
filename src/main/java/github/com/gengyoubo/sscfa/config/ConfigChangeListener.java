package github.com.gengyoubo.sscfa.config;

/** 配置变更回调；监听者通过 {@link SSCAddonConfig#client()} 自取所需字段。 */
public interface ConfigChangeListener {
    void onConfigChanged();
}
