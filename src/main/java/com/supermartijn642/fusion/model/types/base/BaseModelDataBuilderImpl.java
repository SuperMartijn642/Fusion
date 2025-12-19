package com.supermartijn642.fusion.model.types.base;

import com.supermartijn642.fusion.api.model.data.BaseModelData;
import com.supermartijn642.fusion.api.model.data.BaseModelDataBuilder;
import com.supermartijn642.fusion.util.TextureAtlases;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.Identifier;

import java.util.*;

/**
 * Created 06/09/2024 by SuperMartijn642
 */
public class BaseModelDataBuilderImpl implements BaseModelDataBuilder<BaseModelDataBuilderImpl,BaseModelData> {

    private final Set<Identifier> parents = new LinkedHashSet<>(); // This should maintain insertion order
    private final Map<String,String> textures = new HashMap<>();

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
    public BaseModelDataBuilderImpl texture(String key, String reference){
        if(!key.matches("[a-zA-Z_]*"))
            throw new IllegalArgumentException("Texture reference must only contain characters [a-zA-Z_]!");

        // Prepend '#' character
        if(reference.charAt(0) != '#')
            reference = '#' + reference;
        if(this.textures.containsKey(key))
            throw new RuntimeException("Duplicate texture entry for key '" + key + "': '" + this.textures.get(key) + "' and '" + reference + "'!");

        this.textures.put(key, reference);
        return this;
    }

    @Override
    public BaseModelDataBuilderImpl texture(String key, Identifier texture){
        if(!key.matches("[a-zA-Z_]*"))
            throw new IllegalArgumentException("Texture reference must only contain characters [a-zA-Z_]!");
        if(this.textures.containsKey(key))
            throw new RuntimeException("Duplicate texture entry for key '" + key + "': '" + this.textures.get(key) + "' and '" + texture + "'!");

        this.textures.put(key, texture.toString());
        return this;
    }

    @Override
    public BaseModelData build(){
        List<Identifier> parents = new ArrayList<>(this.parents);
        // Create a vanilla model representation of the properties
        Identifier parent = parents.isEmpty() ? null : parents.get(0);
        TextureSlots.Data.Builder textures = new TextureSlots.Data.Builder();
        this.textures.forEach((key, value) -> {
            if(value.charAt(0) == '#')
                textures.addReference(key, value);
            else
                textures.addTexture(key, new Material(TextureAtlases.getBlocks(), Identifier.parse(value)));
        });
        BlockModel vanillaModel = new BlockModel(null, null, null, null, textures.build(), parent);
        return new BaseModelDataImpl(vanillaModel, parents, Collections.emptyList());
    }
}
