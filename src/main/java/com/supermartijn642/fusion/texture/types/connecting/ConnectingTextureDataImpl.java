package com.supermartijn642.fusion.texture.types.connecting;

import com.supermartijn642.fusion.api.texture.data.ConnectingTextureData;
import com.supermartijn642.fusion.api.texture.data.ConnectingTextureLayout;
import org.jetbrains.annotations.Nullable;

/**
 * Created 23/10/2023 by SuperMartijn642
 */
public class ConnectingTextureDataImpl implements ConnectingTextureData {

    private final ConnectingTextureLayout layout;
    private final RenderType renderType;

    public ConnectingTextureDataImpl(ConnectingTextureLayout layout, RenderType renderType){
        this.layout = layout;
        this.renderType = renderType;
    }

    @Override
    public ConnectingTextureLayout getLayout(){
        return this.layout;
    }

    @Override
    public @Nullable RenderType getRenderType(){
        return this.renderType;
    }
}
