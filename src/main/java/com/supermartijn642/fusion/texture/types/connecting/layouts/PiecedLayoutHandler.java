package com.supermartijn642.fusion.texture.types.connecting.layouts;

import com.supermartijn642.fusion.api.texture.custom.SpriteInstance;
import com.supermartijn642.fusion.model.MutableQuad;
import com.supermartijn642.fusion.texture.types.connecting.StitchedConnectingTextureData;
import com.supermartijn642.fusion.texture.types.connecting.TextureConnections;

/**
 * Created 09/09/2024 by SuperMartijn642
 */
public class PiecedLayoutHandler extends ConnectingTextureLayoutHandler {

    private static final int[] CORNER_SPRITE_INDICES = {0, 3, 2, 4, 0, 3, 2, 1};

    public PiecedLayoutHandler(){
        super(5, 1, 0, 0, 3);
    }

    @Override
    public boolean processBlockQuad(int quadIndex, MutableQuad quad, SpriteInstance currentSprite, StitchedConnectingTextureData data, TextureConnections connections){
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
            SpriteInstance newSprite = data.getTiles().get(fullSpriteIndex);
            if(newSprite == null)
                return false;
            swapQuadSpriteUV(quad, currentSprite, newSprite);
            return true;
        }

        // Figure out how much to move each vertex towards corner quadIndex based on uv
        float halfU = (currentSprite.getU1() + currentSprite.getU0()) / 2, halfV = (currentSprite.getV1() + currentSprite.getV0()) / 2;
        int nextCorner = (quadIndex + 1) % 4, oppositeCorner = (quadIndex + 2) % 4, lastCorner = (quadIndex + 3) % 4;
        boolean nextCornerIsSameU = Math.abs(quad.u(nextCorner) - quad.u(quadIndex)) / vertexDistance(quad, quadIndex, nextCorner) < Math.abs(quad.u(lastCorner) - quad.u(quadIndex)) / vertexDistance(quad, quadIndex, lastCorner);
        boolean nextCornerUVSmaller = nextCornerIsSameU ? quad.v(nextCorner) < quad.v(quadIndex) : quad.u(nextCorner) < quad.u(quadIndex);
        float toNextCornerPercentage = nextCornerIsSameU ? nextCornerUVSmaller ? (quad.v(quadIndex) - halfV) / (quad.v(quadIndex) - quad.v(nextCorner)) : (halfV - quad.v(quadIndex)) / (quad.v(nextCorner) - quad.v(quadIndex)) : nextCornerUVSmaller ? (quad.u(quadIndex) - halfU) / (quad.u(quadIndex) - quad.u(nextCorner)) : (halfU - quad.u(quadIndex)) / (quad.u(nextCorner) - quad.u(quadIndex));
        if(toNextCornerPercentage <= 0)
            return false;
        boolean lastCornerUVSmaller = nextCornerIsSameU ? quad.u(lastCorner) < quad.u(quadIndex) : quad.v(lastCorner) < quad.v(quadIndex);
        float toLastCornerPercentage = nextCornerIsSameU ? lastCornerUVSmaller ? (quad.u(quadIndex) - halfU) / (quad.u(quadIndex) - quad.u(lastCorner)) : (halfU - quad.u(quadIndex)) / (quad.u(lastCorner) - quad.u(quadIndex)) : nextCornerUVSmaller ? (quad.v(quadIndex) - halfV) / (quad.v(quadIndex) - quad.v(lastCorner)) : (halfV - quad.v(quadIndex)) / (quad.v(lastCorner) - quad.v(quadIndex));
        if(toLastCornerPercentage <= 0)
            return false;
        float oppositeToNextPercentage = nextCornerIsSameU ? lastCornerUVSmaller ? (halfU - quad.u(oppositeCorner)) / (quad.u(nextCorner) - quad.u(oppositeCorner)) : (quad.u(oppositeCorner) - halfU) / (quad.u(oppositeCorner) - quad.u(nextCorner)) : lastCornerUVSmaller ? (halfV - quad.v(oppositeCorner)) / (quad.v(nextCorner) - quad.v(oppositeCorner)) : (quad.v(oppositeCorner) - halfV) / (quad.v(oppositeCorner) - quad.v(nextCorner));
        float oppositeToLastPercentage = nextCornerIsSameU ? nextCornerUVSmaller ? (halfV - quad.v(oppositeCorner)) / (quad.v(lastCorner) - quad.v(oppositeCorner)) : (quad.v(oppositeCorner) - halfV) / (quad.v(oppositeCorner) - quad.v(lastCorner)) : nextCornerUVSmaller ? (halfU - quad.u(oppositeCorner)) / (quad.u(lastCorner) - quad.u(oppositeCorner)) : (quad.u(oppositeCorner) - halfU) / (quad.u(oppositeCorner) - quad.u(lastCorner));

