package com.titammods.hephaestus_tools.client;

import com.titammods.hephaestus_tools.HephaestusTools;
import com.titammods.hephaestus_tools.client.renderer.ArsenalTableRenderer;
import com.titammods.hephaestus_tools.client.renderer.ToolItemRenderer;
import com.titammods.hephaestus_tools.client.workbench.AnimHammer;
import com.titammods.hephaestus_tools.client.workbench.WorkbenchHandLayer;
import com.titammods.hephaestus_tools.client.workbench.WorkbenchPlayerAnim;
import com.titammods.hephaestus_tools.registry.ModBlocks;
import com.titammods.hephaestus_tools.registry.ModMenus;
import com.titammods.hephaestus_tools.tables.screen.ArsenalTableScreen;
import com.titammods.hephaestus_tools.tables.screen.WorkbenchScreen;
import com.titammods.hephaestus_tools.tools.helper.ToolEnchantments;
import com.titammods.hephaestus_tools.tools.item.ModifiableItem;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterConditionalItemModelPropertyEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = HephaestusTools.MOD_ID, value = Dist.CLIENT)
public class ClientModEvents {

    public static final Identifier TOOL_TINT =
            Identifier.fromNamespaceAndPath(HephaestusTools.MOD_ID, "tool_material");
    public static final Identifier PART_TINT =
            Identifier.fromNamespaceAndPath(HephaestusTools.MOD_ID, "part_material");
    public static final Identifier WORKBENCH_STYLE =
            Identifier.fromNamespaceAndPath(HephaestusTools.MOD_ID, "workbench_style");

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.ARSENAL_TABLE.get(), ArsenalTableScreen::new);
        event.register(ModMenus.WORKBENCH.get(), WorkbenchScreen::new);
    }

    @SubscribeEvent
    public static void registerItemTintSources(RegisterColorHandlersEvent.ItemTintSources event) {
        event.register(TOOL_TINT, ToolColorHandler.Tint.MAP_CODEC);
        event.register(PART_TINT, PartColorHandler.Tint.MAP_CODEC);
    }

    @SubscribeEvent
    public static void registerConditionalProperties(RegisterConditionalItemModelPropertyEvent event) {
        event.register(WORKBENCH_STYLE, WorkbenchStyleProperty.MAP_CODEC);
    }

    @SubscribeEvent
    public static void registerStandaloneModels(ModelEvent.RegisterStandalone event) {
        AnimHammer.register(event);
    }

    @SubscribeEvent
    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerModelType skin : event.getSkins()) {
            AvatarRenderer<AbstractClientPlayer> renderer = event.getPlayerRenderer(skin);
            if (renderer != null) renderer.addLayer(new WorkbenchHandLayer(renderer));
        }
    }

    @SubscribeEvent
    public static void registerSpecialModelRenderers(RegisterSpecialModelRendererEvent event) {
        ToolItemRenderer.Unbaked unbaked = new ToolItemRenderer.Unbaked();
        event.register(ToolItemRenderer.RENDERER_ID, unbaked.type());
    }

    @SubscribeEvent
    public static void registerReloadListeners(AddClientReloadListenersEvent event) {
        ResourceManagerReloadListener listener = manager -> ToolItemRenderer.clearTextureCache();
        event.addListener(Identifier.fromNamespaceAndPath(HephaestusTools.MOD_ID, "tool_overlay_cache"), listener);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (!(event.getItemStack().getItem() instanceof ModifiableItem)) return;
        event.getToolTip().removeIf(ToolEnchantments::isMergedLine);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            BlockEntityRenderers.register(ModBlocks.ARSENAL_TABLE_BE.get(), ArsenalTableRenderer::new);
            WorkbenchPlayerAnim.register();
        });
    }
}
