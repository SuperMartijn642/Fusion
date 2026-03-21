package com.supermartijn642.fusion.texture.custom;

import com.supermartijn642.fusion.api.texture.custom.SpriteBuilder;
import com.supermartijn642.fusion.api.texture.custom.TextureInstance;
import com.supermartijn642.fusion.api.texture.custom.TextureOutput;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Created 23/03/2026 by SuperMartijn642
 */
public class TextureOutputImpl implements TextureOutput<Object> {

    private final List<SpriteBuilderImpl> sprites = new ArrayList<>();
    private Object customData;
    private Consumer<TextureInstance<Object>> callback;

    private SpriteBuilderImpl activeSprite;
    private boolean hasDefaultSprite = false;

    @Override
    public SpriteBuilder createSprite(){
        if(this.activeSprite != null)
            throw new IllegalStateException("Last sprite has not yet been submitted!");
        this.activeSprite = new SpriteBuilderImpl(this);
        return this.activeSprite;
    }

    public void submitSprite(){
        if(this.hasDefaultSprite && this.activeSprite.isMarkedDefault())
            throw new IllegalStateException("Only one sprite can be marked as default!");
        this.hasDefaultSprite |= this.activeSprite.isMarkedDefault();
        this.sprites.add(this.activeSprite);
        this.activeSprite = null;
    }

    public void checkFinished(){
        if(this.activeSprite != null)
            throw new IllegalStateException("Unsubmitted sprite remaining!");
    }

    public List<SpriteBuilderImpl> getSprites(){
        return this.sprites;
    }

    public Object getCustomData(){
        return this.customData;
    }

    public Consumer<TextureInstance<Object>> getCallback(){
        return this.callback;
    }

    @Override
    public void setCustomData(Object customData){
        this.customData = customData;
    }

    @Override
    public void setCreationCallback(Consumer<TextureInstance<Object>> callback){
        this.callback = callback;
    }
}
