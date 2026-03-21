package com.supermartijn642.fusion.texture.types.connecting;

import com.supermartijn642.fusion.api.texture.custom.SpriteInstance;
import com.supermartijn642.fusion.api.texture.data.ConnectingTextureData;

import java.util.List;

/**
 * Created 23/03/2026 by SuperMartijn642
 */
public class StitchedConnectingTextureData extends ConnectingTextureDataImpl {

    private final List<SpriteInstance> tiles;

    public StitchedConnectingTextureData(ConnectingTextureData base, List<SpriteInstance> tiles){
        super(base.getRenderType(), base.isEmissive(), base.getTinting(), base.getLayout());
        this.tiles = tiles;
    }

    public List<SpriteInstance> getTiles(){
        return this.tiles;
    }
}
