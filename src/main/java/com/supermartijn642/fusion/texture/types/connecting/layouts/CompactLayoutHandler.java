package com.supermartijn642.fusion.texture.types.connecting.layouts;

import com.supermartijn642.fusion.texture.types.connecting.TextureConnections;

/**
 * Created 08/09/2024 by SuperMartijn642
 */
public class CompactLayoutHandler extends ConnectingTextureLayoutHandler.SimpleHandler {

    public CompactLayoutHandler(){
        super(5, 1, 8);
    }

    @Override
    protected int connectionsIndex(TextureConnections connections){
        return (connections.top ? 1 : 0)
            | (connections.topRight ? 2 : 0)
            | (connections.right ? 4 : 0)
            | (connections.bottomRight ? 8 : 0)
            | (connections.bottom ? 16 : 0)
            | (connections.bottomLeft ? 32 : 0)
            | (connections.left ? 64 : 0)
            | (connections.topLeft ? 128 : 0);
    }

    @Override
    protected int[] getTilePos(TextureConnections connections){
        int[] uv = null;

        int sides = (connections.left ? 1 : 0) + (connections.top ? 1 : 0) + (connections.right ? 1 : 0) + (connections.bottom ? 1 : 0);
        if(sides == 0 || sides == 1) // 0 or 1 sides
            uv = new int[]{0, 0};
        else if(sides == 2){ // 2 sides
            if(connections.left && connections.right) // straight
                uv = new int[]{3, 0};
            else if(connections.top && connections.bottom) // straight
                uv = new int[]{2, 0};
            else // corner
                uv = new int[]{0, 0};
        }else if(sides == 3){ // 3 sides
            if(connections.left && connections.right){
                if((connections.topLeft && connections.top && connections.topRight) || (connections.bottomLeft && connections.bottom && connections.bottomRight))
                    uv = new int[]{3, 0};
                else
                    uv = new int[]{0, 0};
            }else if(connections.top && connections.bottom){
                if((connections.topLeft && connections.left && connections.bottomLeft) || (connections.topRight && connections.right && connections.bottomRight))
                    uv = new int[]{2, 0};
                else
                    uv = new int[]{0, 0};
            }
        }else if(sides == 4){ // 4 sides
            if(connections.topLeft && connections.topRight && connections.bottomLeft && connections.bottomRight)
                uv = new int[]{1, 0};
            else if(!connections.topLeft && !connections.topRight && !connections.bottomLeft && !connections.bottomRight)
                uv = new int[]{4, 0};
            else
                uv = new int[]{0, 0};
        }

        return uv;
    }
}
