package com.supermartijn642.fusion.texture.types.random;

import com.supermartijn642.fusion.api.texture.types.base.BaseTextureData;
import com.supermartijn642.fusion.api.texture.types.random.RandomTextureData;
import org.jetbrains.annotations.Nullable;

/**
 * Created 07/09/2024 by SuperMartijn642
 */
public class RandomTextureDataBuilderImpl implements RandomTextureData.Builder {

    private BaseTextureData.RenderType renderType;
    private boolean emissive = false;
    private BaseTextureData.QuadTinting tinting;
    private int rows = 1, columns = 1;
    private RandomTextureData.RandomnessSource randomSource = RandomTextureData.RandomnessSource.POSITION_FACING;
    private Long seed;

    @Override
    public RandomTextureDataBuilderImpl renderType(@Nullable BaseTextureData.RenderType renderType){
        this.renderType = renderType;
        return this;
    }

    @Override
    public RandomTextureDataBuilderImpl emissive(boolean emissive){
        this.emissive = emissive;
        return this;
    }

    @Override
    public RandomTextureDataBuilderImpl tinting(@Nullable BaseTextureData.QuadTinting tinting){
        this.tinting = tinting;
        return this;
    }

    @Override
    public RandomTextureData.Builder rows(int rows){
        if(rows < 1 || rows > RandomTextureType.MAX_SIZE)
            throw new IllegalArgumentException("rows must be between 1 and 10");
        this.rows = rows;
        return this;
    }

    @Override
    public RandomTextureData.Builder columns(int columns){
        if(columns < 1 || columns > RandomTextureType.MAX_SIZE)
            throw new IllegalArgumentException("columns must be between 1 and 10");
        this.columns = columns;
        return this;
    }

    @Override
    public RandomTextureData.Builder randomSource(RandomTextureData.RandomnessSource randomSource){
        if(randomSource == null)
            throw new IllegalArgumentException("Randomness source must not be null!");
        this.randomSource = randomSource;
        return this;
    }

    @Override
    public RandomTextureData.Builder seed(Long seed){
        this.seed = seed;
        return this;
    }

    @Override
    public RandomTextureData build(){
        return new RandomTextureDataImpl(this.renderType, this.emissive, this.tinting, this.rows, this.columns, this.randomSource, this.seed);
    }
}
