package com.supermartijn642.fusion.model.types.cuboid;

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
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverride;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Created 29/04/2023 by SuperMartijn642
 */
public class CuboidModelType extends SimpleModelType<BlockModel> {

    public static Map<String,Either<String,ModelMaterial>> convertMaterials(Map<String,com.mojang.datafixers.util.Either<Material,String>> materials){
        if(materials.isEmpty())
            return Map.of();
        ImmutableMap.Builder<String,Either<String,ModelMaterial>> builder = ImmutableMap.builderWithExpectedSize(materials.size());
        for(Map.Entry<String,com.mojang.datafixers.util.Either<Material,String>> entry : materials.entrySet()){
            entry.getValue()
                .ifLeft(material -> builder.put(entry.getKey(), Either.right(ModelMaterial.of(material))))
                .ifRight(reference -> builder.put(entry.getKey(), Either.left(reference)));
        }
        return builder.build();
    }

    @Override
    public Collection<ResourceLocation> getDependencies(BlockModel data){
        ResourceLocation parent = data.parentLocation;
        return parent == null ? List.of() : List.of(parent);
    }

    @Override
    public List<Either<ResourceLocation,UntypedModelInstance>> getParents(BlockModel data){
        ResourceLocation parent = data.parentLocation;
        return parent == null ? List.of() : List.of(Either.left(parent));
    }

    @Override
    public @Nullable Boolean getAmbientOcclusion(BlockModel data){
        return data.hasAmbientOcclusion;
    }

    @Override
    public BlockModel.@Nullable GuiLight getGuiLight(BlockModel data){
        return data.guiLight;
    }

    @Override
    public @Nullable ItemTransform getItemTransform(ItemDisplayContext type, BlockModel data){
        ItemTransform transform = data.transforms.getTransform(type);
        return transform == ItemTransform.NO_TRANSFORM ? null : transform;
    }

    @Override
    public List<ItemOverride> getItemOverrides(BlockModel data){
        return data.getOverrides();
    }

    @Override
    public Map<String,Either<String,ModelMaterial>> getMaterials(BlockModel data){
        return convertMaterials(data.textureMap);
    }

    @Override
    public ModelGeometry getGeometry(BlockModel data){
        List<BlockElement> elements = data.elements;
        return elements == null || elements.isEmpty() ? null : CuboidModelGeometry.of(data);
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
        return data.parentLocation;
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
        builder.parent(model.parentLocation)
            .guiLight(model.guiLight)
            .ambientOcclusion(model.hasAmbientOcclusion)
            .itemTransforms(model.transforms);
        // Copy materials
        for(Map.Entry<String,com.mojang.datafixers.util.Either<Material,String>> entry : model.textureMap.entrySet()){
            entry.getValue().ifLeft(m -> builder.material(entry.getKey(), m.texture()));
            entry.getValue().ifRight(r -> builder.material(entry.getKey(), r));
        }
        // Copy elements
        for(BlockElement element : model.elements)
            builder.elements(CuboidModelGeometry.Element.of(element));
        // Serialize data
        return DefaultModelTypes.BASE.serialize(builder.build());
    }
}
