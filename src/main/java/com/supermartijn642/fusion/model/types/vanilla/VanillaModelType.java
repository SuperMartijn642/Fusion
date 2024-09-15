package com.supermartijn642.fusion.model.types.vanilla;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.*;
import com.supermartijn642.fusion.util.TextureAtlases;
import net.minecraft.client.renderer.model.BlockModel;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ModelBakery;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ModelLoaderRegistry2;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created 29/04/2023 by SuperMartijn642
 */
public class VanillaModelType implements ModelType<BlockModel> {

    @Override
    public Collection<ResourceLocation> getModelDependencies(BlockModel data){
        return data.getDependencies();
    }

    @Override
    public Collection<SpriteIdentifier> getTextureDependencies(GatherTexturesContext context, BlockModel data){
        // Resolve parent models
        resolveParents(context, data);
        // Collect the textures
        Collection<ResourceLocation> materials = data.getTextures(location -> context.getModel(location).getAsVanillaModel(), new HashSet<>());
        return materials.stream().map(t -> SpriteIdentifier.of(TextureAtlases.getBlocks(), t)).collect(Collectors.toList());
    }

    @Override
    public IBakedModel bake(ModelBakingContext context, BlockModel data){
        //noinspection deprecation
        return data.bake(context.getModelBakery(), material -> context.getTexture(SpriteIdentifier.of(TextureAtlases.getBlocks(), material)), context.getTransformation());
    }

    @Nullable
    @Override
    public BlockModel getAsVanillaModel(BlockModel data){
        return data;
    }

    @Override
    public BlockModel deserialize(JsonObject json) throws JsonParseException{
        return ModelLoaderRegistry2.ExpandedBlockModelDeserializer.INSTANCE.fromJson(json, BlockModel.class);
    }

    @Override
    public JsonObject serialize(BlockModel value){
        return (JsonObject)VanillaModelSerializer.GSON.toJsonTree(value);
    }

    private static void resolveParents(GatherTexturesContext context, BlockModel model){
        Set<BlockModel> passedModels = new LinkedHashSet<>();
        while(model.parentLocation != null && model.parent == null){
            passedModels.add(model);
            ModelInstance<?> modelInstance = context.getModel(model.parentLocation);
            if(modelInstance == null)
                return;
            BlockModel parent = modelInstance.getAsVanillaModel();
            if(parent == null)
                BlockModel.LOGGER.warn("Vanilla model {} cannot have parent with model type {} for {}!", model, modelInstance.getModelType(), model.parentLocation);
            if(passedModels.contains(parent)){
                BlockModel.LOGGER.warn("Found 'parent' loop while loading model '{}' in chain: {} -> {}", model, passedModels.stream().map(Object::toString).collect(Collectors.joining(" -> ")), model.parentLocation);
                parent = null;
            }
            if(parent == null){
                model.parentLocation = ModelBakery.MISSING_MODEL_LOCATION;
                parent = context.getModel(model.parentLocation).getAsVanillaModel();
                if(parent == null)
                    throw new RuntimeException("Got null for missing model request!");
            }
            model.parent = parent;
            model = parent;
        }
    }
}
