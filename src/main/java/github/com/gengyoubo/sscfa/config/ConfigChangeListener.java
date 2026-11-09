package github.com.gengyoubo.sscfa.config;

/**
 * 配置变更回调。监听者通过 {@link SSCAddonConfig#client()} / {@link SSCAddonConfig#server()} 自取所需。
 */
public interface ConfigChangeListener {
	void onConfigChanged();
}
