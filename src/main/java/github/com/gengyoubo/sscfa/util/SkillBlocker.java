package github.com.gengyoubo.sscfa.util;

import github.com.gengyoubo.sscfa.config.AddonServerConfig;
import net.minecraft.server.level.ServerPlayer;

/** 技能禁用：玩家标签 {@code ssc_skill_blocked:<形态>:<技能>}（单人）或服务端配置的全局禁用列表。 */
public final class SkillBlocker {
    private static final String SKILL_BLOCKED_PREFIX = "ssc_skill_blocked:";

    private SkillBlocker() {}

    public static boolean isSkillBlocked(ServerPlayer player, String form, String skill) {
        String skillId = form + ":" + skill;
        return player.getTags().contains(SKILL_BLOCKED_PREFIX + skillId) || AddonServerConfig.disabledSkills().contains(skillId);
    }

    public static void blockSkill(ServerPlayer player, String form, String skill) {
        player.addTag(SKILL_BLOCKED_PREFIX + form + ":" + skill);
    }

    public static void unblockSkill(ServerPlayer player, String form, String skill) {
        player.removeTag(SKILL_BLOCKED_PREFIX + form + ":" + skill);
    }
}
