package com.yourname.floptext_rgb.library;

import com.yourname.floptext_rgb.config.FlopTextRGBConfig;
import com.yourname.floptext_rgb.config.FlopTextRGBConfig.GradientPreset;
import com.yourname.floptext_rgb.screen.FlopTextRGBScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.List;

/** Destination chooser shown by Save Preset; it never silently saves to Favorites. */
public final class PresetDestinationScreen extends Screen {
    private final FlopTextRGBScreen mainScreen;
    private final Screen returnScreen;
    private TextFieldWidget nameField;
    private int modalX, modalY, modalW, modalH, viewportTop, viewportBottom, scroll, scrollMax;
    private String notice = "";
    private String pendingName = "";

    public PresetDestinationScreen(FlopTextRGBScreen mainScreen, Screen returnScreen) {
        super(Text.literal("Save Preset")); this.mainScreen=mainScreen; this.returnScreen=returnScreen;
        this.pendingName = mainScreen.getPresetName();
    }
    public static void openFromMain(FlopTextRGBScreen main) {
        Screen back = MinecraftClient.getInstance().currentScreen;
        MinecraftClient.getInstance().setScreen(new PresetDestinationScreen(main, back));
    }
    @Override protected void init() {
        clearChildren();
        modalW=Math.min(440,width-24); modalH=Math.min(height-28,Math.max(210,160+PresetLibraryStore.data().folders.size()*30));
        modalX=(width-modalW)/2;modalY=(height-modalH)/2;viewportTop=modalY+100;viewportBottom=modalY+modalH-42;
        scrollMax=Math.max(0,20+PresetLibraryStore.data().folders.size()*30-(viewportBottom-viewportTop));scroll=Math.max(0,Math.min(scroll,scrollMax));
        nameField=new TextFieldWidget(textRenderer,modalX+16,modalY+50,modalW-32,20,Text.literal("Preset name"));
        nameField.setMaxLength(64);nameField.setText(pendingName);nameField.setChangedListener(v->pendingName=v);addDrawableChild(nameField);
        LibraryUi.button(this,"Save to Favorites",modalX+16,viewportTop,modalW-32,22,b->saveTo(0));
        for(int i=0;i<PresetLibraryStore.data().folders.size();i++){
            final int folderIndex=i;
            int top=viewportTop+29+i*30-scroll;
            if (top < viewportTop || top + 22 > viewportBottom) continue;
            LibraryUi.button(this,"Save to: "+PresetLibraryScreen.clip(PresetLibraryStore.data().folders.get(i).name,30),modalX+16,top,modalW-32,22,b->saveTo(folderIndex+1));
        }
        LibraryUi.button(this,"Cancel",modalX+modalW-104,modalY+modalH-31,88,20,b->cancel());
    }
    @Override public void render(DrawContext context,int mouseX,int mouseY,float delta){
        renderBackground(context,mouseX,mouseY,delta);context.fill(0,0,width,height,0x99000000);LibraryUi.panel(context,modalX,modalY,modalW,modalH);
        context.drawCenteredTextWithShadow(textRenderer,Text.literal("Save Preset"),width/2,modalY+12,0xFFF2F5FF);
        context.drawTextWithShadow(textRenderer,Text.literal("Choose a name and destination."),modalX+16,modalY+31,0xFF9BA9C0);
        if(!notice.isEmpty())context.drawCenteredTextWithShadow(textRenderer,Text.literal(notice),width/2,modalY+modalH-52,0xFFFF8A8A);
        super.render(context,mouseX,mouseY,delta);
    }
    private void saveTo(int destination){
        String name=nameField==null?pendingName:nameField.getText().trim();
        name=PresetLibraryStore.cleanName(name);
        if(name.isBlank()){notice="Enter a preset name.";return;}
        GradientPreset preset=mainScreen.snapshotCurrentPreset(name);
        List<GradientPreset> list;
        if(destination==0) list=FlopTextRGBConfig.get().presets;
        else {
            int index=destination-1;
            if(index<0||index>=PresetLibraryStore.data().folders.size()){notice="Destination not found.";return;}
            PresetLibraryStore.Folder folder=PresetLibraryStore.data().folders.get(index);list=folder.presets;
        }
        for (GradientPreset existing : list) {
            if (existing != null && existing.name != null && existing.name.equalsIgnoreCase(name)) {
                notice="A preset with that name already exists there."; return;
            }
        }
        list.add(preset);
        if(destination==0)FlopTextRGBConfig.save();else PresetLibraryStore.save();
        mainScreen.setPresetName(name);
        MinecraftClient.getInstance().setScreen(returnScreen==null?mainScreen:returnScreen);
    }
    private void cancel(){MinecraftClient.getInstance().setScreen(returnScreen==null?mainScreen:returnScreen);}
    private static boolean inside(double x,double y,int l,int t,int w,int h){return x>=l&&y>=t&&x<l+w&&y<t+h;}
    @Override public boolean mouseScrolled(double mouseX,double mouseY,double h,double v){if(mouseY>=viewportTop&&mouseY<=viewportBottom){scroll=Math.max(0,Math.min(scrollMax,scroll-(int)Math.signum(v)*24));init();return true;}return super.mouseScrolled(mouseX,mouseY,h,v);}
    @Override public void close(){cancel();}
}
