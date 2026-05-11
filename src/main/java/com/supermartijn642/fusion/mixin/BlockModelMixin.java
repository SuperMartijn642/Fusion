package com.supermartijn642.fusion.mixin;

import com.mojang.datafixers.util.Pair;
import com.supermartijn642.fusion.api.model.ModelInstance;
import com.supermartijn642.fusion.extensions.BlockModelExtension;
import com.supermartijn642.fusion.model.FusionBlockModelData;
import net.minecraft.client.renderer.model.BlockModel;
import net.minecraft.client.renderer.model.IUnbakedModel;
import net.minecraft.client.renderer.model.Material;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.Set;
import java.util.function.Function;

/**
 * Created 30/04/2023 by SuperMartijn642
 */
@Mixin(value = BlockModel.class, priority = 900)
public class BlockModelMixin implements BlockModelExtension {

    @Unique
    private ModelInstance<?> fusionModel;

    @Override
    public ModelInstance<?> getFusionModel(){
        return this.fusionModel;
    }

    @Override
    public void setFusionModel(ModelInstance<?> fusionModel){
        this.fusionModel = fusionModel;
    }

    @Inject(
        method = "getMaterials",
        at = @At("HEAD"),
        cancellable = true
    )
    private void getMaterials(Function<ResourceLocation,IUnbakedModel> modelGetter, Set<Pair<String,String>> missingMaterials, CallbackInfoReturnable<Collection<Material>> ci){
        //noinspection DataFlowIssue
        BlockModel model = (BlockModel)(Object)this;
        Collection<Material> materials = FusionBlockModelData.gatherBlockModelMaterials(model, modelGetter, missingMaterials);
        if(materials != null)
            ci.setReturnValue(materials);
    }
}
