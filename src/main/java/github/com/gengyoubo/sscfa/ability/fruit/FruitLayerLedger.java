package github.com.gengyoubo.sscfa.ability.fruit;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/** 单个世界的权威先进先出账本：每个宿主总共最多三层（含所有施法者与果实种类）。 */
public final class FruitLayerLedger {
    public static final int MAX_LAYERS = 3;

    public record Layer(UUID owner, FruitSelection fruit, long plantedAt, long rootedAt, long expiresAt, long sequence) {
        public boolean rooted(long now) { return now >= rootedAt; }
    }

    private final Map<UUID, ArrayDeque<Layer>> hosts = new LinkedHashMap<>();
    private final Map<UUID, LinkedHashSet<UUID>> ownerHosts = new HashMap<>();
    private long sequence;

    /** 种下一层；施法者的宿主数超过上限时移除其最早的宿主。返回需要重新结算的宿主。 */
    public Set<UUID> add(UUID host, UUID owner, FruitSelection fruit, long now, int life, int rooting, int maxHosts) {
        Objects.requireNonNull(host);
        Objects.requireNonNull(owner);
        Objects.requireNonNull(fruit);
        if (fruit == FruitSelection.AUTO) throw new IllegalArgumentException("Resolve AUTO before planting");
        var changed = new LinkedHashSet<UUID>(prune(now, ignored -> true));
        var owned = ownerHosts.computeIfAbsent(owner, ignored -> new LinkedHashSet<>());
        if (!owned.contains(host) && owned.size() >= Math.max(1, maxHosts)) {
            UUID oldest = owned.iterator().next();
            removeOwnerFromHost(owner, oldest);
            changed.add(oldest);
        }
        var layers = hosts.computeIfAbsent(host, ignored -> new ArrayDeque<>());
        if (layers.size() == MAX_LAYERS) layers.removeFirst();
        layers.addLast(new Layer(owner, fruit, now, now + Math.max(0, rooting), now + Math.max(1, life), sequence++));
        changed.add(host);
        rebuildOwners();
        return Set.copyOf(changed);
    }

    public Set<UUID> prune(long now, Predicate<Layer> valid) {
        var changed = new LinkedHashSet<UUID>();
        var iterator = hosts.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (entry.getValue().removeIf(layer -> now >= layer.expiresAt() || !valid.test(layer))) changed.add(entry.getKey());
            if (entry.getValue().isEmpty()) iterator.remove();
        }
        if (!changed.isEmpty()) rebuildOwners();
        return Set.copyOf(changed);
    }

    public Set<UUID> removeOwner(UUID owner) {
        var changed = new LinkedHashSet<UUID>();
        for (UUID host : List.copyOf(hosts.keySet())) {
            if (removeOwnerFromHost(owner, host)) changed.add(host);
        }
        rebuildOwners();
        return Set.copyOf(changed);
    }

    public void removeHost(UUID host) {
        hosts.remove(host);
        rebuildOwners();
    }

    public List<Layer> layers(UUID host) {
        var value = hosts.get(host);
        return value == null ? List.of() : List.copyOf(value);
    }

    public Set<UUID> hosts() { return Set.copyOf(hosts.keySet()); }

    private boolean removeOwnerFromHost(UUID owner, UUID host) {
        var value = hosts.get(host);
        if (value == null) return false;
        boolean removed = value.removeIf(layer -> layer.owner().equals(owner));
        if (value.isEmpty()) hosts.remove(host);
        return removed;
    }

    private void rebuildOwners() {
        var actual = new HashMap<UUID, LinkedHashSet<UUID>>();
        hosts.forEach((host, layers) -> layers.forEach(layer ->
                actual.computeIfAbsent(layer.owner(), ignored -> new LinkedHashSet<>()).add(host)));
        // 施法者在某宿主上仍有层时保留原插入顺序（决定 FIFO 淘汰谁）。
        actual.forEach((owner, active) -> {
            var previous = ownerHosts.get(owner);
            if (previous != null) {
                var ordered = new LinkedHashSet<UUID>();
                previous.stream().filter(active::contains).forEach(ordered::add);
                ordered.addAll(active);
                actual.put(owner, ordered);
            }
        });
        ownerHosts.clear();
        ownerHosts.putAll(actual);
    }
}
