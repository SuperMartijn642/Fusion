package com.supermartijn642.fusion.api.texture.data;

import com.supermartijn642.fusion.texture.types.connecting.ConnectingTextureDataBuilderImpl;

import javax.annotation.Nullable;

/**
 * Created 23/10/2023 by SuperMartijn642
 */
public interface ConnectingTextureData extends BaseTextureData {

    /**
     * Creates a builder for connecting texture data.
     */
    static Builder builder(){
        return new ConnectingTextureDataBuilderImpl();
    }

    ConnectingTextureLayout getLayout();

    interface Builder extends BaseTextureData.Builder<Builder,ConnectingTextureData> {
        /**
         * Sets the layout of the texture.
         * @see ConnectingTextureLayout
         */
        Builder layout(ConnectingTextureLayout layout);

        /**
         * @deprecated use {@link #renderType(BaseTextureData.RenderType)}
         */
        @Deprecated
        Builder renderType(@Nullable RenderType type);
    }

    @Deprecated
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
}
