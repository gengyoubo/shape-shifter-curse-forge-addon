package github.com.gengyoubo.sscfa.enchantment;

import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import github.com.gengyoubo.sscfa.SscAddonForge;

/** 附属附魔注册。 */
public final class RegAddonEnchantments {
    private RegAddonEnchantments() {}
    public static final DeferredRegister<Enchantment> ENCHANTMENTS = DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, SscAddonForge.MOD_ID);
    public static final RegistryObject<Enchantment> SPELL_RESISTANCE = ENCHANTMENTS.register("spell_resistance", SpellResistanceEnchantment::new);
}
