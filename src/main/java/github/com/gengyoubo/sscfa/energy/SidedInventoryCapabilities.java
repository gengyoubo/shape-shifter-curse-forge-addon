package github.com.gengyoubo.sscfa.energy;

import net.minecraft.core.Direction;
import net.minecraft.world.WorldlyContainer;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import org.jetbrains.annotations.Nullable;

/** 为原版双面容器提供 Forge 物品能力，并在卸载时失效。 */
public final class SidedInventoryCapabilities {
    private final WorldlyContainer container;
    private LazyOptional<IItemHandlerModifiable>[] sides;
    private LazyOptional<IItemHandlerModifiable> unsided;

    public SidedInventoryCapabilities(WorldlyContainer container) {
        this.container = container;
        revive();
    }

    public LazyOptional<IItemHandlerModifiable> get(@Nullable Direction side) {
        return side == null ? unsided : sides[side.ordinal()];
    }

    public void invalidate() {
        for (var side : sides) side.invalidate();
        unsided.invalidate();
    }

    public void revive() {
        sides = SidedInvWrapper.create(container, Direction.values());
        unsided = LazyOptional.of(() -> new InvWrapper(container));
    }
}
