package github.com.gengyoubo.sscfa.spell.research;

import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class RuneTooltips {
    private RuneTooltips() {}
    public static Component stat(RuneModifiers.Stat stat,int value){
        String signed=value>0?"+"+value:Integer.toString(value);
        return Component.translatable("research.ssc_addon.runes.stat."+stat.name().toLowerCase(Locale.ROOT),signed);
    }
    public static void append(ItemStack stack,List<Component> lines){
        if(!RuneScheme.isModified(stack))return;
        var n=stack.getTag().getCompound(RuneScheme.KEY);
        lines.add(Component.translatable("research.ssc_addon.runes.profile_level",n.getInt("Level")).withStyle(ChatFormatting.GOLD));
        lines.add(Component.translatable("research.ssc_addon.runes.stability",n.getInt("Stability")).withStyle(ChatFormatting.GRAY));
        var modifiers=RuneModifiers.read(n.getCompound("Modifiers"));
        if(n.contains("Summary"))lines.add(Component.translatable("research.ssc_addon.runes.summary.raw").withStyle(ChatFormatting.DARK_GRAY));
        for(var stat:RuneModifiers.Stat.values())if(modifiers.get(stat)!=0)
            lines.add(stat(stat,modifiers.get(stat)).copy().withStyle(ChatFormatting.GRAY));
        if(n.contains("Summary"))lines.addAll(RuneSummary.effects(n.getCompound("Summary"),false));
        lines.add(Component.translatable("research.ssc_addon.runes.lower_level_tip").withStyle(ChatFormatting.DARK_GRAY));
    }
}
