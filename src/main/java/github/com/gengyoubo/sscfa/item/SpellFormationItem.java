package github.com.gengyoubo.sscfa.item;

import github.com.gengyoubo.sscfa.SscAddon;
import github.com.gengyoubo.sscfa.spell.*;
import github.com.gengyoubo.sscfa.spell.research.*;
// [forge-port] removed yarn wildcard import net.minecraft.item.*
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import java.util.List;

public final class SpellFormationItem extends Item {
    public SpellFormationItem(Properties settings){super(settings);}
    public static ItemStack create(WorldRuneState world,String spell,int level,int[] slots){
        ItemStack stack=new ItemStack(SscAddon.SPELL_FORMATION);var data=stack.getOrCreateTag();
        data.putUUID("World",world.worldId());data.putInt("Version",SlottedFormation.VERSION);
        data.putString("Spell",spell);data.putInt("Level",level);data.putIntArray("Slots",slots);return stack;
    }
    public static ItemStack createEnhanced(WorldRuneState world,int level,int[] slots,RuneBuildEvaluator.Result result){
        if (!result.valid()) throw new IllegalArgumentException("Invalid rune scheme");
        if (!RuneLayout.hasEnhancements(level, slots, RuneLayout.VERSION))
            return ScrollData.create(result.recipe().spell(), level);
        ItemStack stack=create(world,result.recipe().spell(),level,slots);
        stack.getOrCreateTag().putInt("Version",RuneLayout.VERSION);
        stack.getOrCreateTag().put(RuneScheme.KEY,RuneScheme.create(world,level,slots,result));return stack;
    }
    @Override public Component getName(ItemStack stack){
        var data=stack.getTag();Spell spell=data==null?null:SpellRegistry.get(data.getString("Spell"));
        if (spell != null && !RuneScheme.isModified(stack)) return SscAddon.MAGIC_SCROLL.getName(stack);
        return spell==null?super.getName(stack):Component.translatable("research.ssc_addon.slotted.product",Component.translatable(spell.getNameKey()),data.getInt("Level"))
                .append(Component.translatable("research.ssc_addon.runes.modified_suffix"))
                .withStyle(spell.getRarity(ScrollData.getLevel(stack)).color);
    }
    @Override public void appendHoverText(ItemStack stack,Level world,List<Component> lines,TooltipFlag context){
        if (!RuneScheme.isModified(stack)) {
            SscAddon.MAGIC_SCROLL.appendHoverText(stack, world, lines, context);
            return;
        }
        RuneTooltips.append(stack,lines);
    }
}
