package com.supermartijn642.fusion.mixin.forge;

import com.supermartijn642.fusion.extensions.BlockInfoExtension;
import com.supermartijn642.fusion.extensions.VertexLighterFlatExtension;
import net.minecraft.client.renderer.Vector3f;
import net.minecraftforge.client.model.pipeline.VertexLighterSmoothAo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

/**
 * Created 16/07/2026 by SuperMartijn642
 */
@Mixin(VertexLighterSmoothAo.class)
public abstract class VertexLighterSmoothAoMixin {

    @Inject(
        method = "updateBlockInfo",
        at = @At("TAIL"),
        remap = false
    )
    public void modifyRandomOffset(){
        VertexLighterFlatExtension extension = (VertexLighterFlatExtension)this;
        Vector3f offset = extension.getFusionRandomOffsetOverwrite();
        if(offset != null){
            ((BlockInfoExtension)extension.fusionGetBlockInfo()).fusionSetOffset(offset.x(), offset.y(), offset.z());
        }
    }
}
