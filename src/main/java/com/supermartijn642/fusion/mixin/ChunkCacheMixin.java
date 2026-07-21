package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.util.Dimensional;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Created 21/07/2026 by SuperMartijn642
 */
@Mixin(ChunkCache.class)
public class ChunkCacheMixin implements Dimensional {

    @Final
    @Shadow
    private World world;

    @Override
    public int fusionGetDimension(){
        return this.world.provider.getDimension();
    }
}
