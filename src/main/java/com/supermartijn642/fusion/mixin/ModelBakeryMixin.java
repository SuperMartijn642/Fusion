package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.model.items.ItemModelPredicatesReloadListener;
import com.supermartijn642.fusion.model.overlays.BlockModelOverlayReloadListener;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Created 22/09/2024 by SuperMartijn642
 */
@Mixin(ModelBakery.class)
public class ModelBakeryMixin {

    @Final
    @Shadow
    private static ModelResourceLocation MISSING_MODEL_LOCATION;

    @Inject(
        method = "loadTopLevel",
        at = @At("HEAD")
    )
    private void registerBlockModelOverlays(ModelResourceLocation location, CallbackInfo ci){
        if(location == MISSING_MODEL_LOCATION){
            //noinspection DataFlowIssue
            ModelBakery modelBakery = (ModelBakery)(Object)this;
            BlockModelOverlayReloadListener.INSTANCE.registerOverlays(modelBakery);
            ItemModelPredicatesReloadListener.INSTANCE.registerPredicateModels(modelBakery);
        }
    }
}
