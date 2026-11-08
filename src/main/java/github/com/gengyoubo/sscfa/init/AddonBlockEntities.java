package github.com.gengyoubo.sscfa.init;

import github.com.gengyoubo.sscfa.SscAddonForge;
import github.com.gengyoubo.sscfa.block.CreativeEnergyTankBlockEntity;
import github.com.gengyoubo.sscfa.block.EnergyStorageTankBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class AddonBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, SscAddonForge.MOD_ID);
    public static final RegistryObject<BlockEntityType<EnergyStorageTankBlockEntity>> ENERGY_STORAGE_TANK =
            BLOCK_ENTITIES.register("energy_storage_tank", () -> BlockEntityType.Builder.of(
                    EnergyStorageTankBlockEntity::new, AddonBlocks.ENERGY_STORAGE_TANK.get()).build(null));
    public static final RegistryObject<BlockEntityType<CreativeEnergyTankBlockEntity>> CREATIVE_ENERGY_TANK =
            BLOCK_ENTITIES.register("creative_energy_tank", () -> BlockEntityType.Builder.of(
                    CreativeEnergyTankBlockEntity::new, AddonBlocks.CREATIVE_ENERGY_TANK.get()).build(null));

    private AddonBlockEntities() {}
}
