package com.supermartijn642.fusion.texture.types.connecting;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;

/**
 * Created 07/09/2024 by SuperMartijn642
 */
public class SurroundingBlockCache {

    public static final SurroundingBlockCache EMPTY = new SurroundingBlockCache(BlockAndTintGetter.EMPTY, BlockPos.ZERO, null);

    private BlockAndTintGetter level;
    private BlockPos pos;
    private final BlockState[] states = new BlockState[27];
    private final BlockPos.MutableBlockPos lookupPos = new BlockPos.MutableBlockPos();
    private final BlockPos.MutableBlockPos neighborPos = new BlockPos.MutableBlockPos();

    public SurroundingBlockCache(BlockAndTintGetter level, BlockPos pos, BlockState self){
        this.reset(level, pos, self);
    }

    SurroundingBlockCache(){
    }

    /**
     * Reinitializes this cache for a new block, so a single instance can be reused during chunk meshing.
     */
    SurroundingBlockCache reset(BlockAndTintGetter level, BlockPos pos, BlockState self){
        this.level = level;
        this.pos = pos;
        Arrays.fill(this.states, null);
        this.states[13] = self;
        return this;
    }

    public BlockAndTintGetter getLevel(){
        return this.level;
    }

    public BlockPos getRealPos(){
        return this.pos;
    }

    /**
     * Scratch position for callers that need a neighbor position while using this cache.
     */
    BlockPos.MutableBlockPos neighborPos(){
        return this.neighborPos;
    }

    public BlockState getCenter(){
        return this.getState(0, 0, 0);
    }

    public BlockState getState(int x, int y, int z){
        int index = (x + 1) + (y + 1) * 3 + (z + 1) * 9;
        BlockState state = this.states[index];
        if(state == null)
            state = this.states[index] = this.level.getBlockState(this.lookupPos.setWithOffset(this.pos, x, y, z));
        return state;
    }
}
