package com.supermartijn642.fusion.api.model.custom;

import com.supermartijn642.fusion.api.texture.types.connecting.predicates.ConnectionPredicate;
import com.supermartijn642.fusion.api.util.Either;
import com.supermartijn642.fusion.api.util.Property;

import java.util.Map;

/**
 * Default model properties in Fusion.
 * <p>
 * Created 12/05/2026 by SuperMartijn642
 */
public class DefaultModelProperties {
    /**
     * The connections key definitions on a model.
     */
    public static final Property<Map<String,Either<String,ConnectionPredicate>>,Void> MODEL_CONNECTION_PREDICATES = Property.create();
    /**
     * The connections key to use for a face.
     */
    public static final Property<String,Void> FACE_CONNECTIONS_KEY = Property.create();
}
