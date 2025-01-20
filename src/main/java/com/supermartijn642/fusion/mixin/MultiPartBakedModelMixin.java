package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.model.types.base.CustomRenderTypeBakedModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.MultiPartBakedModel;
import net.minecraft.world.level.block.state.BlockState;
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
@Mixin(MultiPartBakedModel.class)
public class MultiPartBakedModelMixin implements CustomRenderTypeBakedModel {

    @Unique
    private Set<RenderType> customBlockRenderTypes;

    @Inject(
        method = "<init>",
        at = @At("TAIL")
    )
    private void init(List<Pair<Predicate<BlockState>,BakedModel>> models, CallbackInfo ci){
        Set<RenderType> customBlockRenderTypes = null;
        for(Pair<Predicate<BlockState>,BakedModel> model : models){
            if(model.getRight() instanceof CustomRenderTypeBakedModel){
                Collection<RenderType> renderTypes = ((CustomRenderTypeBakedModel)model.getRight()).getBlockRenderTypes();
                if(!renderTypes.isEmpty()){
                    if(customBlockRenderTypes == null)
                        customBlockRenderTypes = new HashSet<>();
                    customBlockRenderTypes.addAll(renderTypes);
                }
            }
        }
        this.customBlockRenderTypes = customBlockRenderTypes == null ? Collections.emptySet() : Set.copyOf(customBlockRenderTypes);
    }

    @Override
    public Collection<RenderType> getBlockRenderTypes(){
        return this.customBlockRenderTypes;
    }
}
