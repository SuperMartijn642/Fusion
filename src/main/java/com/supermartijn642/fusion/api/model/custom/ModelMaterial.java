package com.supermartijn642.fusion.api.model.custom;

import com.supermartijn642.fusion.model.custom.ModelMaterialImpl;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.Material;
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
     * Creates a material instance for the given texture and force translucent property.
     * @param texture          sprite identifier for the material
     * @param forceTranslucent whether the material should be translucent even if the sprite is not
     */
    static ModelMaterial of(Identifier texture, boolean forceTranslucent){
        return ModelMaterialImpl.of(texture, forceTranslucent);
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
    static ModelMaterial missing(){
        return ModelMaterialImpl.MISSING;
    }

    /**
     * Sprite identifier for this material.
     */
    Identifier texture();

    /**
     * Whether the material should be translucent even if the sprite is not.
     */
    boolean forceTranslucent();

    /**
     * Converts this material to a vanilla material.
     */
    default Material toMaterial(){
        return new Material(this.texture(), this.forceTranslucent());
    }

    /**
     * Whether this material uses the missing texture atlas sprite.
     */
    default boolean isMissing(){
        return this.equals(missing());
    }

    /**
     * Resolved version of a model material, holding a reference to the texture atlas sprite.
     */
    @ApiStatus.NonExtendable
    interface Resolved {

        /**
         * Creates a resolved material instance for the given sprite and force translucent property.
         * @param sprite           sprite for the material
         * @param forceTranslucent whether the material should be translucent even if the sprite is not
         */
        static Resolved of(TextureAtlasSprite sprite, boolean forceTranslucent){
            return ModelMaterialImpl.of(sprite, forceTranslucent);
        }

        /**
         * Converts the given {@link Material.Baked} to a {@link Resolved} instance.
         */
        static Resolved of(Material.Baked material){
            return ModelMaterialImpl.of(material);
        }

        /**
         * Sprite for this material.
         */
        TextureAtlasSprite sprite();

        /**
         * Whether the material should be translucent even if the sprite is not.
         */
        boolean forceTranslucent();

        /**
         * Converts this resolved material to a vanilla baked material.
         */
        default Material.Baked toBakedMaterial(){
            return new Material.Baked(this.sprite(), this.forceTranslucent());
        }

        /**
         * Whether this material uses the missing texture atlas sprite.
         */
        boolean isMissing();
    }
}
