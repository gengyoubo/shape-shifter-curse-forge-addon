package github.com.gengyoubo.sscfa.compat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;
import net.minecraft.resources.ResourceLocation;
import net.onixary.shapeShifterCurseForge.api.registry.Perk;
import net.onixary.shapeShifterCurseForge.api.registry.PerkTree;
import net.onixary.shapeShifterCurseForge.api.registry.SscJavaRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 为附属形态注册可购买的 perk 树。
 *
 * <p>Fabric 版在 SSC 加载原版 perk 时把能力替换为附属版本（{@code loadMapped}）。SSC Forge 的 perk
 * 只在「树的形态组与 requiredForm 都匹配当前形态」时生效，而附属形态各有独立形态组，原版树对其不可见。
 * 这里在 SSC 内置树注册之后，为每个附属形态复制对应原版树：节点、连线、花费、图标、类型保持一致，
 * 节点 ID 为 {@code my_addon:<形态>/<原版 perk 路径>}，增减的能力按形态映射为附属能力。
 * 原版树变化时镜像自动跟随。购买记录按附属形态独立保存（与原版形态不共享）。</p>
 *
 * <p>远距扑袭：SSC Forge 的豹猫树不含该节点；若出现也会被标记为未实现，保持停用。</p>
 */
public final class AddonPerkTrees {
    private static final Logger LOG = LoggerFactory.getLogger("ssc_addon/perks");
    private static final String SSC = "shape-shifter-curse";
    private static final String ADDON = "my_addon";
    private static boolean registered;

    /** 一次镜像：形态、来源树，以及每个原版 perk 的能力调整。 */
    private record Mirror(String form, String sourceTree, Adjuster adjuster) {}

    /** 输入原版 perk ID 与其增/减能力（可修改的副本）；返回 false 表示该节点保留但不提供任何能力。 */
    @FunctionalInterface
    private interface Adjuster {
        boolean adjust(String perkPath, Set<ResourceLocation> added, Set<ResourceLocation> removed);
    }

    private AddonPerkTrees() {}

    public static synchronized void init() {
        if (registered) return;
        registered = true;
        for (Mirror mirror : mirrors()) {
            try {
                register(mirror);
            } catch (RuntimeException failure) {
                LOG.error("Could not mirror perk tree {} for {}", mirror.sourceTree(), mirror.form(), failure);
            }
        }
    }

    /** 附属形态镜像节点 ID。 */
    public static ResourceLocation perkId(String form, ResourceLocation original) {
        return ResourceLocation.fromNamespaceAndPath(ADDON, form + "/" + original.getPath());
    }

    public static ResourceLocation treeId(String form) {
        return ResourceLocation.fromNamespaceAndPath(ADDON, form + "_perk_tree");
    }

    private static void register(Mirror mirror) {
        ResourceLocation sourceId = ResourceLocation.fromNamespaceAndPath(SSC, mirror.sourceTree());
        PerkTree source = SscJavaRegistries.perkTree(sourceId).orElse(null);
        if (source == null) {
            LOG.warn("SSC perk tree {} is missing; {} has no perk tree", sourceId, mirror.form());
            return;
        }
        Map<ResourceLocation, ResourceLocation> ids = new LinkedHashMap<>();
        for (ResourceLocation original : source.perks()) {
            Perk perk = SscJavaRegistries.perk(original).orElse(null);
            if (perk == null) continue;
            ResourceLocation id = perkId(mirror.form(), original);
            SscJavaRegistries.registerPerk(id, copy(mirror, original, perk));
            ids.put(original, id);
        }
        if (ids.isEmpty()) return;
        PerkTree.Builder tree = PerkTree.builder(ResourceLocation.fromNamespaceAndPath(ADDON, "group_" + mirror.form()))
                .requiredForm(ResourceLocation.fromNamespaceAndPath(ADDON, mirror.form()));
        tree.perks(ids.values().toArray(ResourceLocation[]::new));
        for (PerkTree.Edge edge : source.edges()) {
            ResourceLocation from = ids.get(edge.from()), to = ids.get(edge.to());
            if (from != null && to != null) tree.edge(from, to);
        }
        SscJavaRegistries.registerPerkTree(treeId(mirror.form()), tree.build());
    }

