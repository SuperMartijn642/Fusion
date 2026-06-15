package com.supermartijn642.fusion.model.types.composite;

import com.google.common.collect.ImmutableList;
import com.supermartijn642.fusion.api.model.custom.ModelMaterial;
import com.supermartijn642.fusion.api.model.types.base.BaseModelData;
import com.supermartijn642.fusion.api.model.types.composite.CompositeModelData;
import com.supermartijn642.fusion.api.util.Either;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverride;
import net.minecraft.client.renderer.block.model.ItemTransformVec3f;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created 15/06/2026 by SuperMartijn642
 */
public class CompositeModelDataBuilderImpl implements CompositeModelData.Builder {

    public static CompositeModelData.Builder builder(){
        return new CompositeModelDataBuilderImpl();
    }

    private final List<List<CompositeModelData.ModelEntry>> modelSeries = new ArrayList<>();
    private final BaseModelData.Builder<?,BaseModelData> baseModelData = BaseModelData.builder();

    private CompositeModelDataBuilderImpl(){
    }

    @Override
    public CompositeModelData.Builder model(CompositeModelData.ModelEntry entry){
        this.modelSeries.add(ImmutableList.of(entry));
        return this;
    }

    @Override
    public CompositeModelData.Builder modelSeries(CompositeModelData.ModelEntry... entries){
        if(entries.length != 0)
            this.modelSeries.add(ImmutableList.copyOf(entries));
        return this;
    }

    @Override
    public CompositeModelData.Builder modelSeries(List<CompositeModelData.ModelEntry> entries){
        if(!entries.isEmpty())
            this.modelSeries.add(ImmutableList.copyOf(entries));
        return this;
    }

    @Override
    public CompositeModelData.Builder material(String key, Either<String,ModelMaterial> material){
        this.baseModelData.material(key, material);
        return this;
    }

    @Override
    public CompositeModelData.Builder isGui3d(@Nullable Boolean isGui3d){
        this.baseModelData.isGui3d(isGui3d);
        return this;
    }

    @Override
    public CompositeModelData.Builder ambientOcclusion(@Nullable Boolean ambientOcclusion){
        this.baseModelData.ambientOcclusion(ambientOcclusion);
        return this;
    }

    @Override
    public CompositeModelData.Builder itemTransform(ItemCameraTransforms.TransformType type, @Nullable ItemTransformVec3f transform){
        this.baseModelData.itemTransform(type, transform);
        return this;
    }

    @Override
    public CompositeModelData.Builder itemTransforms(ItemCameraTransforms itemTransforms){
        this.baseModelData.itemTransforms(itemTransforms);
        return this;
    }

    @Override
    public CompositeModelData.Builder itemOverrides(ItemOverride... overrides){
        this.baseModelData.itemOverrides(overrides);
        return this;
    }

    @Override
    public CompositeModelData.Builder shade(@Nullable Boolean shade){
        this.baseModelData.shade(shade);
        return this;
    }

    @Override
    public CompositeModelData.Builder emissive(@Nullable Boolean emissive){
        this.baseModelData.emissive(emissive);
        return this;
    }

    @Override
    public CompositeModelData build(){
        if(this.modelSeries.isEmpty())
            throw new IllegalStateException("Composite model data must have at least one model entry!");
        return new CompositeModelDataImpl(
            ImmutableList.copyOf(this.modelSeries),
            this.baseModelData.build()
        );
    }
}
