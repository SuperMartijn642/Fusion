package com.supermartijn642.fusion.model.types.itemgenerator;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.ModelType;
import com.supermartijn642.fusion.api.model.custom.*;
import com.supermartijn642.fusion.api.model.custom.geometry.CuboidModelGeometry;
import com.supermartijn642.fusion.api.model.custom.geometry.ModelGeometry;
import com.supermartijn642.fusion.api.util.Either;
import com.supermartijn642.fusion.api.util.Property;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.model.ItemModelGenerator;
import net.minecraft.client.renderer.model.ItemTransformVec3f;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Created 02/05/2026 by SuperMartijn642
 */
public class ItemModelGeneratorModelType implements ModelType<Void> {

    private static final ItemModelGenerator ITEM_MODEL_GENERATOR = new ItemModelGenerator();

    @Override
    public Collection<ResourceLocation> getDependencies(Void data){
        return Collections.emptyList();
    }

    @Override
    public List<Either<ResourceLocation,UntypedModelInstance>> getParents(Void data){
        return Collections.emptyList();
    }

    @Override
    public @Nullable Boolean getAmbientOcclusion(Void data){
        return null;
    }

    @Override
    public @Nullable Boolean getIsGui3d(Void data){
        return false;
    }

    @Override
    public @Nullable ItemTransformVec3f getItemTransform(ItemCameraTransforms.TransformType type, Void data){
        return null;
    }

    @Override
    public Map<String,Either<String,ModelMaterial>> getMaterials(Void data){
        return Collections.emptyMap();
    }

    @Override
    public @Nullable ModelGeometry getGeometry(Void data){
        return new ModelGeometry() {
            @Override
            public Collection<Either<String,ModelMaterial>> getRequiredMaterials(){
                return Collections.emptyList();
            }

            @Override
            public CullableQuads bake(ModelTransform transformation, MaterialResolver materialResolver){
                // Create elements
                List<CuboidModelGeometry.Element> elements = new ArrayList<>();
                for(int layerIndex = 0; layerIndex < ItemModelGenerator.LAYERS.size(); layerIndex++){
                    String layerName = ItemModelGenerator.LAYERS.get(layerIndex);
                    TextureAtlasSprite sprite = materialResolver.get(layerName, false);
                    if(ModelMaterial.isMissingSprite(sprite))
                        break;
                    ITEM_MODEL_GENERATOR.processFrames(layerIndex, layerName, sprite)
                        .forEach(e -> elements.add(CuboidModelGeometry.Element.of(e)));
                }
                // Bake as cuboid geometry
                return CuboidModelGeometry.of(elements).bake(transformation, materialResolver);
            }
        };
    }

    @Override
    public @Nullable Boolean getShade(Void data){
        return null;
    }

    @Override
    public @Nullable Boolean getEmissive(Void data){
        return null;
    }

    @Override
    public <X, C> Optional<X> getProperty(Property<X,C> property, C context, Void data){
        return Optional.empty();
    }

    @Override
    public IBakedModel bakeModel(ModelBakingContext context, Void data){
        throw new UnsupportedOperationException("Cannot bake item model generator!");
    }

    @Override
    public Void deserialize(JsonObject json) throws JsonParseException{
        throw new UnsupportedOperationException("Cannot deserialize item model generator!");
    }

    @Override
    public JsonObject serialize(Void data){
        throw new UnsupportedOperationException("Cannot serialize item model generator!");
    }
}
