package com.supermartijn642.fusion.api.model;

import com.supermartijn642.fusion.api.model.custom.UntypedModelInstance;
import com.supermartijn642.fusion.model.ModelInstanceImpl;
import org.jetbrains.annotations.ApiStatus;

/**
 * A container for a model type along with its data.
 * <p>
 * Created 29/04/2023 by SuperMartijn642
 */
@ApiStatus.NonExtendable
public interface ModelInstance<T> extends UntypedModelInstance {

    /**
     * Create a new model instance for the given values.
     */
    static <T> ModelInstance<T> of(ModelType<T> modelType, T modelData){
        return new ModelInstanceImpl<>(modelType, modelData);
    }

    /**
     * The type of the model.
     */
    ModelType<T> getModelType();

    /**
     * The data of the model.
     */
    T getModelData();
}
