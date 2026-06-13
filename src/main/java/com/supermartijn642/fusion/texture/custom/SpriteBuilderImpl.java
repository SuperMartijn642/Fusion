package com.supermartijn642.fusion.texture.custom;

import com.supermartijn642.fusion.api.texture.custom.SpriteBuilder;
import com.supermartijn642.fusion.api.texture.custom.SpriteImageSource;
import com.supermartijn642.fusion.api.texture.custom.SpriteInstance;
import com.supermartijn642.fusion.util.IdentifierUtil;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;

/**
 * Created 23/03/2026 by SuperMartijn642
 */
public class SpriteBuilderImpl implements SpriteBuilder {

    private final TextureOutputImpl<?> textureOutput;
    private String name;
    private SpriteImageSource imageSource;
    private Constructor constructor;
    private int constructorWidth, constructorHeight;
    private boolean markDefault = false;
    private Consumer<SpriteInstance> callback;

    private boolean valid = true;
    private ResourceLocation identifier;

    public SpriteBuilderImpl(TextureOutputImpl<?> textureOutput){
        this.textureOutput = textureOutput;
    }

    public void setIdentifier(ResourceLocation identifier){
        this.identifier = identifier;
    }

    public ResourceLocation getIdentifier(){
        return this.identifier;
    }

    public String getName(){
        return this.name;
    }

    public SpriteImageSource getImageSource(){
        return this.imageSource;
    }

    public Constructor getConstructor(){
        return this.constructor;
    }

    public int getWidth(){
        return this.constructor == null ? ((SpriteImageSourceImpl)this.imageSource).getFrameWidth() : this.constructorWidth;
    }

    public int getHeight(){
        return this.constructor == null ? ((SpriteImageSourceImpl)this.imageSource).getFrameHeight() : this.constructorHeight;
    }

    public void markDefaultUnchecked(){
        this.markDefault = true;
    }

    public boolean isMarkedDefault(){
        return this.markDefault;
    }

    public Consumer<SpriteInstance> getCallback(){
        return this.callback;
    }

    @Override
    public void submit(){
        this.checkValid();
        this.valid = false;
        if(this.imageSource == null && this.constructor == null)
            throw new IllegalStateException("No image source specified!");
        this.textureOutput.submitSprite();
    }

    private void checkValid(){
        if(!this.valid)
            throw new IllegalStateException("Sprite has already been submitted!");
    }

    @Override
    public SpriteBuilder name(String name){
        if(!IdentifierUtil.isValidPath(name))
            throw new IllegalArgumentException("Name must only contain characters [a-zA-Z_]!");
        this.checkValid();
        this.name = name;
        return this;
    }

    @Override
    public SpriteBuilder image(SpriteImageSource image){
        this.checkValid();
        this.imageSource = image;
        return this;
    }

    @Override
    public SpriteBuilder customConstructor(int width, int height, Constructor constructor){
        this.checkValid();
        if(width <= 0 || height <= 0)
            throw new IllegalArgumentException("Width and height must be positive!");
        this.constructor = constructor;
        this.constructorWidth = width;
        this.constructorHeight = height;
        return this;
    }

    @Override
    public SpriteBuilder markDefaultSprite(){
        return this.markDefaultSprite(true);
    }

    @Override
    public SpriteBuilder markDefaultSprite(boolean markDefault){
        this.checkValid();
        this.markDefault = markDefault;
        return this;
    }

    @Override
    public SpriteBuilder setCreationCallback(Consumer<SpriteInstance> callback){
        this.checkValid();
        this.callback = callback;
        return this;
    }
}
