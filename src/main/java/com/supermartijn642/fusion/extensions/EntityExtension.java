package com.supermartijn642.fusion.extensions;

import com.supermartijn642.fusion.entity.model.EntityLayerProperties;

/**
 * Created 23/09/2024 by SuperMartijn642
 */
public interface EntityExtension {

    EntityLayerProperties.ModelChoice getFusionModel(int layerIndex);

    void setFusionModel(int layerIndex, EntityLayerProperties.ModelChoice model);

    boolean shouldFusionRecomputeModel(int layerIndex);

    void markFusionRecomputeModels();
}
