package com.supermartijn642.fusion.texture.types.connecting.layouts;

import com.supermartijn642.fusion.model.MutableQuad;
import com.supermartijn642.fusion.texture.types.connecting.TextureConnections;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/**
 * Created 09/09/2024 by SuperMartijn642
 */
public class PiecedLayoutHandler extends ConnectingTextureLayoutHandler {

    private static final int[] CORNER_SPRITE_INDICES = {0, 3, 2, 4, 0, 3, 2, 1};

    public PiecedLayoutHandler(){
        super(5, 1, 0, 0, 3);
    }

    @Override
    public boolean processBlockQuad(int quadIndex, MutableQuad quad, TextureAtlasSprite sprite, TextureConnections connections){
        // If the connections just happen to match an entire sprite, just use that and discard the auxiliary quads
        int fullSpriteIndex = -1;
        if(!connections.top && !connections.right && !connections.bottom && !connections.left)
            fullSpriteIndex = 0;
        else if(connections.top && connections.topRight && connections.right && connections.bottomRight && connections.bottom && connections.bottomLeft && connections.left && connections.topLeft)
            fullSpriteIndex = 1;
        else if(connections.top && !connections.right && connections.bottom && !connections.left)
            fullSpriteIndex = 2;
        else if(!connections.top && connections.right && !connections.bottom && connections.left)
            fullSpriteIndex = 3;
        else if(connections.top && !connections.topRight && connections.right && !connections.bottomRight && connections.bottom && !connections.bottomLeft && connections.left && !connections.topLeft)
            fullSpriteIndex = 4;
        if(fullSpriteIndex != -1){
            if(quadIndex != 0)
                return false;
            remapUVFullSprite(quad, fullSpriteIndex, sprite);
            return true;
        }

        // Math is complicated, so for now just assume the quad is a rectangle and its uv covers the entire sprite :)
        // TODO do this properly at some point

        // Move all vertices towards the corner of vertex quadIndex
        float[] corner = {quad.x(quadIndex), quad.y(quadIndex), quad.z(quadIndex)};
        for(int i = 0; i < 4; i++){
            if(i == quadIndex)
                continue;
            quad.pos(i, (corner[0] + quad.x(i)) / 2, (corner[1] + quad.y(i)) / 2, (corner[2] + quad.z(i)) / 2);
        }

        // Adjust the uv coordinates
        remapUVCornerSprite(quad, quadIndex, getTileIndex(quadIndex, connections), sprite);
        return true;
    }

    private static int getTileIndex(int corner, TextureConnections connections){
        int index = switch(corner){
            case 0 ->
                (connections.left ? 1 : 0) | ((connections.top ? 1 : 0) << 1) | ((connections.topLeft ? 1 : 0) << 2);
            case 1 ->
                (connections.right ? 1 : 0) | ((connections.top ? 1 : 0) << 1) | ((connections.topRight ? 1 : 0) << 2);
            case 2 ->
                (connections.right ? 1 : 0) | ((connections.bottom ? 1 : 0) << 1) | ((connections.bottomRight ? 1 : 0) << 2);
            case 3 ->
                (connections.left ? 1 : 0) | ((connections.bottom ? 1 : 0) << 1) | ((connections.bottomLeft ? 1 : 0) << 2);
            default -> throw new IllegalStateException("Unexpected value: " + corner);
        };
        return CORNER_SPRITE_INDICES[index];
    }

    private static void remapUVFullSprite(MutableQuad quad, int tileIndex, TextureAtlasSprite sprite){
        for(int i = 0; i < 4; i++){
            float width = sprite.getU1() - sprite.getU0();
            float u = quad.u(i) + width * tileIndex;
            quad.uv(i, u, quad.v(i));
        }
    }

    private static void remapUVCornerSprite(MutableQuad quad, int corner, int tileIndex, TextureAtlasSprite sprite){
        float width = sprite.getU1() - sprite.getU0();
        float height = sprite.getV1() - sprite.getV0();
        float minU = sprite.getU0() + (corner == 0 || corner == 3 ? 0 : 0.5f) * width;
        float maxU = sprite.getU0() + (corner == 0 || corner == 3 ? 0.5f : 1) * width;
        float minV = sprite.getV0() + (corner == 0 || corner == 1 ? 0 : 0.5f) * height;
        float maxV = sprite.getV0() + (corner == 0 || corner == 1 ? 0.5f : 1) * height;
        for(int i = 0; i < 4; i++){
            float u = Math.min(Math.max(quad.u(i), minU), maxU) + width * tileIndex;
            float v = Math.min(Math.max(quad.v(i), minV), maxV);
            quad.uv(i, u, v);
        }
    }

    @Override
    public boolean processItemQuad(int quadIndex, MutableQuad quad, TextureAtlasSprite sprite){
        return quadIndex == 0;
    }
}
