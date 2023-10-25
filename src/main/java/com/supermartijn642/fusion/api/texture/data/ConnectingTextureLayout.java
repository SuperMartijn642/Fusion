package com.supermartijn642.fusion.api.texture.data;

/**
 * Created 28/04/2023 by SuperMartijn642
 */
public enum ConnectingTextureLayout {

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
    COMPACT
}
