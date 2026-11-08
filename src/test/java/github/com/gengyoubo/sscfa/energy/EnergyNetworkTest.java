package github.com.gengyoubo.sscfa.energy;

import java.util.List;

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
        System.out.println("Energy network contract checks passed.");
    }
}
