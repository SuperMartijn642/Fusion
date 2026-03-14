package com.supermartijn642.fusion.texture.types.biome;

import com.supermartijn642.fusion.api.texture.data.BiomeTextureData;
import com.supermartijn642.fusion.texture.types.base.BaseTextureDataImpl;
import net.minecraft.resources.Identifier;

import java.util.Collections;
import java.util.Map;

public class BiomeTextureDataImpl extends BaseTextureDataImpl implements BiomeTextureData {

    private final int rows, columns;
    private final int defaultTile;
    private final Map<Identifier,Integer> biomeTiles;

    public BiomeTextureDataImpl(RenderType renderType, boolean emissive, QuadTinting tinting, int rows, int columns, int defaultTile, Map<Identifier,Integer> biomeTiles){
        super(renderType, emissive, tinting);
        this.rows = rows;
        this.columns = columns;
        this.defaultTile = defaultTile;
        this.biomeTiles = Collections.unmodifiableMap(biomeTiles);
    }

    @Override
    public int getRows(){
        return this.rows;
    }

    @Override
    public int getColumns(){
        return this.columns;
    }

    @Override
    public int getDefaultTile(){
        return this.defaultTile;
    }

    @Override
    public Map<Identifier,Integer> getBiomeTiles(){
        return this.biomeTiles;
    }
}
