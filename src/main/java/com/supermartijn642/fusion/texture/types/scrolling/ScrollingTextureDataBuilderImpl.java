package com.supermartijn642.fusion.texture.types.scrolling;

import com.supermartijn642.fusion.api.texture.types.base.BaseTextureData;
import com.supermartijn642.fusion.api.texture.types.scrolling.ScrollingTextureData;
import org.jetbrains.annotations.Nullable;

/**
 * Created 08/09/2024 by SuperMartijn642
 */
public class ScrollingTextureDataBuilderImpl implements ScrollingTextureData.Builder {

    private BaseTextureData.RenderType renderType;
    private boolean emissive = false;
    private BaseTextureData.QuadTinting tinting;
    private ScrollingTextureData.Position startPosition = ScrollingTextureData.Position.TOP_LEFT, endPosition = ScrollingTextureData.Position.BOTTOM_LEFT;
    private int frameTime = 10, frameWidth = 16, frameHeight = 16;
    private ScrollingTextureData.LoopType loopType = ScrollingTextureData.LoopType.RESET;
    private int loopPause = 0;

    @Override
    public ScrollingTextureDataBuilderImpl renderType(@Nullable BaseTextureData.RenderType renderType){
        this.renderType = renderType;
        return this;
    }

    @Override
    public ScrollingTextureDataBuilderImpl emissive(boolean emissive){
        this.emissive = emissive;
        return this;
    }

    @Override
    public ScrollingTextureDataBuilderImpl tinting(BaseTextureData.@Nullable QuadTinting tinting){
        this.tinting = tinting;
        return this;
    }

    /**
     * Sets the position which the frame should start at. By default, this will be the top left corner.
     */
    public ScrollingTextureDataBuilderImpl startPosition(ScrollingTextureData.Position position){
        this.startPosition = position;
        return this;
    }

    /**
     * Sets the position which the frame should end at. By default, this will be the bottom left corner.
     */
    public ScrollingTextureDataBuilderImpl endPosition(ScrollingTextureData.Position position){
        this.endPosition = position;
        return this;
    }

    /**
     * Sets the duration in ticks each frame is displayed for. By default, this will be 10 ticks.
     */
    public ScrollingTextureDataBuilderImpl frameTime(int ticks){
        this.frameTime = ticks;
        return this;
    }

    /**
     * Sets the width of the frame. The width must be smaller than the width of the texture. By default, this will be 16 pixels.
     */
    public ScrollingTextureDataBuilderImpl frameWidth(int width){
        this.frameWidth = width;
        return this;
    }

    /**
     * Sets the height of the frame. The height must be smaller than the height of the texture. By default, this will be 16 pixels.
     */
    public ScrollingTextureDataBuilderImpl frameHeight(int height){
        this.frameHeight = height;
        return this;
    }

    /**
     * Sets the size of the frame. The size must be smaller than the size of the texture. By default, this will be 16 by 16 pixels.
     */
    public ScrollingTextureDataBuilderImpl frameSize(int width, int height){
        this.frameWidth = width;
        this.frameHeight = height;
        return this;
    }

    /**
     * Sets the loop type. By default, this will be set to {@link ScrollingTextureData.LoopType#RESET}.
     * @see ScrollingTextureData.LoopType
     */
    public ScrollingTextureDataBuilderImpl loopType(ScrollingTextureData.LoopType type){
        this.loopType = type;
        return this;
    }

    /**
     * Sets the pause duration in ticks after each iteration of the loop. By default, this will be set to 0.
     * @see ScrollingTextureData.LoopType
     */
    public ScrollingTextureDataBuilderImpl loopPause(int ticks){
        this.loopPause = ticks;
        return this;
    }

    public ScrollingTextureData build(){
        return new ScrollingTextureDataImpl(
            this.renderType, this.emissive, this.tinting,
            this.startPosition, this.endPosition, this.frameTime, this.frameWidth, this.frameHeight, this.loopType, this.loopPause
        );
    }
}
