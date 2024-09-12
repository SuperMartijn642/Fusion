package com.supermartijn642.fusion.model.types.base;

import com.mojang.datafixers.util.Either;
import com.supermartijn642.fusion.api.model.data.BaseModelData;
import com.supermartijn642.fusion.api.model.data.BaseModelDataBuilder;
import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.util.TextureAtlases;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created 06/09/2024 by SuperMartijn642
 */
public class BaseModelDataBuilderImpl implements BaseModelDataBuilder<BaseModelDataBuilderImpl,BaseModelData> {

    private final Set<ResourceLocation> parents = new LinkedHashSet<>(); // This should maintain insertion order
    private final Map<String,String> textures = new HashMap<>();

    @Override
    public BaseModelDataBuilderImpl parent(ResourceLocation parent){
        return this.parents(parent);
    }

    @Override
    public BaseModelDataBuilderImpl parents(ResourceLocation... parents){
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
    public BaseModelDataBuilderImpl texture(String key, ResourceLocation texture){
        if(!key.matches("[a-zA-Z_]*"))
            throw new IllegalArgumentException("Texture reference must only contain characters [a-zA-Z_]!");
        if(this.textures.containsKey(key))
            throw new RuntimeException("Duplicate texture entry for key '" + key + "': '" + this.textures.get(key) + "' and '" + texture + "'!");

        this.textures.put(key, texture.toString());
        return this;
    }

    @Override
    public BaseModelData build(){
        List<ResourceLocation> parents = new ArrayList<>(this.parents);
        // Create a vanilla model representation of the properties
        ResourceLocation parent = parents.isEmpty() ? null : parents.get(0);
        Map<String,Either<Material,String>> textures = this.textures.entrySet().stream()
            .map(entry -> Pair.of(entry.getKey(), entry.getValue()))
            .map(pair -> pair.<Either<Material,String>>mapRight(s -> s.charAt(0) == '#' ? Either.right(s) : Either.left(new Material(TextureAtlases.getBlocks(), ResourceLocation.parse(s)))))
            .collect(Collectors.toMap(Pair::left, Pair::right));
        BlockModel vanillaModel = new BlockModel(parent, Collections.emptyList(), textures, null, null, ItemTransforms.NO_TRANSFORMS, Collections.emptyList());
        return new BaseModelDataImpl(vanillaModel, parents, Collections.emptyList());
    }
}
