package com.supermartijn642.fusion.api.texture.types.scrolling;

import com.supermartijn642.fusion.api.texture.DefaultTextureTypes;
import com.supermartijn642.fusion.api.texture.types.base.BaseTextureData;
import com.supermartijn642.fusion.texture.types.scrolling.ScrollingTextureDataBuilderImpl;
import org.jetbrains.annotations.ApiStatus;

/**
 * Stores data for the {@link DefaultTextureTypes#SCROLLING} texture type.
 * An instance can be created using the builder provided by {@link ScrollingTextureData#builder()}.
 * <p>
 * Created 28/04/2023 by SuperMartijn642
 * @see DefaultTextureTypes#SCROLLING
 */
@ApiStatus.NonExtendable
public interface ScrollingTextureData extends BaseTextureData {

    /**
     * Creates a builder for scrolling texture data.
     */
    static Builder builder(){
        return new ScrollingTextureDataBuilderImpl();
    }

    /**
     * Position which the frame should start at.
     */
    Position getStartPosition();

    /**
     * Position which the frame should end at.
     */
    Position getEndPosition();

    /**
     * Duration in ticks each frame is displayed for.
     */
    int getFrameTime();

    /**
     * Width of the frame.
     */
    int getFrameWidth();

    /**
     * Height of the frame.
     */
    int getFrameHeight();

    /**
     * How the scrolling loops when the end position is reached.
     */
    LoopType getLoopType();

    /**
     * Pause duration in ticks after each iteration of the loop.
     */
    int getLoopPause();

    @ApiStatus.NonExtendable
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
         * Sets the way the scrolling loops when the end position is reached. By default, this will be set to {@link LoopType#RESET}.
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
         * The frame simply wraps around the image.
         */
        WRAP,
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
