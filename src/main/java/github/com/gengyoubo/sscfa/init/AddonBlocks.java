package github.com.gengyoubo.sscfa.init;

import github.com.gengyoubo.sscfa.SscAddonForge;
import github.com.gengyoubo.sscfa.block.EnergyStorageTankBlock;
import github.com.gengyoubo.sscfa.block.CreativeEnergyTankBlock;
import github.com.gengyoubo.sscfa.block.EnergyConverterBlock;
import github.com.gengyoubo.sscfa.block.EnergyExtractorBlock;
import github.com.gengyoubo.sscfa.block.EnergyBottlerBlock;
import github.com.gengyoubo.sscfa.block.PotionStorageBoxBlock;
import github.com.gengyoubo.sscfa.block.FacingShellBlock;
import github.com.gengyoubo.sscfa.block.PocketSpaceBlocks;
import github.com.gengyoubo.sscfa.block.WebMembraneBlock;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
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

    /** 法术研究台朝北碰撞箱：四角桌腿 + 桌面 + 北侧背板（取自 Fabric 版 SpellResearchTableBlock）。 */
    private static final VoxelShape RESEARCH_TABLE_NORTH_SHAPE = Shapes.or(
            Block.box(0, 0, 0, 2, 10, 2), Block.box(14, 0, 0, 16, 10, 2),
            Block.box(14, 0, 14, 16, 10, 16), Block.box(0, 0, 14, 2, 10, 16),
            Block.box(0, 10, 0, 16, 12, 16), Block.box(0, 12, 0, 16, 16, 1));

    // Internal pocket-space block: like the original, it has no item form.
    public static final RegistryObject<Block> POCKET_SPACE_FLOOR = BLOCKS.register("pocket_space_floor", FloorBlock::new);
    public static final RegistryObject<Block> POCKET_SPACE_BEDROCK_WALL = BLOCKS.register("pocket_space_bedrock_wall",
            PocketSpaceBlocks.InvisibleBedrockBlock::new);
    public static final RegistryObject<Block> POCKET_SPACE_PORTAL = BLOCKS.register("pocket_space_portal",
            PocketSpaceBlocks.PortalBlock::new);

    public static final RegistryObject<Block> WEB_MEMBRANE = BLOCKS.register("web_membrane", () ->
            new WebMembraneBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOL).strength(0.2f)
                    .sound(SoundType.WOOL).noCollission().noOcclusion().ignitedByLava().noLootTable()
                    .pushReaction(PushReaction.DESTROY)));
    // 注入祭坛 / 法术研究台：界面与方块实体依赖法术系统，暂为保留朝向与形状的空壳
    public static final RegistryObject<Block> INFUSION_ALTAR = BLOCKS.register("infusion_altar", () ->
            new FacingShellBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(2.5f)
                    .sound(SoundType.AMETHYST).requiresCorrectToolForDrops().noOcclusion(), null));
    public static final RegistryObject<Block> SPELL_RESEARCH_TABLE = BLOCKS.register("spell_research_table", () ->
            new FacingShellBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE).strength(2.5f)
                    .sound(SoundType.WOOD).requiresCorrectToolForDrops().noOcclusion(), RESEARCH_TABLE_NORTH_SHAPE));
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
