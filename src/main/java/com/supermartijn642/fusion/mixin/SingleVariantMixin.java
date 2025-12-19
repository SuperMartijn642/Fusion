package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.model.FusionBlockModel;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.block.model.SingleVariant;
import net.minecraft.client.renderer.block.model.Variant;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Created 09/04/2025 by SuperMartijn642
 */
@Mixin(SingleVariant.Unbaked.class)
public class SingleVariantMixin {

    @Inject(
        method = "bake",
        at = @At("HEAD"),
        cancellable = true
    )
    private void bake(ModelBaker modelBaker, CallbackInfoReturnable<BlockStateModel> ci){
        //noinspection DataFlowIssue
        SingleVariant.Unbaked unbaked = (SingleVariant.Unbaked)(Object)this;
        Variant variant = unbaked.variant();
        Identifier location = variant.modelLocation();
        ResolvedModel wrapper = modelBaker.getModel(location);
        if(wrapper != null){
            UnbakedModel wrapped = wrapper.wrapped();
            if(wrapped instanceof FusionBlockModel fusionBlockModel){
                Variant.SimpleModelState modelState = variant.modelState();
                BlockStateModel model = fusionBlockModel.bakeBlockModel(wrapper, modelBaker, modelState.asModelState());
                ci.setReturnValue(model);
            }
        }
    }
}
