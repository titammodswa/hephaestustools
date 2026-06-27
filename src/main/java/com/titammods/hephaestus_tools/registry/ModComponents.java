package com.titammods.hephaestus_tools.registry;

import com.mojang.serialization.Codec;
import com.titammods.hephaestus_tools.HephaestusTools;
import com.titammods.hephaestus_tools.materials.MaterialId;
import com.titammods.hephaestus_tools.tools.nbt.ToolConstructionData;
import com.titammods.hephaestus_tools.tools.nbt.ToolPropertiesData;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModComponents {

    public static final DeferredRegister.DataComponents REGISTRAR =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, HephaestusTools.MOD_ID);

    public static final Supplier<DataComponentType<ToolConstructionData>> TOOL_CONSTRUCTION =
            REGISTRAR.registerComponentType(
                    "construction",
                    builder -> builder
                            .persistent(ToolConstructionData.CODEC)
                            .networkSynchronized(ToolConstructionData.STREAM_CODEC)
            );

    public static final Supplier<DataComponentType<ToolPropertiesData>> TOOL_PROPERTIES =
            REGISTRAR.registerComponentType(
                    "properties",
                    builder -> builder
                            .persistent(ToolPropertiesData.CODEC)
                            .networkSynchronized(ToolPropertiesData.STREAM_CODEC)
            );

    public static final Supplier<DataComponentType<Boolean>> TOOL_BROKEN =
            REGISTRAR.registerComponentType(
                    "broken",
                    builder -> builder
                            .persistent(Codec.BOOL)
                            .networkSynchronized(ByteBufCodecs.BOOL)
            );

    public static final Supplier<DataComponentType<MaterialId>> PART_MATERIAL =
            REGISTRAR.registerComponentType(
                    "part_material",
                    builder -> builder
                            .persistent(MaterialId.CODEC)
                            .networkSynchronized(MaterialId.STREAM_CODEC)
            );
}
