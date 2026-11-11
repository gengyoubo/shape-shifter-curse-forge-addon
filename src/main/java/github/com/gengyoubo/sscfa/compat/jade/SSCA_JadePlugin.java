package github.com.gengyoubo.sscfa.compat.jade;

import github.com.gengyoubo.sscfa.block.EnergyBottlerBlockEntity;
import github.com.gengyoubo.sscfa.block.EnergyExtractorBlockEntity;
import github.com.gengyoubo.sscfa.block.EnergyStorageTankBlockEntity;
import github.com.gengyoubo.sscfa.block.InfusionAltarBlockEntity;
import github.com.gengyoubo.sscfa.block.SpellResearchTableBlockEntity;
import github.com.gengyoubo.sscfa.energy.EnergyNetwork;
import github.com.gengyoubo.sscfa.entity.TidalOrbEntity;
import github.com.gengyoubo.sscfa.spell.SpellbookData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.item.ItemStack;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

@WailaPlugin("ssc_addon")
public final class SSCA_JadePlugin implements IWailaPlugin {
    private static final ResourceLocation ENERGY_UID = new ResourceLocation("ssc_addon", "energy");
    private static final ResourceLocation RESEARCH_UID = new ResourceLocation("ssc_addon", "research");
    private static final ResourceLocation ALTAR_UID = new ResourceLocation("ssc_addon", "altar");
    private static final ResourceLocation TOOLTIP_UID = new ResourceLocation("ssc_addon", "block_tooltip");
    private static final ResourceLocation TIDAL_ORB_UID = new ResourceLocation("ssc_addon", "tidal_orb");

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(new EnergyProvider(), EnergyStorageTankBlockEntity.class);
        registration.registerBlockDataProvider(new ExtractorEnergyProvider(), EnergyExtractorBlockEntity.class);
        registration.registerBlockDataProvider(new BottlerEnergyProvider(), EnergyBottlerBlockEntity.class);
        registration.registerBlockDataProvider(new ResearchProvider(), SpellResearchTableBlockEntity.class);
        registration.registerBlockDataProvider(new AltarProvider(), InfusionAltarBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(new BlockTooltipProvider(), Block.class);
        registration.registerEntityComponent(new TidalOrbProvider(), TidalOrbEntity.class);
    }

    private abstract static class BaseBlockProvider implements IServerDataProvider<BlockAccessor> {
        @Override
        public int getDefaultPriority() {
            return 1000;
        }
    }

    private static final class EnergyProvider extends BaseBlockProvider {
        @Override public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (accessor.getBlockEntity() instanceof EnergyStorageTankBlockEntity tank) {
                data.putInt("Energy", tank.getStoredEnergy());
                data.putInt("Capacity", tank.getEnergyCapacity());
            }
        }
        @Override public ResourceLocation getUid() { return ENERGY_UID; }
    }

    private static final class ExtractorEnergyProvider extends BaseBlockProvider {
        @Override public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (accessor.getBlockEntity() instanceof EnergyExtractorBlockEntity extractor) {
                data.putInt("Energy", extractor.getStoredEnergy());
                data.putInt("Capacity", extractor.getEnergyCapacity());
            }
        }
        @Override public ResourceLocation getUid() { return new ResourceLocation("ssc_addon", "extractor_energy"); }
    }

    private static final class BottlerEnergyProvider extends BaseBlockProvider {
        @Override public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (accessor.getBlockEntity() instanceof EnergyBottlerBlockEntity bottler) {
                var network = EnergyNetwork.of(bottler.getEnergyNetwork());
                data.putInt("Energy", network.getStoredEnergy());
                data.putInt("Capacity", network.getCapacity());
                data.putInt("CraftTime", EnergyBottlerBlockEntity.CRAFT_TIME);
                data.putInt("EnergyPerBottle", EnergyBottlerBlockEntity.ENERGY_PER_BOTTLE);
            }
        }
        @Override public ResourceLocation getUid() { return new ResourceLocation("ssc_addon", "bottler_energy"); }
    }

    private static final class ResearchProvider extends BaseBlockProvider {
        @Override public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (accessor.getBlockEntity() instanceof SpellResearchTableBlockEntity table) {
                int ticks = table.getAnalysisTicks();
                data.putInt("AnalysisTicks", ticks);
                data.putInt("AnalysisMax", SpellResearchTableBlockEntity.ANALYSIS_TICKS);
                data.putString("AnalysisStatus",
                        SpellResearchTableBlockEntity.analysisStatus(table, ticks).name().toLowerCase());
            }
        }
        @Override public ResourceLocation getUid() { return RESEARCH_UID; }
    }

    private static final class AltarProvider extends BaseBlockProvider {
        @Override public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (accessor.getBlockEntity() instanceof InfusionAltarBlockEntity altar) {
                ItemStack book = altar.getItem(0);
                data.putInt("SpellbookLevel", SpellbookData.getLevel(book));
                int formations = 0;
                for (int i = 3; i < 3 + SpellbookData.MAX_FORMATION_SLOTS; i++) {
                    if (!altar.getItem(i).isEmpty()) formations++;
                }
                data.putInt("Formations", formations);
            }
        }
        @Override public ResourceLocation getUid() { return ALTAR_UID; }
    }

    private static final class BlockTooltipProvider implements IBlockComponentProvider {
        @Override public ResourceLocation getUid() { return TOOLTIP_UID; }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (data.contains("Energy")) {
                int energy = data.getInt("Energy");
                int capacity = data.getInt("Capacity");
                tooltip.add(Component.translatable("jade.ssc_addon.energy", energy, capacity));
            }
            if (data.contains("AnalysisTicks")) {
                int ticks = data.getInt("AnalysisTicks");
                int max = Math.max(1, data.getInt("AnalysisMax"));
                tooltip.add(Component.translatable("jade.ssc_addon.analysis",
                        String.format("%.1f", ticks / 20.0), String.format("%.1f", max / 20.0)));
                tooltip.add(Component.translatable("jade.ssc_addon.analysis_status." + data.getString("AnalysisStatus")));
            }
            if (data.contains("SpellbookLevel")) {
                tooltip.add(Component.translatable("jade.ssc_addon.spellbook",
                        data.getInt("SpellbookLevel"), data.getInt("Formations")));
            }
        }
    }

    private static final class TidalOrbProvider implements IEntityComponentProvider {
        @Override public ResourceLocation getUid() { return TIDAL_ORB_UID; }
        @Override public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
            Entity entity = accessor.getEntity();
            if (!(entity instanceof TidalOrbEntity orb) || !orb.isTetherActive()) return;
            tooltip.add(Component.translatable("jade.ssc_addon.tether",
                    String.format("%.1f", orb.getTetherRemain() / 20.0),
                    String.format("%.1f", orb.getTetherTotal() / 20.0)));
        }
    }
}
