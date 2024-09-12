package com.supermartijn642.fusion.texture.types.connecting.layouts;

import com.supermartijn642.fusion.texture.types.connecting.TextureConnections;

/**
 * Created 08/09/2024 by SuperMartijn642
 */
public class HorizontalLayoutHandler extends ConnectingTextureLayoutHandler.SimpleHandler {

    public HorizontalLayoutHandler(){
        super(4, 1, 2);
    }

    @Override
    protected int connectionsIndex(TextureConnections connections){
        return (connections.left ? 1 : 0) | (connections.right ? 2 : 0);
    }

    @Override
    protected int[] getTilePos(TextureConnections connections){
        int[] uv;

        if(connections.left && connections.right) // both sides
            uv = new int[]{2, 0};
        else if(connections.left) // only left
            uv = new int[]{3, 0};
        else if(connections.right) // only right
            uv = new int[]{1, 0};
        else // none
            uv = new int[]{0, 0};

        return uv;
    }
}
