package com.supermartijn642.fusion.mixin.forge;

import com.supermartijn642.fusion.api.texture.types.base.BaseTextureData;
import com.supermartijn642.fusion.extensions.BlockInfoExtension;
import com.supermartijn642.fusion.extensions.VertexLighterFlatExtension;
import com.supermartijn642.fusion.texture.QuadTintingHelper;
import net.minecraft.client.renderer.Vector3f;
import net.minecraftforge.client.model.pipeline.BlockInfo;
import net.minecraftforge.client.model.pipeline.VertexLighterFlat;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Created 14/09/2024 by SuperMartijn642
 */
@Mixin(VertexLighterFlat.class)
public class VertexLighterFlatMixin implements VertexLighterFlatExtension {

    @Unique
    private BaseTextureData.QuadTinting tinting;
    @Unique
    private Vector3f offset;

    @Final
    @Shadow(remap = false)
    private BlockInfo blockInfo;

    @Override
    public void setFusionCustomTinting(BaseTextureData.QuadTinting tinting){
        this.tinting = tinting;
    }

    @Override
    public void setFusionRandomOffsetOverwrite(Vector3f offset){
        this.offset = offset;
    }

    @Override
    public Vector3f getFusionRandomOffsetOverwrite(){
        return this.offset;
    }

    @Override
    public BlockInfo fusionGetBlockInfo(){
        return this.blockInfo;
    }

    @ModifyArg(
        method = "processQuad",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraftforge/client/model/pipeline/VertexLighterFlat;updateColor([F[FFFFFI)V"
        ),
        index = 6,
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

    @Inject(
        method = "updateBlockInfo",
        at = @At("TAIL"),
        remap = false
    )
    public void modifyRandomOffset(){
        if(this.offset != null)
            ((BlockInfoExtension)this.blockInfo).fusionSetOffset(this.offset.x(), this.offset.y(), this.offset.z());
    }
}
