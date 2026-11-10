package github.com.gengyoubo.sscfa.spell.research;

import github.com.gengyoubo.sscfa.item.*;
import github.com.gengyoubo.sscfa.block.SpellResearchTableBlockEntity;
import github.com.gengyoubo.sscfa.spell.*;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.onixary.shapeShifterCurseForge.registry.ModItems;
import java.util.*;
import java.util.function.Predicate;

public final class SlottedFormationTransaction {
    private record Take(Container inventory,int slot,int count){}
    public record Result(String key,int school) { public boolean success(){return key.equals("completed");} }
    private SlottedFormationTransaction(){}
    public static Result complete(Container table,Container backpack,FormationKnowledgeComponent knowledge,WorldRuneState world,
                                  int level,int[] slots,UUID operation){
        return complete(table,backpack,knowledge,world,level,slots,operation,false);
    }
    public static Result completeEnhanced(Container table,Container backpack,FormationKnowledgeComponent knowledge,WorldRuneState world,
                                  int level,int[] slots,UUID operation){return complete(table,backpack,knowledge,world,level,slots,operation,true);}
    private static Result complete(Container table,Container backpack,FormationKnowledgeComponent knowledge,WorldRuneState world,
                                  int level,int[] slots,UUID operation,boolean enhanced){
        var progress=knowledge.research();
        if(enhanced&&!world.worldId().equals(progress.world))return new Result("invalid",-1);
        if(!progress.completionToken.equals(operation))return new Result("duplicate",-1);
        var evaluation=enhanced?RuneBuildEvaluator.evaluate(level,slots,world.language(),SlottedSpellRecipes.all()):null;
        var recipe=enhanced?(evaluation.valid()?evaluation.recipe():null):SlottedSpellRecipes.identify(level,slots,world.language());
        if(recipe==null)return new Result("invalid",-1);
        if(progress.rank<level)return new Result("rank",-1);
        if(!table.getItem(SpellResearchTableBlockEntity.SLOT_RESEARCH_OUTPUT).isEmpty())return new Result("output",-1);
        List<Take> plan=new ArrayList<>();
        if(!collect(table,backpack,0,stack->stack.getItem() instanceof BlankFormationPaperItem,1,plan))return new Result("paper",-1);
        int[] inks=SlottedFormation.ink(world.language(),slots);
        for(int school=0;school<8;school++){
            final int expected=school;
            if(!collect(table,backpack,1,stack->stack.getItem() instanceof FormationInkItem ink&&school(ink)==expected,inks[school],plan))return new Result("ink",school);
        }
        if(!collect(table,backpack,2,stack->stack.is(github.com.gengyoubo.sscfa.compat.RegCustomItem.UNTREATED_MOONDUST),(int)Arrays.stream(slots).filter(g->g>=0).count(),plan))return new Result("dust",-1);
        ItemStack result=enhanced?SpellFormationItem.createEnhanced(world,level,slots,evaluation):SpellFormationItem.create(world,recipe.spell(),level,slots);
        for(Take take:plan)take.inventory.getItem(take.slot).shrink(take.count);
        table.setItem(SpellResearchTableBlockEntity.SLOT_RESEARCH_OUTPUT,result);table.setChanged();backpack.setChanged();
        knowledge.recordSpell(recipe.spell());String key=recipe.spell()+":"+level;
        if(enhanced){progress.runeReferences.put(key,slots.clone());progress.runeCompleted.add(key);}
        else{progress.slotReferences.put(key,slots.clone());progress.slotCompleted.add(key);}progress.completionToken=UUID.randomUUID();
        return new Result("completed",recipe.family());
    }
    public static int school(FormationInkItem ink){return ink.getType().element==null?7:ResearchTarget.FAMILIES.indexOf(ink.getType().element.id);}
    /** Manual foundation validation retains the existing main-scroll and upgrade-material costs. */
    public static Result upgradeEnhanced(Container table,Container backpack,FormationKnowledgeComponent knowledge,WorldRuneState world,
                                         int level,int[] slots,UUID operation) {
        ResearchProgress progress=knowledge.research();
        if(!world.worldId().equals(progress.world))return new Result("invalid",-1);
        if(!progress.completionToken.equals(operation))return new Result("duplicate",-1);
        var evaluation=RuneBuildEvaluator.evaluate(level,slots,world.language(),SlottedSpellRecipes.all());
        if(!evaluation.valid())return new Result("invalid",-1);
        var recipe=evaluation.recipe();
        if(progress.rank<level)return new Result("rank",-1);
        if(!knowledge.hasSpell(recipe.spell()))return new Result("upgrade_unknown",-1);
        if(RuneLayout.hasEnhancements(level,slots,RuneLayout.VERSION))return new Result("upgrade_foundation",-1);
        if(!table.getItem(SpellResearchTableBlockEntity.SLOT_RESEARCH_OUTPUT).isEmpty())return new Result("output",-1);
        ItemStack main=table.getItem(SpellResearchTableBlockEntity.SLOT_UPGRADE_SCROLL);
        Spell spell=ScrollData.getSpell(main);
        if(level<2||spell==null||!spell.getId().getPath().equals(recipe.spell())
                ||ScrollData.getLevel(main)!=level-1||main.getCount()!=1||ArcaneAnalysis.isUnanalyzed(main))
            return new Result("upgrade_input",-1);
        if(main.getTag()!=null&&main.getTag().contains(RuneScheme.KEY)&&RuneScheme.authoritativeScroll(world,main)==null)
            return new Result("invalid",-1);
        List<Take> plan=new ArrayList<>();
        if(!collect(table,backpack,SpellResearchTableBlockEntity.SLOT_INK,
                stack->stack.getItem() instanceof FormationInkItem ink&&school(ink)==recipe.family(),
                ScrollWorkshopManager.upgradeInkCost(level),plan))return new Result("ink",recipe.family());
        if(!collect(table,backpack,SpellResearchTableBlockEntity.SLOT_CATALYST,
                stack->stack.is(github.com.gengyoubo.sscfa.compat.RegCustomItem.MOONDUST_CRYSTAL_SHARD),
                ScrollWorkshopManager.upgradeCatalystCost(level),plan))return new Result("upgrade_catalyst",-1);
        ItemStack result=main.copy();
        ScrollData.setLevel(result,level);
        ScrollData.setCastLevel(result,0);
        for(Take take:plan)take.inventory.getItem(take.slot).shrink(take.count);
        table.setItem(SpellResearchTableBlockEntity.SLOT_UPGRADE_SCROLL,ItemStack.EMPTY);
        table.setItem(SpellResearchTableBlockEntity.SLOT_RESEARCH_OUTPUT,result);
        progress.runeReferences.put(recipe.spell()+":"+level,slots.clone());
        progress.runeCompleted.add(recipe.spell()+":"+level);
        progress.completionToken=UUID.randomUUID();
        table.setChanged();backpack.setChanged();
        return new Result("completed",recipe.family());
    }
    public static int count(Container table,Container backpack,int tableSlot,Predicate<ItemStack> predicate){
        int total=predicate.test(table.getItem(tableSlot))?table.getItem(tableSlot).getCount():0;
        for(int slot=0;slot<Math.min(36,backpack.getContainerSize());slot++)if(predicate.test(backpack.getItem(slot)))total+=backpack.getItem(slot).getCount();
        return total;
    }
    private static boolean collect(Container table,Container backpack,int tableSlot,Predicate<ItemStack> predicate,int needed,List<Take> plan){
        if(needed==0)return true;
        ItemStack stored=table.getItem(tableSlot);
        if(predicate.test(stored)){int amount=Math.min(needed,stored.getCount());plan.add(new Take(table,tableSlot,amount));needed-=amount;}
        for(int slot=0;slot<Math.min(36,backpack.getContainerSize())&&needed>0;slot++){
            ItemStack stack=backpack.getItem(slot);if(!predicate.test(stack))continue;
            int amount=Math.min(needed,stack.getCount());plan.add(new Take(backpack,slot,amount));needed-=amount;
        }
        return needed==0;
    }
}