        // Move vertices towards the corner of vertex quadIndex
        if(oppositeToNextPercentage > 0 || oppositeToLastPercentage > 0){
            float oppositeX = quad.x(oppositeCorner);
            float oppositeY = quad.y(oppositeCorner);
            float oppositeZ = quad.z(oppositeCorner);
            if(oppositeToNextPercentage > 0){
                oppositeX += (quad.x(nextCorner) - quad.x(oppositeCorner)) * oppositeToNextPercentage;
                oppositeY += (quad.y(nextCorner) - quad.y(oppositeCorner)) * oppositeToNextPercentage;
                oppositeZ += (quad.z(nextCorner) - quad.z(oppositeCorner)) * oppositeToNextPercentage;
            }
            if(oppositeToLastPercentage > 0){
                oppositeX += (quad.x(lastCorner) - quad.x(oppositeCorner)) * oppositeToLastPercentage;
                oppositeY += (quad.y(lastCorner) - quad.y(oppositeCorner)) * oppositeToLastPercentage;
                oppositeZ += (quad.z(lastCorner) - quad.z(oppositeCorner)) * oppositeToLastPercentage;
            }
            quad.pos(oppositeCorner, oppositeX, oppositeY, oppositeZ);
        }
        if(toNextCornerPercentage < 1){
            quad.pos(
                nextCorner,
                quad.x(quadIndex) + (quad.x(nextCorner) - quad.x(quadIndex)) * toNextCornerPercentage,
                quad.y(quadIndex) + (quad.y(nextCorner) - quad.y(quadIndex)) * toNextCornerPercentage,
                quad.z(quadIndex) + (quad.z(nextCorner) - quad.z(quadIndex)) * toNextCornerPercentage
            );
        }
        if(toLastCornerPercentage < 1){
            quad.pos(
                lastCorner,
                quad.x(quadIndex) + (quad.x(lastCorner) - quad.x(quadIndex)) * toLastCornerPercentage,
                quad.y(quadIndex) + (quad.y(lastCorner) - quad.y(quadIndex)) * toLastCornerPercentage,
                quad.z(quadIndex) + (quad.z(lastCorner) - quad.z(quadIndex)) * toLastCornerPercentage
            );
        }

        // Swap tiles
        int tileIndex = getTileIndex(nextCornerIsSameU ? !nextCornerUVSmaller : !lastCornerUVSmaller, nextCornerIsSameU ? !lastCornerUVSmaller : !nextCornerUVSmaller, connections);
        SpriteInstance newSprite = data.getTiles().get(tileIndex);
        if(newSprite == null)
            return false;
        swapQuadSpriteUV(quad, currentSprite, newSprite);

        // Adjust the uv coordinates
        if(oppositeToNextPercentage > 0 || oppositeToLastPercentage > 0){
            float oppositeU = quad.u(oppositeCorner);
            float oppositeV = quad.v(oppositeCorner);
            if(oppositeToNextPercentage > 0){
                oppositeU += (quad.u(nextCorner) - quad.u(oppositeCorner)) * oppositeToNextPercentage;
                oppositeV += (quad.v(nextCorner) - quad.v(oppositeCorner)) * oppositeToNextPercentage;
            }
            if(oppositeToLastPercentage > 0){
                oppositeU += (quad.u(lastCorner) - quad.u(oppositeCorner)) * oppositeToLastPercentage;
                oppositeV += (quad.v(lastCorner) - quad.v(oppositeCorner)) * oppositeToLastPercentage;
            }
            quad.uv(oppositeCorner, oppositeU, oppositeV);
        }else
            quad.uv(oppositeCorner, quad.u(oppositeCorner), quad.v(oppositeCorner));
        if(toNextCornerPercentage < 1){
            quad.uv(
                nextCorner,
                quad.u(quadIndex) + (quad.u(nextCorner) - quad.u(quadIndex)) * toNextCornerPercentage,
                quad.v(quadIndex) + (quad.v(nextCorner) - quad.v(quadIndex)) * toNextCornerPercentage
            );
        }else
            quad.uv(nextCorner, quad.u(nextCorner), quad.v(nextCorner));
        if(toLastCornerPercentage < 1){
            quad.uv(
                lastCorner,
                quad.u(quadIndex) + (quad.u(lastCorner) - quad.u(quadIndex)) * toLastCornerPercentage,
                quad.v(quadIndex) + (quad.v(lastCorner) - quad.v(quadIndex)) * toLastCornerPercentage
            );
        }else
            quad.uv(lastCorner, quad.u(lastCorner), quad.v(lastCorner));
        return true;
    }

    private static int getTileIndex(boolean top, boolean left, TextureConnections connections){
        int index = top ?
            left ?
                (connections.left ? 1 : 0) | ((connections.top ? 1 : 0) << 1) | ((connections.topLeft ? 1 : 0) << 2) :
                (connections.right ? 1 : 0) | ((connections.top ? 1 : 0) << 1) | ((connections.topRight ? 1 : 0) << 2) :
            left ?
                (connections.left ? 1 : 0) | ((connections.bottom ? 1 : 0) << 1) | ((connections.bottomLeft ? 1 : 0) << 2) :
                (connections.right ? 1 : 0) | ((connections.bottom ? 1 : 0) << 1) | ((connections.bottomRight ? 1 : 0) << 2);
        return CORNER_SPRITE_INDICES[index];
    }

    private static double vertexDistance(MutableQuad quad, int v1, int v2){
        double xDiff = quad.x(v2) - quad.x(v1);
        double yDiff = quad.y(v2) - quad.y(v1);
        double zDiff = quad.z(v2) - quad.z(v1);
        return Math.sqrt(xDiff * xDiff + yDiff * yDiff + zDiff * zDiff);
    }

    @Override
    public boolean processItemQuad(int quadIndex, MutableQuad quad, SpriteInstance currentSprite, StitchedConnectingTextureData data){
        return quadIndex == 0;
    }
}
