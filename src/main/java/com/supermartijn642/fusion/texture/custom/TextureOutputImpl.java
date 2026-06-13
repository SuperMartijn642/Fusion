package com.supermartijn642.fusion.texture.custom;

import com.supermartijn642.fusion.api.texture.RawTextureInstance;
import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.texture.custom.SpriteBuilder;
import com.supermartijn642.fusion.api.texture.custom.TextureInstance;
import com.supermartijn642.fusion.api.texture.custom.TextureOutput;
import com.supermartijn642.fusion.api.util.UserErrorException;
import com.supermartijn642.fusion.texture.TextureTypeRegistryImpl;
import com.supermartijn642.fusion.util.IdentifierUtil;
import net.minecraft.client.resources.data.AnimationMetadataSection;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Created 23/03/2026 by SuperMartijn642
 */
public class TextureOutputImpl<X> implements TextureOutput<X> {

    private final ResourceLocation identifier;
    private final TextureType<?,X> textureType;
    private final List<SpriteBuilderImpl> sprites = new ArrayList<>();
    private X customData;
    private Consumer<TextureInstance<X>> callback, subTextureCallback;
    private final List<TextureOutputImpl<?>> subTextures = new ArrayList<>();
    private boolean isMarkedDefault;

    private SpriteBuilderImpl activeSprite;
    private TextureOutputImpl<?> activeSubTexture;
    private boolean hasDefaultSprite = false;

    public TextureOutputImpl(ResourceLocation identifier, TextureType<?,X> textureType){
        this.identifier = identifier;
        this.textureType = textureType;
    }

    public ResourceLocation getIdentifier(){
        return this.identifier;
    }

    public TextureType<?,X> getTextureType(){
        return this.textureType;
    }

    @Override
    public SpriteBuilder createSprite(){
        if(this.activeSprite != null)
            throw new IllegalStateException("Last sprite has not yet been submitted!");
        if(this.activeSubTexture != null)
            throw new IllegalStateException("Last sub-texture has not yet been submitted!");
        this.activeSprite = new SpriteBuilderImpl(this);
        return this.activeSprite;
    }

    public void submitSprite(){
        if(this.hasDefaultSprite && this.activeSprite.isMarkedDefault())
            throw new IllegalStateException("Only one sprite or sub-texture can be marked as default!");
        this.hasDefaultSprite |= this.activeSprite.isMarkedDefault();
        this.sprites.add(this.activeSprite);
        this.activeSprite = null;
    }

    private void submitSubTexture(){
        if(this.activeSubTexture.isMarkedDefault && this.hasDefaultSprite)
            throw new IllegalStateException("Only one sprite or sub-texture can be marked as default!");
        this.hasDefaultSprite |= this.activeSubTexture.isMarkedDefault;
        this.subTextures.add(this.activeSubTexture);
        this.activeSubTexture = null;
    }

    public void finish(){
        if(this.activeSprite != null)
            throw new IllegalStateException("Unsubmitted sprite remaining!");
        if(this.activeSubTexture != null)
            throw new IllegalStateException("Unsubmitted sub-texture remaining!");
        if(this.sprites.isEmpty() && this.subTextures.isEmpty())
            throw new IllegalStateException("No sprites or sub-texture have been submitted! Texture must have at least one sprite or sub-texture!");

        // Make sure we have a sprite or sub-texture marked as default
        if(!this.hasDefaultSprite){
            if(!this.sprites.isEmpty())
                this.sprites.get(0).markDefaultUnchecked();
            else
                this.subTextures.get(0).isMarkedDefault = true;
        }

        // Identifiers for all sprites
        Set<String> names = new HashSet<>();
        int nameIndex = 0;
        for(SpriteBuilderImpl sprite : this.sprites){
            if(!sprite.isMarkedDefault() && sprite.getName() != null && !names.add(sprite.getName()))
                throw new RuntimeException("Duplicate sprite name '" + sprite.getName() + "'!");
        }
        for(SpriteBuilderImpl sprite : this.sprites){
            if(sprite.isMarkedDefault())
                sprite.setIdentifier(this.identifier);
            else if(sprite.getName() != null)
                sprite.setIdentifier(IdentifierUtil.withSuffix(this.identifier, "_" + sprite.getName()));
            else{
                String name = "sub_sprite_" + nameIndex++;
                while(names.contains(name))
                    name = "sub_sprite_" + nameIndex++;
                sprite.setIdentifier(IdentifierUtil.withSuffix(this.identifier, "_" + name));
            }
        }
        this.overwriteDefaultSpriteIdentifier(this.identifier);
    }

