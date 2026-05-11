package com.supermartijn642.fusion.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

/**
 * Created 28/05/2026 by SuperMartijn642
 */
public class EmptyBlockAndTintGetter implements BlockAndTintGetter {

    public static final BlockAndTintGetter INSTANCE = new EmptyBlockAndTintGetter();

    private EmptyBlockAndTintGetter(){
    }

    @Override
    public float getShade(Direction direction, boolean shade){
        return 0;
    }

    @Override
    public LevelLightEngine getLightEngine(){
        throw new UnsupportedOperationException();
    }

    @Override
    public int getBlockTint(BlockPos pos, ColorResolver colorResolver){
        return 0;
    }

    @Override
    public @Nullable BlockEntity getBlockEntity(BlockPos pos){
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

    @Override
    public int getHeight(){
        return 0;
    }

    @Override
    public int getMinBuildHeight(){
        return 0;
    }
}
