package com.supermartijn642.fusion.api.model.custom;

import com.supermartijn642.fusion.model.custom.ModelMaterialImpl;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

/**
 * Representation of a material used in model baking.
 * <p>
 * Created 29/04/2023 by SuperMartijn642
 */
@ApiStatus.NonExtendable
public interface ModelMaterial {

    /**
     * Creates a material instance for the given texture.
     * @param texture sprite identifier for the material
     */
    static ModelMaterial of(ResourceLocation texture){
        return ModelMaterialImpl.of(texture);
    }

    /**
     * The material for the missing texture atlas sprite.
     */
    static ModelMaterial missing(){
        return ModelMaterialImpl.missing();
    }

    static boolean isMissingSprite(TextureAtlasSprite sprite){
        return ModelMaterialImpl.isMissingSprite(sprite);
    }

    /**
     * Sprite identifier for this material.
     */
    ResourceLocation texture();

    /**
     * Whether this material uses the missing texture atlas sprite.
     */
    default boolean isMissing(){
        return this.texture().equals(missing().texture());
    }
}
