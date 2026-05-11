package com.supermartijn642.fusion.model.modifiers.block;

import com.supermartijn642.fusion.api.model.custom.quad.MutableQuad;
import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.model.WrappedBakedModel;
import com.supermartijn642.fusion.model.custom.quad.MutableQuadImpl;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadView;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/**
 * Created 5/11/2020 by SuperMartijn642
 */
public class PaneCullingBakedModel extends WrappedBakedModel {

    private static final BooleanProperty[] SIDE_PROPERTIES = {
        null,
        null,
        BlockStateProperties.NORTH,
        BlockStateProperties.SOUTH,
        BlockStateProperties.WEST,
        BlockStateProperties.EAST
    };

    private final MutableQuad helperMutableQuad = new MutableQuadImpl();

    public PaneCullingBakedModel(BlockStateModel original){
        super(original);
    }

    @Override
    public void emitQuads(QuadEmitter emitter, BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, Predicate<@Nullable Direction> cullTest){
        // If state has no side properties, then there's nothing to be culled
        if(!state.hasProperty(BlockStateProperties.NORTH) && !state.hasProperty(BlockStateProperties.SOUTH) && !state.hasProperty(BlockStateProperties.WEST) && !state.hasProperty(BlockStateProperties.EAST)){
            super.emitQuads(emitter, level, pos, state, random, cullTest);
            return;
        }

        // Gather the states above and below
        BlockState above = level.getBlockState(pos.above()).getAppearance(level, pos.above(), Direction.DOWN, state, pos);
        if(above.getBlock() != state.getBlock())
            above = null;
        BlockState below = level.getBlockState(pos.below()).getAppearance(level, pos.below(), Direction.UP, state, pos);
        if(below.getBlock() != state.getBlock())
            below = null;
        if(above == null && below == null){
            super.emitQuads(emitter, level, pos, state, random, cullTest);
            return;
        }

        // Filter out certain quads
        BlockState finalStateAbove = above;
        BlockState finalStateBelow = below;
        emitter.pushTransform(quad -> filterQuad(quad, finalStateAbove, finalStateBelow));
        super.emitQuads(emitter, level, pos, state, random, cullTest);
        emitter.popTransform();
    }

    private boolean filterQuad(QuadView quadView, BlockState stateAbove, BlockState stateBelow){
        // Check that the quad is part of the top or bottom face of the pane
        Direction quadDirection = quadView.nominalFace();
        if(quadDirection != Direction.UP && quadDirection != Direction.DOWN)
            return true;

        // Find the center of the quad
        MutableQuad quad = this.helperMutableQuad;
        quad.copyFrapiQuad(quadView);
        float centerX = (quad.x(0) + quad.x(1) + quad.x(2) + quad.x(3)) / 4;
        float centerZ = (quad.z(0) + quad.z(1) + quad.z(2) + quad.z(3)) / 4;
        // If the quad's center is roughly at the center of the block, assume it is the middle part of the glass pane
        double quadDistance = Math.sqrt((centerX - 0.5) * (centerX - 0.5) + (centerZ - 0.5) * (centerZ - 0.5));
        if(quadDistance < 0.1) // Centerpiece
            return quadDirection == Direction.UP ? stateAbove == null : stateBelow == null;

        // Get the side of the pane the quad is on
        Direction partSide = Direction.getApproximateNearest(centerX - 0.5, 0, centerZ - 0.5);
        // If the pane above/below is connected on the quad's side, cull the quad
        return quadDirection == Direction.UP ?
            stateAbove == null || !stateAbove.getValue(SIDE_PROPERTIES[partSide.ordinal()]) :
            stateBelow == null || !stateBelow.getValue(SIDE_PROPERTIES[partSide.ordinal()]);
    }

    @Override
    public @Nullable Object createGeometryKey(BlockAndTintGetter blockView, BlockPos pos, BlockState state, RandomSource random){
        return Pair.of(
            this,
            super.createGeometryKey(blockView, pos, state, random)
        );
    }
}
