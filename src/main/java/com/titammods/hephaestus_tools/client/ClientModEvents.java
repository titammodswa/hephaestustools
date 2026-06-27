package com.titammods.hephaestus_tools.client;

import com.titammods.hephaestus_tools.HephaestusTools;
import com.titammods.hephaestus_tools.registry.ModItems;
import com.titammods.hephaestus_tools.registry.ModMenus;
import com.titammods.hephaestus_tools.tables.screen.PartBuilderScreen;
import com.titammods.hephaestus_tools.tables.screen.TinkersAnvilScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import com.titammods.hephaestus_tools.client.renderer.ToolItemRenderer;

@EventBusSubscriber(modid = HephaestusTools.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {

    private static final java.util.function.Supplier<net.minecraft.world.item.Item[]> TOOLS = () ->
            new net.minecraft.world.item.Item[] {
                    ModItems.PICKAXE.get(), ModItems.SLEDGE_HAMMER.get(), ModItems.VEIN_HAMMER.get(),
                    ModItems.MATTOCK.get(), ModItems.EXCAVATOR.get(), ModItems.HAND_AXE.get(),
                    ModItems.BROAD_AXE.get(), ModItems.KAMA.get(), ModItems.SCYTHE.get(),
                    ModItems.DAGGER.get(), ModItems.SWORD.get(), ModItems.CLEAVER.get()
            };

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.TINKERS_ANVIL.get(), TinkersAnvilScreen::new);
        event.register(ModMenus.PART_BUILDER.get(), PartBuilderScreen::new);
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register(ToolColorHandler.INSTANCE, TOOLS.get());
        event.register(PartColorHandler.INSTANCE,
                ModItems.PICK_HEAD.get(), ModItems.HAMMER_HEAD.get(), ModItems.SMALL_AXE_HEAD.get(),
                ModItems.BROAD_AXE_HEAD.get(), ModItems.SMALL_BLADE.get(), ModItems.LARGE_BLADE.get(),
                ModItems.TOOL_HANDLE.get(), ModItems.TOUGH_HANDLE.get(),
                ModItems.TOOL_BINDING.get(), ModItems.TOUGH_BINDING.get());
    }

    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        ToolItemRenderer renderer = new ToolItemRenderer();
        IClientItemExtensions ext = new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return renderer;
            }
        };
        event.registerItem(ext, TOOLS.get());
    }
}