package github.com.gengyoubo.sscfa.energy;

/** A network participant; only conducting nodes join connected-component traversal. */
public interface IEnergyNode {
    void markNetworkDirty();

    /** Whether this node connects the network through its adjacent faces. */
    boolean conductsEnergy();
}
