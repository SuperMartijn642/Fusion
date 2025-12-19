package com.supermartijn642.fusion.model.types.vanilla;

import com.supermartijn642.fusion.api.model.data.VanillaModelDataBuilder;
import com.supermartijn642.fusion.util.TextureAtlases;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * Created 01/05/2023 by SuperMartijn642
 */
public class VanillaModelDataBuilderImpl implements VanillaModelDataBuilder<VanillaModelDataBuilderImpl,BlockModel> {

    private final Map<String,String> textures = new HashMap<>();
    private Identifier parent;

    @Override
    public VanillaModelDataBuilderImpl parent(Identifier parent){
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
    public VanillaModelDataBuilderImpl texture(String key, Identifier texture){
        if(!key.matches("[a-zA-Z_]*"))
            throw new IllegalArgumentException("Texture reference must only contain characters [a-zA-Z_]!");
        if(this.textures.containsKey(key))
            throw new RuntimeException("Duplicate texture entry for key '" + key + "': '" + this.textures.get(key) + "' and '" + texture + "'!");

        this.textures.put(key, texture.toString());
        return this;
    }

    @Override
    public BlockModel build(){
        TextureSlots.Data.Builder textures = new TextureSlots.Data.Builder();
        this.textures.forEach((key, value) -> {
            if(value.charAt(0) == '#')
                textures.addReference(key, value);
            else
                textures.addTexture(key, new Material(TextureAtlases.getBlocks(), Identifier.parse(value)));
        });
        return new BlockModel(null, null, null, null, textures.build(), this.parent);
    }
}
