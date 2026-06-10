package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.api.model.custom.UntypedModelInstance;
import com.supermartijn642.fusion.model.FusionBlockModelData;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.renderer.item.BlockModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Created 09/04/2025 by SuperMartijn642
 */
@Mixin(BlockModelWrapper.Unbaked.class)
public class BlockModelWrapperMixin {

    @Inject(
        method = "bake",
        at = @At("HEAD"),
        cancellable = true
    )
    private void bake(ItemModel.BakingContext context, CallbackInfoReturnable<ItemModel> ci){
        //noinspection DataFlowIssue
        BlockModelWrapper.Unbaked unbaked = (BlockModelWrapper.Unbaked)(Object)this;
        ResourceLocation location = unbaked.model();
        ResolvedModel wrapper = context.blockModelBaker().getModel(location);
        if(wrapper == null)
            return;
        // Handle Fusion models and models with Fusion textures
        if(FusionBlockModelData.containsFusionModelsOrTextures(wrapper)){
            FusionBlockModelData fusionData = FusionBlockModelData.get(wrapper.wrapped());
            if(fusionData == null){
                UntypedModelInstance model = FusionBlockModelData.getModelInstance(wrapper.wrapped());
                fusionData = new FusionBlockModelData(location, model);
            }
            List<ItemTintSource> tintSources = unbaked.tints();
            ItemModel model = fusionData.bakeItemModel(wrapper, tintSources, context.blockModelBaker(), context.entityModelSet());
            ci.setReturnValue(model);
        }
    }
}
