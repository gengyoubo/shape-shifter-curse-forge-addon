package github.com.gengyoubo.sscfa.energy;

/** A conducting node with persistent internal energy. */
public interface IEnergyStorageNode extends IEnergyNode {
    int getStoredEnergy();
    int getEnergyCapacity();
    void setStoredEnergy(int value);

    @Override
    default boolean conductsEnergy() { return true; }
}
