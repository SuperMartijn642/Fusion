package com.supermartijn642.fusion.api.texture.types.connecting;

import com.supermartijn642.fusion.api.texture.DefaultTextureTypes;
import com.supermartijn642.fusion.api.texture.types.base.BaseTextureData;
import com.supermartijn642.fusion.api.texture.types.connecting.predicates.ConnectionPredicate;
import com.supermartijn642.fusion.api.texture.types.connecting.predicates.DefaultConnectionPredicates;
import com.supermartijn642.fusion.texture.types.connecting.ConnectingTextureDataBuilderImpl;
import org.jetbrains.annotations.Nullable;

/**
 * Data for connecting texture type.
 * <p>
 * Created 23/10/2023 by SuperMartijn642
 * @see DefaultTextureTypes#CONNECTING
 */
public interface ConnectingTextureData extends BaseTextureData {

    /**
     * Creates a builder for connecting texture data.
     */
    static Builder builder(){
        return new ConnectingTextureDataBuilderImpl();
    }

    /**
     * Layout of the connecting texture image.
     */
    Layout getLayout();

    /**
     * Predicate to determine whether the texture should connect in a certain direction.
     */
    @Nullable
    ConnectionPredicate getConnectionPredicate();

    interface Builder extends BaseTextureData.Builder<Builder,ConnectingTextureData> {
        /**
         * Sets the layout of the texture.
         * @see Layout
         */
        Builder layout(Layout layout);

        /**
         * Sets the predicate that determines when to connect in a direction.
         * @see DefaultConnectionPredicates
         */
        Builder connectionPredicate(@Nullable ConnectionPredicate predicate);
    }

    enum Layout {
        /**
         * Allows for connections with direct as well as diagonal neighbors.
         */
        FULL,
        /**
         * Allows for connections with horizontal neighbors.
         */
        HORIZONTAL,
        /**
         * Allows for connections only with direct neighbors.
         */
        SIMPLE,
        /**
         * Allows for connections with vertical neighbors.
         */
        VERTICAL,
        /**
         * Allows for reduced connections with direct neighbors.
         */
        COMPACT,
        /**
         * Allows for patterns with a border similar to the {@link #FULL} layout, but constructed from fewer tiles by piecing together corners from multiple of them.
         */
        PIECED,
        /**
         * Allows for making block overlays whilst using fewer tiles than the {@link #FULL} layout.
         */
        OVERLAY
    }
}
