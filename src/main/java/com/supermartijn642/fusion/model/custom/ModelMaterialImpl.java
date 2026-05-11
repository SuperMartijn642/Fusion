package com.supermartijn642.fusion.model.custom;

import com.supermartijn642.fusion.api.model.custom.ModelMaterial;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;

/**
 * Created 30/04/2023 by SuperMartijn642
 */
public class ModelMaterialImpl implements ModelMaterial {

    public static final ModelMaterial MISSING = new ModelMaterialImpl(MissingTextureAtlasSprite.getLocation(), false) {
        @Override
        public boolean isMissing(){
            return true;
        }
    };

    public static ModelMaterial of(Identifier texture, boolean forceTranslucent){
        if(texture.equals(MISSING.texture()))
            return MISSING;
        return new ModelMaterialImpl(texture, forceTranslucent);
    }

    public static ModelMaterial of(Material material){
        if(material.sprite().equals(MISSING.texture()))
            return MISSING;
        return new ModelMaterialImpl(material);
    }

    public static ModelMaterial.Resolved of(TextureAtlasSprite sprite, boolean forceTranslucent){
        return new Resolved(sprite, forceTranslucent);
    }

    public static ModelMaterial.Resolved of(Material.Baked material){
        return new Resolved(material);
    }

    private final Identifier texture;
    private final boolean forceTranslucent;
    private Material material;

    private ModelMaterialImpl(Identifier texture, boolean forceTranslucent){
        this.texture = texture;
        this.forceTranslucent = forceTranslucent;
    }

    private ModelMaterialImpl(Material material){
        this(material.sprite(), material.forceTranslucent());
        this.material = material;
    }

    @Override
    public Identifier texture(){
        return this.texture;
    }

    @Override
    public boolean forceTranslucent(){
        return this.forceTranslucent;
    }

    @Override
    public Material toMaterial(){
        return this.material == null ? (this.material = ModelMaterial.super.toMaterial()) : this.material;
    }

    @Override
    public boolean isMissing(){
        return this.texture.equals(MISSING.texture());
    }

    @Override
    public final boolean equals(Object o){
        if(!(o instanceof ModelMaterialImpl that)) return false;

        return (this.forceTranslucent == that.forceTranslucent || this.isMissing()) && this.texture.equals(that.texture);
    }

    @Override
    public int hashCode(){
        int result = this.texture.hashCode();
        if(!this.isMissing())
            result = 31 * result + Boolean.hashCode(this.forceTranslucent);
        return result;
    }

    private static class Resolved implements ModelMaterial.Resolved {

        private final TextureAtlasSprite sprite;
        private final boolean forceTranslucent;
        private Material.Baked material;

        private Resolved(TextureAtlasSprite sprite, boolean forceTranslucent){
            this.sprite = sprite;
            this.forceTranslucent = forceTranslucent;
        }

        private Resolved(Material.Baked material){
            this(material.sprite(), material.forceTranslucent());
            this.material = material;
        }

        @Override
        public TextureAtlasSprite sprite(){
            return this.sprite;
        }

        @Override
        public boolean forceTranslucent(){
            return this.forceTranslucent;
        }

        @Override
        public Material.Baked toBakedMaterial(){
            return this.material == null ? (this.material = ModelMaterial.Resolved.super.toBakedMaterial()) : this.material;
        }

        @Override
        public boolean isMissing(){
            return this.sprite.contents().name().equals(MISSING.texture());
        }

        @Override
        public final boolean equals(Object o){
            if(!(o instanceof Resolved that)) return false;

            return this.forceTranslucent == that.forceTranslucent && this.sprite.contents().name().equals(that.sprite.contents().name());
        }

        @Override
        public int hashCode(){
            int result = this.sprite.contents().name().hashCode();
            result = 31 * result + Boolean.hashCode(this.forceTranslucent);
            return result;
        }
    }
}
