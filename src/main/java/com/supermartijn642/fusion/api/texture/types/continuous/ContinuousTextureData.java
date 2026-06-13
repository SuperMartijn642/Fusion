package com.supermartijn642.fusion.api.texture.types.continuous;

import com.supermartijn642.fusion.api.texture.DefaultTextureTypes;
import com.supermartijn642.fusion.api.texture.types.base.BaseTextureData;
import com.supermartijn642.fusion.texture.types.continuous.ContinuousTextureDataBuilderImpl;
import org.jetbrains.annotations.ApiStatus;

/**
 * Data for continuous texture type.
 * <p>
 * Created 22/10/2024 by SuperMartijn642
 * @see DefaultTextureTypes#CONTINUOUS
 */
@ApiStatus.NonExtendable
public interface ContinuousTextureData extends BaseTextureData {

    /**
     * Creates a builder for continuous texture data.
     */
    static ContinuousTextureData.Builder builder(){
        return new ContinuousTextureDataBuilderImpl();
    }

    /**
     * Number of rows of tiles in the image.
     */
    int getRows();

    /**
     * Number of columns of tiles in the image.
     */
    int getColumns();

    @ApiStatus.NonExtendable
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
