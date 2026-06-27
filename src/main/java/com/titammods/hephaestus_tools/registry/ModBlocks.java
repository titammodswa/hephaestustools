package com.titammods.hephaestus_tools.registry;

import com.titammods.hephaestus_tools.HephaestusTools;
import com.titammods.hephaestus_tools.tables.block.PartBuilderBlock;
import com.titammods.hephaestus_tools.tables.block.ScorchedAnvilBlock;
import com.titammods.hephaestus_tools.tables.block.TinkersAnvilBlock;
import com.titammods.hephaestus_tools.tables.blockentity.PartBuilderBlockEntity;
import com.titammods.hephaestus_tools.tables.blockentity.TinkersAnvilBlockEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(HephaestusTools.MOD_ID);

    public static final DeferredRegister<net.minecraft.world.level.block.entity.BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(net.minecraft.core.registries.Registries.BLOCK_ENTITY_TYPE, HephaestusTools.MOD_ID);

    private static BlockBehaviour.Properties anvilProps() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_GRAY)
                .sound(SoundType.ANVIL)
                .pushReaction(PushReaction.BLOCK)
                .requiresCorrectToolForDrops()
                .strength(5.0f, 1200.0f)
                .noOcclusion();
    }

    private static BlockBehaviour.Properties woodTableProps() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .sound(SoundType.WOOD)
                .strength(1.0f, 5.0f)
                .noOcclusion();
    }

    public static final DeferredBlock<TinkersAnvilBlock> TINKERS_ANVIL =
            BLOCKS.register("tinkers_anvil", () -> new TinkersAnvilBlock(anvilProps()));

    public static final DeferredBlock<ScorchedAnvilBlock> SCORCHED_ANVIL =
            BLOCKS.register("scorched_anvil", () -> new ScorchedAnvilBlock(anvilProps()));

    public static final DeferredBlock<PartBuilderBlock> PART_BUILDER =
            BLOCKS.register("part_builder", () -> new PartBuilderBlock(woodTableProps()));

    static {
        registerBlockItem(TINKERS_ANVIL);
        registerBlockItem(SCORCHED_ANVIL);
        registerBlockItem(PART_BUILDER);
    }

    private static <T extends Block> void registerBlockItem(DeferredBlock<T> block) {
        ModItems.ITEMS.register(block.getId().getPath(),
                () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static final Supplier<net.minecraft.world.level.block.entity.BlockEntityType<TinkersAnvilBlockEntity>> TINKERS_ANVIL_BE =
            BLOCK_ENTITIES.register("tinkers_anvil",
                    () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder
                            .of(TinkersAnvilBlockEntity::new, TINKERS_ANVIL.get(), SCORCHED_ANVIL.get())
                            .build(null));

    public static final Supplier<net.minecraft.world.level.block.entity.BlockEntityType<PartBuilderBlockEntity>> PART_BUILDER_BE =
            BLOCK_ENTITIES.register("part_builder",
                    () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder
                            .of(PartBuilderBlockEntity::new, PART_BUILDER.get())
                            .build(null));
}
