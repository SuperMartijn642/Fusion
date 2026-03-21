package com.supermartijn642.fusion.api.texture.data;

import com.supermartijn642.fusion.texture.types.random.RandomTextureDataBuilderImpl;
import org.jetbrains.annotations.Nullable;

/**
 * Created 22/10/2024 by SuperMartijn642
 */
public interface RandomTextureData extends BaseTextureData {

    /**
     * Creates a builder for random texture data.
     */
    static RandomTextureData.Builder builder(){
        return new RandomTextureDataBuilderImpl();
    }

    int getRows();

    int getColumns();

    @Nullable
    Long getSeed();

    interface Builder extends BaseTextureData.Builder<Builder,RandomTextureData> {

        /**
         * Sets the number of rows of tiles in the image.
         */
        Builder rows(int rows);

        /**
         * Sets the number of columns of tiles in the image.
         */
        Builder columns(int columns);

        /**
         * Seed to use for randomness.
         */
        Builder seed(Long seed);
    }
}
