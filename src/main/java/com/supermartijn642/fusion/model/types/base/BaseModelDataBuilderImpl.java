package com.supermartijn642.fusion.model.types.base;

import com.supermartijn642.fusion.api.model.data.BaseModelData;
import com.supermartijn642.fusion.api.model.data.BaseModelDataBuilder;
import net.minecraft.client.resources.model.cuboid.CuboidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.resources.Identifier;

import java.util.*;

/**
 * Created 06/09/2024 by SuperMartijn642
 */
public class BaseModelDataBuilderImpl implements BaseModelDataBuilder<BaseModelDataBuilderImpl,BaseModelData> {

    private final Set<Identifier> parents = new LinkedHashSet<>(); // This should maintain insertion order
    private final Map<String, TextureSlots.SlotContents> textures = new HashMap<>();

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

        // Strip '#' character
        if(reference.charAt(0) == '#')
            reference = reference.substring(1);
        if(this.textures.containsKey(key))
            throw new RuntimeException("Duplicate texture entry for key '" + key + "': '" + this.textures.get(key) + "' and '" + reference + "'!");

        this.textures.put(key, new TextureSlots.Reference(reference));
        return this;
    }

    @Override
    public BaseModelDataBuilderImpl texture(String key, Material material){
        if(!key.matches("[a-zA-Z_]*"))
            throw new IllegalArgumentException("Texture reference must only contain characters [a-zA-Z_]!");
        if(this.textures.containsKey(key))
            throw new RuntimeException("Duplicate texture entry for key '" + key + "': '" + this.textures.get(key) + "' and '" + material + "'!");

        this.textures.put(key, new TextureSlots.Value(material));
        return this;
    }

    @Override
    public BaseModelData build(){
        List<Identifier> parents = new ArrayList<>(this.parents);
        // Create a vanilla model representation of the properties
        Identifier parent = parents.isEmpty() ? null : parents.get(0);
        TextureSlots.Data textures = new TextureSlots.Data(Map.copyOf(this.textures));
        CuboidModel vanillaModel = new CuboidModel(null, null, null, null, textures, parent);
        return new BaseModelDataImpl(vanillaModel, parents, Collections.emptyList());
    }
}
