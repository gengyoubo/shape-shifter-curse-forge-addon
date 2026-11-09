package github.com.gengyoubo.sscfa.ability;

import github.com.gengyoubo.sscfa.cooldown.SkillCastManager;
import github.com.gengyoubo.sscfa.cooldown.SkillCooldowns;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Vex;

/** 堕落悦灵召唤恼鬼：施放持续到最后一只恼鬼消失，之后按 power JSON 的 cooldown 起算（on_end）。 */
public final class FallenAllayVexSkill {

	public static final String SKILL_ID = "my_addon:form_fallen_allay_sp_active_vex_key_activation";
	/** 恼鬼自然寿命（与 SscAddonActions 召唤处的 setLifeTicks 保持一致，服务端权威推给 HUD 辅助栏）。 */
	public static final int VEX_LIFE_TICKS = 700; // 35s * 20 ticks
	private static final String CAST_TAG = "ssca_cast:";
	private static final String VEX_TAG = "ssc_fallen_allay_vex";
	private static final double SEARCH_RADIUS = 128.0;

	private FallenAllayVexSkill() {
	}

	public static boolean hasOwnedVex(ServerPlayer owner, Entity except) {
		String ownerTag = "owner:" + owner.getStringUUID();
		return !owner.serverLevel().getEntitiesOfClass(Vex.class, owner.getBoundingBox().inflate(SEARCH_RADIUS),
				v -> v != except && v.isAlive() && v.getTags().contains(VEX_TAG) && v.getTags().contains(ownerTag)).isEmpty();
	}

    private static boolean hasOwnedVex(ServerPlayer owner, Entity except, long castId) {
        String ownerTag = "owner:" + owner.getStringUUID();
        String castTag = CAST_TAG + castId;
        return !owner.serverLevel().getEntitiesOfClass(Vex.class, owner.getBoundingBox().inflate(SEARCH_RADIUS),
                v -> v != except && v.isAlive() && v.getTags().contains(VEX_TAG)
                        && v.getTags().contains(ownerTag) && v.getTags().contains(castTag)).isEmpty();
    }

	/** 召唤动作之后调用：有恼鬼在场即进入持续阶段，一只都没召出按失败结算。 */
	public static void onSummoned(ServerPlayer owner) {
		long castId = SkillCooldowns.currentCastId(owner, SKILL_ID);
        if (castId < 0) return;
        for (var vex : owner.serverLevel().getEntitiesOfClass(Vex.class, owner.getBoundingBox().inflate(SEARCH_RADIUS),
                v -> v.isAlive() && v.getTags().contains(VEX_TAG)
                        && v.getTags().contains("owner:" + owner.getStringUUID()))) {
            if (vex.getTags().stream().noneMatch(tag -> tag.startsWith(CAST_TAG))) vex.addTag(CAST_TAG + castId);
        }
        if (hasOwnedVex(owner, null, castId)) {
            SkillCooldowns.released(owner, SKILL_ID, castId);
            // 召唤成功：推送存活窗口给客户端（主技能辅助栏自满格倒数「召唤物消失时间」）
            github.com.gengyoubo.sscfa.network.SscAddonNetworking.syncFallenAllayVexWindow(owner, VEX_LIFE_TICKS);
        }
        else SkillCooldowns.failed(owner, SKILL_ID, castId);
	}

	/** 恼鬼死亡时调用：最后一只消失即结束施放。 */
	public static void onVexGone(ServerPlayer owner, Entity vex) {
		for (String tag : vex.getTags()) {
            if (!tag.startsWith(CAST_TAG)) continue;
            try {
                long castId = Long.parseLong(tag.substring(CAST_TAG.length()));
                if (!hasOwnedVex(owner, vex, castId)) {
                    SkillCooldowns.ended(owner, SKILL_ID, castId);
                    // 全部恼鬼消失：关闭存活窗口（辅助栏立即归零）
                    github.com.gengyoubo.sscfa.network.SscAddonNetworking.syncFallenAllayVexWindow(owner, 0);
                }
            } catch (NumberFormatException ignored) { }
        }
	}

	/** 兜底：恼鬼被卸载/清除（不走死亡）时，持续阶段也能结束，避免技能永久锁死。 */
	public static void watchdog(ServerPlayer owner) {
		SkillCastManager.Cast cast = SkillCastManager.get(owner.serverLevel()).control(owner.getUUID(), SKILL_ID);
		if (cast != null && cast.phase == SkillCastManager.PHASE_ACTIVE && !hasOwnedVex(owner, null, cast.castId)) {
			SkillCooldowns.ended(owner, SKILL_ID, cast.castId);
			// 兜底路径全灭：同样关闭存活窗口
			github.com.gengyoubo.sscfa.network.SscAddonNetworking.syncFallenAllayVexWindow(owner, 0);
		}
	}
}
