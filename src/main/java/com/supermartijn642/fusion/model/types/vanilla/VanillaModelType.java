package com.supermartijn642.fusion.model.types.vanilla;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.*;
import com.supermartijn642.fusion.extensions.BlockModelExtension;
import com.supermartijn642.fusion.util.TextureAtlases;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelBlock;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.animation.ModelBlockAnimation;

import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created 29/04/2023 by SuperMartijn642
 */
public class VanillaModelType implements ModelType<ModelBlock> {

    public static ModelLoader modelLoader;
    private static final ModelBlockAnimation defaultModelBlockAnimation = new ModelBlockAnimation(ImmutableMap.of(), ImmutableMap.of());

    @Override
    public Collection<ResourceLocation> getModelDependencies(ModelBlock data){
        return getAsForgeModel(data).getDependencies();
    }

    @Override
    public Collection<SpriteIdentifier> getTextureDependencies(GatherTexturesContext context, ModelBlock data){
        // Find the parent models
        resolveParents(context, data);
        // Get the textures
        Collection<ResourceLocation> materials = getAsForgeModel(data).getTextures();
        return materials.stream().map(i -> SpriteIdentifier.of(TextureAtlases.getBlocks(), i)).collect(Collectors.toList());
    }

    @Override
    public IBakedModel bake(ModelBakingContext context, ModelBlock data){
        return getAsForgeModel(data).bake(context.getTransformation(), DefaultVertexFormats.BLOCK, material -> context.getTexture(SpriteIdentifier.of(TextureAtlases.getBlocks(), material)));
    }

    @Nullable
    @Override
    public ModelBlock getAsVanillaModel(ModelBlock data){
        return data;
    }

    @Override
    public ModelBlock deserialize(JsonObject json) throws JsonParseException{
        return ModelBlock.SERIALIZER.fromJson(json, ModelBlock.class);
    }

    @Override
    public JsonObject serialize(ModelBlock value){
        return (JsonObject)VanillaModelSerializer.GSON.toJsonTree(value);
    }

    private static final Constructor<? extends IModel> vanillaModelWrapperConstructor;

    static{
        //noinspection OptionalGetWithoutIsPresent
        Class<?> vanillaModelWrapperClass = Arrays.stream(ModelLoader.class.getDeclaredClasses())
            .filter(aClass -> aClass.getName().equals("net.minecraftforge.client.model.ModelLoader$VanillaModelWrapper"))
            .findFirst().get();
        //noinspection unchecked
        vanillaModelWrapperConstructor = (Constructor<? extends IModel>)vanillaModelWrapperClass.getDeclaredConstructors()[0];
        vanillaModelWrapperConstructor.setAccessible(true);
    }

    private static IModel getAsForgeModel(ModelBlock model){
        IModel wrapper = ((BlockModelExtension)model).getWrapper();
        if(wrapper == null){
            try{
                vanillaModelWrapperConstructor.newInstance(modelLoader, new ResourceLocation(model.name), model, false, defaultModelBlockAnimation);
            }catch(InstantiationException | IllegalAccessException | InvocationTargetException e){
                throw new RuntimeException(e);
            }
            wrapper = ((BlockModelExtension)model).getWrapper();
        }
        return wrapper;
    }

    private static void resolveParents(GatherTexturesContext context, ModelBlock model){
        Set<ModelBlock> passedModels = new LinkedHashSet<>();
        while(model.parentLocation != null && model.parent == null){
            passedModels.add(model);
            ModelInstance<?> modelInstance = context.getModel(model.parentLocation);
            ModelBlock parent = modelInstance.getAsVanillaModel();
            if(parent == null)
                ModelBlock.LOGGER.warn("Vanilla model {} cannot have parent with model type {} for {}!", model, modelInstance.getModelType(), model.parentLocation);
            if(passedModels.contains(parent)){
                ModelBlock.LOGGER.warn("Found 'parent' loop while loading model '{}' in chain: {} -> {}", model, passedModels.stream().map(Object::toString).collect(Collectors.joining(" -> ")), model.parentLocation);
                parent = null;
            }
            if(parent == null){
                model.parentLocation = ModelBakery.MODEL_MISSING;
                parent = context.getModel(model.parentLocation).getAsVanillaModel();
                if(parent == null)
                    throw new RuntimeException("Got null for missing model request!");
            }
            model.parent = parent;
            model = parent;
        }
    }
}
