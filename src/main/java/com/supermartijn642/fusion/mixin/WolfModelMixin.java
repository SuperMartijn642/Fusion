package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.extensions.ModelExtension;
import com.supermartijn642.fusion.extensions.ModelPartExtension;
import net.minecraft.client.model.WolfModel;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Created 18/09/2024 by SuperMartijn642
 */
@Mixin(WolfModel.class)
public class WolfModelMixin implements ModelExtension {

    @Unique
    private boolean hasFusionModel;

    @Inject(
        method = "<init>",
        at = @At("TAIL")
    )
    private void init(ModelPart part, CallbackInfo ci) {
        //noinspection DataFlowIssue
        this.hasFusionModel = ((ModelPartExtension)(Object)part).isFusionModelPart();
    }

    @Override
    public boolean containsFusionModel(){
        return this.hasFusionModel;
    }
}
