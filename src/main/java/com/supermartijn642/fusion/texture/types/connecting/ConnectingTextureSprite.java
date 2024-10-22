package com.supermartijn642.fusion.texture.types.connecting;

import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.api.texture.data.ConnectingTextureData;
import com.supermartijn642.fusion.texture.types.base.BaseTextureSprite;
import com.supermartijn642.fusion.texture.types.connecting.layouts.ConnectingTextureLayoutHandler;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/**
 * Created 30/04/2023 by SuperMartijn642
 */
public class ConnectingTextureSprite extends BaseTextureSprite {

    private final int originalWidth, originalHeight;
    private float startU, startV;
    private boolean rotatedImage;

    protected ConnectingTextureSprite(TextureAtlasSprite original, ConnectingTextureData data){
        super(original, data);
        this.originalWidth = this.width;
        this.originalHeight = this.height;
        this.resizeUV();
    }

    public float getStartU(){
        return this.startU;
    }

    public float getStartV(){
        return this.startV;
    }

    @Override
    public ConnectingTextureData data(){
        return (ConnectingTextureData)super.data();
    }

    @Override
    public void initSprite(int inX, int inY, int originInX, int originInY, boolean rotatedIn){
        super.initSprite(inX, inY, originInX, originInY, rotatedIn);
        this.startU = this.minU;
        this.startV = this.minV;
        this.resizeUV();
        if(this.rotated && !this.rotatedImage)
            this.rotateLayout();
    }

    private void resizeUV(){
        ConnectingTextureLayoutHandler layoutHandler = ConnectingTextureLayoutHandler.get(this.data().getLayout());
        int layoutWidth = layoutHandler.getWidth();
        int layoutHeight = layoutHandler.getHeight();
        if(this.rotated){
            int width = layoutWidth;
            //noinspection SuspiciousNameCombination
            layoutWidth = layoutHeight;
            //noinspection SuspiciousNameCombination
            layoutHeight = width;
        }
        float tileWidth = (this.maxU - this.minU) / layoutWidth;
        float tileHeight = (this.maxV - this.minV) / layoutHeight;
        this.maxU = this.minU + tileWidth * (layoutHandler.defaultTileX() + 1);
        this.maxV = this.minV + tileHeight * (layoutHandler.defaultTileY() + 1);
        this.minU = this.minU + tileWidth * layoutHandler.defaultTileX();
        this.minV = this.minV + tileHeight * layoutHandler.defaultTileY();
    }

    public void rotateLayout(){
        for(int frame = 0; frame < this.framesTextureData.size(); frame++){
            int[][] pixelsPerLevel = this.framesTextureData.get(frame);
            ConnectingTextureLayoutHandler layoutHandler = ConnectingTextureLayoutHandler.get(this.data().getLayout());
            int layoutWidth = layoutHandler.getWidth(), layoutHeight = layoutHandler.getHeight();
            int textureWidth = this.originalWidth, textureHeight = this.originalHeight;

            // Rotate the sprite tiling
            int tileWidth = textureWidth / layoutWidth, tileHeight = textureHeight / layoutHeight;
            int[] rotatedPixels = new int[textureWidth * textureHeight];
            for(int tileX = 0; tileX < layoutWidth; tileX++){
                for(int tileY = 0; tileY < layoutHeight; tileY++){
                    // Copy one whole tile from tile position (x,y) to (y,x)
                    for(int line = 0; line < tileHeight; line++)
                        System.arraycopy(pixelsPerLevel[0], textureWidth * (tileY * tileHeight + line) + tileX * tileWidth, rotatedPixels, textureHeight * (tileX * tileHeight + line) + tileY * tileWidth, tileWidth);
                }
            }
            pixelsPerLevel[0] = rotatedPixels;
            this.framesTextureData.set(frame, pixelsPerLevel);
        }

        try{
            this.generateMipmaps(this.framesTextureData.get(0).length - 1);
        }catch(Exception e){
            FusionClient.LOGGER.error("Encountered an exception whilst generating mipmaps for rotated connecting texture:", e);
        }

        //noinspection SuspiciousNameCombination
        this.width = this.originalHeight;
        //noinspection SuspiciousNameCombination
        this.height = this.originalWidth;
        this.rotatedImage = true;
    }
}
