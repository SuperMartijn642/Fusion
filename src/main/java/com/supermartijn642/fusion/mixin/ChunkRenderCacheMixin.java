package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.util.BiomeGetter;
import com.supermartijn642.fusion.util.Dimensional;
import net.minecraft.client.renderer.chunk.ChunkRenderCache;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Created 21/07/2026 by SuperMartijn642
 */
@Mixin(ChunkRenderCache.class)
public class ChunkRenderCacheMixin implements Dimensional, BiomeGetter {

    @Final
    @Shadow
    private World level;

    @Override
    public ResourceLocation fusionGetDimension(){
        return this.level.dimension().location();
    }

    @Override
    public @Nullable ResourceLocation fusionGetBiome(BlockPos pos){
        return ((BiomeGetter)this.level).fusionGetBiome(pos);
    }
}
