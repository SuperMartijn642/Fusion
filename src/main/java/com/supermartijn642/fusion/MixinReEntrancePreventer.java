package com.supermartijn642.fusion;

import com.supermartijn642.fusion.model.FusionBlockModelData;
import net.minecraft.client.renderer.model.BlockModel;
import net.minecraft.client.renderer.model.IUnbakedModel;
import net.minecraft.client.renderer.model.ModelBakery;
import net.minecraft.util.ResourceLocation;

import java.lang.ref.WeakReference;
import java.util.Collection;
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
}
