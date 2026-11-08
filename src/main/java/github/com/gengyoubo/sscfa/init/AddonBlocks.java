package github.com.gengyoubo.sscfa.init;

import github.com.gengyoubo.sscfa.SscAddonForge;
import github.com.gengyoubo.sscfa.block.EnergyStorageTankBlock;
import github.com.gengyoubo.sscfa.block.CreativeEnergyTankBlock;
import github.com.gengyoubo.sscfa.block.EnergyConverterBlock;
import github.com.gengyoubo.sscfa.block.EnergyExtractorBlock;
import github.com.gengyoubo.sscfa.block.EnergyBottlerBlock;
import github.com.gengyoubo.sscfa.block.PotionStorageBoxBlock;
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
    public static final RegistryObject<Block> ENERGY_CONVERTER = BLOCKS.register("energy_converter", () ->
            new EnergyConverterBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0f)
                    .sound(SoundType.COPPER).requiresCorrectToolForDrops().noOcclusion()
                    .lightLevel(EnergyConverterBlock::luminanceOf)));
    public static final RegistryObject<Block> ENERGY_EXTRACTOR = BLOCKS.register("energy_extractor", () ->
            new EnergyExtractorBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(3.0f)
                    .sound(SoundType.AMETHYST).requiresCorrectToolForDrops().noOcclusion()));
    public static final RegistryObject<Block> ENERGY_BOTTLER = BLOCKS.register("energy_bottler", () ->
            new EnergyBottlerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.5f)
                    .sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));
    public static final RegistryObject<Block> POTION_STORAGE_BOX = BLOCKS.register("potion_storage_box", () ->
            new PotionStorageBoxBlock(BlockBehaviour.Properties.of().mapColor(MapColor.PODZOL)
                    .strength(2.0f).sound(SoundType.WOOD)));

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
