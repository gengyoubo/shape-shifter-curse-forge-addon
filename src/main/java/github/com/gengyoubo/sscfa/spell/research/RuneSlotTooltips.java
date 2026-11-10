package github.com.gengyoubo.sscfa.spell.research;

import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;

import static github.com.gengyoubo.sscfa.spell.research.RuneModifiers.Stat.*;

/** Slot descriptions consume the current server evaluation, never a second client rule calculation. */
public final class RuneSlotTooltips {
    private RuneSlotTooltips() {}
    private static Component text(String key,Object... args){
        return Component.translatable("research.ssc_addon.runes."+key,args);
    }
    public static CompoundTag write(RuneBuildEvaluator.Result result){
        CompoundTag data=new CompoundTag(),slots=new CompoundTag();
        result.contributions().forEach((index,contribution)->{
            CompoundTag slot=new CompoundTag();slot.put("Modifiers",contribution.modifiers().write());
            slot.putInt("Stability",contribution.stability());slot.putString("InactiveReason",contribution.inactiveReason());
            slots.put(Integer.toString(index),slot);
        });
        data.put("Slots",slots);ListTag hints=new ListTag();
        for(var problem:result.problems())addHint(hints,problem,false);
        for(var interaction:result.interactions())addHint(hints,interaction,true);
        data.put("Hints",hints);return data;
    }
    private static void addHint(ListTag hints,RuneBuildEvaluator.Problem problem,boolean interaction){
        if(problem.first()<0)return;
        CompoundTag hint=new CompoundTag();hint.putString("Rule",problem.rule());hint.putInt("First",problem.first());
        hint.putInt("Second",problem.second());hint.putInt("Amount",problem.amount());hint.putBoolean("Interaction",interaction);hints.add(hint);
    }
    public static List<Component> slot(int level,int index,int[] slots,int[] meanings,CompoundTag evaluation,boolean current){
        List<Component> lines=new ArrayList<>();boolean enhanced=RuneLayout.enhancement(level,index);
        int glyph=slots[index],meaning=glyph>=0?meanings[glyph]:-1;
        if(glyph>=0)lines.add((meaning<0?SlottedResearchManager.text("unknown",glyph+1)
                :SlottedResearchManager.text("role."+meaning)).copy().withStyle(ChatFormatting.WHITE));
        lines.add(text(enhanced?"slot_enhanced":"slot_base",index+1).copy().withStyle(enhanced?ChatFormatting.AQUA:ChatFormatting.GOLD));
        if(glyph<0){
            lines.add(text(enhanced?"slot_empty_outer":"slot_empty_base").copy().withStyle(ChatFormatting.GRAY));
            return lines;
        }
        if(!enhanced){lines.add(text("slot_foundation_tip").copy().withStyle(ChatFormatting.GRAY));return lines;}
        if(meaning<0){lines.add(text("slot_unknown_tip").copy().withStyle(ChatFormatting.GRAY));return lines;}
        var role=RuneRole.values()[meaning];
        lines.add(text("effect."+role.name().toLowerCase(Locale.ROOT)).copy().withStyle(ChatFormatting.GRAY));
        var entries=evaluation.getCompound("Slots");String key=Integer.toString(index);
        if(!current||!entries.contains(key,Tag.TAG_COMPOUND)){
            lines.add(text("slot_pending").copy().withStyle(ChatFormatting.DARK_GRAY));return lines;
        }
        var data=entries.getCompound(key);var modifiers=RuneModifiers.read(data.getCompound("Modifiers"));
        String reason=data.getString("InactiveReason");
        if(!reason.isEmpty())lines.add(text("inactive."+reason).copy().withStyle(ChatFormatting.YELLOW));
        else lines.add(text("slot_contribution").copy().withStyle(ChatFormatting.DARK_GRAY));
        for(var stat:RuneModifiers.Stat.values()){
            if(stat==MANA||stat==TIME||stat==CD||stat==FLAT_MANA||modifiers.get(stat)==0)continue;
            lines.add(RuneTooltips.stat(stat,modifiers.get(stat)).copy().withStyle(modifiers.get(stat)>0?ChatFormatting.GREEN:ChatFormatting.RED));
        }
        int stability=data.getInt("Stability");
        lines.add(text("slot_stability",signed(stability)).copy().withStyle(stability>0?ChatFormatting.GREEN:ChatFormatting.GRAY));
        int mana=modifiers.get(MANA),flat=modifiers.get(FLAT_MANA);
        lines.add(text(mana==0?"slot_mana_flat":"slot_mana",mana==0?signed(flat):signed(mana),flat).copy().withStyle(ChatFormatting.GRAY));
        int time=modifiers.get(TIME);
        if(time!=0)lines.add(RuneTooltips.stat(TIME,time).copy().withStyle(time<0?ChatFormatting.GREEN:ChatFormatting.GRAY));
        for(var hint:evaluation.getList("Hints",Tag.TAG_COMPOUND)){
            CompoundTag problem=(CompoundTag)hint;
            int first=problem.getInt("First"),second=problem.getInt("Second");
            if(first!=index&&second!=index)continue;
            if(problem.getBoolean("Interaction")){
                lines.add(text("pair."+problem.getString("Rule"),(first==index?second:first)+1,problem.getInt("Amount")).copy().withStyle(ChatFormatting.YELLOW));
            }else{
                lines.add(SlottedResearchManager.text("rune_"+problem.getString("Rule"),first+1,second+1,problem.getInt("Amount")).copy().withStyle(ChatFormatting.RED));
            }
        }
        return lines;
    }
    private static String signed(int value){return value>0?"+"+value:Integer.toString(value);}
}
