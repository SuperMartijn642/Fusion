package com.supermartijn642.fusion.model.modifiers.block;

import com.google.common.collect.ImmutableMap;
import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.api.model.custom.quad.MutableQuad;
import com.supermartijn642.fusion.model.BlockRenderContext;
import com.supermartijn642.fusion.model.WrappedBakedModel;
import net.minecraft.block.BlockPane;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created 5/11/2020 by SuperMartijn642
 */
public class PaneCullingBakedModel extends WrappedBakedModel {

    private static final PropertyBool[] SIDE_PROPERTIES = {
        null,
        null,
        BlockPane.NORTH,
        BlockPane.SOUTH,
        BlockPane.WEST,
        BlockPane.EAST
    };

    private final ThreadLocal<MutableQuad> helperMutableQuad = ThreadLocal.withInitial(MutableQuad::create);

    public PaneCullingBakedModel(IBakedModel original){
        super(original);
    }

    @Override
    public @Nonnull List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing cullDirection, long seed){
        if(state == null)
            return super.getQuads(null, cullDirection, seed);
        // If state has no side properties, then there's nothing to be culled
        ImmutableMap<IProperty<?>,Comparable<?>> properties = state.getProperties();
        if(!properties.containsKey(BlockPane.NORTH) && !properties.containsKey(BlockPane.SOUTH) && !properties.containsKey(BlockPane.WEST) && !properties.containsKey(BlockPane.EAST))
            return super.getQuads(state, cullDirection, seed);

        // Gather the states above and below
        BlockRenderContext blockRenderContext = FusionClient.BLOCK_RENDER_CONTEXT.get();
        if(blockRenderContext == null || blockRenderContext.level() == null || blockRenderContext.pos() == null)
            return super.getQuads(state, cullDirection, seed);
        IBlockAccess level = blockRenderContext.level();
        BlockPos posAbove = blockRenderContext.pos().up();
        IBlockState stateAbove = level.getBlockState(posAbove).getActualState(level, posAbove);
        if(stateAbove.getBlock() != state.getBlock())
            stateAbove = null;
        BlockPos posBelow = blockRenderContext.pos().down();
        IBlockState stateBelow = level.getBlockState(posBelow).getActualState(level, posBelow);
        if(stateBelow.getBlock() != state.getBlock())
            stateBelow = null;

        if(stateAbove == null && stateBelow == null)
            return super.getQuads(state, cullDirection, seed);

        // Filter out certain quads
        List<BakedQuad> quads = super.getQuads(state, cullDirection, seed);
        List<BakedQuad> culledQuads = new ArrayList<>(quads.size());
        for(BakedQuad quad : quads){
            if(this.filterQuad(quad, stateAbove, stateBelow))
                culledQuads.add(quad);
        }
        return culledQuads;
    }

    private boolean filterQuad(BakedQuad bakedQuad, IBlockState stateAbove, IBlockState stateBelow){
        // Check that the quad is part of the top or bottom face of the pane
        EnumFacing quadDirection = bakedQuad.getFace();
        if(quadDirection != EnumFacing.UP && quadDirection != EnumFacing.DOWN)
            return true;

        // Find the center of the quad
        MutableQuad quad = this.helperMutableQuad.get();
        quad.copyBakedQuad(bakedQuad);
        float centerX = (quad.x(0) + quad.x(1) + quad.x(2) + quad.x(3)) / 4;
        float centerZ = (quad.z(0) + quad.z(1) + quad.z(2) + quad.z(3)) / 4;
        // If the quad's center is roughly at the center of the block, assume it is the middle part of the glass pane
        double quadDistance = Math.sqrt((centerX - 0.5) * (centerX - 0.5) + (centerZ - 0.5) * (centerZ - 0.5));
        if(quadDistance < 0.1) // Centerpiece
            return quadDirection == EnumFacing.UP ? stateAbove == null : stateBelow == null;

        // Get the side of the pane the quad is on
        EnumFacing partSide = EnumFacing.getFacingFromVector(centerX - 0.5f, 0, centerZ - 0.5f);
        // If the pane above/below is connected on the quad's side, cull the quad
        return quadDirection == EnumFacing.UP ?
            stateAbove == null || !stateAbove.getValue(SIDE_PROPERTIES[partSide.ordinal()]) :
            stateBelow == null || !stateBelow.getValue(SIDE_PROPERTIES[partSide.ordinal()]);
    }
}
