package com.supermartijn642.fusion.model;

import com.supermartijn642.fusion.api.model.ModelMaterial;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;

/**
 * Created 30/04/2023 by SuperMartijn642
 */
public class ModelMaterialImpl implements ModelMaterial {

    public static final ModelMaterial MISSING = of(MissingTextureAtlasSprite.getLocation(), false);

    public static ModelMaterial of(Identifier texture, boolean forceTranslucent){
        return new ModelMaterialImpl(texture, forceTranslucent);
    }

    public static ModelMaterial of(Material material){
        return new ModelMaterialImpl(material);
    }

    private final Identifier texture;
    private final boolean forceTranslucent;
    private Material material;

    private ModelMaterialImpl(Identifier texture, boolean forceTranslucent) {
        this.texture = texture;
        this.forceTranslucent = forceTranslucent;
    }

    private ModelMaterialImpl(Material material){
        this(material.sprite(), material.forceTranslucent());
        this.material = material;
    }

    @Override
    public Identifier getTexture(){
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
    public final boolean equals(Object o){
        if(!(o instanceof ModelMaterialImpl that)) return false;

        return this.forceTranslucent == that.forceTranslucent && this.texture.equals(that.texture);
    }

    @Override
    public int hashCode(){
        int result = this.texture.hashCode();
        result = 31 * result + Boolean.hashCode(this.forceTranslucent);
        return result;
    }
}
