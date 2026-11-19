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
 * <p>The powers come from the spider_moon_weaver perk tree that {@link AddonPerkTrees} mirrors
 * from SSC's spider tree.</p>
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
        return ResourceLocation.fromNamespaceAndPath("my_addon", path);
    }
}
