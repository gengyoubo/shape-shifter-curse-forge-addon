package github.com.gengyoubo.sscfa.energy;

/** 内部能量到 FE 的结算及推送、拉取共用的单 tick 输出预算。 */
public final class ConverterOutput {
    public static final int RATIO = 16;
    public static final int MAX_PER_TICK = 512;
    private long tick = Long.MIN_VALUE;
    private int used;

    public int available(IEnergyNetwork network, long currentTick) {
        int budget = currentTick == tick ? MAX_PER_TICK - used : MAX_PER_TICK;
        return (int) Math.min(budget, Math.max(0L, (long) network.getStoredEnergy()) * RATIO);
    }

    /** 模拟不扣能量、不占预算；实际输出向上取整扣除内部能量，与原转换器一致。 */
    public int extract(IEnergyNetwork network, long currentTick, int amount, boolean simulate) {
        if (amount <= 0) return 0;
        int output = Math.min(amount, available(network, currentTick));
        if (simulate || output == 0) return output;
        reset(currentTick);
        int cost = (output + RATIO - 1) / RATIO;
        int paid = network.extract(cost);
        output = Math.min(output, paid * RATIO);
        used += output;
        return output;
    }

    /** 接收方实际接收不足时退还未使用的整点内部能量和输出预算。 */
    public void refund(IEnergyNetwork network, long currentTick, int offered, int accepted) {
        if (currentTick != tick || accepted < 0 || accepted > offered || offered > used) {
            throw new IllegalArgumentException("Invalid converter refund");
        }
        int charged = (offered + RATIO - 1) / RATIO;
        int consumed = (accepted + RATIO - 1) / RATIO;
        if (charged > consumed) network.insert(charged - consumed);
        used -= offered - accepted;
    }

    private void reset(long currentTick) {
        if (tick != currentTick) {
            tick = currentTick;
            used = 0;
        }
    }
}
