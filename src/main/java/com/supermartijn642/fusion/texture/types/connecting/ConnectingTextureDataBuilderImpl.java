package com.supermartijn642.fusion.texture.types.connecting;

import com.supermartijn642.fusion.api.texture.data.BaseTextureData;
import com.supermartijn642.fusion.api.texture.data.ConnectingTextureData;
import com.supermartijn642.fusion.api.texture.data.ConnectingTextureLayout;
import org.jetbrains.annotations.Nullable;

/**
 * Created 07/09/2024 by SuperMartijn642
 */
public class ConnectingTextureDataBuilderImpl implements ConnectingTextureData.Builder {

    private boolean emissive = false;
    private BaseTextureData.QuadTinting tinting;
    private ConnectingTextureLayout layout = ConnectingTextureLayout.FULL;

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
    public ConnectingTextureData build(){
        return new ConnectingTextureDataImpl(this.emissive, this.tinting, this.layout);
    }
}
