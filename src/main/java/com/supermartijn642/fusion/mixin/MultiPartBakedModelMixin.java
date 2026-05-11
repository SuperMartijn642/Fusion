package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.model.CustomRenderTypeBakedModel;
import com.supermartijn642.fusion.model.ModelRenderTypeHelper;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.MultiPartBakedModel;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Created 20/01/2025 by SuperMartijn642
 */
@Mixin(MultiPartBakedModel.class)
public class MultiPartBakedModelMixin implements CustomRenderTypeBakedModel {

    @Final
    @Shadow
    private List<Pair<Predicate<BlockState>,BakedModel>> selectors;
    @Final
    @Shadow
    private Map<BlockState,BitSet> selectorCache;

    @Unique
    private boolean hasCustomRenderTypeModels;

    @Inject(
        method = "<init>",
        at = @At("TAIL")
    )
    private void init(List<Pair<Predicate<BlockState>,BakedModel>> models, CallbackInfo ci){
        for(Pair<Predicate<BlockState>,BakedModel> model : models){
            if(model.getRight() instanceof CustomRenderTypeBakedModel){
                this.hasCustomRenderTypeModels = true;
                break;
            }
        }
    }

    @Override
    public boolean canRenderInLayer(BlockState state, RenderType layer){
        boolean isDefaultRenderType = ModelRenderTypeHelper.couldBlockRenderInLayerOriginally(state, layer);
        if(!this.hasCustomRenderTypeModels)
            return isDefaultRenderType;

        // If the block state is already cached, use the cache
        BitSet bitset = this.selectorCache.get(state);
        if(bitset != null){
            for(int i = 0; i < bitset.length(); ++i){
                if(bitset.get(i)){
                    BakedModel model = this.selectors.get(i).getRight();
                    if(ModelRenderTypeHelper.canRenderInLayer(model, state, layer, isDefaultRenderType))
                        return true;
                }
            }
            return false;
        }

        // Check the predicate for each model
        for(Pair<Predicate<BlockState>,BakedModel> selector : this.selectors){
            if(selector.getLeft().test(state)){
                BakedModel model = selector.getRight();
                if(ModelRenderTypeHelper.canRenderInLayer(model, state, layer, isDefaultRenderType))
                    return true;
            }
        }
        return false;
    }
}
