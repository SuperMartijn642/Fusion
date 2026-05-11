package com.supermartijn642.fusion.extensions;

import com.supermartijn642.fusion.api.model.ModelInstance;

/**
 * Created 30/04/2023 by SuperMartijn642
 */
public interface BlockModelExtension {

    ModelInstance<?> getFusionModel();

    void setFusionModel(ModelInstance<?> model);
}
