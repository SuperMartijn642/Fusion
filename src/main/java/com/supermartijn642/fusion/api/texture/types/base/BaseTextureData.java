package com.supermartijn642.fusion.api.texture.types.base;

import com.supermartijn642.fusion.api.texture.DefaultTextureTypes;
import com.supermartijn642.fusion.texture.types.base.BaseTextureDataBuilderImpl;
import org.jetbrains.annotations.Nullable;

/**
 * Data for base texture type.
 * <p>
 * Created 06/09/2024 by SuperMartijn642
 * @see DefaultTextureTypes#BASE
 */
public interface BaseTextureData {

    /**
     * Creates a builder for base texture data.
     */
    static Builder<?,BaseTextureData> builder(){
        return new BaseTextureDataBuilderImpl();
    }

    /**
     * Render type used for the texture.
     * @see RenderType
     */
    @Nullable
    RenderType getRenderType();

    /**
     * Whether the texture is emissive.
     */
    boolean isEmissive();

    /**
     * Tinting used for the texture.
     * @see QuadTinting
     */
    @Nullable
    QuadTinting getTinting();

    enum RenderType {
        /**
         * Pixels in the texture will be rendered fully opaque.
         */
        OPAQUE,
        /**
         * Every pixel in the texture will be either fully transparent or fully opaque.
         */
        CUTOUT,
        /**
         * Pixels will be rendered with the transparency in the texture.
         */
        TRANSLUCENT
    }

    enum QuadTinting {
        /**
         * Provides a grass tint based on the biome the model is in.
         */
        BIOME_GRASS,
        /**
         * Provides a foliage tint based on the biome the model is in.
         */
        BIOME_FOLIAGE,
        /**
         * Provides a water tint based on the biome the model is in.
         */
        BIOME_WATER
    }

    interface Builder<T extends Builder<T,S>, S> {

        /**
         * Sets the render type used for the texture.
         */
        T renderType(@Nullable RenderType type);

        /**
         * Sets whether the texture is emissive.
         */
        T emissive(boolean emissive);

        /**
         * Sets the tinting of the texture.
         */
        T tinting(@Nullable QuadTinting tinting);

        /**
         * Builds the data.
         */
        S build();
    }
}
