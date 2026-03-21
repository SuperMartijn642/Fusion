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

    RandomnessSource getRandomSource();

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
         * Sets inputs that are used for the randomness.
         */
        Builder randomSource(RandomnessSource randomSource);

        /**
         * Seed to use for randomness.
         */
        Builder seed(Long seed);
    }

    enum RandomnessSource {
        /**
         * The randomness only uses the block position, meaning the tile picked will be the same for all sides at a given position.
         */
        POSITION,
        /**
         * The randomness uses the block position and facing of the quad, meaning the tile picked may be different for between sides at a given position.
         */
        POSITION_FACING,
        /**
         * The randomness uses the block position and axis of the quad's facing.
         */
        POSITION_AXIS
    }
}
