package com.supermartijn642.fusion.api.model.custom;

import com.supermartijn642.fusion.model.custom.ModelMaterialImpl;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.ApiStatus;

/**
 * Representation of a material used in model baking.
 * <p>
 * Created 29/04/2023 by SuperMartijn642
 */
@ApiStatus.NonExtendable
public interface ModelMaterial {

    /**
     * Creates a material instance for the given texture and atlas.
     * @param atlas   atlas location for the material
     * @param texture sprite identifier for the material
     */
    static ModelMaterial of(Identifier atlas, Identifier texture){
        return ModelMaterialImpl.of(atlas, texture);
    }

    /**
     * Creates a material instance for the given texture and atlas.
     * @param texture sprite identifier for the material
     */
    static ModelMaterial blockOrItemAtlas(Identifier texture){
        return ModelMaterialImpl.blockOrItemAtlas(texture);
    }

    /**
     * Converts the given {@link Material} to a {@link ModelMaterial} instance.
     */
    static ModelMaterial of(Material material){
        return ModelMaterialImpl.of(material);
    }

    /**
     * The material for the missing texture atlas sprite.
     */
    static ModelMaterial missing(Identifier atlas){
        return ModelMaterialImpl.missing(atlas);
    }

    /**
     * The material for the missing texture atlas sprite.
     */
    static ModelMaterial missingBlockOrItemAtlas(){
        return ModelMaterialImpl.missingBlockOrItemAtlas();
    }

    static boolean isMissingSprite(TextureAtlasSprite sprite){
        return ModelMaterialImpl.isMissingSprite(sprite);
    }

    /**
     * Atlas location for this material.
     */
    Identifier atlas();

    /**
     * Sprite identifier for this material.
     */
    Identifier texture();

    /**
     * Converts this material to a vanilla material.
     */
    default Material toMaterial(){
        return new Material(this.atlas(), this.texture());
    }

    /**
     * Whether this material uses the missing texture atlas sprite.
     */
    default boolean isMissing(){
        return this.texture().equals(missingBlockOrItemAtlas().texture());
    }
}
