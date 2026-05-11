package com.supermartijn642.fusion.texture.types.connecting.layouts;

import com.supermartijn642.fusion.api.model.custom.quad.MutableQuad;
import com.supermartijn642.fusion.api.texture.custom.SpriteInstance;
import com.supermartijn642.fusion.api.texture.data.ConnectingTextureLayout;
import com.supermartijn642.fusion.texture.types.connecting.StitchedConnectingTextureData;
import com.supermartijn642.fusion.texture.types.connecting.TextureConnections;

import java.util.Arrays;

/**
 * Created 08/09/2024 by SuperMartijn642
 */
public abstract class ConnectingTextureLayoutHandler {

    /**
     * @see ConnectingTextureLayout
     */
    private static ConnectingTextureLayoutHandler[] HANDLERS;

    public static ConnectingTextureLayoutHandler get(ConnectingTextureLayout layout){
        if(HANDLERS == null){
            HANDLERS = new ConnectingTextureLayoutHandler[]{
                new FullLayoutHandler(),
                new HorizontalLayoutHandler(),
                new SimpleLayoutHandler(),
                new VerticalLayoutHandler(),
                new CompactLayoutHandler(),
                new PiecedLayoutHandler(),
                new OverlayLayoutHandler()
            };
            if(HANDLERS.length != ConnectingTextureLayout.values().length)
                throw new AssertionError("Missing connecting texture layout handlers!");
        }
        return HANDLERS[layout.ordinal()];
    }

    protected final int width, height;
    protected final int defaultTileX, defaultTileY;
    protected final int auxiliaryQuadCount;

    public ConnectingTextureLayoutHandler(int width, int height, int defaultTileX, int defaultTileY, int auxiliaryQuadCount){
        if(auxiliaryQuadCount > 15) // Currently, 4 bits are allocated for quad index in the connecting model, hence at most 15 auxiliary quads
            throw new IllegalArgumentException("Invalid auxiliary quad count: " + auxiliaryQuadCount);
        this.width = width;
        this.height = height;
        this.defaultTileX = defaultTileX;
        this.defaultTileY = defaultTileY;
        this.auxiliaryQuadCount = auxiliaryQuadCount;
    }

    public int getWidth(){
        return this.width;
    }

    public int getHeight(){
        return this.height;
    }

    public int getAuxiliaryQuadCount(){
        return this.auxiliaryQuadCount;
    }

    public int defaultTileX(){
        return this.defaultTileX;
    }

    public int defaultTileY(){
        return this.defaultTileY;
    }

    /**
     * @param quadIndex indicates the quad index when {@link #auxiliaryQuadCount} is greater than 0
     * @return if {@code false} is returned, the quad will be discarded
     */
    public abstract boolean processBlockQuad(int quadIndex, MutableQuad quad, SpriteInstance currentSprite, StitchedConnectingTextureData data, TextureConnections connections);

    /**
     * @param quadIndex indicates the quad index when {@link #auxiliaryQuadCount} is greater than 0
     * @return if {@code false} is returned, the quad will be discarded
     */
    public abstract boolean processItemQuad(int quadIndex, MutableQuad quad, SpriteInstance currentSprite, StitchedConnectingTextureData data);

    public abstract static class SimpleHandler extends ConnectingTextureLayoutHandler {

        /**
         * {@code indexMap[i]} specifies the tile index to use for connections index {@code i}
         */
        private final int[] tileMapping;

        public SimpleHandler(int width, int height, int maxIndexSize){
            super(width, height, 0, 0, 0);

            // Pre-compute all the tile locations
            this.tileMapping = new int[(int)Math.pow(2, maxIndexSize)];
            Arrays.fill(this.tileMapping, -1);
            for(TextureConnections connections : TextureConnections.iterateAll()){
                int i = this.connectionsIndex(connections);
                if(this.tileMapping[i] == -1){
                    int[] pos = this.getTilePos(connections);
                    this.tileMapping[i] = pos[0] + pos[1] * this.getWidth();
                }
            }
        }

        protected abstract int connectionsIndex(TextureConnections connections);

        protected abstract int[] getTilePos(TextureConnections connections);

        @Override
        public boolean processBlockQuad(int quadIndex, MutableQuad quad, SpriteInstance currentSprite, StitchedConnectingTextureData data, TextureConnections connections){
            // Get the correct tile index
            int tile = this.tileMapping[this.connectionsIndex(connections)];
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
            return true;
        }
    }

    public static void swapQuadSpriteUV(MutableQuad quad, SpriteInstance currentSprite, SpriteInstance newSprite){
        for(int i = 0; i < 4; i++){
            quad.uv(
                i,
                newSprite.getU0() + (quad.u(i) - currentSprite.getU0()) / (currentSprite.getU1() - currentSprite.getU0()) * (newSprite.getU1() - newSprite.getU0()),
                newSprite.getV0() + (quad.v(i) - currentSprite.getV0()) / (currentSprite.getV1() - currentSprite.getV0()) * (newSprite.getV1() - newSprite.getV0())
            );
        }
    }
}