    private static Perk copy(Mirror mirror, ResourceLocation original, Perk perk) {
        Set<ResourceLocation> added = new LinkedHashSet<>(perk.powersAdded());
        Set<ResourceLocation> removed = new LinkedHashSet<>(perk.powersRemoved());
        boolean active = !original.getPath().contains("long_pounce")
                && mirror.adjuster().adjust(original.getPath(), added, removed);
        Perk.Builder builder = Perk.builder()
                .requiredAttunerLevel(perk.requiredAttunerLevel())
                .experienceLevels(perk.experienceLevels())
                .experiencePoints(perk.experiencePoints())
                .kind(perk.kind())
                .implemented(perk.implemented() && !original.getPath().contains("long_pounce"));
        if (perk.iconX() >= 0 && perk.iconY() >= 0) builder.icon(perk.iconX(), perk.iconY());
        if (perk.costItem() != null && perk.itemCost() > 0) builder.itemCost(perk.costItem(), perk.itemCost());
        if (active) {
            if (!added.isEmpty()) builder.addPowers(added.toArray(ResourceLocation[]::new));
            if (!removed.isEmpty()) builder.removePowers(removed.toArray(ResourceLocation[]::new));
        }
        return builder.build();
    }

    // ------------------------------------------------------------------ 映射定义

