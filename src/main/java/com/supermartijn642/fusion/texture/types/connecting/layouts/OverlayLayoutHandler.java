package com.supermartijn642.fusion.texture.types.connecting.layouts;

import com.supermartijn642.fusion.api.model.custom.quad.MutableQuad;
import com.supermartijn642.fusion.api.texture.custom.SpriteInstance;
import com.supermartijn642.fusion.texture.types.connecting.StitchedConnectingTextureData;
import com.supermartijn642.fusion.texture.types.connecting.TextureConnections;

/**
 * Created 12/10/2024 by SuperMartijn642
 */
public class OverlayLayoutHandler extends ConnectingTextureLayoutHandler {

    /**
     * {@code indexMap[q][i]} specifies the tile index to use for quad {@code q} and connections index {@code i}
     */
    private final int[][] tileMapping;

    public OverlayLayoutHandler(){
        super(6, 3, 1, 1, 3);

        // Pre-compute all the tile locations
        this.tileMapping = new int[4][(int)Math.pow(2, 8)];
        for(TextureConnections connections : TextureConnections.iterateAll()){
            int index = this.connectionsIndex(connections);
            for(int quad = 0; quad < 4; quad++){
                int[] pos = this.getTilePos(quad, connections);
                this.tileMapping[quad][index] = pos == null ? -1 : pos[0] + pos[1] * this.getWidth();
            }
        }
    }

    private int connectionsIndex(TextureConnections connections){
        return (connections.top ? 1 : 0)
            | (connections.topRight ? 2 : 0)
            | (connections.right ? 4 : 0)
            | (connections.bottomRight ? 8 : 0)
            | (connections.bottom ? 16 : 0)
            | (connections.bottomLeft ? 32 : 0)
            | (connections.left ? 64 : 0)
            | (connections.topLeft ? 128 : 0);
    }

    private int[] getTilePos(int quadIndex, TextureConnections connections){
        if(connections.top && connections.right && connections.bottom && connections.left)
            return quadIndex == 0 ? new int[]{4, 1} : null;
        if(!connections.top && !connections.topRight && !connections.right && !connections.bottomRight && !connections.bottom && !connections.bottomLeft && !connections.left && !connections.topLeft)
            return null;
        if(quadIndex == 0){
            if(connections.left)
                return null;
            if(!connections.top)
                return connections.topLeft ? new int[]{2, 2} : null;
            if(!connections.right)
                return new int[]{1, 2};
            if(!connections.bottom)
                return new int[]{3, 2};
            return new int[]{3, 1};
        }else if(quadIndex == 1){
            if(connections.top)
                return null;
            if(!connections.right)
                return connections.topRight ? new int[]{0, 2} : null;
            if(!connections.bottom)
                return new int[]{0, 1};
            if(!connections.left)
                return new int[]{3, 0};
            return new int[]{4, 0};
        }else if(quadIndex == 2){
            if(connections.right)
                return null;
            if(!connections.bottom)
                return connections.bottomRight ? new int[]{0, 0} : null;
            if(!connections.left)
                return new int[]{1, 0};
            if(!connections.top)
                return new int[]{5, 0};
            return new int[]{5, 1};
        }else if(quadIndex == 3){
            if(connections.bottom)
                return null;
            if(!connections.left)
                return connections.bottomLeft ? new int[]{2, 0} : null;
            if(!connections.top)
                return new int[]{2, 1};
            if(!connections.right)
                return new int[]{5, 2};
            return new int[]{4, 2};
        }
        return null;
    }

    @Override
    public boolean processBlockQuad(int quadIndex, MutableQuad quad, SpriteInstance currentSprite, StitchedConnectingTextureData data, TextureConnections connections){
        // Get the correct tile position
        int tile = this.tileMapping[quadIndex][this.connectionsIndex(connections)];
        // If the quad is not needed, discard it
        if(tile == -1)
            return false;
        // Discard quad if tile is empty
        SpriteInstance newSprite = data.getTiles().get(tile);
        if(newSprite == null)
            return false;
        // Adjust the quad's uv
        swapQuadSpriteUV(quad, currentSprite, newSprite);
        return true;
    }

    @Override
    public boolean processItemQuad(int quadIndex, MutableQuad quad, SpriteInstance currentSprite, StitchedConnectingTextureData data){
        return false;
    }
}
