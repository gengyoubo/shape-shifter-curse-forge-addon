package github.com.gengyoubo.sscfa.power;

import github.com.gengyoubo.sscfa.SscAddonForge;
import github.com.gengyoubo.sscfa.init.AddonEffects;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * 幽雾化形的碰撞箱：带雾化效果的玩家尺寸为宽 0.2、高 0.1、眼高 0.08（可钻 0.25 格缝隙）。
 * 用 Forge 的尺寸事件实现（Fabric 版用 Pehkui 缩放）；效果出现或消失时两端都重新计算尺寸，
 * 本人客户端的移动预测与服务端一致。离线时 {@link MistFormAbilityPower#onRemoved} 在保存玩家数据之前就解除雾化，
 * 所以重连后是原尺寸。
 */
@Mod.EventBusSubscriber(modid = SscAddonForge.MOD_ID)
public final class MistFormDimensions {
	private static final EntityDimensions MIST = EntityDimensions.scalable(0.2f, 0.1f);
	private static final float MIST_EYE_HEIGHT = 0.08f;
	/** 每个玩家上一 tick 是否雾化（两端各一份，弱引用不阻止回收）。 */
	private static final Map<Player, Boolean> LAST = new WeakHashMap<>();

	private MistFormDimensions() {}

	/**
	 * 低优先级：覆盖形态自身的尺寸缩放。Forge 在实体构造函数里也会发这个事件，那时效果表还没建好
	 * （{@code getActiveEffectsMap()} 为 null），直接跳过。
	 */
	@SubscribeEvent(priority = EventPriority.LOW)
	public static void onSize(EntityEvent.Size event) {
		if (event.getEntity() instanceof Player player && player.getActiveEffectsMap() != null
				&& AddonEffects.MIST_FORM.isPresent() && player.hasEffect(AddonEffects.MIST_FORM.get())) {
			event.setNewSize(MIST, false);
			event.setNewEyeHeight(MIST_EYE_HEIGHT);
		}
	}

	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase != TickEvent.Phase.END) return;
		Player player = event.player;
		boolean mist = player.hasEffect(AddonEffects.MIST_FORM.get());
		Boolean previous;
		synchronized (LAST) {
			previous = LAST.put(player, mist);
		}
		if (previous != null && previous != mist) player.refreshDimensions();
	}
}
