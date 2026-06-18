package com.supermartijn642.fusion.extensions;

import com.supermartijn642.fusion.entity.model.EntityLayerProperties;

/**
 * Created 29/09/2025 by SuperMartijn642
 */
public interface EntityRenderStateExtension {

    void setFusionModel(int layerIndex, EntityLayerProperties.ModelChoice model);

    EntityLayerProperties.ModelChoice getFusionModel(int layerIndex);

    boolean hasFusionContext();
}
