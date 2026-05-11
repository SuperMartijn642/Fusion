package com.supermartijn642.fusion.api.model.custom;

import com.supermartijn642.fusion.api.model.types.connecting.predicates.ConnectionPredicate;
import com.supermartijn642.fusion.api.util.Either;
import net.neoforged.neoforge.client.model.ExtraFaceData;

import java.util.Map;

/**
 * A key for some typed property of an unbaked model.
 * <p>
 * Created 01/05/2026 by SuperMartijn642
 * @param <X> value type of the property
 * @param <C> context type of the property
 * @see com.supermartijn642.fusion.api.model.ModelInstance#getProperty(ModelProperty, Object)
 */
public final class ModelProperty<X, C> {

    /**
     * The connections key definitions on a model.
     */
    public static final ModelProperty<Map<String,Either<String,ConnectionPredicate>>,Void> MODEL_CONNECTION_PREDICATES = create();
    /**
     * The connections key to use for a face.
     */
    public static final ModelProperty<String,Void> FACE_CONNECTIONS_KEY = create();

    // NeoForge properties
    /**
     * @see ExtraFaceData#color()
     */
    public static final ModelProperty<Integer,Void> NEO_GEOMETRY_COLOR = create();
    /**
     * @see ExtraFaceData#blockLight()
     */
    public static final ModelProperty<Integer,Void> NEO_GEOMETRY_BLOCK_LIGHT = create();
    /**
     * @see ExtraFaceData#skyLight()
     */
    public static final ModelProperty<Integer,Void> NEO_GEOMETRY_SKY_LIGHT = create();
    /**
     * @see ExtraFaceData#ambientOcclusion()
     */
    public static final ModelProperty<Boolean,Void> NEO_GEOMETRY_AMBIENT_OCCLUSION = create();

    /**
     * Creates a new property key.
     * @param <X> value type of the property
     * @param <C> context type of the property
     */
    public static <X, C> ModelProperty<X,C> create(){
        //noinspection InstantiationOfUtilityClass
        return new ModelProperty<>();
    }

    private ModelProperty(){
    }
}
