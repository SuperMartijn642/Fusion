package com.supermartijn642.fusion.texture.types.connecting;

import com.supermartijn642.fusion.api.texture.data.ConnectingTextureLayout;
import com.supermartijn642.fusion.model.types.connecting.SurroundingBlockData;

import java.util.function.Function;


/**
 * Created 25/10/2023 by SuperMartijn642
 */
public class ConnectingTextureLayoutHelper {

    private static final LayoutProperties[] LAYOUT_TO_PROPERTIES = {LayoutProperties.FULL, LayoutProperties.HORIZONTAL, LayoutProperties.SIMPLE, LayoutProperties.VERTICAL, LayoutProperties.COMPACT};

    public static int getWidth(ConnectingTextureLayout layout){
        return LAYOUT_TO_PROPERTIES[layout.ordinal()].width;
    }

    public static int getHeight(ConnectingTextureLayout layout){
        return LAYOUT_TO_PROPERTIES[layout.ordinal()].height;
    }

    public static int[] getStatePosition(ConnectingTextureLayout layout, SurroundingBlockData.SideConnections connections){
        return LAYOUT_TO_PROPERTIES[layout.ordinal()].tilePicker.apply(connections);
    }

    public static boolean shouldBeRotated(ConnectingTextureLayout layout){
        return getHeight(layout) > getWidth(layout);
    }

    private enum LayoutProperties {

        FULL(8, 8, connections -> {
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
        }),
        HORIZONTAL(4, 1, connections -> {
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
        }),
        SIMPLE(4, 4, connections -> {
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
        }),
        VERTICAL(1, 4, connections -> {
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
        }),
        COMPACT(5, 1, connections -> {
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
        });

        final int width, height;
        final Function<SurroundingBlockData.SideConnections,int[]> tilePicker;

        LayoutProperties(int width, int height, Function<SurroundingBlockData.SideConnections,int[]> tilePicker){
            this.width = width;
            this.height = height;
            this.tilePicker = tilePicker;
        }
    }
}
