package com.supermartijn642.fusion.mixin;

import com.google.common.base.Predicate;
import com.supermartijn642.fusion.model.CustomRenderTypeBakedModel;
import com.supermartijn642.fusion.model.ModelRenderTypeHelper;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.MultipartBakedModel;
import net.minecraft.util.BlockRenderLayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

/**
 * Created 20/01/2025 by SuperMartijn642
 */
@Mixin(MultipartBakedModel.class)
public class MultiPartBakedModelMixin implements CustomRenderTypeBakedModel {

    @Final
    @Shadow
    private Map<Predicate<IBlockState>,IBakedModel> selectors;

    @Unique
    private boolean hasCustomRenderTypeModels;

    @Inject(
        method = "<init>",
        at = @At("TAIL")
    )
    private void init(Map<Predicate<IBlockState>,IBakedModel> models, CallbackInfo ci){
        for(IBakedModel model : models.values()){
            if(model instanceof CustomRenderTypeBakedModel){
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

        // Check the predicate for each model
        for(Map.Entry<Predicate<IBlockState>,IBakedModel> selector : this.selectors.entrySet()){
            if(selector.getKey().test(state)){
                IBakedModel model = selector.getValue();
                if(ModelRenderTypeHelper.canRenderInLayer(model, state, layer, isDefaultRenderType))
                    return true;
            }
        }
        return false;
    }
}
