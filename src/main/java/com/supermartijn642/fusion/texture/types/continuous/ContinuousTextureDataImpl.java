package com.supermartijn642.fusion.texture.types.continuous;

import com.supermartijn642.fusion.api.texture.types.continuous.ContinuousTextureData;
import com.supermartijn642.fusion.texture.types.base.BaseTextureDataImpl;

/**
 * Created 22/10/2024 by SuperMartijn642
 */
public class ContinuousTextureDataImpl extends BaseTextureDataImpl implements ContinuousTextureData {

    private final int rows, columns;

    public ContinuousTextureDataImpl(RenderType renderType, boolean emissive, QuadTinting tinting, int rows, int columns){
        super(renderType, emissive, tinting);
        this.rows = rows;
        this.columns = columns;
    }

    @Override
    public int getRows(){
        return this.rows;
    }

    @Override
    public int getColumns(){
        return this.columns;
    }
}
