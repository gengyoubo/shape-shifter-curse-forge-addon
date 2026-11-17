package github.com.gengyoubo.sscfa.loot;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import github.com.gengyoubo.sscfa.SscAddonForge;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/** Kept registered for old loot tables; natural scrolls no longer gain rune enhancements. */
public final class RandomRuneLootFunction extends LootItemConditionalFunction {
    public static final DeferredRegister<LootItemFunctionType> FUNCTIONS =
            DeferredRegister.create(Registries.LOOT_FUNCTION_TYPE, SscAddonForge.MOD_ID);
    private static final RegistryObject<LootItemFunctionType> TYPE = FUNCTIONS.register("random_rune_formation",
            () -> new LootItemFunctionType(new Serializer()));

    private RandomRuneLootFunction(LootItemCondition[] conditions) { super(conditions); }

    /** Registration now happens through {@link #FUNCTIONS}; kept for the Fabric call site. */
    public static void register() {}

    public static LootItemConditionalFunction.Builder<?> builder() { return simpleBuilder(RandomRuneLootFunction::new); }

    @Override public LootItemFunctionType getType() { return TYPE.get(); }

    @Override protected ItemStack run(ItemStack stack, LootContext context) {
        return stack;
    }

    public static final class Serializer extends LootItemConditionalFunction.Serializer<RandomRuneLootFunction> {
        @Override public RandomRuneLootFunction deserialize(JsonObject json, JsonDeserializationContext context,
                                                         LootItemCondition[] conditions) {
            return new RandomRuneLootFunction(conditions);
        }
    }
}
