package github.com.gengyoubo.sscfa.compat;

import github.com.gengyoubo.sscfa.ability.GoldenSandstormErosionBrand;
import github.com.gengyoubo.sscfa.resource.ResourceBars;
import github.com.gengyoubo.sscfa.util.AbilityUseGuard;
import github.com.gengyoubo.sscfa.util.FormUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Golden Sandstorm's versions of the shared wolf perks. Each rank is an addon power; a rank
 * counts only while the player is in this form and holds that power.
 *
 * <p>The powers come from the golden_sandstorm_sp perk tree that {@link AddonPerkTrees} mirrors
 * from SSC's wolf tree (the Fabric version remapped the original purchases in {@code loadMapped}).</p>
 */
public final class GoldenSandstormPerks {
    public static final ResourceLocation RESPONSE_1 = addon("perk_golden_sandstorm_response_1");
    public static final ResourceLocation RESPONSE_2 = addon("perk_golden_sandstorm_response_2");
    public static final ResourceLocation AMPLIFICATION = addon("perk_golden_sandstorm_amplification");
    public static final ResourceLocation EXCITATION = addon("perk_golden_sandstorm_excitation");
    public static final ResourceLocation VITALITY_1 = addon("perk_golden_sandstorm_vitality_1");
    public static final ResourceLocation VITALITY_2 = addon("perk_golden_sandstorm_vitality_2");
    public static final ResourceLocation RECALL = addon("perk_golden_sandstorm_recall");

    private GoldenSandstormPerks() {}

    public static int responseRank(Player player) {
        return enabled(player, RESPONSE_2) ? 2 : enabled(player, RESPONSE_1) ? 1 : 0;
    }

    public static int normalCooldown(int baseTicks, int rank) {
        return Math.max(0, baseTicks - 60 * Math.max(0, Math.min(2, rank)));
    }

    public static int sandstormStacks(Player player) {
        return enabled(player, EXCITATION) ? 2 : 1;
    }

    public static float damagePercent(Player player, float base) {
        return scaledDamage(base, enabled(player, AMPLIFICATION));
    }

    public static float damageCap(Player player, float base) {
        return scaledDamage(base, enabled(player, AMPLIFICATION));
    }

    public static float scaledDamage(float base, boolean amplified) {
        return amplified ? base * 1.25f : base;
    }

    public static float recallHealing(float maximumHealth, int layers) {
        return Math.max(0, maximumHealth) * (Math.min(10, Math.max(0, layers)) * 0.02f);
    }

    /** Brand recall: needs the recall perk; false (a failed cast) when no brand was recalled. */
    public static boolean recall(ServerPlayer player) {
        return enabled(player, RECALL) && !AbilityUseGuard.blocked(player)
                && GoldenSandstormErosionBrand.recallAll(player) > 0;
    }

    private static boolean enabled(Player player, ResourceLocation id) {
        return player != null && FormUtils.isGoldenSandstormSP(player) && ResourceBars.hasPowerId(player, id);
    }

    private static ResourceLocation addon(String path) {
        return ResourceLocation.fromNamespaceAndPath("my_addon", path);
    }
}
