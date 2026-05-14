package com.supermartijn642.fusion.texture.types.connecting;

import com.supermartijn642.fusion.util.EmptyBlockDisplayReader;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IEnviromentBlockReader;

/**
 * Created 07/09/2024 by SuperMartijn642
 */
public class SurroundingBlockCache {

    public static final SurroundingBlockCache EMPTY = new SurroundingBlockCache(EmptyBlockDisplayReader.INSTANCE, BlockPos.ZERO, null);

    private final IEnviromentBlockReader level;
    private final BlockPos pos;
    private final BlockState[] states = new BlockState[27];

    public SurroundingBlockCache(IEnviromentBlockReader level, BlockPos pos, BlockState self){
        this.level = level;
        this.pos = pos;
        this.states[13] = self;
    }

    public IEnviromentBlockReader getLevel(){
        return this.level;
    }

    public BlockPos getRealPos(){
        return this.pos;
    }

    public BlockState getCenter(){
        return this.states[13];
    }

    public BlockState getState(int x, int y, int z){
        int index = (x + 1) + (y + 1) * 3 + (z + 1) * 9;
        BlockState state = this.states[index];
        if(state == null)
            state = this.states[index] = this.level.getBlockState(this.pos.offset(x, y, z));
        return state;
    }
}
