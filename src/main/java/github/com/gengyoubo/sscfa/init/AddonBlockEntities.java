package github.com.gengyoubo.sscfa.init;

import github.com.gengyoubo.sscfa.SscAddonForge;
import github.com.gengyoubo.sscfa.block.PocketSpaceBlocks;
import github.com.gengyoubo.sscfa.block.InfusionAltarBlockEntity;
import github.com.gengyoubo.sscfa.block.SpellResearchTableBlockEntity;
import github.com.gengyoubo.sscfa.block.CreativeEnergyTankBlockEntity;
import github.com.gengyoubo.sscfa.block.EnergyStorageTankBlockEntity;
import github.com.gengyoubo.sscfa.block.EnergyConverterBlockEntity;
import github.com.gengyoubo.sscfa.block.EnergyExtractorBlockEntity;
import github.com.gengyoubo.sscfa.block.EnergyBottlerBlockEntity;
import github.com.gengyoubo.sscfa.block.PotionStorageBoxBlockEntity;
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
    public static final RegistryObject<BlockEntityType<EnergyConverterBlockEntity>> ENERGY_CONVERTER =
            BLOCK_ENTITIES.register("energy_converter", () -> BlockEntityType.Builder.of(
                    EnergyConverterBlockEntity::new, AddonBlocks.ENERGY_CONVERTER.get()).build(null));
    public static final RegistryObject<BlockEntityType<EnergyExtractorBlockEntity>> ENERGY_EXTRACTOR =
            BLOCK_ENTITIES.register("energy_extractor", () -> BlockEntityType.Builder.of(
                    EnergyExtractorBlockEntity::new, AddonBlocks.ENERGY_EXTRACTOR.get()).build(null));
    public static final RegistryObject<BlockEntityType<EnergyBottlerBlockEntity>> ENERGY_BOTTLER =
            BLOCK_ENTITIES.register("energy_bottler", () -> BlockEntityType.Builder.of(
                    EnergyBottlerBlockEntity::new, AddonBlocks.ENERGY_BOTTLER.get()).build(null));
    public static final RegistryObject<BlockEntityType<PotionStorageBoxBlockEntity>> POTION_STORAGE_BOX =
            BLOCK_ENTITIES.register("potion_storage_box", () -> BlockEntityType.Builder.of(
                    PotionStorageBoxBlockEntity::new, AddonBlocks.POTION_STORAGE_BOX.get()).build(null));

    // 闂佸憡鐟辩紞鈧い锝嗗缁艾煤椤忓拑绱甸梺鎸庣⊕濮樸劑鍩為弽顓炴闁荤喓澧楅崑銉╂倵閸︻厼浠掔紓宥咁儔閺佸秶浠﹂懖鈺冾啍闂佷紮绲鹃悷锕傚极閻愬搫绀冮悘鐐烘？缁ㄤ即鏌涜箛鏇炲付妞ゆ梹鍔欏顔款槻闁宦板妼椤?    
    public static final RegistryObject<BlockEntityType<PocketSpaceBlocks.VoidWallBlockEntity>> POCKET_SPACE_BEDROCK_WALL =
            BLOCK_ENTITIES.register("pocket_space_bedrock_wall", () -> BlockEntityType.Builder.of(
                    PocketSpaceBlocks.VoidWallBlockEntity::new, AddonBlocks.POCKET_SPACE_BEDROCK_WALL.get()).build(null));
    public static final RegistryObject<BlockEntityType<PocketSpaceBlocks.PortalBlockEntity>> POCKET_SPACE_PORTAL =
            BLOCK_ENTITIES.register("pocket_space_portal", () -> BlockEntityType.Builder.of(
                    PocketSpaceBlocks.PortalBlockEntity::new, AddonBlocks.POCKET_SPACE_PORTAL.get()).build(null));


    public static final RegistryObject<BlockEntityType<InfusionAltarBlockEntity>> INFUSION_ALTAR =
            BLOCK_ENTITIES.register("infusion_altar", () -> BlockEntityType.Builder.of(InfusionAltarBlockEntity::new, AddonBlocks.INFUSION_ALTAR.get()).build(null));
    public static final RegistryObject<BlockEntityType<SpellResearchTableBlockEntity>> SPELL_RESEARCH_TABLE =
            BLOCK_ENTITIES.register("spell_research_table", () -> BlockEntityType.Builder.of(SpellResearchTableBlockEntity::new, AddonBlocks.SPELL_RESEARCH_TABLE.get()).build(null));

    private AddonBlockEntities() {}
}
