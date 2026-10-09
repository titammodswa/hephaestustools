package com.titammods.hephaestus_tools.client.workbench;

import com.mojang.blaze3d.vertex.PoseStack;
import com.titammods.hephaestus_tools.HephaestusTools;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.standalone.SimpleUnbakedStandaloneModel;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import org.jspecify.annotations.Nullable;

import java.util.List;

public final class AnimHammer {
    public static final Identifier MODEL = Identifier.fromNamespaceAndPath(HephaestusTools.MOD_ID, "special/anim_hammer");
    public static final StandaloneModelKey<Baked> KEY = new StandaloneModelKey<>(MODEL::toString);
    private static final int[] NO_TINTS = new int[0];

    public record Baked(List<BakedQuad> quads, ItemTransform hand) {}

    private AnimHammer() {}

    public static void register(ModelEvent.RegisterStandalone event) {
        event.register(KEY, new SimpleUnbakedStandaloneModel<>(MODEL, (model, baker, name) -> new Baked(
                model.bakeTopGeometry(model.getTopTextureSlots(), baker, BlockModelRotation.IDENTITY).getAll(),
                model.getTopTransforms().getTransform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND))));
    }

    @Nullable
    public static Baked baked() {
        try {
            return Minecraft.getInstance().getModelManager().getStandaloneModel(KEY);
        } catch (RuntimeException e) {
            return null;
        }
    }

    public static boolean submit(PoseStack pose, SubmitNodeCollector collector, int light) {
        Baked baked = baked();
        if (baked == null || baked.quads().isEmpty()) return false;
        pose.pushPose();
        baked.hand().apply(false, pose.last());
        collector.submitItem(pose, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, light, OverlayTexture.NO_OVERLAY, 0,
                NO_TINTS, baked.quads(), ItemStackRenderState.FoilType.NONE);
        pose.popPose();
        return true;
    }
}
