package com.supermartijn642.fusion.api.texture.data;

import com.supermartijn642.fusion.api.texture.DefaultTextureTypes;
import com.supermartijn642.fusion.texture.types.scrolling.ScrollingTextureDataBuilderImpl;

/**
 * Stores data for the {@link DefaultTextureTypes#SCROLLING} texture type.
 * An instance can be created using the builder provided by {@link ScrollingTextureData#builder()}.
 * <p>
 * Created 28/04/2023 by SuperMartijn642
 */
public interface ScrollingTextureData extends BaseTextureData {

    /**
     * Creates a builder for scrolling texture data.
     */
    static Builder builder(){
        return new ScrollingTextureDataBuilderImpl();
    }

    Position getStartPosition();

    Position getEndPosition();

    int getFrameTime();

    int getFrameWidth();

    int getFrameHeight();

    LoopType getLoopType();

    int getLoopPause();

    interface Builder extends BaseTextureData.Builder<Builder,ScrollingTextureData> {

        /**
         * Sets the position which the frame should start at. By default, this will be the top left corner.
         */
        Builder startPosition(Position position);

        /**
         * Sets the position which the frame should end at. By default, this will be the bottom left corner.
         */
        Builder endPosition(Position position);

        /**
         * Sets the duration in ticks each frame is displayed for. By default, this will be 10 ticks.
         */
        Builder frameTime(int ticks);

        /**
         * Sets the width of the frame. The width must be smaller than the width of the texture. By default, this will be 16 pixels.
         */
        Builder frameWidth(int width);

        /**
         * Sets the height of the frame. The height must be smaller than the height of the texture. By default, this will be 16 pixels.
         */
        Builder frameHeight(int height);

        /**
         * Sets the size of the frame. The size must be smaller than the size of the texture. By default, this will be 16 by 16 pixels.
         */
        Builder frameSize(int width, int height);

        /**
         * Sets the loop type. By default, this will be set to {@link LoopType#RESET}.
         * @see LoopType
         */
        Builder loopType(LoopType type);

        /**
         * Sets the pause duration in ticks after each iteration of the loop. By default, this will be set to 0.
         * @see LoopType
         */
        Builder loopPause(int ticks);
    }

    enum Position {
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
    }

    enum LoopType {
        /**
         * After an iteration is complete, the frame will reset to the starting position.
         */
        RESET,
        /**
         * The scrolling will go back in reserve order to the starting position.
         */
        REVERSE
    }
}
