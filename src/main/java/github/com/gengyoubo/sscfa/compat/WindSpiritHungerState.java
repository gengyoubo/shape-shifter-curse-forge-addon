package github.com.gengyoubo.sscfa.compat;

import net.minecraft.world.entity.player.Player;

/** Owner binding for the server-authoritative hunger hooks. */
public interface WindSpiritHungerState {
    void ssca$setOwner(Player player);
}
