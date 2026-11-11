package github.com.gengyoubo.sscfa.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import github.com.gengyoubo.sscfa.SscAddon;
import github.com.gengyoubo.sscfa.block.SpellResearchTableBlockEntity;
import github.com.gengyoubo.sscfa.client.tutorial.SpellTutorialClient;
import github.com.gengyoubo.sscfa.compat.RegCustomItem;
import github.com.gengyoubo.sscfa.item.AnalyzedSpellDiagramItem;
import github.com.gengyoubo.sscfa.item.BlankFormationPaperItem;
import github.com.gengyoubo.sscfa.item.FormationInkItem;
import github.com.gengyoubo.sscfa.network.AddonNetwork;
import github.com.gengyoubo.sscfa.screen.SpellResearchTableScreenHandler;
import github.com.gengyoubo.sscfa.spell.FormationElement;
import github.com.gengyoubo.sscfa.spell.ScrollData;
import github.com.gengyoubo.sscfa.spell.ScrollWorkshopManager;
import github.com.gengyoubo.sscfa.spell.Spell;
import github.com.gengyoubo.sscfa.spell.SpellRegistry;
import github.com.gengyoubo.sscfa.spell.research.FormationDiagnosis;
import github.com.gengyoubo.sscfa.spell.research.FormationDiagram;
import github.com.gengyoubo.sscfa.spell.research.FormationResearchNetworking;
import github.com.gengyoubo.sscfa.spell.research.ResearchProgress;
import github.com.gengyoubo.sscfa.spell.research.ResearchTarget;
import github.com.gengyoubo.sscfa.spell.research.RuneLayout;
import github.com.gengyoubo.sscfa.spell.research.RuneSlotTooltips;
import github.com.gengyoubo.sscfa.spell.research.RuneSummary;
import github.com.gengyoubo.sscfa.spell.research.SlottedFormationTransaction;
import github.com.gengyoubo.sscfa.spell.research.SlottedResearchManager;
import github.com.gengyoubo.sscfa.spell.research.SlottedSpellRecipes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class SlottedFormationScreen extends AbstractContainerScreen<SpellResearchTableScreenHandler> {
    private static final int PALETTE_LEFT=8,PALETTE_TOP=26,PALETTE_CELL=22,PALETTE_HEIGHT=118;
    private static final int PALETTE_TRACK_X=53,PALETTE_TRACK_WIDTH=4;
    private static final int PALETTE_ROWS=(18+1)/2,PALETTE_MAX_SCROLL=PALETTE_ROWS*PALETTE_CELL-PALETTE_HEIGHT;
    private static final int PALETTE_THUMB_HEIGHT=PALETTE_HEIGHT*PALETTE_HEIGHT/(PALETTE_ROWS*PALETTE_CELL);
    private static final int REFERENCE_COLUMNS=4,REFERENCE_CAPACITY=15,REFERENCE_LEFT=64,REFERENCE_TOP=94;
    private final SpellResearchTableScreen parent;
    private final Inventory inventory;
    private final List<Button> pageButtons=new ArrayList<>();
    private final List<Button> studyButtons=new ArrayList<>();
    private final Map<Integer,int[]> drafts=new HashMap<>();
    private final Map<String,int[]> unlockedReferences=new HashMap<>();
    private String requestedProjection,projectedSpell;
    private boolean upgrading;
    private Button modeButton;
    private final BitSet touched=new BitSet();
    private final FeedbackState feedbackState=new FeedbackState();
    private ResearchProgress progress=new ResearchProgress();
    private int level=1,selectedGlyph,revision,paletteScroll,notesScroll,referenceScroll,summaryScroll;
    private int[] slots=RuneLayout.empty(1),meanings=new int[18],schools=new int[18],reference=new int[0];
    private int[] diagnosis=new int[0];
    private int stability=100;private boolean runeValid;private int evaluatedRevision=-1;
    private int testedRevision=-1,failedRevision=-1;
    private Tooltip testTip,retestTip,finishTip,untestedTip;
    private CompoundTag runeTooltipState=new CompoundTag();
    private CompoundTag runeSummary=new CompoundTag();
    private final BitSet problems=new BitSet(),inactive=new BitSet(),synergy=new BitSet(),suppressed=new BitSet();
    private final List<Integer> referenceInventorySlots=new ArrayList<>();
    private int selectedReferenceSlot=-1;
    private List<FormationDiagram.Point> positions=RuneLayout.positions(1);
    private UUID world,operation;
    private boolean notes,summary,referenceMenu,referenceVisible,handoff,busy,dragging,erasing;
    private boolean dirty,loading,paletteDragging,paletteScrolling;
    private double paletteGrabOffset;
    private long sentAt,animation,editedAt;
    private Component feedback=text("ready");

    public SlottedFormationScreen(SpellResearchTableScreenHandler handler,Inventory inventory,SpellResearchTableScreen parent){
        super(handler,inventory,text("title"));this.parent=parent;this.inventory=inventory;
        this.imageWidth=312;this.imageHeight=212;Arrays.fill(meanings,-1);Arrays.fill(schools,7);
    }
    public SpellResearchTableScreenHandler getScreenHandler(){return this.menu;}
    public void projectUnlocked(String spell,int selectedLevel){
        requestedProjection=spell;level=Math.max(1,Math.min(5,selectedLevel));slots=RuneLayout.empty(level);
    }
    private static ResourceLocation image(String name){return new ResourceLocation("ssc_addon","textures/gui/formation_research/"+name+".png");}
    private static Component text(String key,Object...args){return SlottedResearchManager.text(key,args);}
    @Override protected void init(){
        super.init();this.topPos=(this.height-this.imageHeight-28)/2+28;handoff=false;pageButtons.clear();studyButtons.clear();dragging=false;paletteScrolling=false;paletteDragging=false;
        this.menu.setActivePage(ResearchTableTabs.RESEARCH);
        this.menu.setRuneUpgrade(upgrading);
        if(this.minecraft.gameMode!=null)this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId,ResearchTableTabs.RESEARCH);
        syncMode();
        positions=RuneLayout.positions(level);
        for(int index=0;index<ResearchTableTabs.COUNT;index++){final int tab=index;this.addRenderableWidget(ResearchTableTabs.create(this.leftPos,this.topPos,index,()->tab==ResearchTableTabs.RESEARCH,()->{if(tab!=ResearchTableTabs.RESEARCH)back(tab);}));}
        this.addRenderableWidget(ResearchTableTabs.tutorial(this.leftPos,this.topPos,()->{save();handoff=true;SpellTutorialClient.replay();}));
        for(int rank=1;rank<=5;rank++){final int tier=rank;pageButtons.add(button(244+(rank-1)*12,18,12,18,Component.literal(String.valueOf(rank)),()->switchLevel(tier)));}
        var notesButton=button(244,44,29,18,Component.translatable("research.ssc_addon.runes.summary.notes"),()->{
            notes=!notes;summary=false;referenceMenu=false;notesScroll=0;visibility();
        });
        notesButton.setTooltip(Tooltip.create(text("notes")));pageButtons.add(notesButton);
        studyButtons.add(button(244,86,60,18,text("identify"),()->send(SlottedResearchManager.INSPECT)));
        studyButtons.add(button(244,108,60,18,text("train"),()->send(SlottedResearchManager.TRAIN)));
        pageButtons.add(button(244,152,60,18,text("test"),()->send(SlottedResearchManager.TEST)));
        pageButtons.add(button(244,174,60,18,text("finish"),()->send(SlottedResearchManager.COMPLETE)));
        pageButtons.add(icon(20,153,false,"reference",()->{
            if(referenceVisible){referenceVisible=false;}
            referenceMenu=!referenceMenu;referenceScroll=0;refreshReferenceList();
        }));
        pageButtons.add(icon(20,176,true,"clear",this::clearCurrent));
        pageButtons.add(button(275,44,29,18,Component.translatable("research.ssc_addon.runes.summary.button"),()->{
            summary=!summary;notes=false;referenceMenu=false;summaryScroll=0;visibility();
            if(summary&&world!=null&&!loading){evaluatedRevision=-1;send(SlottedResearchManager.SAVE);}
        }));
        modeButton=button(244,2,60,14,text("make_mode"),()->{
            upgrading=!upgrading;syncMode();refreshMode();
        });
        refreshMode();
        visibility();
        if(world==null)send(SlottedResearchManager.OPEN);
    }
    private void syncMode(){
        this.menu.setRuneUpgrade(upgrading);
        if(this.minecraft.gameMode!=null)this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId,
                upgrading?SpellResearchTableScreenHandler.UPGRADE_ON:SpellResearchTableScreenHandler.UPGRADE_OFF);
    }
    private void refreshMode(){
        if(modeButton!=null){modeButton.setMessage(text(upgrading?"upgrade_mode":"make_mode"));
            modeButton.setTooltip(Tooltip.create(text(upgrading?"upgrade_help":"make_help")));}
    }
    private Button button(int left,int top,int width,int height,Component title,Runnable action){
        var button=this.addRenderableWidget(Button.builder(Component.literal(this.font.plainSubstrByWidth(title.getString(),Math.max(1,width-4))),ignored->action.run()).bounds(this.leftPos+left,this.topPos+top,width,height).build());
        button.setTooltip(Tooltip.create(title));return button;
    }
    private Button icon(int left,int top,boolean clearButton,String key,Runnable action){
        return this.addRenderableWidget(new Button(this.leftPos+left,this.topPos+top,20,20,text(key),ignored->action.run(),supplier->supplier.get()){
            @Override public void renderWidget(GuiGraphics context,int mouseX,int mouseY,float delta){
                if(clearButton){
                    active=!busy&&!loading&&!notes&&!summary&&Arrays.stream(slots).anyMatch(value->value>=0);
                    RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();
                    context.setColor(1,1,1,active?1f:.35f);
                    context.blit(image("widgets"),getX()+2,getY()+2,16,16,0,0,32,32,32,32);
                    context.setColor(1,1,1,1);
                }else context.renderItem(new ItemStack(Items.BUNDLE),getX()+2,getY()+2);
                if(isHovered()&&!referenceMenu)context.renderTooltip(SlottedFormationScreen.this.font,getMessage(),mouseX,mouseY);
            }
        });
    }
    private void visibility(){for(var button:studyButtons)button.visible=notes;for(int tier=0;tier<5;tier++)pageButtons.get(tier).active=tier+1!=level;}
    private void switchLevel(int next){
        if(next==level||busy)return;save();level=next;slots=drafts.getOrDefault(level,RuneLayout.empty(level)).clone();
        revision++;reference=new int[0];referenceVisible=false;selectedReferenceSlot=-1;projectedSpell=null;unlockedReferences.clear();notesScroll=0;summaryScroll=0;positions=RuneLayout.positions(level);visibility();send(SlottedResearchManager.OPEN);
    }
    private void save(){drafts.put(level,slots.clone());send(SlottedResearchManager.SAVE);}
    private void send(int action){
        if(this.minecraft==null||this.minecraft.player==null||this.minecraft.player.containerMenu!=this.menu)return;
        if(world==null)action=SlottedResearchManager.OPEN;
        if(busy&&action!=SlottedResearchManager.SAVE)return;
        CompoundTag request=new CompoundTag();request.putInt("Action",action);request.putInt("Level",level);request.putInt("Revision",revision);
        request.putInt("Version",RuneLayout.VERSION);request.putIntArray("Slots",slots);request.putInt("Glyph",selectedGlyph);
        request.putBoolean("Upgrade",upgrading);
        if(world!=null)request.putUUID("World",world);if(operation!=null)request.putUUID("Operation",operation);
        if(action!=SlottedResearchManager.SAVE)request.putUUID("Request",feedbackState.begin(action));
        var packet=AddonNetwork.buffer();packet.writeVarInt(this.menu.containerId);packet.writeNbt(request);AddonNetwork.sendToServer(FormationResearchNetworking.ACTION,packet);
        if(action==SlottedResearchManager.TEST)playSound(SoundEvents.ENCHANTMENT_TABLE_USE,.25f,.85f);
        if(action==SlottedResearchManager.SAVE)dirty=false;
        if(action==SlottedResearchManager.OPEN)loading=true;
        if(action!=SlottedResearchManager.SAVE){busy=true;sentAt=System.currentTimeMillis();}
    }
    public void accept(CompoundTag state,Component message,boolean valid){
        if(!state.hasUUID("World")||world!=null&&!world.equals(state.getUUID("World")))return;
        int action=state.getInt("Action");
        if(action!=SlottedResearchManager.SAVE&&(!state.hasUUID("Request")||!feedbackState.finish(state.getUUID("Request"),action)))return;
        world=state.getUUID("World");progress.read(state.getCompound("Progress"));
        if(state.hasUUID("Operation"))operation=state.getUUID("Operation");
        int[] received=state.getIntArray("Meanings");if(received.length==18)meanings=received;
        received=state.getIntArray("Schools");if(received.length==18)schools=received;
        if(state.getInt("Level")==level&&state.getInt("Revision")==revision&&state.contains("RuneTooltipState")){
            stability=state.getInt("Stability");runeValid=state.getBoolean("RuneValid");evaluatedRevision=revision;
            runeTooltipState=state.getCompound("RuneTooltipState").copy();
            runeSummary=state.getCompound("RuneSummary").copy();
            for(var entry:Map.of("Problems",problems,"Inactive",inactive,"Synergy",synergy,"Suppressed",suppressed).entrySet()){entry.getValue().clear();for(int p:state.getIntArray(entry.getKey()))entry.getValue().set(p);}}
        if(action==SlottedResearchManager.SAVE)return;
        busy=false;
        if(state.getBoolean("Slotted")&&(state.getInt("Level")!=level||state.getInt("Revision")!=revision))return;
        feedback=message;
        if(action==SlottedResearchManager.OPEN){
            loading=false;
            received=state.getIntArray("Slots");if(RuneLayout.validDraft(level,received))slots=received;
        }
        if(state.getInt("Level")==level&&state.contains("UnlockedReferences")){
            unlockedReferences.clear();
            var entries=state.getList("UnlockedReferences",Tag.TAG_COMPOUND);
            for(int i=0;i<Math.min(23,entries.size());i++){
                var entry=entries.getCompound(i);int[] projected=entry.getIntArray("Slots");
                if(entry.getInt("Level")==level&&SlottedSpellRecipes.get(entry.getString("Spell"))!=null&&RuneLayout.validDraft(level,projected))
                    unlockedReferences.put(entry.getString("Spell"),projected);
            }
            if(requestedProjection!=null){
                int[] projected=unlockedReferences.get(requestedProjection);
                if(projected!=null){reference=projected.clone();referenceVisible=true;projectedSpell=requestedProjection;selectedReferenceSlot=-1;
                    feedback=text("projected",Component.translatable(SpellRegistry.get(projectedSpell).getNameKey()),level);}
                else feedback=text("reference_locked");
                requestedProjection=null;
            }
        }
        // 阶段B：接收试运行诊断；失败时用分层诊断文本替代笼统的 invalid 提示
        diagnosis=state.getIntArray("Diagnosis");
        if(action==SlottedResearchManager.TEST&&diagnosis.length>0)feedback=FormationDiagnosis.message(diagnosis);
        if(valid)animation=System.currentTimeMillis();
        if(action==SlottedResearchManager.TEST){
            // 完成门禁（2026-10-08）：最近一次试运行的裁决生效——通过才允许完成；失败则锁定试运行，直到修改符文（revision 变化）才能再测
            if(valid){testedRevision=revision;failedRevision=-1;}else{failedRevision=revision;testedRevision=-1;}
            playSound(valid?SoundEvents.AMETHYST_BLOCK_CHIME:SoundEvents.FIRE_EXTINGUISH,valid?.4f:.18f,valid?1.25f:.8f);
        }else if(action==SlottedResearchManager.COMPLETE&&!valid)playSound(SoundEvents.FIRE_EXTINGUISH,.18f,.8f);
    }
    @Override protected void containerTick(){
        long now=System.currentTimeMillis();
        if(busy&&now-sentAt>8000){busy=false;loading=false;feedbackState.cancel();feedback=text("timeout");}
        if(dirty&&!busy&&world!=null&&now-editedAt>600)save();
        if(referenceVisible&&projectedSpell!=null&&!unlockedReferences.containsKey(projectedSpell)){referenceVisible=false;reference=new int[0];}
        if(referenceVisible&&projectedSpell==null&&(selectedReferenceSlot<0||!AnalyzedSpellDiagramItem.valid(inventory.getItem(selectedReferenceSlot),world)
            ||!Arrays.equals(reference,foundationReference(inventory.getItem(selectedReferenceSlot).getTag())))){referenceVisible=false;reference=new int[0];}
    }
    @Override protected void renderBg(GuiGraphics context,float delta,int mouseX,int mouseY){
        context.blit(image("background"),this.leftPos,this.topPos,0,0,312,212,312,212);
    }
    @Override protected void renderLabels(GuiGraphics context,int mouseX,int mouseY){}
    @Override public void render(GuiGraphics context,int mouseX,int mouseY,float delta){
        RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();context.setColor(1,1,1,1);
        renderBackground(context);renderBg(context,delta,mouseX,mouseY);
        context.drawString(this.font,text("filled",Arrays.stream(slots).filter(value->value>=0).count(),slots.length),this.leftPos+65,this.topPos+6,0xff414141,false);
        Component rank=text("rank_short",progress.rank);
        context.drawString(this.font,rank,this.leftPos+236-this.font.width(rank),this.topPos+6,0xff414141,false);
        if(!notes)context.drawString(this.font,Component.translatable("research.ssc_addon.runes.stability",stability),this.leftPos+244,this.topPos+68,stability<20?0xffaa2222:0xff414141,false);
        if(testTip==null){testTip=Tooltip.create(text("test"));retestTip=Tooltip.create(text("retest_blocked"));finishTip=Tooltip.create(text("finish"));untestedTip=Tooltip.create(text("untested"));}
        pageButtons.get(6).active=!busy&&!loading&&failedRevision!=revision;
        pageButtons.get(6).setTooltip(failedRevision==revision?retestTip:testTip);
        pageButtons.get(7).active=!busy&&!loading&&evaluatedRevision==revision&&testedRevision==revision&&runeValid
                &&(!upgrading||level>=2&&!this.menu.getSlot(SpellResearchTableBlockEntity.SLOT_UPGRADE_SCROLL).getItem().isEmpty()
                    &&ScrollData.getLevel(this.menu.getSlot(SpellResearchTableBlockEntity.SLOT_UPGRADE_SCROLL).getItem())==level-1)
                &&this.menu.getSlot(SpellResearchTableBlockEntity.SLOT_RESEARCH_OUTPUT).getItem().isEmpty();
        modeButton.active=!busy&&!loading;
        pageButtons.get(7).setTooltip(testedRevision==revision?finishTip:untestedTip);
        int hovered=-1;context.enableScissor(this.leftPos+PALETTE_LEFT,this.topPos+PALETTE_TOP,this.leftPos+PALETTE_LEFT+PALETTE_CELL*2,this.topPos+PALETTE_TOP+PALETTE_HEIGHT);
        for(int glyph=0;glyph<18;glyph++){
            int left=this.leftPos+PALETTE_LEFT+glyph%2*PALETTE_CELL,top=this.topPos+PALETTE_TOP+glyph/2*PALETTE_CELL-paletteScroll;
            if(glyph==selectedGlyph)context.blit(image("sockets"),left,top,PALETTE_CELL,PALETTE_CELL,0,0,64,64,128,64);
            drawGlyph(context,glyph,left+2,top+2,18);
            if(mouseX>=left&&mouseX<left+PALETTE_CELL&&mouseY>=Math.max(top,this.topPos+PALETTE_TOP)&&mouseY<Math.min(top+PALETTE_CELL,this.topPos+PALETTE_TOP+PALETTE_HEIGHT))hovered=glyph;
        }
        context.disableScissor();
        context.blit(image("palette_scroll"),this.leftPos+PALETTE_TRACK_X,this.topPos+PALETTE_TOP,PALETTE_TRACK_WIDTH,PALETTE_HEIGHT,0,0,4,32,8,32);
        context.blit(image("palette_scroll"),this.leftPos+PALETTE_TRACK_X,paletteThumbY(),PALETTE_TRACK_WIDTH,PALETTE_THUMB_HEIGHT,4,0,4,32,8,32);
        if(notes)renderNotes(context);else if(summary)renderSummary(context);else{
            context.blit(image("tier_"+level),this.leftPos+63,this.topPos+19,172,172,0,0,1024,1024,1024,1024);
            if(animation>0&&System.currentTimeMillis()-animation<1500)context.blit(image("tier_"+level+"_glow"),this.leftPos+63,this.topPos+19,172,172,0,0,1024,1024,1024,1024);
            int hoveredSlot=slotAt(mouseX,mouseY);
            for(int index=0;index<slots.length;index++){
                var point=positions.get(index);float centerX=(float)(this.leftPos+63+point.x()*172/512),centerY=(float)(this.topPos+19+point.y()*172/512);
                context.pose().pushPose();context.pose().translate(centerX,centerY,0);
                if(slots[index]>=0)drawGlyph(context,slots[index],-8,-8,16);
                else if(referenceVisible&&reference.length==slots.length&&reference[index]>=0){context.setColor(1,1,1,.55f);drawGlyph(context,reference[index],-8,-8,16);context.setColor(1,1,1,1);}
                if(hoveredSlot==index)context.blit(image("sockets"),-8,-8,16,16,RuneLayout.enhancement(level,index)?64:0,0,64,64,128,64);
                context.pose().popPose();
            }
            // 阶段B：问题环高亮——诊断报出问题的环，其槽位外圈染淡红（仅视觉提示，不影响判定）
            if(diagnosis.length>=6){
                int layers=Math.min(diagnosis[0],(diagnosis.length-2)/4);
                int[] counts=RuneLayout.layers(level);int offset=0;
                for(int layer=0;layer<layers;layer++){
                    int empty=diagnosis[1+layer*4],structural=diagnosis[1+layer*4+1],semantic=diagnosis[1+layer*4+2];
                    if(empty>0||structural>0||semantic>0){
                        for(int p=offset;p<offset+counts[layer];p++){
                            var point=positions.get(p);
                            border(context,
                                (int)(this.leftPos+63+point.x()*172/512)-9,
                                (int)(this.topPos+19+point.y()*172/512)-9,
                                18,18,0x66ff5544);
                        }
                    }
                    offset+=counts[layer];
                }
            }
        }
        int[] costs=inkCosts();int ink=Arrays.stream(costs).sum(),dust=(int)Arrays.stream(slots).filter(value->value>=0).count();
        if(!notes){
            if(upgrading){
                context.drawString(this.font,text("upgrade_scroll"),this.leftPos+244,this.topPos+78,0xff414141,false);
                var main=this.menu.getSlot(SpellResearchTableBlockEntity.SLOT_UPGRADE_SCROLL);
                drawInputSlot(context,main.x,main.y,main.getItem());
            }else{context.renderItem(new ItemStack(SscAddon.BLANK_FORMATION_PAPER),this.leftPos+246,this.topPos+89);context.drawString(this.font,"1",this.leftPos+270,this.topPos+93,0xff414141,false);}
            context.renderItem(new ItemStack(upgrading?RegCustomItem.MOONDUST_CRYSTAL_SHARD:RegCustomItem.UNTREATED_MOONDUST),this.leftPos+246,this.topPos+109);
            context.drawString(this.font,String.valueOf(upgrading?ScrollWorkshopManager.upgradeCatalystCost(level):dust),this.leftPos+270,this.topPos+113,0xff414141,false);
            if(upgrading)ink=ScrollWorkshopManager.upgradeInkCost(level);
            context.renderItem(new ItemStack(SscAddon.FORMATION_INK_NORMAL),this.leftPos+246,this.topPos+129);context.drawString(this.font,String.valueOf(ink),this.leftPos+270,this.topPos+133,0xff414141,false);
            int left=this.leftPos+SpellResearchTableScreenHandler.RESEARCH_OUTPUT_X,top=this.topPos+SpellResearchTableScreenHandler.RESEARCH_OUTPUT_Y;
            context.drawString(this.font,Component.translatable("research.ssc_addon.runes.output"),left,top-12,0xff414141,false);
            context.fill(left-1,top-1,left+17,top+17,0xff8b8b8b);
            context.fill(left-1,top-1,left+17,top,0xff373737);context.fill(left-1,top,left,top+17,0xff373737);
            context.fill(left,top+16,left+17,top+17,0xffffffff);context.fill(left+16,top,left+17,top+16,0xffffffff);
            ItemStack product=this.menu.getSlot(SpellResearchTableBlockEntity.SLOT_RESEARCH_OUTPUT).getItem();
            context.renderItem(product,left,top);context.renderItemDecorations(this.font,product,left,top);
            if(overOutput(mouseX,mouseY))context.fill(left,top,left+16,top+16,0x50ffffff);
        }
        String status=this.font.plainSubstrByWidth(feedback.getString(),292);
        context.drawString(this.font,status,this.leftPos+(312-this.font.width(status))/2,this.topPos+198,0xff414141,false);
        for(GuiEventListener child:this.children())if(child instanceof Renderable renderable)renderable.render(context,mouseX,mouseY,delta);
        if(paletteDragging&&!notes&&!summary){context.setColor(1,1,1,.75f);drawGlyph(context,selectedGlyph,mouseX-8,mouseY-8,16);context.setColor(1,1,1,1);}
        if(overUpgradeInput(mouseX,mouseY)){
            ItemStack main=this.menu.getSlot(SpellResearchTableBlockEntity.SLOT_UPGRADE_SCROLL).getItem();
            if(!main.isEmpty())context.renderTooltip(this.font,main,mouseX,mouseY);
            else context.renderTooltip(this.font,text("upgrade_input"),mouseX,mouseY);
        }else if(referenceMenu){
            refreshReferenceList();
            int first=referenceScroll*REFERENCE_COLUMNS;
            int panelLeft=this.leftPos+REFERENCE_LEFT-2,panelTop=this.topPos+REFERENCE_TOP-2;
            int rows=(REFERENCE_CAPACITY+REFERENCE_COLUMNS-1)/REFERENCE_COLUMNS;
            context.fill(panelLeft,panelTop,panelLeft+REFERENCE_COLUMNS*18+4,panelTop+rows*20+4,0xE0101010);
            for(int index=0;index<REFERENCE_CAPACITY&&first+index<referenceInventorySlots.size();index++){
                int slotIndex=first+index;
                ItemStack stack=inventory.getItem(referenceInventorySlots.get(slotIndex));
                int ix=this.leftPos+REFERENCE_LEFT+index%REFERENCE_COLUMNS*18;
                int iy=this.topPos+REFERENCE_TOP+index/REFERENCE_COLUMNS*20;
                context.renderItem(stack,ix,iy);
                context.renderItemDecorations(this.font,stack,ix,iy);
            }
            int hoveredReference=referenceAt(mouseX,mouseY);
            if(hoveredReference>=0){
                int local=hoveredReference-first;
                border(context,this.leftPos+REFERENCE_LEFT+1+local%REFERENCE_COLUMNS*18,this.topPos+REFERENCE_TOP+1+local/REFERENCE_COLUMNS*20,18,20,0xffffdd88);
            }
            if(hoveredReference>=0)context.renderTooltip(this.font,inventory.getItem(referenceInventorySlots.get(hoveredReference)),mouseX,mouseY);
        }else if(overOutput(mouseX,mouseY)){
            ItemStack product=this.menu.getSlot(SpellResearchTableBlockEntity.SLOT_RESEARCH_OUTPUT).getItem();
            if(product.isEmpty())context.renderTooltip(this.font,Component.translatable("research.ssc_addon.runes.output"),mouseX,mouseY);
            else context.renderTooltip(this.font,product,mouseX,mouseY);
        }else if(hovered>=0)context.renderTooltip(this.font,glyphName(hovered),mouseX,mouseY);
        else if(!notes&&slotAt(mouseX,mouseY)>=0){
            int at=slotAt(mouseX,mouseY);
            List<FormattedCharSequence> wrapped=new ArrayList<>();
            for(Component line:RuneSlotTooltips.slot(level,at,slots,meanings,runeTooltipState,evaluatedRevision==revision))
                wrapped.addAll(this.font.split(line,Math.max(1,Math.min(260,this.width-24))));
            context.renderTooltip(this.font,wrapped,mouseX,mouseY);
        }
        else if(!notes&&mouseX>=this.leftPos+244&&mouseX<this.leftPos+305&&mouseY>=this.topPos+65&&mouseY<this.topPos+84){
            List<Component> lines=new ArrayList<>();
            if(evaluatedRevision!=revision)lines.add(Component.translatable("research.ssc_addon.runes.slot_pending"));
            else{lines.add(Component.translatable("research.ssc_addon.runes.stability_detail",stability));lines.addAll(RuneSummary.effects(runeSummary,false));}
            List<FormattedCharSequence> wrapped=new ArrayList<>();
            for(Component line:lines)wrapped.addAll(this.font.split(line,Math.max(1,Math.min(260,this.width-24))));
            context.renderTooltip(this.font,wrapped,mouseX,mouseY);
        }
        else if(!notes&&mouseX>=this.leftPos+244&&mouseX<this.leftPos+305&&mouseY>=this.topPos+125&&mouseY<this.topPos+148)context.renderComponentTooltip(this.font,upgrading?upgradeCosts():inkLines(costs),mouseX,mouseY);
        else if(!notes&&mouseX>=this.leftPos+244&&mouseX<this.leftPos+305&&mouseY>=this.topPos+105&&mouseY<this.topPos+125){
            if(upgrading)context.renderComponentTooltip(this.font,upgradeCosts(),mouseX,mouseY);
            else context.renderTooltip(this.font,text("dust_cost",dust,SlottedFormationTransaction.count(this.menu.getInventory(),inventory,2,stack->stack.is(RegCustomItem.UNTREATED_MOONDUST))),mouseX,mouseY);
        }
        else if(!notes&&mouseX>=this.leftPos+244&&mouseX<this.leftPos+305&&mouseY>=this.topPos+85&&mouseY<this.topPos+105)context.renderTooltip(this.font,text("paper_cost",SlottedFormationTransaction.count(this.menu.getInventory(),inventory,0,stack->stack.getItem() instanceof BlankFormationPaperItem)),mouseX,mouseY);
        else if(mouseX>=this.leftPos+10&&mouseX<this.leftPos+302&&mouseY>=this.topPos+195&&mouseY<this.topPos+210)context.renderTooltip(this.font,feedback,mouseX,mouseY);
    }
    private void drawInputSlot(GuiGraphics context,int left,int top,ItemStack stack){
        context.fill(this.leftPos+left-1,this.topPos+top-1,this.leftPos+left+17,this.topPos+top+17,0xff8b8b8b);
        border(context,this.leftPos+left-1,this.topPos+top-1,18,18,0xff373737);
        context.fill(this.leftPos+left,this.topPos+top+16,this.leftPos+left+17,this.topPos+top+17,0xffffffff);
        context.fill(this.leftPos+left+16,this.topPos+top,this.leftPos+left+17,this.topPos+top+16,0xffffffff);
        context.renderItem(stack,this.leftPos+left,this.topPos+top);context.renderItemDecorations(this.font,stack,this.leftPos+left,this.topPos+top);
    }
    private List<Component> upgradeCosts(){
        Spell spell=ScrollData.getSpell(this.menu.getSlot(SpellResearchTableBlockEntity.SLOT_UPGRADE_SCROLL).getItem());
        return List.of(text("upgrade_costs",spell==null||spell.getElement()==null?text("upgrade_scroll"):Component.translatable(spell.getElement().getNameKey()),
                ScrollWorkshopManager.upgradeInkCost(level),ScrollWorkshopManager.upgradeCatalystCost(level)));
    }
    private boolean overUpgradeInput(double mouseX,double mouseY){
        var slot=this.menu.getSlot(SpellResearchTableBlockEntity.SLOT_UPGRADE_SCROLL);
        return upgrading&&!notes&&!summary&&!referenceMenu&&mouseX>=this.leftPos+slot.x-1&&mouseX<this.leftPos+slot.x+17&&mouseY>=this.topPos+slot.y-1&&mouseY<this.topPos+slot.y+17;
    }
    private void drawGlyph(GuiGraphics context,int glyph,int left,int top,int size){
        if(glyph>=0&&glyph<18){RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();context.blit(image("runes"),left,top,size,size,glyph*64,0,64,64,18*64,64);}
    }
    private Component glyphName(int glyph){return meanings[glyph]<0?text("unknown",glyph+1):text("role."+meanings[glyph]);}
    private int[] inkCosts(){int[] counts=new int[8];for(int glyph:slots)if(glyph>=0)counts[schools[glyph]]++;for(int school=0;school<8;school++)counts[school]=(counts[school]+1)/2;return counts;}
    private List<Component> inkLines(int[] costs){
        List<Component> lines=new ArrayList<>();var table=this.menu.getInventory();lines.add(text("ink_total",Arrays.stream(costs).sum()));
        for(int school=0;school<8;school++)if(costs[school]>0){final int type=school;int owned=SlottedFormationTransaction.count(table,inventory,1,stack->stack.getItem() instanceof FormationInkItem ink&&SlottedFormationTransaction.school(ink)==type);
            FormationElement element=school==7?FormationElement.UNIVERSAL:FormationElement.byId(ResearchTarget.FAMILIES.get(school));
            lines.add(text("ink_cost",Component.translatable(element.getNameKey()),costs[school],owned));}
        lines.add(text("cost_tip"));return lines;
    }
    private int paletteThumbY(){return this.topPos+PALETTE_TOP+(int)Math.round((double)paletteScroll/PALETTE_MAX_SCROLL*(PALETTE_HEIGHT-PALETTE_THUMB_HEIGHT));}
    private boolean inPalette(double mouseX,double mouseY){return mouseX>=this.leftPos+6&&mouseX<this.leftPos+58&&mouseY>=this.topPos+PALETTE_TOP&&mouseY<this.topPos+PALETTE_TOP+PALETTE_HEIGHT;}
    private void dragPaletteScroll(double mouseY){
        double fraction=(mouseY-this.topPos-PALETTE_TOP-paletteGrabOffset)/(PALETTE_HEIGHT-PALETTE_THUMB_HEIGHT);
        paletteScroll=(int)Math.round(Math.max(0,Math.min(1,fraction))*PALETTE_MAX_SCROLL);
    }
    private void renderNotes(GuiGraphics context){
        List<Component> lines=new ArrayList<>();lines.add(text("discovery_rules"));lines.add(Component.translatable("research.ssc_addon.runes.rules"));
        lines.add(glyphName(selectedGlyph));
        int total=0;for(Component line:lines)total+=this.font.split(line,158).size()*11+5;
        notesScroll=Math.max(0,Math.min(notesScroll,Math.max(0,total-160)));
        context.enableScissor(this.leftPos+66,this.topPos+23,this.leftPos+231,this.topPos+189);int top=this.topPos+24-notesScroll;
        for(Component text:lines){for(var line:this.font.split(text,158)){context.drawString(this.font,line,this.leftPos+69,top,0xffece0bc,false);top+=11;}top+=5;}
        context.disableScissor();
    }
    private void renderSummary(GuiGraphics context){
        List<Component> lines=evaluatedRevision==revision?RuneSummary.lines(runeSummary)
                :List.of(Component.translatable("research.ssc_addon.runes.slot_pending"));
        int total=0;for(Component line:lines)total+=this.font.split(line,158).size()*11+5;
        summaryScroll=Math.max(0,Math.min(summaryScroll,Math.max(0,total-160)));
        context.enableScissor(this.leftPos+66,this.topPos+23,this.leftPos+231,this.topPos+189);int top=this.topPos+24-summaryScroll;
        for(Component line:lines){for(var wrapped:this.font.split(line,158)){context.drawString(this.font,wrapped,this.leftPos+69,top,0xffece0bc,false);top+=11;}top+=5;}
        context.disableScissor();
    }
    private int slotAt(double mouseX,double mouseY){
        if(notes||summary||referenceMenu)return -1;
        for(int index=0;index<positions.size();index++)if(Math.hypot(mouseX-(this.leftPos+63+positions.get(index).x()*172/512),mouseY-(this.topPos+19+positions.get(index).y()*172/512))<8)return index;
        return -1;
    }
    private boolean overOutput(double mouseX,double mouseY){
        return !notes&&!referenceMenu&&mouseX>=this.leftPos+SpellResearchTableScreenHandler.RESEARCH_OUTPUT_X-1
                &&mouseX<this.leftPos+SpellResearchTableScreenHandler.RESEARCH_OUTPUT_X+17
                &&mouseY>=this.topPos+SpellResearchTableScreenHandler.RESEARCH_OUTPUT_Y-1
                &&mouseY<this.topPos+SpellResearchTableScreenHandler.RESEARCH_OUTPUT_Y+17;
    }
    @Override public boolean mouseClicked(double mouseX,double mouseY,int button){
        if(referenceMenu){
            refreshReferenceList();int chosen=referenceAt(mouseX,mouseY);
            if(button==0&&chosen>=0){
                projectedSpell=null;selectedReferenceSlot=referenceInventorySlots.get(chosen);CompoundTag data=inventory.getItem(selectedReferenceSlot).getTag();
                if(data!=null&&data.getInt("Level")==level){reference=foundationReference(data);referenceVisible=true;notes=false;summary=false;visibility();}
                else{feedback=text("diagram_level",data==null?0:data.getInt("Level"));referenceVisible=false;}
            }
            referenceMenu=false;return true;
        }
        for(GuiEventListener child:this.children())if(child.mouseClicked(mouseX,mouseY,button)){setFocused(child);return true;}
        if(overUpgradeInput(mouseX,mouseY)&&(button==0||button==1)){
            if(this.minecraft.gameMode!=null)this.minecraft.gameMode.handleInventoryMouseClick(this.menu.containerId,SpellResearchTableBlockEntity.SLOT_UPGRADE_SCROLL,button,
                    hasShiftDown()?ClickType.QUICK_MOVE:ClickType.PICKUP,this.minecraft.player);return true;
        }
        if(button==0&&overOutput(mouseX,mouseY)){
            if(this.minecraft!=null&&this.minecraft.player!=null&&this.minecraft.gameMode!=null)
                this.minecraft.gameMode.handleInventoryMouseClick(this.menu.containerId,SpellResearchTableBlockEntity.SLOT_RESEARCH_OUTPUT,0,ClickType.QUICK_MOVE,this.minecraft.player);
            return true;
        }
        if(button==0&&inPalette(mouseX,mouseY)&&mouseX>=this.leftPos+PALETTE_TRACK_X){
            int thumb=paletteThumbY();paletteGrabOffset=mouseY>=thumb&&mouseY<thumb+PALETTE_THUMB_HEIGHT?mouseY-thumb:PALETTE_THUMB_HEIGHT/2.0;
            paletteScrolling=true;paletteDragging=false;dragging=false;dragPaletteScroll(mouseY);return true;
        }
        if(world==null||loading)return true;
        if(button==0&&mouseX>=this.leftPos+PALETTE_LEFT&&mouseX<this.leftPos+PALETTE_LEFT+PALETTE_CELL*2&&inPalette(mouseX,mouseY)){
            int glyph=((int)(mouseY-this.topPos-PALETTE_TOP)+paletteScroll)/PALETTE_CELL*2+(int)(mouseX-this.leftPos-PALETTE_LEFT)/PALETTE_CELL;
            if(glyph>=0&&glyph<18){selectedGlyph=glyph;paletteDragging=true;}return true;
        }
        int slot=slotAt(mouseX,mouseY);
        if(slot>=0&&(button==0||button==1)){dragging=true;erasing=button==1;touched.clear();paint(slot);return true;}
        return false;
    }
    private void paint(int slot){
        if(slot<0||touched.get(slot))return;touched.set(slot);int value=erasing?-1:selectedGlyph;
        setSlot(slot,value);
    }
    private void setSlot(int slot,int value){
        if(slots[slot]==value)return;slots[slot]=value;edited();
        if(feedbackState.allowEdit(System.nanoTime()))playSound(value<0?SoundEvents.BOOK_PAGE_TURN:SoundEvents.AMETHYST_BLOCK_CHIME,value<0?.18f:.22f,value<0?1.2f:1.45f+slot%3*.1f);
    }
    private void playSound(SoundEvent sound,float volume,float pitch){
        if(this.minecraft!=null)this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(sound,pitch,volume));
    }
    @Override public boolean mouseDragged(double mouseX,double mouseY,int button,double deltaX,double deltaY){
        if(paletteScrolling&&button==0){dragPaletteScroll(mouseY);return true;}
        if(paletteDragging)return true;
        if(!dragging)return false;
        int steps=Math.max(1,(int)Math.ceil(Math.hypot(deltaX,deltaY)/3));
        for(int step=1;step<=steps;step++)paint(slotAt(mouseX-deltaX+deltaX*step/steps,mouseY-deltaY+deltaY*step/steps));return true;
    }
    @Override public boolean mouseReleased(double mouseX,double mouseY,int button){
        if(paletteScrolling){paletteScrolling=false;return true;}
        if(paletteDragging&&button==0){int slot=slotAt(mouseX,mouseY);if(slot>=0)setSlot(slot,selectedGlyph);}
        paletteDragging=false;dragging=false;return true;
    }
    @Override public boolean mouseScrolled(double mouseX,double mouseY,double amount){
        if(referenceMenu){referenceScroll=Math.max(0,Math.min(maxReferenceScroll(),referenceScroll-(int)amount));return true;}
        if(notes&&mouseX>=this.leftPos+63&&mouseX<this.leftPos+236){notesScroll-=(int)(amount*22);return true;}
        if(summary&&mouseX>=this.leftPos+63&&mouseX<this.leftPos+236){summaryScroll-=(int)(amount*22);return true;}
        if(inPalette(mouseX,mouseY)){paletteScroll=Math.max(0,Math.min(PALETTE_MAX_SCROLL,paletteScroll-(int)(amount*PALETTE_CELL)));return true;}return false;
    }
    private void edited(){revision++;animation=0;dirty=true;editedAt=System.currentTimeMillis();feedback=text("edited");diagnosis=new int[0];drafts.put(level,slots.clone());}
    private void clearCurrent(){
        if(busy||loading||notes||summary||Arrays.stream(slots).noneMatch(value->value>=0))return;
        dragging=false;erasing=false;paletteDragging=false;paletteScrolling=false;touched.clear();
        referenceVisible=false;referenceMenu=false;slots=RuneLayout.empty(level);edited();feedback=text("cleared");
        playSound(SoundEvents.BOOK_PAGE_TURN,.25f,.8f);
    }
    private int[] foundationReference(CompoundTag data){return AnalyzedSpellDiagramItem.referenceSlots(data);}
    private void refreshReferenceList(){
        referenceInventorySlots.clear();if(world!=null)for(int slot=0;slot<Math.min(36,inventory.getContainerSize());slot++)if(AnalyzedSpellDiagramItem.valid(inventory.getItem(slot),world))referenceInventorySlots.add(slot);
        referenceScroll=Math.max(0,Math.min(referenceScroll,maxReferenceScroll()));
    }
    private int maxReferenceScroll(){return (Math.max(0,referenceInventorySlots.size()-REFERENCE_CAPACITY)+REFERENCE_COLUMNS-1)/REFERENCE_COLUMNS;}
    private int referenceAt(double mouseX,double mouseY){
        double localX=mouseX-this.leftPos-REFERENCE_LEFT-1,localY=mouseY-this.topPos-REFERENCE_TOP-1;
        if(localX<0||localY<0||localX>=REFERENCE_COLUMNS*18||localY>=80)return -1;
        int local=(int)localY/20*REFERENCE_COLUMNS+(int)localX/18,index=referenceScroll*REFERENCE_COLUMNS+local;
        return local<REFERENCE_CAPACITY&&index<referenceInventorySlots.size()?index:-1;
    }
    @Override public boolean keyPressed(int key,int scan,int modifiers){
        if(key==GLFW.GLFW_KEY_ESCAPE){if(referenceMenu){referenceMenu=false;return true;}if(notes||summary){notes=false;summary=false;visibility();return true;}back(0);return true;}
        if(this.minecraft!=null&&this.minecraft.options.keyInventory.matches(key,scan)){back(0);return true;}
        return false;
    }
    private void back(int tab){save();handoff=true;parent.openResearchTab(tab);}
    @Override public void onClose(){back(0);}
    @Override public void removed(){if(!handoff){save();super.removed();}}

    private static void border(GuiGraphics context,int x,int y,int width,int height,int color){
        context.fill(x,y,x+width,y+1,color);
        context.fill(x,y+height-1,x+width,y+height,color);
        context.fill(x,y,x+1,y+height,color);
        context.fill(x+width-1,y,x+width,y+height,color);
    }

    public static final class FeedbackState {
        private UUID pending;
        private int action;
        private long lastEdit;
        private boolean playedEdit;

        public UUID begin(int nextAction){action=nextAction;pending=UUID.randomUUID();return pending;}
        public boolean finish(UUID request,int replyAction){
            if(pending==null||!pending.equals(request)||replyAction!=action)return false;
            pending=null;return true;
        }
        public void cancel(){pending=null;}
        public boolean allowEdit(long now){
            if(playedEdit&&now-lastEdit<70_000_000L)return false;
            playedEdit=true;lastEdit=now;return true;
        }
    }
}