    private static List<Mirror> mirrors() {
        List<Mirror> list = new ArrayList<>();
        // 美西螈 SP：水流魔力效率与潮汐牵引换为 SP 版本。
        list.add(new Mirror("axolotl_sp", "axolotl_3_perk_tree", mapping(Map.of(
                "perks/axolotl_water_magic_efficiency", addon("perk_axolotl_sp_water_magic_efficiency"),
                "perks/axolotl_tidal_pull", addon("perk_axolotl_sp_tidal_pull")))));
        // 荧光幼灵：耐旱为在各湿润档位之上的无条件加成（不移除原档位）；湿润回流额外提供命中回湿。
        list.add(new Mirror("axolotl_fluorescent", "axolotl_3_perk_tree", (perk, added, removed) -> {
            map(added, removed, Map.of(
                    "perks/axolotl_water_magic_efficiency", addon("perk_axolotl_fluorescent_water_magic_efficiency"),
                    "perks/axolotl_tidal_pull", addon("perk_axolotl_sp_tidal_pull"),
                    "perks/axolotl_dry_tolerance", addon("perk_axolotl_fluorescent_dry_tolerance")));
            if (perk.equals("axolotl_dry_tolerance")) removed.clear();
            if (perk.startsWith("axolotl_moisture_return_")) added.add(addon("perk_axolotl_fluorescent_moisture_return"));
            return true;
        }));
        // 升级美西螈：全部 perk 换为升级版本；植被与推进同时移除附属基础惩罚。
        list.add(new Mirror("upgrade_axolotl", "axolotl_3_perk_tree", (perk, added, removed) -> {
            map(added, removed, Map.of(
                    "perks/axolotl_vegetation", addon("perk_upgrade_axolotl_vegetation"),
                    "perks/axolotl_dry_tolerance", addon("perk_upgrade_axolotl_dry_tolerance"),
                    "perks/axolotl_propulsion_efficiency", addon("perk_upgrade_axolotl_propulsion_efficiency"),
                    "perks/axolotl_wave_efficiency", addon("perk_upgrade_axolotl_wave_efficiency"),
                    "perks/axolotl_water_magic_efficiency", addon("perk_upgrade_axolotl_water_magic_efficiency"),
                    "perks/axolotl_tidal_pull", addon("perk_upgrade_axolotl_tidal_pull")));
            if (perk.equals("axolotl_vegetation")) removed.add(addon("form_upgrade_axolotl_moisture_drain"));
            if (perk.equals("axolotl_propulsion_efficiency")) removed.add(addon("form_upgrade_axolotl_sprint_jump"));
            return true;
        }));
        // 使魔 SP / 赤狐 / 契灵：虹吸、渗透场换为附属版本；原版增益免疫换为形态自身的免疫能力。
        for (String form : new String[]{"familiar_fox_sp", "familiar_fox_red", "familiar_fox_mancianima"}) {
            ResourceLocation immunity = addon(form.equals("familiar_fox_mancianima")
                    ? "form_familiar_fox_mancianima_no_buff_effect" : "form_familiar_fox_sp_no_buff_effect");
            list.add(new Mirror(form, "familiar_fox_3_perk_tree", (perk, added, removed) -> {
                boolean field = perk.startsWith("familiar_fox_permeable_field_");
                map(added, removed, Map.of(
                        "perks/familiar_fox_siphon_1", addon("perk_familiar_fox_sp_siphon_1"),
                        "perks/familiar_fox_siphon_2", addon("perk_familiar_fox_sp_siphon_2"),
                        "perks/familiar_fox_permeable_field_1", addon("perk_familiar_fox_sp_permeable_field_1"),
                        "perks/familiar_fox_permeable_field_2", addon("perk_familiar_fox_sp_permeable_field_2"),
                        "form_familiar_fox_3_no_buff_effect", immunity));
                // 契灵的 Origin 同时带有原版与附属免疫能力。
                if (field && form.equals("familiar_fox_mancianima")) removed.add(ssc("form_familiar_fox_3_no_buff_effect"));
                return true;
            }));
        }
        // 升级使魔：原版 perks/familiar_fox_* 全部换为 perk_upgrade_familiar_fox_*。
        list.add(new Mirror("upgrade_familiar_fox", "familiar_fox_3_perk_tree", (perk, added, removed) -> {
            UnaryOperator<ResourceLocation> upgrade = power -> SSC.equals(power.getNamespace())
                    && power.getPath().startsWith("perks/familiar_fox_")
                    ? addon("perk_upgrade_" + power.getPath().substring("perks/".length())) : power;
            replace(added, upgrade);
            replace(removed, upgrade);
            if (perk.startsWith("familiar_fox_permeable_field_")) {
                removed.add(addon("form_upgrade_familiar_fox_buff_immunity_effect_immunity"));
            }
            return true;
        }));
        // 雪狐 SP / 霜脊：霜潜与轻踏换为附属版本。
        for (String form : new String[]{"snow_fox_sp", "snow_fox_frostspine"}) {
            list.add(new Mirror(form, "snow_fox_3_perk_tree", mapping(Map.of(
                    "perks/snow_fox_frost_dive", addon("perk_snow_fox_sp_frost_dive"),
                    "perks/snow_fox_elusive_paws", addon("perk_snow_fox_sp_elusive_paws")))));
        }
        // 风灵 / 新星：代谢与代谢过载换为附属版本；代谢 II 还要移除代谢 I 的附属版本。
        for (String[] ocelot : new String[][]{{"ocelot_wind_spirit", "perk_wind_spirit_"}, {"ocelot_nova", "perk_ocelot_nova_"}}) {
            String prefix = ocelot[1];
            list.add(new Mirror(ocelot[0], "ocelot_3_perk_tree", (perk, added, removed) -> {
                map(added, removed, Map.of(
                        "perks/ocelot_metabolism_1", addon(prefix + "metabolism_1"),
                        "perks/ocelot_metabolism_2", addon(prefix + "metabolism_2"),
                        "perks/ocelot_metabolic_overload", addon(prefix + "metabolic_overload")));
                if (perk.equals("ocelot_metabolism_2")) removed.add(addon(prefix + "metabolism_1"));
                return true;
            }));
        }
        // 冥裁（阿努比斯 SP）：召唤、魂激与魂召回换为附属版本。
        list.add(new Mirror("anubis_wolf_sp", "anubis_wolf_3_perk_tree", mapping(Map.of(
                "perks/anubis_wolf_summoning_1", addon("perk_anubis_wolf_sp_summoning_1"),
                "perks/anubis_wolf_summoning_2", addon("perk_anubis_wolf_sp_summoning_2"),
                "perks/anubis_wolf_soul_excitation", addon("perk_anubis_wolf_sp_soul_excitation"),
                "perks/anubis_wolf_soul_recall", addon("perk_anubis_wolf_sp_soul_recall")))));
        // 金沙岚：只加载附属能力，从不加载原版召唤/凋零能力；亡灵辨识只移除原版惩罚。
        list.add(new Mirror("golden_sandstorm_sp", "anubis_wolf_3_perk_tree", (perk, added, removed) -> {
            ResourceLocation mapped = switch (perk) {
                case "anubis_wolf_pack_response_1" -> addon("perk_golden_sandstorm_response_1");
                case "anubis_wolf_pack_response_2" -> addon("perk_golden_sandstorm_response_2");
                case "anubis_wolf_pack_amplification" -> addon("perk_golden_sandstorm_amplification");
                case "anubis_wolf_soul_excitation" -> addon("perk_golden_sandstorm_excitation");
                case "anubis_wolf_wither_tolerance_1" -> addon("perk_golden_sandstorm_vitality_1");
                case "anubis_wolf_wither_tolerance_2" -> addon("perk_golden_sandstorm_vitality_2");
                case "anubis_wolf_soul_recall" -> addon("perk_golden_sandstorm_recall");
                default -> null;
            };
            if (perk.equals("anubis_wolf_undead_discernment")) {
                added.clear();
                return true;
            }
            added.clear();
            removed.clear();
            if (mapped == null) return false;
            added.add(mapped);
            if (perk.equals("anubis_wolf_pack_response_2")) removed.add(addon("perk_golden_sandstorm_response_1"));
            if (perk.equals("anubis_wolf_wither_tolerance_2")) removed.add(addon("perk_golden_sandstorm_vitality_1"));
            return true;
        }));
        // 吸血蝠 / 寄生果蝠：回声定位与振翅换为附属版本；吸血蝠的回声定位还移除其基础版本。
        list.add(new Mirror("bat_desmodus", "bat_3_perk_tree", (perk, added, removed) -> {
            map(added, removed, Map.of(
                    "perks/bat_echolocation", addon("perk_bat_desmodus_echolocation"),
                    "perks/bat_wingbeat", addon("perk_bat_desmodus_wingbeat")));
            if (perk.equals("bat_echolocation")) removed.add(addon("form_bat_desmodus_echolocation"));
            return true;
        }));
        list.add(new Mirror("bat_parasitic_fruit", "bat_3_perk_tree", mapping(Map.of(
                "perks/bat_echolocation", addon("perk_bat_parasitic_fruit_echolocation"),
                "perks/bat_wingbeat", addon("perk_bat_parasitic_fruit_wingbeat")))));
        // 月织蛛：织桥、速纺与感知网换为附属版本。
        list.add(new Mirror("spider_moon_weaver", "spider_3_perk_tree", mapping(Map.of(
                "perks/spider_bridge_weaver", addon("perk_spider_moon_weaver_bridge_weaver"),
                "perks/spider_rapid_spinner", addon("perk_spider_moon_weaver_rapid_spinner"),
                "perks/spider_perceptual_web", addon("perk_spider_moon_weaver_perceptual_web")))));
        // 跳蛛：茧食、织桥、感知、速纺、稳弹与丝液换为跳蛛版本。
        list.add(new Mirror("spider_salticidae", "spider_3_perk_tree", mapping(Map.of(
                "perks/spider_cocoon_digestion_1", addon("perk_spider_salticidae_feeding_1"),
                "perks/spider_cocoon_digestion_2", addon("perk_spider_salticidae_feeding_2"),
                "perks/spider_bridge_weaver", addon("perk_spider_salticidae_charge"),
                "perks/spider_perceptual_web", addon("perk_spider_salticidae_sense"),
                "perks/spider_rapid_spinner", addon("perk_spider_salticidae_rapid"),
                "perks/spider_stabilized_projectile", addon("perk_spider_salticidae_stable"),
                "perks/spider_silk_secretion_1", addon("perk_spider_salticidae_recovery_1"),
                "perks/spider_silk_secretion_2", addon("perk_spider_salticidae_recovery_2")))));
        return list;
    }

