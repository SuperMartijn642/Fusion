package com.supermartijn642.fusion.api.texture.data;

import com.supermartijn642.fusion.api.texture.DefaultTextureTypes;
import com.supermartijn642.fusion.texture.types.scrolling.ScrollingTextureDataImpl;

/**
 * Stores data for the {@link DefaultTextureTypes#SCROLLING} texture type.
 * An instance can be created using the builder provided by {@link ScrollingTextureData#builder()}.
 * <p>
 * Created 28/04/2023 by SuperMartijn642
 */
public interface ScrollingTextureData {

    /**
     * Creates a builder for scrolling texture data.
     */
    static Builder builder(){
        return new Builder();
    }

    Position getStartPosition();

    Position getEndPosition();

    int getFrameTime();

    int getFrameWidth();

    int getFrameHeight();

    LoopType getLoopType();

    int getLoopPause();

    final class Builder {

        private Position startPosition = Position.TOP_LEFT, endPosition = Position.BOTTOM_LEFT;
        private int frameTime = 10, frameWidth = 16, frameHeight = 16;
        private LoopType loopType = LoopType.RESET;
        private int loopPause = 0;

        private Builder(){
        }

        /**
         * Sets the position which the frame should start at. By default, this will be the top left corner.
         */
        public Builder startPosition(Position position){
            this.startPosition = position;
            return this;
        }

        /**
         * Sets the position which the frame should end at. By default, this will be the bottom left corner.
         */
        public Builder endPosition(Position position){
            this.endPosition = position;
            return this;
        }

        /**
         * Sets the duration in ticks each frame is displayed for. By default, this will be 10 ticks.
         */
        public Builder frameTime(int ticks){
            this.frameTime = ticks;
            return this;
        }

        /**
         * Sets the width of the frame. The width must be smaller than the width of the texture. By default, this will be 16 pixels.
         */
        public Builder frameWidth(int width){
            this.frameWidth = width;
            return this;
        }

        /**
         * Sets the height of the frame. The height must be smaller than the height of the texture. By default, this will be 16 pixels.
         */
        public Builder frameHeight(int height){
            this.frameHeight = height;
            return this;
        }

        /**
         * Sets the size of the frame. The size must be smaller than the size of the texture. By default, this will be 16 by 16 pixels.
         */
        public Builder frameSize(int width, int height){
            this.frameWidth = width;
            this.frameHeight = height;
            return this;
        }

        /**
         * Sets the loop type. By default, this will be set to {@link LoopType#RESET}.
         * @see LoopType
         */
        public Builder loopType(LoopType type){
            this.loopType = type;
            return this;
        }

        /**
         * Sets the pause duration in ticks after each iteration of the loop. By default, this will be set to 0.
         * @see LoopType
         */
        public Builder loopPause(int ticks){
            this.loopPause = ticks;
            return this;
        }

        public ScrollingTextureData build(){
            return new ScrollingTextureDataImpl(this.startPosition, this.endPosition, this.frameTime, this.frameWidth, this.frameHeight, this.loopType, this.loopPause);
        }
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
