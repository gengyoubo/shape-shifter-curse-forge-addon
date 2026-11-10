package github.com.gengyoubo.sscfa.spell;

import github.com.gengyoubo.sscfa.block.SpellResearchTableBlockEntity;
import github.com.gengyoubo.sscfa.item.BlankFormationPaperItem;
import github.com.gengyoubo.sscfa.item.FormationInkItem;
import github.com.gengyoubo.sscfa.screen.SpellResearchTableScreenHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.onixary.shapeShifterCurseForge.registry.ModItems;

public final class ResearchTableManager {
    private ResearchTableManager() {}

    public static void scribe(ServerPlayer player,String elementId,String variant,int level){
        execute(player,elementId,variant,level,true,0);
    }

    public static void learn(ServerPlayer player,String elementId,String variant,int level,int discount){
        execute(player,elementId,variant,level,false,discount);
    }

    private static void execute(ServerPlayer player,String elementId,String variant,int level,boolean scribing,int discount){
        if(!(player.containerMenu instanceof SpellResearchTableScreenHandler handler)||!handler.stillValid(player)
                ||!(handler.getInventory() instanceof SpellResearchTableBlockEntity table))return;
        FormationElement element=FormationElement.byId(elementId);
        if(element==null||level<1||level>FormationData.MAX_FORMATION_LEVEL)return;
        String result=transact(table,FormationKnowledgeComponent.get(player),elementId,variant,level,scribing,discount);
        if(!result.equals("ok")){
            if(result.equals("no_ink"))error(player,result,Component.translatable(element.getNameKey()),level);
            else if(result.equals("no_dust"))error(player,result,learningCost(level,discount));
            else error(player,result);
            return;
        }
        if(!scribing)FormationKnowledgeComponent.sync(player);
        handler.broadcastChanges();
        player.level().playSound(null,table.getBlockPos(),scribing?SoundEvents.BOOK_PAGE_TURN:SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,SoundSource.BLOCKS,.5f,1.2f);
        player.displayClientMessage(Component.translatable("message.ssc_addon.research."+(scribing?"scribed":"learned"),Component.translatable(element.getNameKey()),level).withStyle(ChatFormatting.GREEN),true);
    }

    public static String transact(Container table,FormationKnowledgeComponent knowledge,String elementId,String variant,int level,boolean scribing,int discount){
        FormationElement element=FormationElement.byId(elementId);
        if(element==null||level<1||level>FormationData.MAX_FORMATION_LEVEL)return "not_recorded";
        String normalized=FormationData.normalizeVariant(variant);
        if(element==FormationElement.UNIVERSAL&&normalized==null)normalized=FormationData.VARIANT_REGEN;
        if(!scribing){
            if(!knowledge.hasRecorded(element,normalized,level))return "not_recorded";
            if(knowledge.hasLearned(element,normalized,level))return "already_learned";
            int cost=learningCost(level,discount);ItemStack dust=table.getItem(SpellResearchTableBlockEntity.SLOT_MOONDUST);
            if(!dust.is(github.com.gengyoubo.sscfa.compat.RegCustomItem.UNTREATED_MOONDUST)||dust.getCount()<cost)return "no_dust";
            dust.shrink(cost);knowledge.learn(element,normalized,level);table.setChanged();return "ok";
        }
        if(!knowledge.hasLearned(element,normalized,level))return "not_learned";
        if(!(table.getItem(SpellResearchTableBlockEntity.SLOT_PAPER).getItem() instanceof BlankFormationPaperItem))return "no_paper";
        ItemStack ink=table.getItem(SpellResearchTableBlockEntity.SLOT_INK);
        if(!(ink.getItem() instanceof FormationInkItem inkItem)||ink.getCount()<level
                ||(element==FormationElement.UNIVERSAL?inkItem.getType()!=FormationInkItem.Type.NORMAL:inkItem.getType().element!=element)){
            return "no_ink";
        }
        if(!table.getItem(SpellResearchTableBlockEntity.SLOT_OUTPUT).isEmpty())return "output_full";
        table.getItem(SpellResearchTableBlockEntity.SLOT_PAPER).shrink(1);ink.shrink(level);
        table.setItem(SpellResearchTableBlockEntity.SLOT_OUTPUT,FormationData.create(element,level,element==FormationElement.UNIVERSAL?normalized:null));
        table.setChanged();return "ok";
    }

    private static int learningCost(int level,int discount){return Math.max(1,level*2-level*2*Math.max(0,Math.min(40,discount))/100);}

    private static void error(ServerPlayer player,String key,Object...args){
        player.displayClientMessage(Component.translatable("message.ssc_addon.research."+key,args).withStyle(ChatFormatting.RED),true);
    }
}