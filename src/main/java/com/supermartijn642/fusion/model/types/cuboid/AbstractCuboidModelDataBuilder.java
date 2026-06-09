package com.supermartijn642.fusion.model.types.cuboid;

import com.supermartijn642.fusion.api.model.custom.ModelMaterial;
import com.supermartijn642.fusion.api.model.custom.geometry.CuboidModelGeometry;
import com.supermartijn642.fusion.api.model.types.CuboidModelDataBuilder;
import com.supermartijn642.fusion.api.util.Either;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverride;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

/**
 * Created 06/05/2026 by SuperMartijn642
 */
public abstract class AbstractCuboidModelDataBuilder<T extends AbstractCuboidModelDataBuilder<T,S>, S> implements CuboidModelDataBuilder<T,S> {

    protected ResourceLocation parent;
    protected final Map<String,Either<String,ModelMaterial>> materials = new HashMap<>();
    protected List<CuboidModelGeometry.Element> elements;
    protected BlockModel.GuiLight guiLight;
    protected Boolean ambientOcclusion;
    protected final Map<ItemTransforms.TransformType,ItemTransform> itemTransforms = new EnumMap<>(ItemTransforms.TransformType.class);
    protected final List<ItemOverride> itemOverrides = new ArrayList<>();

    @Override
    public T parent(ResourceLocation parent){
        this.parent = parent;
        return this.self();
    }

    @Override
    public T material(String key, Either<String,ModelMaterial> material){
        if(!key.matches("[a-zA-Z_]*"))
            throw new IllegalArgumentException("Material key must only contain characters [a-zA-Z_]!");
        if(this.materials.containsKey(key)){
            Either<String,ModelMaterial> existing = this.materials.get(key);
            throw new RuntimeException("Duplicate materials entry for key '" + key + "': '" + existing.flatMap(r -> '#' + r, m -> m.texture().toString()) + "' and '" + material.flatMap(r -> r.isEmpty() || r.charAt(0) != '#' ? '#' + r : r, m -> m.texture().toString()) + "'!");
        }

        // Remove '#' character from references
        if(material.isLeft() && !material.left().isEmpty() && material.left().charAt(0) == '#')
            this.materials.put(key, Either.left(material.left().substring(1)));
        else
            this.materials.put(key, material);
        return this.self();
    }

    @Override
    public T elements(CuboidModelGeometry.Element... elements){
        if(this.elements == null)
            this.elements = new ArrayList<>();
        this.elements.addAll(Arrays.asList(elements));
        return this.self();
    }

    @Override
    public T guiLight(BlockModel.GuiLight guiLight){
        this.guiLight = guiLight;
        return this.self();
    }

    @Override
    public T ambientOcclusion(Boolean ambientOcclusion){
        this.ambientOcclusion = ambientOcclusion;
        return this.self();
    }

    @Override
    public T itemTransform(ItemTransforms.TransformType type, ItemTransform transform){
        if(transform == null)
            this.itemTransforms.remove(type);
        else
            this.itemTransforms.put(type, transform);
        return this.self();
    }

    @Override
    public T itemTransforms(ItemTransforms itemTransforms){
        this.itemTransforms.clear();
        for(ItemTransforms.TransformType type : ItemTransforms.TransformType.values())
            this.itemTransforms.put(type, itemTransforms.getTransform(type));
        return this.self();
    }

    @Override
    public T itemOverrides(ItemOverride... overrides){
        this.itemOverrides.addAll(Arrays.asList(overrides));
        return this.self();
    }

    private T self(){
        //noinspection unchecked
        return (T)this;
    }
}
