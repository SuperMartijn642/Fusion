package com.supermartijn642.fusion.api.texture.data;

import com.supermartijn642.fusion.texture.types.connecting.ConnectingTextureDataBuilderImpl;

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
    }
}
