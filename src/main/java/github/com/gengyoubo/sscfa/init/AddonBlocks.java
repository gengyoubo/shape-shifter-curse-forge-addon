package github.com.gengyoubo.sscfa.init;

import github.com.gengyoubo.sscfa.SscAddonForge;
import github.com.gengyoubo.sscfa.block.EnergyStorageTankBlock;
import github.com.gengyoubo.sscfa.block.CreativeEnergyTankBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GameMasterBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class AddonBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, SscAddonForge.MOD_ID);

    // Internal pocket-space block: like the original, it has no item form.
    public static final RegistryObject<Block> POCKET_SPACE_FLOOR = BLOCKS.register("pocket_space_floor", FloorBlock::new);
    public static final RegistryObject<Block> ENERGY_STORAGE_TANK = BLOCKS.register("energy_storage_tank", () ->
            new EnergyStorageTankBlock(tankProperties()));
    public static final RegistryObject<Block> CREATIVE_ENERGY_TANK = BLOCKS.register("creative_energy_tank", () ->
            new CreativeEnergyTankBlock(tankProperties()));

    private static BlockBehaviour.Properties tankProperties() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(3.0f)
                .sound(SoundType.COPPER).requiresCorrectToolForDrops();
    }

    private AddonBlocks() {}

    private static final class FloorBlock extends Block implements GameMasterBlock {
        private FloorBlock() {
            super(BlockBehaviour.Properties.copy(Blocks.SMOOTH_QUARTZ)
                    .strength(-1.0f, 3_600_000.0f).noLootTable().pushReaction(PushReaction.BLOCK));
        }
    }
}
