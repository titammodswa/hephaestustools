package com.titammods.hephaestus_tools.tables.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ScorchedAnvilBlock extends TinkersAnvilBlock {

    private static final MapCodec<ScorchedAnvilBlock> CODEC = simpleCodec(ScorchedAnvilBlock::new);

    private static final VoxelShape PART_BASE = Block.box(2.0, 0.0, 2.0, 14.0, 4.0, 14.0);
    private static final VoxelShape X_AXIS_AABB = Shapes.or(
            PART_BASE,
            Block.box(4.0, 4.0, 5.0, 12.0, 7.0, 11.0),
            Block.box(5.0, 4.0, 2.5, 11.0, 10.0, 6.5),
            Block.box(5.0, 4.0, 9.5, 11.0, 10.0, 13.5),
            Block.box(3.0, 10.0, 0.0, 13.0, 16.0, 16.0)
    );
    private static final VoxelShape Z_AXIS_AABB = Shapes.or(
            PART_BASE,
            Block.box(5.0, 4.0, 4.0, 11.0, 7.0, 12.0),
            Block.box(2.5, 4.0, 5.0, 6.5, 10.0, 11.0),
            Block.box(9.5, 4.0, 5.0, 13.5, 10.0, 11.0),
            Block.box(0.0, 10.0, 3.0, 16.0, 16.0, 13.0)
    );

    public ScorchedAnvilBlock(BlockBehaviour.Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext ctx) {
        Direction dir = state.getValue(FACING);
        return dir.getAxis() == Direction.Axis.X ? X_AXIS_AABB : Z_AXIS_AABB;
    }
}