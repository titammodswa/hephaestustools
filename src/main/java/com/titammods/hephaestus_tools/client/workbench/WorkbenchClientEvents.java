package com.titammods.hephaestus_tools.client.workbench;

import com.mojang.math.Axis;
import com.titammods.hephaestus_tools.HephaestusTools;
import com.titammods.hephaestus_tools.tables.scene.WorkbenchOrigin;
import com.titammods.hephaestus_tools.tables.scene.WorkbenchScene;
import com.titammods.hephaestus_tools.tables.scene.WorkbenchTuning;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

@EventBusSubscriber(modid = HephaestusTools.MOD_ID, value = Dist.CLIENT)
public final class WorkbenchClientEvents {
    private static final double HIDE_BODY_DISTANCE = 0.6;
    private static final double HEAD_HEIGHT = 1.62;

    private WorkbenchClientEvents() {}

    @SubscribeEvent
    public static void onGuiLayer(RenderGuiLayerEvent.Pre event) {
        if (!WorkbenchSession.hidesHud()) return;
        Identifier name = event.getName();
        if (!"minecraft".equals(name.getNamespace())) return;
        if (name.getPath().contains("debug")) return;
        if (name.getPath().contains("hotbar") && !WorkbenchCraft.isBusy()) return;
        event.setCanceled(true);
    }

    public static void drawVignette(GuiGraphicsExtractor g) {
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
    }

    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        if (!WorkbenchSession.isRunning()) return;
        double shot = Mth.lerp(WorkbenchSession.toolFocus(), WorkbenchTuning.CAMERA_FOV.get(), WorkbenchTuning.FOCUS_FOV.get());
        shot = Mth.lerp(WorkbenchSession.inspectMix(), shot, WorkbenchTuning.INSPECT_FOV.get());
        shot = Mth.lerp(WorkbenchCraft.mix(), shot, (WorkbenchCraft.isTinker() ? WorkbenchTuning.TINKER_FOV : WorkbenchTuning.CRAFT_FOV).get());
        shot = Mth.lerp(WorkbenchCraft.mix() * WorkbenchCraft.takeMix(), shot, WorkbenchTuning.TAKE_FOV.get());
        double fov = Mth.lerp(WorkbenchSession.blend(), event.getFOV(), shot);
        WorkbenchSession.setLastFov(fov);
        event.setFOV((float) fov);
    }

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre<?> event) {
        Minecraft mc = Minecraft.getInstance();
        AvatarRenderState state = event.getRenderState();
        if (mc.player == null || state.id != mc.player.getId() || !WorkbenchSession.isRunning()) return;

        WorkbenchOrigin origin = WorkbenchSession.origin();
        BlockState table = WorkbenchSession.tableState();
        if (origin == null || table == null || WorkbenchSession.tablePos() == null) return;

        Vec3 anchor = WorkbenchScene.playerPosition(WorkbenchSession.tablePos(), table);
        float craftMix = WorkbenchCraft.mix();
        Vec3 workSpot = WorkbenchScene.craftPlayerPosition(WorkbenchSession.tablePos(), table, WorkbenchCraft.stationMix());
        Vec3 visualFeet = WorkbenchSession.phase() == WorkbenchSession.Phase.EXITING
                ? origin.feet()
                : origin.feet().lerp(anchor, WorkbenchSession.blend()).lerp(workSpot, craftMix);
        Vec3 renderedFeet = new Vec3(state.x, state.y, state.z);

        Vec3 head = visualFeet.add(0.0, HEAD_HEIGHT, 0.0);
        Vec3 camera = mc.gameRenderer.getMainCamera().position();
        double hide = WorkbenchSession.phase() == WorkbenchSession.Phase.ACTIVE ? 0.25 : HIDE_BODY_DISTANCE;
        if (camera.distanceToSqr(head) < hide * hide) {
            event.setCanceled(true);
            return;
        }

        Vec3 shift = visualFeet.subtract(renderedFeet);
        event.getPoseStack().translate(shift.x, shift.y, shift.z);

        float actualYaw = state.bodyRot;
        float wantedYaw = Mth.rotLerp(craftMix, WorkbenchAnimation.visualBodyYaw(),
                WorkbenchScene.craftTurnYaw(WorkbenchSession.tablePos(), table, WorkbenchCraft.stationMix()));
        event.getPoseStack().mulPose(Axis.YP.rotationDegrees(actualYaw - wantedYaw));
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        WorkbenchSession.tick();
        RemoteCraft.tick();

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        BlockState state = WorkbenchSession.tableState();
        if (player == null || state == null || !WorkbenchSession.isSceneActive()) return;

        float yaw = WorkbenchScene.playerYaw(state);
        float pitch = WorkbenchScene.PLAYER_PITCH;
        player.setYRot(yaw);
        player.yRotO = yaw;
        player.setXRot(pitch);
        player.xRotO = pitch;
        player.yHeadRot = yaw;
        player.yHeadRotO = yaw;
        player.yBodyRot = yaw;
        player.yBodyRotO = yaw;
        player.setDeltaMovement(Vec3.ZERO);
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        WorkbenchSession.hardReset();
        RemoteCraft.clear();
    }

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        WorkbenchTuningCommand.register(event.getDispatcher());
    }
}
