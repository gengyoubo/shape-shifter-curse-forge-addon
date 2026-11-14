package github.com.gengyoubo.sscfa.event;

import github.com.gengyoubo.sscfa.entity.AxolotlShifterEntity;
import github.com.gengyoubo.sscfa.init.AddonEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public final class AxolotlShifterSpawnHandler {

	private static final String CHECKED_TAG = "ssc_axolotl_shifter_checked";
	private static final double COMPANION_RADIUS = 16.0;
	private static boolean initialized;

	private AxolotlShifterSpawnHandler() {}

	public static void register() {
		if (initialized) return;
		initialized = true;

		FMLJavaModLoadingContext.get().getModEventBus()
				.addListener(AxolotlShifterSpawnHandler::onSpawnPlacementRegister);
		MinecraftForge.EVENT_BUS.addListener(AxolotlShifterSpawnHandler::onFinalizeSpawn);
		MinecraftForge.EVENT_BUS.addListener(AxolotlShifterSpawnHandler::onEntityJoin);
	}

	private static void onSpawnPlacementRegister(SpawnPlacementRegisterEvent event) {
		SpawnPlacements.SpawnPredicate<AxolotlShifterEntity> predicate =
				(type, level, reason, pos, random) -> AxolotlShifterEntity.canSpawnInWater(type, level, reason, pos, random);
		event.register(
				AddonEntities.AXOLOTL_SHIFTER.get(),
				SpawnPlacements.Type.IN_WATER,
				Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				predicate,
				SpawnPlacementRegisterEvent.Operation.REPLACE
		);
	}

	private static void onFinalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
		if (event.getSpawnType() != MobSpawnType.NATURAL) return;
		if (!(event.getEntity() instanceof Axolotl axolotl)) return;
		if (!(event.getLevel() instanceof ServerLevel level)) return;
		trySpawnCompanion(axolotl, level);
	}

	private static void onEntityJoin(EntityJoinLevelEvent event) {
		if (event.getLevel().isClientSide()) return;
		if (!(event.getEntity() instanceof Axolotl axolotl)) return;
		if (!(event.getLevel() instanceof ServerLevel level)) return;
		if (level.dimension() != Level.OVERWORLD) return;
		if (axolotl.fromBucket()) return;
		if (axolotl.getTags().contains(CHECKED_TAG)) return;
		trySpawnCompanion(axolotl, level);
	}

	private static void trySpawnCompanion(Axolotl axolotl, ServerLevel level) {
		if (axolotl.getTags().contains(CHECKED_TAG)) return;
		axolotl.addTag(CHECKED_TAG);
		if (level.dimension() != Level.OVERWORLD) return;
		if (!level.getBiome(axolotl.blockPosition()).is(Biomes.LUSH_CAVES)) return;

		boolean nearbyExists = !level.getEntitiesOfClass(AxolotlShifterEntity.class,
				axolotl.getBoundingBox().inflate(COMPANION_RADIUS), Entity::isAlive).isEmpty();
		if (nearbyExists) return;

		AxolotlShifterEntity shifter = AddonEntities.AXOLOTL_SHIFTER.get().create(level);
		if (shifter == null) return;

		double offsetX = (axolotl.getRandom().nextDouble() - 0.5) * 3.0;
		double offsetZ = (axolotl.getRandom().nextDouble() - 0.5) * 3.0;
		shifter.moveTo(
				axolotl.getX() + offsetX,
				axolotl.getY(),
				axolotl.getZ() + offsetZ,
				axolotl.getRandom().nextFloat() * 360f,
				0f
		);
		level.addFreshEntity(shifter);
	}
}
