package com.yourname.floptext_rgb.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import java.util.function.IntConsumer;

/** Reconstructed color picker. Click a hue/saturation swatch and confirm. */
public final class ColorPickerScreen extends Screen {
    private final Screen parent;
    private final IntConsumer onPicked;
    private int chosen=0xFF3D9A;
    private int gridX,gridY,cellW,cellH,gridCols=18,gridRows=9;
    public ColorPickerScreen(Screen parent,IntConsumer onPicked){super(Text.literal("Color Picker"));this.parent=parent;this.onPicked=onPicked;}
    @Override protected void init(){
        clearChildren();
        gridX=Math.max(12,(width-360)/2); gridY=70;
        cellW=Math.max(1,Math.min(20,(width-24)/gridCols)); cellH=22;
        addDrawableChild(net.minecraft.client.gui.widget.ButtonWidget.builder(Text.literal("Use Color"),b->onPicked.accept(chosen)).dimensions(width/2-100,height-42,90,22).build());
        addDrawableChild(net.minecraft.client.gui.widget.ButtonWidget.builder(Text.literal("Cancel"),b->MinecraftClient.getInstance().setScreen(parent)).dimensions(width/2+10,height-42,90,22).build());
    }
    @Override public void render(DrawContext c,int mx,int my,float delta){renderBackground(c,mx,my,delta);c.drawCenteredTextWithShadow(textRenderer,Text.literal("Pick a Gradient Color"),width/2,25,0xFFF2F5FF);c.drawCenteredTextWithShadow(textRenderer,Text.literal("Select a hue and shade, then press Use Color."),width/2,42,0xFF9BA9C0);
        for(int row=0;row<gridRows;row++)for(int col=0;col<gridCols;col++){float hue=col/(float)gridCols;float sat=1f-row/(float)(gridRows+1);int rgb=java.awt.Color.HSBtoRGB(hue,sat,1f);int x=gridX+col*cellW,y=gridY+row*cellH;c.fill(x,y,x+cellW-2,y+cellH-2,0xFF000000|rgb);if((rgb&0xFFFFFF)==chosen){c.fill(x,y,x+cellW-2,y+2,0xFFFFFFFF);c.fill(x,y+cellH-4,x+cellW-2,y+cellH-2,0xFFFFFFFF);}}
        c.fill(width/2-45,gridY+gridRows*cellH+8,width/2+45,gridY+gridRows*cellH+33,0xFF000000|chosen);c.drawCenteredTextWithShadow(textRenderer,Text.literal(String.format("#%06X",chosen)),width/2,gridY+gridRows*cellH+41,0xFFF2F5FF);super.render(c,mx,my,delta);}
    @Override public boolean mouseClicked(Click click, boolean doubled){double x=click.x(),y=click.y();if(click.button()==0&&x>=gridX&&x<gridX+gridCols*cellW&&y>=gridY&&y<gridY+gridRows*cellH){int col=(int)(x-gridX)/cellW,row=(int)(y-gridY)/cellH;chosen=java.awt.Color.HSBtoRGB(col/(float)gridCols,1f-row/(float)(gridRows+1),1f)&0xFFFFFF;return true;}return super.mouseClicked(click,doubled);}
    @Override public void close(){net.minecraft.client.MinecraftClient.getInstance().setScreen(parent);}
}
