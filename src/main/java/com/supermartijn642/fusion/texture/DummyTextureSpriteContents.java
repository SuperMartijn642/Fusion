package com.supermartijn642.fusion.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.texture.custom.TextureInstance;
import com.supermartijn642.fusion.texture.custom.SpriteBuilderImpl;
import com.supermartijn642.fusion.texture.custom.SpriteImageSourceImpl;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceMetadata;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Created 20/03/2026 by SuperMartijn642
 */
public class DummyTextureSpriteContents extends SpriteContents {

    private static final NativeImage EMPTY_IMAGE = new NativeImage(NativeImage.Format.RGBA, 1, 1, true);

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

    private final ResourceLocation identifier;
    private final TextureType<?,Object> textureType;
    private final Object textureData;
    private final List<SpriteBuilderImpl> spriteBuilders;
    private final Consumer<TextureInstance<Object>> textureCreationCallback;
    private List<Child> children;

    public DummyTextureSpriteContents(ResourceLocation identifier, ResourceMetadata resourceMetadata, TextureType<?,Object> textureType, Object textureData, List<SpriteBuilderImpl> spriteBuilders, Consumer<TextureInstance<Object>> textureCreationCallback){
        super(identifier, new FrameSize(0, 0), EMPTY_IMAGE, ResourceMetadata.EMPTY);
        this.metadata = resourceMetadata;
        this.identifier = identifier;
        this.textureType = textureType;
        this.textureData = textureData;
        this.spriteBuilders = spriteBuilders;
        this.textureCreationCallback = textureCreationCallback;
    }

    public ResourceLocation identifier(){
        return this.identifier;
    }

    public TextureType<?,Object> textureType(){
        return this.textureType;
    }

    public Object textureData(){
        return this.textureData;
    }

    public List<SpriteBuilderImpl> spriteBuilders(){
        return this.spriteBuilders;
    }

    public Consumer<TextureInstance<Object>> textureCreationCallback(){
        return this.textureCreationCallback;
    }

    public List<Child> createChildren(){
        this.children = new ArrayList<>(this.spriteBuilders.size());
        for(SpriteBuilderImpl spriteBuilder : this.spriteBuilders){
            ResourceLocation identifier = spriteBuilder.getName() == null ?
                this.identifier : this.identifier.withSuffix("_" + spriteBuilder.getName());
            int width = spriteBuilder.getConstructor() == null ?
                ((SpriteImageSourceImpl)spriteBuilder.getImageSource()).getFrameWidth() : spriteBuilder.getConstructorWidth();
            int height = spriteBuilder.getConstructor() == null ?
                ((SpriteImageSourceImpl)spriteBuilder.getImageSource()).getFrameHeight() : spriteBuilder.getConstructorHeight();
            this.children.add(new Child(
                identifier,
                new FrameSize(width, height),
                spriteBuilder
            ));
        }
        this.children = List.copyOf(this.children);
        return this.children;
    }

    public List<Child> children(){
        return this.children;
    }

    public class Child extends SpriteContents {

        private final SpriteBuilderImpl spriteBuilder;

        public Child(ResourceLocation identifier, FrameSize frameSize, SpriteBuilderImpl spriteBuilder){
            super(identifier, frameSize, EMPTY_IMAGE, ResourceMetadata.EMPTY);
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
