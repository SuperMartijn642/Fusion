package com.supermartijn642.fusion.model.custom;

import com.supermartijn642.fusion.api.model.custom.ModelMaterial;
import net.minecraft.client.renderer.texture.MissingTextureSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ResourceLocation;

/**
 * Created 30/04/2023 by SuperMartijn642
 */
public class ModelMaterialImpl implements ModelMaterial {

    private static final ModelMaterial MISSING = new ModelMaterialImpl(MissingTextureSprite.getLocation()) {
        @Override
        public boolean isMissing(){
            return true;
        }
    };

    public static ModelMaterial of(ResourceLocation texture){
        if(texture.equals(MISSING.texture()))
            return MISSING;
        return new ModelMaterialImpl(texture);
    }

    public static ModelMaterial missing(){
        return MISSING;
    }

    public static boolean isMissingSprite(TextureAtlasSprite sprite){
        return sprite.getName().equals(MISSING.texture());
    }

    private final ResourceLocation texture;

    private ModelMaterialImpl(ResourceLocation texture){
        this.texture = texture;
    }

    @Override
    public ResourceLocation texture(){
        return this.texture;
    }

    @Override
    public boolean isMissing(){
        return this.texture.equals(MISSING.texture());
    }

    @Override
    public final boolean equals(Object o){
        if(!(o instanceof ModelMaterialImpl)) return false;

        ModelMaterialImpl that = (ModelMaterialImpl)o;
        return this.texture.equals(that.texture());
    }

    @Override
    public int hashCode(){
        return this.texture.hashCode();
    }
}
