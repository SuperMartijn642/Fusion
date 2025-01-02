package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.api.model.ModelInstance;
import com.supermartijn642.fusion.extensions.BlockModelExtension;
import com.supermartijn642.fusion.model.FusionBlockModel;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.resources.model.UnbakedModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Created 30/04/2023 by SuperMartijn642
 */
@Mixin(value = BlockModel.class, priority = 900)
public class BlockModelMixin implements BlockModelExtension {

    @Unique
    private ModelInstance<?> fusionModel;

    @ModifyVariable(
        method = "resolveDependencies(Lnet/minecraft/client/resources/model/ResolvableModel$Resolver;)V",
        at = @At("HEAD"),
        ordinal = 0
    )
    private UnbakedModel.Resolver adjustModelGetter(UnbakedModel.Resolver resolver){
        return location -> {
            UnbakedModel model = resolver.resolve(location);
            if(model instanceof FusionBlockModel)
                return ((FusionBlockModel)model).hasVanillaModel() ? ((FusionBlockModel)model).getVanillaModel() : FusionBlockModel.DUMMY_MODEL;
            return model;
        };
    }

    @Override
    public ModelInstance<?> getFusionModel(){
        return this.fusionModel;
    }

    @Override
    public void setFusionModel(ModelInstance<?> fusionModel){
        this.fusionModel = fusionModel;
    }
}
