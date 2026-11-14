package github.com.gengyoubo.sscfa.event;

import github.com.gengyoubo.sscfa.entity.WitchFamiliarEntity;
import github.com.gengyoubo.sscfa.init.AddonEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * 女巫使魔伴生逻辑 + 野外自然生成注册。
 * - 袭击中生成的女巫会在附近生成1-3只女巫使魔
 * - 野外自然生成的女巫有低概率携带一只无主使魔（自动寻找附近女巫认主）
 * 使用命令标签确保每只女巫只检查一次（重新加载区块不会重复触发）。
 */
public final class WitchFamiliarSpawnHandler {

	private static final String CHECKED_TAG = "ssc_familiar_checked";
	private static final float WILD_FAMILIAR_CHANCE = 0.05f;
	private static boolean initialized;

	private WitchFamiliarSpawnHandler() {}

	public static void register() {
		if (initialized) return;
		initialized = true;

		FMLJavaModLoadingContext.get().getModEventBus()
				.addListener(WitchFamiliarSpawnHandler::onSpawnPlacementRegister);
		MinecraftForge.EVENT_BUS.addListener(WitchFamiliarSpawnHandler::onFinalizeSpawn);
		MinecraftForge.EVENT_BUS.addListener(WitchFamiliarSpawnHandler::onEntityJoin);
	}

	private static void onSpawnPlacementRegister(SpawnPlacementRegisterEvent event) {
		SpawnPlacements.SpawnPredicate<WitchFamiliarEntity> predicate =
				(type, level, reason, pos, random) ->
						Monster.checkMonsterSpawnRules(type, level, reason, pos, random);
		event.register(
				AddonEntities.WITCH_FAMILIAR.get(),
				SpawnPlacements.Type.ON_GROUND,
				Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				predicate,
				SpawnPlacementRegisterEvent.Operation.REPLACE
		);
	}

	/**
	 * 野外自然生成的女巫低概率携带一只无主使魔，弥补 Forge 侧没有 Fabric BiomeModifications 的问题。
	 */
	private static void onFinalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
		if (event.getSpawnType() != MobSpawnType.NATURAL) return;
		if (!(event.getEntity() instanceof Witch witch)) return;
		if (!(event.getLevel() instanceof ServerLevel level)) return;
		if (level.getRandom().nextFloat() >= WILD_FAMILIAR_CHANCE) return;

		WitchFamiliarEntity familiar = AddonEntities.WITCH_FAMILIAR.get().create(level);
		if (familiar == null) return;

		double offsetX = (witch.getRandom().nextDouble() - 0.5) * 3.0;
		double offsetZ = (witch.getRandom().nextDouble() - 0.5) * 3.0;
		familiar.moveTo(
				witch.getX() + offsetX,
				witch.getY(),
				witch.getZ() + offsetZ,
				witch.getRandom().nextFloat() * 360f,
				0f
		);
		level.addFreshEntity(familiar);
	}

	/**
	 * 袭击中生成的女巫伴生1-3只使魔（每只女巫只检查一次）。
	 */
	private static void onEntityJoin(EntityJoinLevelEvent event) {
		if (event.getLevel().isClientSide()) return;
		if (!(event.getEntity() instanceof Witch witch)) return;
		if (!(event.getLevel() instanceof ServerLevel level)) return;
		// 仅主世界伴生：女巫使魔不在下界/末地等非主世界生成（跟随原版女巫生成逻辑）
		if (level.dimension() != Level.OVERWORLD) return;
		// 每只女巫只检查一次
		if (witch.getTags().contains(CHECKED_TAG)) return;
		witch.addTag(CHECKED_TAG);

		// 仅袭击中生成的女巫才会伴生使魔
		if (!witch.hasActiveRaid()) return;

		// 随机生成1-3只使魔
		int count = 1 + witch.getRandom().nextInt(3);
		for (int i = 0; i < count; i++) {
			WitchFamiliarEntity familiar = AddonEntities.WITCH_FAMILIAR.get().create(level);
			if (familiar == null) continue;

			// 设置主人为该女巫
			familiar.setOwnerUuid(witch.getUUID());

			double offsetX = (witch.getRandom().nextDouble() - 0.5) * 3.0;
			double offsetZ = (witch.getRandom().nextDouble() - 0.5) * 3.0;
			familiar.moveTo(
					witch.getX() + offsetX,
					witch.getY(),
					witch.getZ() + offsetZ,
					witch.getRandom().nextFloat() * 360f,
					0f
			);
			level.addFreshEntity(familiar);
		}
	}
}
