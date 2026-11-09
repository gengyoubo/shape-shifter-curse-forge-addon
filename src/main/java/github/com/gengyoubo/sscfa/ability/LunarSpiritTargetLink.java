package github.com.gengyoubo.sscfa.ability;

import github.com.gengyoubo.sscfa.entity.LunarSpiritBoltEntity;
import github.com.gengyoubo.sscfa.entity.LunarSpiritEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import java.util.List;

/**
 * 鏈堢伒鐩爣鑱斿姩锛坖ackcooper锛屾湇鍔＄鏉冨▉锛夈€傛寕 {@code ALLOW_DAMAGE}锛堜激瀹崇粨绠楀墠璁板綍锛宺eturn true 涓嶆嫤鎴級锛?
 * <ul>
 *   <li>涓讳汉锛堢帺瀹讹級浜叉墜瀵圭敓鐗╅€犳垚浼ゅ 鈫?璇ョ帺瀹跺満涓婂叏閮ㄦ湀鐏典互鏈€楂樹紭鍏堢骇闆嗙伀鍙楀鑰咃紙銆屼綘鎵撹皝鏈堢伒灏辨墦璋併€嶏級銆?
 *       鏈堢伒鍏夊脊鐨勪激瀹虫簮鏄厜寮规湰浣擄紝鏄庣‘鎺掗櫎锛岄伩鍏嶅鍙湀鐏垫瘡娆″懡涓兘浜掔浉鎶㈢洰鏍囷紱</li>
 *   <li>涓讳汉琚敓鐗╂敾鍑?鈫?娌℃湁鏇撮珮浼樺厛绾х洰鏍囩殑鏈堢伒鍙嶅嚮鏀诲嚮鑰咃紙鎶や富锛夈€?/li>
 * </ul>
 * 鍚堟硶鎬э紙鐧藉悕鍗?/ 鍚屼富浜烘湀鐏?/ 鍒涢€犵帺瀹?/ 14 鏍煎唴锛夌敱 {@link LunarSpiritEntity#offerTarget} 缁熶竴鏍￠獙銆?
 * 鏈堢伒濮嬬粓鍦ㄤ富浜?3 鏍煎唴锛屾晠鍙渶鍦ㄤ富浜哄懆鍥?8 鏍兼壘鏈堢伒銆?
 */
public final class LunarSpiritTargetLink {
	private LunarSpiritTargetLink() {
	}

	public static void init() {
		github.com.gengyoubo.sscfa.util.SscEvents.allowDamage((victim, source, amount) -> {
			if (!(victim.level() instanceof ServerLevel serverWorld)) {
				return true;
			}
			Entity attackerEntity = source.getEntity();
			// 鎯呭舰涓€锛氫富浜轰翰鎵嬮€犳垚浼ゅ锛堣繎鎴?/ 绠?/ 娉曟湳锛宎ttacker 閮芥槸鐜╁锛夆啋 闆嗙伀锛涘厜寮瑰懡涓帓闄?
			if (attackerEntity instanceof ServerPlayer player
					&& !(source.getDirectEntity() instanceof LunarSpiritBoltEntity)) {
				if (victim instanceof LunarSpiritEntity spirit && player.getUUID().equals(spirit.getOwnerUuid())) {
					return true; // 涓讳汉璇嚮鑷繁鐨勬湀鐏典笉绠?
				}
				for (LunarSpiritEntity spirit : spiritsOf(serverWorld, player)) {
					spirit.offerTarget(victim, LunarSpiritEntity.TIER_OWNER_ATTACKED);
				}
				return true;
			}
			// 鎯呭舰浜岋細涓讳汉琚敾鍑?鈫?鏈堢伒鎶や富鍙嶅嚮
			if (victim instanceof ServerPlayer owner
					&& attackerEntity instanceof LivingEntity attacker && attacker != owner) {
				for (LunarSpiritEntity spirit : spiritsOf(serverWorld, owner)) {
					spirit.offerTarget(attacker, LunarSpiritEntity.TIER_DEFENSE);
				}
			}
			return true; // 涓嶆嫤鎴激瀹虫湰韬?
		});
	}

	private static List<LunarSpiritEntity> spiritsOf(ServerLevel serverWorld, Player owner) {
		return serverWorld.getEntitiesOfClass(LunarSpiritEntity.class,
				owner.getBoundingBox().inflate(8.0),
				spirit -> owner.getUUID().equals(spirit.getOwnerUuid()));
	}
}
