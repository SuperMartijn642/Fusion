package com.supermartijn642.fusion.mixin;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.supermartijn642.fusion.model.types.base.CustomRenderTypeBakedModel;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.MultipartBakedModel;
import net.minecraft.util.BlockRenderLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

/**
 * Created 20/01/2025 by SuperMartijn642
 */
@Mixin(MultipartBakedModel.class)
public class MultiPartBakedModelMixin implements CustomRenderTypeBakedModel {


    @Unique
    private Set<BlockRenderLayer> customBlockRenderTypes;

    @Inject(
        method = "<init>",
        at = @At("TAIL")
    )
    private void init(Map<Predicate<IBlockState>,IBakedModel> models, CallbackInfo ci){
        Set<BlockRenderLayer> customBlockRenderTypes = null;
        for(IBakedModel model : models.values()){
            if(model instanceof CustomRenderTypeBakedModel){
                Collection<BlockRenderLayer> renderTypes = ((CustomRenderTypeBakedModel)model).getBlockRenderTypes();
                if(!renderTypes.isEmpty()){
                    if(customBlockRenderTypes == null)
                        customBlockRenderTypes = new HashSet<>();
                    customBlockRenderTypes.addAll(renderTypes);
                }
            }
        }
        this.customBlockRenderTypes = customBlockRenderTypes == null ? Collections.emptySet() : ImmutableSet.copyOf(customBlockRenderTypes);
    }

    @Override
    public Collection<BlockRenderLayer> getBlockRenderTypes(){
        return this.customBlockRenderTypes;
    }
}
