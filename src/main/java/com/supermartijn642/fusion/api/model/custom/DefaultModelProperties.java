package com.supermartijn642.fusion.api.model.custom;

import com.supermartijn642.fusion.api.texture.types.connecting.predicates.ConnectionPredicate;
import com.supermartijn642.fusion.api.util.Either;
import com.supermartijn642.fusion.api.util.Property;
import net.neoforged.neoforge.client.model.ExtraFaceData;

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

    // NeoForge properties
    /**
     * @see ExtraFaceData#color()
     */
    public static final Property<Integer,Void> NEO_GEOMETRY_COLOR = Property.create();
    /**
     * @see ExtraFaceData#blockLight()
     */
    public static final Property<Integer,Void> NEO_GEOMETRY_BLOCK_LIGHT = Property.create();
    /**
     * @see ExtraFaceData#skyLight()
     */
    public static final Property<Integer,Void> NEO_GEOMETRY_SKY_LIGHT = Property.create();
    /**
     * @see ExtraFaceData#ambientOcclusion()
     */
    public static final Property<Boolean,Void> NEO_GEOMETRY_AMBIENT_OCCLUSION = Property.create();
}
