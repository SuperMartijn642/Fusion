package com.supermartijn642.fusion.api.model;

import com.supermartijn642.fusion.model.ModelMaterialImpl;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;

/**
 * Created 29/04/2023 by SuperMartijn642
 */
public interface ModelMaterial {

    static ModelMaterial of(Identifier texture, boolean forceTranslucent){
        return ModelMaterialImpl.of(texture, forceTranslucent);
    }

    static ModelMaterial of(Material material){
        return ModelMaterialImpl.of(material);
    }

    /**
     * @return the identifier for the missing texture sprite in the block atlas
     */
    static ModelMaterial missing(){
        return ModelMaterialImpl.MISSING;
    }

    Identifier getTexture();

    boolean forceTranslucent();

    default Material toMaterial(){
        return new Material(this.getTexture(), this.forceTranslucent());
    }
}
