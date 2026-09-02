package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.util.BiomeGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Created 03/09/2026 by SuperMartijn642
 */
@Mixin(LevelReader.class)
public interface LevelReaderMixin extends BiomeGetter {

    @Override
    default @Nullable ResourceLocation fusionGetBiome(BlockPos pos){
        Biome biome = ((LevelReader)this).getBiome(pos);
        return biome == null ? null : BuiltinRegistries.BIOME.getKey(biome);
    }
}
