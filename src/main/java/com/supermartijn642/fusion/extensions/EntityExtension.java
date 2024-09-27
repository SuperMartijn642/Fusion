package com.supermartijn642.fusion.extensions;

import com.supermartijn642.fusion.util.Triple;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;

/**
 * Created 23/09/2024 by SuperMartijn642
 */
public interface EntityExtension {

    Triple<ModelPart,ResourceLocation,Float> getFusionModel(int layerIndex);

    void setFusionModel(int layerIndex, Triple<ModelPart,ResourceLocation,Float> model);

    boolean shouldFusionRecomputeModel(int layerIndex);

    void markFusionRecomputeModels();
}
