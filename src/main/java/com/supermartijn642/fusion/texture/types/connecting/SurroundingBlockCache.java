package com.supermartijn642.fusion.texture.types.connecting;

import com.supermartijn642.fusion.util.EmptyBlockDisplayReader;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * Created 07/09/2024 by SuperMartijn642
 */
public class SurroundingBlockCache {

    public static final SurroundingBlockCache EMPTY = new SurroundingBlockCache(EmptyBlockDisplayReader.INSTANCE, BlockPos.ORIGIN, null);

    private final IBlockAccess level;
    private final BlockPos pos;
    private final IBlockState[] states = new IBlockState[27];

    public SurroundingBlockCache(IBlockAccess level, BlockPos pos, IBlockState self){
        this.level = level;
        this.pos = pos;
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
        if(state == null){
            BlockPos pos = this.pos.add(x, y, z);
            state = this.states[index] = this.level.getBlockState(pos).getActualState(this.level, pos);
        }
        return state;
    }
}
