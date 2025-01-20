package com.supermartijn642.fusion.mixin;

import com.google.common.collect.ImmutableSet;
import com.supermartijn642.fusion.model.types.base.CustomRenderTypeBakedModel;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.MultipartBakedModel;
import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;
import java.util.function.Predicate;

/**
 * Created 20/01/2025 by SuperMartijn642
 */
@Mixin(MultipartBakedModel.class)
public class MultiPartBakedModelMixin implements CustomRenderTypeBakedModel {

    @Unique
    private Set<RenderType> customBlockRenderTypes;

    @Inject(
        method = "<init>",
        at = @At("TAIL")
    )
    private void init(List<Pair<Predicate<BlockState>,IBakedModel>> models, CallbackInfo ci){
        Set<RenderType> customBlockRenderTypes = null;
        for(Pair<Predicate<BlockState>,IBakedModel> model : models){
            if(model.getRight() instanceof CustomRenderTypeBakedModel){
                Collection<RenderType> renderTypes = ((CustomRenderTypeBakedModel)model.getRight()).getBlockRenderTypes();
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
    public Collection<RenderType> getBlockRenderTypes(){
        return this.customBlockRenderTypes;
    }
}
