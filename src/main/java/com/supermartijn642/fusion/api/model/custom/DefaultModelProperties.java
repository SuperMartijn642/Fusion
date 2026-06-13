package com.supermartijn642.fusion.api.model.custom;

import com.supermartijn642.fusion.api.model.DefaultModelTypes;
import com.supermartijn642.fusion.api.texture.types.connecting.predicates.ConnectionPredicate;
import com.supermartijn642.fusion.api.util.Either;
import com.supermartijn642.fusion.api.util.Property;
import net.minecraftforge.client.RenderTypeGroup;
import net.minecraftforge.client.model.ForgeFaceData;
import net.minecraftforge.client.model.geometry.BlockGeometryBakingContext;

import java.util.Map;

/**
 * Default model properties in Fusion.
 * <p>
 * Created 12/05/2026 by SuperMartijn642
 */
public final class DefaultModelProperties {
    /**
     * Material definition for a given key, pointing to either a material or another material key.
     */
    public static final Property<Either<String,ModelMaterial>,String> MATERIAL = Property.create(String.class);
    /**
     * The connections key to use for a face.
     */
    public static final Property<String,Void> FACE_MATERIAL_KEY = Property.create();

    /**
     * Connections key definitions pointing to either a connection predicate or another connections key.
     * @see DefaultModelTypes#CONNECTING
     */
    public static final Property<Map<String,Either<String,ConnectionPredicate>>,Void> CONNECTION_PREDICATES = Property.create();
    /**
     * Connections definition for a given key, pointing to either a connection predicate or another connections key.
     * @see DefaultModelTypes#CONNECTING
     */
    public static final Property<Either<String,ConnectionPredicate>,String> CONNECTION_PREDICATE = Property.create(String.class);
    /**
     * Connections key for a model element face.
     */
    public static final Property<String,Void> FACE_CONNECTIONS_KEY = Property.create();

    // Forge properties
    /**
     * @see BlockGeometryBakingContext#getRenderTypeHint()
     */
    public static final Property<RenderTypeGroup,Void> FORGE_MODEL_RENDER_TYPE = Property.create();
    /**
     * @see ForgeFaceData#color()
     */
    public static final Property<Integer,Void> FORGE_GEOMETRY_COLOR = Property.create();
    /**
     * @see ForgeFaceData#blockLight()
     */
    public static final Property<Integer,Void> FORGE_GEOMETRY_BLOCK_LIGHT = Property.create();
    /**
     * @see ForgeFaceData#skyLight()
     */
    public static final Property<Integer,Void> FORGE_GEOMETRY_SKY_LIGHT = Property.create();
    /**
     * @see ForgeFaceData#ambientOcclusion()
     */
    public static final Property<Boolean,Void> FORGE_GEOMETRY_AMBIENT_OCCLUSION = Property.create();
}
