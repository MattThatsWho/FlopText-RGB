package com.yourname.floptext_rgb.library;

import com.yourname.floptext_rgb.config.FlopTextRGBConfig;
import com.yourname.floptext_rgb.config.FlopTextRGBConfig.GradientPreset;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** In-game file browser for preset JSON and legacy folder ZIP files. */
public final class PresetFileBrowserScreen extends Screen {
    public static final int IMPORT_FAVORITES_JSON=1, EXPORT_FAVORITES_JSON=2, IMPORT_FOLDER_JSON=3,
        EXPORT_FOLDER_JSON=4, EXPORT_FAVORITE_PRESET_JSON=5, EXPORT_FOLDER_PRESET_JSON=6,
        IMPORT_FOLDER_PRESETS_JSON=7, LEGACY_ZIP_IMPORT=8, LEGACY_ZIP_EXPORT=9;
    private final Object owner;
    private final int mode;
    private final int itemIndex;
    private final boolean saveMode;
    private final Path home;
    private Path currentDir;
    private List<Path> entries=new ArrayList<>();
    private int scroll,scrollMax,listTop=100,listBottom;
    private TextFieldWidget filenameField;
    private String status="";
    private int statusTicks;
    private final String defaultName;

    private PresetFileBrowserScreen(Object owner,int mode,int itemIndex){
        super(Text.literal(isSave(mode)?"Export FlopText RGB JSON":"Import FlopText RGB File"));
        this.owner=owner;this.mode=mode;this.itemIndex=itemIndex;this.saveMode=isSave(mode);
        this.home=PresetFolderWorkflow.directory().toAbsolutePath().normalize();this.currentDir=home;this.defaultName=getDefaultName();
    }
    private static boolean isSave(int mode){return mode==EXPORT_FAVORITES_JSON||mode==EXPORT_FOLDER_JSON||mode==EXPORT_FAVORITE_PRESET_JSON||mode==EXPORT_FOLDER_PRESET_JSON||mode==LEGACY_ZIP_EXPORT;}
    public static void openTransfer(Object owner,int mode,int index){MinecraftClient.getInstance().setScreen(new PresetFileBrowserScreen(owner,mode,index));}
    public static void openLegacyImport(){openLegacyImport(MinecraftClient.getInstance().currentScreen);}
    public static void openLegacyImport(Object owner){openTransfer(owner,LEGACY_ZIP_IMPORT,-1);}
    public static void openLegacyExportFolder(String folderName){openTransfer(MinecraftClient.getInstance().currentScreen,LEGACY_ZIP_EXPORT,-1);}

