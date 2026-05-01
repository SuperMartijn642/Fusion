package com.supermartijn642.fusion.extensions;

import com.supermartijn642.fusion.api.model.ModelInstance;
import com.supermartijn642.fusion.model.FusionBlockModel;

/**
 * Created 30/04/2023 by SuperMartijn642
 */
public interface CuboidModelExtension {

    ModelInstance<?> getFusionModel();

    void setFusionModel(ModelInstance<?> model);

    FusionBlockModel getFusionBlockModelData();

    void setFusionBlockModelData(FusionBlockModel data);
}
