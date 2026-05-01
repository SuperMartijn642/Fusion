package com.supermartijn642.fusion.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.texture.custom.TextureInstance;
import com.supermartijn642.fusion.texture.custom.SpriteBuilderImpl;
import com.supermartijn642.fusion.texture.custom.SpriteImageSourceImpl;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.client.resources.metadata.texture.TextureMetadataSection;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Created 20/03/2026 by SuperMartijn642
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class DummyTextureSpriteContents extends SpriteContents {

    private static final NativeImage EMPTY_IMAGE = new NativeImage(1, 1, true);

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

    public static SpriteContents createSubImage(SpriteContents contents, Identifier identifier, int x, int y, int width, int height){
        if(x < 0 || y < 0 || x + width > contents.width || y + height > contents.height)
            throw new IllegalArgumentException("Given area extends past given sprite contents!");
        // Create new image
        NativeImage originalImage = contents.originalImage;
        int frameColumns = originalImage.getWidth() / contents.width, frameRows = originalImage.getHeight() / contents.height;
        int frames = frameRows * frameColumns;
        NativeImage subImage = new NativeImage(width, height * frames, false);
        for(int frame = 0; frame < frames; frame++){
            originalImage.copyRect(
                subImage,
                x + (frame % frameColumns) * contents.width,
                y + frame / frameRows * contents.height,
                0, frame * height,
                width, height,
                false, false
            );
        }
        // Create new sprite contents
        SpriteContents subContents = new SpriteContents(
            identifier,
            new FrameSize(width, height),
            subImage,
            Optional.empty(),
            contents.additionalMetadata,
            Optional.empty()
        );
        if(contents.animatedTexture != null)
            subContents.animatedTexture = subContents.new AnimatedTexture(contents.animatedTexture.frames, 1, contents.animatedTexture.interpolateFrames);
        subContents.mipmapStrategy = contents.mipmapStrategy;
        subContents.alphaCutoffBias = contents.alphaCutoffBias;
        return subContents;
    }

    private final Identifier identifier;
    private final TextureType<?,Object> textureType;
    private final Object textureData;
    private final List<SpriteBuilderImpl> spriteBuilders;
    private final Consumer<TextureInstance<Object>> textureCreationCallback;
    private final Optional<TextureMetadataSection> textureMetadata;
    private List<Child> children;

    public DummyTextureSpriteContents(Identifier identifier, TextureType<?,Object> textureType, Object textureData, List<SpriteBuilderImpl> spriteBuilders, Consumer<TextureInstance<Object>> textureCreationCallback, Optional<TextureMetadataSection> textureMetadata){
        super(identifier, new FrameSize(0, 0), EMPTY_IMAGE);
        this.identifier = identifier;
        this.textureType = textureType;
        this.textureData = textureData;
        this.spriteBuilders = spriteBuilders;
        this.textureCreationCallback = textureCreationCallback;
        this.textureMetadata = textureMetadata;
    }

    public Identifier identifier(){
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

    public Optional<TextureMetadataSection> textureMetadata(){
        return this.textureMetadata;
    }

    public List<Child> createChildren(){
        this.children = new ArrayList<>(this.spriteBuilders.size());
        for(SpriteBuilderImpl spriteBuilder : this.spriteBuilders){
            Identifier identifier = spriteBuilder.getName() == null ?
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

        public Child(Identifier identifier, FrameSize frameSize, SpriteBuilderImpl spriteBuilder){
            super(identifier, frameSize, EMPTY_IMAGE);
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
