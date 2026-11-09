package github.com.gengyoubo.sscfa.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

/**
 * 服务端配置（{@code serverconfig/ssc_addon-server.toml}，随存档保存）：玩法平衡与权威判定相关，
 * 只有服务器/单机宿主的修改有效。服务器启动前读取会抛异常，只在游戏逻辑里调用。
 */
public final class AddonServerConfig {
    public enum BookLanguage { CHINESE, ENGLISH }

    public static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.BooleanValue WHITELIST_ENABLED;
    private static final ForgeConfigSpec.EnumValue<BookLanguage> BOOK_LANGUAGE;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> DISABLED_SKILLS;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        WHITELIST_ENABLED = b.comment("白名单总开关：开启后白名单内的目标不受技能伤害与负面效果。")
                .define("whitelistEnabled", true);
        BOOK_LANGUAGE = b.comment("故事书语言（影响服务器宝箱生成的书籍内容）。")
                .defineEnum("bookLanguage", BookLanguage.CHINESE);
        DISABLED_SKILLS = b.comment("全局禁用的技能，格式为 形态:技能。")
                .defineListAllowEmpty(List.of("disabledSkills"), List::of, o -> o instanceof String);
        SPEC = b.build();
    }

    private AddonServerConfig() {}

    public static boolean whitelistEnabled() { return WHITELIST_ENABLED.get(); }
    public static BookLanguage bookLanguage() { return BOOK_LANGUAGE.get(); }
    public static List<? extends String> disabledSkills() { return DISABLED_SKILLS.get(); }
}
