package com.supermartijn642.fusion.model.custom;

import com.supermartijn642.fusion.api.model.custom.ModelMaterial;
import net.minecraft.client.renderer.model.Material;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.MissingTextureSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ResourceLocation;

/**
 * Created 30/04/2023 by SuperMartijn642
 */
public class ModelMaterialImpl implements ModelMaterial {

    private static final ModelMaterial MISSING = new ModelMaterialImpl(AtlasTexture.LOCATION_BLOCKS, MissingTextureSprite.getLocation()) {
        @Override
        public boolean isMissing(){
            return true;
        }
    };

    public static ModelMaterial of(ResourceLocation atlas, ResourceLocation texture){
        if(texture.equals(MISSING.texture()) && atlas.equals(MISSING.atlas()))
            return MISSING;
        return new ModelMaterialImpl(atlas, texture);
    }

    public static ModelMaterial blockAtlas(ResourceLocation texture){
        if(texture.equals(MISSING.texture()))
            return MISSING;
        return new ModelMaterialImpl(MISSING.atlas(), texture);
    }

    public static ModelMaterial of(Material material){
        if(material.texture().equals(MISSING.texture()) && material.atlasLocation().equals(MISSING.atlas()))
            return MISSING;
        return new ModelMaterialImpl(material);
    }

    public static ModelMaterial missing(ResourceLocation atlas){
        if(atlas.equals(MISSING.atlas()))
            return MISSING;
        return new ModelMaterialImpl(atlas, MissingTextureSprite.getLocation());
    }

    public static ModelMaterial missingBlockAtlas(){
        return MISSING;
    }

    public static boolean isMissingSprite(TextureAtlasSprite sprite){
        return sprite.getName().equals(MISSING.texture());
    }

    private final ResourceLocation atlas;
    private final ResourceLocation texture;
    private Material material;

    private ModelMaterialImpl(ResourceLocation atlas, ResourceLocation texture){
        this.atlas = atlas;
        this.texture = texture;
    }

    private ModelMaterialImpl(Material material){
        this(material.atlasLocation(), material.texture());
        this.material = material;
    }

    @Override
    public ResourceLocation atlas(){
        return this.atlas;
    }

    @Override
    public ResourceLocation texture(){
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
        if(!(o instanceof ModelMaterialImpl)) return false;

        ModelMaterialImpl that = (ModelMaterialImpl)o;
        return this.atlas.equals(that.atlas) && this.texture.equals(that.texture);
    }

    @Override
    public int hashCode(){
        int result = this.atlas.hashCode();
        result = 31 * result + this.texture.hashCode();
        return result;
    }
}
