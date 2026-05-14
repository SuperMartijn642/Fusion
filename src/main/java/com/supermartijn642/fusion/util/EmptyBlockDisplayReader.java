package com.supermartijn642.fusion.util;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockDisplayReader;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.lighting.WorldLightManager;
import org.jetbrains.annotations.Nullable;

/**
 * Created 28/05/2026 by SuperMartijn642
 */
public class EmptyBlockDisplayReader implements IBlockDisplayReader {

    public static final IBlockDisplayReader INSTANCE = new EmptyBlockDisplayReader();

    private EmptyBlockDisplayReader(){
    }

    @Override
    public float getShade(Direction direction, boolean shade){
        return 0;
    }

    @Override
    public WorldLightManager getLightEngine(){
        throw new UnsupportedOperationException();
    }

    @Override
    public int getBlockTint(BlockPos pos, ColorResolver colorResolver){
        return 0;
    }

    @Override
    public @Nullable TileEntity getBlockEntity(BlockPos pos){
        return null;
    }

    @Override
    public BlockState getBlockState(BlockPos pos){
        return Blocks.AIR.defaultBlockState();
    }

    @Override
    public FluidState getFluidState(BlockPos pos){
        return Fluids.EMPTY.defaultFluidState();
    }
}
