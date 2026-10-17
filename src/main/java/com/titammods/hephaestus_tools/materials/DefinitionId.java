package com.titammods.hephaestus_tools.materials;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.titammods.hephaestus_tools.HephaestusTools;
import net.minecraft.resources.Identifier;

import java.util.Optional;

public final class DefinitionId {

    public static final Codec<Identifier> CODEC = Codec.STRING.comapFlatMap(
            raw -> resolve(raw)
                    .map(DataResult::success)
                    .orElseGet(() -> DataResult.error(() -> "Not a valid id: " + raw)),
            Identifier::toString);

    private DefinitionId() {}

    public static Optional<Identifier> resolve(String raw) {
        if (raw == null || raw.isEmpty()) return Optional.empty();
        if (raw.indexOf(':') < 0) return Optional.ofNullable(Identifier.tryBuild(HephaestusTools.MOD_ID, raw));
        return Optional.ofNullable(Identifier.tryParse(raw));
    }
}
