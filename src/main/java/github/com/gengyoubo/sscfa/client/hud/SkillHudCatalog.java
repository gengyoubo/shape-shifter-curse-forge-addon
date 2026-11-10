package github.com.gengyoubo.sscfa.client.hud;

import github.com.gengyoubo.sscfa.SscAddon;
import github.com.gengyoubo.sscfa.ability.AxolotlWaterSpurtHandler;
import github.com.gengyoubo.sscfa.ability.FluorescentLaserManager;
import github.com.gengyoubo.sscfa.ability.MancianimaPrimary;
import github.com.gengyoubo.sscfa.ability.MancianimaTeleport;
import github.com.gengyoubo.sscfa.ability.SpiderMoonWeaverWebManager;
import github.com.gengyoubo.sscfa.ability.VortexChargeManager;
import github.com.gengyoubo.sscfa.ability.WaterSpearLeapManager;
import github.com.gengyoubo.sscfa.balance.BalanceIntegration;
import github.com.gengyoubo.sscfa.evolution.AxolotlTree;
import github.com.gengyoubo.sscfa.evolution.RegEvolutionComponent;
import github.com.gengyoubo.sscfa.util.FormIdMigration;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.PowerUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.power.FormActivePowerService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

/** 当前形态的可显示技能表（HUD 用）。 */
public final class SkillHudCatalog {
    public record Skill(ResourceLocation icon, ResourceLocation cooldown, ResourceLocation internalCooldown,
                        int internalTicks, String talent, boolean primary,
                        Predicate<Player> condition,
                        Function<Player, ResourceLocation> iconOverride) {

        public String authorityId() {
            return cooldown == null ? "" : cooldown.toString();
        }

        public ResourceLocation resolveIcon(Player player) {
            return iconOverride == null ? icon : iconOverride.apply(player);
        }
    }

    private static final ResourceLocation ICON_INVISIBILITY =
            new ResourceLocation("my_addon", "textures/gui/skill_icons/invisibility.png");
    private static final ResourceLocation ICON_WEAKNESS =
            new ResourceLocation("my_addon", "textures/gui/skill_icons/weakness_spot.png");

    private static final Map<String, List<Skill>> FORMS = new HashMap<>();

    static int balanceInt(String scope, String parameter, int fallback) {
        var snapshot = BalanceIntegration.clientSnapshot();
        return snapshot == null ? fallback : (int) snapshot.getInt(scope, parameter);
    }

    private static boolean hasMana(Player player, double need) {
        return FormActivePowerService.hasMana(player, (float) need);
    }

    private static boolean hasResource(Player player, ResourceLocation id, int need) {
        return PowerUtils.getClientResourceValue(player, id) >= need;
    }

    private static boolean wildCatInvisible(Player player) {
        return player.hasEffect(SscAddon.TRUE_INVISIBILITY);
    }

    private static final Predicate<Player> SNOW_15 =
            p -> hasResource(p, FormIdentifiers.SNOW_FOX_RESOURCE,
                    balanceInt("abilities.snow_fox_sp_melee", "mana_cost", 15));
    private static final Predicate<Player> SNOW_20 =
            p -> hasResource(p, FormIdentifiers.SNOW_FOX_RESOURCE,
                    balanceInt("abilities.snow_fox_sp_teleport", "mana_cost_success", 30));
    private static final Predicate<Player> SNOW_30 =
            p -> hasResource(p, FormIdentifiers.SNOW_FOX_RESOURCE,
                    balanceInt("abilities.snow_fox_sp_frost_storm", "mana_cost", 30));

    private static final List<Skill> SNOW_MELEE = List.of(
            skill("snow_teleport", "form_snow_fox_sp_melee_secondary", true, SNOW_20),
            skill("snow_dash", "form_snow_fox_sp_melee_primary", false, SNOW_15),
            skill("snow_mode_melee", "form_snow_fox_sp_toggle", "form_snow_fox_sp_gain_cooldown", 300, null, false));
    private static final List<Skill> SNOW_RANGED = List.of(
            skill("snow_storm", "form_snow_fox_sp_ranged_secondary", true, SNOW_30),
            skill("snow_ball", "form_snow_fox_sp_ranged_primary", false,
                    p -> hasResource(p, FormIdentifiers.SNOW_FOX_RESOURCE, 15)),
            skill("snow_mode_ranged", "form_snow_fox_sp_toggle", "form_snow_fox_sp_gain_cooldown", 300, null, false));

