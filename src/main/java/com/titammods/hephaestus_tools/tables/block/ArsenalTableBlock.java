package com.titammods.hephaestus_tools.tables.block;

import com.mojang.serialization.MapCodec;
import com.titammods.hephaestus_tools.registry.ModSounds;
import com.titammods.hephaestus_tools.table.TableStyle;
import com.titammods.hephaestus_tools.tables.blockentity.ArsenalTableBlockEntity;
import com.titammods.hephaestus_tools.tables.scene.WorkbenchOrigin;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

public class ArsenalTableBlock extends BaseEntityBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<TablePart> PART = EnumProperty.create("part", TablePart.class);
    private static final MapCodec<ArsenalTableBlock> CODEC = simpleCodec(ArsenalTableBlock::new);
    private static final double[][] MAIN_BOXES = {
            {0, 0, 1, 3, 13, 4}, {0, 0, 12, 3, 13, 15}, {12, 0, 1, 15, 13, 4}, {12, 0, 12, 15, 13, 15},
            {1, 2, 2, 14, 13, 14}, {0, 13, 0, 16, 16, 16},
            {5, 16, 4, 11, 18, 12}, {6, 18, 5, 10, 20, 11}, {5, 20, 3, 11, 24, 13}};
    private static final double[][] SECOND_BOXES = {
            {1, 0, 1, 4, 13, 4}, {1, 0, 12, 4, 13, 15},
            {2, 2, 2, 16, 4, 14}, {3, 4, 13, 16, 13, 14}, {1, 13, 0, 16, 16, 16}};
    private static final Map<Direction, VoxelShape> MAIN_SHAPES = shapes(MAIN_BOXES), SECOND_SHAPES = shapes(SECOND_BOXES);

    private static Map<Direction, VoxelShape> shapes(double[][] boxes) {
        Map<Direction, VoxelShape> out = new EnumMap<>(Direction.class);
        Direction[] facings = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
        for (int turns = 0; turns < 4; turns++) {
            VoxelShape shape = Shapes.empty();
            for (double[] b : boxes) {
                double x1 = b[0], z1 = b[2], x2 = b[3], z2 = b[5];
                for (int t = 0; t < turns; t++) {
                    double nx1 = 16 - z2, nx2 = 16 - z1, nz1 = x1, nz2 = x2;
                    x1 = nx1; x2 = nx2; z1 = nz1; z2 = nz2;
                }
                shape = Shapes.or(shape, Block.box(x1, b[1], z1, x2, b[4], z2));
            }
            out.put(facings[turns], shape.optimize());
        }
        return out;
    }

    public ArsenalTableBlock(Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH).setValue(PART, TablePart.MAIN));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return state.getValue(PART) == TablePart.MAIN ? RenderShape.MODEL : RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return (s.getValue(PART) == TablePart.MAIN ? MAIN_SHAPES : SECOND_SHAPES).get(s.getValue(FACING));
    }

    @Override
    protected VoxelShape getVisualShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return Shapes.empty();
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) { return true; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, PART);
    }

    private static Direction secondDir(Direction facing) { return facing.getCounterClockWise(); }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction facing = ctx.getHorizontalDirection().getOpposite();
        BlockPos second = ctx.getClickedPos().relative(secondDir(facing));
        Level level = ctx.getLevel();
        if (level.getBlockState(second).canBeReplaced(ctx) && level.getWorldBorder().isWithinBounds(second))
            return this.defaultBlockState().setValue(FACING, facing).setValue(PART, TablePart.MAIN);
        return null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (!level.isClientSide()) {
            Direction facing = state.getValue(FACING);
            BlockPos second = pos.relative(secondDir(facing));
            level.setBlock(second, state.setValue(PART, TablePart.SECOND), 3);
            state.updateNeighbourShapes(level, pos, 3);
        }
    }

    private static BlockPos mainPos(BlockState state, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        return state.getValue(PART) == TablePart.MAIN ? pos : pos.relative(secondDir(facing).getOpposite());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == TablePart.MAIN ? new ArsenalTableBlockEntity(pos, state) : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            BlockPos mp = mainPos(state, pos);
            if (level.getBlockEntity(mp) instanceof ArsenalTableBlockEntity arsenal) {
                final BlockPos openPos = mp;
                final boolean modern = TableStyle.isModern(player);
                final WorkbenchOrigin origin = WorkbenchOrigin.of(player);
                player.openMenu(arsenal, buf -> {
                    buf.writeBlockPos(openPos);
                    if (modern) origin.write(buf);
                });
                level.playSound(null, pos, ModSounds.ARSENAL_TABLE_OPEN.get(), SoundSource.BLOCKS, 0.7F, 1.0F);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        TablePart part = state.getValue(PART);
        Direction facing = state.getValue(FACING);
        BlockPos otherPos = part == TablePart.MAIN
                ? pos.relative(secondDir(facing))
                : pos.relative(secondDir(facing).getOpposite());
        BlockState other = level.getBlockState(otherPos);
        if (other.is(this) && other.getValue(PART) != part) {
            level.removeBlock(otherPos, false);
        }
    }
}
