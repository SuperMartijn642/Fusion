package com.supermartijn642.fusion.texture.types.connecting.layouts;

import com.supermartijn642.fusion.texture.types.connecting.TextureConnections;

/**
 * Created 08/09/2024 by SuperMartijn642
 */
public class SimpleLayoutHandler extends ConnectingTextureLayoutHandler.SimpleHandler {

    public SimpleLayoutHandler(){
        super(4, 4, 4);
    }

    @Override
    protected int connectionsIndex(TextureConnections connections){
        return (connections.top ? 1 : 0)
            | (connections.right ? 2 : 0)
            | (connections.bottom ? 4 : 0)
            | (connections.left ? 8 : 0);
    }

    @Override
    protected int[] getTilePos(TextureConnections connections){
        int[] uv;

        if(!connections.left && !connections.top && !connections.right && !connections.bottom) // none
            uv = new int[]{0, 0};
        else{ // one direction
            if(connections.left && !connections.top && !connections.right && !connections.bottom)
                uv = new int[]{3, 0};
            else if(!connections.left && connections.top && !connections.right && !connections.bottom)
                uv = new int[]{3, 1};
            else if(!connections.left && !connections.top && connections.right && !connections.bottom)
                uv = new int[]{2, 1};
            else if(!connections.left && !connections.top && !connections.right && connections.bottom)
                uv = new int[]{2, 0};
            else{ // two directions
                if(connections.left && !connections.top && connections.right && !connections.bottom)
                    uv = new int[]{0, 1};
                else if(!connections.left && connections.top && !connections.right && connections.bottom)
                    uv = new int[]{1, 1};
                else if(connections.left && connections.top && !connections.right && !connections.bottom)
                    uv = new int[]{3, 3};
                else if(!connections.left && connections.top && connections.right && !connections.bottom)
                    uv = new int[]{2, 3};
                else if(!connections.left && !connections.top && connections.right && connections.bottom)
                    uv = new int[]{2, 2};
                else if(connections.left && !connections.top && !connections.right && connections.bottom)
                    uv = new int[]{3, 2};
                else{ // three directions
                    if(!connections.left)
                        uv = new int[]{0, 2};
                    else if(!connections.top)
                        uv = new int[]{1, 2};
                    else if(!connections.right)
                        uv = new int[]{1, 3};
                    else if(!connections.bottom)
                        uv = new int[]{0, 3};
                    else // four directions
                        uv = new int[]{1, 0};
                }
            }
        }

        return uv;
    }
}
