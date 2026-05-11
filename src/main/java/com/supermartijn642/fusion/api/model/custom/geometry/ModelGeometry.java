package com.supermartijn642.fusion.api.model.custom.geometry;

import com.supermartijn642.fusion.api.model.custom.CullableQuads;
import com.supermartijn642.fusion.api.model.custom.ModelMaterial;
import com.supermartijn642.fusion.api.model.custom.ModelTransform;
import com.supermartijn642.fusion.api.util.Either;
import com.supermartijn642.fusion.model.custom.geometry.ModelGeometryImpl;
import net.minecraft.client.renderer.model.BlockModel;
import net.minecraft.client.renderer.model.IUnbakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
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
     * Converts the given {@link IUnbakedModel} to a {@link ModelGeometry} instance.
     */
    static ModelGeometry of(IUnbakedModel model){
        return ModelGeometryImpl.of(model);
    }

    /**
     * Converts the given {@link BlockModel} to a {@link CuboidModelGeometry} instance.
     * @see CuboidModelGeometry
     */
    static CuboidModelGeometry of(BlockModel cuboidModel){
        return CuboidModelGeometry.of(cuboidModel);
    }

    /**
     * Whether this geometry is an instance of {@link CuboidModelGeometry}.
     */
    @ApiStatus.NonExtendable
    default boolean isCuboidGeometry(){
        return this instanceof CuboidModelGeometry;
    }

    /**
     * Gets all the required materials or material references for the geometry.
     */
    Collection<Either<String,ModelMaterial>> getRequiredMaterials();

    /**
     * Bakes the geometry into a collection of quads.
     * @param transformation   transformations that should be applied to the geometry
     * @param materialResolver resolver for material keys
     */
    CullableQuads bake(ModelTransform transformation, MaterialResolver materialResolver);

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
                                              Function<ModelMaterial,TextureAtlasSprite> materialResolver,
                                              Consumer<String> reportMissing,
                                              Consumer<List<String>> reportCircular){
            return ModelGeometryImpl.fromKeyLookup(lookup, materialResolver, reportMissing, reportCircular);
        }

        /**
         * Resolves a given material key.
         * @param key      material key to resolve
         * @param required whether the key is required, if {@code true} and the key is missing, it will be reported
         */
        TextureAtlasSprite get(String key, boolean required);

        /**
         * Resolves a given material key. If the key is missing, it will be reported.
         */
        @ApiStatus.NonExtendable
        default TextureAtlasSprite get(String key){
            return this.get(key, true);
        }
    }
}
