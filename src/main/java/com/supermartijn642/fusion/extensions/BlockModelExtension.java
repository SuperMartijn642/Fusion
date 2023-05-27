package com.supermartijn642.fusion.extensions;

import com.supermartijn642.fusion.api.model.ModelInstance;
import net.minecraftforge.client.model.IModel;

/**
 * Created 30/04/2023 by SuperMartijn642
 */
public interface BlockModelExtension {

    ModelInstance<?> getFusionModel();

    void setFusionModel(ModelInstance<?> model);

    IModel getWrapper();

    void setWrapper(IModel model);
}
