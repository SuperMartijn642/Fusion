package com.supermartijn642.fusion.model.types.base;

import com.supermartijn642.fusion.api.model.ModelMaterial;
import com.supermartijn642.fusion.api.model.data.BaseModelData;
import com.supermartijn642.fusion.api.model.data.BaseModelDataBuilder;
import com.supermartijn642.fusion.api.util.Either;
import net.minecraft.client.resources.model.cuboid.CuboidModel;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.resources.Identifier;

import java.util.*;

/**
 * Created 06/09/2024 by SuperMartijn642
 */
public class BaseModelDataBuilderImpl implements BaseModelDataBuilder<BaseModelDataBuilderImpl,BaseModelData> {

    private final Set<Identifier> parents = new LinkedHashSet<>(); // This should maintain insertion order
    private final Map<String,Either<String,ModelMaterial>> textures = new HashMap<>();

    @Override
    public BaseModelDataBuilderImpl parent(Identifier parent){
        return this.parents(parent);
    }

    @Override
    public BaseModelDataBuilderImpl parents(Identifier... parents){
        this.parents.addAll(Arrays.asList(parents));
        return this;
    }

    @Override
    public BaseModelDataBuilderImpl material(String key, String reference){
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
    public BaseModelDataBuilderImpl material(String key, Identifier texture, boolean forceTranslucent){
        if(!key.matches("[a-zA-Z_]*"))
            throw new IllegalArgumentException("Texture reference must only contain characters [a-zA-Z_]!");
        if(this.textures.containsKey(key))
            throw new RuntimeException("Duplicate texture entry for key '" + key + "': '" + this.textures.get(key) + "' and '" + texture + "'!");

        this.textures.put(key, Either.right(ModelMaterial.of(texture, forceTranslucent)));
        return this;
    }

    @Override
    public BaseModelData build(){
        List<Identifier> parents = new ArrayList<>(this.parents);
        // Create a vanilla model representation of the properties
        Identifier parent = parents.isEmpty() ? null : parents.get(0);
        TextureSlots.Data.Builder textures = new TextureSlots.Data.Builder();
        this.textures.forEach((key, value) -> {
            if(value.isLeft())
                textures.addReference(key, value.left());
            else
                textures.addTexture(key, value.right().toMaterial());
        });
        CuboidModel vanillaModel = new CuboidModel(null, null, null, null, textures.build(), parent);
        return new BaseModelDataImpl(vanillaModel, parents, Collections.emptyList());
    }
}
