package com.supermartijn642.fusion.texture.types.base;

import com.supermartijn642.fusion.api.texture.types.base.BaseTextureData;
import org.jetbrains.annotations.Nullable;

/**
 * Created 07/09/2024 by SuperMartijn642
 */
public class BaseTextureDataBuilderImpl implements BaseTextureData.Builder<BaseTextureDataBuilderImpl,BaseTextureData> {

    private BaseTextureData.RenderType renderType;
    private boolean emissive = false;
    private BaseTextureData.QuadTinting tinting;

    @Override
    public BaseTextureDataBuilderImpl renderType(@Nullable BaseTextureData.RenderType renderType){
        this.renderType = renderType;
        return this;
    }

    @Override
    public BaseTextureDataBuilderImpl emissive(boolean emissive){
        this.emissive = emissive;
        return this;
    }

    @Override
    public BaseTextureDataBuilderImpl tinting(BaseTextureData.@Nullable QuadTinting tinting){
        this.tinting = tinting;
        return this;
    }

    @Override
    public BaseTextureData build(){
        return new BaseTextureDataImpl(this.renderType, this.emissive, this.tinting);
    }
}
