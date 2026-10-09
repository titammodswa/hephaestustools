package com.titammods.hephaestus_tools.client.workbench;

import com.titammods.hephaestus_tools.HephaestusTools;
import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranim.api.PlayerAnimationAccess;
import com.zigythebird.playeranim.api.PlayerAnimationFactory;
import com.zigythebird.playeranimcore.animation.AnimationController;
import com.zigythebird.playeranimcore.animation.AnimationData;
import com.zigythebird.playeranimcore.animation.keyframe.event.data.CustomInstructionKeyframeData;
import com.zigythebird.playeranimcore.animation.layered.modifier.AbstractFadeModifier;
import com.zigythebird.playeranimcore.easing.EasingType;
import com.zigythebird.playeranimcore.enums.PlayState;
import com.zigythebird.playeranimcore.event.EventResult;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Avatar;
import net.minecraft.resources.Identifier;

import org.jetbrains.annotations.Nullable;

public final class WorkbenchPlayerAnim {
    public static final Identifier LAYER = id("workbench");
    public static final Identifier BUILD = id("craft_build");
    public static final Identifier MODIFY = id("craft_modify");

    private WorkbenchPlayerAnim() {}

    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath(HephaestusTools.MOD_ID, path); }

    public static void register() {
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(LAYER, 1600,
                player -> new PlayerAnimationController(player, (controller, state, setter) -> PlayState.STOP)
                        .setCustomInstructionKeyframeHandler(WorkbenchPlayerAnim::instruction));
    }

    private static EventResult instruction(float tick, AnimationController controller, CustomInstructionKeyframeData data, AnimationData state) {
        if (controller instanceof PlayerAnimationController pc && data.getInstructions().contains("strike")) {
            if (pc.getAvatar() == Minecraft.getInstance().player) WorkbenchCraft.animStrike();
            else RemoteCraft.animStrike(pc.getAvatar().getId());
        }
        return EventResult.PASS;
    }

    @Nullable
    private static PlayerAnimationController controller(@Nullable Avatar player) {
        if (player == null) return null;
        try {
            return PlayerAnimationAccess.getPlayerAnimationLayer(player, LAYER) instanceof PlayerAnimationController c ? c : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static boolean play(@Nullable Avatar player, boolean modify) {
        PlayerAnimationController c = controller(player);
        if (c == null) return false;
        c.removeAllModifiers();
        if (!c.replaceAnimationWithFade(AbstractFadeModifier.standardFadeIn(6, EasingType.EASE_IN_OUT_SINE), modify ? MODIFY : BUILD)) return false;
        c.addModifierLast(AbstractFadeModifier.standardFadeOut(10, EasingType.EASE_IN_OUT_SINE));
        return true;
    }

    public static boolean isPlaying(@Nullable Avatar player) {
        PlayerAnimationController c = controller(player);
        return c != null && c.isActive();
    }

    public static void stop(@Nullable Avatar player) {
        PlayerAnimationController c = controller(player);
        if (c != null) c.stop();
    }
}
