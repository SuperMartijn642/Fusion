package com.supermartijn642.fusion;

import com.supermartijn642.fusion.api.model.custom.UntypedModelInstance;
import com.supermartijn642.fusion.model.FusionBlockModelData;
import net.minecraft.client.renderer.model.BlockModel;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.IUnbakedModel;
import net.minecraft.client.renderer.model.ModelBakery;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.ISprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.model.IModelState;
import org.apache.commons.lang3.tuple.Triple;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Created 03/06/2026 by SuperMartijn642
 */
public class MixinReEntrancePreventer {

    public static Collection<ResourceLocation> fusionBlockModelData$gatherBlockModelMaterials(BlockModel model, Function<ResourceLocation,IUnbakedModel> modelGetter, Set<String> missingMaterials){
        return FusionBlockModelData.gatherBlockModelMaterials(model, modelGetter, missingMaterials);
    }

    public static ThreadLocal<ResourceLocation> fusionBlockModel$CURRENT_MODEL(){
        return FusionBlockModelData.CURRENT_MODEL;
    }

    public static void FusionBlockModelData$modelBakery(WeakReference<ModelBakery> value){
        FusionBlockModelData.modelBakery = value;
    }

    public static void modelBakeryMixin$getBakedModel(ResourceLocation location, ISprite modelState, VertexFormat vertexFormat, CallbackInfoReturnable<IBakedModel> ci, ModelBakery modelBakery, IUnbakedModel unbakedModel, AtlasTexture blockAtlas, Map<Triple<ResourceLocation,IModelState,Boolean>,IBakedModel> bakedCache){
        // Handle Fusion models and models with Fusion textures
        if(FusionBlockModelData.containsFusionModelsOrTextures(unbakedModel, blockAtlas)){
            FusionBlockModelData fusionData = FusionBlockModelData.get(unbakedModel);
            if(fusionData == null){
                UntypedModelInstance model = FusionBlockModelData.getModelInstance(unbakedModel);
                fusionData = new FusionBlockModelData(location, model);
                FusionBlockModelData.gatherBlockModelMaterials(fusionData, l -> {
                    IUnbakedModel m = modelBakery.unbakedCache.get(l);
                    return m == null ? modelBakery.unbakedCache.get(l) : m;
                }, new LinkedHashSet<>());
            }
            IBakedModel baked = fusionData.bake(modelBakery, blockAtlas::getSprite, modelState, vertexFormat);
            // Add baked model to the cache
            Triple<ResourceLocation,IModelState,Boolean> key = Triple.of(location, modelState.getState(), modelState.isUvLocked());
            bakedCache.put(key, baked);
            ci.setReturnValue(baked);
        }
    }
}
