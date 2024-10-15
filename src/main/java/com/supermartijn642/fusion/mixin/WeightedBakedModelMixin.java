package com.supermartijn642.fusion.mixin;

import com.google.common.collect.ImmutableSet;
import com.supermartijn642.fusion.model.types.base.CustomRenderTypeBakedModel;
import net.minecraft.client.renderer.block.model.WeightedBakedModel;
import net.minecraft.util.BlockRenderLayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created 26/10/2023 by SuperMartijn642
 */
@Mixin(WeightedBakedModel.class)
public class WeightedBakedModelMixin implements CustomRenderTypeBakedModel {

    @Final
    @Shadow
    private List<WeightedBakedModel.WeightedModel> models;
    @Unique
    private Set<BlockRenderLayer> customBlockRenderTypes;

    @Inject(
        method = "<init>",
        at = @At("TAIL")
    )
    public void init(List<WeightedBakedModel.WeightedModel> models, CallbackInfo ci){
        Set<BlockRenderLayer> customBlockRenderTypes = new HashSet<>();
        this.models.stream()
            .map(o -> o.model)
            .filter(CustomRenderTypeBakedModel.class::isInstance)
            .map(CustomRenderTypeBakedModel.class::cast)
            .map(CustomRenderTypeBakedModel::getBlockRenderTypes)
            .forEach(customBlockRenderTypes::addAll);
        this.customBlockRenderTypes = ImmutableSet.copyOf(customBlockRenderTypes);
    }

    @Override
    public Collection<BlockRenderLayer> getBlockRenderTypes(){
        return this.customBlockRenderTypes;
    }
}
