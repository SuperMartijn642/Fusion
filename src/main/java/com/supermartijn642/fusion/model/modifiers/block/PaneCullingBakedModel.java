package com.supermartijn642.fusion.model.modifiers.block;

import com.supermartijn642.fusion.api.model.custom.quad.MutableQuad;
import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.model.WrappedBakedModel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TriState;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

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

    private final MutableQuad helperMutableQuad = MutableQuad.create();

    public PaneCullingBakedModel(BlockStateModel original){
        super(original);
    }

    @Override
    public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockModelPart> parts){
        if(state == null){
            parts.addAll(super.collectParts(level, pos, state, random));
            return;
        }
        // If state has no side properties, then there's nothing to be culled
        if(!state.hasProperty(BlockStateProperties.NORTH) && !state.hasProperty(BlockStateProperties.SOUTH) && !state.hasProperty(BlockStateProperties.WEST) && !state.hasProperty(BlockStateProperties.EAST)){
            parts.addAll(super.collectParts(level, pos, state, random));
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
            parts.addAll(super.collectParts(level, pos, state, random));
            return;
        }
        Pair<BlockState,BlockState> neighbors = Pair.of(above, below);

        // Wrap the parts with a quad filter
        for(BlockModelPart part : super.collectParts(level, pos, state, random))
            parts.add(new FilteringModelPart(part, neighbors));
    }

    private List<BakedQuad> getQuads(BlockModelPart part, Pair<BlockState,BlockState> neighbors, @Nullable Direction cullDirection){
        // Filter out certain quads
        List<BakedQuad> quads = part.getQuads(cullDirection);
        List<BakedQuad> culledQuads = new ArrayList<>(quads.size());
        for(BakedQuad quad : quads){
            if(this.filterQuad(quad, neighbors.left(), neighbors.right()))
                culledQuads.add(quad);
        }
        return culledQuads;
    }

    private boolean filterQuad(BakedQuad bakedQuad, BlockState stateAbove, BlockState stateBelow){
        // Check that the quad is part of the top or bottom face of the pane
        Direction quadDirection = bakedQuad.direction();
        if(quadDirection != Direction.UP && quadDirection != Direction.DOWN)
            return true;

        // Find the center of the quad
        MutableQuad quad = this.helperMutableQuad;
        quad.copyBakedQuad(bakedQuad);
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
        Object key = super.createGeometryKey(blockView, pos, state, random);
        return key == null ? null : Pair.of(this, key);
    }

    private class FilteringModelPart implements BlockModelPart {

        private final BlockModelPart original;
        private final Pair<BlockState,BlockState> neighbors;

        private FilteringModelPart(BlockModelPart original, Pair<BlockState,BlockState> neighbors){
            this.original = original;
            this.neighbors = neighbors;
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable Direction cullDirection){
            return PaneCullingBakedModel.this.getQuads(this.original, this.neighbors, cullDirection);
        }

        @Override
        public boolean useAmbientOcclusion(){
            return this.original.useAmbientOcclusion();
        }

        @Override
        public TextureAtlasSprite particleIcon(){
            return this.original.particleIcon();
        }

        @Override
        public ChunkSectionLayer getRenderType(BlockState state){
            return this.original.getRenderType(state);
        }

        @Override
        public TriState ambientOcclusion(){
            return this.original.ambientOcclusion();
        }
    }
}
