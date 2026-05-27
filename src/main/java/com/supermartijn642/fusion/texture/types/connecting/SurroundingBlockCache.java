package com.supermartijn642.fusion.texture.types.connecting;

import com.supermartijn642.fusion.util.EmptyBlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Created 07/09/2024 by SuperMartijn642
 */
public class SurroundingBlockCache {

    public static final SurroundingBlockCache EMPTY = new SurroundingBlockCache(EmptyBlockAndTintGetter.INSTANCE, BlockPos.ZERO, null);

    private final BlockAndTintGetter level;
    private final BlockPos pos;
    private final BlockState[] states = new BlockState[27];

    public SurroundingBlockCache(BlockAndTintGetter level, BlockPos pos, BlockState self){
        this.level = level;
        this.pos = pos;
        this.states[13] = self;
    }

    public BlockAndTintGetter getLevel(){
        return this.level;
    }

    public BlockPos getRealPos(){
        return this.pos;
    }

    public BlockState getCenter(){
        return this.getState(0, 0, 0);
    }

    public BlockState getState(int x, int y, int z){
        int index = (x + 1) + (y + 1) * 3 + (z + 1) * 9;
        BlockState state = this.states[index];
        if(state == null)
            state = this.states[index] = this.level.getBlockState(this.pos.offset(x, y, z));
        return state;
    }
}
