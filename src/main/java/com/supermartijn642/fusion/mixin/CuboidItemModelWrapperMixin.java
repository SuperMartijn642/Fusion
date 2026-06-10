package com.supermartijn642.fusion.mixin;

import com.mojang.math.Transformation;
import com.supermartijn642.fusion.api.model.custom.UntypedModelInstance;
import com.supermartijn642.fusion.model.FusionBlockModelData;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Created 09/04/2025 by SuperMartijn642
 */
@Mixin(CuboidItemModelWrapper.Unbaked.class)
public class CuboidItemModelWrapperMixin {

    @Inject(
        method = "bake",
        at = @At("HEAD"),
        cancellable = true
    )
    private void bake(ItemModel.BakingContext context, Matrix4fc transformation, CallbackInfoReturnable<ItemModel> ci){
        //noinspection DataFlowIssue
        CuboidItemModelWrapper.Unbaked unbaked = (CuboidItemModelWrapper.Unbaked)(Object)this;
        Identifier location = unbaked.model();
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
            transformation = Transformation.compose(transformation, unbaked.transformation());
            ItemModel model = fusionData.bakeItemModel(wrapper, transformation, tintSources, context.blockModelBaker(), context.entityModelSet());
            ci.setReturnValue(model);
        }
    }
}
