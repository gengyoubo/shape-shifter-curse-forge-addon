package github.com.gengyoubo.sscfa.energy;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;

/** Checks the shared-pool operations without a running world. */
public final class EnergyNetworkTest {
    private static final class Storage implements IEnergyStorageNode {
        private int energy;
        @Override public int getStoredEnergy() { return energy; }
        @Override public int getEnergyCapacity() { return 100; }
        @Override public void setStoredEnergy(int value) { energy = value; }
        @Override public void markNetworkDirty() {}
    }
    private static final class Machine implements IEnergyMachine {
        @Override public void markNetworkDirty() {}
    }
    private static void check(boolean condition) {
        if (!condition) throw new AssertionError("Energy network contract failed");
    }
    public static void main(String[] args) {
        Storage first = new Storage(), second = new Storage();
        Machine machine = new Machine();
        List<IEnergyNode> nodes = List.of(first, machine, second);
        IEnergyNetwork network = EnergyNetwork.of(nodes);
        check(first.conductsEnergy() && !machine.conductsEnergy());
        check(network.getCapacity() == 200);
        check(network.insert(-1) == 0 && network.extract(-1) == 0);
        check(network.insert(150) == 150);
        check(network.getStoredEnergy() == 150);
        check(network.insert(100) == 50);
        check(network.extract(125) == 125);
        check(network.getStoredEnergy() == 75);
        check(network.extract(100) == 75);
        check(network.getStoredEnergy() == 0);
        check(network.insert(100) == 100);
        ConverterOutput converter = new ConverterOutput();
        check(converter.extract(network, 1, 17, true) == 17 && network.getStoredEnergy() == 100);
        check(converter.extract(network, 1, 17, false) == 17 && network.getStoredEnergy() == 98);
        int reserved = converter.extract(network, 1, 32, false);
        check(reserved == 32 && network.getStoredEnergy() == 96);
        converter.refund(network, 1, reserved, 0);
        check(network.getStoredEnergy() == 98 && converter.available(network, 1) == 495);
        check(converter.extract(network, 1, 1000, false) == 495);
        check(converter.available(network, 1) == 0);
        check(converter.available(network, 2) == 512);
        int before = network.getStoredEnergy();
        int offered = converter.extract(network, 2, 40, false);
        check(offered == 40 && network.getStoredEnergy() == before - 3);
        converter.refund(network, 2, offered, 17);
        check(network.getStoredEnergy() == before - 2);
        check(converter.available(network, 2) == 512 - 17);
        check(converter.extract(network, 3, 1, true) == 1);
        check(network.getStoredEnergy() == before - 2);
        Map<BlockPos, IEnergyNode> grid = new HashMap<>();
        BlockPos origin = BlockPos.ZERO;
        grid.put(origin, first);
        grid.put(origin.east(), machine);
        grid.put(origin.east(2), second);
        check(EnergyNetworkTopology.collect(origin, grid::get).equals(List.of(first)));
        grid.put(origin.east(), new Storage());
        check(EnergyNetworkTopology.collect(origin, grid::get).size() == 3);
        grid.clear();
        for (int x = 0; x < EnergyNetworkTopology.MAX_NETWORK; x++) grid.put(origin.east(x), new Storage());
        check(EnergyNetworkTopology.collect(origin, grid::get).size() == EnergyNetworkTopology.MAX_NETWORK);
        grid.put(origin.east(EnergyNetworkTopology.MAX_NETWORK), new Storage());
        check(EnergyNetworkTopology.collect(origin, grid::get).isEmpty());
        check(EnergyNetworkTopology.collect(origin.east(EnergyNetworkTopology.MAX_NETWORK), grid::get).isEmpty());

        Storage source = new Storage();
        source.setStoredEnergy(100);
        IEnergyNetwork converterNetwork = EnergyNetwork.of(List.of(source));
        ConverterOutput sharedOutput = new ConverterOutput();
        long[] clock = {10};
        boolean[] enabled = {false};
        boolean[] ready = {true};
        var forgeEnergy = new ConverterEnergyStorage(sharedOutput, () -> converterNetwork, () -> clock[0],
                () -> enabled[0], () -> ready[0]);
        check(!forgeEnergy.canReceive() && !forgeEnergy.canExtract());
        check(forgeEnergy.extractEnergy(16, false) == 0 && source.getStoredEnergy() == 100);
        enabled[0] = true;
        check(forgeEnergy.canExtract() && forgeEnergy.receiveEnergy(16, false) == 0);
        check(forgeEnergy.extractEnergy(160, true) == 160 && source.getStoredEnergy() == 100);
        check(sharedOutput.extract(converterNetwork, clock[0], 128, false) == 128);
        check(forgeEnergy.extractEnergy(512, false) == 384 && source.getStoredEnergy() == 68);
        check(forgeEnergy.extractEnergy(1, false) == 0);
        clock[0]++;
        check(forgeEnergy.extractEnergy(16, false) == 16 && source.getStoredEnergy() == 67);
        ready[0] = false;
        check(forgeEnergy.getEnergyStored() == 0 && forgeEnergy.extractEnergy(16, false) == 0);
        System.out.println("Energy network contract checks passed.");
    }
}
