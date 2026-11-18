package github.com.gengyoubo.sscfa.loot;

import github.com.gengyoubo.sscfa.SscAddon;
import github.com.gengyoubo.sscfa.spell.FormationData;
import github.com.gengyoubo.sscfa.spell.FormationElement;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetNbtFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

/**
 * 增强法阵的自然宝箱生成（jackcooper）。与魔法卷轴同域注入：目标结构箱子 3% 概率触发 →
 * 在各系与通用变体的一至五级法阵中按权重抽取，四、五级为稀有产出。
 */
public final class FormationLoot {
	private FormationLoot() {
	}

	/** 每个箱子生成法阵的概率（比卷轴 5% 更稀有）。 */
	private static final float CHANCE = 0.03F;

	/** 各等级生成权重（index = level-1）。 */
	private static final int[] LEVEL_WEIGHTS = {40, 25, 12, 5, 2};

	/** 目标原版结构箱子战利品表（与魔法卷轴同域）。 */
	private static final ResourceLocation[] TARGET_CHESTS = {
			ResourceLocation.fromNamespaceAndPath("minecraft", "chests/simple_dungeon"),
			ResourceLocation.fromNamespaceAndPath("minecraft", "chests/abandoned_mineshaft"),
			ResourceLocation.fromNamespaceAndPath("minecraft", "chests/igloo_chest"),
			ResourceLocation.fromNamespaceAndPath("minecraft", "chests/woodland_mansion"),
			ResourceLocation.fromNamespaceAndPath("minecraft", "chests/ruined_portal"),
			ResourceLocation.fromNamespaceAndPath("minecraft", "chests/shipwreck_treasure"),
			ResourceLocation.fromNamespaceAndPath("minecraft", "chests/buried_treasure"),
			ResourceLocation.fromNamespaceAndPath("minecraft", "chests/underwater_ruin_small"),
			ResourceLocation.fromNamespaceAndPath("minecraft", "chests/underwater_ruin_big"),
			ResourceLocation.fromNamespaceAndPath("minecraft", "chests/stronghold_library"),
			ResourceLocation.fromNamespaceAndPath("minecraft", "chests/stronghold_corridor"),
			ResourceLocation.fromNamespaceAndPath("minecraft", "chests/stronghold_crossing"),
			ResourceLocation.fromNamespaceAndPath("minecraft", "chests/ancient_city"),
			ResourceLocation.fromNamespaceAndPath("minecraft", "chests/bastion_other"),
			ResourceLocation.fromNamespaceAndPath("minecraft", "chests/end_city_treasure")
	};

	public static void register() {
		LootTableMods.register((resourceManager, lootManager, id, tableBuilder, source) -> {
			if (!isTargetChest(id)) {
				return;
			}
// 3% 概率触发；触发后按系别（+通用系三变体）× 1-3 级法阵按权重抽一张
		LootPool.Builder pool = LootPool.lootPool()
				.setRolls(ConstantValue.exactly(1.0F))
				.when(LootItemRandomChanceCondition.randomChance(CHANCE));
			for (FormationElement element : FormationElement.values()) {
				for (String variant : variantsOf(element)) {
					for (int level = 1; level <= LEVEL_WEIGHTS.length; level++) {
						pool.add(formationEntry(element, variant, level, LEVEL_WEIGHTS[level - 1]));
					}
				}
			}
			tableBuilder.withPool(pool);
		});
	}

	/** 通用系三变体迭代（非通用系返回 null 占位）。 */
	private static String[] variantsOf(FormationElement element) {
		return element == FormationElement.UNIVERSAL
				? new String[]{FormationData.VARIANT_REGEN, FormationData.VARIANT_MANA,
						FormationData.VARIANT_EXP, FormationData.VARIANT_RECOVERY}
				: new String[]{null};
	}

	// 1.20.1 中 SetNbtLootFunction.builder(NbtCompound) 是唯一可用重载（@Deprecated 但无替代，同 MagicScrollLoot）
	@SuppressWarnings("deprecation")
	private static net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer.Builder<?> formationEntry(FormationElement element, String variant, int level, int weight) {
		CompoundTag nbt = createNaturalFormation(element, level, variant).getOrCreateTag();
		return LootItem.lootTableItem(SscAddon.FORMATION)
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

	/** 供命令调试：生成一张指定系别等级的法阵。 */
	public static ItemStack createFormation(FormationElement element, int level) {
		return FormationData.create(element, level);
	}

	public static ItemStack createNaturalFormation(FormationElement element, int level, String variant) {
		return github.com.gengyoubo.sscfa.spell.ArcaneAnalysis.markUnanalyzed(FormationData.create(element, level, variant));
	}
}
