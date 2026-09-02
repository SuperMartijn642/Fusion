package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.util.BiomeGetter;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.biome.Biome;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Created 03/09/2026 by SuperMartijn642
 */
@Mixin(IWorldReader.class)
public interface IWorldReaderMixin extends BiomeGetter {

    @Override
    default @Nullable Biome fusionGetBiome(BlockPos pos){
        return ((IWorldReader)this).getBiome(pos);
    }
}
