package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.extensions.ModelExtension;
import com.supermartijn642.fusion.extensions.ModelPartExtension;
import net.minecraft.client.model.HierarchicalModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Created 18/09/2024 by SuperMartijn642
 */
@Mixin(HierarchicalModel.class)
public class HierarchicalModelMixin implements ModelExtension {

    @Unique
    private Boolean hasFusionModel;

    @Override
    public boolean containsFusionModel(){
        if(this.hasFusionModel == null)
            //noinspection DataFlowIssue
            this.hasFusionModel = ((ModelPartExtension)(Object)((HierarchicalModel<?>)(Object)this).root()).isFusionModelPart();
        return this.hasFusionModel;
    }
}
