package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.util.Dimensional;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Created 21/07/2026 by SuperMartijn642
 */
@Mixin(RenderSectionRegion.class)
public class RenderSectionRegionMixin implements Dimensional {

    @Final
    @Shadow
    private Level level;

    @Override
    public ResourceLocation fusionGetDimension(){
        return this.level.dimension().location();
    }
}
