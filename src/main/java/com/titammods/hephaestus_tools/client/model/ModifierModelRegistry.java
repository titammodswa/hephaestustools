package com.titammods.hephaestus_tools.client.model;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public final class ModifierModelRegistry {

    private ModifierModelRegistry() {}

    private static final Map<ResourceLocation, ResourceLocation> TEXTURE_OVERRIDES = new HashMap<>();

    @Nullable
    public static ResourceLocation getTexture(Item toolItem, ResourceLocation modifierId) {
        ResourceLocation override = TEXTURE_OVERRIDES.get(modifierId);
        if (override != null) {
            return override;
        }

        ResourceLocation toolId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(toolItem);
        if (toolId == null) {
            return null;
        }

        String toolPath = toolId.getPath();
        String modifierSuffix = modifierId.getNamespace() + "_" + modifierId.getPath();

        return ResourceLocation.fromNamespaceAndPath(
                "hephaestus_tools",
                "item/tool/" + toolPath + "/modifiers/" + modifierSuffix
        );
    }

    public static void registerOverride(ResourceLocation modifierId, ResourceLocation texture) {
        TEXTURE_OVERRIDES.put(modifierId, texture);
    }
}