package com.titammods.hephaestus_tools.client.model;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
import com.mojang.math.Transformation;
import com.titammods.hephaestus_tools.tools.nbt.ToolConstructionData;
import com.titammods.hephaestus_tools.tools.nbt.ToolStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.SimpleModelState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.geometry.UnbakedGeometryHelper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class ToolBakedModel extends BakedModelWrapper<BakedModel> {

    private final Item toolItem;
    private final MaterialOverrideHandler overrides;

    public ToolBakedModel(BakedModel originalModel, Item toolItem) {
        super(originalModel);
        this.toolItem = toolItem;
        this.overrides = new MaterialOverrideHandler(originalModel, toolItem);
    }

    @Override
    public ItemOverrides getOverrides() {
        return overrides;
    }

    private static final class MaterialOverrideHandler extends ItemOverrides {
        private final BakedModel baseModel;
        private final Item toolItem;

        private final Cache<List<ResourceLocation>, BakedModel> cache =
                CacheBuilder.newBuilder().maximumSize(256).build();

        private MaterialOverrideHandler(BakedModel baseModel, Item toolItem) {
            this.baseModel = baseModel;
            this.toolItem = toolItem;
        }

        @Override
        public BakedModel resolve(BakedModel model, ItemStack stack,
                                  @Nullable ClientLevel level,
                                  @Nullable LivingEntity entity, int seed) {
            if (!ToolStack.isInitialized(stack)) {
                return baseModel;
            }

            List<ToolConstructionData.ModifierEntry> modifiers = ToolStack.getModifiers(stack);
            if (modifiers.isEmpty()) {
                return baseModel;
            }

            ImmutableList.Builder<ResourceLocation> keyBuilder = ImmutableList.builder();
            for (ToolConstructionData.ModifierEntry mod : modifiers) {
                ResourceLocation tex = ModifierModelRegistry.getTexture(toolItem, mod.id());
                if (tex != null) {
                    keyBuilder.add(mod.id());
                }
            }
            List<ResourceLocation> key = keyBuilder.build();

            if (key.isEmpty()) {
                return baseModel;
            }

            try {
                return cache.get(key, () -> new OverlaidModel(baseModel, toolItem, modifiers));
            } catch (ExecutionException e) {
                return baseModel;
            }
        }
    }

    private static final class OverlaidModel extends BakedModelWrapper<BakedModel> {
        private final List<BakedQuad> overlayQuads;

        private OverlaidModel(BakedModel base, Item toolItem,
                              List<ToolConstructionData.ModifierEntry> modifiers) {
            super(base);
            this.overlayQuads = bakeOverlays(toolItem, modifiers);
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                                        RandomSource rand) {
            return getQuads(state, side, rand, ModelData.EMPTY, null);
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                                        RandomSource rand, ModelData extraData,
                                        @Nullable RenderType renderType) {
            List<BakedQuad> baseQuads = originalModel.getQuads(state, side, rand, extraData, renderType);
            List<BakedQuad> quads = new ArrayList<>(baseQuads);
            if (side == null) {
                quads.addAll(overlayQuads);
            }
            if (!overlayQuads.isEmpty() && !LOGGED_RENDER) {
                LOGGED_RENDER = true;
                com.mojang.logging.LogUtils.getLogger().info(
                        "[HephTools/render] getQuads chamado: side={} renderType={} base={} overlay={} total={}",
                        side, renderType, baseQuads.size(), overlayQuads.size(), quads.size());
            }
            return quads;
        }

        private static boolean LOGGED_RENDER = false;

        private static List<BakedQuad> bakeOverlays(Item toolItem,
                                                    List<ToolConstructionData.ModifierEntry> modifiers) {
            List<BakedQuad> result = new ArrayList<>();
            var atlas = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
            for (ToolConstructionData.ModifierEntry mod : modifiers) {
                ResourceLocation texture = ModifierModelRegistry.getTexture(toolItem, mod.id());
                if (texture == null) continue;

                TextureAtlasSprite sprite = atlas.apply(texture);
                if (sprite.contents().name().equals(MissingTextureAtlasSprite.getLocation())) {
                    continue;
                }

                var unbaked = UnbakedGeometryHelper.createUnbakedItemElements(-1, sprite, null);
                result.addAll(UnbakedGeometryHelper.bakeElements(unbaked, m -> sprite,
                        new SimpleModelState(Transformation.identity())));
            }
            return result;
        }
    }
}