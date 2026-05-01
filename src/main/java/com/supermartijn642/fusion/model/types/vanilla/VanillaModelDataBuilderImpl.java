package com.supermartijn642.fusion.model.types.vanilla;

import com.supermartijn642.fusion.api.model.ModelMaterial;
import com.supermartijn642.fusion.api.model.data.VanillaModelDataBuilder;
import com.supermartijn642.fusion.api.util.Either;
import net.minecraft.client.resources.model.cuboid.CuboidModel;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * Created 01/05/2023 by SuperMartijn642
 */
public class VanillaModelDataBuilderImpl implements VanillaModelDataBuilder<VanillaModelDataBuilderImpl,CuboidModel> {

    private final Map<String,Either<String,ModelMaterial>> textures = new HashMap<>();
    private Identifier parent;

    @Override
    public VanillaModelDataBuilderImpl parent(Identifier parent){
        this.parent = parent;
        return this;
    }

    @Override
    public VanillaModelDataBuilderImpl material(String key, String reference){
        if(!key.matches("[a-zA-Z_]*"))
            throw new IllegalArgumentException("Texture reference must only contain characters [a-zA-Z_]!");

        // Remove '#' character
        if(reference.charAt(0) == '#')
            reference = reference.substring(1);
        if(this.textures.containsKey(key))
            throw new RuntimeException("Duplicate texture entry for key '" + key + "': '" + this.textures.get(key) + "' and '" + reference + "'!");

        this.textures.put(key, Either.left(reference));
        return this;
    }

    @Override
    public VanillaModelDataBuilderImpl material(String key, Identifier texture, boolean forceTranslucent){
        if(!key.matches("[a-zA-Z_]*"))
            throw new IllegalArgumentException("Texture reference must only contain characters [a-zA-Z_]!");
        if(this.textures.containsKey(key))
            throw new RuntimeException("Duplicate texture entry for key '" + key + "': '" + this.textures.get(key) + "' and '" + texture + "'!");

        this.textures.put(key, Either.right(ModelMaterial.of(texture, forceTranslucent)));
        return this;
    }

    @Override
    public CuboidModel build(){
        TextureSlots.Data.Builder textures = new TextureSlots.Data.Builder();
        this.textures.forEach((key, value) -> {
            if(value.isLeft())
                textures.addReference(key, value.left());
            else
                textures.addTexture(key, value.right().toMaterial());
        });
        return new CuboidModel(null, null, null, null, textures.build(), this.parent);
    }
}
