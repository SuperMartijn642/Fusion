package com.supermartijn642.fusion.texture;

import com.supermartijn642.fusion.api.texture.custom.AllocatedSprite;
import com.supermartijn642.fusion.texture.custom.SpriteBuilderImpl;
import com.supermartijn642.fusion.texture.custom.TextureOutputImpl;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.data.AnimationMetadataSection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created 20/03/2026 by SuperMartijn642
 */
public class DummyTextureSpriteContents {

    public static boolean isSubImageEmpty(NativeImage image, int x, int y, int width, int height){
        if(x < 0 || y < 0 || x + width > image.getWidth() || y + height > image.getHeight())
            throw new IllegalArgumentException("Given area extends past given sprite contents!");
        if(!image.format().hasAlpha() || (image.format() != NativeImage.PixelFormat.RGB && image.format() != NativeImage.PixelFormat.RGBA))
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

    public class Child extends TextureAtlasSprite.Info {

        private final SpriteBuilderImpl spriteBuilder;
        private AllocatedSprite allocation;

        public Child(SpriteBuilderImpl spriteBuilder){
            super(spriteBuilder.getIdentifier(), spriteBuilder.getWidth(), spriteBuilder.getHeight(), AnimationMetadataSection.EMPTY);
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

        public void setAllocation(AllocatedSprite allocation){
            this.allocation = allocation;
        }
    }
}
