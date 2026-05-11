package com.supermartijn642.fusion.model.custom;

import com.supermartijn642.fusion.api.model.custom.ModelMaterial;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.resources.Identifier;

/**
 * Created 30/04/2023 by SuperMartijn642
 */
public class ModelMaterialImpl implements ModelMaterial {

    private static final ModelMaterial MISSING = new ModelMaterialImpl(ModelManager.BLOCK_OR_ITEM, MissingTextureAtlasSprite.getLocation()) {
        @Override
        public boolean isMissing(){
            return true;
        }
    };

    public static ModelMaterial of(Identifier atlas, Identifier texture){
        if(texture.equals(MISSING.texture()) && atlas.equals(MISSING.atlas()))
            return MISSING;
        return new ModelMaterialImpl(atlas, texture);
    }

    public static ModelMaterial blockOrItemAtlas(Identifier texture){
        if(texture.equals(MISSING.texture()))
            return MISSING;
        return new ModelMaterialImpl(ModelManager.BLOCK_OR_ITEM, texture);
    }

    public static ModelMaterial of(Material material){
        if(material.texture().equals(MISSING.texture()) && material.atlasLocation().equals(MISSING.atlas()))
            return MISSING;
        return new ModelMaterialImpl(material);
    }

    public static ModelMaterial missing(Identifier atlas){
        if(atlas.equals(ModelManager.BLOCK_OR_ITEM))
            return MISSING;
        return new ModelMaterialImpl(atlas, MissingTextureAtlasSprite.getLocation());
    }

    public static ModelMaterial missingBlockOrItemAtlas(){
        return MISSING;
    }

    public static boolean isMissingSprite(TextureAtlasSprite sprite){
        return sprite.contents().name().equals(MISSING.texture());
    }

    private final Identifier atlas;
    private final Identifier texture;
    private Material material;

    private ModelMaterialImpl(Identifier atlas, Identifier texture){
        this.atlas = atlas;
        this.texture = texture;
    }

    private ModelMaterialImpl(Material material){
        this(material.atlasLocation(), material.texture());
        this.material = material;
    }

    @Override
    public Identifier atlas(){
        return this.atlas;
    }

    @Override
    public Identifier texture(){
        return this.texture;
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

        return this.atlas.equals(that.atlas) && this.texture.equals(that.texture);
    }

    @Override
    public int hashCode(){
        int result = this.atlas.hashCode();
        result = 31 * result + this.texture.hashCode();
        return result;
    }
}
