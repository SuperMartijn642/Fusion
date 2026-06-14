package com.supermartijn642.fusion.texture.types.connecting.layouts;

import com.supermartijn642.fusion.api.model.custom.quad.EmittableQuad;
import com.supermartijn642.fusion.api.texture.types.connecting.ConnectingTextureData;
import com.supermartijn642.fusion.texture.types.connecting.TextureConnections;

import java.util.Arrays;

/**
 * Created 08/09/2024 by SuperMartijn642
 */
public abstract class ConnectingTextureLayoutHandler {

    /**
     * @see ConnectingTextureData.Layout
     */
    private static ConnectingTextureLayoutHandler[] HANDLERS;

    public static ConnectingTextureLayoutHandler get(ConnectingTextureData.Layout layout){
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
            if(HANDLERS.length != ConnectingTextureData.Layout.values().length)
                throw new AssertionError("Missing connecting texture layout handlers!");
        }
        return HANDLERS[layout.ordinal()];
    }

    protected final int width, height;
    protected final int defaultTileX, defaultTileY;

    public ConnectingTextureLayoutHandler(int width, int height, int defaultTileX, int defaultTileY){
        this.width = width;
        this.height = height;
        this.defaultTileX = defaultTileX;
        this.defaultTileY = defaultTileY;
    }

    public int getWidth(){
        return this.width;
    }

    public int getHeight(){
        return this.height;
    }

    public int defaultTileX(){
        return this.defaultTileX;
    }

    public int defaultTileY(){
        return this.defaultTileY;
    }

    public abstract void processQuad(EmittableQuad quad, TileEmitter tileEmitter, TextureConnections connections);

    public interface TileEmitter {
        void emit(int tile, EmittableQuad emitter);
    }

    public abstract static class SimpleHandler extends ConnectingTextureLayoutHandler {

        /**
         * {@code indexMap[i]} specifies the tile index to use for connections index {@code i}
         */
        private final int[] tileMapping;

        public SimpleHandler(int width, int height, int maxIndexSize){
            super(width, height, 0, 0);

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
        public void processQuad(EmittableQuad quad, TileEmitter tileEmitter, TextureConnections connections){
            // Get the correct tile index
            int tile = this.tileMapping[this.connectionsIndex(connections)];
            tileEmitter.emit(tile, quad);
        }
    }
}
