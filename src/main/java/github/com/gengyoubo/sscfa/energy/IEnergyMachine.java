package github.com.gengyoubo.sscfa.energy;

/** A machine without its own storage, normally connected to one adjacent network. */
public interface IEnergyMachine extends IEnergyNode {
    @Override
    default boolean conductsEnergy() { return false; }
}
