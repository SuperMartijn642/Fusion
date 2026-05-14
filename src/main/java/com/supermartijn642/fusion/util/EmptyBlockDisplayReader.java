package com.supermartijn642.fusion.util;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.fluid.Fluids;
import net.minecraft.fluid.IFluidState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IEnviromentBlockReader;
import net.minecraft.world.LightType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biomes;
import org.jetbrains.annotations.Nullable;

/**
 * Created 28/05/2026 by SuperMartijn642
 */
public class EmptyBlockDisplayReader implements IEnviromentBlockReader {

    public static final IEnviromentBlockReader INSTANCE = new EmptyBlockDisplayReader();

    private EmptyBlockDisplayReader(){
    }

    @Override
    public Biome getBiome(BlockPos pos){
        return Biomes.DEFAULT;
    }

    @Override
    public int getBrightness(LightType type, BlockPos pos){
        return 1;
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
    public IFluidState getFluidState(BlockPos pos){
        return Fluids.EMPTY.defaultFluidState();
    }
}
