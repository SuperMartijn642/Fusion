package com.supermartijn642.fusion.texture.types.connecting;

import com.supermartijn642.fusion.api.texture.custom.TextureInstance;
import com.supermartijn642.fusion.api.texture.types.connecting.ConnectingTextureData;

import java.util.List;

/**
 * Created 23/03/2026 by SuperMartijn642
 */
public class StitchedConnectingTextureData extends ConnectingTextureDataImpl {

    private final List<TextureInstance<?>> tiles;

    public StitchedConnectingTextureData(ConnectingTextureData base, List<TextureInstance<?>> tiles){
        super(base.getRenderType(), base.isEmissive(), base.getTinting(), base.getLayout(), base.getConnectionPredicate() == null ? null : base.getConnectionPredicate().simplify(), null, false);
        this.tiles = tiles;
    }

    public List<TextureInstance<?>> getTiles(){
        return this.tiles;
    }
}
