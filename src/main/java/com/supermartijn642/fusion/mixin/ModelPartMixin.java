package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.extensions.ModelPartExtension;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Created 18/09/2024 by SuperMartijn642
 */
@Mixin(ModelPart.class)
public class ModelPartMixin implements ModelPartExtension {

    @Unique
    private boolean fusionModel;

    @Override
    public boolean isFusionModelPart(){
        return this.fusionModel;
    }

    @Override
    public void setFusionModelPart(){
        this.fusionModel = true;
    }
}
