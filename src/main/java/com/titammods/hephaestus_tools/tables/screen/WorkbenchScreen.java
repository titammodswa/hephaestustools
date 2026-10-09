package com.titammods.hephaestus_tools.tables.screen;

import com.titammods.hephaestus_tools.HephaestusTools;
import com.titammods.hephaestus_tools.client.renderer.ToolItemRenderer;
import com.titammods.hephaestus_tools.client.workbench.WorkbenchAssembly;
import com.titammods.hephaestus_tools.client.workbench.WorkbenchTuningStore;
import com.titammods.hephaestus_tools.network.CraftAnimPayload;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import com.titammods.hephaestus_tools.client.workbench.WorkbenchCraft;
import com.titammods.hephaestus_tools.client.workbench.WorkbenchSession;
import com.titammods.hephaestus_tools.materials.MaterialManager;
import com.titammods.hephaestus_tools.materials.trait.MaterialTrait;
import com.titammods.hephaestus_tools.mixin.client.SlotAccessor;
import com.titammods.hephaestus_tools.table.ToolAssembly;
import com.titammods.hephaestus_tools.table.MasteryLevel;
import com.titammods.hephaestus_tools.table.ToolMastery;
import com.titammods.hephaestus_tools.table.ToolRole;
import com.titammods.hephaestus_tools.table.ToolUpgrade;
import com.titammods.hephaestus_tools.table.ToolUpgrades;
import com.titammods.hephaestus_tools.table.ToolXp;
import com.titammods.hephaestus_tools.tables.menu.WorkbenchMenu;
import com.titammods.hephaestus_tools.tools.helper.ToolBuildHandler;
import com.titammods.hephaestus_tools.tools.nbt.ToolConstructionData;
import com.titammods.hephaestus_tools.tools.nbt.ToolStack;
import com.titammods.hephaestus_tools.tools.part.ToolPartItem;
import com.titammods.hephaestus_tools.materials.MaterialId;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

public class WorkbenchScreen extends AbstractContainerScreen<WorkbenchMenu> {
    private static final int EDGE = 0xFF373737, EDGE_DIM = 0xFF2B2B2B;
    private static final int ACCENT = 0xFFFFAA00, ACCENT_DIM = 0xFFAA7700, HOLO = 0xFF55FFFF;
    private static final int TEXT = 0xFFFFFFFF, TEXT_DIM = 0xFFAAAAAA, TEXT_FAINT = 0xFF777777, WHITE = 0xFFFFFFFF;
    private static final int GOOD = 0xFF55FF55, BAD = 0xFFFF5555, RIBBON_TEXT = 0xFFFFFFFF, RIBBON_SUB = 0xFFFFFFFF;

    private static Identifier spr(String name) { return Identifier.fromNamespaceAndPath(HephaestusTools.MOD_ID, "workbench/" + name); }
    private static final Identifier SPR_PANEL = spr("panel"), SPR_PANEL_DARK = spr("panel_dark"), SPR_SLOT = spr("slot");
    private static final Identifier SPR_DOCK = spr("dock_frame"), SPR_BUTTON = spr("button"), SPR_BUTTON_HOVER = spr("button_hover"), SPR_BUTTON_OFF = spr("button_disabled");
    private static final Identifier SPR_READY = spr("button_ready"), SPR_READY_HOVER = spr("button_ready_hover");
    private static final Identifier SPR_TAB = spr("tab"), SPR_TAB_HOVER = spr("tab_hover"), SPR_TAB_ON = spr("tab_selected");
    private static final Identifier HOTBAR_SPRITE = Identifier.withDefaultNamespace("hud/hotbar");
    private static final Identifier HOTBAR_SELECTION_SPRITE = Identifier.withDefaultNamespace("hud/hotbar_selection");
    private static final Identifier SPR_DARK_PANEL = spr("panel_darkmode"), SPR_DARK_SLOT = spr("slot_darkmode");
    private static final Identifier SPR_HEADER = spr("header"), SPR_BAND = spr("band"), SPR_STITCH = spr("stitch");
    private static final Identifier SPR_CHECK = spr("check"), SPR_CROSS = spr("cross");
    private static final Identifier SPR_KEY = spr("key"), SPR_BAR = spr("bar"), SPR_ROW = spr("row"), SPR_ROW_ON = spr("row_selected");

    private static final int MARGIN = 12, PAD = 10;
    private static final int TOP_Y = 8, TAB_Y = TOP_Y, TAB_W = 22, TAB_H = 22, TAB_GAP = 4, SIDE_Y = 52;
    private static final int LEFT_W = 122;
    private static final int RIGHT_W = 124;
    private static final int TOOL_CELL = 25, TOOL_COLS = 4, HEADER_H = 18, PART_ROW = 30;
    private static final int ROW_H = 16, ROW_STEP = 17, POP_W = 150, POP_ROWS = 4, CARD_GAP = 10;
    private static final int SLOT_W = 22, SLOT_H = 22;
    private static final int OUT_SIZE = 22, CRAFT_W = 72, SIDE_W = 72, DRAWER_W = 162, DRAWER_H = 54;
    private static final int HOTBAR_H = 22;
    private static final int BTN_TEXT = 0xFFFFF1E6, BTN_SHADOW = 0xFF4A1C12;
    private static final float TEXT_BOOST = 1.25f;

    private boolean craftableOnly;
    private boolean toolsOpen;
    private boolean inspect;
    private boolean hideUi;
    private int fillModY = -1;
    private boolean rotating;
    private int rightTab;
    private boolean draggingScroll;
    private int selectedEntry = 0, listOffset = 0, lastTab = -1;
    private long frameId, bgFrame = -1;
    private Item lastTool;
    private List<ToolUpgrade> visibleUpgrades = List.of();
    private ItemStack preview = ItemStack.EMPTY, details = ItemStack.EMPTY;
    private final List<Component> traitDetails = new ArrayList<>();
    private final List<Component[]> traitRows = new ArrayList<>();
    private final List<int[]> tipRects = new ArrayList<>();
    private final List<List<Component>> tipTexts = new ArrayList<>();
    private final List<Tip> tipRich = new ArrayList<>();
    private int tipShown = -1;
    private boolean morphWas;
    private float morphAge;
    private float tipT = 0f;
    private ItemStack compare = ItemStack.EMPTY;

    private int socketOpen = -1;
    private long lastMs = -1L;
    private float dt, drawerT = -1f, tabT = 1f;
    private final float[] cardLift = new float[3];
    private boolean carriedBefore;

    private static float ease(float t) {
        t = Mth.clamp(t, 0f, 1f);
        return t * t * (3f - 2f * t);
    }

    private static float outCubic(float t) {
        t = Mth.clamp(t, 0f, 1f);
        float u = 1f - t;
        return 1f - u * u * u;
    }

    private static float outBack(float t) {
        t = Mth.clamp(t, 0f, 1f);
        float c1 = 1.25f, c3 = c1 + 1f, u = t - 1f;
        return 1f + c3 * u * u * u + c1 * u * u;
    }

    private static Identifier rl(String name) {
        return Identifier.fromNamespaceAndPath(HephaestusTools.MOD_ID, "textures/gui/forge_table/" + name);
    }

    private static Component tr(String key) { return Component.translatable("gui.hephaestus_tools.build." + key); }

