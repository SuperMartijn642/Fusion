package com.supermartijn642.fusion.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.supermartijn642.fusion.api.texture.custom.ImageHelper;
import com.supermartijn642.fusion.texture.custom.SpriteBuilderImpl;
import com.supermartijn642.fusion.texture.custom.TextureOutputImpl;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.server.packs.resources.ResourceMetadata;

import java.util.ArrayList;
import java.util.List;

/**
 * Created 20/03/2026 by SuperMartijn642
 */
public class DummyTextureSpriteContents extends SpriteContents {

    private static final NativeImage EMPTY_IMAGE = ImageHelper.createEmpty(1, 1);

    public static boolean isSubImageEmpty(NativeImage image, int x, int y, int width, int height){
        if(x < 0 || y < 0 || x + width > image.getWidth() || y + height > image.getHeight())
            throw new IllegalArgumentException("Given area extends past given sprite contents!");
        if(!image.format().hasAlpha() || image.format().hasLuminance())
            return false;
        for(int xx = 0; xx < width; xx++){
            for(int yy = 0; yy < height; yy++){
                if(image.getLuminanceOrAlpha(xx + x, yy + y) != 0)
                    return false;
            }
        }
        return true;
    }

    private final DummyTextureSpriteContents parent;
    private final TextureOutputImpl<?> textureOutput;
    private final List<DummyTextureSpriteContents> subTextures;
    private List<Child> children;

    private DummyTextureSpriteContents(DummyTextureSpriteContents parent, TextureOutputImpl<?> textureOutput, ResourceMetadata resourceMetadata){
        super(textureOutput.getIdentifier(), new FrameSize(0, 0), EMPTY_IMAGE, ResourceMetadata.EMPTY);
        this.parent = parent;
        this.textureOutput = textureOutput;
        this.metadata = resourceMetadata;

        List<DummyTextureSpriteContents> subTextures = new ArrayList<>(textureOutput.getSubTextures().size());
        for(TextureOutputImpl<?> subTexture : textureOutput.getSubTextures())
            subTextures.add(new DummyTextureSpriteContents(this, subTexture, resourceMetadata));
        this.subTextures = subTextures;
    }

    public DummyTextureSpriteContents(TextureOutputImpl<?> textureOutput, ResourceMetadata resourceMetadata){
        this(null, textureOutput, resourceMetadata);
    }

    public DummyTextureSpriteContents getTopTexture(){
        return this.parent == null ? this : this.parent.getTopTexture();
    }

    public TextureOutputImpl<?> getTextureOutput(){
        return this.textureOutput;
    }

    public List<DummyTextureSpriteContents> getSubTextures(){
        return this.subTextures;
    }

    public List<Child> createChildren(){
        this.children = new ArrayList<>();
        for(SpriteBuilderImpl spriteBuilder : this.textureOutput.getSprites())
            this.children.add(new Child(spriteBuilder));
        this.children = List.copyOf(this.children);
        List<Child> combinedChildren = new ArrayList<>(this.children);
        for(DummyTextureSpriteContents subTexture : this.subTextures)
            combinedChildren.addAll(subTexture.createChildren());
        return combinedChildren;
    }

    public List<Child> children(){
        return this.children;
    }

    public class Child extends SpriteContents {

        private final SpriteBuilderImpl spriteBuilder;

        public Child(SpriteBuilderImpl spriteBuilder){
            super(spriteBuilder.getIdentifier(), new FrameSize(spriteBuilder.getWidth(), spriteBuilder.getHeight()), EMPTY_IMAGE, ResourceMetadata.EMPTY);
            this.spriteBuilder = spriteBuilder;
        }

        public DummyTextureSpriteContents parent(){
            return DummyTextureSpriteContents.this;
        }

        public SpriteBuilderImpl spriteBuilder(){
            return this.spriteBuilder;
        }
    }
}
