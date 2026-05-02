package com.supermartijn642.fusion.texture.types.connecting;

import com.supermartijn642.fusion.api.texture.data.ConnectingTextureData;
import com.supermartijn642.fusion.api.texture.data.ConnectingTextureLayout;
import com.supermartijn642.fusion.texture.types.base.BaseTextureDataImpl;

/**
 * Created 23/10/2023 by SuperMartijn642
 */
public class ConnectingTextureDataImpl extends BaseTextureDataImpl implements ConnectingTextureData {

    private final ConnectingTextureLayout layout;

    public ConnectingTextureDataImpl(boolean emissive, QuadTinting tinting, ConnectingTextureLayout layout){
        super(emissive, tinting);
        this.layout = layout;
    }

    @Override
    public ConnectingTextureLayout getLayout(){
        return this.layout;
    }
}
