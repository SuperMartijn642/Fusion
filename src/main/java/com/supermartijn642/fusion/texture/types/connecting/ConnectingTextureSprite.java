package com.supermartijn642.fusion.texture.types.connecting;

import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.api.texture.data.ConnectingTextureData;
import com.supermartijn642.fusion.api.texture.data.ConnectingTextureLayout;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/**
 * Created 30/04/2023 by SuperMartijn642
 */
public class ConnectingTextureSprite extends TextureAtlasSprite {

    private final ConnectingTextureLayout layout;
    private final ConnectingTextureData.RenderType renderType;

    protected ConnectingTextureSprite(TextureAtlasSprite original, ConnectingTextureLayout layout, ConnectingTextureData.RenderType renderType){
        super(original.getIconName());
        this.layout = layout;
        this.renderType = renderType;
        this.copyFrom(original);
        this.framesTextureData = original.framesTextureData;
        this.animationMetadata = original.animationMetadata;
        this.resizeUV();

        if(ConnectingTextureLayoutHelper.shouldBeRotated(layout))
            this.rotateLayout();
    }

    public ConnectingTextureLayout getLayout(){
        return this.layout;
    }

    public ConnectingTextureData.RenderType getRenderType(){
        return this.renderType;
    }

    public void rotateLayout(){
        int[][] pixelsPerLevel = this.framesTextureData.get(0);
        int layoutWidth = ConnectingTextureLayoutHelper.getWidth(this.layout), layoutHeight = ConnectingTextureLayoutHelper.getHeight(this.layout);
        int textureWidth = (int)Math.round(Math.sqrt((double)pixelsPerLevel[0].length * layoutWidth / layoutHeight)), textureHeight = pixelsPerLevel[0].length / textureWidth;

        // Rotate the sprite tiling so we get width > height
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

        this.framesTextureData.add(pixelsPerLevel);
        try{
            this.generateMipmaps(pixelsPerLevel.length - 1);
        }catch(Exception e){
            FusionClient.LOGGER.error("Encountered an exception whilst generating mipmaps for rotated connecting texture:", e);
        }
    }

    @Override
    public void initSprite(int inX, int inY, int originInX, int originInY, boolean rotatedIn){
        super.initSprite(inX, inY, originInX, originInY, rotatedIn);
        this.resizeUV();
    }

    private void resizeUV(){
        int layoutWidth = ConnectingTextureLayoutHelper.getWidth(this.layout);
        int layoutHeight = ConnectingTextureLayoutHelper.getHeight(this.layout);
        if(layoutHeight > layoutWidth){
            int width = layoutWidth;
            //noinspection SuspiciousNameCombination
            layoutWidth = layoutHeight;
            //noinspection SuspiciousNameCombination
            layoutHeight = width;
        }
        this.maxU = this.minU + (this.maxU - this.minU) / layoutWidth;
        this.maxV = this.minV + (this.maxV - this.minV) / layoutHeight;
    }
}
