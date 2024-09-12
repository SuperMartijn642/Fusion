package com.supermartijn642.fusion.texture.types.connecting.layouts;

import com.supermartijn642.fusion.texture.types.connecting.TextureConnections;

/**
 * Created 08/09/2024 by SuperMartijn642
 */
public class VerticalLayoutHandler extends ConnectingTextureLayoutHandler.SimpleHandler {

    public VerticalLayoutHandler(){
        super(1, 4, 2);
    }

    @Override
    protected int connectionsIndex(TextureConnections connections){
        return (connections.top ? 1 : 0) | (connections.bottom ? 2 : 0);
    }

    @Override
    protected int[] getTilePos(TextureConnections connections){
        int[] uv;

        if(connections.top && connections.bottom) // both sides
            uv = new int[]{0, 2};
        else if(connections.top) // only up
            uv = new int[]{0, 3};
        else if(connections.bottom) // only down
            uv = new int[]{0, 1};
        else // none
            uv = new int[]{0, 0};

        return uv;
    }
}
