package com.supermartijn642.fusion.model;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

/**
 * Created 05/06/2026 by SuperMartijn642
 */
public class BlockRenderContext {

    private final IBlockAccess level;
    private final BlockPos pos;
    private final IBlockState state;

    public BlockRenderContext(IBlockAccess level, BlockPos pos, IBlockState state){
        this.level = level;
        this.pos = pos;
        this.state = state;
    }

    @Nullable
    @Contract(pure = true)
    public IBlockAccess level(){
        return this.level;
    }

    @Nullable
    @Contract(pure = true)
    public BlockPos pos(){
        return this.pos;
    }

    @Nullable
    @Contract(pure = true)
    public IBlockState state(){
        return this.state;
    }
}