    static {
        FORMS.put("familiar_fox_sp", List.of(
                skill("blue_fire_ring", "form_familiar_fox_sp_blue_fire_ring_key_activation",
                        null, 0, null, true, p -> hasMana(p, 99.0)),
                skill("fox_fire_breath", "form_familiar_fox_sp_fox_fire_breath", false, p -> hasMana(p, 15.0))));
        FORMS.put("familiar_fox_red", List.of(
                skill("red_fire_ring", "form_familiar_fox_red_blue_fire_ring_key_activation",
                        null, 0, null, true, p -> hasMana(p, 99.0)),
                skill("red_fire_breath", "form_familiar_fox_red_fox_fire_breath", false, p -> hasMana(p, 20.0))));
        FORMS.put("familiar_fox_sp_amulet", List.of(
                skill("blue_fire_ring", "form_familiar_fox_sp_blue_fire_ring_amulet_key_activation",
                        null, 0, null, true, p -> hasMana(p, 99.0)),
                FORMS.get("familiar_fox_sp").get(1)));
        FORMS.put("familiar_fox_red_amulet", List.of(
                skill("red_fire_ring", "form_familiar_fox_red_blue_fire_ring_amulet_key_activation",
                        null, 0, null, true, p -> hasMana(p, 119.0)),
                FORMS.get("familiar_fox_red").get(1)));
        FORMS.put("familiar_fox_mancianima", List.of(
                skill("contract_mark", "form_familiar_fox_mancianima_contract_mark", true,
                        p -> hasMana(p, balanceInt("abilities.mancianima_primary", "fizzle_mana_cost",
                                MancianimaPrimary.FIZZLE_MANA_COST))),
                skill("soul_teleport", "form_familiar_fox_mancianima_soul_teleport",
                        "form_mancianima_link_cd", MancianimaTeleport.RED_LINK_CD_TICKS, null, false,
                        p -> hasMana(p, balanceInt("abilities.mancianima_teleport", "mana_cost",
                                MancianimaTeleport.MANA_COST)))));
        pair("spider_moon_weaver", "moon_web", "form_spider_moon_weaver_web",
                "web_swing", "form_spider_moon_weaver_swing",
                p -> hasMana(p, SpiderMoonWeaverWebManager.startManaForHud()), p -> hasMana(p, 1.0));
        FORMS.put("axolotl_fluorescent", List.of(
                skill("fluorescent_laser", "form_axolotl_fluorescent_laser",
                        "form_axolotl_fluorescent_shot_cd", FluorescentLaserManager.shotTicksForHud(), null, true),
                skill("tidal_wave", "form_axolotl_fluorescent_tidal", false),
                skill("water_dash", "shape-shifter-curse:form_axolotl_2_water_spurt", false)));
        FORMS.put("axolotl_aling", FORMS.get("axolotl_fluorescent"));
        FORMS.put("anubis_wolf_sp", List.of(
                skill("death_domain", "form_anubis_wolf_sp_death_domain", true),
                skill("summon_wolves", "form_anubis_wolf_sp_summon_wolves", false)));
        pair("bat_desmodus", "blood_mist", "form_bat_desmodus_mist_form",
                "sonic_wave", "form_bat_desmodus_sonic_wave");
        pair("bat_parasitic_fruit", "parasitic_seed", "form_bat_parasitic_fruit_parasitic_seed",
                "spore_bomb", "form_bat_parasitic_fruit_spore_bomb");
        pair("golden_sandstorm_sp", "wither_sand", "form_golden_sandstorm_sp_wither_sand",
                "brand_detonation", "form_golden_sandstorm_sp_erosion_brand");
        pair("ocelot_nova", "nova_explosion", "form_ocelot_nova_charge",
                "spirit_leap", "form_ocelot_nova_leap");
        pair("ocelot_wind_spirit", "wind_dash", "form_ocelot_wind_spirit_wind_dash",
                "wind_claws", "form_ocelot_wind_spirit_claw_buff");
        pair("spider_salticidae", "jump_kill", "form_spider_salticidae_jump_kill",
                "venom_strike", "form_spider_salticidae_venom");
        pair("wild_cat_nightmare", "nightmare_fear", "form_wild_cat_nightmare_fear",
                "nightmare_spook", "form_wild_cat_nightmare_spook");
        FORMS.put("wild_cat_sp", List.of(
                new Skill(ICON_INVISIBILITY, powerId("true_invisibility"), null, 0, null, true, null,
                        p -> wildCatInvisible(p) ? ICON_WEAKNESS : ICON_INVISIBILITY),
                skill("intimidating_dash", "true_invisibility", false, SkillHudCatalog::wildCatInvisible),
                skill("ink_blind", "wild_cat_sp_ink_blind", false)));
        FORMS.put("allay_sp", List.of(
                skill("purify", "form_allay_sp_purify_key_activation", true),
                skill("group_heal", "form_allay_sp_group_heal_key_activation", false,
                        p -> hasResource(p, FormIdentifiers.ALLAY_MANA_RESOURCE, 100))));
        FORMS.put("fallen_allay_sp", List.of(
                skill("summon_vex", "form_fallen_allay_sp_active_vex_key_activation", true),
                skill("shadow_scream", "form_fallen_allay_sp_active_scream_key_activation", false)));
        FORMS.put("snow_fox_frostspine", List.of(
                skill("frost_spikes", "form_snow_fox_frostspine_frost_spike", true),
                skill("frost_forge", null, false, p -> hasHoverThorn())));
        FORMS.put("axolotl_sp", List.of(
                skill("vortex_impact", "form_axolotl_sp_vortex_charge", true,
                        p -> p.getAirSupply() >= VortexChargeManager.airPerHitForHud(p)),
                skill("play_dead", "form_axolotl_sp_play_dead_activate", false),
                skill("water_burst", "form_axolotl_sp_water_ball", false),
                skill("water_spear_craft", "form_axolotl_sp_water_spear_craft_spear", false),
                skill("water_jump", "form_axolotl_sp_jump_out_water", false),
                skill("water_dash", "shape-shifter-curse:form_axolotl_2_water_spurt", false)));
        FORMS.put("upgrade_axolotl", List.of(
                skill("water_spear", "form_upgrade_axolotl_water_spear", null, 0, AxolotlTree.NODE_WATER_SPEAR, true,
                        p -> p.getAirSupply() >= WaterSpearLeapManager.airCostForHud(p)),
                skill("vortex_guide", "form_upgrade_axolotl_vortex_guide", null, 0, AxolotlTree.NODE_VORTEX_GUIDE, false),
                skill("water_dash", "form_upgrade_axolotl_dash_hud_water", null, 0, AxolotlTree.NODE_WATER_SPURT, false),
                skill("land_dash", "form_upgrade_axolotl_dash_hud_land", null, 0, AxolotlTree.NODE_WATER_SPURT, false,
                        p -> p.getAirSupply() >= AxolotlWaterSpurtHandler.landAirCostForHud(p)),
                skill("water_jump", "form_upgrade_axolotl_jump_out_water", null, 0, "aquatic_adapt", false)));
        FORMS.put("upgrade_familiar_fox", List.of(
                skill("fire_spark", "form_upgrade_familiar_fox_spark", null, 0, "spark", true,
                        p -> hasMana(p, 11.0)),
                skill("fire_ring", "form_upgrade_familiar_fox_fire_ring", null, 0, "fire_ring", true,
                        p -> hasMana(p, 11.0)),
                skill("fire_rocket", "shape-shifter-curse:form_familiar_fox_fire_arrow_cooldown",
                        null, 0, "rocket", false, p -> hasMana(p, 2.5))));
    }

