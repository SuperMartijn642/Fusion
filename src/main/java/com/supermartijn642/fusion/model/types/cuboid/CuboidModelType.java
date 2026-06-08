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
import net.minecraft.client.renderer.model.BlockModel;
import net.minecraft.client.renderer.model.BlockPart;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.model.ItemTransformVec3f;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Created 29/04/2023 by SuperMartijn642
 */
public class CuboidModelType extends SimpleModelType<BlockModel> {

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
    public Collection<ResourceLocation> getDependencies(BlockModel data){
        return data.getDependencies();
    }

    @Override
    public List<Either<ResourceLocation,UntypedModelInstance>> getParents(BlockModel data){
        ResourceLocation parent = data.getParentLocation();
        return parent == null ? Collections.emptyList() : ImmutableList.of(Either.left(parent));
    }

    @Override
    public @Nullable Boolean getAmbientOcclusion(BlockModel data){
        return data.hasAmbientOcclusion;
    }

    @Override
    public @Nullable Boolean getIsGui3d(BlockModel data){
        return data.isGui3d();
    }

    @Override
    public @Nullable ItemTransformVec3f getItemTransform(ItemCameraTransforms.TransformType type, BlockModel data){
        ItemTransformVec3f transform = data.transforms.getTransform(type);
        return transform == ItemTransformVec3f.NO_TRANSFORM ? null : transform;
    }

    @Override
    public Map<String,Either<String,ModelMaterial>> getMaterials(BlockModel data){
        return convertMaterials(data.textureMap);
    }

    @Override
    public ModelGeometry getGeometry(BlockModel data){
        List<BlockPart> elements = data.getElements();
        return elements.isEmpty() ? null : CuboidModelGeometry.of(data);
    }

    @Override
    public @Nullable Boolean getShade(BlockModel data){
        return null;
    }

    @Override
    public @Nullable Boolean getEmissive(BlockModel data){
        return null;
    }

    @Override
    public <X, C> Optional<X> getProperty(Property<X,C> property, C context, BlockModel data){
        return Optional.empty();
    }

    @Override
    protected @Nullable ResourceLocation getParent(BlockModel data){
        return data.getParentLocation();
    }

    @Override
    protected void bakeGeometry(ModelBakingContext context, ModelStack modelStack, BlockModel data, ModelTransform transform, ModelGeometry.MaterialKeyResolver materialResolver, ModelGeometry.QuadConsumer quadConsumer){
        this.getGeometry(data).bake(quadConsumer, transform, materialResolver);
    }

    @Override
    public BlockModel deserialize(JsonObject json) throws JsonParseException{
        return BlockModel.GSON.fromJson(json, BlockModel.class);
    }

    @Override
    public JsonObject serialize(BlockModel model){
        // Use base model type to serialize vanilla cuboid model
        BaseModelData.Builder<?,BaseModelData> builder = BaseModelData.builder();
        // Copy properties
        builder.parent(model.getParentLocation())
            .isGui3d(model.isGui3d())
            .ambientOcclusion(model.hasAmbientOcclusion)
            .itemTransforms(model.transforms);
        // Copy materials
        for(Map.Entry<String,String> entry : model.textureMap.entrySet()){
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
