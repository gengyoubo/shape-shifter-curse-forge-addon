package github.com.gengyoubo.sscfa.event;

import github.com.gengyoubo.sscfa.story.MoonScarStoryManager;
import github.com.gengyoubo.sscfa.story.TideSpiritStoryManager;

import java.util.UUID;

/**
 * 銆屾湀鐥曚箣鍔?銆屾疆姹愪箣鐏点€嶅墽鎯呯湡鐫℃湡闂达紝闃绘璇ョ帺瀹跺弬涓庡師鐗堣烦澶滃垽?
 * 锛堢敱 SscPlayerMixin ?canResetTimeBySleeping 娉ㄥ叆杩佺Щ鍒板畼?{@link EntitySleepEvents#ALLOW_RESETTING_TIME}锛?
 * <p>杩斿洖 false = 璇ョ帺瀹剁湡鐫′笉鎺ㄨ繘鏃堕棿 / 涓嶈烦澶滐紝璇箟绮剧‘绛変环浜庡師 {@code canResetTimeBySleeping} 杩斿洖 false?
 * 瀹㈡埛?STORY_SLEEPING 涓虹┖ ?杩斿洖 true 涓嶄粙鍏ワ紝涓庡師 mixin 琛屼负涓€鑷?
 */
public final class StorySleepTimeGuardHandler {

	private StorySleepTimeGuardHandler() {
	}

	public static void register() {
		github.com.gengyoubo.sscfa.util.SscEvents.allowResettingTime(player -> {
			UUID uuid = player.getUUID();
			if (MoonScarStoryManager.isStorySleeping(uuid) || TideSpiritStoryManager.isStorySleeping(uuid)) {
				return false; // 鍓ф儏鐪熺潯锛氫笉閲嶇疆鏃堕棿 / 涓嶈烦?
			}
			return true; // 榛樿鍏佽
		});
	}
}