    @Override protected void init(){
        clearChildren();listBottom=height-105;listTop=94;
        if (width >= 540) {
            LibraryUi.button(this,"Back",14,12,74,22,b->returnToOwner(""));
            LibraryUi.button(this,"Up",96,12,54,22,b->goUp());
            LibraryUi.button(this,"Refresh",156,12,72,22,b->refresh());
            LibraryUi.button(this,"Open Preset Folder",234,12,138,22,b->{currentDir=home;refreshEntries();init();});
            LibraryUi.button(this,"Settings",width-92,12,78,22,b->PresetSettingsScreen.open(this));
        } else {
            LibraryUi.button(this,"Back",14,12,60,22,b->returnToOwner(""));
            LibraryUi.button(this,"Up",80,12,46,22,b->goUp());
            LibraryUi.button(this,"Refresh",132,12,64,22,b->refresh());
            LibraryUi.button(this,"Settings",width-78,12,64,22,b->PresetSettingsScreen.open(this));
            LibraryUi.button(this,"Open Preset Folder",14,40,width-28,22,b->{currentDir=home;refreshEntries();init();});
        }
        filenameField=new TextFieldWidget(textRenderer,14,height-78,width-190,20,Text.literal("File name"));filenameField.setMaxLength(120);filenameField.setText(defaultName);addDrawableChild(filenameField);
        LibraryUi.button(this,saveMode?"Save Here":"Import Selected",width-166,height-78,152,20,b->{if(saveMode)doExport();else {Path selected=selectedPath();if(selected==null)setNotice("Select a JSON or ZIP file first.");else doImport(selected);}});
        LibraryUi.button(this,"Cancel",width-92,height-48,78,20,b->returnToOwner(""));
        LibraryUi.button(this,"Open Folder",14,height-48,104,20,b->{Path p=selectedPath();if(p!=null&&Files.isDirectory(p)){currentDir=p;refreshEntries();init();}else setNotice("Select a folder first.");});
        refreshEntries();
        int maxRows=Math.max(0,(listBottom-listTop)/22);scrollMax=Math.max(0,entries.size()-maxRows);scroll=Math.max(0,Math.min(scroll,scrollMax));
        int row=0;for(int i=scroll;i<Math.min(entries.size(),scroll+maxRows);i++){Path path=entries.get(i);final int index=i;String label=(Files.isDirectory(path)?"[Folder] ":"")+clip(path.getFileName().toString(),38);LibraryUi.button(this,label,18,listTop+row*22,width-36,20,b->{if(Files.isDirectory(path)){currentDir=path;scroll=0;refreshEntries();init();}else if(filenameField!=null){filenameField.setText(path.getFileName().toString());setNotice("Selected "+path.getFileName());}});row++;}
    }
    public void refresh(){refreshEntries();init();}
    private void refreshEntries(){try{Files.createDirectories(currentDir);entries.clear();try(var stream=Files.list(currentDir)){entries.addAll(stream.filter(p->Files.isDirectory(p)||supported(p)).sorted(Comparator.comparing((Path p)->!Files.isDirectory(p)).thenComparing(p->p.getFileName().toString().toLowerCase(Locale.ROOT))).toList());}}catch(Exception e){setNotice("Could not read folder: "+msg(e));entries=new ArrayList<>();}}
    private boolean supported(Path p){String n=p.getFileName().toString().toLowerCase(Locale.ROOT);return n.endsWith(".json")||n.endsWith(".zip");}
    private String getDefaultName(){return switch(mode){case EXPORT_FAVORITES_JSON->"favorites.json";case EXPORT_FOLDER_JSON->safeName(folderName())+".json";case EXPORT_FAVORITE_PRESET_JSON,EXPORT_FOLDER_PRESET_JSON->"preset.json";case LEGACY_ZIP_EXPORT->safeName(folderName())+".zip";default->"";};}
    /** Toolbar controls are constructed directly in init(); this hook intentionally adds no duplicates. */
    public void installConvenienceButtons() { /* See init(): all toolbar buttons are already installed there. */ }
    @Override public void render(DrawContext c,int mx,int my,float delta){renderBackground(c,mx,my,delta);LibraryUi.panel(c,10,8,width-20,height-16);c.drawCenteredTextWithShadow(textRenderer,Text.literal(saveMode?"Export FlopText RGB":"Import FlopText RGB"),width/2,40,0xFFF2F5FF);c.drawTextWithShadow(textRenderer,Text.literal("Folder: "+clip(currentDir.toString(),Math.max(20,(width-40)/6))),18,68,0xFF9BA9C0);c.drawTextWithShadow(textRenderer,Text.literal("JSON exchange folder: config/floptext_rgb/preset_files/"),18,height-94,0xFF9BA9C0);if(statusTicks>0){c.drawCenteredTextWithShadow(textRenderer,Text.literal(status),width/2,height-25,0xFF70D6A5);statusTicks--;}super.render(c,mx,my,delta);}
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){if(y>=listTop&&y<=listBottom){scroll=Math.max(0,Math.min(scrollMax,scroll-(int)Math.signum(vertical)*2));init();return true;}return super.mouseScrolled(x,y,horizontal,vertical);}
    @Override public void close(){returnToOwner("");}
    private void goUp(){Path parent=currentDir.getParent();if(parent!=null&&parent.startsWith(home.getParent())){currentDir=parent;scroll=0;refreshEntries();init();}}
    private Path selectedPath() {
        String name = filenameField == null ? "" : filenameField.getText().trim();
        if (name.isEmpty() || name.equals(".") || name.equals("..")
                || name.contains("/") || name.contains("\\")) return null;
        Path typed = currentDir.resolve(name).normalize();
        return typed.startsWith(currentDir) && Files.exists(typed) ? typed : null;
    }

    private void doImport(Path path) {
        boolean ok;
        try {
            ok = switch (mode) {
                case LEGACY_ZIP_IMPORT -> PresetLibraryStore.importFolder(path);
                case IMPORT_FOLDER_JSON -> PresetJsonTransfer.importFolderFile(path);
                case IMPORT_FAVORITES_JSON -> PresetJsonTransfer.importJsonToFavorites(path);
                case IMPORT_FOLDER_PRESETS_JSON -> PresetJsonTransfer.importJsonToFolder(path, folderForOwner());
                default -> throw new IllegalArgumentException("This screen is not configured for importing this file type.");
            };
            String message = mode == LEGACY_ZIP_IMPORT
                ? PresetLibraryStore.status() : PresetJsonTransfer.lastStatus();
            if (message == null || message.isBlank()) message = ok ? "Import completed." : "Import failed.";
            setNotice(message);
        } catch (Exception e) {
            setNotice("Import failed: " + msg(e));
        }
    }

    private void doExport() {
        try {
            String name = safeName(filenameField == null ? defaultName : filenameField.getText());
            if (name.isBlank()) name = safeName(defaultName);
            if (name.isBlank() || name.equals(".") || name.equals("..")) {
                setNotice("Enter a valid file name.");
                return;
            }
            Path target = currentDir.resolve(name).normalize();
            if (!target.startsWith(currentDir) || target.equals(currentDir)) {
                setNotice("Choose a file name in the current folder.");
                return;
            }
            boolean ok = switch (mode) {
                case EXPORT_FAVORITES_JSON -> PresetJsonTransfer.exportFavoritesJson(target);
                case EXPORT_FOLDER_JSON -> PresetJsonTransfer.exportFolderJson(folderForOwner(), target);
                case EXPORT_FAVORITE_PRESET_JSON -> exportFavoritePreset(target);
                case EXPORT_FOLDER_PRESET_JSON -> exportFolderPreset(target);
                case LEGACY_ZIP_EXPORT -> exportFolderZip(target);
                default -> false;
            };
            String message = mode == LEGACY_ZIP_EXPORT
                ? PresetLibraryStore.status() : PresetJsonTransfer.lastStatus();
            if (message == null || message.isBlank()) message = ok ? "Export completed." : "Export failed.";
            setNotice(message);
        } catch (Exception e) {
            setNotice("Export failed: " + msg(e));
        }
    }
    private boolean exportFavoritePreset(Path target){List<GradientPreset> fav=FlopTextRGBConfig.get().presets;return itemIndex>=0&&itemIndex<fav.size()&&PresetJsonTransfer.exportPresetJson(fav.get(itemIndex),target);}
    private boolean exportFolderPreset(Path target){PresetLibraryStore.Folder f=folderForOwner();return f!=null&&itemIndex>=0&&itemIndex<f.presets.size()&&PresetJsonTransfer.exportPresetJson(f.presets.get(itemIndex),target);}
    private boolean exportFolderZip(Path target){PresetLibraryStore.Folder f=folderForOwner();return f!=null&&PresetLibraryStore.exportFolder(f,target);}
    private PresetLibraryStore.Folder folderForOwner(){if(owner instanceof PresetFolderScreen f)return f.getFolder();if(owner instanceof PresetLibraryScreen)return null;return null;}
    private void returnToOwner(String message){if(owner instanceof PresetLibraryScreen l&&!message.isBlank())l.setNotice(message);if(owner instanceof PresetFolderScreen f&&!message.isBlank())f.setNotice(message);MinecraftClient.getInstance().setScreen(owner instanceof Screen s?s:null);}
    private String msg(Throwable t){return t.getMessage()==null?t.getClass().getSimpleName():t.getMessage();}
    private static String safeName(String s) {
        if (s == null) return "";
        String name = s.replaceAll("[^A-Za-z0-9._ -]", "_").trim();
        if (name.equals(".") || name.equals("..")) return "";
        return name;
    }
    private static String clip(String s,int limit){if(s==null)return "";return s.length()<=limit?s:s.substring(0,Math.max(0,limit-1))+"…";}
    private String folderName(){if(owner instanceof PresetFolderScreen f&&f.getFolder()!=null)return f.getFolder().name;return "floptext-rgb";}
    public void setNotice(String value){status=value==null?"":value;statusTicks=180;}
}
