package github.com.gengyoubo.sscfa.event;

import github.com.gengyoubo.sscfa.story.MoonScarStoryManager;
import github.com.gengyoubo.sscfa.story.TideSpiritStoryManager;

import java.util.UUID;

/**
 * 「月痕之?「潮汐之灵」剧情真睡期间，阻止该玩家参与原版跳夜判?
 * （由 SscPlayerMixin ?canResetTimeBySleeping 注入迁移到官?{@link EntitySleepEvents#ALLOW_RESETTING_TIME}?
 * <p>返回 false = 该玩家真睡不推进时间 / 不跳夜，语义精确等价于原 {@code canResetTimeBySleeping} 返回 false?
 * 客户?STORY_SLEEPING 为空 ?返回 true 不介入，与原 mixin 行为一?
 */
public final class StorySleepTimeGuardHandler {

	private StorySleepTimeGuardHandler() {
	}

	public static void register() {
		github.com.gengyoubo.sscfa.util.SscEvents.allowResettingTime(player -> {
			UUID uuid = player.getUUID();
			if (MoonScarStoryManager.isStorySleeping(uuid) || TideSpiritStoryManager.isStorySleeping(uuid)) {
				return false; // 剧情真睡：不重置时间 / 不跳?
			}
			return true; // 默认允许
		});
	}
}
