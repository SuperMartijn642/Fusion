package com.supermartijn642.fusion.model.modifiers.block;

import com.supermartijn642.fusion.api.model.custom.quad.MutableQuad;
import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.model.WrappedBakedModel;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ILightReader;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.data.ModelDataMap;
import net.minecraftforge.client.model.data.ModelProperty;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created 5/11/2020 by SuperMartijn642
 */
public class PaneCullingBakedModel extends WrappedBakedModel {

    private static final ModelProperty<Pair<BlockState,BlockState>> NEIGHBOR_PROPERTY = new ModelProperty<>();
    private static final BooleanProperty[] SIDE_PROPERTIES = {
        null,
        null,
        BlockStateProperties.NORTH,
        BlockStateProperties.SOUTH,
        BlockStateProperties.WEST,
        BlockStateProperties.EAST
    };

    private final MutableQuad helperMutableQuad = MutableQuad.create();

    public PaneCullingBakedModel(IBakedModel original){
        super(original);
    }

    @Override
    public @Nonnull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction cullDirection, @Nonnull Random random, @Nonnull IModelData data){
        if(state == null)
            return super.getQuads(null, cullDirection, random, data);
        // If state has no side properties, then there's nothing to be culled
        if(!state.hasProperty(BlockStateProperties.NORTH) && !state.hasProperty(BlockStateProperties.SOUTH) && !state.hasProperty(BlockStateProperties.WEST) && !state.hasProperty(BlockStateProperties.EAST))
            return super.getQuads(state, cullDirection, random, data);

        // Gather the states above and below
        Pair<BlockState,BlockState> neighbors = data.getData(NEIGHBOR_PROPERTY);
        if(neighbors == null)
            return super.getQuads(state, cullDirection, random, data);
        BlockState stateAbove = neighbors.left();
        if(stateAbove.getBlock() != state.getBlock())
            stateAbove = null;
        BlockState stateBelow = neighbors.right();
        if(stateBelow.getBlock() != state.getBlock())
            stateBelow = null;

        if(stateAbove == null && stateBelow == null)
            return super.getQuads(state, cullDirection, random, data);

        // Filter out certain quads
        List<BakedQuad> quads = super.getQuads(state, cullDirection, random, data);
        List<BakedQuad> culledQuads = new ArrayList<>(quads.size());
        for(BakedQuad quad : quads){
            if(this.filterQuad(quad, stateAbove, stateBelow))
                culledQuads.add(quad);
        }
        return culledQuads;
    }

    @Override
    public IModelData getModelData(ILightReader level, BlockPos pos, BlockState state, IModelData data){
        data = super.getModelData(level, pos, state, data);
        if(!data.getClass().equals(ModelDataMap.class)){
            return new ModelDataMap.Builder()
                .withInitial(NEIGHBOR_PROPERTY, Pair.of(level.getBlockState(pos.above()), level.getBlockState(pos.below())))
                .build();
        }
        data.setData(NEIGHBOR_PROPERTY, Pair.of(level.getBlockState(pos.above()), level.getBlockState(pos.below())));
        return data;
    }

    private boolean filterQuad(BakedQuad bakedQuad, BlockState stateAbove, BlockState stateBelow){
        // Check that the quad is part of the top or bottom face of the pane
        Direction quadDirection = bakedQuad.getDirection();
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
        Direction partSide = Direction.getNearest(centerX - 0.5, 0, centerZ - 0.5);
        // If the pane above/below is connected on the quad's side, cull the quad
        return quadDirection == Direction.UP ?
            stateAbove == null || !stateAbove.getValue(SIDE_PROPERTIES[partSide.ordinal()]) :
            stateBelow == null || !stateBelow.getValue(SIDE_PROPERTIES[partSide.ordinal()]);
    }
}
