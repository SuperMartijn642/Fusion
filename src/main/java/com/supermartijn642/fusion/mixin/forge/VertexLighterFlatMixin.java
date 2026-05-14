package com.supermartijn642.fusion.mixin.forge;

import com.supermartijn642.fusion.api.texture.types.base.BaseTextureData;
import com.supermartijn642.fusion.extensions.VertexLighterFlatExtension;
import com.supermartijn642.fusion.texture.QuadTintingHelper;
import net.minecraftforge.client.model.pipeline.BlockInfo;
import net.minecraftforge.client.model.pipeline.VertexLighterFlat;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Created 14/09/2024 by SuperMartijn642
 */
@Mixin(VertexLighterFlat.class)
public class VertexLighterFlatMixin implements VertexLighterFlatExtension {

    @Unique
    private BaseTextureData.QuadTinting tinting;
    @Final
    @Shadow(remap = false)
    private BlockInfo blockInfo;

    @Override
    public void setFusionCustomTinting(BaseTextureData.QuadTinting tinting){
        this.tinting = tinting;
    }

    @ModifyVariable(
        method = "processQuad",
        at = @At(
            value = "INVOKE_ASSIGN",
            target = "Lnet/minecraftforge/client/model/pipeline/BlockInfo;getColorMultiplier(I)I"
        ),
        ordinal = 0,
        remap = false
    )
    private int tintQuad(int multiplier){
        // Apply custom tinting
        if(this.tinting != null)
            return QuadTintingHelper.getColor(this.tinting, this.blockInfo.getState(), this.blockInfo.getWorld(), this.blockInfo.getBlockPos());
        return multiplier;
    }

    @Inject(
        method = "processQuad",
        at = @At("RETURN"),
        remap = false
    )
    private void clearTinting(CallbackInfo ci){
        this.tinting = null;
    }
}