    private SkillHudCatalog() {
    }

    private static ResourceLocation powerId(String path) {
        return path == null ? null : new ResourceLocation(path.contains(":") ? path : "my_addon:" + path);
    }

    private static Skill skill(String icon, String cooldown, boolean primary) {
        return skill(icon, cooldown, null, 0, null, primary);
    }

    private static Skill skill(String icon, String cooldown, boolean primary, Predicate<Player> condition) {
        return skill(icon, cooldown, null, 0, null, primary, condition);
    }

    private static Skill skill(String icon, String cooldown, String internal, int ticks, String talent, boolean primary) {
        return skill(icon, cooldown, internal, ticks, talent, primary, null);
    }

    private static Skill skill(String icon, String cooldown, String internal, int ticks, String talent,
                               boolean primary, Predicate<Player> condition) {
        return new Skill(new ResourceLocation("my_addon", "textures/gui/skill_icons/" + icon + ".png"),
                powerId(cooldown), powerId(internal), ticks, talent, primary, condition, null);
    }

    private static void pair(String form, String primaryIcon, String primaryId,
                             String secondaryIcon, String secondaryId) {
        FORMS.put(form, List.of(skill(primaryIcon, primaryId, true), skill(secondaryIcon, secondaryId, false)));
    }

    private static void pair(String form, String primaryIcon, String primaryId,
                             String secondaryIcon, String secondaryId,
                             Predicate<Player> primaryCondition, Predicate<Player> secondaryCondition) {
        FORMS.put(form, List.of(skill(primaryIcon, primaryId, true, primaryCondition),
                skill(secondaryIcon, secondaryId, false, secondaryCondition)));
    }

