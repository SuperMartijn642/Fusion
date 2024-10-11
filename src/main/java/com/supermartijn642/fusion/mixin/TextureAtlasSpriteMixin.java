package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.api.texture.DefaultTextureTypes;
import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.extensions.TextureAtlasSpriteExtension;
import net.minecraft.client.renderer.texture.PngSizeInfo;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.data.AnimationFrame;
import net.minecraft.client.resources.data.AnimationMetadataSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created 26/04/2023 by SuperMartijn642
 */
@Mixin(TextureAtlasSprite.class)
public class TextureAtlasSpriteMixin implements TextureAtlasSpriteExtension {

    @Shadow
    private List<int[][]> framesTextureData;
    @Shadow
    private AnimationMetadataSection animationMetadata;
    @Shadow
    private int width;
    @Shadow
    private int height;

    @Shadow
    private static int[][] getFrameTextureData(int[][] pixels, int width, int height, int level){
        return null;
    }

    @Unique
    private TextureType<?> fusionType;
    @Unique
    private int textureWidth, textureHeight;

    @Override
    public void setFusionTextureType(TextureType<?> type){
        this.fusionType = type;
    }

    @Override
    public TextureType<?> getFusionTextureType(){
        return this.fusionType;
    }

    @Override
    public void setTextureSize(int width, int height){
        this.textureWidth = width;
        this.textureHeight = height;
    }

    @Override
    public Pair<Integer,Integer> getTextureSize(){
        return Pair.of(this.textureWidth, this.textureHeight);
    }

    @Inject(
        method = "loadSprite",
        at = @At(
            value = "INVOKE",
            target = "Ljava/lang/RuntimeException;<init>(Ljava/lang/String;)V",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    private void loadSprite(PngSizeInfo sizeInfo, boolean hasAnimation, CallbackInfo ci){
        if(this.fusionType != null)
            ci.cancel();
    }

    @Inject(
        method = "loadSpriteFrames",
        at = @At("HEAD"),
        cancellable = true
    )
    public void loadSpriteFrames(IResource resource, int mipmapLevels, CallbackInfo ci) throws IOException{
        if(this.fusionType == null || this.fusionType == DefaultTextureTypes.VANILLA)
            return;
        ci.cancel();

        // Vanilla randomly uses the textures with for copying each frame's height, so fix that
        BufferedImage bufferedimage = TextureUtil.readBufferedImage(resource.getInputStream());
        AnimationMetadataSection animation = resource.getMetadata("animation");
        int[][] pixelsPerLevel = new int[mipmapLevels][];
        pixelsPerLevel[0] = new int[bufferedimage.getWidth() * bufferedimage.getHeight()];
        bufferedimage.getRGB(0, 0, bufferedimage.getWidth(), bufferedimage.getHeight(), pixelsPerLevel[0], 0, bufferedimage.getWidth());

        if(animation == null){
            this.framesTextureData.add(pixelsPerLevel);
            return;
        }

        int frames = bufferedimage.getHeight() / this.height;
        if(animation.getFrameCount() > 0){
            for(Integer index : animation.getFrameIndexSet()){
                if(index >= frames)
                    throw new RuntimeException("invalid frameindex " + index);
                while(this.framesTextureData.size() <= index)
                    this.framesTextureData.add(null);
                this.framesTextureData.set(index, getFrameTextureData(pixelsPerLevel, this.width, this.height, index));
            }
            this.animationMetadata = animation;
        }else{
            List<AnimationFrame> list = new ArrayList<>();
            for(int index = 0; index < frames; ++index){
                this.framesTextureData.add(getFrameTextureData(pixelsPerLevel, this.width, this.height, index));
                list.add(new AnimationFrame(index, -1));
            }
            this.animationMetadata = new AnimationMetadataSection(list, this.width, this.height, animation.getFrameTime(), animation.isInterpolate());
        }
    }
}
