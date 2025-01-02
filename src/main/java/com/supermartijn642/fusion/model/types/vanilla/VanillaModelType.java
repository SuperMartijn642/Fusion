package com.supermartijn642.fusion.model.types.vanilla;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.api.model.ModelBakingContext;
import com.supermartijn642.fusion.api.model.ModelInstance;
import com.supermartijn642.fusion.api.model.ModelType;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.MissingBlockModel;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created 29/04/2023 by SuperMartijn642
 */
public class VanillaModelType implements ModelType<BlockModel> {

    @Override
    public Collection<ResourceLocation> getModelDependencies(BlockModel data){
        return data.parentLocation == null ? List.of() : List.of(data.parentLocation);
    }

    @Override
    public BakedModel bake(ModelBakingContext context, BlockModel data){
        // Resolve parent models
        resolveParents(context, data);
        // Bake the model
        return data.bake(
            new TextureSlots(context.getTopLevelTextureReferences()),
            context.getModelBaker(),
            context.getTransformation(),
            context.getTopLevelAmbientOcclusion(),
            context.getTopLevelUseBlockLighting(),
            context.getTopLevelItemTransforms(),
            context.getNeoForgeAdditionalProperties()
        );
    }

    @Nullable
    @Override
    public BlockModel getAsVanillaModel(BlockModel data){
        return data;
    }

    @Override
    public List<ResourceLocation> getParentModels(BlockModel data){
        return data.parentLocation == null ? List.of() : List.of(data.parentLocation);
    }

    @Override
    public BlockModel deserialize(JsonObject json) throws JsonParseException{
        return BlockModel.GSON.fromJson(json, BlockModel.class);
    }

    @Override
    public JsonObject serialize(BlockModel value){
        return (JsonObject)VanillaModelSerializer.GSON.toJsonTree(value);
    }

    private static void resolveParents(ModelBakingContext context, BlockModel model){
        Set<UnbakedModel> passedModels = new LinkedHashSet<>();
        while(model.parentLocation != null && model.parent == null){
            passedModels.add(model);
            ModelInstance<?> modelInstance = context.getModel(model.parentLocation);
            if(modelInstance == null)
                return;
            UnbakedModel parent = modelInstance.getAsVanillaModel();
            if(parent == null)
                FusionClient.LOGGER.warn("Vanilla model {} cannot have parent with model type {} for {}!", model, modelInstance.getModelType(), model.parentLocation);
            if(passedModels.contains(parent)){
                FusionClient.LOGGER.warn("Found 'parent' loop while loading model '{}' in chain: {} -> {}", model, passedModels.stream().map(Object::toString).collect(Collectors.joining(" -> ")), model.parentLocation);
                parent = null;
            }
            if(parent == null){
                model.parentLocation = MissingBlockModel.LOCATION;
                parent = context.getModel(model.parentLocation).getAsVanillaModel();
                if(parent == null)
                    throw new RuntimeException("Got null for missing model request!");
            }
            model.parent = parent;
            if(!(parent instanceof BlockModel))
                break;
            model = (BlockModel)parent;
        }
    }
}
