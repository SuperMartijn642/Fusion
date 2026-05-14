package com.supermartijn642.fusion.util;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;
import org.jetbrains.annotations.Nullable;

/**
 * Created 28/05/2026 by SuperMartijn642
 */
public class EmptyBlockDisplayReader implements IBlockAccess {

    public static final IBlockAccess INSTANCE = new EmptyBlockDisplayReader();

    private EmptyBlockDisplayReader(){
    }

    @Override
    public Biome getBiome(BlockPos pos){
        return Biomes.DEFAULT;
    }

    @Override
    public @Nullable TileEntity getTileEntity(BlockPos pos){
        return null;
    }

    @Override
    public IBlockState getBlockState(BlockPos pos){
        return Blocks.AIR.getDefaultState();
    }

    @Override
    public boolean isAirBlock(BlockPos pos){
        return true;
    }

    @Override
    public int getCombinedLight(BlockPos pos, int lightValue){
        return 0;
    }

    @Override
    public int getStrongPower(BlockPos pos, EnumFacing direction){
        return 0;
    }

    @Override
    public WorldType getWorldType(){
        return WorldType.DEFAULT;
    }

    @Override
    public boolean isSideSolid(BlockPos pos, EnumFacing side, boolean _default){
        return false;
    }
}
