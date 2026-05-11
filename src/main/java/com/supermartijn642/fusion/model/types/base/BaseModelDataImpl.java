package com.supermartijn642.fusion.model.types.base;

import com.google.common.collect.ImmutableMap;
import com.supermartijn642.fusion.api.model.custom.ModelMaterial;
import com.supermartijn642.fusion.api.model.custom.geometry.CuboidModelGeometry;
import com.supermartijn642.fusion.api.model.types.base.BaseModelData;
import com.supermartijn642.fusion.api.util.Either;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemTransformVec3f;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Created 06/09/2024 by SuperMartijn642
 */
public class BaseModelDataImpl implements BaseModelData {

    protected final ResourceLocation parent;
    protected final Map<String,Either<String,ModelMaterial>> materials;
    protected final Boolean ambientOcclusion, shade, emissive;
    protected final Boolean isGui3d;
    protected final CuboidModelGeometry geometry;
    protected final Map<ItemCameraTransforms.TransformType,ItemTransformVec3f> itemTransforms;

    public BaseModelDataImpl(ResourceLocation parent, Map<String,Either<String,ModelMaterial>> materials, Boolean ambientOcclusion, Boolean shade, Boolean emissive, Boolean isGui3d, CuboidModelGeometry geometry, Map<ItemCameraTransforms.TransformType,ItemTransformVec3f> itemTransforms){
        this.parent = parent;
        this.materials = ImmutableMap.copyOf(materials);
        this.ambientOcclusion = ambientOcclusion;
        this.shade = shade;
        this.emissive = emissive;
        this.isGui3d = isGui3d;
        this.geometry = geometry;
        this.itemTransforms = ImmutableMap.copyOf(itemTransforms);
    }

    @Override
    public @Nullable ResourceLocation getParent(){
        return this.parent;
    }

    @Override
    public Map<String,Either<String,ModelMaterial>> getMaterials(){
        return this.materials;
    }

    @Override
    public CuboidModelGeometry getGeometry(){
        return this.geometry;
    }

    @Override
    public @Nullable Boolean getAmbientOcclusion(){
        return this.ambientOcclusion;
    }

    @Override
    public @Nullable Boolean getIsGui3d(){
        return this.isGui3d;
    }

    @Override
    public @Nullable ItemTransformVec3f getItemTransform(ItemCameraTransforms.TransformType type){
        return this.itemTransforms.get(type);
    }

    @Override
    public @Nullable Boolean getShade(){
        return this.shade;
    }

    @Override
    public @Nullable Boolean getEmissive(){
        return this.emissive;
    }
}
