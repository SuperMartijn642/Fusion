package com.supermartijn642.fusion.texture.types.connecting.layouts;

import com.supermartijn642.fusion.api.model.custom.quad.EmittableQuad;
import com.supermartijn642.fusion.api.model.custom.quad.MutableQuad;
import com.supermartijn642.fusion.api.texture.SpriteHelper;
import com.supermartijn642.fusion.api.texture.custom.SpriteInstance;
import com.supermartijn642.fusion.texture.types.connecting.TextureConnections;

/**
 * Created 09/09/2024 by SuperMartijn642
 */
public class PiecedLayoutHandler extends ConnectingTextureLayoutHandler {

    private static final int[] CORNER_SPRITE_INDICES = {0, 3, 2, 4, 0, 3, 2, 1};

    public PiecedLayoutHandler(){
        super(5, 1, 0, 0);
    }

    @Override
    public void processQuad(EmittableQuad quad, TileEmitter tileEmitter, TextureConnections connections){
        // If the connections just happen to match an entire sprite, just use that
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
            tileEmitter.emit(fullSpriteIndex, quad);
            return;
        }

        // Handle each corner
        for(int corner = 0; corner < 4; corner++)
            transformCorner(corner, quad, tileEmitter, connections);
    }

    private static void transformCorner(int corner, EmittableQuad quad, TileEmitter tileEmitter, TextureConnections connections){
        SpriteInstance originalSprite = SpriteHelper.getSpriteInstance(quad.sprite());
        if(originalSprite == null)
            return;

        // Figure out how much to move each vertex towards corner quadIndex based on uv
        float halfU = (originalSprite.getU1() + originalSprite.getU0()) / 2, halfV = (originalSprite.getV1() + originalSprite.getV0()) / 2;
        int nextCorner = (corner + 1) % 4, oppositeCorner = (corner + 2) % 4, lastCorner = (corner + 3) % 4;
        boolean nextCornerIsSameU = Math.abs(quad.u(nextCorner) - quad.u(corner)) / vertexDistance(quad, corner, nextCorner) < Math.abs(quad.u(lastCorner) - quad.u(corner)) / vertexDistance(quad, corner, lastCorner);
        boolean nextCornerUVSmaller = nextCornerIsSameU ? quad.v(nextCorner) < quad.v(corner) : quad.u(nextCorner) < quad.u(corner);
        float toNextCornerPercentage = nextCornerIsSameU ? nextCornerUVSmaller ? (quad.v(corner) - halfV) / (quad.v(corner) - quad.v(nextCorner)) : (halfV - quad.v(corner)) / (quad.v(nextCorner) - quad.v(corner)) : nextCornerUVSmaller ? (quad.u(corner) - halfU) / (quad.u(corner) - quad.u(nextCorner)) : (halfU - quad.u(corner)) / (quad.u(nextCorner) - quad.u(corner));
        if(toNextCornerPercentage <= 0)
            return;
        boolean lastCornerUVSmaller = nextCornerIsSameU ? quad.u(lastCorner) < quad.u(corner) : quad.v(lastCorner) < quad.v(corner);
        float toLastCornerPercentage = nextCornerIsSameU ? lastCornerUVSmaller ? (quad.u(corner) - halfU) / (quad.u(corner) - quad.u(lastCorner)) : (halfU - quad.u(corner)) / (quad.u(lastCorner) - quad.u(corner)) : nextCornerUVSmaller ? (quad.v(corner) - halfV) / (quad.v(corner) - quad.v(lastCorner)) : (halfV - quad.v(corner)) / (quad.v(lastCorner) - quad.v(corner));
        if(toLastCornerPercentage <= 0)
            return;
        float oppositeToNextPercentage = nextCornerIsSameU ? lastCornerUVSmaller ? (halfU - quad.u(oppositeCorner)) / (quad.u(nextCorner) - quad.u(oppositeCorner)) : (quad.u(oppositeCorner) - halfU) / (quad.u(oppositeCorner) - quad.u(nextCorner)) : lastCornerUVSmaller ? (halfV - quad.v(oppositeCorner)) / (quad.v(nextCorner) - quad.v(oppositeCorner)) : (quad.v(oppositeCorner) - halfV) / (quad.v(oppositeCorner) - quad.v(nextCorner));
        float oppositeToLastPercentage = nextCornerIsSameU ? nextCornerUVSmaller ? (halfV - quad.v(oppositeCorner)) / (quad.v(lastCorner) - quad.v(oppositeCorner)) : (quad.v(oppositeCorner) - halfV) / (quad.v(oppositeCorner) - quad.v(lastCorner)) : nextCornerUVSmaller ? (halfU - quad.u(oppositeCorner)) / (quad.u(lastCorner) - quad.u(oppositeCorner)) : (quad.u(oppositeCorner) - halfU) / (quad.u(oppositeCorner) - quad.u(lastCorner));

        // Create transform
        EmittableQuad.Transform transform = q -> {
            // Move vertices towards the corner of vertex quadIndex
            if(oppositeToNextPercentage > 0 || oppositeToLastPercentage > 0){
                float oppositeX = q.x(oppositeCorner);
                float oppositeY = q.y(oppositeCorner);
                float oppositeZ = q.z(oppositeCorner);
                if(oppositeToNextPercentage > 0){
                    oppositeX += (q.x(nextCorner) - q.x(oppositeCorner)) * oppositeToNextPercentage;
                    oppositeY += (q.y(nextCorner) - q.y(oppositeCorner)) * oppositeToNextPercentage;
                    oppositeZ += (q.z(nextCorner) - q.z(oppositeCorner)) * oppositeToNextPercentage;
                }
                if(oppositeToLastPercentage > 0){
                    oppositeX += (q.x(lastCorner) - q.x(oppositeCorner)) * oppositeToLastPercentage;
                    oppositeY += (q.y(lastCorner) - q.y(oppositeCorner)) * oppositeToLastPercentage;
                    oppositeZ += (q.z(lastCorner) - q.z(oppositeCorner)) * oppositeToLastPercentage;
                }
                q.position(oppositeCorner, oppositeX, oppositeY, oppositeZ);
            }
            if(toNextCornerPercentage < 1){
                q.position(
                    nextCorner,
                    q.x(corner) + (q.x(nextCorner) - q.x(corner)) * toNextCornerPercentage,
                    q.y(corner) + (q.y(nextCorner) - q.y(corner)) * toNextCornerPercentage,
                    q.z(corner) + (q.z(nextCorner) - q.z(corner)) * toNextCornerPercentage
                );
            }
            if(toLastCornerPercentage < 1){
                q.position(
                    lastCorner,
                    q.x(corner) + (q.x(lastCorner) - q.x(corner)) * toLastCornerPercentage,
                    q.y(corner) + (q.y(lastCorner) - q.y(corner)) * toLastCornerPercentage,
                    q.z(corner) + (q.z(lastCorner) - q.z(corner)) * toLastCornerPercentage
                );
            }

            // Adjust the uv coordinates
            if(oppositeToNextPercentage > 0 || oppositeToLastPercentage > 0){
                float oppositeU = q.u(oppositeCorner);
                float oppositeV = q.v(oppositeCorner);
                if(oppositeToNextPercentage > 0){
                    oppositeU += (q.u(nextCorner) - q.u(oppositeCorner)) * oppositeToNextPercentage;
                    oppositeV += (q.v(nextCorner) - q.v(oppositeCorner)) * oppositeToNextPercentage;
                }
                if(oppositeToLastPercentage > 0){
                    oppositeU += (q.u(lastCorner) - q.u(oppositeCorner)) * oppositeToLastPercentage;
                    oppositeV += (q.v(lastCorner) - q.v(oppositeCorner)) * oppositeToLastPercentage;
                }
                q.uv(oppositeCorner, oppositeU, oppositeV);
            }else
                q.uv(oppositeCorner, q.u(oppositeCorner), q.v(oppositeCorner));
            if(toNextCornerPercentage < 1){
                q.uv(
                    nextCorner,
                    q.u(corner) + (q.u(nextCorner) - q.u(corner)) * toNextCornerPercentage,
                    q.v(corner) + (q.v(nextCorner) - q.v(corner)) * toNextCornerPercentage
                );
            }else
                q.uv(nextCorner, q.u(nextCorner), q.v(nextCorner));
            if(toLastCornerPercentage < 1){
                q.uv(
                    lastCorner,
                    q.u(corner) + (q.u(lastCorner) - q.u(corner)) * toLastCornerPercentage,
                    q.v(corner) + (q.v(lastCorner) - q.v(corner)) * toLastCornerPercentage
                );
            }else
                q.uv(lastCorner, q.u(lastCorner), q.v(lastCorner));
            q.emit();
        };

        // Emit tile
        int tileIndex = getTileIndex(nextCornerIsSameU ? !nextCornerUVSmaller : !lastCornerUVSmaller, nextCornerIsSameU ? !lastCornerUVSmaller : !nextCornerUVSmaller, connections);
        try(EmittableQuad.Popper p = quad.pushTransform(transform)){
            tileEmitter.emit(tileIndex, quad);
        }
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
}
