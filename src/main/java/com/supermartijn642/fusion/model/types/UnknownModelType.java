package com.supermartijn642.fusion.model.types;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.ModelInstance;
import com.supermartijn642.fusion.api.model.ModelType;
import com.supermartijn642.fusion.api.model.custom.ModelBakingContext;
import com.supermartijn642.fusion.api.model.custom.ModelMaterial;
import com.supermartijn642.fusion.api.model.custom.ModelWalker;
import com.supermartijn642.fusion.api.model.custom.UntypedModelInstance;
import com.supermartijn642.fusion.api.model.custom.geometry.ModelGeometry;
import com.supermartijn642.fusion.api.util.Either;
import com.supermartijn642.fusion.api.util.Property;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Created 30/04/2023 by SuperMartijn642
 */
public class UnknownModelType<T extends UnbakedModel> implements ModelType<T> {

    public static final Map<Direction,List<BakedQuad>> EMPTY_CULLED_QUADS;

    static{
        ImmutableMap.Builder<Direction,List<BakedQuad>> builder = ImmutableMap.builder();
        for(Direction direction : Direction.values())
            builder.put(direction, List.of());
        EMPTY_CULLED_QUADS = builder.build();
    }

    @Override
    public Collection<ResourceLocation> getDependencies(T data){
        return List.of();
    }

    @Override
    public List<Either<ResourceLocation,UntypedModelInstance>> getParents(T data){
        return List.of();
    }

    @Override
    public Boolean getAmbientOcclusion(T data){
        return null;
    }

    @Override
    public BlockModel.GuiLight getGuiLight(T data){
        return null;
    }

    @Override
    public ItemTransform getItemTransform(ItemDisplayContext type, T data){
        return null;
    }

    @Override
    public Map<String,Either<String,ModelMaterial>> getMaterials(T data){
        return Map.of();
    }

    @Override
    public ModelGeometry getGeometry(T data){
        return ModelGeometry.of(data);
    }

    @Override
    public @Nullable Boolean getShade(T data){
        return null;
    }

    @Override
    public @Nullable Boolean getEmissive(T data){
        return null;
    }

    @Override
    public <X, C> Optional<X> getProperty(Property<X,C> property, C context, T data){
        return Optional.empty();
    }

    @Override
    public BakedModel bakeModel(ModelBakingContext context, T data){
        // Create dummy model baker instance
        Function<Material,TextureAtlasSprite> spriteGetter = material -> context.getMaterial(ModelMaterial.of(material));
        ModelBaker modelBaker = new ModelBaker() {
            @Override
            public BakedModel bake(ResourceLocation identifier, ModelState modelState){
                ModelInstance<?> model = context.getModel(identifier);
                if(model == null)
                    return context.getMissingModel();
                return model.bakeModel(context);
            }

            @Override
            public BakedModel bake(ResourceLocation identifier, ModelState modelState, Function<Material,TextureAtlasSprite> spriteGetter){
                return this.bake(identifier, modelState);
            }

            @Override
            public Function<Material,TextureAtlasSprite> getModelTextureGetter(){
                return spriteGetter;
            }
        };
        // Bake the model
        return data.bake(modelBaker, spriteGetter, context.getTransformation().toModelState());
    }

    public static <T> T findPropertyInStackAndParents(ModelBakingContext context, ModelWalker.ModelStack currentStack, Function<UntypedModelInstance,T> property, T defaultValue){
        // First check the current stack
        for(UntypedModelInstance modelInstance : currentStack){
            T value = property.apply(modelInstance);
            if(value != null)
                return value;
        }
        // Check parents of the last model in the stack
        Optional<T> result = context.walkModelTree(
            currentStack.get(currentStack.size() - 1),
            (modelInstance, stack) -> {
                T value = property.apply(modelInstance);
                return value == null ? ModelWalker.Result.proceed() : ModelWalker.Result.stop(value);
            }
        );
        return result.orElse(defaultValue);
    }

    @Override
    public T deserialize(JsonObject json) throws JsonParseException{
        throw new UnsupportedOperationException("Cannot deserialize unknown model type!");
    }

    @Override
    public JsonObject serialize(T value){
        throw new UnsupportedOperationException("Cannot serialize unknown model type!");
    }
}