    public WorkbenchScreen(WorkbenchMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override protected void init() {
        super.init();
        fitToWindow();
    }

    private void fitToWindow() {
        setIntField("imageWidth", width);
        setIntField("imageHeight", height);
        leftPos = 0;
        topPos = 0;
        layoutSlots();
    }

    private void setIntField(String name, int value) {
        try {
            java.lang.reflect.Field field = AbstractContainerScreen.class.getDeclaredField(name);
            field.setAccessible(true);
            field.setInt(this, value);
        } catch (Exception ignored) {
        }
    }

    @Override
    protected boolean hasClickedOutside(double mx, double my, int xo, int yo) {
        return mx < 0 || my < 0 || mx >= width || my >= height;
    }

    private static final int FILL_LINK = 0xFFFFD23F, FILL_HOVER = 0xFFFFF29A;
    private static final int TABLE_GUI_SCALE = 3;
    private int savedGuiScale = -1;

    @Override public void added() {
        super.added();
        applyTableGuiScale();
        WorkbenchSession.start(menu.getBlockEntity(), menu.getOrigin());
    }

    @Override public void removed() {
        WorkbenchSession.stop();
        restoreGuiScale();
        super.removed();
    }

    private void applyTableGuiScale() {
        var option = minecraft.options.guiScale();
        if (savedGuiScale < 0) savedGuiScale = option.get();
        if (option.get() != TABLE_GUI_SCALE) option.set(TABLE_GUI_SCALE);
    }

    private void restoreGuiScale() {
        if (savedGuiScale < 0) return;
        var option = minecraft.options.guiScale();
        int original = savedGuiScale;
        savedGuiScale = -1;
        if (option.get() != original) option.set(original);
    }

    @Override public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        int w = g.guiWidth(), h = g.guiHeight();
        int band = h / 5;
        g.fillGradient(0, 0, w, band, 0x90000000, 0x00000000);
        g.fillGradient(0, h - band, w, h, 0x00000000, 0x90000000);
        final int steps = 20, side = w / 6;
        for (int i = 0; i < steps; i++) {
            double k = 1.0 - i / (double) steps;
            int color = ((int) (0x70 * k * k)) << 24;
            int x0 = side * i / steps, x1 = side * (i + 1) / steps;
            g.fill(x0, 0, x1, h, color);
            g.fill(w - x1, 0, w - x0, h, color);
        }
        if (minecraft != null && minecraft.player != null && !minecraft.options.hideGui && !WorkbenchCraft.isBusy()) {
            g.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SPRITE, w / 2 - 91, h - 22, 182, 22);
            g.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SELECTION_SPRITE, w / 2 - 91 - 1 + minecraft.player.getInventory().getSelectedSlot() * 20, h - 22 - 1, 24, 23);
        }
        drawBg(g, partialTick, mouseX, mouseY);
    }

    private static final int PAGE_TOP = 30, PP = 11, ROW = 29, CELL = 24;
    private static final int INK = 0xFFE3E3E3, INK_HEAD = 0xFFDAC6C0, INK2 = 0xFF9A8F8C, INK3 = 0xFF6E6563;
    private static final int INK_ORANGE = 0xFFDE8A5C, INK_GREEN = 0xFF8FC48A, INK_RED = 0xFFD97A6E, INK_BLUE = 0xFF8FB0CC;
    private static final Identifier SPR_DSLOT_HOVER = spr("drawer_slot_hover"), TEX_DRAWER = rl("inventory_drawer.png");
    private static final Identifier SPR_PAGE = spr("page"), SPR_PTAB = spr("page_tab"), SPR_PTAB_ON = spr("page_tab_selected");
    private static final Identifier SPR_BAR_FILL = spr("bar_fill"), SPR_PSLOT_SEL = spr("paper_slot_selected"), SPR_PIP_USED = spr("slot_pip_used"), SPR_PIP_FREE = spr("slot_pip_free"), SPR_PIP_LOCKED = spr("slot_pip_locked"), SPR_MLINK = spr("mastery_link"), SPR_MLINK_ON = spr("mastery_link_on"), SPR_SCROLL_TRACK = spr("scroll_track"), SPR_SCROLL_THUMB = spr("scroll_thumb");
    private static final Identifier SPR_PSLOT = spr("paper_slot"), SPR_PSLOT_LOCKED = spr("paper_slot_locked"), SPR_LOCK = spr("icon_lock"), SPR_PSLOT_HOVER = spr("paper_slot_hover");
    private static final Identifier SPR_PROW_ON = spr("paper_row_selected"), SPR_PROW_HOVER = spr("paper_row_hover");
    private static final String[] TAB_KEYS = {"tab_build", "tab_modify", "tab_mastery"};

    private float pagesT = 0f;
    private float leftHt = 120f, rightHt = 120f;
    private int measureBottom, actY = 200;
    private void measure(int bottom) { measureBottom = Math.max(measureBottom, bottom); }
    private final float[] rowHover = new float[8];
    private final float[] flash = new float[8];
    private final java.util.Map<Identifier, Integer> lastLevels = new java.util.HashMap<>();
    private final List<float[]> sparks = new ArrayList<>();

    private int cx() { return width / 2; }
    private int pageW() { return Mth.clamp((width - 250) / 2 - 12, 116, 148); }
    private int maxPageH() { return height - PAGE_TOP - HOTBAR_H - 16 - hintsH(); }
    private boolean showRight() { return menu.getActiveTab() == 0 || toolReady(); }
    private int leftPageH() { return Math.min(maxPageH(), Math.round(leftHt)); }
    private int rightPageH() { return Math.min(maxPageH(), Math.round(rightHt)); }
    private int pageShift() { return Math.round((1f - outBack(pagesT)) * (pageW() + 30)); }
    private static final int HALF_GAP = 150;
    private int lpX() { return Math.max(12, cx() - HALF_GAP - pageW()) - pageShift(); }
    private int rpX() { return Math.min(width - 12 - pageW(), cx() + HALF_GAP) + pageShift(); }
    private int midY() { return (height - HOTBAR_H - 8) / 2; }
    private int lTop() { return Mth.clamp(midY() - leftPageH() / 2, PAGE_TOP, Math.max(PAGE_TOP, height - HOTBAR_H - 12 - leftPageH())); }
    private int rTop() { return Mth.clamp(midY() - rightPageH() / 2, 12, Math.max(12, height - HOTBAR_H - 16 - hintsH() - rightPageH())); }
    private int inW() { return pageW() - 2 * PP; }
    private int lIn() { return lpX() + PP; }
    private int rIn() { return rpX() + PP; }
    private int pBottom() { return rTop() + maxPageH() - PP; }
    private static final int HEAD_TEXT = 30;
    private int headSlotX() { return rIn(); }
    private int headSlotY() { return rTop() + PP; }
    private static final int HG = 4;
    private int bodyY() { return rTop() + PP + 36; }
    private int actionY() { return actY; }
    private int placeAction() { actY = Math.min(measureBottom + 6, pBottom() - 18); return actY; }
    private static final int MOD_ROWS = 5, SCROLL_W = 5;
    private boolean scrollDrag = false;
    private boolean needScroll() { return menu.getActiveTab() == 1 && entryCount() > MOD_ROWS; }
    private int listW() { return inW() - (needScroll() ? SCROLL_W + 4 : 0); }
    private int rowY(int i) { return lTop() + PP + 16 + HG + (i - listOffset) * ROW; }
    private int listH() { return MOD_ROWS * ROW - 2; }
    private int maxOffset() { return Math.max(0, entryCount() - MOD_ROWS); }
    private void ensureVisible() {
        if (selectedEntry < listOffset) listOffset = selectedEntry;
        else if (selectedEntry >= listOffset + MOD_ROWS) listOffset = selectedEntry - MOD_ROWS + 1;
        listOffset = Mth.clamp(listOffset, 0, maxOffset());
    }
    private int thumbH() { return Math.max(12, listH() * MOD_ROWS / Math.max(1, entryCount())); }
    private int thumbY() { return lTop() + PP + 14 + HG + (maxOffset() == 0 ? 0 : (listH() - thumbH()) * listOffset / maxOffset()); }
    private void scrollTo(double my) {
        int track = listH() - thumbH();
        if (track <= 0) return;
        double f = (my - (lTop() + PP + 14 + HG) - thumbH() / 2.0) / track;
        listOffset = Mth.clamp((int) Math.round(f * maxOffset()), 0, maxOffset());
    }
    private void drawScrollbar(GuiGraphicsExtractor g, int mx, int my) {
        if (!needScroll()) return;
        int sx = lIn() + inW() - SCROLL_W, sy = lTop() + PP + 14 + HG;
        g.blitSprite(RenderPipelines.GUI_TEXTURED, SPR_SCROLL_TRACK, sx + 1, sy, 3, listH());
        boolean hot = scrollDrag || in(mx, my, sx - 2, sy, SCROLL_W + 4, listH());
        g.blitSprite(RenderPipelines.GUI_TEXTURED, SPR_SCROLL_THUMB, sx, thumbY(), SCROLL_W, thumbH());
        if (hot) fill(g, sx, thumbY(), SCROLL_W, thumbH(), 0x30FFFFFF);
    }
    private int bagX() { return cx() - 91 - 26; }
    private int bagY() { return height - 22; }
    private boolean pagesOpen() { return pagesT > 0.9f; }

    private int tabW(int i) { return (pageW() - 5) / 3; }
    private int tabX(int i) {
        int x = lpX() + 2;
        for (int k = 0; k < i; k++) x += tabW(k) + 2;
        return x;
    }

    private int partCount() { return menu.isRepair() ? 1 : Math.max(1, Math.min(4, menu.parts().size())); }
    private int toolHeadY() { return lTop() + PP; }
    private int recipeY() { return lTop() + PP + 30; }
    private int partRowY(int i) { return recipeY() + 16 + HG + i * ROW; }
    private int toolCols() { return Math.max(1, inW() / CELL); }
    private int toolCellX(int i) { return lIn() + (i % toolCols()) * CELL; }
    private int toolCellY(int i) { return recipeY() + 16 + HG + (i / toolCols()) * CELL; }
    private int fillY() { return partRowY(partCount()) + 1; }

    private int gridCols() { return Math.max(1, inW() / CELL); }
    private int gridX(int k) { return rIn() + (k % gridCols()) * CELL; }
    private int gridY(int k) { return bodyY() + 15 + HG + (k / gridCols()) * CELL; }

    private static final int MROW = 36;
    private int mRowY(int i) { return lTop() + PP + 16 + HG + i * MROW; }

    private int drawerX() { return Math.max(13, bagX() - 6 - DRAWER_W - 7); }
    private int drawerY() { return bagY() + 20 - DRAWER_H - 7; }

    private static final int DOCK_H = 22, DOCK_BTN_W = 78, DOCK_GAP = 4, DOCK_PAD = 6;
    private int dockX() { return cx() - (22 + DOCK_GAP + DOCK_BTN_W) / 2; }
    private int dockY() { return height - HOTBAR_H - 12 - DOCK_H; }
    private int dockBtnX() { return dockX() + 22 + DOCK_GAP; }
    private boolean dockBtnHit(double mx, double my) { return pagesT > 0.05f && in(mx, my, dockBtnX(), dockY(), DOCK_BTN_W, DOCK_H); }
    private int invX() { return drawerX() + 1; }
    private int invY() { return drawerY() + 1; }

    private void layoutSlots() {
        if (menu.slots.size() < 42) return;
        int off = -3000, tab = menu.getActiveTab();
        boolean on = pagesT > 0.02f;
        place(0, on && tab != 0 ? dockX() + 3 : off, on && tab != 0 ? dockY() + 3 : off);
        for (int i = 0; i < 4; i++) place(i + 1, off, off);
        if (on && tab == 0 && repairVisible() && !toolsOpen) place(1, lIn() + 3, partRowY(0) + 3);
        place(5, on && tab == 0 ? dockX() + 3 : off, on && tab == 0 ? dockY() + 3 : off);
        int slide = Math.round((1f - ease(Math.max(0f, drawerT))) * 14f);
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            place(6 + col + row * 9, invX() + col * 18, invY() + row * 18 + slide);
        for (int col = 0; col < 9; col++) place(33 + col, cx() - 88 + col * 20, height - 19);
    }

    private void page(GuiGraphicsExtractor g, int x, int top, int h) { g.blitSprite(RenderPipelines.GUI_TEXTURED, SPR_PAGE, x - 5, top - 4, pageW() + 10, h + 8); }

    private void pslot(GuiGraphicsExtractor g, int x, int y, int s, boolean hover) { g.blitSprite(RenderPipelines.GUI_TEXTURED, hover ? SPR_PSLOT_HOVER : SPR_PSLOT, x, y, s, s); measure(y + s); }

    private void heading(GuiGraphicsExtractor g, int x, int y, int w, Component title, Component right) {
        int rw = right == null ? 0 : Math.min(w / 2, Math.round(font.width(right) * .66f * TEXT_BOOST) + 2);
        text(g, title, x, y, w - rw - 4, .74f, INK_HEAD);
        if (right != null) textRight(g, right, x + w, y + 1, rw, .66f, INK_ORANGE);
        fill(g, x, y + 11, w, 1, 0xFF121010);
        fill(g, x, y + 12, w, 1, 0x1CFFFFFF);
    }

    private void rowBg(GuiGraphicsExtractor g, int i, int x, int y, int w, int h, boolean selected, boolean hover) {
        measure(y + h);
        if (i >= 0 && i < rowHover.length) rowHover[i] += ((hover ? 1f : 0f) - rowHover[i]) * Math.min(1f, dt * 14f);
        float hv = i >= 0 && i < rowHover.length ? rowHover[i] : hover ? 1f : 0f;
        g.blitSprite(RenderPipelines.GUI_TEXTURED, SPR_PROW_HOVER, x, y, w, h);
        if (selected) g.blitSprite(RenderPipelines.GUI_TEXTURED, SPR_PROW_ON, x, y, w, h);
        else if (hv > 0.02f) {
            g.blitSprite(RenderPipelines.GUI_TEXTURED, SPR_PROW_ON, x, y, w, h, argb(hv * 0.55f, 1f, 1f, 1f));
        }
    }

    private void inkSprite(GuiGraphicsExtractor g, Identifier sprite, int x, int y, int size) {
        g.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, size, size, argb(1f, 0.9f, 0.82f, 0.74f));
    }

    private void paperBar(GuiGraphicsExtractor g, int x, int y, int w, float frac, int color) {
        g.blitSprite(RenderPipelines.GUI_TEXTURED, SPR_BAR, x, y, w, 6);
        int f = Math.round((w - 2) * Mth.clamp(frac, 0f, 1f));
        if (f > 0) g.blitSprite(RenderPipelines.GUI_TEXTURED, SPR_BAR_FILL, x + 1, y + 1, Math.max(f, 6), 4);
    }

    private void labelBox(GuiGraphicsExtractor g, Component label, int centerX, int y, float scale, int color) {
        centered(g, label, centerX, y + 6, width - 40, scale, color == HOLO ? 0xFF55FFFF : WHITE);
    }

    private List<Component[]> hintLines() {
        List<Component[]> out = new ArrayList<>();
        if (readyToCraft()) out.add(new Component[] {Component.literal("Space"), tr("hint_craft_short")});
        if (hideUi) {
            out.add(new Component[] {Component.literal("H"), tr("hint_show_short")});
            out.add(new Component[] {Component.literal("Esc"), tr("hint_exit_short")});
            return out;
        }
        if (canFill() || canFillModifier()) out.add(new Component[] {Component.literal("G"), tr("hint_fill_short")});
        if (hasSubject()) out.add(new Component[] {Component.literal("F"), tr("hint_inspect_short")});
        out.add(new Component[] {Component.literal("E"), tr(menu.isInventoryVisible() ? "hint_close_short" : "hint_bag_short"), Component.literal("H"), tr("hint_hide_short")});
        out.add(new Component[] {Component.literal("Tab"), tr("hint_tab_short")});
        out.add(new Component[] {Component.literal("Esc"), tr("hint_exit_short")});
        return out;
    }

    private static final int KEY_COLOR = 0xFFF0804F;
    private Component hintLine(Component[] line) {
        Component out = Component.literal("[" + line[0].getString() + "] ").withColor(KEY_COLOR & 0xFFFFFF)
                .append(line[1].copy().withColor(0xFFFFFF));
        if (line.length >= 4) out = out.copy().append(Component.literal("   [" + line[2].getString() + "] ").withColor(KEY_COLOR & 0xFFFFFF))
                .append(line[3].copy().withColor(0xFFFFFF));
        return out;
    }
    private int hintsH() { return hintLines().size() * 10; }
    private int hintsY() { return height - 12 - hintsH(); }

    private void drawCornerHints(GuiGraphicsExtractor g) {
        int y = hintsY();
        for (Component[] line : hintLines()) {
            textRight(g, hintLine(line), width - 12, y, 200, .62f, WHITE);
            y += 10;
        }
    }

    private void drawBg(GuiGraphicsExtractor g, float tick, int mx, int my) {
        if (bgFrame == frameId) return;
        bgFrame = frameId;
        tipRects.clear();
        tipTexts.clear();
        tipRich.clear();
        int tab = menu.getActiveTab();
        if (lastTab != tab || lastTool != menu.tool().getItem()) {
            selectedEntry = 0; listOffset = 0; visibleUpgrades = List.of();
            if (lastTab != tab) tabT = 0f;
            lastTab = tab; lastTool = menu.tool().getItem();
        }
        if (tab != 0) refreshEntries();
        preview = tab == 0 ? menu.preview() : ItemStack.EMPTY;
        details = tab == 0
                ? (!menu.output().isEmpty() ? menu.output() : !preview.isEmpty() ? preview : ItemStack.EMPTY)
                : menu.tool();

        if (WorkbenchCraft.isBusy()) { drawCraftProgress(g); return; }
        if (inspect) labelBox(g, tr("inspect_hint2"), cx(), 6, .75f, TEXT);

        if (pagesT > 0.01f) {
            drawTabs(g, mx, my, false);
            drawTabs(g, mx, my, true);
            page(g, lpX(), lTop(), leftPageH());
            if (showRight()) page(g, rpX(), rTop(), rightPageH());
            int slide = Math.round((1f - outCubic(tabT)) * 12f);
            g.pose().pushMatrix();
            g.pose().translate(-slide, 0);
            measureBottom = lTop() + 40;
            if (tab == 0) drawBuildLeft(g, mx, my);
            else if (tab == 1) drawModifyLeft(g, mx, my);
            else drawMasteryLeft(g, mx, my);
            g.pose().popMatrix();
            leftHt += (measureBottom + PP - lTop() - leftHt) * Math.min(1f, dt * 12f);
            if (showRight()) {
                g.pose().pushMatrix();
                g.pose().translate(slide, 0);
                measureBottom = rTop() + 40;
                g.enableScissor(rpX() - 16, rTop() - 4, rpX() + pageW() + 16, rTop() + rightPageH() - 3);
                if (tab == 0) drawBuildRight(g, mx, my);
                else if (tab == 1) drawModifyRight(g, mx, my);
                else drawMasteryRight(g, mx, my);
                g.disableScissor();
                g.pose().popMatrix();
            }
            rightHt += (measureBottom + PP - rTop() - rightHt) * Math.min(1f, dt * 12f);
            drawSparks(g);
            drawDock(g, mx, my);
        }
        drawCornerHints(g);
        if (!hideUi) drawBag(g, mx, my);
        if (tab == 0 && !inspect) drawPartLabel(g);
        drawDrawer(g, mx, my);
    }

    private void tickAnimations() {
        long now = net.minecraft.util.Util.getMillis();
        dt = lastMs < 0 ? 0f : Mth.clamp((now - lastMs) / 1000f, 0f, 0.1f);
        lastMs = now;
        if (drawerT < 0f) drawerT = menu.isInventoryVisible() ? 1f : 0f;
        drawerT = Mth.clamp(drawerT + (menu.isInventoryVisible() ? 1f : -1f) * dt / 0.22f, 0f, 1f);
        tabT = Math.min(1f, tabT + dt / 0.35f);
        boolean hide = inspect || hideUi || WorkbenchCraft.isBusy();
        pagesT = Mth.clamp(pagesT + (hide ? -1f : 1f) * dt / 0.32f, 0f, 1f);
        for (int i = 0; i < flash.length; i++) flash[i] = Math.max(0f, flash[i] - dt / 0.7f);
    }

    private void drawTabs(GuiGraphicsExtractor g, int mx, int my, boolean activeOnly) {
        int ty = lTop() - 19;
        for (int i = 0; i < 3; i++) {
            boolean on = menu.getActiveTab() == i;
            if (on != activeOnly) continue;
            int x = tabX(i), w = tabW(i);
            boolean hover = in(mx, my, x, ty, w, 15);
            g.blitSprite(RenderPipelines.GUI_TEXTURED, on ? SPR_PTAB_ON : hover ? SPR_TAB_HOVER : SPR_PTAB, x, ty, w, 18);
            centered(g, tr(TAB_KEYS[i]), x + w / 2, ty + 6, w - 4, .72f, on ? WHITE : INK);
        }
    }

    private void drawDock(GuiGraphicsExtractor g, int mx, int my) {
        int tab = menu.getActiveTab();
        int sx = dockX(), sy = dockY();
        g.blitSprite(RenderPipelines.GUI_TEXTURED, SPR_DOCK, sx - DOCK_PAD, sy - DOCK_PAD, 22 + DOCK_GAP + DOCK_BTN_W + DOCK_PAD * 2, DOCK_H + DOCK_PAD * 2);
        Slot slot = menu.slots.get(tab == 0 ? 5 : 0);
        ItemStack carried = menu.getCarried();
        boolean hover = in(mx, my, sx, sy, 22, 22);
        g.blitSprite(RenderPipelines.GUI_TEXTURED, hover ? SPR_PSLOT_HOVER : SPR_PSLOT, sx, sy, 22, 22);
        boolean accepts = !carried.isEmpty() && slot.getItem().isEmpty() && slot.mayPlace(carried);
        boolean ready = tab == 0 && readyToCraft();
        if (tab == 0 && slot.getItem().isEmpty() && ready) {
            g.item(preview, sx + 3, sy + 3);
            g.pose().pushMatrix();
            g.pose().translate(0, 0);
            g.fill(sx + 3, sy + 3, sx + 19, sy + 19, 0x70201C1C);
            g.pose().popMatrix();
        }
        boolean done = tab == 0 && !menu.output().isEmpty();
        if (slot.getItem().isEmpty()) tip(sx, sy, 22, 22, tr("tool"), tr(tab != 0 ? "insert_tool" : done ? "take_hint" : ready ? "craft_hint" : "tool_slot_hint"));
        int bx = dockBtnX(), bw = DOCK_BTN_W;
        boolean bh = in(mx, my, bx, sy, bw, DOCK_H);
        if (tab == 0) {
            if (menu.isRepair() && !menu.input(0).isEmpty() && !(menu.input(0).getItem() instanceof ToolPartItem)) {
                boolean ok = canRepairNow();
                readyButton(g, bx, sy, bw, DOCK_H, ok, ok && bh, tr("repair_button"));
                tip(bx, sy, bw, DOCK_H, tr("repair_button_hint"));
                return;
            }
            if (menu.isRepair() && !done) return;
            if (done) { readyButton(g, bx, sy, bw, DOCK_H, true, bh, tr("take")); tip(bx, sy, bw, DOCK_H, tr("take_hint")); }
            else { readyButton(g, bx, sy, bw, DOCK_H, ready, ready && bh, tr("craft")); if (ready) tip(bx, sy, bw, DOCK_H, tr("craft_hint")); }
        } else if (tab == 1) {
            if (!toolReady()) { readyButton(g, bx, sy, bw, DOCK_H, false, false, tr("install_modifier")); return; }
            ItemStack staged = menu.input(0);
            int pi = stagedPart();
            ToolUpgrade su = stagedUpgrade();
            boolean isPart = staged.getItem() instanceof ToolPartItem;
            boolean ok = !staged.isEmpty() && stagedAttached() && (isPart ? pi >= 0 && WorkbenchMenu.canSwap(menu.tool(), staged, pi)
                    : su != null && minecraft.player != null && ToolUpgrades.canApply(minecraft.player, menu.tool(), su) && morphReady());
            int slv = su == null ? 0 : ToolUpgrades.currentLevel(menu.tool(), su);
            readyButton(g, bx, sy, bw, DOCK_H, ok, ok && bh, tr(isPart ? "swap_part" : slv > 0 ? "upgrade_modifier" : "install_modifier"));
        } else {
            boolean can = toolReady() && !masteryChosen() && canChoose();
            readyButton(g, bx, sy, bw, DOCK_H, can, can && bh, tr("select_mastery"));
            if (toolReady() && !masteryChosen() && !canChoose()) tip(bx, sy, bw, DOCK_H, trArgs("mastery_need_level", MasteryLevel.T1));
        }
    }

    private void drawBag(GuiGraphicsExtractor g, int mx, int my) {
        int bx = bagX(), by = bagY();
        boolean bagHover = in(mx, my, bx, by, 20, 20);
        boolean lit = bagHover || menu.isInventoryVisible();
        g.blitSprite(RenderPipelines.GUI_TEXTURED, spr(lit ? "icon_bag_hover" : "icon_bag"), bx + 2, by + 2 - (lit ? 1 : 0), 16, 16);
        tip(bx, by, 20, 20, tr("inventory_hint"));
    }

    private void drawSparks(GuiGraphicsExtractor g) {
        for (int k = sparks.size() - 1; k >= 0; k--) {
            float[] s = sparks.get(k);
            s[0] += s[2] * dt; s[1] += s[3] * dt; s[3] += 110f * dt; s[4] -= dt * 1.5f;
            if (s[4] <= 0f) { sparks.remove(k); continue; }
            fill(g, Math.round(s[0]), Math.round(s[1]), 2, 2, ((int) (255 * Mth.clamp(s[4], 0f, 1f)) << 24) | ((int) s[5] & 0xFFFFFF));
        }
    }

    private void burst(int sx, int sy, int color) {
        java.util.Random rnd = new java.util.Random();
        for (int k = 0; k < 16; k++) {
            double a = rnd.nextDouble() * Math.PI * 2, v = 30 + rnd.nextDouble() * 55;
            sparks.add(new float[] {sx + 11f, sy + 11f, (float) (Math.cos(a) * v), (float) (Math.sin(a) * v) - 30f, 1f, color});
        }
    }

    private void drawHead(GuiGraphicsExtractor g, int mx, int my, Component title, Component sub, int subColor, boolean waiting) {
        int x = headSlotX(), y = headSlotY();
        ItemStack shown = menu.getActiveTab() == 0 ? menu.output() : menu.tool();
        boolean icon = !shown.isEmpty();
        if (icon) {
            g.pose().pushMatrix();
            g.pose().translate(x, y);
            g.pose().scale(1.5f, 1.5f);
            g.item(shown, 0, 0);
            g.pose().popMatrix();
        }
        measure(y + 26);
        int tx = icon || waiting ? x + HEAD_TEXT : x;
        text(g, title, tx, y + 1, x + inW() - tx, .8f, INK);
        if (sub != null) text(g, sub, tx, y + 12, x + inW() - tx, .6f, subColor);
    }

    private void drawToolHead(GuiGraphicsExtractor g, int mx, int my) {
        ItemStack carried = menu.getCarried();
        boolean accepts = !carried.isEmpty() && menu.tool().isEmpty() && menu.slots.get(0).mayPlace(carried);
        if (!toolReady()) {
            drawHead(g, mx, my, tr("insert_tool"), tr("insert_tool_hint"), INK2, true);
            return;
        }
        ItemStack tool = menu.tool();
        int lvl = ToolXp.getLevel(tool), into = ToolXp.xpIntoLevel(tool), need = ToolXp.xpForLevel(tool);
        Component sub = Component.literal("Lv " + lvl + "   ").withColor(INK_ORANGE & 0xFFFFFF)
                .append(Component.literal(lvl >= ToolXp.MAX_LEVEL ? "MAX" : into + "/" + need + " XP").withColor(INK2 & 0xFFFFFF));
        drawHead(g, mx, my, tool.getHoverName(), sub, INK2, false);
        paperBar(g, headSlotX() + HEAD_TEXT, headSlotY() + 20, inW() - HEAD_TEXT, lvl >= ToolXp.MAX_LEVEL ? 1f : need > 0 ? into / (float) need : 0f, 0xFFE0921E);
        tip(headSlotX() + HEAD_TEXT, headSlotY(), inW() - HEAD_TEXT, 26, tool.getHoverName(), Component.translatable(ToolXp.rankKey(lvl)), trArgs("xp_tip", into, need));
    }

    private void drawBuildLeft(GuiGraphicsExtractor g, int mx, int my) {
        int x = lIn(), w = inW(), hy = toolHeadY();
        int sel = menu.getSelectedTool();
        boolean none = sel < 0 || sel >= ToolAssembly.REGISTRY.size();
        ItemStack shown = !menu.output().isEmpty() ? menu.output()
                : none ? ItemStack.EMPTY : new ItemStack(ToolAssembly.REGISTRY.get(sel).result());
        boolean hHover = in(mx, my, x - 3, hy - 2, w + 6, 26);
        rowBg(g, 7, x - 3, hy - 2, w + 6, 26, toolsOpen, hHover);
        measure(hy + 22);
        if (!shown.isEmpty()) g.item(shown, x + 3, hy + 3);
        else if (!ToolAssembly.REGISTRY.isEmpty()) {
            tintedItem(g, new ItemStack(ToolAssembly.REGISTRY.get(0).result()), x + 3, hy + 3, argb(1f, 0.42f, 0.40f, 0.40f));
        }
        text(g, shown.isEmpty() ? tr("choose_tool") : shown.getHoverName(), x + 27, hy + 2, w - 27, .8f, INK);
        text(g, tr(toolsOpen ? "tools_close" : shown.isEmpty() ? "tools_pick" : "tools_change"), x + 27, hy + 13, w - 27, .58f, shown.isEmpty() && !toolsOpen ? INK_ORANGE : INK2);
        int y = recipeY();
        if (toolsOpen) {
            heading(g, x, y, w, tr("tools"), null);
            for (int i = 0; i < ToolAssembly.REGISTRY.size(); i++) {
                int cx = toolCellX(i), cy = toolCellY(i);
                boolean h = in(mx, my, cx, cy, 22, 22);
                if (menu.getSelectedTool() == i) g.blitSprite(RenderPipelines.GUI_TEXTURED, SPR_PSLOT_SEL, cx, cy, 22, 22);
                else pslot(g, cx, cy, 22, h);
                ItemStack st = new ItemStack(ToolAssembly.REGISTRY.get(i).result());
                g.item(st, cx + 3, cy + 3);
                tip(cx, cy, 22, 22, st.getHoverName());
            }
            return;
        }
        if (menu.isRepair()) {
            heading(g, x, y, w, tr("repair_swap"), null);
            int ry = partRowY(0);
            ItemStack staged = menu.input(0);
            boolean empty = staged.isEmpty();
            boolean part = !empty && staged.getItem() instanceof ToolPartItem;
            rowBg(g, 6, x - 3, ry - 2, w + 6, ROW - 2, false, in(mx, my, x - 3, ry - 2, w + 6, ROW - 2));
            ItemStack need = repairMaterial();
            if (empty && !need.isEmpty()) ghostItem(g, need, x + 3, ry + 3, .4f);
            else if (!empty) g.item(staged, x + 3, ry + 3);
            text(g, !empty ? staged.getHoverName() : need.isEmpty() ? tr("repair") : need.getHoverName(), x + 27, ry + 2, w - 27, .72f, INK);
            boolean ok = part ? buildSwapIndex() >= 0 : canRepairNow();
            Component second = empty ? tr("repair_drop") : part ? tr(ok ? "swap_drag" : "swap_same") : tr(ok ? "repair_press" : "repair_full");
            text(g, second, x + 27, ry + 13, w - 27, .58f, empty ? INK_ORANGE : ok ? INK_GREEN : INK_RED);
            wrapped(g, tr("workbench_repair_hint"), x, ry + 30, w, .56f, INK2, 5);
            return;
        }
        if (none) {
            wrapped(g, tr("no_tool_hint"), x, y + 2, w, .6f, INK2, 5);
            return;
        }
        if (!menu.output().isEmpty()) {
            heading(g, x, y, w, tr("recipe"), null);
            wrapped(g, tr("tool_done"), x, y + 17 + HG, w, .6f, INK_GREEN, 3);
            return;
        }
        int needed = Math.max(1, menu.parts().size());
        int placedN = Math.min(needed, WorkbenchAssembly.placedCount());
        heading(g, x, y, w, tr("recipe"), Component.literal(placedN + " / " + needed));
        ItemStack carried = menu.getCarried();
        float pulse = 0.5f + 0.5f * (float) Math.sin(WorkbenchSession.sceneSeconds() * 4.0);
        int next = -1;
        for (int i = 0; i < partCount(); i++) if (next < 0 && menu.input(i).isEmpty()) next = i;
        for (int i = 0; i < partCount(); i++) {
            float local = outCubic((tabT - i * 0.10f) / 0.6f);
            int ry = partRowY(i), rx = x - Math.round((1f - local) * 12f);
            boolean placed = !menu.input(i).isEmpty();
            boolean attached = placed && WorkbenchAssembly.isMerged(i);
            boolean accepts = !placed && !carried.isEmpty() && menu.slots.get(i + 1).mayPlace(carried);
            boolean h = in(mx, my, x - 3, ry - 2, w + 6, ROW - 2);
            rowBg(g, i, rx - 3, ry - 2, w + 6, ROW - 2, i == next || accepts, h);
            measure(ry + 22);
            if (accepts) border(g, rx - 1, ry - 1, 24, 24, ((int) (0x50 + 0xA0 * pulse) << 24) | (INK_BLUE & 0xFFFFFF));
            ItemStack icon = placed ? menu.input(i) : new ItemStack(menu.parts().get(i));
            if (placed) g.item(icon, rx + 3, ry + 3); else ghostItem(g, icon, rx + 3, ry + 3, .4f);
            if (placed) g.blitSprite(RenderPipelines.GUI_TEXTURED, SPR_CHECK, rx + w - 13, ry + 7, 10, 10);
            int owned = placed ? 1 : countInInventory(icon);
            text(g, new ItemStack(menu.parts().get(i)).getHoverName(), rx + 27, ry + 2, w - 27 - (placed ? 15 : 0), .72f, INK);
            Component second = attached ? tr("part_attached_short") : placed ? tr("part_on_table_short")
                    : owned > 0 ? tr("part_drag") : tr("part_missing_short");
            text(g, second, rx + 27, ry + 13, w - 27 - (placed ? 15 : 0), .58f, attached ? INK_GREEN : placed ? INK_BLUE : owned > 0 ? INK_ORANGE : INK_RED);
            tip(x - 3, ry - 2, w + 6, ROW - 2, icon.getHoverName(), tr(attached ? "part_attached" : placed ? "part_on_table" : "part_missing"));
        }
        int hintTop = canFill() ? fillY() + 20 : partRowY(partCount()) + 2;
        boolean complete = WorkbenchAssembly.isComplete();
        String hint = complete ? "ready_hint" : !carried.isEmpty() ? "drop_hint2" : WorkbenchAssembly.placedCount() > 0 ? "attach_hint" : "drop_hint";
        wrapped(g, tr(hint), x, hintTop, w, .56f, complete ? INK_GREEN : INK_BLUE, 3);
        if (canFill()) {
            boolean h = in(mx, my, x, fillY(), w, 16);
            Component fillLabel = tr("fill_parts_short");
            int fw = Math.min(w, Math.round(font.width(fillLabel) * .58f * TEXT_BOOST));
            text(g, fillLabel, x, fillY() + 4, w, .58f, h ? FILL_HOVER : FILL_LINK);
            if (h) fill(g, x, fillY() + 13, fw, 1, FILL_LINK);
            measure(fillY() + 16);
        }
    }

    private void drawBuildRight(GuiGraphicsExtractor g, int mx, int my) {
        int x = rIn(), w = inW();
        boolean done = !menu.output().isEmpty();
        boolean ready = readyToCraft();
        boolean none = menu.getSelectedTool() < 0 && menu.output().isEmpty();
        Component name = !details.isEmpty() ? details.getHoverName()
                : menu.isRepair() ? tr("repair") : none ? tr("choose_tool") : new ItemStack(menu.assembly().result()).getHoverName();
        Component state = tr(done ? "collect" : ready ? "ready" : !preview.isEmpty() ? "join_parts" : "place_parts");
        boolean headIcon = menu.output().isEmpty() && !menu.isRepair() && !none;
        drawHead(g, mx, my, name, state, done ? INK_GREEN : ready ? INK_BLUE : INK2, ready || headIcon);
        if (headIcon) {
            ItemStack icon = !preview.isEmpty() ? preview : new ItemStack(menu.assembly().result());
            g.pose().pushMatrix();
            g.pose().translate(headSlotX(), headSlotY());
            g.pose().scale(1.5f, 1.5f);
            if (!preview.isEmpty()) g.item(icon, 0, 0);
            else ghostItem(g, icon, 0, 0, .35f);
            g.pose().popMatrix();
        }
        int y = bodyY();
        heading(g, x, y, w, tr("status_title"), null);
        y += 16 + HG;
        if (details.isEmpty() || !ToolStack.isInitialized(details)) {
            wrapped(g, tr("preview_hint"), x, y, w, .6f, INK2, 4);
        } else {
            collectTraits();
            ItemStack alt = compare;
            float[] now = {ToolStack.getDurability(details), ToolStack.getMiningSpeed(details), ToolStack.getAttackDamage(details), ToolStack.getAttackSpeed(details)};
            float[] then = alt.isEmpty() ? null : new float[] {ToolStack.getDurability(alt), ToolStack.getMiningSpeed(alt),
                    ToolStack.getAttackDamage(alt), ToolStack.getAttackSpeed(alt)};
            String[] keys = {"stat_durability", "stat_speed", "stat_damage", "stat_agility"};
            String[] help = {"help_durability", "help_speed", "help_damage", "help_agility"};
            Identifier[] icons = {spr("icon_durability"), spr("icon_mining_speed"), spr("icon_damage"), spr("icon_attack_speed")};
            for (int i = 0; i < 4; i++) {
                inkSprite(g, icons[i], x, y, 8);
                text(g, tr(keys[i]), x + 11, y + 1, w - 64, .64f, INK2);
                String value = i == 0 ? String.valueOf(Math.round(now[i])) : fmt1(now[i]);
                textRight(g, Component.literal(value), x + w, y + 1, 34, .68f, INK);
                if (then != null && Math.abs(then[i] - now[i]) > 0.001f) {
                    float d = then[i] - now[i];
                    String t = (d > 0 ? "+" : "") + (i == 0 ? String.valueOf(Math.round(d)) : fmt1(d));
                    textRight(g, Component.literal(t), x + w - Math.round(font.width(value) * .68f * TEXT_BOOST) - 4, y + 1, 30, .6f, d > 0 ? INK_GREEN : INK_RED);
                }
                tip(x, y - 1, w, 11, tr(keys[i]), Component.literal(fmt(now[i]) + (then != null ? " → " + fmt(then[i]) : "")), tr(help[i]));
                y += 11;
            }
            var tier = ToolStack.getHarvestTier(details);
            inkSprite(g, spr("icon_harvest"), x, y, 8);
            text(g, tr("mines_up_to"), x + 11, y + 1, w - 70, .64f, INK2);
            textRight(g, Component.translatable("tooltip.hephaestus_tools.tier." + tier.id()), x + w, y + 1, 60, .68f, INK);
            tip(x, y - 1, w, 11, tr("mines_up_to"), tr("help_harvest"));
            y += 15;
            y = drawSlotPips(g, x, y, w);
            y = drawPartsBlock(g, x, y, w);
        }
    }

    private int drawSlotPips(GuiGraphicsExtractor g, int x, int y, int w) {
        int total = ToolUpgrades.MAX_MODIFIER_SLOTS, unlocked = ToolUpgrades.unlockedSlots(details), used = Math.min(unlocked, ToolUpgrades.usedSlots(details));
        inkSprite(g, spr("icon_socket_plus"), x, y - 1, 8);
        text(g, tr("slots_label"), x + 11, y + 1, w - 60, .64f, INK2);
        int px = x + w - total * 9 + 2;
        for (int k = 0; k < total; k++)
            g.blitSprite(RenderPipelines.GUI_TEXTURED, k < used ? SPR_PIP_USED : k < unlocked ? SPR_PIP_FREE : SPR_PIP_LOCKED, px + k * 9, y, 7, 7);
        tip(x, y - 1, w, 11, tr("slots_label"), trArgs("slots_tip", used, unlocked, total), tr("slots_unlock"));
        return y + 15;
    }

    private int drawPartsBlock(GuiGraphicsExtractor g, int x, int y, int w) {
        var materials = ToolStack.getMaterials(details);
        var parts = ToolBuildHandler.getToolParts(details.getItem());
        if (materials.isEmpty()) return y;
        y += 3;
        heading(g, x, y, w, tr("parts_title"), null);
        y += 17 + HG;
        int room = pBottom() - y;
        int pitch = Mth.clamp(room / Math.max(1, parts.size()), 17, 22);
        int lineGap = pitch < 21 ? 8 : 10;
        for (int i = 0; i < parts.size(); i++) {
            var mat = materials.get(Math.min(i, materials.size() - 1));
            measure(y + pitch);
            ItemStack partStack = parts.get(i) instanceof ToolPartItem tp ? tp.withMaterial(mat) : new ItemStack(parts.get(i));
            g.item(partStack, x, y + 1);
            Component matName = Component.translatable("material.hephaestus_tools." + mat.id().getPath());
            text(g, new ItemStack(parts.get(i)).getHoverName().copy().append(Component.literal("  ")).append(matName.copy().withColor(INK2 & 0xFFFFFF)), x + 20, y, w - 20, .64f, INK);
            var stats = MaterialManager.getInstance().getStatsForSlot(mat, i);
            var trait = MaterialTrait.byId(stats.traitId());
            if (trait.isEmpty() && i == 0) trait = MaterialTrait.byId(MaterialManager.getInstance().getStatsForSlot(mat, 0).traitId());
            Component tn = null, td = null;
            if (trait.isPresent()) {
                tn = Component.translatable("trait.hephaestus_tools." + trait.get().id());
                td = Component.translatable("trait.hephaestus_tools." + trait.get().id() + ".desc");
                text(g, tn.copy().withColor(INK_ORANGE & 0xFFFFFF).append(Component.literal("  ")).append(td.copy().withColor(INK2 & 0xFFFFFF)), x + 20, y + lineGap, w - 20, .52f, INK2);
            } else {
                text(g, tr("part_no_trait"), x + 20, y + lineGap, w - 20, .52f, INK3);
            }
            if (tn != null) tip(x, y - 1, w, pitch, partStack.getHoverName(), tn.copy().withStyle(ChatFormatting.GOLD), td, matName);
            else tip(x, y - 1, w, pitch, partStack.getHoverName(), matName);
            y += pitch;
        }
        return y;
    }

    private int socketCount() { return Math.max(ToolUpgrades.MAX_MODIFIER_SLOTS, installedModifiers().size()); }
    private boolean toolReady() { return !menu.tool().isEmpty() && ToolStack.isInitialized(menu.tool()); }
    private boolean isLocked(int i) { return i >= installedModifiers().size() && i >= ToolUpgrades.unlockedSlots(menu.tool()); }

    private void drawModifyLeft(GuiGraphicsExtractor g, int mx, int my) {
        int x = lIn(), w = inW(), top = lTop() + PP;
        heading(g, x, top, w, tr("modifiers_title"), toolReady() ? Component.literal(String.valueOf(entryCount())) : null);
        if (!toolReady()) { wrapped(g, tr("insert_tool_hint"), x, top + 17 + HG, w, .6f, INK2, 4); return; }
        if (entryCount() == 0) { wrapped(g, tr("none_available"), x, top + 17 + HG, w, .6f, INK2, 2); return; }
        ItemStack held = menu.getCarried();
        float pulse = 0.5f + 0.5f * (float) Math.sin(WorkbenchSession.sceneSeconds() * 6.0);
        listOffset = Mth.clamp(listOffset, 0, maxOffset());
        w = listW();
        for (int i = listOffset; i < Math.min(entryCount(), listOffset + MOD_ROWS); i++) {
            float local = outCubic((tabT - (i - listOffset) * 0.07f) / 0.6f);
            int ry = rowY(i), rx = x - Math.round((1f - local) * 12f);
            ToolUpgrade up = upgrades().get(i);
            int level = ToolUpgrades.currentLevel(menu.tool(), up), maxL = up.maxLevel();
            boolean installed = level > 0, maxed = level >= maxL;
            boolean can = minecraft.player != null && ToolUpgrades.canAfford(minecraft.player, details, up);
            boolean hover = in(mx, my, x - 3, ry - 2, w + 6, ROW - 2);
            rowBg(g, i, rx - 3, ry - 2, w + 6, ROW - 2, i == selectedEntry, hover);
            measure(ry + 22);
            badge(g, entryIcon(i), rx, ry + 1, i == selectedEntry ? BADGE_SEL : maxed ? BADGE_MAX : installed || can ? BADGE_EDGE : BADGE_DIM,
                    i == selectedEntry ? BADGE_FILL_SEL : BADGE_FILL, installed || can ? 1f : 0.5f);
            int cat = mute(categoryColor(up));
            int sqW = installed ? 30 : 0;
            text(g, entryName(i), rx + 27, ry + 2, w - 27 - sqW - 4, .72f, INK);
            if (installed) levelSquares(g, rx + w - 22 - 6, ry + 4, level, maxL, 5, false);
            text(g, tr(categoryKey(up)), rx + 27, ry + 13, w - 27 - 40, .56f, cat);
            Component state = maxed ? Component.literal("MAX") : installed ? Component.literal("Lv " + level) : tr("not_installed");
            textRight(g, state, rx + w - 6, ry + 13, 40, .56f, installed ? INK_ORANGE : INK3);
            if (can) {
                int ay = ry + 1 + Math.round(pulse * 1.5f), ax = rx + 16;
                fill(g, ax + 2, ay, 1, 1, INK_GREEN); fill(g, ax + 1, ay + 1, 3, 1, INK_GREEN); fill(g, ax, ay + 2, 5, 1, INK_GREEN); fill(g, ax + 1, ay + 3, 3, 2, INK_GREEN);
            }
            if (!held.isEmpty() && up.costItem() == held.getItem() && can)
                border(g, rx - 2, ry - 2, 26, 26, ((int) (0x70 + 0x8F * pulse) << 24) | (INK_BLUE & 0xFFFFFF));
            richTip(x - 3, ry - 2, w + 6, ROW - 2, modifierTip(up, level));
        }
        drawScrollbar(g, mx, my);
    }

    private static int mute(int argb) {
        int r = ((argb >> 16) & 255) * 55 / 100 + 0x9A * 45 / 100, gg = ((argb >> 8) & 255) * 55 / 100 + 0x8F * 45 / 100, b = (argb & 255) * 55 / 100 + 0x8C * 45 / 100;
        return 0xFF000000 | (r << 16) | (gg << 8) | b;
    }

    private static int darker(int argb) {
        int r = ((argb >> 16) & 255) * 55 / 100, gg = ((argb >> 8) & 255) * 55 / 100, b = (argb & 255) * 55 / 100;
        return 0xFF000000 | (r << 16) | (gg << 8) | b;
    }

    private static final int SLOT_STEP = 24;
    private int slotsY() { return bodyY() + 15 + HG; }
    private int slotX(int i) { return rIn() + (inW() - (socketCount() * SLOT_STEP - 2)) / 2 + i * SLOT_STEP; }

    private int selectedModSlot() {
        if (entryCount() == 0) return -1;
        var id = upgrades().get(Mth.clamp(selectedEntry, 0, entryCount() - 1)).id();
        var inst = installedModifiers();
        for (int i = 0; i < inst.size(); i++) if (inst.get(i).id().equals(id)) return i;
        return -1;
    }

    private void drawModifyRight(GuiGraphicsExtractor g, int mx, int my) {
        drawToolHead(g, mx, my);
        if (!toolReady()) return;
        int x = rIn(), w = inW(), y = bodyY();
        var installed = installedModifiers();
        int unlocked = ToolUpgrades.unlockedSlots(menu.tool());
        heading(g, x, y, w, tr("slots"), Component.literal(installed.size() + " / " + unlocked));
        float pulse = 0.5f + 0.5f * (float) Math.sin(WorkbenchSession.sceneSeconds() * 6.0);
        int selSlot = selectedModSlot(), sy = slotsY();
        ItemStack held = menu.getCarried();
        for (int i = 0; i < socketCount(); i++) {
            float local = outCubic((tabT - i * 0.07f) / 0.6f);
            int sx = slotX(i), yy = sy + Math.round((1f - local) * 6f);
            boolean hover = in(mx, my, sx, sy, 22, 22);
            boolean lockedSlot = i >= installed.size() && isLocked(i);
            g.blitSprite(RenderPipelines.GUI_TEXTURED, lockedSlot ? SPR_PSLOT_LOCKED : i == selSlot ? SPR_PSLOT_SEL : hover ? SPR_PSLOT_HOVER : SPR_PSLOT, sx, yy, 22, 22);
            measure(yy + 22);
            if (i < installed.size()) {
                var entry = installed.get(i);
                ToolUpgrade up = ToolUpgrades.find(entry.id()).orElse(null);
                int max = up != null ? up.maxLevel() : entry.level();
                boolean maxed = entry.level() >= max;
                Integer before = lastLevels.get(entry.id());
                if (before != null && entry.level() > before && i < flash.length) {
                    flash[i] = 1f;
                    burst(sx, yy, up != null ? categoryColor(up) : ACCENT);
                    if (minecraft != null) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.ANVIL_USE, 1.4f, 0.7f));
                    if (maxed && minecraft != null) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.PLAYER_LEVELUP, 1f, 0.9f));
                }
                lastLevels.put(entry.id(), entry.level());
                Identifier tex = installedIcon(entry.id());
                if (tex != null) g.blit(RenderPipelines.GUI_TEXTURED, tex, sx + 3, yy + 3, 0f, 0f, 16, 16, 16, 16, 16, 16);
                int cat = up != null ? categoryColor(up) : ACCENT;
                levelSquares(g, sx + 11, yy + 24, entry.level(), max, 4, true);
                if (up != null) richTip(sx, sy, 22, 22, modifierTip(up, entry.level()));
            } else if (isLocked(i)) {
                g.blitSprite(RenderPipelines.GUI_TEXTURED, SPR_LOCK, sx + 3, yy + 3, 16, 16);
                tip(sx, sy, 22, 22, tr("locked_title"), trArgs("slot_locked", ToolUpgrades.unlockLevel(i)));
            } else {
                blitSpriteAlpha(g, spr("icon_socket_plus"), sx + 3, yy + 3, 16, 0.55f);
                tip(sx, sy, 22, 22, tr("slot_free_short"), tr("pick_left"));
            }
            if (i < flash.length && flash[i] > 0f) fill(g, sx, yy, 22, 22, ((int) (0xB0 * flash[i]) << 24) | 0xFFF6D8);
        }
        y = sy + 40;
        if (entryCount() == 0) return;
        int sel = Mth.clamp(selectedEntry, 0, entryCount() - 1);
        ToolUpgrade up = upgrades().get(sel);
        int level = ToolUpgrades.currentLevel(menu.tool(), up), maxL = up.maxLevel();
        boolean max = level >= maxL;
        boolean hoverBtn = dockBtnHit(mx, my);
        boolean canApply = minecraft.player != null && ToolUpgrades.canApply(minecraft.player, details, up);
        heading(g, x, y, w, entryName(sel), Component.literal(level > 0 ? "Lv " + level + "/" + maxL : "Lv 0/" + maxL));
        y = wrapped(g, entryDescription(sel), x, y + 15 + HG, w, .56f, INK2, 2) + 3;
        if (!max) {
            y = inkStatPreview(g, up, level, x, y, w, 3, hoverBtn && canApply) + 3;
            costLine(g, up, level, x, y, w);
            y += 13;
            if (level == 0 && installed.size() >= unlocked) text(g, tr("no_free_slot"), x, y, w, .54f, INK_RED);
        } else {
            centered(g, tr("max_level"), x + w / 2, y + 4, w, .8f, INK_ORANGE);
        }
        ItemStack staged = menu.input(0);
        int hy = placeAction();
        fillModY = -1;
        if (staged.isEmpty() || canFillModifier()) {
            int ly = staged.isEmpty() ? wrapped(g, tr(!menu.getCarried().isEmpty() ? "drop_on_tool_now" : max ? "drop_part_hint" : "drop_material_hint"), x, hy, w, .56f,
                    !menu.getCarried().isEmpty() ? INK_BLUE : INK2, 4) : hy;
            if (canFillModifier()) {
                fillModY = ly + 3;
                boolean h = in(mx, my, x, fillModY, w, 12);
                Component fillLabel = tr("fill_material_short");
                int fw = Math.min(w, Math.round(font.width(fillLabel) * .58f * TEXT_BOOST));
                text(g, fillLabel, x, fillModY + 2, w, .58f, h ? FILL_HOVER : FILL_LINK);
                if (h) fill(g, x, fillModY + 11, fw, 1, FILL_LINK);
                measure(fillModY + 13);
            }
        } else {
            boolean isPart = staged.getItem() instanceof ToolPartItem;
            int pi = stagedPart();
            ToolUpgrade su = stagedUpgrade();
            boolean ok = isPart ? pi >= 0 && WorkbenchMenu.canSwap(menu.tool(), staged, pi)
                    : su != null && minecraft.player != null && ToolUpgrades.canApply(minecraft.player, menu.tool(), su);
            if (ok && !stagedAttached()) wrapped(g, tr("drag_staged_hint"), x, hy, w, .56f, INK_BLUE, 3);
            else wrapped(g, tr(ok ? "press_button" : isPart ? "swap_same" : "staged_cant"), x, hy, w, .56f, ok ? INK_GREEN : INK_RED, 3);
        }
    }

    private void costLine(GuiGraphicsExtractor g, ToolUpgrade up, int level, int x, int y, int w) {
        int owned = ToolUpgrades.stagedCount(minecraft.player, up.costItem());
        int need = up.costFor(level + 1);
        boolean ok = owned >= need;
        ItemStack cost = new ItemStack(up.costItem());
        icon(g, cost, x, y);
        text(g, Component.literal(need + " x ").append(cost.getHoverName()), x + 13, y + 1, w - 13 - 28, .6f, INK);
        textRight(g, Component.literal(String.valueOf(owned)), x + w, y + 1, 28, .62f, ok ? INK_GREEN : INK_RED);
        tip(x, y - 1, w, 11, cost.getHoverName(), tr("drop_to_upgrade"));
    }

    private void futureCosts(GuiGraphicsExtractor g, ToolUpgrade up, int level, int x, int y, int w) {
        var line = Component.empty();
        for (int lv = level + 2; lv <= Math.min(up.maxLevel(), level + 5); lv++)
            line.append(Component.literal("Lv" + lv + " ").withColor(INK3 & 0xFFFFFF)).append(Component.literal("x" + up.costFor(lv) + "   ").withColor(INK2 & 0xFFFFFF));
        text(g, line, x, y, w, .52f, INK3);
        tip(x, y - 1, w, 9, tr("cost_future"));
    }

    private int inkStatPreview(GuiGraphicsExtractor g, ToolUpgrade up, int level, int x, int y, int w, int maxRows, boolean emphasise) {
        ItemStack after;
        try {
            after = details.copy();
            ToolStack.addModifier(after, new ToolConstructionData.ModifierEntry(up.id(), level + 1));
            ToolStack.recalculate(after);
        } catch (RuntimeException e) {
            return y;
        }
        String[] keys = {"stat_durability", "stat_speed", "stat_damage", "stat_agility"};
        float[] a = {ToolStack.getDurability(details), ToolStack.getMiningSpeed(details), ToolStack.getAttackDamage(details), ToolStack.getAttackSpeed(details)};
        float[] b = {ToolStack.getDurability(after), ToolStack.getMiningSpeed(after), ToolStack.getAttackDamage(after), ToolStack.getAttackSpeed(after)};
        text(g, tr("next_level"), x, y, w, .56f, INK3);
        y += 9;
        int shown = 0;
        for (int i = 0; i < 4 && shown < maxRows; i++) {
            if (Math.abs(b[i] - a[i]) < 0.001f) continue;
            String from = i == 0 ? String.valueOf(Math.round(a[i])) : fmt1(a[i]);
            String to = i == 0 ? String.valueOf(Math.round(b[i])) : fmt1(b[i]);
            text(g, tr(keys[i]), x, y, w - 60, .6f, INK);
            textRight(g, Component.literal(from + " → " + to), x + w, y, 60, emphasise ? .68f : .62f, b[i] > a[i] ? INK_GREEN : INK_RED);
            y += 10;
            shown++;
        }
        if (shown == 0) { text(g, tr("effect_special"), x, y, w, .56f, INK2); y += 10; }
        return y;
    }

    private int stagedPart() {
        ItemStack st = menu.input(0);
        return st.isEmpty() || !(st.getItem() instanceof ToolPartItem) ? -1 : WorkbenchMenu.partIndexFor(menu.tool(), st);
    }

    private ToolUpgrade stagedUpgrade() {
        ItemStack st = menu.input(0);
        if (st.isEmpty() || st.getItem() instanceof ToolPartItem || minecraft.player == null) return null;
        ToolUpgrade pick = null;
        for (int k = 0; k < upgrades().size(); k++) {
            var up = upgrades().get(k);
            if (up.costItem() != st.getItem()) continue;
            boolean can = ToolUpgrades.canAfford(minecraft.player, menu.tool(), up);
            if (k == selectedEntry) return up;
            if (pick == null || (can && !ToolUpgrades.canAfford(minecraft.player, menu.tool(), pick))) pick = up;
        }
        return pick;
    }

    private ToolUpgrade dropTarget(ItemStack held) {
        if (held.isEmpty() || !toolReady() || minecraft.player == null) return null;
        ToolUpgrade pick = null;
        for (int k = 0; k < upgrades().size(); k++) {
            var up = upgrades().get(k);
            if (up.costItem() != held.getItem() || !ToolUpgrades.canAfford(minecraft.player, menu.tool(), up)) continue;
            if (k == selectedEntry) return up;
            if (pick == null) pick = up;
        }
        return pick;
    }

    private boolean masteryChosen() { return !ToolMastery.selected(menu.tool()).isEmpty(); }
    private boolean canChoose() { return ToolXp.getLevel(menu.tool()) >= MasteryLevel.T1; }

    private void drawMasteryLeft(GuiGraphicsExtractor g, int mx, int my) {
        int x = lIn(), w = inW();
        boolean chosen = toolReady() && masteryChosen();
        heading(g, x, lTop() + PP, w, tr("mastery_title"), toolReady() && !chosen ? tr("choose_one") : null);
        if (!toolReady()) { wrapped(g, tr("insert_tool_hint"), x, lTop() + PP + 17 + HG, w, .6f, INK2, 4); return; }
        int n = Math.min(3, entryCount());
        for (int i = 0; i < n; i++) {
            float local = outCubic((tabT - i * 0.12f) / 0.6f);
            int ry = mRowY(i), rx = x - Math.round((1f - local) * 12f);
            boolean sel = i == selectedEntry, hover = !chosen && in(mx, my, x - 3, ry - 2, w + 6, MROW - 2);
            boolean dim = chosen && !sel;
            rowBg(g, i, rx - 3, ry - 2, w + 6, MROW - 2, sel, hover);
            measure(ry + 26);
            badge(g, entryIcon(i), rx + 3, ry + 4, sel ? BADGE_SEL : BADGE_EDGE, sel ? BADGE_FILL_SEL : BADGE_FILL, dim ? 0.35f : 1f);
            text(g, entryName(i), rx + 29, ry + 1, w - 29, .72f, dim ? INK3 : INK);
            wrapped(g, entryDescription(i), rx + 29, ry + 12, w - 29, .52f, dim ? INK3 : INK2, 2);
            if (!chosen) tip(x - 3, ry - 2, w + 6, MROW - 2, entryName(i).copy().withStyle(ChatFormatting.GOLD), entryDescription(i), tr("mastery_permanent"));
        }
        int ty = mRowY(n) + 2;
        wrapped(g, tr(chosen ? "mastery_locked" : "mastery_permanent"), x, ty, w, .54f, chosen ? INK3 : INK_RED, 3);
    }

    private void drawMasteryRight(GuiGraphicsExtractor g, int mx, int my) {
        drawToolHead(g, mx, my);
        if (!toolReady() || entryCount() == 0) return;
        int x = rIn(), w = inW(), y = bodyY();
        int idx = Mth.clamp(selectedEntry, 0, entryCount() - 1);
        boolean chosen = masteryChosen();
        heading(g, x, y, w, entryName(idx), null);
        y = wrapped(g, entryDescription(idx), x, y + 15 + HG, w, .56f, INK2, 2) + 5;
        int lvl = ToolXp.getLevel(menu.tool());
        int[] req = {MasteryLevel.T1, MasteryLevel.T2, MasteryLevel.T3};
        float pulse = 0.5f + 0.5f * (float) Math.sin(WorkbenchSession.sceneSeconds() * 3.0);
        boolean nextFound = false;
        int step = 24;
        for (int i = 0; i < 3; i++) {
            int ny = y + i * step;
            boolean on = chosen && lvl >= req[i];
            boolean next = chosen && !on && !nextFound;
            if (next) nextFound = true;
            if (i < 2) g.blitSprite(RenderPipelines.GUI_TEXTURED, on && lvl >= req[i + 1] ? SPR_MLINK_ON : SPR_MLINK, x + 6, ny + 16, 2, step - 16);
            g.blitSprite(RenderPipelines.GUI_TEXTURED, spr("mastery_tier_" + (i + 1)), x, ny, 15, 16,
                    argb(on ? 1f : next ? 0.6f + 0.4f * pulse : 0.38f, 1f, 1f, 1f));
            text(g, Component.literal("Lv " + req[i] + "  ").append(tr("mastery_tier_" + (i + 1))), x + 21, ny + 2, w - 21, .64f, on ? INK : INK2);
            if (step >= 20) text(g, tr(on ? "mastery_tier_active" : "mastery_tier_locked"), x + 21, ny + 11, w - 21, .5f, on ? INK_GREEN : INK3);
            tip(x, ny, w, 16, tr("mastery_tier_" + (i + 1)), trArgs("mastery_tier_level", req[i]), entryDescription(idx));
        }
        int by = placeAction();
        if (!chosen) {
            wrapped(g, tr(canChoose() ? "press_button" : "mastery_need_level_short"), x, by, w, .56f, canChoose() ? INK_GREEN : INK_RED, 3);
        } else {
            Component nextTier = lvl >= ToolXp.MAX_LEVEL ? tr("mastery_max")
                    : trArgs("mastery_next_tier", lvl < MasteryLevel.T1 ? MasteryLevel.T1 : lvl < MasteryLevel.T2 ? MasteryLevel.T2 : MasteryLevel.T3);
            centered(g, nextTier, x + w / 2, by + 5, w, .64f, INK_ORANGE);
        }
    }

    private void drawTooltips(GuiGraphicsExtractor g, int mx, int my) {
        if (inspect) return;
        for (int i = tipRects.size() - 1; i >= 0; i--) {
            int[] r = tipRects.get(i);
            if (!in(mx, my, r[0], r[1], r[2], r[3])) continue;
            int id = r[0] * 31 + r[1] * 131 + r[2] * 7 + r[3] + i * 100003;
            if (id != tipShown) { tipShown = id; tipT = 0f; }
            tipT = Math.min(1f, tipT + dt / 0.16f);
            Tip t = tipRich.get(i);
            if (t == null) t = Tip.fromLines(tipTexts.get(i), font);
            drawRichTip(g, t, mx, my);
            return;
        }
        tipShown = -1;
        tipT = 0f;
    }

    private static final class Tip {
        static final int TITLE = 0, SUB = 1, BAR = 2, BODY = 3, GAP = 4, STAT = 5, COST = 6, HINT = 7;
        final List<Object[]> rows = new ArrayList<>();
        Tip title(Component c, int color) { rows.add(new Object[] {TITLE, c, color}); return this; }
        Tip sub(Component c, int color, Component right, int rightColor) { rows.add(new Object[] {SUB, c, color, right, rightColor}); return this; }
        Tip bar(int level, int max, Component label, int labelColor) { rows.add(new Object[] {BAR, level, max, label, labelColor}); return this; }
        Tip body(Component c, int color) { rows.add(new Object[] {BODY, c, color}); return this; }
        Tip gap() { rows.add(new Object[] {GAP}); return this; }
        Tip stat(Component left, Component right, int rightColor) { rows.add(new Object[] {STAT, left, right, rightColor}); return this; }
        Tip cost(ItemStack st, Component c, Component right, int rightColor) { rows.add(new Object[] {COST, st, c, right, rightColor}); return this; }
        Tip hint(Component c, int color) { rows.add(new Object[] {HINT, c, color}); return this; }

        static Tip fromLines(List<Component> lines, net.minecraft.client.gui.Font font) {
            Tip t = new Tip();
            boolean first = true;
            for (Component c : lines) {
                if (c.getString().isEmpty()) { t.gap(); continue; }
                if (first) { t.title(c, INK_HEAD); first = false; }
                else t.body(c, INK2);
            }
            return t;
        }
    }

    private void tdraw(GuiGraphicsExtractor g, Component c, int x, int y, float scale, int color) {
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(scale, scale);
        g.text(font, c, 0, 0, color, bright(color));
        g.pose().popMatrix();
    }

    private int tw(Component c, float scale) { return Math.round(font.width(c) * scale); }

    private int wrapDraw(GuiGraphicsExtractor g, List<FormattedCharSequence> lines, int x, int y, float scale, int color) {
        int adv = Math.round(scale * 9f) + 3;
        for (FormattedCharSequence line : lines) {
            g.pose().pushMatrix();
            g.pose().translate(x, y);
            g.pose().scale(scale, scale);
            g.text(font, line, 0, 0, color, bright(color));
            g.pose().popMatrix();
            y += adv;
        }
        return y;
    }

    private void drawRichTip(GuiGraphicsExtractor g, Tip t, int mx, int my) {
        final int PAD = 8, MAXW = 150;
        final float TS = .86f, SS = .64f, BS = .66f, XS = .68f;
        int natural = 24;
        for (Object[] r : t.rows) {
            switch ((Integer) r[0]) {
                case 0 -> natural = Math.max(natural, Math.min(MAXW, tw((Component) r[1], TS)));
                case 1 -> natural = Math.max(natural, tw((Component) r[1], SS) + (r[3] == null ? 0 : tw((Component) r[3], SS) + 14));
                case 2 -> natural = Math.max(natural, 110);
                case 3 -> natural = Math.max(natural, Math.min(MAXW, tw((Component) r[1], BS)));
                case 4 -> { }
                case 5 -> natural = Math.max(natural, tw((Component) r[1], XS) + tw((Component) r[2], XS) + 16);
                case 6 -> natural = Math.max(natural, 14 + tw((Component) r[2], XS) + tw((Component) r[3], XS) + 16);
                default -> natural = Math.max(natural, Math.min(MAXW, tw((Component) r[1], SS)));
            }
        }
        natural = Math.min(natural, MAXW);
        int w = Math.max(24, natural) + PAD * 2 + 2, inner = w - PAD * 2;
        int rowsH = 0;
        List<List<FormattedCharSequence>> wraps = new ArrayList<>();
        for (Object[] r : t.rows) {
            List<FormattedCharSequence> wrap = null;
            switch ((Integer) r[0]) {
                case 0 -> { wrap = font.split((Component) r[1], Math.round(inner / TS)); rowsH += wrap.size() * (Math.round(TS * 9f) + 3); }
                case 1 -> rowsH += 10;
                case 2 -> rowsH += 10;
                case 3 -> { wrap = font.split((Component) r[1], Math.round(inner / BS)); rowsH += wrap.size() * (Math.round(BS * 9f) + 3); }
                case 4 -> rowsH += 5;
                case 5 -> rowsH += 10;
                case 6 -> rowsH += 13;
                default -> { wrap = font.split((Component) r[1], Math.round(inner / SS)); rowsH += wrap.size() * (Math.round(SS * 9f) + 3); }
            }
            wraps.add(wrap);
        }
        int h = PAD * 2 + rowsH - 5;
        float e = outCubic(tipT);
        int x = mx + 12, y = my - 10;
        if (x + w > width - 4) x = mx - w - 10;
        if (y + h > height - 4) y = height - 4 - h;
        if (y < 4) y = 4;
        y += Math.round((1f - e) * 5f);
        g.nextStratum();
        g.pose().pushMatrix();
        g.blitSprite(RenderPipelines.GUI_TEXTURED, SPR_PAGE, x, y, w, h, argb(e, 1f, 1f, 1f));
        fill(g, x + 3, y + 3, w - 6, h - 6, ((int) (0xB0 * e) << 24));
        int cy = y + PAD - 1, cx = x + PAD;
        for (int i = 0; i < t.rows.size(); i++) {
            Object[] r = t.rows.get(i);
            switch ((Integer) r[0]) {
                case 0 -> cy = wrapDraw(g, wraps.get(i), cx, cy, TS, (Integer) r[2]);
                case 1 -> {
                    tdraw(g, (Component) r[1], cx, cy, SS, (Integer) r[2]);
                    if (r[3] != null) tdraw(g, (Component) r[3], cx + inner - tw((Component) r[3], SS), cy, SS, (Integer) r[4]);
                    cy += 10;
                }
                case 2 -> {
                    int lv = (Integer) r[1], mx2 = (Integer) r[2];
                    Component label = (Component) r[3];
                    int lw = tw(label, SS);
                    paperBar(g, cx, cy + 1, inner - lw - 6, lv / (float) Math.max(1, mx2), 0);
                    tdraw(g, label, cx + inner - lw, cy, SS, (Integer) r[4]);
                    cy += 10;
                }
                case 3 -> cy = wrapDraw(g, wraps.get(i), cx, cy, BS, (Integer) r[2]);
                case 4 -> cy += 5;
                case 5 -> {
                    tdraw(g, (Component) r[1], cx, cy, XS, INK);
                    tdraw(g, (Component) r[2], cx + inner - tw((Component) r[2], XS), cy, XS, (Integer) r[3]);
                    cy += 10;
                }
                case 6 -> {
                    icon(g, (ItemStack) r[1], cx, cy);
                    tdraw(g, (Component) r[2], cx + 14, cy + 1, XS, INK);
                    tdraw(g, (Component) r[3], cx + inner - tw((Component) r[3], XS), cy + 1, XS, (Integer) r[4]);
                    cy += 13;
                }
                default -> cy = wrapDraw(g, wraps.get(i), cx, cy, SS, (Integer) r[2]);
            }
        }
        g.pose().popMatrix();
    }

    private Tip modifierTip(ToolUpgrade up, int level) {
        int maxL = up.maxLevel(), cat = mute(categoryColor(up));
        Tip t = new Tip().title(Component.translatable(up.nameKey()), cat);
        t.sub(tr(categoryKey(up)), cat, null, 0);
        t.bar(level, maxL, level >= maxL ? tr("max_level_short") : level > 0 ? Component.literal("Lv " + level + "/" + maxL) : tr("not_installed"),
                level > 0 ? INK_ORANGE : INK3);
        t.gap();
        t.body(Component.translatable("gui.hephaestus_tools.upgrade." + up.id().getPath() + ".desc"), INK2);
        if (level >= maxL) return t;
        t.gap();
        t.sub(tr("next_level"), INK3, null, 0);
        try {
            ItemStack after = details.copy();
            ToolStack.addModifier(after, new ToolConstructionData.ModifierEntry(up.id(), level + 1));
            ToolStack.recalculate(after);
            String[] keys = {"stat_durability", "stat_speed", "stat_damage", "stat_agility"};
            float[] a = {ToolStack.getDurability(details), ToolStack.getMiningSpeed(details), ToolStack.getAttackDamage(details), ToolStack.getAttackSpeed(details)};
            float[] b = {ToolStack.getDurability(after), ToolStack.getMiningSpeed(after), ToolStack.getAttackDamage(after), ToolStack.getAttackSpeed(after)};
            boolean any = false;
            for (int i = 0; i < 4; i++) {
                if (Math.abs(b[i] - a[i]) < 0.001f) continue;
                any = true;
                String from = i == 0 ? String.valueOf(Math.round(a[i])) : fmt1(a[i]);
                String to = i == 0 ? String.valueOf(Math.round(b[i])) : fmt1(b[i]);
                t.stat(tr(keys[i]), Component.literal(from + " \u2192 " + to), b[i] > a[i] ? INK_GREEN : INK_RED);
            }
            if (!any) t.body(tr("effect_special"), INK2);
        } catch (RuntimeException ignored) { }
        int need = up.costFor(level + 1), staged = ToolUpgrades.stagedCount(minecraft.player, up.costItem());
        t.gap();
        t.cost(new ItemStack(up.costItem()), Component.literal(need + " x ").append(new ItemStack(up.costItem()).getHoverName()),
                Component.literal(staged + "/" + need), staged >= need ? INK_GREEN : INK_RED);
        t.gap();
        t.hint(tr("drop_to_upgrade"), INK_ORANGE);
        return t;
    }

    private boolean freeSpace(double mx, double my) {
        if (my > height - HOTBAR_H - 2) return false;
        if (in(mx, my, bagX(), bagY(), 20, 20)) return false;
        if (inspect) return true;
        if (pagesT > 0.05f && in(mx, my, dockX() - DOCK_PAD, dockY() - DOCK_PAD, 22 + DOCK_GAP + DOCK_BTN_W + DOCK_PAD * 2, DOCK_H + DOCK_PAD * 2)) return false;
        if (pagesT > 0.05f) {
            if (in(mx, my, lpX() - 6, lTop() - 22, pageW() + 12, leftPageH() + 26)) return false;
            if (showRight() && in(mx, my, rpX() - 6, rTop() - 6, pageW() + 12, rightPageH() + 10)) return false;
        }
        if (!inspect) {
            int hw = 0;
            for (Component[] l : hintLines()) hw = Math.max(hw, Math.round(font.width(hintLine(l)) * .62f * TEXT_BOOST));
            if (in(mx, my, width - 12 - hw, hintsY(), hw, hintsH())) return false;
        }
        if (drawerT > 0.05f && in(mx, my, drawerX() - 7, drawerY() - 7, DRAWER_W + 14, DRAWER_H + 14)) return false;
        return true;
    }

    @Override public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        if (WorkbenchCraft.isBusy() && keyCode != 256) return true;
        if (keyCode == 72 || (keyCode == 256 && hideUi)) { setHideUi(!hideUi && keyCode == 72); click(); return true; }
        if (keyCode == 256 && toolsOpen && !inspect) { toolsOpen = false; click(); return true; }
        if (inspect && keyCode == 256) { inspect = false; click(); return true; }
        if (keyCode == 70 && hasSubject()) { inspect = !inspect; click(); return true; }
        if (!inspect) {
            if (keyCode == 258) { send(WorkbenchMenu.TAB_BASE + (menu.getActiveTab() + 1) % 3); return true; }
            if (minecraft != null && minecraft.options.keyInventory.matches(event)) {
                menu.setInventoryVisible(!menu.isInventoryVisible());
                click();
                return true;
            }
            if (keyCode == 32 && readyToCraft()) { startCraft(); return true; }
            if (keyCode == 71 && canFill()) { fillParts(); return true; }
            if (keyCode == 71 && canFillModifier()) { fillModifier(); return true; }
            if ((keyCode == 265 || keyCode == 264) && menu.getActiveTab() == 1 && toolReady() && entryCount() > 0) {
                int nextSel = Math.floorMod(selectedEntry + (keyCode == 264 ? 1 : -1), entryCount());
                releaseBench(nextSel);
                selectedEntry = nextSel;
                ensureVisible();
                click();
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        int button = event.button();
        if (WorkbenchCraft.isBusy()) return true;
        if (button == 0 && !inspect && !hideUi) {
            if (in(mx, my, bagX(), bagY(), 20, 20)) {
                menu.setInventoryVisible(!menu.isInventoryVisible());
                click();
                return true;
            }
            if (pagesOpen()) {
                for (int i = 0; i < 3; i++) if (in(mx, my, tabX(i), lTop() - 19, tabW(i), 15)) {
                    if (menu.getActiveTab() != i) send(WorkbenchMenu.TAB_BASE + i);
                    return true;
                }
                int tab = menu.getActiveTab();
                if (tab == 0 && buildClick(mx, my)) return true;
                if (tab != 0 && progressionClick(mx, my)) return true;
            }
        }
        if ((button == 0 || button == 1) && hoveredSlot == null && freeSpace(mx, my)) {
            if (button == 0 && !inspect && dropOnTool(mx, my)) return true;
            if (menu.getCarried().isEmpty()) {
                if (menu.getActiveTab() <= 1 && !inspect) {
                    float[] p = WorkbenchAssembly.mouseToPad(mx, my, width, height);
                    if (p != null) {
                        int part = WorkbenchAssembly.pick(p[0], p[1]);
                        if (part >= 0) {
                            boolean wasAttached = WorkbenchAssembly.isMerged(part);
                            if (button == 0) {
                                WorkbenchAssembly.beginDrag(part, p[0], p[1]);
                                if (wasAttached) WorkbenchAssembly.detachFx(menu.input(part)); else click();
                            } else {
                                Slot sl = menu.slots.get(part + 1);
                                slotClicked(sl, sl.index, 0, ContainerInput.PICKUP);
                                click();
                            }
                            return true;
                        }
                    }
                }
                if (hasSubject()) { rotating = true; return true; }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private boolean buildClick(double mx, double my) {
        int x = rIn(), w = inW();
        if (dockBtnHit(mx, my)) {
            if (menu.isRepair() && !menu.input(0).isEmpty() && !(menu.input(0).getItem() instanceof ToolPartItem)) {
                if (canRepairNow()) startRepair(); else denied();
                return true;
            }
            if (readyToCraft()) { startCraft(); return true; }
            if (!menu.output().isEmpty() && menu.getCarried().isEmpty()) {
                Slot out = menu.slots.get(5);
                boolean room = minecraft != null && minecraft.player != null && minecraft.player.getInventory().getFreeSlot() >= 0;
                slotClicked(out, out.index, 0, room ? ContainerInput.QUICK_MOVE : ContainerInput.PICKUP);
                click();
                return true;
            }
            return true;
        }
        if (in(mx, my, lIn() - 3, toolHeadY() - 2, inW() + 6, 26)) { toolsOpen = !toolsOpen; click(); return true; }
        if (toolsOpen) {
            for (int i = 0; i < ToolAssembly.REGISTRY.size(); i++)
                if (in(mx, my, toolCellX(i), toolCellY(i), 22, 22)) {
                    send(WorkbenchMenu.SELECT_BASE + i);
                    toolsOpen = false;
                    return true;
                }
            return in(mx, my, lpX(), lTop(), pageW(), leftPageH());
        }
        if (partsListVisible() && menu.output().isEmpty()) {
            if (canFill() && in(mx, my, lIn(), fillY(), inW(), 16)) { fillParts(); return true; }
            for (int i = 0; i < partCount(); i++) {
                if (!in(mx, my, lIn() - 3, partRowY(i) - 2, inW() + 6, ROW - 2)) continue;
                Slot sl = menu.slots.get(i + 1);
                if (menu.getCarried().isEmpty() && !menu.input(i).isEmpty()) slotClicked(sl, sl.index, 0, ContainerInput.PICKUP);
                else if (!menu.getCarried().isEmpty() && menu.input(i).isEmpty() && sl.mayPlace(menu.getCarried()))
                    slotClicked(sl, sl.index, 0, ContainerInput.PICKUP);
                click();
                return true;
            }
        }
        return false;
    }

    private boolean progressionClick(double mx, double my) {
        if (!toolReady()) return false;
        refreshEntries();
        int tab = menu.getActiveTab();
        int x = rIn(), w = inW();
        if (tab == 1) {
            var inst = installedModifiers();
            ItemStack held = menu.getCarried();
            if (needScroll() && in(mx, my, lIn() + inW() - SCROLL_W - 2, lTop() + PP + 14 + HG, SCROLL_W + 4, listH())) { scrollDrag = true; scrollTo(my); return true; }
            for (int i = listOffset; i < Math.min(entryCount(), listOffset + MOD_ROWS); i++) {
                if (!in(mx, my, lIn() - 3, rowY(i) - 2, listW() + 6, ROW - 2)) continue;
                releaseBench(i);
                selectedEntry = i;
                click();
                return true;
            }
            for (int i = 0; i < socketCount(); i++) {
                if (!in(mx, my, slotX(i), slotsY(), 22, 22)) continue;
                if (i < inst.size()) {
                    for (int k = 0; k < entryCount(); k++) if (upgrades().get(k).id().equals(inst.get(i).id())) selectedEntry = k;
                    ensureVisible();
                    click();
                } else denied();
                return true;
            }
            if (fillModY >= 0 && canFillModifier() && in(mx, my, x, fillModY, w, 12)) { fillModifier(); return true; }
            if (!menu.input(0).isEmpty() && dockBtnHit(mx, my)) {
                if (!stagedAttached()) { denied(); return true; }
                int pi = stagedPart();
                ToolUpgrade up = stagedUpgrade();
                if (pi >= 0 && WorkbenchMenu.canSwap(menu.tool(), menu.input(0), pi)) startSwap(pi);
                else if (up != null && minecraft.player != null && morphReady() && ToolUpgrades.canApply(minecraft.player, menu.tool(), up)) applyUpgrade(up);
                else denied();
                return true;
            }
            return false;
        }
        if (tab == 2 && !masteryChosen()) {
            for (int i = 0; i < Math.min(3, entryCount()); i++)
                if (in(mx, my, lIn() - 3, mRowY(i) - 2, inW() + 6, MROW - 2)) { selectedEntry = i; click(); return true; }
            if (dockBtnHit(mx, my)) {
                if (!canChoose()) { denied(); return true; }
                int entry = Mth.clamp(selectedEntry, 0, entryCount() - 1);
                if (!WorkbenchCraft.isBusy()) {
                    inspect = false;
                    if (skipCinematic()) { send(WorkbenchMenu.MASTERY_SELECT_BASE + entry); quickDone(); return true; }
                    click();
                    if (WorkbenchCraft.startTinker(menu.tool(), ItemStack.EMPTY, () -> {
                        menu.setInspecting(false);
                        send(WorkbenchMenu.MASTERY_SELECT_BASE + entry);
                    })) announce(true);
                }
                return true;
            }
        }
        return false;
    }

    private void place(int slotIndex, int x, int y) {
        Slot slot = menu.slots.get(slotIndex);
        ((SlotAccessor) slot).hephaestusTools$setX(x);
        ((SlotAccessor) slot).hephaestusTools$setY(y);
    }

    private boolean hasSubject() {
        return menu.getActiveTab() == 0
                ? !menu.output().isEmpty() || WorkbenchAssembly.isComplete()
                : !menu.tool().isEmpty();
    }

    private ItemStack morphPreview() {
        if (menu.getActiveTab() != 1 || !toolReady() || minecraft == null || minecraft.player == null) return ItemStack.EMPTY;
        ItemStack st = menu.input(0);
        if (st.isEmpty() || st.getItem() instanceof ToolPartItem || !stagedAttached()) return ItemStack.EMPTY;
        ToolUpgrade su = stagedUpgrade();
        if (su == null || !ToolUpgrades.canApply(minecraft.player, menu.tool(), su)) return ItemStack.EMPTY;
        try {
            ItemStack copy = menu.tool().copy();
            ToolStack.addModifier(copy, new ToolConstructionData.ModifierEntry(su.id(), ToolUpgrades.currentLevel(menu.tool(), su) + 1));
            ToolStack.recalculate(copy);
            return copy;
        } catch (RuntimeException e) {
            return ItemStack.EMPTY;
        }
    }

    private boolean stagedAttached() {
        return menu.getActiveTab() == 1 && !menu.input(0).isEmpty() && WorkbenchAssembly.isMerged(0);
    }

    private boolean morphReady() { return morphWas && morphAge >= 0.3f; }

    private boolean readyToCraft() {
        return menu.getActiveTab() == 0 && !menu.isRepair() && menu.output().isEmpty() && !menu.preview().isEmpty()
                && WorkbenchAssembly.isComplete();
    }

    private void syncCinematicScene() {
        List<ItemStack> inputs = new ArrayList<>(4);
        for (int i = 0; i < 4; i++) inputs.add(menu.getActiveTab() == 0 || (menu.getActiveTab() == 1 && i == 0) ? menu.input(i) : ItemStack.EMPTY);
        ItemStack scenePreview = menu.getActiveTab() == 0 ? menu.preview() : ItemStack.EMPTY;
        ItemStack morph = morphPreview();
        boolean morphOn = !morph.isEmpty();
        if (morphOn && !morphWas) WorkbenchCraft.bumpMorph();
        morphAge = morphOn ? (morphWas ? morphAge + dt : 0f) : 0f;
        morphWas = morphOn;
        WorkbenchSession.updateItems(menu.getBlockEntity().getBlockPos(), inputs, scenePreview, morphOn ? morph : menu.tool());
        WorkbenchAssembly.sync(inputs, menu.getActiveTab() == 0 && !menu.isRepair() ? menu.parts().size() : 0);

        if (menu.getActiveTab() == 0 && menu.isRepair() && menu.isRepairMaterial(menu.input(0))) WorkbenchAssembly.attachNow(0, menu.input(0));
        boolean showGhost = menu.getActiveTab() == 0 && !menu.isRepair() && menu.output().isEmpty()
                && menu.getSelectedTool() >= 0 && menu.getSelectedTool() < ToolAssembly.REGISTRY.size();
        WorkbenchAssembly.setGhost(showGhost
                ? new ItemStack(ToolAssembly.REGISTRY.get(Mth.clamp(menu.getSelectedTool(), 0, ToolAssembly.REGISTRY.size() - 1)).result())
                : ItemStack.EMPTY);
        ItemStack solid = ItemStack.EMPTY;
        if (showGhost && WorkbenchAssembly.mergedCount() > 0) {
            List<MaterialId> mats = new ArrayList<>();
            MaterialId fallback = null;
            for (int i = 0; i < menu.parts().size() && i < 4; i++)
                if (menu.input(i).getItem() instanceof ToolPartItem tp && !tp.getMaterial(menu.input(i)).isEmpty()) { fallback = tp.getMaterial(menu.input(i)); break; }
            for (int i = 0; i < menu.parts().size() && i < 4; i++) {
                MaterialId m = menu.input(i).getItem() instanceof ToolPartItem tp ? tp.getMaterial(menu.input(i)) : null;
                mats.add(m != null && !m.isEmpty() ? m : fallback);
            }
            if (fallback != null) solid = ToolStack.createTool(new ItemStack(menu.assembly().result()), mats);
        }
        WorkbenchAssembly.setGhostSolid(solid);
        compare = buildCompare();
        boolean anyPart = false;
        for (ItemStack s : inputs) anyPart |= !s.isEmpty();
        boolean working = menu.getActiveTab() == 0 || !menu.tool().isEmpty();
        WorkbenchSession.setToolFocus(working);
        if (inspect && !hasSubject()) inspect = false;
        WorkbenchSession.setInspect(inspect && !WorkbenchCraft.isBusy());
        menu.setInspecting(inspect || WorkbenchCraft.isBusy());
    }

    private boolean in(double mx, double my, int x, int y, int w, int h) { return mx >= x && mx < x + w && my >= y && my < y + h; }

    private void fill(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) { g.fill(x, y, x + w, y + h, color); }

    private void border(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        fill(g, x, y, w, 1, color);
        fill(g, x, y + h - 1, w, 1, color);
        fill(g, x, y + 1, 1, h - 2, color);
        fill(g, x + w - 1, y + 1, 1, h - 2, color);
    }

    private void seal(GuiGraphicsExtractor g, int x, int y, boolean ok) {
        g.blitSprite(RenderPipelines.GUI_TEXTURED, ok ? SPR_CHECK : SPR_CROSS, x, y, 12, 12);
    }

    private float fit(Component c, int width, float scale) {
        return Math.min(scale * TEXT_BOOST, width / (float) Math.max(1, font.width(c)));
    }

    private void drawText(GuiGraphicsExtractor g, Component text, int x, int y, float scale, int color) {
        measure(y + Math.round(9 * scale));
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(scale, scale);
        g.text(font, text, 0, 0, color, bright(color));
        g.pose().popMatrix();
    }

    private static boolean bright(int color) {
        int r = (color >> 16) & 255, gr = (color >> 8) & 255, b = color & 255;
        return r * 299 + gr * 587 + b * 114 > 150_000;
    }

    private void text(GuiGraphicsExtractor g, Component text, int x, int y, int width, float scale, int color) {
        drawText(g, text, x, y, fit(text, width, scale), color);
    }

    private void textRight(GuiGraphicsExtractor g, Component text, int right, int y, int width, float scale, int color) {
        float s = fit(text, width, scale);
        drawText(g, text, right - Math.round(font.width(text) * s), y, s, color);
    }

    private void centered(GuiGraphicsExtractor g, Component label, int cx, int y, int width, float scale, int color) {
        float s = fit(label, width, scale);
        drawText(g, label, cx - Math.round(font.width(label) * s / 2), y, s, color);
    }

    private int wrapped(GuiGraphicsExtractor g, Component label, int x, int y, int width, float scale, int color, int maxLines) {
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        scale *= TEXT_BOOST;
        g.pose().scale(scale, scale);
        int lineY = 0, lines = 0;
        for (FormattedCharSequence line : font.split(label, (int) (width / scale))) {
            if (lines++ >= maxLines) break;
            g.text(font, line, 0, lineY, color, bright(color));
            lineY += font.lineHeight + 2;
        }
        g.pose().popMatrix();
        measure(y + Math.round(lineY * scale));
        return y + Math.round(lineY * scale);
    }

    private void bar(GuiGraphicsExtractor g, int x, int y, int w, int h, float fraction, int color) {
        g.blitSprite(RenderPipelines.GUI_TEXTURED, SPR_BAR, x, y, w, h);
        int filled = Math.round((w - 2) * Mth.clamp(fraction, 0f, 1f));
        if (filled > 0) fill(g, x + 1, y + 1, filled, h - 2, color);
        if (filled > 0) fill(g, x + 1, y + 1, filled, 1, 0x40FFFFFF);
    }

    private void button(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean enabled, boolean hover, Component label) {
        measure(y + h);
        g.blitSprite(RenderPipelines.GUI_TEXTURED, !enabled ? SPR_BUTTON_OFF : hover ? SPR_BUTTON_HOVER : SPR_BUTTON, x, y, w, h);
        centered(g, label, x + w / 2, y + (h - 7) / 2, w - 6, .85f, enabled ? WHITE : 0xFF777777);
    }

    private void readyButton(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean enabled, boolean hover, Component label) {
        g.blitSprite(RenderPipelines.GUI_TEXTURED, !enabled ? SPR_BUTTON_OFF : hover ? SPR_BUTTON_HOVER : SPR_BUTTON, x, y, w, h);
        measure(y + h);
        float sc = Math.min(1f, (w - 6) / (float) Math.max(1, font.width(label)));
        int tx = x + w / 2 - Math.round(font.width(label) * sc / 2), ty = y + (h - 8) / 2;
        measure(ty + Math.round(9 * sc));
        g.pose().pushMatrix();
        g.pose().translate(tx, ty);
        g.pose().scale(sc, sc);
        g.text(font, label, 1, 1, enabled ? BTN_SHADOW : 0xFF0B0909, false);
        g.text(font, label, 0, 0, enabled ? (hover ? 0xFFFFFFFF : BTN_TEXT) : 0xFF6E6666, false);
        g.pose().popMatrix();
    }

    private void ghostItem(GuiGraphicsExtractor g, ItemStack stack, int x, int y, float alpha) {
        tintedItem(g, stack, x, y, argb(alpha, 1f, 1f, 1f));
    }

    private final net.minecraft.client.renderer.item.ItemStackRenderState ghostState = new net.minecraft.client.renderer.item.ItemStackRenderState();

    private void tintedItem(GuiGraphicsExtractor g, ItemStack stack, int x, int y, int color) {
        if (stack.isEmpty() || minecraft == null) return;
        if (ToolItemRenderer.layersFor(stack.getItem()) != null) {
            ToolItemRenderer.renderGuiPreviewTinted(g, stack, x, y, 16, color);
            return;
        }
        minecraft.getItemModelResolver().updateForTopItem(ghostState, stack, net.minecraft.world.item.ItemDisplayContext.GUI, minecraft.level, null, 0);
        var material = ghostState.pickParticleMaterial(net.minecraft.util.RandomSource.create(0L));
        if (material == null) return;
        g.blitSprite(RenderPipelines.GUI_TEXTURED, material.sprite(), x, y, 16, 16, color);
    }

    private static int argb(float a, float r, float gr, float b) {
        return ((int) (Mth.clamp(a, 0f, 1f) * 255) << 24) | ((int) (Mth.clamp(r, 0f, 1f) * 255) << 16)
                | ((int) (Mth.clamp(gr, 0f, 1f) * 255) << 8) | (int) (Mth.clamp(b, 0f, 1f) * 255);
    }

    public int getSlotColor(int index) { return 0x30FFFFFF; }

    @Override protected void extractLabels(GuiGraphicsExtractor g, int mx, int my) { }

    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float tick) {
        frameId++;
        tickAnimations();
        if (width != imageWidth || height != imageHeight || leftPos != 0 || topPos != 0) fitToWindow();
        syncCinematicScene();
        layoutSlots();
        updateHover(mx, my);
        super.extractRenderState(g, mx, my, tick);
        if (!WorkbenchCraft.isBusy()) drawTooltips(g, mx, my);
    }

    private void drawCraftProgress(GuiGraphicsExtractor g) {
        float m = WorkbenchCraft.mix();
        if (!WorkbenchCraft.isActive()) return;
        int w = 120, x = cx() - w / 2, y = height - 22;
        paperBar(g, x, y - 1, w, WorkbenchCraft.progress(), 0);
        centered(g, tr("crafting"), cx(), y - 12, 160, .8f, WHITE);
    }

    private void blitSpriteAlpha(GuiGraphicsExtractor g, Identifier sprite, int x, int y, int size, float alpha) {
        g.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, size, size, argb(alpha, 1f, 1f, 1f));
    }

    private void blitTex(GuiGraphicsExtractor g, Identifier tex, int x, int y, int size, float alpha) {
        g.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, 0f, 0f, size, size, 16, 16, 16, 16, argb(alpha, 1f, 1f, 1f));
    }

    private static Component trArgs(String key, Object... args) { return Component.translatable("gui.hephaestus_tools.build." + key, args); }

    private void tip(int x, int y, int w, int h, Component... lines) {
        tipRects.add(new int[] {x, y, w, h});
        tipTexts.add(List.of(lines));
        tipRich.add(null);
    }

    private void richTip(int x, int y, int w, int h, Tip t) {
        tipRects.add(new int[] {x, y, w, h});
        tipTexts.add(List.of());
        tipRich.add(t);
    }

    private void icon(GuiGraphicsExtractor g, ItemStack stack, int x, int y) {
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(0.625f, 0.625f);
        g.item(stack, 0, 0);
        g.pose().popMatrix();
    }

    private static ItemStack harvestIcon(com.titammods.hephaestus_tools.tools.stat.HarvestTier tier) {
        return new ItemStack(switch (tier.id()) {
            case "wood", "gold" -> net.minecraft.world.item.Items.COAL_ORE;
            case "stone" -> net.minecraft.world.item.Items.IRON_ORE;
            case "iron" -> net.minecraft.world.item.Items.DIAMOND_ORE;
            default -> net.minecraft.world.item.Items.OBSIDIAN;
        });
    }

    private static String fmt1(float f) { return String.format(Locale.ROOT, "%.1f", f); }

    private void collectTraits() {
        traitDetails.clear();
        traitRows.clear();
        if (details.isEmpty() || !ToolStack.isInitialized(details)) return;
        var materials = ToolStack.getMaterials(details);
        var parts = ToolBuildHandler.getToolParts(details.getItem());
        for (int i = 0; i < materials.size(); i++) {
            var mat = materials.get(i);
            var stats = MaterialManager.getInstance().getStatsForSlot(mat, 0);
            var trait = MaterialTrait.byId(stats.traitId());
            if (trait.isEmpty()) continue;
            Component traitName = Component.translatable("trait.hephaestus_tools." + trait.get().id());
            Component traitDesc = Component.translatable("trait.hephaestus_tools." + trait.get().id() + ".desc");
            Component materialName = Component.translatable("material.hephaestus_tools." + mat.id().getPath());
            Component partName = i < parts.size() ? new ItemStack(parts.get(i)).getHoverName() : Component.literal("");
            traitDetails.add(traitName.copy().append(" - ").append(materialName).append(" (").append(partName).append(")"));
            traitRows.add(new Component[] {traitName, traitDesc, materialName});
        }
    }

    private int categoryColor(ToolUpgrade up) {
        var r = up.roles();
        if (r.size() > 1) return 0xFF55FFFF;
        if (r.contains(ToolRole.COMBAT)) return 0xFFFF5555;
        if (r.contains(ToolRole.MINING)) return 0xFF55FF55;
        return 0xFFFFFF55;
    }

    private String categoryKey(ToolUpgrade up) {
        var r = up.roles();
        if (r.size() > 1) return "cat_utility";
        if (r.contains(ToolRole.COMBAT)) return "cat_combat";
        if (r.contains(ToolRole.MINING)) return "cat_mining";
        return "cat_harvest";
    }

    private static final Identifier TEX_DIAMOND_FILL = rl("diamond_fill.png"), TEX_DIAMOND_BORDER = rl("diamond_border.png");
    private static final int BADGE = 22, BADGE_EDGE = 0xFFE86C44, BADGE_SEL = 0xFFFFDFC0, BADGE_MAX = 0xFFFFC857, BADGE_DIM = 0xFF8A4A38,
            BADGE_FILL = 0xFF451F1A, BADGE_FILL_SEL = 0xFF5E2A21;

    private void badge(GuiGraphicsExtractor g, Identifier icon, int x, int y, int edge, int fill, float alpha) {
        int a = (int) (Mth.clamp(alpha, 0f, 1f) * 255) << 24;
        g.blit(RenderPipelines.GUI_TEXTURED, TEX_DIAMOND_FILL, x, y, 0f, 0f, BADGE, BADGE, BADGE, BADGE, BADGE, BADGE, a | (fill & 0xFFFFFF));
        g.blit(RenderPipelines.GUI_TEXTURED, icon, x + 3, y + 3, 0f, 0f, 16, 16, 16, 16, 16, 16, a | 0xFFFFFF);
        g.blit(RenderPipelines.GUI_TEXTURED, TEX_DIAMOND_BORDER, x, y, 0f, 0f, BADGE, BADGE, BADGE, BADGE, BADGE, BADGE, a | (edge & 0xFFFFFF));
    }

    private void levelSquares(GuiGraphicsExtractor g, int x, int y, int level, int max, int size, boolean centerOnX) {
        int w = 22;
        paperBar(g, centerOnX ? x - w / 2 : x, y, w, level / (float) Math.max(1, max), 0);
    }

    private void applyUpgrade(ToolUpgrade up) {
        int serverIndex = up == null ? -1 : ToolUpgrades.availableFor(menu.tool().getItem()).indexOf(up);
        if (serverIndex >= 0 && ToolUpgrades.canApply(minecraft.player, menu.tool(), up)) startApply(serverIndex);
    }

    private static Component sq(int filled, int total) {
        return Component.literal("\u25A0".repeat(Math.max(0, filled))).withColor(0xFFAA00)
                .append(Component.literal("\u25A0".repeat(Math.max(0, total - filled))).withColor(0x555555));
    }

    private List<Component> wrapGray(Component c, int px) {
        List<Component> out = new ArrayList<>();
        for (var line : font.getSplitter().splitLines(c, px, net.minecraft.network.chat.Style.EMPTY))
            out.add(Component.literal(line.getString()).withStyle(ChatFormatting.GRAY));
        return out;
    }

    private Component[] modifierTooltip(ToolUpgrade up, int level) {
        List<Component> t = new ArrayList<>();
        int cat = categoryColor(up), maxL = up.maxLevel();
        t.add(Component.translatable(up.nameKey()).withColor(cat & 0xFFFFFF));
        Component sub = tr(categoryKey(up)).copy().withStyle(ChatFormatting.DARK_GRAY);
        if (level > 0) sub = sub.copy().append(Component.literal("  Lv " + level + "/" + maxL + "  ").withColor(0xFFAA00)).append(sq(level, maxL));
        t.add(sub);
        t.addAll(wrapGray(Component.translatable("gui.hephaestus_tools.upgrade." + up.id().getPath() + ".desc"), 170));
        if (level >= maxL) {
            t.add(Component.empty());
            t.add(tr("max_level").copy().withStyle(ChatFormatting.GOLD));
            return t.toArray(new Component[0]);
        }
        t.add(Component.empty());
        t.add(tr("next_level").copy().withStyle(ChatFormatting.DARK_GRAY));
        try {
            ItemStack after = details.copy();
            ToolStack.addModifier(after, new ToolConstructionData.ModifierEntry(up.id(), level + 1));
            ToolStack.recalculate(after);
            String[] keys = {"stat_durability", "stat_speed", "stat_damage", "stat_agility"};
            float[] a = {ToolStack.getDurability(details), ToolStack.getMiningSpeed(details), ToolStack.getAttackDamage(details), ToolStack.getAttackSpeed(details)};
            float[] b = {ToolStack.getDurability(after), ToolStack.getMiningSpeed(after), ToolStack.getAttackDamage(after), ToolStack.getAttackSpeed(after)};
            boolean any = false;
            for (int i = 0; i < 4; i++) {
                if (Math.abs(b[i] - a[i]) < 0.001f) continue;
                any = true;
                String from = i == 0 ? String.valueOf(Math.round(a[i])) : fmt1(a[i]);
                String to = i == 0 ? String.valueOf(Math.round(b[i])) : fmt1(b[i]);
                t.add(Component.literal(" ").append(tr(keys[i])).append(": " + from + " \u2192 " + to).withStyle(b[i] > a[i] ? ChatFormatting.GREEN : ChatFormatting.RED));
            }
            if (!any) t.add(Component.literal(" ").append(tr("effect_special")).withStyle(ChatFormatting.GRAY));
        } catch (RuntimeException ignored) { }
        int owned = ToolUpgrades.countIn(minecraft.player, up.costItem()), need = up.costFor(level + 1);
        t.add(Component.literal(need + " x ").append(new ItemStack(up.costItem()).getHoverName()).append("  (" + owned + ")")
                .withStyle(owned >= need ? ChatFormatting.GREEN : ChatFormatting.RED));
        t.add(Component.empty());
        t.add(tr(level > 0 ? "click_upgrade" : "click_install").copy().withStyle(ChatFormatting.YELLOW));
        return t.toArray(new Component[0]);
    }

    private List<ToolConstructionData.ModifierEntry> installedModifiers() {
        var unique = new LinkedHashMap<Identifier, ToolConstructionData.ModifierEntry>();
        for (var entry : ToolStack.getModifiers(menu.tool())) if (entry.level() > 0) unique.put(entry.id(), entry);
        return List.copyOf(unique.values());
    }

    private Identifier installedIcon(Identifier id) {
        if (!id.getNamespace().equals(HephaestusTools.MOD_ID)) return null;
        String name = id.getPath();
        if (name.equals("looting")) name = "fortune";
        return List.of("reinforced", "haste", "fortune", "sharpness", "silk_touch", "flame", "sweeping", "looting").contains(name)
                ? rl("workbench/modifier_icon/modifier_" + (name.equals("silk_touch") ? "silky" : name) + ".png") : null;
    }

    private boolean partsListVisible() { return menu.getActiveTab() == 0 && !menu.isRepair() && !toolsOpen && !menu.parts().isEmpty(); }

    private int countInInventory(ItemStack part) {
        if (minecraft == null || minecraft.player == null) return 0;
        int n = 0;
        var inv = minecraft.player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) if (inv.getItem(i).is(part.getItem())) n += inv.getItem(i).getCount();
        return n;
    }

    private boolean repairVisible() {
        ItemStack out = menu.output();
        return menu.isRepair() && (ToolStack.getCurrentDamage(out) > 0 || !menu.input(0).isEmpty());
    }

    private ItemStack repairMaterial() {
        try {
            var id = ToolStack.getMaterial(menu.output(), 0);
            var mat = id == null ? null : MaterialManager.getInstance().getMaterial(id);
            if (mat == null) return ItemStack.EMPTY;
            var ing = mat.ingredient();
            if (!ing.tag()) return new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(ing.id()));
            for (var holder : net.minecraft.core.registries.BuiltInRegistries.ITEM.getTagOrEmpty(
                    net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, ing.id())))
                return new ItemStack(holder);
            return ItemStack.EMPTY;
        } catch (RuntimeException e) {
            return ItemStack.EMPTY;
        }
    }

    private boolean canFill() {
        if (menu.getActiveTab() != 0 || menu.isRepair() || !menu.output().isEmpty() || !menu.getCarried().isEmpty()) return false;
        for (int i = 0; i < menu.parts().size() && i < 4; i++) if (menu.input(i).isEmpty()) return true;
        return false;
    }

    private ItemStack buildCompare() {
        if (menu.getActiveTab() != 0 || menu.isRepair()) return ItemStack.EMPTY;
        ItemStack carried = menu.getCarried();
        ItemStack base = menu.preview();
        if (base.isEmpty() || !ToolStack.isInitialized(base) || !(carried.getItem() instanceof ToolPartItem cp)) return ItemStack.EMPTY;
        try {
            MaterialId mat = cp.getMaterial(carried);
            if (mat == null || mat.isEmpty()) return ItemStack.EMPTY;
            List<MaterialId> mats = new ArrayList<>(ToolStack.getMaterials(base));
            for (int i = 0; i < menu.parts().size() && i < mats.size(); i++) {
                if (!carried.is(menu.parts().get(i))) continue;
                if (mat.equals(mats.get(i))) return ItemStack.EMPTY;
                mats.set(i, mat);
                return ToolStack.createTool(new ItemStack(menu.assembly().result()), mats);
            }
        } catch (RuntimeException ignored) {
        }
        return ItemStack.EMPTY;
    }

    private void drawPartLabel(GuiGraphicsExtractor g) {
        if (WorkbenchAssembly.isDraggingAny() || !menu.getCarried().isEmpty() || minecraft == null || minecraft.level == null) return;
        int part = -1;
        for (int i = 0; i < 4; i++) if (WorkbenchAssembly.isHover(i)) part = i;
        if (part < 0 || menu.input(part).isEmpty()) return;
        float[] s = WorkbenchAssembly.padToScreen(WorkbenchAssembly.forward(part), WorkbenchAssembly.across(part), width, height);
        if (s == null) return;
        ItemStack stack = menu.input(part);
        List<Component> lines = new ArrayList<>();
        try {
            lines.addAll(stack.getTooltipLines(Item.TooltipContext.of(minecraft.level), minecraft.player, TooltipFlag.NORMAL));
        } catch (RuntimeException e) {
            lines.add(stack.getHoverName());
        }
        if (lines.isEmpty()) return;
        if (lines.size() > 4) lines = lines.subList(0, 4);
        g.setComponentTooltipForNextFrame(font, lines, Math.round(s[0]) - 12, Math.round(s[1]) - 10);
    }

    private void drawDrawer(GuiGraphicsExtractor g, int mx, int my) {
        if (drawerT <= 0.001f) return;
        float e = ease(drawerT);
        int slide = Math.round((1f - outBack(drawerT)) * 16f);
        int gx = drawerX(), gy = drawerY() + slide;
        g.blit(RenderPipelines.GUI_TEXTURED, TEX_DRAWER, gx - 7, gy - 7, 0f, 0f, DRAWER_W + 14, DRAWER_H + 14, DRAWER_W + 14, DRAWER_H + 14, DRAWER_W + 14, DRAWER_H + 14, argb(e, 1f, 1f, 1f));
        if (menu.isInventoryVisible()) {
            for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) {
                int sx = gx + col * 18, sy = gy + row * 18;
                if (in(mx, my, sx, sy, 18, 18)) g.blitSprite(RenderPipelines.GUI_TEXTURED, SPR_DSLOT_HOVER, sx, sy, 18, 18, argb(e, 1f, 1f, 1f));
            }
        }
        if (!menu.isInventoryVisible()) {
            for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) {
                ItemStack st = menu.slots.get(6 + col + row * 9).getItem();
                if (st.isEmpty()) continue;
                g.item(st, invX() + col * 18, invY() + row * 18 + slide);
                g.itemDecorations(font, st, invX() + col * 18, invY() + row * 18 + slide);
            }
        }
    }

    private void updateHover(int mx, int my) {
        int hover = -1;
        if (menu.getActiveTab() <= 1 && !inspect && !WorkbenchCraft.isBusy() && !WorkbenchAssembly.isDraggingAny()
                && menu.getCarried().isEmpty() && hoveredSlot == null && freeSpace(mx, my)) {
            float[] p = WorkbenchAssembly.mouseToPad(mx, my, width, height);
            if (p != null) hover = WorkbenchAssembly.pick(p[0], p[1]);
        }
        WorkbenchAssembly.setHover(hover);
    }

    private void entryTooltip(GuiGraphicsExtractor g, int index, int mx, int my) {
        var lines = new ArrayList<FormattedCharSequence>();
        lines.addAll(font.split(entryName(index).copy().withStyle(ChatFormatting.GOLD), 180));
        lines.addAll(font.split(entryDescription(index), 180));
        if (menu.getActiveTab() == 1) {
            var up = upgrades().get(index);
            int level = ToolUpgrades.currentLevel(menu.tool(), up);
            lines.add(Component.literal(level + " / " + up.maxLevel()).getVisualOrderText());
            if (level < up.maxLevel()) {
                int owned = ToolUpgrades.countIn(minecraft.player, up.costItem());
                Component cost = tr("materials_required").copy().append(": " + up.costFor(level + 1) + " x ")
                        .append(new ItemStack(up.costItem()).getHoverName());
                lines.addAll(font.split(cost, 180));
                lines.addAll(font.split(tr("materials_owned").copy().append(": " + owned)
                        .withStyle(owned >= up.costFor(level + 1) ? ChatFormatting.GREEN : ChatFormatting.RED), 180));
                if (owned < up.costFor(level + 1)) lines.addAll(font.split(
                        Component.translatable("gui.hephaestus_tools.build.materials_missing", up.costFor(level + 1) - owned)
                                .withStyle(ChatFormatting.RED), 180));
                if (level == 0 && ToolUpgrades.usedSlots(menu.tool()) >= ToolUpgrades.unlockedSlots(menu.tool()))
                    lines.addAll(font.split(tr("no_modifier_slots").copy().withStyle(ChatFormatting.RED), 180));
                if (ToolUpgrades.canAfford(minecraft.player, menu.tool(), up))
                    lines.addAll(font.split(tr("ready_to_apply").copy().withStyle(ChatFormatting.GREEN), 180));
            } else lines.add(tr("max_level").getVisualOrderText());
        } else {
            String chosen = ToolMastery.selected(menu.tool());
            lines.addAll(font.split(tr(chosen.isEmpty() ? "mastery_permanent" : "mastery_locked")
                    .copy().withStyle(ChatFormatting.GOLD), 180));
            var requirement = Component.translatable("gui.hephaestus_tools.build.mastery_requires_level", MasteryLevel.T1);
            if (ToolXp.getLevel(menu.tool()) < MasteryLevel.T1) requirement.withStyle(ChatFormatting.RED);
            lines.addAll(font.split(requirement, 180));
        }
        g.setTooltipForNextFrame(font, lines, mx, my);
    }

    private List<ToolUpgrade> upgrades() { return visibleUpgrades; }

    private void refreshEntries() {
        if (menu.getActiveTab() == 1) {
            Identifier previous = selectedEntry >= 0 && selectedEntry < visibleUpgrades.size()
                    ? visibleUpgrades.get(selectedEntry).id() : null;
            var all = ToolUpgrades.availableFor(menu.tool().getItem());
            var previousList = visibleUpgrades;
            visibleUpgrades = List.copyOf(all);
            selectedEntry = 0;
            for (int i = 0; i < visibleUpgrades.size(); i++)
                if (visibleUpgrades.get(i).id().equals(previous)) { selectedEntry = i; break; }
            if (!visibleUpgrades.equals(previousList)) {
                if (selectedEntry < listOffset) listOffset = selectedEntry;
                else if (selectedEntry >= listOffset + visibleRows()) listOffset = selectedEntry - visibleRows() + 1;
            }
        } else {
            String chosen = ToolMastery.selected(menu.tool());
            var paths = ToolMastery.forTool(menu.tool().getItem());
            if (!chosen.isEmpty()) selectedEntry = Math.max(0, paths.indexOf(chosen));
            else selectedEntry = Math.min(selectedEntry, Math.max(0, paths.size() - 1));
        }
        listOffset = Math.max(0, Math.min(listOffset, Math.max(0, entryCount() - visibleRows())));
    }

    private int entryCount() {
        return menu.getActiveTab() == 1 ? upgrades().size() : ToolMastery.forTool(menu.tool().getItem()).size();
    }

    private Component entryName(int index) {
        return menu.getActiveTab() == 1 ? Component.translatable(upgrades().get(index).nameKey())
                : Component.translatable("mastery.hephaestus_tools." + ToolMastery.forTool(menu.tool().getItem()).get(index));
    }

    private Identifier entryIcon(int index) {
        if (menu.getActiveTab() == 1) {
            String id = upgrades().get(index).id().getPath();
            return rl("workbench/modifier_icon/modifier_" + (id.equals("looting") ? "fortune" : id.equals("silk_touch") ? "silky" : id) + ".png");
        }
        return rl("workbench/masterys/" + ToolMastery.toolId(menu.tool().getItem()) + "/"
                + ToolMastery.forTool(menu.tool().getItem()).get(index) + ".png");
    }

    private Component entryDescription() { return entryDescription(selectedEntry); }

    private Component entryDescription(int index) {
        String id = menu.getActiveTab() == 1 ? upgrades().get(index).id().getPath()
                : ToolMastery.forTool(menu.tool().getItem()).get(index);
        return Component.translatable((menu.getActiveTab() == 1 ? "gui.hephaestus_tools.upgrade." : "mastery.hephaestus_tools.") + id + ".desc");
    }

    private static String fmt(float f) { return String.format(Locale.ROOT, "%.2f", f); }

    private void send(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1f));
        }
    }

    private void denied() {
        if (minecraft == null) return;
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.DISPENSER_FAIL, 0.9f, 0.85f));
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.CHAIN_HIT, 0.7f, 0.6f));
    }

    private void click() {
        if (minecraft != null) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1f));
    }

    private int visibleRows() { return menu.getActiveTab() == 1 ? MOD_ROWS : POP_ROWS; }

    private boolean dropOnTool(double mx, double my) {
        ItemStack carried = menu.getCarried();
        if (carried.isEmpty()) return false;
        float[] spot = WorkbenchAssembly.mouseToPad(mx, my, width, height);
        if (spot != null) WorkbenchAssembly.setDropSpot(spot[0], spot[1]);
        if (menu.getActiveTab() == 0) {
            for (int i = 0; i < 4; i++) {
                Slot slot = menu.slots.get(i + 1);
                if (slot.isActive() && slot.getItem().isEmpty() && slot.mayPlace(carried)) {
                    slotClicked(slot, slot.index, 0, ContainerInput.PICKUP);
                    return true;
                }
            }
        } else {
            Slot slot = menu.slots.get(0);
            if (slot.isActive() && slot.getItem().isEmpty() && slot.mayPlace(carried)) {
                slotClicked(slot, slot.index, 0, ContainerInput.PICKUP);
                return true;
            }
            Slot st = menu.slots.get(1);
            if (menu.getActiveTab() == 1 && toolReady() && st.isActive() && st.mayPlace(carried)
                    && (st.getItem().isEmpty() || ItemStack.isSameItemSameComponents(st.getItem(), carried))) {
                ToolUpgrade up = null;
                for (ToolUpgrade u : upgrades()) if (u.costItem() == carried.getItem()) { up = u; break; }
                int put = carried.getCount();
                if (up != null) {
                    int missing = up.costFor(ToolUpgrades.currentLevel(menu.tool(), up) + 1) - (st.getItem().isEmpty() ? 0 : st.getItem().getCount());
                    if (missing <= 0) { denied(); return true; }
                    put = Math.min(put, missing);
                }
                if (put >= carried.getCount()) slotClicked(st, st.index, 0, ContainerInput.PICKUP);
                else for (int k = 0; k < put; k++) slotClicked(st, st.index, 1, ContainerInput.PICKUP);
                if (up != null) { selectedEntry = Math.max(0, upgrades().indexOf(up)); ensureVisible(); }
                return true;
            }
            if (menu.getActiveTab() == 1 && toolReady() && spot != null && overTool(spot)) denied();
        }
        return false;
    }

    private void releaseBench(int newSel) {
        if (menu.getActiveTab() != 1) return;
        Slot st = menu.slots.get(1);
        if (!st.isActive() || st.getItem().isEmpty()) return;
        var ups = upgrades();
        if (newSel >= 0 && newSel < ups.size() && st.getItem().getItem() == ups.get(newSel).costItem()) return;
        slotClicked(st, st.index, 0, ContainerInput.QUICK_MOVE);
    }

    private static boolean overTool(float[] pad) { return pad[0] * pad[0] + pad[1] * pad[1] < 0.36f * 0.36f; }

    private void setHideUi(boolean value) {
        hideUi = value;
        if (value) { toolsOpen = false; menu.setInventoryVisible(false); }
    }

    private void fillParts() {
        WorkbenchAssembly.expectAutoAttach();
        send(WorkbenchMenu.AUTOFILL);
        click();
    }

    private boolean canFillModifier() {
        if (menu.getActiveTab() != 1 || !toolReady() || !menu.getCarried().isEmpty() || minecraft == null || minecraft.player == null || entryCount() == 0) return false;
        ToolUpgrade up = upgrades().get(Mth.clamp(selectedEntry, 0, entryCount() - 1));
        int lvl = ToolUpgrades.currentLevel(menu.tool(), up);
        if (lvl >= up.maxLevel()) return false;
        ItemStack bench = menu.input(0);
        int have = bench.is(up.costItem()) ? bench.getCount() : 0;
        if (have >= Math.min(99, up.costFor(lvl + 1))) return false;
        var inv = minecraft.player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) if (inv.getItem(i).is(up.costItem())) return true;
        return false;
    }

    private void fillModifier() {
        ToolUpgrade up = upgrades().get(Mth.clamp(selectedEntry, 0, entryCount() - 1));
        int serverIndex = ToolUpgrades.availableFor(menu.tool().getItem()).indexOf(up);
        if (serverIndex < 0) return;
        WorkbenchAssembly.expectAutoAttach();
        if (menu.input(0).is(up.costItem())) WorkbenchAssembly.attachNow(0, menu.input(0));
        send(WorkbenchMenu.FILL_MOD_BASE + serverIndex);
        click();
    }

    private int buildSwapIndex() {
        ItemStack staged = menu.input(0), tool = menu.output();
        int pi = WorkbenchMenu.partIndexFor(tool, staged);
        return pi >= 0 && WorkbenchMenu.canSwap(tool, staged, pi) ? pi : -1;
    }

    private boolean canRepairNow() {
        return menu.isRepair() && ToolStack.getCurrentDamage(menu.output()) > 0 && menu.isRepairMaterial(menu.input(0));
    }

    private void startRepair() {
        if (WorkbenchCraft.isBusy()) return;
        inspect = false;
        click();
        if (skipCinematic()) { send(WorkbenchMenu.REPAIR); quickDone(); return; }
        if (WorkbenchCraft.startTinker(menu.output(), ItemStack.EMPTY, () -> {
            menu.setInspecting(false);
            send(WorkbenchMenu.REPAIR);
        })) announce(true);
    }

    private void startSwap(int partIndex) {
        if (WorkbenchCraft.isBusy()) return;
        inspect = false;
        click();
        ItemStack part = menu.input(0).copyWithCount(1);
        Runnable done = () -> {
            menu.setInspecting(false);
            send(WorkbenchMenu.SWAP_BASE + partIndex);
            WorkbenchAssembly.attachFx(part);
        };
        if (skipCinematic()) { done.run(); quickDone(); return; }
        if (WorkbenchCraft.startTinker(menu.tool(), part, done)) announce(true);
    }

    private void startCraft() {
        if (!readyToCraft() || !menu.getCarried().isEmpty() || WorkbenchCraft.isBusy()) return;
        inspect = false;
        if (skipCinematic()) { finishCraft(); quickDone(); return; }
        click();
        if (WorkbenchCraft.start(menu.preview(), this::finishCraft)) announce(false);
    }

    private boolean skipCinematic() {
        return (minecraft != null && minecraft.hasShiftDown()) || WorkbenchTuningStore.skipCinematics();
    }

    private void quickDone() {
        if (minecraft != null) minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.SMITHING_TABLE_USE, 1.0f, 0.8f));
    }

    private void announce(boolean tinker) {
        ClientPacketDistributor.sendToServer(new CraftAnimPayload(menu.getBlockEntity().getBlockPos(), tinker));
    }

    private void finishCraft() {
        menu.setInspecting(false);
        if (menu.getActiveTab() != 0 || minecraft == null || minecraft.player == null) return;
        if (menu.preview().isEmpty() || !menu.output().isEmpty()) return;
        send(WorkbenchMenu.ASSEMBLE);
    }

    private void startApply(int serverIndex) {
        if (WorkbenchCraft.isBusy()) return;
        inspect = false;
        click();
        var ups = ToolUpgrades.availableFor(menu.tool().getItem());
        ItemStack material = serverIndex >= 0 && serverIndex < ups.size() ? new ItemStack(ups.get(serverIndex).costItem()) : ItemStack.EMPTY;
        if (skipCinematic()) { send(WorkbenchMenu.APPLY_BASE + serverIndex); quickDone(); return; }
        ItemStack shown = morphPreview();
        if (shown.isEmpty()) shown = menu.tool();
        if (WorkbenchCraft.startTinker(shown, material, () -> {
            menu.setInspecting(false);
            send(WorkbenchMenu.APPLY_BASE + serverIndex);
        })) announce(true);
    }

    @Override protected void slotClicked(Slot slot, int slotId, int button, ContainerInput type) {
        if (slot != null && slot.index == 5 && menu.output().isEmpty() && menu.getCarried().isEmpty() && type != ContainerInput.SWAP) return;
        super.slotClicked(slot, slotId, button, type);
    }

    @Override public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        double mx = event.x(), my = event.y();
        if (WorkbenchAssembly.isDraggingAny()) {
            float[] p = WorkbenchAssembly.mouseToPad(mx, my, width, height);
            if (p != null) WorkbenchAssembly.drag(p[0], p[1]);
            return true;
        }
        if (scrollDrag) { scrollTo(my); return true; }
        if (rotating) { WorkbenchSession.rotateView(dx * 0.6, dy * 0.4); return true; }
        return super.mouseDragged(event, dx, dy);
    }

    @Override public boolean mouseReleased(MouseButtonEvent event) {
        double mx = event.x(), my = event.y();
        int button = event.button();
        if (WorkbenchAssembly.isDraggingAny()) {
            int d = WorkbenchAssembly.draggingIndex();
            if (WorkbenchAssembly.endDrag() && d >= 0) {
                WorkbenchAssembly.attachFx(menu.input(d));
                if (menu.getActiveTab() == 0 && menu.isRepair() && d == 0 && menu.input(0).getItem() instanceof ToolPartItem) {
                    int pi = buildSwapIndex();
                    if (pi >= 0) send(WorkbenchMenu.SWAP_BASE + pi);
                    else { WorkbenchAssembly.release(0); denied(); }
                }
            }
            return true;
        }
        if (scrollDrag) { scrollDrag = false; return true; }
        if (rotating) { rotating = false; WorkbenchSession.releaseView(); return true; }
        if (button == 0 && !inspect && !menu.getCarried().isEmpty() && hoveredSlot == null && freeSpace(mx, my) && dropOnTool(mx, my)) return true;
        return super.mouseReleased(event);
    }

    @Override public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        if (WorkbenchCraft.isBusy()) return true;
        if (needScroll() && !inspect && in(mx, my, lIn() - 6, lTop(), inW() + 12, listH() + 30)) {
            listOffset = Mth.clamp(listOffset - (int) Math.signum(vertical), 0, maxOffset());
            return true;
        }
        if (hoveredSlot == null && freeSpace(mx, my)) { WorkbenchSession.zoomBy(vertical); return true; }
        return super.mouseScrolled(mx, my, horizontal, vertical);
    }
}