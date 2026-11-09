package github.com.gengyoubo.sscfa.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.function.BooleanSupplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class ResearchTableTabs {
    public static final int ANALYSIS=0, RESEARCH=1, WORKSHOP=2, SCRIBE=3, LEARN=4, COUNT=5;
    private static final ResourceLocation TABS=new ResourceLocation("minecraft","textures/gui/container/creative_inventory/tabs.png");
    private static final Item[] ICONS={Items.WRITABLE_BOOK,Items.ENCHANTED_BOOK,Items.MAP,Items.PAPER,Items.BOOK};
    private ResearchTableTabs(){}
    public static Button tutorial(int left,int top,Runnable action){
        return Button.builder(Component.translatable("tutorial.ssc_addon.replay"),ignored->action.run())
                .bounds(left+244,top-24,60,20).build();
    }
    public static Button create(int left,int top,int index,BooleanSupplier selected,Runnable action){
        Component title=Component.translatable("research.ssc_addon.slotted.tab."+index);
        var button=new Button(left+index*27,top-28,26,28,title,ignored->action.run(),supplier->supplier.get()){
            @Override public void renderWidget(GuiGraphics context,int mouseX,int mouseY,float delta){
                RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();context.setColor(1,1,1,1);
                context.blit(TABS,getX(),getY(),index*26,selected.getAsBoolean()?32:0,26,selected.getAsBoolean()?32:28,256,256);
                context.renderItem(new ItemStack(ICONS[index]),getX()+5,getY()+9);
            }
        };
        button.setTooltip(Tooltip.create(title));return button;
    }
}
