package github.com.gengyoubo.sscfa.compat;

/** Per-entity transient water-recovery snapshot; client/server copies never share it. */
public interface MoistureTickSnapshot {
    int ssca$moistureBefore(long epoch);
    void ssca$moistureBefore(long epoch, int air);
}
