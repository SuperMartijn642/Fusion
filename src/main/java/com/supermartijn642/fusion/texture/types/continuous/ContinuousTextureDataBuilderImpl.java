package com.supermartijn642.fusion.texture.types.continuous;

import com.supermartijn642.fusion.api.texture.types.base.BaseTextureData;
import com.supermartijn642.fusion.api.texture.types.continuous.ContinuousTextureData;
import org.jetbrains.annotations.Nullable;

/**
 * Created 07/09/2024 by SuperMartijn642
 */
public class ContinuousTextureDataBuilderImpl implements ContinuousTextureData.Builder {

    private BaseTextureData.RenderType renderType;
    private boolean emissive = false;
    private BaseTextureData.QuadTinting tinting;
    private int rows = 1, columns = 1;

    @Override
    public ContinuousTextureDataBuilderImpl renderType(@Nullable BaseTextureData.RenderType renderType){
        this.renderType = renderType;
        return this;
    }

    @Override
    public ContinuousTextureDataBuilderImpl emissive(boolean emissive){
        this.emissive = emissive;
        return this;
    }

    @Override
    public ContinuousTextureDataBuilderImpl tinting(BaseTextureData.@Nullable QuadTinting tinting){
        this.tinting = tinting;
        return this;
    }

    @Override
    public ContinuousTextureData.Builder rows(int rows){
        this.rows = rows;
        return this;
    }

    @Override
    public ContinuousTextureData.Builder columns(int columns){
        this.columns = columns;
        return this;
    }

    @Override
    public ContinuousTextureData build(){
        return new ContinuousTextureDataImpl(this.renderType, this.emissive, this.tinting, this.rows, this.columns);
    }
}
