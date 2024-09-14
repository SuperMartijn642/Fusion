package com.supermartijn642.fusion.model.types.connecting;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * Created 07/09/2024 by SuperMartijn642
 */
public class SurroundingBlockCache {

    private final IBlockAccess level;
    private final BlockPos pos;
    private final IBlockState[] states = new IBlockState[27];

    public SurroundingBlockCache(IBlockAccess level, BlockPos pos, IBlockState self){
        this.level = level;
        this.pos = pos;
        this.states[13] = self;
    }

    public void fillAll(){
        for(int index = 0; index < 27; index++){
            if(this.states[index] != null)
                continue;
            int x = (index % 3) - 1;
            int y = (index % 9) / 3 - 1;
            int z = index / 9 - 1;
            this.states[index] = this.level.getBlockState(this.pos.add(x, y, z));
        }
    }

    public void setSelf(IBlockState self){
        this.states[13] = self;
    }

    public IBlockAccess getLevel(){
        return this.level;
    }

    public BlockPos getRealPos(){
        return this.pos;
    }

    public IBlockState getCenter(){
        return this.states[13];
    }

    public IBlockState getState(int x, int y, int z){
        int index = (x + 1) + (y + 1) * 3 + (z + 1) * 9;
        IBlockState state = this.states[index];
        if(state == null)
            state = this.states[index] = this.level.getBlockState(this.pos.add(x, y, z));
        return state;
    }
}
