package com.supermartijn642.fusion.texture.types.connecting.layouts;

import com.supermartijn642.fusion.api.texture.data.ConnectingTextureLayout;
import com.supermartijn642.fusion.model.types.connecting.MutableQuad;
import com.supermartijn642.fusion.texture.types.connecting.TextureConnections;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

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
                new PiecedLayoutHandler()
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
    public abstract boolean processBlockQuad(int quadIndex, MutableQuad quad, TextureAtlasSprite sprite, TextureConnections connections);

    /**
     * @param quadIndex indicates the quad index when {@link #auxiliaryQuadCount} is greater than 0
     * @return if {@code false} is returned, the quad will be discarded
     */
    public abstract boolean processItemQuad(int quadIndex, MutableQuad quad, TextureAtlasSprite sprite);

    public abstract static class SimpleHandler extends ConnectingTextureLayoutHandler {

        private final int[][] uvs;

        public SimpleHandler(int width, int height, int maxIndexSize){
            super(width, height, 0, 0, 0);

            // Pre-compute all the tile locations
            this.uvs = new int[(int)Math.pow(2, maxIndexSize)][];
            for(TextureConnections connections : TextureConnections.iterateAll()){
                int i = this.connectionsIndex(connections);
                if(this.uvs[i] == null)
                    this.uvs[i] = this.getTilePos(connections);
            }
        }

        protected abstract int connectionsIndex(TextureConnections connections);

        protected abstract int[] getTilePos(TextureConnections connections);

        @Override
        public boolean processBlockQuad(int quadIndex, MutableQuad quad, TextureAtlasSprite sprite, TextureConnections connections){
            // Get the correct tile position
            int[] tile = this.uvs[this.connectionsIndex(connections)];
            // Adjust the quad's uv
            adjustQuadUV(quad, tile[0], tile[1], sprite);
            // Always keep the quad
            return true;
        }

        @Override
        public boolean processItemQuad(int quadIndex, MutableQuad quad, TextureAtlasSprite sprite){
            return true;
        }

        private static void adjustQuadUV(MutableQuad quad, int tileU, int tileV, TextureAtlasSprite sprite){
            for(int i = 0; i < 4; i++){
                float width = sprite.getU1() - sprite.getU0();
                float u = quad.u(i) + width * tileU;

                float height = sprite.getV1() - sprite.getV0();
                float v = quad.v(i) + height * tileV;
                quad.uv(i, u, v);
            }
        }
    }
}
