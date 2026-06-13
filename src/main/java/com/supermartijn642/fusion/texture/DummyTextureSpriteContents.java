package com.supermartijn642.fusion.texture;

import com.supermartijn642.fusion.api.texture.custom.AllocatedSprite;
import com.supermartijn642.fusion.texture.custom.AllocatedSpriteImpl;
import com.supermartijn642.fusion.texture.custom.SpriteBuilderImpl;
import com.supermartijn642.fusion.texture.custom.TextureOutputImpl;
import net.minecraft.client.renderer.texture.PngSizeInfo;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created 20/03/2026 by SuperMartijn642
 */
public class DummyTextureSpriteContents {

    public static boolean isSubImageEmpty(BufferedImage image, int x, int y, int width, int height){
        if(x < 0 || y < 0 || x + width > image.getWidth() || y + height > image.getHeight())
            throw new IllegalArgumentException("Given area extends past given sprite contents!");
        if(image.getType() != BufferedImage.TYPE_INT_ARGB)
            return false;
        for(int xx = 0; xx < width; xx++){
            for(int yy = 0; yy < height; yy++){
                if(((image.getRGB(xx, yy) >> 24) & 255) != 0)
                    return false;
            }
        }
        return true;
    }

    private final DummyTextureSpriteContents parent;
    private final TextureOutputImpl<?> textureOutput;
    private final List<DummyTextureSpriteContents> subTextures;
    private List<Child> children;

    private DummyTextureSpriteContents(DummyTextureSpriteContents parent, TextureOutputImpl<?> textureOutput){
        this.parent = parent;
        this.textureOutput = textureOutput;

        List<DummyTextureSpriteContents> subTextures = new ArrayList<>(textureOutput.getSubTextures().size());
        for(TextureOutputImpl<?> subTexture : textureOutput.getSubTextures())
            subTextures.add(new DummyTextureSpriteContents(this, subTexture));
        this.subTextures = subTextures;
    }

    public DummyTextureSpriteContents(TextureOutputImpl<?> textureOutput){
        this(null, textureOutput);
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
        this.children = Collections.unmodifiableList(this.children);
        List<Child> combinedChildren = new ArrayList<>(this.children);
        for(DummyTextureSpriteContents subTexture : this.subTextures)
            combinedChildren.addAll(subTexture.createChildren());
        return combinedChildren;
    }

    public List<Child> children(){
        return this.children;
    }

    public boolean hasAllAllocations(){
        for(Child child : this.children){
            if(child.allocation == null)
                return false;
        }
        for(DummyTextureSpriteContents subTexture : this.subTextures){
            if(!subTexture.hasAllAllocations())
                return false;
        }
        return true;
    }

    public class Child extends TextureAtlasSprite {

        private final SpriteBuilderImpl spriteBuilder;
        private final ResourceLocation identifier;
        private AllocatedSprite allocation;

        public Child(SpriteBuilderImpl spriteBuilder){
            super(spriteBuilder.getIdentifier().toString());
            this.identifier = spriteBuilder.getIdentifier();
            this.width = spriteBuilder.getWidth();
            this.height = spriteBuilder.getHeight();
            this.spriteBuilder = spriteBuilder;
        }

        public DummyTextureSpriteContents parent(){
            return DummyTextureSpriteContents.this;
        }

        public SpriteBuilderImpl spriteBuilder(){
            return this.spriteBuilder;
        }

        public AllocatedSprite allocation(){
            return this.allocation;
        }

        @Override
        public void initSprite(int atlasWidth, int atlasHeight, int x, int y, boolean rotated){
            if(rotated)
                throw new IllegalStateException("Dummy sprites should not be rotated!");
            super.initSprite(atlasWidth, atlasHeight, x, y, false);
            this.allocation = new AllocatedSpriteImpl(
                this.identifier,
                x, y, this.width, this.height,
                (float)x / atlasWidth, (float)(x + this.width) / atlasWidth,
                (float)y / atlasHeight, (float)(y + this.height) / atlasHeight
            );
        }

        @Override
        public void loadSprite(PngSizeInfo sizeInfo, boolean hasAnimation){
            throw new IllegalStateException("Dummy sprites should not be loaded!");
        }

        @Override
        public void loadSpriteFrames(IResource resource, int mipmapLevels){
            throw new IllegalStateException("Dummy sprites should not be loaded!");
        }
    }
}
