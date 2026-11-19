package github.com.gengyoubo.sscfa.config;

/** 客户端配置：HUD 布局/开关/缩放与本地偏好，由 {@link SSCAddonConfig} 以 JSON 持久化。 */
public class SSCAddonClientConfig {
    public github.com.gengyoubo.sscfa.client.particle.ParticleAvoidance.Strength firstPersonParticleAvoidance =
            github.com.gengyoubo.sscfa.client.particle.ParticleAvoidance.Strength.OFF;
    public boolean showChargeBar = true;
    public boolean showCdBar = true;
    public boolean showCdSeconds = true;
    public boolean showSpellbookHud = true;
    public boolean showSscMagicHud = true;
    public boolean showManaBar = true;
    public boolean showInstinctBar = true;
    public boolean showNeoStabilityBar = true;
    public boolean showDisintegrationDebugBar = true;
    public boolean showSscItemStoreHud = true;

    public boolean chargeMirrorRight = true;
    public int chargeBarPosType = 4;
    public int chargeBarPosOffsetX = 0;
    public int chargeBarPosOffsetY = -34;

    public boolean cdMirrorRight = false;
    public int cdBarPosType = 4;
    public int cdBarPosOffsetX = 0;
    public int cdBarPosOffsetY = -34;
    public boolean cdSymmetric = false;
    public int cdSecondaryBarPosOffsetX = 0;
    public int cdSecondaryBarPosOffsetY = 0;

    public int skillHudLayoutVersion = 4;

    public int spellbookHudPosType = 4;
    public int spellbookHudPosOffsetX = 26;
    public int spellbookHudPosOffsetY = -60;

    public java.util.Map<String, Integer> hudScalePercent = new java.util.HashMap<>();

    /** 是否用附属高级调色界面替换 SSC 原版形态颜色选择菜单。 */
    public boolean enableColorEditor = true;

    /** 已观看过颜色编辑器教程的存档/服务器标识。 */
    public java.util.List<String> colorTutorialSeenSaves = new java.util.ArrayList<>();

    /** Gson 不经构造器还原缺失字段，这里补回集合与枚举的默认值并钳制缩放百分比。 */
    public void sanitize() {
        if (firstPersonParticleAvoidance == null) {
            firstPersonParticleAvoidance = github.com.gengyoubo.sscfa.client.particle.ParticleAvoidance.Strength.OFF;
        }
        if (hudScalePercent == null) hudScalePercent = new java.util.HashMap<>();
        hudScalePercent.values().removeIf(java.util.Objects::isNull);
        hudScalePercent.replaceAll((key, value) -> github.com.gengyoubo.sscfa.client.hud.HudScaleGeometry.percent(value));
        if (colorTutorialSeenSaves == null) colorTutorialSeenSaves = new java.util.ArrayList<>();
        colorTutorialSeenSaves.removeIf(java.util.Objects::isNull);
    }

    public void migrateSkillHudLayout() {
        // Forge 简化版：不再执行旧版布局迁移。
        skillHudLayoutVersion = 4;
    }
}
