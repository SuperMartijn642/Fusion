package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.model.CustomRenderTypeBakedModel;
import com.supermartijn642.fusion.model.ModelRenderTypeHelper;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.WeightedBakedModel;
import net.minecraft.util.BlockRenderLayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Created 26/10/2023 by SuperMartijn642
 */
@Mixin(WeightedBakedModel.class)
public class WeightedBakedModelMixin implements CustomRenderTypeBakedModel {

    @Final
    @Shadow
    private List<WeightedBakedModel.WeightedModel> models;
    @Unique
    private boolean hasCustomRenderTypeModels;

    @Inject(
        method = "<init>",
        at = @At("TAIL")
    )
    public void init(List<WeightedBakedModel.WeightedModel> models, CallbackInfo ci){
        for(WeightedBakedModel.WeightedModel entry : this.models){
            if(entry.model instanceof CustomRenderTypeBakedModel){
                this.hasCustomRenderTypeModels = true;
                break;
            }
        }
    }

    @Override
    public boolean canRenderInLayer(IBlockState state, BlockRenderLayer layer){
        boolean isDefaultRenderType = ModelRenderTypeHelper.couldBlockRenderInLayerOriginally(state, layer);
        if(!this.hasCustomRenderTypeModels)
            return isDefaultRenderType;
        for(WeightedBakedModel.WeightedModel entry : this.models){
            IBakedModel model = entry.model;
            if(ModelRenderTypeHelper.canRenderInLayer(model, state, layer, isDefaultRenderType))
                return true;
        }
        return false;
    }
}
