package com.supermartijn642.fusion.mixin.forge;

import com.supermartijn642.fusion.extensions.BlockInfoExtension;
import net.minecraftforge.client.model.pipeline.BlockInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.vecmath.Vector3f;

/**
 * Created 16/07/2026 by SuperMartijn642
 */
@Mixin(BlockInfo.class)
public class BlockInfoMixin implements BlockInfoExtension {

    @Shadow
    private float shx = 0;
    private float shy = 0;
    private float shz = 0;

    @Unique
    private Vector3f offset;

    @Override
    public void setFusionOffsetOverwrite(Vector3f offset){
        this.offset = offset;
    }

    @Inject(
        method = "updateShift",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private void updateShift(CallbackInfo ci){
        if(this.offset == null)
            return;
        this.shx = this.offset.getX();
        this.shy = this.offset.getY();
        this.shz = this.offset.getZ();
        ci.cancel();
    }
}
