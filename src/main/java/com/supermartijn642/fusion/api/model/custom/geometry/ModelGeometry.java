package com.supermartijn642.fusion.api.model.custom.geometry;

import com.supermartijn642.fusion.api.model.custom.CullableQuads;
import com.supermartijn642.fusion.api.model.custom.ModelMaterial;
import com.supermartijn642.fusion.api.model.custom.ModelTransform;
import com.supermartijn642.fusion.api.model.custom.quad.MutableQuad;
import com.supermartijn642.fusion.api.util.Either;
import com.supermartijn642.fusion.api.util.PropertyGetter;
import com.supermartijn642.fusion.model.custom.geometry.ModelGeometryImpl;
import net.minecraft.client.resources.model.cuboid.UnbakedCuboidGeometry;
import net.minecraft.client.resources.model.geometry.UnbakedGeometry;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * An interface representing some geometry that can be baked into a collection of quads.
 * <p>
 * Created 02/05/2026 by SuperMartijn642
 * @see CuboidModelGeometry
 * @see com.supermartijn642.fusion.api.model.ModelType#getGeometry(Object)
 */
public interface ModelGeometry {

    /**
     * Converts the given {@link UnbakedGeometry} to a {@link ModelGeometry} instance.
     */
    static ModelGeometry of(UnbakedGeometry geometry){
        return ModelGeometryImpl.of(geometry);
    }

    /**
     * Converts the given {@link UnbakedCuboidGeometry} to a {@link CuboidModelGeometry} instance.
     * @see CuboidModelGeometry
     */
    static CuboidModelGeometry of(UnbakedCuboidGeometry cuboidGeometry){
        return CuboidModelGeometry.of(cuboidGeometry);
    }

    /**
     * Whether this geometry is an instance of {@link CuboidModelGeometry}.
     */
    @ApiStatus.NonExtendable
    default boolean isCuboidGeometry(){
        return this instanceof CuboidModelGeometry;
    }

    /**
     * Bakes the geometry into a collection of quads.
     * @param transformation   transformations that should be applied to the geometry
     * @param materialResolver resolver for material keys
     */
    @ApiStatus.NonExtendable
    default CullableQuads bake(ModelTransform transformation, MaterialResolver materialResolver){
        CullableQuads.Builder quads = CullableQuads.builder();
        this.bake(
            (quad, cullDirection, properties) -> quads.add(cullDirection, quad),
            transformation,
            materialResolver
        );
        return quads.build();
    }

    /**
     * Bakes the geometry into quads.
     * @param transformation   transformations that should be applied to the geometry
     * @param materialResolver resolver for material keys
     * @param consumer         consumer for the quads along with properties from the geometry
     */
    void bake(QuadConsumer consumer, ModelTransform transformation, MaterialResolver materialResolver);

    @FunctionalInterface
    interface QuadConsumer {
        /**
         * Consumes a quad.
         * @param quad               the created quad
         * @param cullDirection      cull direction of the quad
         * @param geometryProperties properties from the quad's geometry
         */
        void consume(MutableQuad quad, Direction cullDirection, PropertyGetter geometryProperties);
    }

    /**
     * Resolver for material keys into resolved materials.
     */
    @FunctionalInterface
    interface MaterialResolver {

        /**
         * Creates a material resolver from a lookup for material references.
         * @param lookup           lookup for keys into other keys or unresolved materials
         * @param materialResolver resolves unresolved materials
         * @param reportMissing    consumer for reporting missing required material keys, the missing key is given as an argument
         * @param reportCircular   consumer for reporting circular material references, the chain of references is given as an argument
         */
        static MaterialResolver fromKeyLookup(Function<String,@Nullable Either<String,ModelMaterial>> lookup,
                                              Function<ModelMaterial,ModelMaterial.Resolved> materialResolver,
                                              Consumer<String> reportMissing,
                                              Consumer<List<String>> reportCircular){
            return ModelGeometryImpl.fromKeyLookup(lookup, materialResolver, reportMissing, reportCircular);
        }

        /**
         * Resolves a given material key.
         * @param key      material key to resolve
         * @param required whether the key is required, if {@code true} and the key is missing, it will be reported
         */
        ModelMaterial.Resolved get(String key, boolean required);

        /**
         * Resolves a given material key. If the key is missing, it will be reported.
         */
        @ApiStatus.NonExtendable
        default ModelMaterial.Resolved get(String key){
            return this.get(key, true);
        }
    }
}
