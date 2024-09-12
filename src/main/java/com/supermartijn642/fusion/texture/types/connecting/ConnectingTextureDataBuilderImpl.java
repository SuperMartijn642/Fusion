package com.supermartijn642.fusion.texture.types.connecting;

import com.supermartijn642.fusion.api.texture.data.BaseTextureData;
import com.supermartijn642.fusion.api.texture.data.ConnectingTextureData;
import com.supermartijn642.fusion.api.texture.data.ConnectingTextureLayout;
import org.jetbrains.annotations.Nullable;

/**
 * Created 07/09/2024 by SuperMartijn642
 */
public class ConnectingTextureDataBuilderImpl implements ConnectingTextureData.Builder {

    private BaseTextureData.RenderType renderType;
    private boolean emissive = false;
    private BaseTextureData.QuadTinting tinting;
    private ConnectingTextureLayout layout = ConnectingTextureLayout.FULL;

    @Override
    public ConnectingTextureDataBuilderImpl renderType(@Nullable BaseTextureData.RenderType renderType){
        this.renderType = renderType;
        return this;
    }

    @Override
    public ConnectingTextureDataBuilderImpl emissive(boolean emissive){
        this.emissive = emissive;
        return this;
    }

    @Override
    public ConnectingTextureDataBuilderImpl tinting(BaseTextureData.@Nullable QuadTinting tinting){
        this.tinting = tinting;
        return this;
    }

    @Override
    public ConnectingTextureData.Builder layout(ConnectingTextureLayout layout){
        this.layout = layout;
        return this;
    }

    @Override
    public ConnectingTextureData.Builder renderType(@Nullable ConnectingTextureData.RenderType type){
        if(type == null)
            return this.renderType((BaseTextureData.RenderType)null);
        switch(type){
            case OPAQUE:
                return this.renderType(BaseTextureData.RenderType.OPAQUE);
            case CUTOUT:
                return this.renderType(BaseTextureData.RenderType.CUTOUT);
            case TRANSLUCENT:
                return this.renderType(BaseTextureData.RenderType.TRANSLUCENT);
        }
        return this;
    }

    @Override
    public ConnectingTextureData build(){
        return new ConnectingTextureDataImpl(this.renderType, this.emissive, this.tinting, this.layout);
    }
}
