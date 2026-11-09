package github.com.gengyoubo.sscfa.config;

/** 配置访问统一入口（Forge 简化版）。 */
public final class SSCAddonConfig {
    private static final SSCAddonClientConfig CLIENT = new SSCAddonClientConfig();
    private SSCAddonConfig() {}
    public static SSCAddonClientConfig client() { return CLIENT; }
}
