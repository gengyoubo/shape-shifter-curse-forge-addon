package github.com.gengyoubo.sscfa.spell.spells;

import github.com.gengyoubo.sscfa.spell.Spell;
import github.com.gengyoubo.sscfa.spell.SpellRarity;
import github.com.gengyoubo.sscfa.spell.pocket.PocketSpaceManager;
import github.com.gengyoubo.sscfa.spell.pocket.PocketSpaceStorage;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class PocketSpaceSpell extends Spell {
	public PocketSpaceSpell() {
		super(new ResourceLocation("ssc_addon", "pocket_space"), SpellRarity.WHITE);
	}

	@Override
	public boolean canCast(ServerPlayer caster) {
		return PocketSpaceManager.canEnter(caster);
	}

	@Override
	public net.minecraft.network.chat.Component getCastFailureMessage(ServerPlayer caster) {
		if (github.com.gengyoubo.sscfa.anomaly.AnomalyEffects.teleportBlocked(caster))
            return net.minecraft.network.chat.Component.translatable("message.ssc_addon.anomaly.teleport_blocked");
		return net.minecraft.network.chat.Component.translatable("message.ssc_addon.pocket_space.unavailable");
	}

	@Override
	public boolean prepareScroll(ServerPlayer caster, ItemStack scroll) {
		if (!canCast(caster) || PocketSpaceStorage.bind(caster.getServer(), scroll) == null) {
			PocketSpaceManager.message(caster, "unavailable");
			return false;
		}
		PocketSpaceManager.prepareRoom(caster, scroll.getTag().getUUID(PocketSpaceStorage.SCROLL_ID));
		return true;
	}

	@Override
	public void cast(ServerPlayer caster, float power, boolean solo) {}

	@Override
	public boolean readyToRelease(ServerPlayer caster, ItemStack scroll) {
		return PocketSpaceManager.prepareRoom(caster, scroll.getTag().getUUID(PocketSpaceStorage.SCROLL_ID));
	}

	@Override
	public boolean canContinueCasting(ServerPlayer caster, ItemStack scroll) {
		return canCast(caster) && PocketSpaceStorage.findRoom(caster.getServer(),
				scroll.getTag().getUUID(PocketSpaceStorage.SCROLL_ID)) != null;
	}

	@Override
	public void cast(ServerPlayer caster, float power, boolean solo, int level, ItemStack scroll) {
		PocketSpaceManager.enterNow(caster, scroll.getTag().getUUID(PocketSpaceStorage.SCROLL_ID));
	}

}
