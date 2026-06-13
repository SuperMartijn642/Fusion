package com.supermartijn642.fusion.api.texture.types.random;

import com.supermartijn642.fusion.api.texture.DefaultTextureTypes;
import com.supermartijn642.fusion.api.texture.RawTextureInstance;
import com.supermartijn642.fusion.api.texture.types.base.BaseTextureData;
import com.supermartijn642.fusion.texture.types.random.RandomTextureDataBuilderImpl;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * Data for random texture type.
 * <p>
 * Created 22/10/2024 by SuperMartijn642
 * @see DefaultTextureTypes#RANDOM
 */
@ApiStatus.NonExtendable
public interface RandomTextureData extends BaseTextureData {

    /**
     * Creates a builder for random texture data.
     */
    static RandomTextureData.Builder builder(){
        return new RandomTextureDataBuilderImpl();
    }

    /**
     * Number of rows of tiles in the image.
     */
    int getRows();

    /**
     * Number of columns of tiles in the image.
     */
    int getColumns();

    /**
     * Inputs that are used for the randomness.
     */
    RandomnessSource getRandomSource();

    /**
     * Seed to use for randomness.
     */
    @Nullable
    Long getSeed();

    /**
     * Texture type and data used for each of the tiles.
     */
    @Nullable
    RawTextureInstance<?,?> subTexture();

    /**
     * Whether the animation covers the entire texture or each tile should be handled as its own animation.
     */
    boolean perTileAnimation();

    @ApiStatus.NonExtendable
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
        Builder seed(@Nullable Long seed);

        /**
         * Sets texture type and data to be used for each of the tiles.
         */
        Builder subTexture(@Nullable RawTextureInstance<?,?> subTextureType);

        /**
         * Sets whether the animation covers the entire texture or each tile should be handled as its own animation.
         */
        Builder perTileAnimation(boolean perTileAnimation);
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
