package github.com.gengyoubo.sscfa.event;

import github.com.gengyoubo.sscfa.SscAddonForge;
import github.com.gengyoubo.sscfa.moonmarrow.MoonMarrowForms;
import github.com.gengyoubo.sscfa.moonmarrow.MoonMarrowRing;
import github.com.gengyoubo.sscfa.util.FormIdMigration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.npc.Villager;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 阻止特定形态与村民交易（双端拦截：客户端拦下即不发交互包，服务端兜底）? * <ul>
 *   <li>进化使魔：任何右键村民都禁止交易?/li>
 *   <li>灵界之主：潜行右键村民时禁止交易?/li>
 * </ul>
 * 形?ID 统一?{@link FormIdMigration#canonical} 比较，新?ID 都能识别? */
@Mod.EventBusSubscriber(modid = SscAddonForge.MOD_ID)
public final class VillagerTradeGuardHandler {
	private static final ResourceLocation UPGRADE_FAMILIAR_FOX = FormIdMigration.canonical(MoonMarrowRing.UPGRADE_FAMILIAR_FOX);
	private static final ResourceLocation FAMILIAR_FOX_SP = FormIdMigration.canonical(MoonMarrowRing.FAMILIAR_FOX_SP);

	private VillagerTradeGuardHandler() {}

	@SubscribeEvent
	public static void onInteractEntity(PlayerInteractEvent.EntityInteract event) {
		if (!(event.getTarget() instanceof Villager)) return;
		ResourceLocation form = FormIdMigration.canonical(MoonMarrowForms.currentFormId(event.getEntity()));
		if (form == null) return;
		if (UPGRADE_FAMILIAR_FOX.equals(form) || event.getEntity().isShiftKeyDown() && FAMILIAR_FOX_SP.equals(form)) {
			event.setCanceled(true);
			event.setCancellationResult(InteractionResult.FAIL);
		}
	}
}
