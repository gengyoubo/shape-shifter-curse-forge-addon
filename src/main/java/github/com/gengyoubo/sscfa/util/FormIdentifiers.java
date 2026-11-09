package github.com.gengyoubo.sscfa.util;

import net.minecraft.resources.ResourceLocation;

public class FormIdentifiers {
	// 羊形态（羊了个羊法术专属，2026-09-29 用户定稿）：空壳无 power 形态——变羊=切到它（技能/被动/魔法全失效），
	// 渲染由 SheepFormRenderMixin 画原版羊模型；仅法术触发，不进选择菜单/进化链
	public static final ResourceLocation SHEEP_FORM = new ResourceLocation("my_addon", "sheep_spell_form");
	public static final ResourceLocation FAMILIAR_FOX_SP = new ResourceLocation("my_addon", "familiar_fox_spirit_realm_lord");
	public static final ResourceLocation UPGRADE_FAMILIAR_FOX = new ResourceLocation("my_addon", "familiar_fox_upgrade");
	public static final ResourceLocation FAMILIAR_FOX_RED = new ResourceLocation("my_addon", "familiar_fox_red");
	public static final ResourceLocation FAMILIAR_FOX_MANCIANIMA = new ResourceLocation("my_addon", "familiar_fox_mancianima");
	public static final ResourceLocation SNOW_FOX_SP = new ResourceLocation("my_addon", "snow_fox_frost_spirit");
	// 寒棘狐（Frostspine）——雪狐线月髓环进化形态（原版雪狐 snow_fox_3 经月髓环进化）；复用原版雪狐模型/贴图，能力完全等同原版雪狐
	public static final ResourceLocation SNOW_FOX_FROSTSPINE = new ResourceLocation("my_addon", "snow_fox_frostspine");
	public static final ResourceLocation ALLAY_SP = new ResourceLocation("my_addon", "allay_crystal_sound");
	public static final ResourceLocation WILD_CAT_SP = new ResourceLocation("my_addon", "wild_cat_moonlight_phantom");
	// 食梦魔（Nightmare）——野猫线月髓环进化形态；入梦机制（累计伤害触发 debuff 免疫 + 粉红描边）
	public static final ResourceLocation WILD_CAT_NIGHTMARE = new ResourceLocation("my_addon", "wild_cat_nightmare");
	// 风灵（月髓环豹猫）——原版豹猫永久形态 ocelot_3 经月髓环进化获得；注册名 ocelot_wind_spirit
	public static final ResourceLocation OCELOT_SP = new ResourceLocation("my_addon", "ocelot_wind_spirit");
	// 朔望（月髓环豹猫）——与风灵同为原版豹猫 ocelot_3 线；定位：九命灵猫（极限生存/不死）
	public static final ResourceLocation OCELOT_NOVA = new ResourceLocation("my_addon", "ocelot_nova");
	// 朔望九命资源（0-9，被动死亡触发复活；HUD 用 NineLivesHudRenderer）
	public static final ResourceLocation OCELOT_NOVA_NINE_LIVES = new ResourceLocation("my_addon", "form_ocelot_nova_nine_lives");
	// 朔望舍身爆炸蓄力标记资源（0/1，同步客户端；蓄力期门控 sneaking_speed_up，禁 shift 潜行加速）
	public static final ResourceLocation OCELOT_NOVA_CHARGING = new ResourceLocation("my_addon", "form_ocelot_nova_charging");
	public static final ResourceLocation AXOLOTL_SP = new ResourceLocation("my_addon", "axolotl_abyssal_overlord");
	// 进化美西螈（Upgrade Axolotl）- SSCA 进化加点路线起点形态，基于原版美西螈 axolotl_3，能力按进化树解锁
	public static final ResourceLocation UPGRADE_AXOLOTL = new ResourceLocation("my_addon", "axolotl_upgrade");
	// 荧光幼灵（Axolotl Fluorescent）- SP美西螈经进化石进化获得
	public static final ResourceLocation AXOLOTL_FLUORESCENT = new ResourceLocation("my_addon", "axolotl_fluorescent");
	// 阿澪（Aling）- 特殊形态，基于荧光幼灵，技能一致，专属模型/贴图，颜色不可改
	public static final ResourceLocation AXOLOTL_ALING = new ResourceLocation("my_addon", "axolotl_aling");
	public static final ResourceLocation FALLEN_ALLAY_SP = new ResourceLocation("my_addon", "allay_spirit_of_the_fallen");
	public static final ResourceLocation ANUBIS_WOLF_SP = new ResourceLocation("my_addon", "anubis_wolf_netherjudge");
	public static final ResourceLocation SNOW_FOX_RESOURCE = new ResourceLocation("my_addon", "form_snow_fox_sp_resource");
	public static final ResourceLocation SNOW_FOX_REGEN_COOLDOWN = new ResourceLocation("my_addon", "form_snow_fox_sp_frost_regen_cooldown_resource");
	public static final ResourceLocation SNOW_FOX_MELEE_PRIMARY = new ResourceLocation("my_addon", "form_snow_fox_sp_melee_primary");
	public static final ResourceLocation SNOW_FOX_MELEE_SECONDARY = new ResourceLocation("my_addon", "form_snow_fox_sp_melee_secondary");
	public static final ResourceLocation SNOW_FOX_RANGED_SECONDARY = new ResourceLocation("my_addon", "form_snow_fox_sp_ranged_secondary");
	public static final ResourceLocation ALLAY_MANA_RESOURCE = new ResourceLocation("my_addon", "form_allay_sp_mana_resource");
	public static final ResourceLocation ALLAY_MANA_COOLDOWN = new ResourceLocation("my_addon", "form_allay_sp_mana_cooldown_resource");
	public static final ResourceLocation ALLAY_GROUP_HEAL = new ResourceLocation("my_addon", "form_allay_sp_group_heal");
	public static final ResourceLocation ALLAY_GROUP_HEAL_EXECUTE = new ResourceLocation("my_addon", "form_allay_sp_group_heal_heal_execute");
	public static final ResourceLocation ALLAY_GROUP_HEAL_SOLO_DAMAGE_TIMER = new ResourceLocation("my_addon", "form_allay_sp_group_heal_solo_damage_timer");
	// 使魔系红狐 SP 魔力初始化 resource（form_familiar_fox_sp_init_mana）
	public static final ResourceLocation FAMILIAR_FOX_MANA = new ResourceLocation("my_addon", "form_familiar_fox_sp_init_mana");
	// 使魔系红狐 SP 隐身 resource（form_familiar_fox_sp_visibility）
	public static final ResourceLocation FAMILIAR_FOX_VISIBILITY = new ResourceLocation("my_addon", "form_familiar_fox_sp_visibility");
	// 通用技能CD资源（用于HUD显示）
	public static final ResourceLocation SP_PRIMARY_CD = new ResourceLocation("my_addon", "form_sp_primary_cd");
	public static final ResourceLocation SP_SECONDARY_CD = new ResourceLocation("my_addon", "form_sp_secondary_cd");
	// 阿努比斯之狼SP灵魂能量资源
	public static final ResourceLocation ANUBIS_WOLF_SP_SOUL_ENERGY = new ResourceLocation("my_addon", "form_anubis_wolf_sp_soul_energy");
	// 吸血蝙蝠SP形态ID
	public static final ResourceLocation BAT_DESMODUS = new ResourceLocation("my_addon", "bat_desmodus");
	// 月织蛛SP形态ID（月髓环进化）
	public static final ResourceLocation SPIDER_MOON_WEAVER = new ResourceLocation("my_addon", "spider_moon_weaver");
	// 跳蛛（Salticidae）：蜘蛛_3 进化石分支（与月髓环→月织蛛并行）
	public static final ResourceLocation SPIDER_SALTICIDAE = new ResourceLocation("my_addon", "spider_salticidae");
	// 寄生果蝠形态ID
	public static final ResourceLocation BAT_PARASITIC_FRUIT = new ResourceLocation("my_addon", "bat_parasitic_fruit");
	// 寄生果蝠"种子量"能量资源（最大 10）
	public static final ResourceLocation BAT_PARASITIC_FRUIT_SEED_ENERGY = new ResourceLocation("my_addon", "form_bat_parasitic_fruit_seed_energy");
	// 吸血蝙蝠形态雾血资源
	public static final ResourceLocation BAT_BLOOD_RESOURCE = new ResourceLocation("my_addon", "form_bat_desmodus_blood_resource");
	// 雪狐形态切换状态
	public static final ResourceLocation SNOW_FOX_SWITCH_STATE = new ResourceLocation("my_addon", "form_snow_fox_sp_switch_state");
	// 金沙岚SP
	public static final ResourceLocation GOLDEN_SANDSTORM_SP = new ResourceLocation("my_addon", "anubis_wolf_golden_sandstorm");
	public static final ResourceLocation GOLDEN_SANDSTORM_COUNTER_BURST_CD = new ResourceLocation("my_addon", "form_golden_sandstorm_sp_counter_burst_cd");
	// 契灵 - 抗伤栏与无敌帧资源
	public static final ResourceLocation MANCIANIMA_RESISTANCE = new ResourceLocation("my_addon", "form_mancianima_resistance");
	public static final ResourceLocation MANCIANIMA_IFRAMES = new ResourceLocation("my_addon", "form_mancianima_iframes");
	// 契灵 - 红标联动攻击独立 CD（10s；期间禁联动、普通闪现仍 3.5s）
	public static final ResourceLocation MANCIANIMA_LINK_CD = new ResourceLocation("my_addon", "form_mancianima_link_cd");
	// SP使魔 / 红堕落使魔「击杀赋能」被动资源（KillEmpowerManager）。
	// 注意：宿主 power 是 apoli:multiple（form_empower_state），子资源真实 ID = power_id + "_" + sub_key
	public static final ResourceLocation EMPOWER_STATE = new ResourceLocation("my_addon", "form_empower_state_state");
	public static final ResourceLocation EMPOWER_TICKS = new ResourceLocation("my_addon", "form_empower_state_ticks");
	public static final ResourceLocation EMPOWER_RING_TICKS = new ResourceLocation("my_addon", "form_empower_state_ring_ticks");
	public static final ResourceLocation EMPOWER_RING_DURATION = new ResourceLocation("my_addon", "form_empower_state_ring_duration");

	private FormIdentifiers() {
	}
}
