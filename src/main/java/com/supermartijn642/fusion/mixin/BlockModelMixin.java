package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.api.model.ModelInstance;
import com.supermartijn642.fusion.extensions.BlockModelExtension;
import net.minecraft.client.renderer.block.model.BlockModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Created 30/04/2023 by SuperMartijn642
 */
@Mixin(value = BlockModel.class, priority = 900)
public class BlockModelMixin implements BlockModelExtension {

    @Unique
    private ModelInstance<?> fusionModel;

    @Override
    public ModelInstance<?> getFusionModel(){
        return this.fusionModel;
    }

    @Override
    public void setFusionModel(ModelInstance<?> fusionModel){
        this.fusionModel = fusionModel;
    }
}
