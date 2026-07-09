package com.supermartijn642.fusion.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.supermartijn642.fusion.api.model.custom.ModelResolver;
import com.supermartijn642.fusion.model.FusionBlockModelData;
import net.minecraft.client.resources.model.ModelDiscovery;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Created 09/07/2026 by SuperMartijn642
 */
@Mixin(ModelDiscovery.class)
public class ModelDiscoveryMixin {

    @Final
    @Shadow
    private ModelDiscovery.ModelWrapper missingModel;

    @Shadow
    private ModelDiscovery.ModelWrapper getOrCreateModel(ResourceLocation id) {
        throw new AssertionError();
    }

    @Inject(
        method = "createAndQueueWrapper",
        at = @At(
            value = "INVOKE_ASSIGN",
            target = "Lnet/minecraft/client/resources/model/ModelDiscovery;isRoot(Lnet/minecraft/client/resources/model/UnbakedModel;)Z",
            shift = At.Shift.AFTER
        )
    )
    private void createAndQueueWrapper(ResourceLocation identifier, UnbakedModel model, CallbackInfoReturnable<ModelDiscovery.ModelWrapper> ci, @Local LocalBooleanRef isRoot) {
        FusionBlockModelData fusionData = FusionBlockModelData.get(model);
        if(fusionData == null)
            return;
        if(!fusionData.getDependencies().isEmpty())
            isRoot.set(false);
    }

    @ModifyExpressionValue(
        method = "discoverDependencies",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/resources/model/UnbakedModel;parent()Lnet/minecraft/resources/ResourceLocation;"
        )
    )
    private ResourceLocation redirectParent(ResourceLocation parent, @Local(ordinal = 0) ModelDiscovery.ModelWrapper current) {
        if(parent != null)
            return parent;
        FusionBlockModelData fusionData = FusionBlockModelData.get(current.wrapped());
        if(fusionData == null)
            return null;
        return ModelResolver.MISSING_MODEL;
    }

    @Inject(
        method = "discoverDependencies",
        at = @At(
            value = "INVOKE_ASSIGN",
            target = "Lnet/minecraft/client/resources/model/ModelDiscovery;getOrCreateModel(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/resources/model/ModelDiscovery$ModelWrapper;",
            shift = At.Shift.AFTER
        )
    )
    private void discoverDependencies(List<ModelDiscovery.ModelWrapper> toValidate, CallbackInfo ci, @Local(ordinal = 0) ModelDiscovery.ModelWrapper current, @Local(ordinal = 1) LocalRef<ModelDiscovery.ModelWrapper> parent) {
        FusionBlockModelData fusionData = FusionBlockModelData.get(current.wrapped());
        if(fusionData == null)
            return;
        boolean allValid = true;
        for(ResourceLocation dependency : fusionData.getDependencies()){
            ModelDiscovery.ModelWrapper m = this.getOrCreateModel(dependency);
            if(!m.valid)
                allValid = false;
        }
        if(allValid)
            current.valid = true;
        else
            toValidate.add(current);
        parent.set(this.missingModel);
    }
}
