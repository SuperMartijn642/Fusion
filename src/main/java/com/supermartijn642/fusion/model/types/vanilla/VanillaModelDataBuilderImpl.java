package com.supermartijn642.fusion.model.types.vanilla;

import com.supermartijn642.fusion.api.model.data.VanillaModelDataBuilder;
import net.minecraft.client.resources.model.cuboid.CuboidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * Created 01/05/2023 by SuperMartijn642
 */
public class VanillaModelDataBuilderImpl implements VanillaModelDataBuilder<VanillaModelDataBuilderImpl,CuboidModel> {

    private final Map<String,TextureSlots.SlotContents> textures = new HashMap<>();
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

        // Strip '#' character
        if(reference.charAt(0) == '#')
            reference = reference.substring(1);
        if(this.textures.containsKey(key))
            throw new RuntimeException("Duplicate texture entry for key '" + key + "': '" + this.textures.get(key) + "' and '" + reference + "'!");

        this.textures.put(key, new TextureSlots.Reference(reference));
        return this;
    }

    @Override
    public VanillaModelDataBuilderImpl texture(String key, Material material){
        if(!key.matches("[a-zA-Z_]*"))
            throw new IllegalArgumentException("Texture reference must only contain characters [a-zA-Z_]!");
        if(this.textures.containsKey(key))
            throw new RuntimeException("Duplicate texture entry for key '" + key + "': '" + this.textures.get(key) + "' and '" + material + "'!");

        this.textures.put(key, new TextureSlots.Value(material));
        return this;
    }

    @Override
    public CuboidModel build(){
        TextureSlots.Data textures = new TextureSlots.Data(Map.copyOf(this.textures));
        return new CuboidModel(null, null, null, null, textures, this.parent);
    }
}
