package github.com.gengyoubo.sscfa.screen;

import net.minecraft.world.inventory.ContainerData;

/** Encodes the first wideCount integers as two unsigned 16-bit screen properties. */
public final class SplitIntPropertyDelegate implements ContainerData {
    private final ContainerData source;
    private final int wideCount;

    public SplitIntPropertyDelegate(ContainerData source, int wideCount) {
        if (wideCount < 0 || wideCount > source.getCount()) throw new IllegalArgumentException("wideCount");
        this.source = source;
        this.wideCount = wideCount;
    }

    @Override
    public int get(int index) {
        if (index < wideCount * 2) return (source.get(index / 2) >>> ((index & 1) * 16)) & 0xffff;
        return source.get(index - wideCount);
    }

    @Override
    public void set(int index, int value) {
        if (index < wideCount * 2) {
            int shift = (index & 1) * 16;
            int previous = source.get(index / 2);
            source.set(index / 2, (previous & ~(0xffff << shift)) | ((value & 0xffff) << shift));
        } else source.set(index - wideCount, value);
    }

    @Override
    public int getCount() { return source.getCount() + wideCount; }
}
