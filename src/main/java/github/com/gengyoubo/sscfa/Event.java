package github.com.gengyoubo.sscfa;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.function.Function;

/**
 * Minimal array-backed callback event with the same shape as Fabric's {@code Event}.
 * Forge events are bridged into these in {@code ForgeEventBridge}.
 */
public final class Event<T> {
    private final Class<? super T> type;
    private final Function<T[], T> invokerFactory;
    private volatile T[] handlers;
    private volatile T invoker;

    @SuppressWarnings("unchecked")
    Event(Class<? super T> type, Function<T[], T> invokerFactory) {
        this.type = type;
        this.invokerFactory = invokerFactory;
        this.handlers = (T[]) Array.newInstance(type, 0);
        this.invoker = invokerFactory.apply(this.handlers);
    }

    public T invoker() {
        return invoker;
    }

    public synchronized void register(T listener) {
        T[] next = Arrays.copyOf(handlers, handlers.length + 1);
        next[handlers.length] = listener;
        handlers = next;
        invoker = invokerFactory.apply(next);
    }

    public boolean hasListeners() {
        return handlers.length > 0;
    }
}
