package com.supermartijn642.fusion.api.texture.data;

import com.supermartijn642.fusion.texture.types.base.BaseTextureDataBuilderImpl;
import org.jetbrains.annotations.Nullable;

/**
 * Created 06/09/2024 by SuperMartijn642
 */
public interface BaseTextureData {

    static Builder<?,BaseTextureData> builder(){
        return new BaseTextureDataBuilderImpl();
    }

    boolean isEmissive();

    QuadTinting getTinting();

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
        T emissive(boolean emissive);

        T tinting(@Nullable QuadTinting tinting);

        S build();
    }
}
