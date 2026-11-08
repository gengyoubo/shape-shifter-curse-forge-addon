package github.com.gengyoubo.sscfa.block;

import github.com.gengyoubo.sscfa.energy.ConverterOutput;
import github.com.gengyoubo.sscfa.energy.ConverterEnergyStorage;
import github.com.gengyoubo.sscfa.energy.EnergyNetwork;
import github.com.gengyoubo.sscfa.energy.EnergyNetworkTopology;
import github.com.gengyoubo.sscfa.energy.IEnergyMachine;
import github.com.gengyoubo.sscfa.energy.IEnergyNetwork;
import github.com.gengyoubo.sscfa.energy.IEnergyNode;
import github.com.gengyoubo.sscfa.init.AddonBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** 不储能的机器：向 Forge 能量接收方单向输出相邻网络的能量。 */
public final class EnergyConverterBlockEntity extends BlockEntity implements IEnergyMachine {
    private final ConverterOutput output = new ConverterOutput();
    private EnergyNetworkTopology.Cache networkCache;
    private boolean networkDirty = true;
    private boolean pushing;

    private final IEnergyStorage energyStorage = new ConverterEnergyStorage(output, this::getEnergyNetwork,
            () -> level.getGameTime(), this::isActive,
            () -> level != null && !level.isClientSide && !pushing);
    private LazyOptional<IEnergyStorage> capability = LazyOptional.of(() -> energyStorage);

    public EnergyConverterBlockEntity(BlockPos pos, BlockState state) {
        super(AddonBlockEntities.ENERGY_CONVERTER.get(), pos, state);
    }

    private boolean isActive() {
        return getBlockState().hasProperty(EnergyConverterBlock.ACTIVE)
                && getBlockState().getValue(EnergyConverterBlock.ACTIVE);
    }

    public IEnergyNetwork getEnergyNetwork() {
        List<IEnergyNode> nodes = networkCache == null ? null : networkCache.resolve(level);
        if (networkDirty || nodes == null) {
            nodes = List.of();
            if (level != null) {
                for (Direction direction : Direction.values()) {
                    BlockPos adjacent = worldPosition.relative(direction);
                    BlockEntity be = EnergyNetworkTopology.getLoadedBlockEntity(level, adjacent);
                    if (be instanceof IEnergyNode node && node.conductsEnergy() && !be.isRemoved()) {
                        nodes = EnergyNetworkTopology.collect(level, adjacent);
                        break;
                    }
                }
            }
            networkCache = new EnergyNetworkTopology.Cache(level, nodes);
            networkDirty = false;
        }
        return EnergyNetwork.of(nodes);
    }

    @Override public void markNetworkDirty() { networkDirty = true; }

    public static void tick(Level world, BlockPos pos, BlockState state, EnergyConverterBlockEntity converter) {
        if (world.isClientSide || !state.getValue(EnergyConverterBlock.ACTIVE)) return;
        for (Direction direction : Direction.values()) {
            BlockEntity neighbor = EnergyNetworkTopology.getLoadedBlockEntity(world, pos.relative(direction));
            if (neighbor == null) continue;
            neighbor.getCapability(ForgeCapabilities.ENERGY, direction.getOpposite()).ifPresent(target -> {
                if (target.canReceive()) converter.pushTo(target, world.getGameTime());
            });
        }
    }

    private void pushTo(IEnergyStorage target, long tick) {
        if (pushing) return;
        IEnergyNetwork network = getEnergyNetwork();
        int available = output.available(network, tick);
        if (available <= 0) return;
        pushing = true;
        try {
            int requested = Math.min(available, Math.max(0, target.receiveEnergy(available, true)));
            int offered = output.extract(network, tick, requested, false);
            if (offered <= 0) return;
            int accepted = 0;
            try {
                accepted = Math.min(offered, Math.max(0, target.receiveEnergy(offered, false)));
            } finally {
                output.refund(network, tick, offered, accepted);
            }
        } finally {
            pushing = false;
        }
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return cap == ForgeCapabilities.ENERGY ? capability.cast() : super.getCapability(cap, side);
    }

    @Override public void invalidateCaps() { super.invalidateCaps(); capability.invalidate(); }
    @Override public void reviveCaps() { super.reviveCaps(); capability = LazyOptional.of(() -> energyStorage); }
}
