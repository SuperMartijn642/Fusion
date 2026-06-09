package com.supermartijn642.fusion.model.types.itemgenerator;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.custom.ModelMaterial;
import com.supermartijn642.fusion.api.model.custom.ModelTransform;
import com.supermartijn642.fusion.api.model.custom.UntypedModelInstance;
import com.supermartijn642.fusion.api.model.custom.geometry.CuboidModelGeometry;
import com.supermartijn642.fusion.api.model.custom.geometry.ModelGeometry;
import com.supermartijn642.fusion.api.util.Either;
import com.supermartijn642.fusion.api.util.Property;
import com.supermartijn642.fusion.model.SimpleModelType;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Created 02/05/2026 by SuperMartijn642
 */
public class ItemModelGeneratorModelType extends SimpleModelType<Void> {

    private static final ItemModelGenerator ITEM_MODEL_GENERATOR = new ItemModelGenerator();

    @Override
    public Collection<ResourceLocation> getDependencies(Void data){
        return List.of();
    }

    @Override
    public List<Either<ResourceLocation,UntypedModelInstance>> getParents(Void data){
        return List.of();
    }

    @Override
    public @Nullable Boolean getAmbientOcclusion(Void data){
        return null;
    }

    @Override
    public BlockModel.@Nullable GuiLight getGuiLight(Void data){
        return BlockModel.GuiLight.FRONT;
    }

    @Override
    public @Nullable ItemTransform getItemTransform(ItemTransforms.TransformType type, Void data){
        return null;
    }

    @Override
    public List<ItemOverride> getItemOverrides(Void data){
        return List.of();
    }

    @Override
    public Map<String,Either<String,ModelMaterial>> getMaterials(Void data){
        return Map.of();
    }

    @Override
    public @Nullable ModelGeometry getGeometry(Void data){
        return new ModelGeometry() {
            @Override
            public Collection<Either<String,ModelMaterial>> getRequiredMaterials(){
                return List.of();
            }

            @Override
            public boolean isGui3d(){
                return false;
            }

            @Override
            public void bake(QuadConsumer quadConsumer, ModelTransform transformation, MaterialKeyResolver materialResolver){
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
                CuboidModelGeometry.of(elements).bake(quadConsumer, transformation, materialResolver);
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
    protected @Nullable ResourceLocation getParent(Void data){
        return null;
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
