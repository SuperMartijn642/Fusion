package com.supermartijn642.fusion.texture.types.biome;

import com.supermartijn642.fusion.api.texture.data.BaseTextureData;
import com.supermartijn642.fusion.api.texture.data.BiomeTextureData;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class BiomeTextureDataBuilderImpl implements BiomeTextureData.Builder {

    private BaseTextureData.RenderType renderType;
    private boolean emissive = false;
    private BaseTextureData.QuadTinting tinting;
    private int rows = 1, columns = 1;
    private int defaultTile = 0;
    private final Map<Identifier,Integer> biomeTiles = new HashMap<>();

    @Override
    public BiomeTextureDataBuilderImpl renderType(@Nullable BaseTextureData.RenderType renderType){
        this.renderType = renderType;
        return this;
    }

    @Override
    public BiomeTextureDataBuilderImpl emissive(boolean emissive){
        this.emissive = emissive;
        return this;
    }

    @Override
    public BiomeTextureDataBuilderImpl tinting(BaseTextureData.@Nullable QuadTinting tinting){
        this.tinting = tinting;
        return this;
    }

    @Override
    public BiomeTextureData.Builder rows(int rows){
        if(rows < 1 || rows > 10)
            throw new IllegalArgumentException("rows must be between 1 and 10");
        this.rows = rows;
        return this;
    }

    @Override
    public BiomeTextureData.Builder columns(int columns){
        if(columns < 1 || columns > 10)
            throw new IllegalArgumentException("columns must be between 1 and 10");
        this.columns = columns;
        return this;
    }

    @Override
    public BiomeTextureData.Builder defaultTile(int tile){
        if(tile < 0)
            throw new IllegalArgumentException("default tile must be non-negative");
        this.defaultTile = tile;
        return this;
    }

    @Override
    public BiomeTextureData.Builder biomeTile(Identifier biome, int tile){
        if(biome == null)
            throw new IllegalArgumentException("biome cannot be null");
        if(tile < 0)
            throw new IllegalArgumentException("tile must be non-negative");
        this.biomeTiles.put(biome, tile);
        return this;
    }

    @Override
    public BiomeTextureData build(){
        int max = this.rows * this.columns;
        if(this.defaultTile >= max)
            throw new IllegalArgumentException("default tile cannot be greater than or equal to rows * columns!");
        for(Map.Entry<Identifier,Integer> entry : this.biomeTiles.entrySet()){
            if(entry.getValue() >= max)
                throw new IllegalArgumentException("tile index for biome '" + entry.getKey() + "' cannot be greater than or equal to rows * columns!");
        }
        return new BiomeTextureDataImpl(this.renderType, this.emissive, this.tinting, this.rows, this.columns, this.defaultTile, this.biomeTiles);
    }
}
