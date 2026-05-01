package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.extensions.MaterialInfoExtension;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Created 01/05/2026 by SuperMartijn642
 */
@Mixin(BakedQuad.MaterialInfo.class)
public class BakedQuadMaterialInfoMixin implements MaterialInfoExtension {

    @Unique
    private boolean fusionAmbientOcclusion;

    @Override
    public void setFusionAmbientOcclusion(boolean ambientOcclusion){
        this.fusionAmbientOcclusion = ambientOcclusion;
    }

    @Override
    public boolean getFusionAmbientOcclusion(){
        return this.fusionAmbientOcclusion;
    }
}
