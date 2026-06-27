package com.titammods.hephaestus_tools.materials;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public record MaterialId(ResourceLocation id) {

    public static final MaterialId EMPTY = new MaterialId(ResourceLocation.withDefaultNamespace("empty"));

    public static final Codec<MaterialId> CODEC = ResourceLocation.CODEC.xmap(MaterialId::new, MaterialId::id);

    public static final StreamCodec<RegistryFriendlyByteBuf, MaterialId> STREAM_CODEC =
            StreamCodec.of(
                    (buf, mat) -> buf.writeResourceLocation(mat.id()),
                    buf -> new MaterialId(buf.readResourceLocation())
            );

    public static MaterialId of(String namespace, String path) {
        return new MaterialId(ResourceLocation.fromNamespaceAndPath(namespace, path));
    }

    public static MaterialId of(String id) {
        return new MaterialId(ResourceLocation.parse(id));
    }

    public boolean isEmpty() {
        return this.equals(EMPTY);
    }

    @Override
    public String toString() {
        return id.toString();
    }
}