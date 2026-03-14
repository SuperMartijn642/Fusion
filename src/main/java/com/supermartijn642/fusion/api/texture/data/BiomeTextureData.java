package com.supermartijn642.fusion.api.texture.data;

import com.supermartijn642.fusion.texture.types.biome.BiomeTextureDataBuilderImpl;
import net.minecraft.resources.Identifier;

import java.util.Map;

public interface BiomeTextureData extends BaseTextureData {

    /**
     * Creates a builder for biome texture data.
     */
    static BiomeTextureData.Builder builder(){
        return new BiomeTextureDataBuilderImpl();
    }

    int getRows();

    int getColumns();

    /**
     * @return the default tile index to use when no biome matches.
     */
    int getDefaultTile();

    /**
     * @return map of biome identifiers to tile indices.
     */
    Map<Identifier,Integer> getBiomeTiles();

    interface Builder extends BaseTextureData.Builder<Builder,BiomeTextureData> {

        /**
         * Sets the number of rows of tiles in the image.
         */
        Builder rows(int rows);

        /**
         * Sets the number of columns of tiles in the image.
         */
        Builder columns(int columns);

        /**
         * Sets the default tile index.
         */
        Builder defaultTile(int tile);

        /**
         * Sets the tile index to use for the given biome.
         */
        Builder biomeTile(Identifier biome, int tile);
    }
}
