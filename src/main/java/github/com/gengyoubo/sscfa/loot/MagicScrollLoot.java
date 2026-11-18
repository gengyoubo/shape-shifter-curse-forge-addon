package github.com.gengyoubo.sscfa.loot;

import github.com.gengyoubo.sscfa.SscAddon;
import github.com.gengyoubo.sscfa.spell.ScrollData;
import github.com.gengyoubo.sscfa.spell.ArcaneAnalysis;
import github.com.gengyoubo.sscfa.spell.SpellRegistry;
import github.com.gengyoubo.sscfa.spell.research.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetNbtFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

/**
 * 魔法卷轴的自然宝箱生成（jackcooper）。各法术卷轴等级固定 1-5 级，<b>不可升级</b>，
 * 只能从世界内自然生成的箱子中开出（照 {@link EvolutionItemsLoot} 模式注入原版战利品表）。
 *
 * <p>生成规则：目标结构箱子 5% 概率触发 → 先按「法术权重」抽出一个法术，再在「该法术 1-5 级」中
 * 按等级权重抽出一张（1 级最常见、5 级最稀有：权重 30/28/22/13/7）。等级存卷轴 NBT {@code Level}（缺省 1）。</p>
 */
public final class MagicScrollLoot {
	private MagicScrollLoot() {
	}

	/** 各等级生成权重（index = level-1）。 */
	private static final int[] LEVEL_WEIGHTS = {30, 28, 22, 13, 7};
	/** 各法术生成权重（与下方法顺序一致；新法术比冰锥稍稀有，AOE/防御类再低一档）。 */
	private static final int[] SPELL_WEIGHTS = {30, 22, 14, 8, 18, 14, 10, 16, 12, 14, 12, 8, 10, 8, 6, 10, 10, 8, 10, 8, 6, 6, 8};
	/** 参与宝箱抽取的法术 id 列表（顺序与权重一致）：冰锥/火球/烈焰新星/陨火/冰锥齐射/冰霜新星/霜甲/月光箭/月华治愈/月幕/月相/诅咒标记/恐惧低语/腐蚀之雾/召唤月灵/伙伴共鸣/虚空吞噬/虚空侵蚀/空间跳跃/空间漫步/空间归途/随身空间/羊了个羊。 */
	private static final String[] SPELL_PATHS = {
			"frost_spike", "fire_bolt", "flame_nova", "meteor", "ice_barrage", "frost_nova", "frost_armor",
			"moonlight_arrow", "lunar_mend", "lunar_veil", "lunar_phase", "curse_mark", "dread_whisper", "corrupt_mist",
			"summon_lunar_spirit", "companion_resonance", "void_devour", "void_erosion",
			"space_blink", "space_stride", "space_recall", "pocket_space", "beep_sheep"
	};
	/** 每个箱子生成魔法卷轴的概率。 */
	private static final float CHANCE = 0.05F;

