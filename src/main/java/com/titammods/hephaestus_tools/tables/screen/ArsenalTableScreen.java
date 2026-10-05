package com.titammods.hephaestus_tools.tables.screen;

import com.titammods.hephaestus_tools.HephaestusTools;
import com.titammods.hephaestus_tools.materials.MaterialManager;
import com.titammods.hephaestus_tools.materials.trait.MaterialTrait;
import com.titammods.hephaestus_tools.table.ArsenalTableLayout;
import com.titammods.hephaestus_tools.table.ToolAssembly;
import com.titammods.hephaestus_tools.tables.menu.ArsenalTableMenu;
import com.titammods.hephaestus_tools.tools.nbt.ToolStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import com.titammods.hephaestus_tools.client.renderer.ToolItemRenderer;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ArsenalTableScreen extends AbstractContainerScreen<ArsenalTableMenu> {
    private static final int LIST_X=178, LIST_Y=56, LIST_W=88, ROW_STEP=24;
    private static final int APPLY_X=203, APPLY_Y=128, APPLY_W=63, APPLY_H=14;
    private static final int CHECK_X=APPLY_X-16, CHECK_Y=APPLY_Y+(APPLY_H-10)/2;
    private static final int MASTERY_LIST_X=181, MASTERY_LIST_Y=60, MASTERY_LIST_W=84;
    private static final int MASTERY_CENTER_X=MASTERY_LIST_X+MASTERY_LIST_W/2;
    private static final int SCROLL_X=273, SCROLL_H=70, THUMB_H=7;
    private boolean draggingModifiers;
    private boolean craftableOnly;
    private List<com.titammods.hephaestus_tools.table.ToolUpgrade> visibleUpgrades=List.of();
    private static final int SELECT_X=305, SELECT_Y=124, SELECT_W=63, SELECT_H=14;
    private double scrollGrab;
    private static final ResourceLocation MODIFY = rl("modify_screen.png"), MASTERY = rl("mastery_patch_screen.png");
    private int selectedEntry = 0, listOffset = 0, lastTab = -1;
    private net.minecraft.world.item.Item lastTool;
    private boolean expandedLeftStats = false;
    private static final ResourceLocation BG = rl("build_screen.png"), ATLAS = rl("icons_atlas.png"),
            LATERAL = rl("lateral_tab.png"), COLLAPSE = rl("lateral_collapse.png");
    private boolean expandedStats = false;
    private static final int LATERAL_LEFT_X = 16, LATERAL_RIGHT_X = 288, LATERAL_Y = 42;
    private static final int LATERAL_W = 80, LATERAL_TOOLS_H = 108;
    private static final int STATS_W = 96, STATS_PADDING = 10;
    private static final int LATERAL_STATS_H = 107, LATERAL_STATS_EXPANDED_H = 128;
    private static final int TAB_Y = 27;
    private static final int[] TAB_X = {107, 166, 225};
    private static final int[] SLOT = {258,81,22,24}, HEX = {187,51,65,77};
    private static final int[] BAR_EMPTY = {19,15,55,6}, BAR_FULL = {19,23,55,6};
    private static final int[][] STAT_ICONS = {{114,42,8,7},{114,51,8,8},{114,61,8,8},{114,71,8,8},{114,81,8,8}};
    private ItemStack preview = ItemStack.EMPTY, details = ItemStack.EMPTY;
    private final List<Component> traitDetails = new ArrayList<>();
    private static ResourceLocation rl(String name) {
        return ResourceLocation.fromNamespaceAndPath(HephaestusTools.MOD_ID, "textures/gui/forge_table/" + name);
    }
    private static Component tr(String key) { return Component.translatable("gui.hephaestus_tools.build." + key); }
    public ArsenalTableScreen(ArsenalTableMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 384; imageHeight = 256;
    }
    private void atlas(GuiGraphics g, int[] r, int x, int y, int w, int h) {
        g.pose().pushPose(); g.pose().translate(leftPos+x, topPos+y, 0);
        g.pose().scale(w/(float)r[2], h/(float)r[3], 1);
        g.blit(ATLAS, 0, 0, (float)r[0], (float)r[1], r[2], r[3], 384, 256);
        g.pose().popPose();
    }

    private void slicePart(GuiGraphics g, int x, int y, int w, int h,
                           int u, int v, int uw, int vh) {
        if (w <= 0 || h <= 0) return;
        g.blit(LATERAL, leftPos+x, topPos+y, w, h,
                (float)u, (float)v, uw, vh, 24, 24);
    }

    private void lateralPanel(GuiGraphics g, int x, int y, int w, int h) {
        final int b = 5, innerW = 24 - b * 2, innerH = 24 - b * 2;
        int centerW = Math.max(0, w - b * 2), centerH = Math.max(0, h - b * 2);
        slicePart(g,x,y,b,b,0,0,b,b);
        slicePart(g,x+b,y,centerW,b,b,0,innerW,b);
        slicePart(g,x+w-b,y,b,b,24-b,0,b,b);
        slicePart(g,x,y+b,b,centerH,0,b,b,innerH);
        slicePart(g,x+b,y+b,centerW,centerH,b,b,innerW,innerH);
        slicePart(g,x+w-b,y+b,b,centerH,24-b,b,b,innerH);
        slicePart(g,x,y+h-b,b,b,0,24-b,b,b);
        slicePart(g,x+b,y+h-b,centerW,b,b,24-b,innerW,b);
        slicePart(g,x+w-b,y+h-b,b,b,24-b,24-b,b,b);
    }
    private int statsHeight() {
        return expandedStats ? LATERAL_STATS_EXPANDED_H : LATERAL_STATS_H;
    }
    private int toolX(int index) {
        int gridWidth = 3 * 20 + 2 * 4;
        return LATERAL_LEFT_X + (LATERAL_W - gridWidth) / 2 + (index % 3) * 24;
    }
    private int toolY(int index) {
        return 50 + (index / 3) * 24;
    }
    private int collapseX() {
        return LATERAL_RIGHT_X + STATS_W - 20;
    }
    private int collapseY() {
        return LATERAL_Y + statsHeight() - 13;
    }
    private void drawCollapse(GuiGraphics g) {
        g.blit(COLLAPSE, leftPos + collapseX(), topPos + collapseY(), 16, 8,
                0, expandedStats ? 0 : 8, 16, 8, 16, 16);
    }
    private void text(GuiGraphics g, Component text, int x, int y, int width, float scale, int color) {
        scale = Math.min(scale, width/(float)Math.max(1,font.width(text)));
        g.pose().pushPose(); g.pose().translate(leftPos+x,topPos+y,0); g.pose().scale(scale,scale,1);
        g.drawString(font,text,0,0,color,false); g.pose().popPose();
    }
    private void centered(GuiGraphics g, Component label, int cx, int y, int width, float scale, int color) {
        scale = Math.min(scale, width / (float)Math.max(1,font.width(label)));
        text(g,label,cx-Math.round(font.width(label)*scale/2),y,width,scale,color);
    }
    private void ghostPart(GuiGraphics g, ItemStack stack, int x, int y) {
        var model = minecraft.getItemRenderer().getModel(stack, minecraft.level, minecraft.player, 0);
        g.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.setColor(1,1,1,.25f);
        g.blit(leftPos+x,topPos+y,0,16,16,model.getParticleIcon());
        g.setColor(1,1,1,1);
        RenderSystem.disableBlend();
    }
    @Override public int getSlotColor(int index) { return 0x18C9824C; }
    @Override protected void renderBg(GuiGraphics g,float tick,int mx,int my) {
        if (lastTab != menu.getActiveTab() || lastTool != menu.tool().getItem()) {
            selectedEntry = 0; listOffset = 0; visibleUpgrades = List.of();
            draggingModifiers = false;
            lastTab = menu.getActiveTab(); lastTool = menu.tool().getItem();
        }
        if (menu.getActiveTab() != 0) {
            refreshEntries();
            renderProgression(g, mx, my); return;
        }
        preview = menu.preview();
        details = !menu.output().isEmpty() ? menu.output() : !preview.isEmpty() ? preview : menu.tool();
        g.blit(BG,leftPos+96,topPos,96,0,192,256,384,256);
        lateralPanel(g,LATERAL_LEFT_X,LATERAL_Y,LATERAL_W,LATERAL_TOOLS_H);
        lateralPanel(g,LATERAL_RIGHT_X,LATERAL_Y,STATS_W,statsHeight());
        for(int i=0;i<3;i++) {
            atlas(g,i==0 ? new int[]{131,144,50,14} : new int[]{184,144,50,14},TAB_X[i],TAB_Y,50,16);
            centered(g,tr(i==0?"tab_build":i==1?"tab_modify":"tab_mastery"),TAB_X[i]+25,34,42,.85f,i==0?0xFFFFFF:0x77716B);
        }
        centered(g,tr("tools"),LATERAL_LEFT_X+LATERAL_W/2,32,LATERAL_W-12,.8f,0xE8DFD5);
        for(int i=0;i<ToolAssembly.REGISTRY.size();i++) {
            int x=toolX(i), y=toolY(i);
            boolean selected=!menu.isRepair()&&menu.getSelectedTool()==i;
            boolean hover=in(mx,my,x,y,20,20);
            atlas(g,(selected||hover)?new int[]{42,193,22,23}:new int[]{17,193,22,23},x,y,20,20);
            g.renderItem(new ItemStack(ToolAssembly.REGISTRY.get(i).result()),leftPos+x+2,topPos+y+2);
        }
        drawCollapse(g);

        atlas(g,HEX,ArsenalTableLayout.HEX_X,ArsenalTableLayout.HEX_Y,ArsenalTableLayout.HEX_W,ArsenalTableLayout.HEX_H);
        int[][] positions=ArsenalTableLayout.hexSlotPositions();
        for(int i=0;i<4;i++) {
            if(!menu.slots.get(i+1).isActive()) continue;
            atlas(g,SLOT,positions[i][0],positions[i][1],ArsenalTableLayout.SLOT_W,ArsenalTableLayout.SLOT_H);
            if(!menu.isRepair() && menu.input(i).isEmpty()) {
                ghostPart(g,new ItemStack(menu.parts().get(i)),positions[i][0]+ArsenalTableLayout.SLOT_ITEM_INSET,
                        positions[i][1]+(ArsenalTableLayout.SLOT_H-16)/2);
            }
        }
        if(!menu.isRepair()) {
            ItemStack center=preview.isEmpty()?new ItemStack(menu.assembly().result()):preview;
            ToolItemRenderer.renderGuiPreview(g,center,leftPos+ArsenalTableLayout.CENTER_X-16,topPos+ArsenalTableLayout.CENTER_Y-16,32,.32f);
            if(menu.output().isEmpty()&&!preview.isEmpty()) g.renderItem(preview,leftPos+ArsenalTableLayout.OUTPUT_X+5,topPos+ArsenalTableLayout.OUTPUT_Y+5);
        } else if(!menu.output().isEmpty()) {
            ToolItemRenderer.renderGuiPreview(g,menu.output(),leftPos+ArsenalTableLayout.CENTER_X-16,topPos+ArsenalTableLayout.CENTER_Y-16,32,.55f);
        }
        Component name=!details.isEmpty()?details.getHoverName():menu.isRepair()?tr("repair"):new ItemStack(menu.assembly().result()).getHoverName();
        centered(g,name,238,59,76,.85f,0xEEE6DB);
        centered(g,tr(!menu.output().isEmpty()?"collect":preview.isEmpty()?"place_parts":"ready"),238,72,76,.65f,0xADA294);
        if(!details.isEmpty()&&ToolStack.isInitialized(details)) {
            int max=ToolStack.getDurability(details), remaining=max-ToolStack.getCurrentDamage(details);
            centered(g,Component.literal(remaining+" / "+max),238,118,68,.7f,0xBCAE98);
            atlas(g,BAR_EMPTY,211,129,55,6);
            int filled = Math.round(55f * remaining / Math.max(1,max));
            if (filled > 0) {
                g.enableScissor(leftPos+211,topPos+129,leftPos+211+filled,topPos+135);
                atlas(g,BAR_FULL,211,129,55,6);
                g.disableScissor();
            }
        }
        g.enableScissor(leftPos+LATERAL_RIGHT_X+5, topPos+LATERAL_Y+5,
                leftPos+LATERAL_RIGHT_X+STATS_W-5, topPos+LATERAL_Y+statsHeight()-5);
        drawStats(g);
        g.disableScissor();
    }
    private void drawStats(GuiGraphics g) {
        text(g,tr("stats"),300,51,62,.85f,0xEEE6DB);
        traitDetails.clear();
        if(details.isEmpty()||!ToolStack.isInitialized(details)) {
            text(g,tr(menu.getActiveTab()==0?"preview_hint":"insert_tool"),300,68,62,.65f,0xA2978B); return;
        }
        String[] names={"durability","mining_speed","harvest","attack_damage","attack_speed"};
        String[] values={String.valueOf(ToolStack.getDurability(details)),fmt(ToolStack.getMiningSpeed(details)),
                Component.translatable("tooltip.hephaestus_tools.tier."+ToolStack.getHarvestTier(details).name().toLowerCase(Locale.ROOT)).getString(),
                fmt(ToolStack.getAttackDamage(details)),fmt(ToolStack.getAttackSpeed(details))};
        for(int i=0;i<5;i++) {
            int rowY = 64 + i * (menu.getActiveTab()==0?11:10);
            atlas(g,STAT_ICONS[i],300,rowY,8,STAT_ICONS[i][3]);
            text(g,tr(names[i]),312,rowY+2,36,.55f,0xB1A79B);
            Component value = Component.literal(values[i]);
            float scale = Math.min(.6f,22f / Math.max(1,font.width(value)));
            int valueRight = LATERAL_RIGHT_X + STATS_W - STATS_PADDING;
            text(g,value,valueRight-Math.round(font.width(value)*scale),rowY+2,22,scale,0xDED1BE);
        }
        text(g,tr("traits"),300,menu.getActiveTab()==0?122:116,74,.75f,0xEEE6DB);
        var materials=ToolStack.getMaterials(details);
        var parts=com.titammods.hephaestus_tools.tools.helper.ToolBuildHandler.getToolParts(details.getItem());
        for(int i=0;i<materials.size();i++) {
            var mat=materials.get(i);
            var stats=MaterialManager.getInstance().getStatsForSlot(mat,0);
            var trait=MaterialTrait.byId(stats.traitId());
            if(trait.isEmpty()) continue;
            Component traitName=Component.translatable("trait.hephaestus_tools."+trait.get().id());
            Component materialName=Component.translatable("material.hephaestus_tools."+mat.id().getPath());
            Component partName=i<parts.size()?new ItemStack(parts.get(i)).getHoverName():Component.literal("");
            traitDetails.add(traitName.copy().append(" - ").append(materialName).append(" (").append(partName).append(")"));
            int traitY=(menu.getActiveTab()==0?133:127)+(traitDetails.size()-1)*7;
            int bottom=LATERAL_Y+(menu.getActiveTab()==0?statsHeight():(expandedLeftStats?128:107))-15;
            if(traitDetails.size()<=4 && traitY+Math.ceil(font.lineHeight*.52f)<=bottom)
                text(g,traitName.copy().append(" - ").append(materialName),300,traitY,74,.52f,0xB1A79B);
        }
        if(traitDetails.isEmpty()) text(g,tr("no_traits"),300,menu.getActiveTab()==0?133:127,74,.6f,0xA2978B);
    }
    private static String fmt(float f) { return String.format(Locale.ROOT,"%.2f",f); }
    @Override protected void renderLabels(GuiGraphics g,int mx,int my) { }
    @Override public void render(GuiGraphics g,int mx,int my,float tick) {
        super.render(g,mx,my,tick);
        renderTooltip(g,mx,my);
        if (menu.getActiveTab() != 0) { progressionTooltip(g, mx, my); return; }
        Component tooltip=null;
        for(int i=0;i<ToolAssembly.REGISTRY.size();i++)
            if(in(mx,my,toolX(i),toolY(i),20,20)) tooltip=new ItemStack(ToolAssembly.REGISTRY.get(i).result()).getHoverName();
        if(in(mx,my,collapseX(),collapseY(),16,8)) tooltip=tr(expandedStats?"collapse":"expand");
        int[][] pos=ArsenalTableLayout.hexSlotPositions();
        for(int i=0;i<4;i++) if(menu.slots.get(i+1).isActive()&&menu.input(i).isEmpty()&&in(mx,my,pos[i][0],pos[i][1],ArsenalTableLayout.SLOT_W,ArsenalTableLayout.SLOT_H))
            tooltip=menu.isRepair()?tr("repair_hint"):new ItemStack(menu.parts().get(i)).getHoverName();
        if(in(mx,my,300,120,64,50)&&!traitDetails.isEmpty())
            g.renderComponentTooltip(font,traitDetails,mx,my);
        else if(tooltip!=null) g.renderTooltip(font,tooltip,mx,my);
        else if(menu.output().isEmpty()&&!preview.isEmpty()&&in(mx,my,225,83,26,26)) g.renderTooltip(font,preview,mx,my);
    }
    private boolean in(double mx,double my,int x,int y,int w,int h) { return mx>=leftPos+x&&mx<leftPos+x+w&&my>=topPos+y&&my<topPos+y+h; }
    private void send(int id) {
        if(minecraft!=null&&minecraft.gameMode!=null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id);
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK,1f));
        }
    }
    @Override public boolean mouseClicked(double mx,double my,int button) {
        if(button==0) {
            for (int i=0;i<3;i++) if(in(mx,my,TAB_X[i],TAB_Y,50,16)) {
                if (menu.getActiveTab()!=i) send(ArsenalTableMenu.TAB_BASE+i);
                return true;
            }
            if (menu.getActiveTab()!=0 && progressionClick(mx,my)) return true;
            if (menu.getActiveTab()!=0) return super.mouseClicked(mx,my,button);
            for(int i=0;i<ToolAssembly.REGISTRY.size();i++) if(in(mx,my,toolX(i),toolY(i),20,20)) {
                send(ArsenalTableMenu.SELECT_BASE+i); return true;
            }
            if(in(mx,my,collapseX(),collapseY(),16,8)) { expandedStats=!expandedStats; return true; }
        }
        return super.mouseClicked(mx,my,button);
    }

    private List<com.titammods.hephaestus_tools.table.ToolUpgrade> upgrades() {
        return visibleUpgrades;
    }
    private void refreshEntries() {
        if(menu.getActiveTab()==1) {
            ResourceLocation previous=selectedEntry>=0 && selectedEntry<visibleUpgrades.size()?visibleUpgrades.get(selectedEntry).id():null;
            var all=com.titammods.hephaestus_tools.table.ToolUpgrades.availableFor(menu.tool().getItem());
            var previousList=visibleUpgrades;
            visibleUpgrades=all.stream().filter(up->!craftableOnly || minecraft.player!=null &&
                    com.titammods.hephaestus_tools.table.ToolUpgrades.canApply(minecraft.player,menu.tool(),up)).toList();
            selectedEntry=0;
            for(int i=0;i<visibleUpgrades.size();i++) if(visibleUpgrades.get(i).id().equals(previous)) { selectedEntry=i; break; }
            if(!visibleUpgrades.equals(previousList)) {
                if(selectedEntry<listOffset) listOffset=selectedEntry;
                else if(selectedEntry>=listOffset+3) listOffset=selectedEntry-2;
            }
        } else {
            String chosen=com.titammods.hephaestus_tools.table.ToolMastery.selected(menu.tool());
            var paths=com.titammods.hephaestus_tools.table.ToolMastery.forTool(menu.tool().getItem());
            if(!chosen.isEmpty()) selectedEntry=Math.max(0,paths.indexOf(chosen));
            else selectedEntry=Math.min(selectedEntry,Math.max(0,paths.size()-1));
        }
        listOffset=Math.max(0,Math.min(listOffset,Math.max(0,entryCount()-3)));
    }
    private int entryCount() { return menu.getActiveTab()==1 ? upgrades().size() : com.titammods.hephaestus_tools.table.ToolMastery.forTool(menu.tool().getItem()).size(); }
    private Component entryName(int index) {
        return menu.getActiveTab()==1 ? Component.translatable(upgrades().get(index).nameKey())
                : Component.translatable("mastery.hephaestus_tools."+com.titammods.hephaestus_tools.table.ToolMastery.forTool(menu.tool().getItem()).get(index));
    }
    private ResourceLocation entryIcon(int index) {
        if(menu.getActiveTab()==1) {
            String id=upgrades().get(index).id().getPath();
            return rl("modifier_icon/modifier_"+(id.equals("looting")?"fortune":id.equals("silk_touch")?"silky":id)+".png");
        }
        return rl("masterys/"+com.titammods.hephaestus_tools.table.ToolMastery.toolId(menu.tool().getItem())+"/"+com.titammods.hephaestus_tools.table.ToolMastery.forTool(menu.tool().getItem()).get(index)+".png");
    }
    private Component entryDescription() {
        return entryDescription(selectedEntry);
    }
    private Component entryDescription(int index) {
        String id=menu.getActiveTab()==1 ? upgrades().get(index).id().getPath()
                : com.titammods.hephaestus_tools.table.ToolMastery.forTool(menu.tool().getItem()).get(index);
        return Component.translatable((menu.getActiveTab()==1 ? "gui.hephaestus_tools.upgrade." : "mastery.hephaestus_tools.")+id+".desc");
    }
    private void icon(GuiGraphics g, ResourceLocation texture, int x, int y) {
        g.blit(texture,leftPos+x,topPos+y,0,0,16,16,16,16);
    }
    private void wrapped(GuiGraphics g, Component label, int x,int y,int width,int maxHeight) {
        float scale=.6f;
        g.pose().pushPose(); g.pose().translate(leftPos+x,topPos+y,0); g.pose().scale(scale,scale,1);
        int lineY=0;
        for(var line:font.split(label,(int)(width/scale))) {
            if ((lineY+font.lineHeight)*scale>maxHeight) break;
            g.drawString(font,line,0,lineY,0xB1A79B,false); lineY+=font.lineHeight+2;
        }
        g.pose().popPose();
    }
    private void renderProgression(GuiGraphics g,int mx,int my) {
        preview=ItemStack.EMPTY; details=menu.tool();
        int tab=menu.getActiveTab(), leftHeight=expandedLeftStats?128:107;
        g.blit(tab==1?MODIFY:MASTERY,leftPos+96,topPos,96,0,192,256,384,256);
        lateralPanel(g,0,LATERAL_Y,STATS_W,leftHeight);
        lateralPanel(g,LATERAL_RIGHT_X,LATERAL_Y,STATS_W,statsHeight());
        for(int i=0;i<3;i++) {
            atlas(g,i==tab?new int[]{131,144,50,14}:new int[]{184,144,50,14},TAB_X[i],TAB_Y,50,16);
            centered(g,tr(i==0?"tab_build":i==1?"tab_modify":"tab_mastery"),TAB_X[i]+25,34,42,.85f,i==tab?0xFFFFFF:0xAAA197);
        }
        g.enableScissor(leftPos+5,topPos+47,leftPos+STATS_W-5,topPos+LATERAL_Y+leftHeight-15);
        g.pose().pushPose(); g.pose().translate(-LATERAL_RIGHT_X,0,0); drawStats(g); g.pose().popPose();
        g.disableScissor();
        g.blit(COLLAPSE,leftPos+STATS_W-20,topPos+LATERAL_Y+leftHeight-13,16,8,0,expandedLeftStats?0:8,16,8,16,16);
        drawCollapse(g);
        atlas(g,SLOT,109,52,22,24);
        wrapped(g,details.isEmpty()?tr("insert_tool"):details.getHoverName(),135,54,38,18);
        if(!details.isEmpty()) ToolItemRenderer.renderGuiPreview(g,details,leftPos+123,topPos+77,34,1f);
        if(!details.isEmpty()&&ToolStack.isInitialized(details)) {
            int xpLvl=com.titammods.hephaestus_tools.table.ToolXp.getLevel(details);
            centered(g,Component.literal("level: "+xpLvl),140,114,62,.55f,0xEEE6DB);
        } else {
            centered(g,tr("progression_preview"),140,114,62,.55f,0xA2978B);
        }
        atlas(g,BAR_EMPTY,112,123,55,6);
        if(!details.isEmpty()&&ToolStack.isInitialized(details)) {
            int xpInto=com.titammods.hephaestus_tools.table.ToolXp.xpIntoLevel(details);
            int xpNeed=com.titammods.hephaestus_tools.table.ToolXp.xpForLevel(details);
            int xpFilled=xpNeed>0?Math.round(55f*xpInto/xpNeed):0;
            if(xpFilled>0){
                g.enableScissor(leftPos+112,topPos+123,leftPos+112+xpFilled,topPos+123+6);
                atlas(g,BAR_FULL,112,123,55,6);
                g.disableScissor();
            }
        }
        if(tab==2) centered(g,tr("mastery_paths"),MASTERY_CENTER_X,48,68,.65f,0xEEE6DB);
        int count=entryCount();
        if(tab==1) drawModifierScroll(g);
        for(int row=0;row<3 && listOffset+row<count;row++) {
            int index=listOffset+row,y=listY()+row*ROW_STEP,x=listX();
            boolean locked=tab==2 && !com.titammods.hephaestus_tools.table.ToolMastery.selected(details).isEmpty();
            boolean hover=!locked && in(mx,my,x,y,listWidth(),22);
            atlas(g,hover?new int[]{17,111,89,23}:index==selectedEntry?new int[]{17,137,89,23}:new int[]{17,59,89,23},x,y,listWidth(),22);
            icon(g,entryIcon(index),x+3,y+3);
            text(g,entryName(index),x+22,y+8,tab==1?46:listWidth()-26,.6f,locked&&index!=selectedEntry?0x77716B:0xEEE6DB);
            if(tab==1) {
                int level=com.titammods.hephaestus_tools.table.ToolUpgrades.currentLevel(details,upgrades().get(index));
                atlas(g,new int[]{19+18*(Math.max(1,Math.min(5,level))-1),33,15,16},x+72,y+4,12,13);
            }
        }
        if(count==0) wrapped(g,tr(tab==1 && craftableOnly && !details.isEmpty()?"no_craftable_modifiers":"insert_tool"),184,76,80,40);
        text(g,tr(tab==1?"modifier_slots":"mastery_details"),300,51,74,.75f,0xEEE6DB);
        if(tab==1) {
            drawModifierSlots(g);
            atlas(g,new int[]{128,176,10,10},CHECK_X,CHECK_Y,10,10);
            if(craftableOnly) atlas(g,new int[]{128,188,10,10},CHECK_X,CHECK_Y,10,10);
            if(count>0) {
                var up=upgrades().get(selectedEntry);
                boolean max=com.titammods.hephaestus_tools.table.ToolUpgrades.currentLevel(details,up)>=up.maxLevel();
                boolean canApply=minecraft.player!=null && com.titammods.hephaestus_tools.table.ToolUpgrades.canApply(minecraft.player,details,up);
                atlas(g,new int[]{143,canApply&&in(mx,my,APPLY_X,APPLY_Y,APPLY_W,APPLY_H)?190:174,63,14},APPLY_X,APPLY_Y,APPLY_W,APPLY_H);
                centered(g,tr(max?"max_level":"apply_upgrade"),APPLY_X+APPLY_W/2,APPLY_Y+4,55,.6f,canApply?0xEEE6DB:0x77716B);
            }
            return;
        }
        if(count==0) { wrapped(g,tr("insert_tool"),300,70,72,45); return; }
        icon(g,entryIcon(selectedEntry),300,67);
        text(g,entryName(selectedEntry),320,72,54,.65f,0xEEE6DB);
        wrapped(g,entryDescription(),300,91,72,29);
        if(com.titammods.hephaestus_tools.table.ToolMastery.selected(details).isEmpty()) {
            boolean hover=in(mx,my,SELECT_X,SELECT_Y,SELECT_W,SELECT_H);
            atlas(g,new int[]{143,hover?190:174,63,14},SELECT_X,SELECT_Y,SELECT_W,SELECT_H);
            centered(g,tr("select_mastery"),SELECT_X+SELECT_W/2,SELECT_Y+4,55,.6f,0xEEE6DB);
        } else {
            text(g,tr("mastery_levels"),300,130,74,.55f,0xDED1BE);
            if(expandedStats) text(g,tr("mastery_locked"),300,143,70,.55f,0xDED1BE);
        }
    }
    private int listX() { return menu.getActiveTab()==1?LIST_X:MASTERY_LIST_X; }
    private int listY() { return menu.getActiveTab()==1?LIST_Y:MASTERY_LIST_Y; }
    private int listWidth() { return menu.getActiveTab()==1?LIST_W:MASTERY_LIST_W; }
    private int scrollThumbY() {
        return LIST_Y+(entryCount()<=3?0:Math.round(listOffset*(SCROLL_H-THUMB_H)/(float)(entryCount()-3)));
    }
    private void drawModifierScroll(GuiGraphics g) {
        atlas(g,new int[]{146,49,3,80},SCROLL_X+1,LIST_Y,3,SCROLL_H);
        atlas(g,new int[]{152,51,5,7},SCROLL_X,scrollThumbY(),5,THUMB_H);
    }
    private void scrollFromMouse(double my) {
        double fraction=(my-topPos-LIST_Y-scrollGrab)/(SCROLL_H-THUMB_H);
        listOffset=Math.max(0,Math.min(Math.max(0,entryCount()-3),(int)Math.round(fraction*Math.max(0,entryCount()-3))));
    }
    private List<com.titammods.hephaestus_tools.tools.nbt.ToolConstructionData.ModifierEntry> installedModifiers() {
        var unique=new java.util.LinkedHashMap<ResourceLocation,com.titammods.hephaestus_tools.tools.nbt.ToolConstructionData.ModifierEntry>();
        for(var entry:ToolStack.getModifiers(menu.tool())) if(entry.level()>0) unique.put(entry.id(),entry);
        return List.copyOf(unique.values());
    }
    private ResourceLocation installedIcon(ResourceLocation id) {
        if(!id.getNamespace().equals(HephaestusTools.MOD_ID)) return null;
        String name=id.getPath();
        if(name.equals("looting")) name="fortune";
        return List.of("reinforced","haste","fortune","sharpness","silk_touch","flame","sweeping","looting").contains(name)?rl("modifier_icon/modifier_"+(name.equals("silk_touch")?"silky":name.equals("looting")?"fortune":name)+".png"):null;
    }
    private void drawModifierSlots(GuiGraphics g) {
        var installed=installedModifiers();
        int max=com.titammods.hephaestus_tools.table.ToolUpgrades.MAX_MODIFIER_SLOTS;
        text(g,tr("slots_used").copy().append(" "+installed.size()+" / "+max),300,67,74,.65f,0xDED1BE);
        for(int i=0;i<Math.max(max,installed.size());i++) {
            int x=300+(i%5)*15,y=82+(i/5)*17;
            if(y+13>LATERAL_Y+statsHeight()-16) break;
            atlas(g,new int[]{114,97,12,12},x,y,13,13);
            if(i<installed.size()) {
                var texture=installedIcon(installed.get(i).id());
                if(texture!=null) g.blit(texture,leftPos+x+1,topPos+y+1,11,11,0,0,16,16,16,16);
                else centered(g,Component.literal("+"),x+6,y+3,9,.8f,0xDED1BE);
            }
        }
        int ruleY=Math.max(105,82+((Math.max(max,installed.size())+4)/5)*17+6);
        wrapped(g,tr("slots_rule"),300,ruleY,74,Math.max(0,LATERAL_Y+statsHeight()-16-ruleY));
    }
    private void entryTooltip(GuiGraphics g,int index,int mx,int my) {
        var lines=new ArrayList<net.minecraft.util.FormattedCharSequence>();
        lines.addAll(font.split(entryName(index).copy().withStyle(net.minecraft.ChatFormatting.GOLD),180));
        lines.addAll(font.split(entryDescription(index),180));
        if(menu.getActiveTab()==1) {
            var up=upgrades().get(index);
            int level=com.titammods.hephaestus_tools.table.ToolUpgrades.currentLevel(menu.tool(),up);
            lines.add(Component.literal(level+" / "+up.maxLevel()).getVisualOrderText());
            if(level<up.maxLevel()) {
                int owned=com.titammods.hephaestus_tools.table.ToolUpgrades.countIn(minecraft.player,up.costItem());
                Component cost=tr("materials_required").copy().append(": "+up.costFor(level+1)+" x ")
                        .append(new ItemStack(up.costItem()).getHoverName());
                lines.addAll(font.split(cost,180));
                lines.addAll(font.split(tr("materials_owned").copy().append(": "+owned)
                        .withStyle(owned>=up.costFor(level+1)?net.minecraft.ChatFormatting.GREEN:net.minecraft.ChatFormatting.RED),180));
                boolean creative=minecraft.player!=null && minecraft.player.getAbilities().instabuild;
                if(creative) lines.addAll(font.split(tr("creative_materials"),180));
                else if(owned<up.costFor(level+1)) lines.addAll(font.split(
                        Component.translatable("gui.hephaestus_tools.build.materials_missing",up.costFor(level+1)-owned)
                                .withStyle(net.minecraft.ChatFormatting.RED),180));
                if(level==0 && com.titammods.hephaestus_tools.table.ToolUpgrades.usedSlots(menu.tool())>=com.titammods.hephaestus_tools.table.ToolUpgrades.MAX_MODIFIER_SLOTS)
                    lines.addAll(font.split(tr("no_modifier_slots").copy().withStyle(net.minecraft.ChatFormatting.RED),180));
                if(com.titammods.hephaestus_tools.table.ToolUpgrades.canApply(minecraft.player,menu.tool(),up))
                    lines.addAll(font.split(tr("ready_to_apply").copy().withStyle(net.minecraft.ChatFormatting.GREEN),180));
            } else lines.add(tr("max_level").getVisualOrderText());
        } else {
            String chosen=com.titammods.hephaestus_tools.table.ToolMastery.selected(menu.tool());
            lines.addAll(font.split(tr(chosen.isEmpty()?"mastery_permanent":"mastery_locked")
                    .copy().withStyle(net.minecraft.ChatFormatting.GOLD),180));
            var requirement=Component.translatable("gui.hephaestus_tools.build.mastery_requires_level",com.titammods.hephaestus_tools.table.MasteryLevel.T1);
            if(com.titammods.hephaestus_tools.table.ToolXp.getLevel(menu.tool())<com.titammods.hephaestus_tools.table.MasteryLevel.T1) requirement.withStyle(net.minecraft.ChatFormatting.RED);
            lines.addAll(font.split(requirement,180));
        }
        g.renderTooltip(font,lines,mx,my);
    }
    private boolean progressionClick(double mx,double my) {
        refreshEntries();
        if(menu.getActiveTab()==1 && in(mx,my,CHECK_X,CHECK_Y,10,10)) {
            craftableOnly=!craftableOnly; listOffset=0; draggingModifiers=false;
            refreshEntries(); return true;
        }
        if(menu.getActiveTab()==2 && entryCount()>0 && in(mx,my,SELECT_X,SELECT_Y,SELECT_W,SELECT_H)) {
            if(com.titammods.hephaestus_tools.table.ToolMastery.selected(menu.tool()).isEmpty())
                send(ArsenalTableMenu.MASTERY_SELECT_BASE+selectedEntry);
            return true;
        }
        if(menu.getActiveTab()==1 && in(mx,my,SCROLL_X-1,LIST_Y,7,SCROLL_H)) {
            if(entryCount()>3) {
                scrollGrab=in(mx,my,SCROLL_X-1,scrollThumbY(),7,THUMB_H)?my-topPos-scrollThumbY():THUMB_H/2.0;
                draggingModifiers=true; scrollFromMouse(my);
            }
            return true;
        }
        if(in(mx,my,STATS_W-20,LATERAL_Y+(expandedLeftStats?128:107)-13,16,8)) { expandedLeftStats=!expandedLeftStats; return true; }
        if(in(mx,my,collapseX(),collapseY(),16,8)) { expandedStats=!expandedStats; return true; }
        for(int row=0;row<3 && listOffset+row<entryCount();row++) if(in(mx,my,listX(),listY()+row*ROW_STEP,listWidth(),22)) {
            if(menu.getActiveTab()==2 && !com.titammods.hephaestus_tools.table.ToolMastery.selected(menu.tool()).isEmpty()) return true;
            selectedEntry=listOffset+row; return true;
        }
        if(menu.getActiveTab()==1 && entryCount()>0 && in(mx,my,APPLY_X,APPLY_Y,APPLY_W,APPLY_H)) {
            var chosen=upgrades().get(selectedEntry);
            int serverIndex=com.titammods.hephaestus_tools.table.ToolUpgrades.availableFor(menu.tool().getItem()).indexOf(chosen);
            if(serverIndex>=0 && com.titammods.hephaestus_tools.table.ToolUpgrades.canApply(minecraft.player,menu.tool(),chosen))
                send(ArsenalTableMenu.APPLY_BASE+serverIndex);
            return true;
        }
        return false;
    }
    private void progressionTooltip(GuiGraphics g,int mx,int my) {
        if(!menu.tool().isEmpty() && (in(mx,my,135,52,36,18) || in(mx,my,123,77,34,34))) {
            g.renderTooltip(font,menu.tool(),mx,my); return;
        }
        if(menu.getActiveTab()==1 && entryCount()>0 && in(mx,my,APPLY_X,APPLY_Y,APPLY_W,APPLY_H)) {
            entryTooltip(g,selectedEntry,mx,my); return;
        }
        if(menu.getActiveTab()==1 && in(mx,my,CHECK_X,CHECK_Y,10,10)) {
            g.renderTooltip(font,tr("filter_craftable"),mx,my); return;
        }
        if(menu.getActiveTab()==2 && entryCount()>0 && in(mx,my,SELECT_X,SELECT_Y,SELECT_W,SELECT_H)
                && com.titammods.hephaestus_tools.table.ToolMastery.selected(menu.tool()).isEmpty()) {
            g.renderTooltip(font,font.split(tr("mastery_permanent"),180),mx,my); return;
        }
        if(in(mx,my,collapseX(),collapseY(),16,8)) { g.renderTooltip(font,tr(expandedStats?"collapse":"expand"),mx,my); return; }
        if(in(mx,my,STATS_W-20,LATERAL_Y+(expandedLeftStats?128:107)-13,16,8)) {
            g.renderTooltip(font,tr(expandedLeftStats?"collapse":"expand"),mx,my); return;
        }
        for(int row=0;row<3 && listOffset+row<entryCount();row++)
            if(in(mx,my,listX(),listY()+row*ROW_STEP,listWidth(),22)) { entryTooltip(g,listOffset+row,mx,my); return; }
        if(in(mx,my,12,114,74,(expandedLeftStats?128:107)-77) && !traitDetails.isEmpty()) {
            g.renderComponentTooltip(font,traitDetails,mx,my); return;
        }
        if(menu.getActiveTab()==1) {
            var installed=installedModifiers();
            for(int i=0;i<installed.size();i++) {
                int y=82+(i/5)*17;
                if(y+13>LATERAL_Y+statsHeight()-16) break;
                if(in(mx,my,300+(i%5)*15,y,13,13)) {
                    var e=installed.get(i);
                    String key="modifier."+e.id().getNamespace()+"."+e.id().getPath();
                    var up=com.titammods.hephaestus_tools.table.ToolUpgrades.find(e.id());
                    Component name=Component.translatable(up.isPresent()?up.get().nameKey():key);
                    g.renderTooltip(font,name.copy().append(" "+e.level()),mx,my); return;
                }
            }
        } else if(entryCount()>0 && in(mx,my,300,65,74,68)) {
            entryTooltip(g,selectedEntry,mx,my);
        }
    }
    @Override public boolean mouseDragged(double mx,double my,int button,double dx,double dy) {
        if(button==0 && draggingModifiers && menu.getActiveTab()==1) { scrollFromMouse(my); return true; }
        return super.mouseDragged(mx,my,button,dx,dy);
    }
    @Override public boolean mouseReleased(double mx,double my,int button) {
        if(button==0 && draggingModifiers) { draggingModifiers=false; return true; }
        return super.mouseReleased(mx,my,button);
    }
    @Override public boolean mouseScrolled(double mx,double my,double horizontal,double vertical) {
        if(menu.getActiveTab()!=0 && in(mx,my,listX(),listY(),101,70)) {
            listOffset=Math.max(0,Math.min(Math.max(0,entryCount()-3),listOffset-(int)Math.signum(vertical)));
            return true;
        }
        return super.mouseScrolled(mx,my,horizontal,vertical);
    }
}