    /** 列出所有镜像（形态、来源树），供测试与语言文件生成核对。 */
    public static List<String[]> describe() {
        List<String[]> out = new ArrayList<>();
        for (Mirror mirror : mirrors()) out.add(new String[]{mirror.form(), mirror.sourceTree()});
        return out;
    }

    private static Adjuster mapping(Map<String, ResourceLocation> byOriginalPath) {
        return (perk, added, removed) -> {
            map(added, removed, byOriginalPath);
            return true;
        };
    }

    /** 增减集合中的 SSC 原版能力按路径替换为附属能力。 */
    private static void map(Set<ResourceLocation> added, Set<ResourceLocation> removed, Map<String, ResourceLocation> byPath) {
        UnaryOperator<ResourceLocation> op = power -> SSC.equals(power.getNamespace())
                ? byPath.getOrDefault(power.getPath(), power) : power;
        replace(added, op);
        replace(removed, op);
    }

    private static void replace(Set<ResourceLocation> set, UnaryOperator<ResourceLocation> op) {
        List<ResourceLocation> copy = new ArrayList<>(set);
        set.clear();
        for (ResourceLocation power : copy) set.add(op.apply(power));
    }

    private static ResourceLocation addon(String path) { return ResourceLocation.fromNamespaceAndPath(ADDON, path); }

    private static ResourceLocation ssc(String path) { return ResourceLocation.fromNamespaceAndPath(SSC, path); }
}
