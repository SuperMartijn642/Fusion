package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.util.Dimensional;
import net.minecraft.client.renderer.chunk.ChunkRenderCache;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Created 21/07/2026 by SuperMartijn642
 */
@Mixin(ChunkRenderCache.class)
public class ChunkRenderCacheMixin implements Dimensional {

    @Final
    @Shadow
    private World level;

    @Override
    public ResourceLocation fusionGetDimension(){
        return this.level.dimension().location();
    }
}
