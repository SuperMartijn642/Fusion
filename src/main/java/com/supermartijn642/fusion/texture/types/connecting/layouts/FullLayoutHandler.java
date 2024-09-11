package com.supermartijn642.fusion.texture.types.connecting.layouts;

import com.supermartijn642.fusion.texture.types.connecting.TextureConnections;

/**
 * Created 08/09/2024 by SuperMartijn642
 */
public class FullLayoutHandler extends ConnectingTextureLayoutHandler.SimpleHandler {

    public FullLayoutHandler(){
        super(8, 8, 8);
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
        int[] uv;

        if(!connections.left && !connections.top && !connections.right && !connections.bottom) // all directions
            uv = new int[]{0, 0};
        else{ // one direction
            if(connections.left && !connections.top && !connections.right && !connections.bottom)
                uv = new int[]{3, 0};
            else if(!connections.left && connections.top && !connections.right && !connections.bottom)
                uv = new int[]{0, 3};
            else if(!connections.left && !connections.top && connections.right && !connections.bottom)
                uv = new int[]{1, 0};
            else if(!connections.left && !connections.top && !connections.right && connections.bottom)
                uv = new int[]{0, 1};
            else{ // two directions
                if(connections.left && !connections.top && connections.right && !connections.bottom)
                    uv = new int[]{2, 0};
                else if(!connections.left && connections.top && !connections.right && connections.bottom)
                    uv = new int[]{0, 2};
                else if(connections.left && connections.top && !connections.right && !connections.bottom){
                    if(connections.topLeft)
                        uv = new int[]{3, 3};
                    else
                        uv = new int[]{5, 1};
                }else if(!connections.left && connections.top && connections.right && !connections.bottom){
                    if(connections.topRight)
                        uv = new int[]{1, 3};
                    else
                        uv = new int[]{4, 1};
                }else if(!connections.left && !connections.top && connections.right && connections.bottom){
                    if(connections.bottomRight)
                        uv = new int[]{1, 1};
                    else
                        uv = new int[]{4, 0};
                }else if(connections.left && !connections.top && !connections.right && connections.bottom){
                    if(connections.bottomLeft)
                        uv = new int[]{3, 1};
                    else
                        uv = new int[]{5, 0};
                }else{ // three directions
                    if(!connections.left){
                        if(connections.topRight && connections.bottomRight)
                            uv = new int[]{1, 2};
                        else if(connections.topRight)
                            uv = new int[]{4, 2};
                        else if(connections.bottomRight)
                            uv = new int[]{6, 2};
                        else
                            uv = new int[]{6, 0};
                    }else if(!connections.top){
                        if(connections.bottomLeft && connections.bottomRight)
                            uv = new int[]{2, 1};
                        else if(connections.bottomLeft)
                            uv = new int[]{7, 2};
                        else if(connections.bottomRight)
                            uv = new int[]{5, 2};
                        else
                            uv = new int[]{7, 0};
                    }else if(!connections.right){
                        if(connections.topLeft && connections.bottomLeft)
                            uv = new int[]{3, 2};
                        else if(connections.topLeft)
                            uv = new int[]{7, 3};
                        else if(connections.bottomLeft)
                            uv = new int[]{5, 3};
                        else
                            uv = new int[]{7, 1};
                    }else if(!connections.bottom){
                        if(connections.topLeft && connections.topRight)
                            uv = new int[]{2, 3};
                        else if(connections.topLeft)
                            uv = new int[]{4, 3};
                        else if(connections.topRight)
                            uv = new int[]{6, 3};
                        else
                            uv = new int[]{6, 1};
                    }else{ // four directions
                        if(connections.topLeft && connections.topRight && connections.bottomLeft && connections.bottomRight)
                            uv = new int[]{2, 2};
                        else{
                            if(!connections.topLeft && connections.topRight && connections.bottomLeft && connections.bottomRight)
                                uv = new int[]{7, 5};
                            else if(connections.topLeft && !connections.topRight && connections.bottomLeft && connections.bottomRight)
                                uv = new int[]{6, 5};
                            else if(connections.topLeft && connections.topRight && !connections.bottomLeft && connections.bottomRight)
                                uv = new int[]{7, 4};
                            else if(connections.topLeft && connections.topRight && connections.bottomLeft && !connections.bottomRight)
                                uv = new int[]{6, 4};
                            else{
                                if(!connections.topLeft && connections.topRight && !connections.bottomRight && connections.bottomLeft)
                                    uv = new int[]{0, 4};
                                else if(connections.topLeft && !connections.topRight && connections.bottomRight && !connections.bottomLeft)
                                    uv = new int[]{0, 5};
                                else if(!connections.topLeft && !connections.topRight && connections.bottomRight && connections.bottomLeft)
                                    uv = new int[]{3, 4};
                                else if(connections.topLeft && !connections.topRight && !connections.bottomRight && connections.bottomLeft)
                                    uv = new int[]{3, 5};
                                else if(connections.topLeft && connections.topRight && !connections.bottomRight && !connections.bottomLeft)
                                    uv = new int[]{2, 5};
                                else if(!connections.topLeft && connections.topRight && connections.bottomRight && !connections.bottomLeft)
                                    uv = new int[]{2, 4};
                                else{
                                    if(connections.topLeft)
                                        uv = new int[]{5, 5};
                                    else if(connections.topRight)
                                        uv = new int[]{4, 5};
                                    else if(connections.bottomRight)
                                        uv = new int[]{4, 4};
                                    else if(connections.bottomLeft)
                                        uv = new int[]{5, 4};
                                    else
                                        uv = new int[]{1, 4};
                                }
                            }
                        }
                    }
                }
            }
        }

        return uv;
    }
}
