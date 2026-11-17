package github.com.gengyoubo.sscfa.compat;

import github.com.gengyoubo.sscfa.resource.ResourceBars;
import github.com.gengyoubo.sscfa.state.SpiderMoonWeaverStateComponent;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.FormUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * Moon weaver's versions of the spider perks: faster web-bridge and attack charging, and the
 * perceptual web. Each is an addon power and counts while the player is in this form and holds it.
 *
 * <p>The Fabric version remapped SSC's spider perk purchases to these powers ({@code loadMapped});
 * SSC Forge applies perk powers per perk tree and no addon tree is registered for this form yet.</p>
 */
public final class SpiderMoonWeaverPerks {
    public static final ResourceLocation BRIDGE_WEAVER = addon("perk_spider_moon_weaver_bridge_weaver");
    public static final ResourceLocation RAPID_SPINNER = addon("perk_spider_moon_weaver_rapid_spinner");
    public static final ResourceLocation PERCEPTUAL_WEB = addon("perk_spider_moon_weaver_perceptual_web");

    private SpiderMoonWeaverPerks() {}

    public static boolean acceleratesCharge(Player player, int mode) {
        if (player == null || !FormUtils.isForm(player, FormIdentifiers.SPIDER_MOON_WEAVER)) return false;
        ResourceLocation power = mode == SpiderMoonWeaverStateComponent.MODE_ATTACK ? RAPID_SPINNER : BRIDGE_WEAVER;
        return ResourceBars.hasPowerId(player, power);
    }

    private static ResourceLocation addon(String path) {
        return new ResourceLocation("my_addon", path);
    }
}
