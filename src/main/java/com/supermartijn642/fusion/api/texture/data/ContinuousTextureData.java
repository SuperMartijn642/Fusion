package com.supermartijn642.fusion.api.texture.data;

import com.supermartijn642.fusion.texture.types.continuous.ContinuousTextureDataBuilderImpl;

/**
 * Created 22/10/2024 by SuperMartijn642
 */
public interface ContinuousTextureData extends BaseTextureData {

    /**
     * Creates a builder for continuous texture data.
     */
    static ContinuousTextureData.Builder builder(){
        return new ContinuousTextureDataBuilderImpl();
    }

    int getRows();

    int getColumns();

    interface Builder extends BaseTextureData.Builder<Builder,ContinuousTextureData> {

        /**
         * Sets the number of rows of tiles in the image.
         */
        Builder rows(int rows);

        /**
         * Sets the number of columns of tiles in the image.
         */
        Builder columns(int columns);
    }
}
