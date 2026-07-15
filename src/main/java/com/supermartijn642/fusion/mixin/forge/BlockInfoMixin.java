package com.supermartijn642.fusion.mixin.forge;

import com.supermartijn642.fusion.extensions.BlockInfoExtension;
import net.minecraftforge.client.model.pipeline.BlockInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Created 16/07/2026 by SuperMartijn642
 */
@Mixin(BlockInfo.class)
public class BlockInfoMixin implements BlockInfoExtension {

    @Shadow
    private float shx = 0;
    @Shadow
    private float shy = 0;
    @Shadow
    private float shz = 0;

    @Override
    public void fusionSetOffset(float x, float y, float z){
        this.shx = x;
        this.shy = y;
        this.shz = z;
    }
}
