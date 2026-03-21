package com.supermartijn642.fusion.model.types.connecting;

import com.supermartijn642.fusion.api.predicate.ConnectionPredicate;
import com.supermartijn642.fusion.api.texture.data.ConnectingTextureLayout;
import com.supermartijn642.fusion.model.types.base.BaseModelQuad;
import com.supermartijn642.fusion.texture.types.connecting.StitchedConnectingTextureData;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.util.Direction;

/**
 * Created 07/09/2024 by SuperMartijn642
 */
public class ConnectingModelQuad extends BaseModelQuad {

    private final ConnectionPredicate predicate;
    private final ConnectingTextureLayout layout;

    public ConnectingModelQuad(BakedQuad bakedQuad, Direction cullDirection, Integer lightEmission, ConnectionPredicate connectionPredicate){
        super(bakedQuad, cullDirection, lightEmission);
        if(this.spriteInstance() != null && this.spriteInstance().getTexture().getCustomData() instanceof StitchedConnectingTextureData){
            StitchedConnectingTextureData data = (StitchedConnectingTextureData)this.spriteInstance().getTexture().getCustomData();
            this.predicate = connectionPredicate;
            this.layout = data.getLayout();
        }else{
            this.predicate = null;
            this.layout = null;
        }
    }

    public ConnectionPredicate connectionPredicate(){
        return this.predicate;
    }

    public ConnectingTextureLayout getLayout(){
        return this.layout;
    }

    public boolean hasConnectingTexture(){
        return this.layout != null;
    }
}
