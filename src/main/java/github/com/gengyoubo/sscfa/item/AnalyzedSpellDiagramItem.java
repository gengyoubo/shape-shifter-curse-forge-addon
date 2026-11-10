package github.com.gengyoubo.sscfa.item;

import github.com.gengyoubo.sscfa.SscAddon;
import github.com.gengyoubo.sscfa.spell.*;
import github.com.gengyoubo.sscfa.spell.research.*;
import net.minecraft.ChatFormatting;
// [forge-port] removed yarn wildcard import net.minecraft.item.*
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import java.util.*;

public final class AnalyzedSpellDiagramItem extends Item {
    public AnalyzedSpellDiagramItem(Properties settings){super(settings);}
    public static ItemStack create(WorldRuneState world,String spell,int level){
        var recipe=SlottedSpellRecipes.get(spell);
        if(!SlottedSpellRecipes.available(recipe,level))return ItemStack.EMPTY;
        ItemStack stack=new ItemStack(SscAddon.ANALYZED_SPELL_DIAGRAM);var data=stack.getOrCreateTag();
        data.putUUID("World",world.worldId());data.putInt("Version",RuneLayout.VERSION);
        data.putString("Spell",spell);data.putInt("Level",level);data.putInt("CustomModelData",level);
        data.putIntArray("Slots",RuneLayout.foundation(recipe,level,world.language()));return stack;
    }
    public static ItemStack create(WorldRuneState world,ItemStack scroll){
        Spell spell=ScrollData.getSpell(scroll);
        if(spell==null)return ItemStack.EMPTY;
        var data=scroll.getTag();
        if(data==null||!data.contains(RuneScheme.KEY))return create(world,spell.getId().getPath(),ScrollData.getLevel(scroll));
        var mirror=data.getCompound(RuneScheme.KEY);
        var saved=RuneScheme.authoritativeScroll(world,scroll);
        if(saved==null)return ItemStack.EMPTY;
        if (!RuneScheme.hasEnhancements(saved)) return create(world, spell.getId().getPath(), ScrollData.getLevel(scroll));
        ItemStack diagram=create(world,spell.getId().getPath(),saved.getInt("Level"));
        if(diagram.isEmpty())return diagram;
        diagram.getOrCreateTag().putIntArray("Slots",saved.getIntArray("Slots"));
        diagram.getOrCreateTag().putInt("Version",saved.getInt("Version"));
        diagram.getOrCreateTag().put(RuneScheme.KEY,mirror.copy());
        return diagram;
    }
    /** Legacy outer glyphs described the old grammar and must not become modifier references. */
    public static int[] referenceSlots(CompoundTag data){
        int[] slots=data.getIntArray("Slots");
        int level=data.getInt("Level"),version=data.getInt("Version");
        if(version==SlottedFormation.VERSION||!data.contains(RuneScheme.KEY))
            for(int i=RuneLayout.baseSize(level,version==1?RuneLayout.PREVIOUS_VERSION:version);i<slots.length;i++)slots[i]=-1;
        return RuneLayout.migrateDraft(level,slots,version);
    }
    public static boolean valid(ItemStack stack,UUID world){
        CompoundTag data=stack.getTag();
        return stack.is(SscAddon.ANALYZED_SPELL_DIAGRAM)&&data!=null&&data.hasUUID("World")
                &&data.getUUID("World").equals(world)
                &&RuneLayout.validDraft(data.getInt("Level"),data.getIntArray("Slots"),data.getInt("Version"))
                &&Arrays.stream(Arrays.copyOf(data.getIntArray("Slots"),data.getInt("Version")==1?SlottedFormation.size(data.getInt("Level")):RuneLayout.baseSize(data.getInt("Level"),data.getInt("Version")))).allMatch(glyph->glyph>=0)
                &&SlottedSpellRecipes.available(SlottedSpellRecipes.get(data.getString("Spell")),data.getInt("Level"));
    }
    @Override public Component getName(ItemStack stack){
        var data=stack.getTag();Spell spell=data==null?null:SpellRegistry.get(data.getString("Spell"));
        if(spell==null)return super.getName(stack);
        var element=spell.getElement();
        return (element==null?Component.translatable("research.ssc_addon.slotted.diagram_format",Component.translatable(spell.getNameKey()))
            :Component.translatable("research.ssc_addon.slotted.diagram_format_element",Component.translatable(spell.getNameKey()),Component.translatable(element.getNameKey())))
            .withStyle(spell.getRarity(Math.max(1,Math.min(5,data.getInt("Level")))).color);
    }
    @Override public Optional<TooltipComponent> getTooltipImage(ItemStack stack){
        var data=stack.getTag();Spell spell=data==null?null:SpellRegistry.get(data.getString("Spell"));
        return spell==null?Optional.empty():Optional.of(new MagicScrollItem.SpellIconTooltipData(spell.getIconTexture()));
    }
    @Override public void appendHoverText(ItemStack stack,Level world,List<Component> lines,TooltipFlag context){
        var data=stack.getTag();Spell spell=data==null?null:SpellRegistry.get(data.getString("Spell"));
        if(spell!=null){
            SpellRarity rarity=spell.getRarity(Math.max(1,Math.min(5,data.getInt("Level"))));
            lines.add(Component.translatable("item.ssc_addon.magic_scroll.rarity_tier",Component.translatable(rarity.getTranslationKey()).withStyle(rarity.color),
                    Component.translatable("spell.ssc_addon.tier."+spell.getConfig().spellTier.name().toLowerCase(Locale.ROOT)).withStyle(ChatFormatting.GRAY)));
            for(String line:Component.translatable(spell.getDescKey()).getString().split("\n"))lines.add(Component.literal(line).withStyle(ChatFormatting.GRAY));
        }
        for(String line:Component.translatable("research.ssc_addon.slotted.diagram_tip").getString().split("\n"))lines.add(Component.literal(line).withStyle(ChatFormatting.DARK_GRAY));
        RuneTooltips.append(stack,lines);
    }
}
