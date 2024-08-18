package com.supermartijn642.fusion.model.types.vanilla;

import com.mojang.datafixers.util.Either;
import com.supermartijn642.fusion.api.model.data.VanillaModelDataBuilder;
import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.util.TextureAtlases;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created 01/05/2023 by SuperMartijn642
 */
public class VanillaModelDataBuilderImpl implements VanillaModelDataBuilder<VanillaModelDataBuilderImpl,BlockModel> {

    private final Map<String,String> textures = new HashMap<>();
    private ResourceLocation parent;

    @Override
    public VanillaModelDataBuilderImpl parent(ResourceLocation parent){
        this.parent = parent;
        return this;
    }

    @Override
    public VanillaModelDataBuilderImpl texture(String key, String reference){
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
    public VanillaModelDataBuilderImpl texture(String key, ResourceLocation texture){
        if(!key.matches("[a-zA-Z_]*"))
            throw new IllegalArgumentException("Texture reference must only contain characters [a-zA-Z_]!");
        if(this.textures.containsKey(key))
            throw new RuntimeException("Duplicate texture entry for key '" + key + "': '" + this.textures.get(key) + "' and '" + texture + "'!");

        this.textures.put(key, texture.toString());
        return this;
    }

    @Override
    public BlockModel build(){
        Map<String,Either<Material,String>> textures = this.textures.entrySet().stream()
            .map(entry -> Pair.of(entry.getKey(), entry.getValue()))
            .map(pair -> pair.<Either<Material,String>>mapRight(s -> s.charAt(0) == '#' ? Either.right(s) : Either.left(new Material(TextureAtlases.getBlocks(), ResourceLocation.parse(s)))))
            .collect(Collectors.toMap(Pair::left, Pair::right));
        return new BlockModel(this.parent, Collections.emptyList(), textures, null, null, ItemTransforms.NO_TRANSFORMS, Collections.emptyList());
    }
}