    private void overwriteDefaultSpriteIdentifier(ResourceLocation identifier){
        for(SpriteBuilderImpl sprite : this.sprites){
            if(sprite.isMarkedDefault()){
                sprite.setIdentifier(identifier);
                return;
            }
        }
        for(TextureOutputImpl<?> subTexture : this.subTextures){
            if(subTexture.isMarkedDefault){
                subTexture.overwriteDefaultSpriteIdentifier(identifier);
                return;
            }
        }
    }

    public List<SpriteBuilderImpl> getSprites(){
        return this.sprites;
    }

    public List<TextureOutputImpl<?>> getSubTextures(){
        return this.subTextures;
    }

    public boolean isMarkedDefault(){
        return this.isMarkedDefault;
    }

    public X getCustomData(){
        return this.customData;
    }

    public Consumer<TextureInstance<X>> getCreationCallback(){
        return this.callback;
    }

    public Consumer<TextureInstance<X>> getSubTextureCallback(){
        return this.subTextureCallback;
    }

    @Override
    public void setCustomData(X customData){
        this.customData = customData;
    }

    @Override
    public void setCreationCallback(Consumer<TextureInstance<X>> callback){
        this.callback = callback;
    }

    @Override
    public <Y> SubTextureOutput<Y> createSubTexture(RawTextureInstance<?,Y> texture,
                                                    @Nullable String name,
                                                    BufferedImage image,
                                                    @Nullable AnimationMetadataSection animationMetadata) throws UserErrorException{
        if(this.activeSprite != null)
            throw new IllegalStateException("Last sprite has not yet been submitted!");
        if(this.activeSubTexture != null)
            throw new IllegalStateException("Last sub-texture has not yet been submitted!");

        ResourceLocation subIdentifier = name == null ?
            IdentifierUtil.withSuffix(this.identifier, "_sub_texture_" + (this.subTextures.size() + 1)) :
            IdentifierUtil.withSuffix(this.identifier, "_" + name);
        TextureOutputImpl<Y> subOutput = new TextureOutputImpl<>(subIdentifier, texture.getTextureType());
        TextureCreationContextImpl context = new TextureCreationContextImpl(subIdentifier, image, animationMetadata);
        try{
            texture.createTexture(subOutput, context);
            subOutput.finish();
        }catch(UserErrorException e){
            throw new UserErrorException("Failed to create sub-texture of type '" + TextureTypeRegistryImpl.getIdentifier(texture.getTextureType()) + "'", e);
        }catch(Exception e){
            throw new RuntimeException("Encountered an exception whilst creating sub-texture of type '" + TextureTypeRegistryImpl.getIdentifier(texture.getTextureType()) + "' for texture '" + this.identifier + "'!", e);
        }
        this.activeSubTexture = subOutput;
        return subOutput.new SubTexture(this);
    }

    private class SubTexture implements SubTextureOutput<X> {

        private final TextureOutputImpl<?> parent;

        private boolean valid = true;

        private SubTexture(TextureOutputImpl<?> parent){
            this.parent = parent;
        }

        private void checkValid(){
            if(!this.valid)
                throw new IllegalStateException("Sub-texture has already been submitted!");
        }

        @Override
        public SubTextureOutput<X> markDefault(boolean markDefault){
            this.checkValid();
            TextureOutputImpl.this.isMarkedDefault = markDefault;
            return this;
        }

        @Override
        public SubTextureOutput<X> setCreationCallback(Consumer<TextureInstance<X>> callback){
            this.checkValid();
            TextureOutputImpl.this.subTextureCallback = callback;
            return this;
        }

        @Override
        public void submit(){
            this.checkValid();
            this.valid = false;
            this.parent.submitSubTexture();
        }
    }
}
