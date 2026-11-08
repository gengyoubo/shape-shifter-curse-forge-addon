package github.com.gengyoubo.sscfa.energy;

import net.minecraftforge.energy.IEnergyStorage;

import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/** 转换器的单向 Forge Energy 能力；推送和拉取共用同一个输出预算。 */
public final class ConverterEnergyStorage implements IEnergyStorage {
    private final ConverterOutput output;
    private final Supplier<IEnergyNetwork> network;
    private final LongSupplier tick;
    private final BooleanSupplier enabled;
    private final BooleanSupplier available;

    public ConverterEnergyStorage(ConverterOutput output, Supplier<IEnergyNetwork> network, LongSupplier tick,
                                  BooleanSupplier enabled, BooleanSupplier available) {
        this.output = output;
        this.network = network;
        this.tick = tick;
        this.enabled = enabled;
        this.available = available;
    }

    @Override public int receiveEnergy(int maxReceive, boolean simulate) { return 0; }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        if (!enabled.getAsBoolean() || !available.getAsBoolean()) return 0;
        return output.extract(network.get(), tick.getAsLong(), maxExtract, simulate);
    }

    @Override
    public int getEnergyStored() {
        return enabled.getAsBoolean() && available.getAsBoolean()
                ? output.available(network.get(), tick.getAsLong()) : 0;
    }

    @Override public int getMaxEnergyStored() { return ConverterOutput.MAX_PER_TICK; }
    @Override public boolean canExtract() { return enabled.getAsBoolean(); }
    @Override public boolean canReceive() { return false; }
}
