package github.com.gengyoubo.sscfa.network;

/** 服务端时间锚，而不是墙钟计时器；暂停与最小值显式记录。 */
public record CountdownValue(int value, long tick, int minimum, boolean running) {
    public int at(long now) {
        if (!running) return value;
        long elapsed = Math.max(0, now - tick);
        return (int) Math.max(minimum, (long) value - elapsed);
    }

    public boolean agreesWith(CountdownValue actual) {
        return minimum == actual.minimum && running == actual.running && at(actual.tick) == actual.value;
    }
}