    private static boolean hasHoverThorn() {
        return github.com.gengyoubo.sscfa.client.ClientTickCache.hasHoverThorn();
    }

    public static List<Skill> forForm(ResourceLocation form, Player player) {
        if (form == null || !"my_addon".equals(form.getNamespace())) return List.of();
        ResourceLocation canonical = FormIdMigration.canonical(form);
        String resourcePath = FormIdMigration.legacy(canonical).getPath();
        if ((FormIdentifiers.FAMILIAR_FOX_SP.equals(canonical) || FormIdentifiers.FAMILIAR_FOX_RED.equals(canonical))
                && github.com.gengyoubo.sscfa.client.ClientTickCache.isWearing(SscAddon.BLUE_FIRE_AMULET)) {
            return FORMS.getOrDefault(resourcePath + "_amulet", List.of());
        }
        if (FormIdentifiers.SNOW_FOX_SP.equals(canonical)) {
            return PowerUtils.getClientResourceValue(player, FormIdentifiers.SNOW_FOX_SWITCH_STATE) == 1
                    ? SNOW_RANGED : SNOW_MELEE;
        }
        return FORMS.getOrDefault(resourcePath, List.of());
    }

    public static boolean isUnlocked(Skill skill, Player player) {
        if (skill.talent() == null) return true;
        var evolution = RegEvolutionComponent.EVOLUTION.get(player);
        if ("spark".equals(skill.talent()) && evolution.isUnlocked("fire_ring")) return false;
        return evolution.isUnlocked(skill.talent());
    }

    public static List<Skill> forHud(ResourceLocation form, Player player) {
        return entriesForHud(form, player).stream().map(SkillHudLayout.Entry::skill).toList();
    }

    public static List<SkillHudLayout.Entry<Skill>> entriesForHud(ResourceLocation form, Player player) {
        if (form == null || player == null) return List.of();
        List<Skill> unlocked = forForm(form, player).stream()
                .filter(skill -> isUnlocked(skill, player)).toList();
        return SkillHudLayout.select(unlocked, Skill::primary, SkillHudCatalog::extraEligible, List.of());
    }

    private static boolean extraEligible(Skill skill) {
        String icon = skill.icon().getPath();
        return !icon.endsWith("/water_spear_craft.png") && !icon.endsWith("/water_jump.png")
                && !icon.endsWith("/water_dash.png") && !icon.endsWith("/land_dash.png");
    }
}
