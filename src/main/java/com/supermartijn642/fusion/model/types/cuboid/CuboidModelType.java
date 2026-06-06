package com.supermartijn642.fusion.model.types.cuboid;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.DefaultModelTypes;
import com.supermartijn642.fusion.api.model.custom.*;
import com.supermartijn642.fusion.api.model.custom.geometry.CuboidModelGeometry;
import com.supermartijn642.fusion.api.model.custom.geometry.ModelGeometry;
import com.supermartijn642.fusion.api.model.types.base.BaseModelData;
import com.supermartijn642.fusion.api.util.Either;
import com.supermartijn642.fusion.api.util.Property;
import com.supermartijn642.fusion.model.SimpleModelType;
import com.supermartijn642.fusion.util.IdentifierUtil;
import net.minecraft.client.renderer.block.model.BlockPart;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemTransformVec3f;
import net.minecraft.client.renderer.block.model.ModelBlock;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Created 29/04/2023 by SuperMartijn642
 */
public class CuboidModelType extends SimpleModelType<ModelBlock> {

    public static Map<String,Either<String,ModelMaterial>> convertMaterials(Map<String,String> materials){
        if(materials.isEmpty())
            return Collections.emptyMap();
        ImmutableMap.Builder<String,Either<String,ModelMaterial>> builder = ImmutableMap.builder();
        for(Map.Entry<String,String> entry : materials.entrySet()){
            if(IdentifierUtil.isValidIdentifier(entry.getValue()))
                builder.put(entry.getKey(), Either.right(ModelMaterial.of(new ResourceLocation(entry.getValue()))));
            else{
                String reference = entry.getValue();
                if(reference.startsWith("#"))
                    reference = reference.substring(1);
                builder.put(entry.getKey(), Either.left(reference));
            }
        }
        return builder.build();
    }

    @Override
    public Collection<ResourceLocation> getDependencies(ModelBlock data){
        ResourceLocation parent = data.getParentLocation();
        return parent == null ? Collections.emptyList() : ImmutableList.of(parent);
    }

    @Override
    public List<Either<ResourceLocation,UntypedModelInstance>> getParents(ModelBlock data){
        ResourceLocation parent = data.getParentLocation();
        return parent == null ? Collections.emptyList() : ImmutableList.of(Either.left(parent));
    }

    @Override
    public @Nullable Boolean getAmbientOcclusion(ModelBlock data){
        return data.ambientOcclusion ? null : false;
    }

    @Override
    public @Nullable Boolean getIsGui3d(ModelBlock data){
        return data.isGui3d();
    }

    @Override
    public @Nullable ItemTransformVec3f getItemTransform(ItemCameraTransforms.TransformType type, ModelBlock data){
        ItemTransformVec3f transform = data.cameraTransforms.getTransform(type);
        return transform == ItemTransformVec3f.DEFAULT ? null : transform;
    }

    @Override
    public Map<String,Either<String,ModelMaterial>> getMaterials(ModelBlock data){
        return convertMaterials(data.textures);
    }

    @Override
    public ModelGeometry getGeometry(ModelBlock data){
        return data.elements.isEmpty() ? null : CuboidModelGeometry.of(data);
    }

    @Override
    public @Nullable Boolean getShade(ModelBlock data){
        return null;
    }

    @Override
    public @Nullable Boolean getEmissive(ModelBlock data){
        return null;
    }

    @Override
    public <X, C> Optional<X> getProperty(Property<X,C> property, C context, ModelBlock data){
        return Optional.empty();
    }

    @Override
    protected @Nullable ResourceLocation getParent(ModelBlock data){
        return data.getParentLocation();
    }

    @Override
    protected void bakeGeometry(ModelBakingContext context, ModelStack modelStack, ModelBlock data, ModelTransform transform, ModelGeometry.MaterialResolver materialResolver, ModelGeometry.QuadConsumer quadConsumer){
        this.getGeometry(data).bake(quadConsumer, transform, materialResolver);
    }

    @Override
    public ModelBlock deserialize(JsonObject json) throws JsonParseException{
        return ModelBlock.SERIALIZER.fromJson(json, ModelBlock.class);
    }

    @Override
    public JsonObject serialize(ModelBlock model){
        // Use base model type to serialize vanilla cuboid model
        BaseModelData.Builder<?,BaseModelData> builder = BaseModelData.builder();
        // Copy properties
        builder.parent(model.getParentLocation())
            .isGui3d(model.isGui3d())
            .ambientOcclusion(model.ambientOcclusion)
            .itemTransforms(model.cameraTransforms);
        // Copy materials
        for(Map.Entry<String,String> entry : model.textures.entrySet()){
            String value = entry.getValue();
            if(IdentifierUtil.isValidIdentifier(value))
                builder.material(entry.getKey(), new ResourceLocation(entry.getValue()));
            else{
                if(value.startsWith("#"))
                    value = value.substring(1);
                builder.material(entry.getKey(), value);
            }
        }
        // Copy elements
        for(BlockPart element : model.elements)
            builder.elements(CuboidModelGeometry.Element.of(element));
        // Serialize data
        return DefaultModelTypes.BASE.serialize(builder.build());
    }
}
