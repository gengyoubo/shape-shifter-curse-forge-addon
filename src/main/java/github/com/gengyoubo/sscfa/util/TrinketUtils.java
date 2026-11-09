package github.com.gengyoubo.sscfa.util;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

import java.util.function.Predicate;

/**
 * 饰品检测：实体是否在 Curios 饰品栏里戴着某饰品。Curios 是可选依赖，没装时一律视为未佩戴，
 * 可以在服务端 tick 和 HUD 里放心调用。
 */
public final class TrinketUtils {
    private static final boolean CURIOS = ModList.get() != null && ModList.get().isLoaded("curios");

    private TrinketUtils() {}

    public static boolean isWearing(LivingEntity entity, Item item) {
        return entity != null && CURIOS && Curios.isEquipped(entity, stack -> stack.is(item));
    }

    /** 与 {@link #isWearing(LivingEntity, Item)} 相同（Fabric 版用来兼容多套饰品框架，Forge 上只有 Curios）。 */
    public static boolean isWearingAuto(LivingEntity entity, Item item) {
        return isWearing(entity, item);
    }

    public static boolean isWearing(LivingEntity entity, Predicate<ItemStack> predicate) {
        return entity != null && CURIOS && Curios.isEquipped(entity, predicate);
    }

    /** 第一个满足条件的已佩戴饰品；没有返回 null。 */
    public static @Nullable ItemStack findFirstEquipped(LivingEntity entity, Predicate<ItemStack> predicate) {
        return entity != null && CURIOS ? Curios.findFirst(entity, predicate) : null;
    }

    /** 只在装了 Curios 时加载，避免引用 Curios 类导致类加载失败。 */
    private static final class Curios {
        static boolean isEquipped(LivingEntity entity, Predicate<ItemStack> predicate) {
            return CuriosApi.getCuriosInventory(entity).resolve()
                    .flatMap(inventory -> inventory.findFirstCurio(predicate)).isPresent();
        }

        static @Nullable ItemStack findFirst(LivingEntity entity, Predicate<ItemStack> predicate) {
            return CuriosApi.getCuriosInventory(entity).resolve()
                    .flatMap(inventory -> inventory.findFirstCurio(predicate)).map(SlotResult::stack).orElse(null);
        }
    }
}