	/** 目标原版结构箱子战利品表（覆盖常见探险结构，与故事书生成同域）。 */
	private static final ResourceLocation[] TARGET_CHESTS = {
			// 村庄各类箱子
			ResourceLocation.fromNamespaceAndPath("minecraft", "chests/village/village_plains_house"),
			ResourceLocation.fromNamespaceAndPath("minecraft", "chests/village/village_savanna_house"),
			ResourceLocation.fromNamespaceAndPath("minecraft", "chests/village/village_snowy_house"),
			ResourceLocation.fromNamespaceAndPath("minecraft", "chests/village/village_desert_house"),
			ResourceLocation.fromNamespaceAndPath("minecraft", "chests/village/village_taiga_house"),
			ResourceLocation.fromNamespaceAndPath("minecraft", "chests/village/village_fisher"),
			ResourceLocation.fromNamespaceAndPath("minecraft", "chests/village/village_armorer"),
			ResourceLocation.fromNamespaceAndPath("minecraft", "chests/village/village_butcher"),
			ResourceLocation.fromNamespaceAndPath("minecraft", "chests/village/village_cartographer"),
			ResourceLocation.fromNamespaceAndPath("minecraft", "chests/village/village_mason"),
			ResourceLocation.fromNamespaceAndPath("minecraft", "chests/village/village_shepherd"),
			ResourceLocation.fromNamespaceAndPath("minecraft", "chests/village/village_tannery"),
			ResourceLocation.fromNamespaceAndPath("minecraft", "chests/village/village_temple"),
			ResourceLocation.fromNamespaceAndPath("minecraft", "chests/village/village_toolsmith"),
			ResourceLocation.fromNamespaceAndPath("minecraft", "chests/village/village_weaponsmith"),
			// 地下城 / 废弃矿井 / 雪屋 / 林地府邸 / 废弃传送门
			ResourceLocation.fromNamespaceAndPath("minecraft", "chests/simple_dungeon"),
			ResourceLocation.fromNamespaceAndPath("minecraft", "chests/abandoned_mineshaft"),
			ResourceLocation.fromNamespaceAndPath("minecraft", "chests/igloo_chest"),
			ResourceLocation.fromNamespaceAndPath("minecraft", "chests/woodland_mansion"),
			ResourceLocation.fromNamespaceAndPath("minecraft", "chests/ruined_portal"),
			// 沉船 / 埋藏的宝藏 / 水下遗迹
			ResourceLocation.fromNamespaceAndPath("minecraft", "chests/shipwreck_supply"),
			ResourceLocation.fromNamespaceAndPath("minecraft", "chests/shipwreck_treasure"),
			ResourceLocation.fromNamespaceAndPath("minecraft", "chests/buried_treasure"),
			ResourceLocation.fromNamespaceAndPath("minecraft", "chests/underwater_ruin_small"),
			ResourceLocation.fromNamespaceAndPath("minecraft", "chests/underwater_ruin_big"),
			// 要塞 / 古城 / 堡垒遗迹 / 末地城
			ResourceLocation.fromNamespaceAndPath("minecraft", "chests/stronghold_library"),
			ResourceLocation.fromNamespaceAndPath("minecraft", "chests/stronghold_corridor"),
			ResourceLocation.fromNamespaceAndPath("minecraft", "chests/stronghold_crossing"),
			ResourceLocation.fromNamespaceAndPath("minecraft", "chests/ancient_city"),
			ResourceLocation.fromNamespaceAndPath("minecraft", "chests/bastion_other"),
			ResourceLocation.fromNamespaceAndPath("minecraft", "chests/end_city_treasure")
	};

	public static void register() {
		RandomRuneLootFunction.register();
		LootTableMods.register((resourceManager, lootManager, id, tableBuilder, source) -> {
			if (!isTargetChest(id)) {
				return;
			}
			// 5% 概率触发；触发后先抽法术、再抽等级（权重两层级联）
			LootPool.Builder pool = LootPool.lootPool()
					.setRolls(ConstantValue.exactly(1.0F))
					.when(LootItemRandomChanceCondition.randomChance(CHANCE));
			for (int s = 0; s < SPELL_PATHS.length; s++) {
				for (int level = 1; level <= LEVEL_WEIGHTS.length; level++) {
					pool.add(scrollEntry(SPELL_PATHS[s], level, LEVEL_WEIGHTS[level - 1] * SPELL_WEIGHTS[s]));
				}
			}
			tableBuilder.withPool(pool);
		});
	}

	// 1.20.1 中 SetNbtLootFunction.builder(NbtCompound) 是唯一可用重载（@Deprecated 但无替代，同 StoryBookLoot）
	@SuppressWarnings("deprecation")
	private static net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer.Builder<?> scrollEntry(String spellPath, int level, int weight) {
		CompoundTag nbt = createNaturalScroll(spellPath, level).getOrCreateTag();
		return LootItem.lootTableItem(SscAddon.MAGIC_SCROLL)
				.apply(SetNbtFunction.setTag(nbt))
				.setWeight(weight);
	}

	private static boolean isTargetChest(ResourceLocation id) {
		for (ResourceLocation target : TARGET_CHESTS) {
			if (target.equals(id)) {
				return true;
			}
		}
		return false;
	}

	/** 供命令调试：生成一张指定等级的冰锥卷轴。 */
	public static ItemStack createFrostSpikeScroll(int level) {
		return ScrollData.create("frost_spike", level);
	}

	public static ItemStack createNaturalScroll(String spellPath, int level) {
		var spell = SpellRegistry.get(spellPath);
		if (spell == null || level < 1 || level > spell.getMaxLevel()) return ItemStack.EMPTY;
		return ArcaneAnalysis.markUnanalyzed(ScrollData.create(spellPath, level));
	}

	public static ItemStack createNaturalScroll(WorldRuneState world, String spellPath, int level, long seed) {
		return createNaturalScroll(spellPath, level);
	}

	/** Compatibility for old callers: new loot is plain, existing imprints remain untouched. */
	public static ItemStack addRandomRunes(WorldRuneState world, ItemStack scroll, long seed) {
		return scroll;
	}
}
