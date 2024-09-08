package com.supermartijn642.fusion.model.types.base;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Either;
import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.api.model.DefaultModelTypes;
import com.supermartijn642.fusion.api.model.ModelBakingContext;
import com.supermartijn642.fusion.api.model.ModelInstance;
import com.supermartijn642.fusion.api.model.SpriteIdentifier;
import com.supermartijn642.fusion.api.model.data.BaseModelData;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created 06/09/2024 by SuperMartijn642
 */
public class BaseModelDataImpl implements BaseModelData {

    protected static final FaceBakery FACE_BAKERY = new FaceBakery();

    protected final BlockModel model;
    protected final List<ResourceLocation> parents;
    protected final List<BaseModelElement> elements;

    public BaseModelDataImpl(BlockModel model, List<ResourceLocation> parents, List<BaseModelElement> elements){
        this.model = model;
        this.parents = ImmutableList.copyOf(parents);
        this.elements = ImmutableList.copyOf(elements);
    }

    @Override
    public BlockModel getVanillaModel(){
        return this.model;
    }

    @Override
    public List<ResourceLocation> getParents(){
        return this.parents;
    }

    public List<? extends BaseModelElement> getElements(){
        return this.elements;
    }

    public void validateParents(ModelBakingContext context){
        List<ResourceLocation> encounteredModels = new ArrayList<>();
        for(ResourceLocation parent : this.parents)
            this.validateParents(context, parent, encounteredModels);
    }

    private void validateParents(ModelBakingContext context, ResourceLocation modelLocation, List<ResourceLocation> encounteredModels){
        if(encounteredModels.contains(modelLocation))
            throw new IllegalStateException("Unable to bake model '" + context.getModelIdentifier() + "' due to circular dependency " + encounteredModels.stream().map(o -> "'" + o + "'").collect(Collectors.joining("->")) + "->'" + modelLocation + "'!");
        encounteredModels.add(modelLocation);

        ModelInstance<?> model = context.getModel(modelLocation);
        if(model != null){
            for(ResourceLocation dependency : model.getParentModels())
                this.validateParents(context, dependency, encounteredModels);
        }

        encounteredModels.remove(encounteredModels.size() - 1);
    }

    public <T> T findProperty(ModelBakingContext context, Function<BlockModel,T> property, T defaultValue){
        T value = this.findProperty(context, ModelInstance.of(DefaultModelTypes.BASE, this), property);
        return value == null ? defaultValue : value;
    }

    private <T> T findProperty(ModelBakingContext context, ModelInstance<?> model, Function<BlockModel,T> property){
        BlockModel vanillaModel = model.getAsVanillaModel();
        if(vanillaModel != null){
            T value = property.apply(vanillaModel);
            if(value != null)
                return value;
        }

        for(ResourceLocation location : model.getParentModels()){
            ModelInstance<?> dependency = context.getModel(location);
            if(dependency != null){
                T childValue = this.findProperty(context, dependency, property);
                if(childValue != null)
                    return childValue;
            }
        }
        return null;
    }

    public SpriteIdentifier findParticleSprite(ModelBakingContext context){
        ModelInstance<?> model = ModelInstance.of(DefaultModelTypes.BASE, this);

        // Repeatedly resolve key references until we get to a texture
        List<String> encounteredKeys = new ArrayList<>();
        encounteredKeys.add("particle");
        String currentKey = "particle";
        while(true){
            String finalCurrentKey = currentKey;
            Either<Material,String> either = this.findProperty(context, model, m -> m.textureMap.get(finalCurrentKey));
            // If a key could not be found, return the missing texture
            if(either == null)
                return SpriteIdentifier.missing();

            // If a texture is found return it
            if(either.left().isPresent())
                return SpriteIdentifier.of(either.left().get());

            // Check if a key has already been encountered
            currentKey = either.right().get();
            if(encounteredKeys.contains(currentKey)){
                FusionClient.LOGGER.warn("Unable to resolve texture due to circular references {}->'{}' in '{}'!", encounteredKeys.stream().map(o -> "'" + o + "'").collect(Collectors.joining("->")), currentKey, context.getModelIdentifier());
                return SpriteIdentifier.missing();
            }
            encounteredKeys.add(currentKey);
        }
    }

    public List<BaseModelQuad> bakeQuads(ModelBakingContext context){
        List<BaseModelQuad> quads = new ArrayList<>();
        this.bakeQuads(context, ModelInstance.of(DefaultModelTypes.BASE, this), new LinkedList<>(), quads::add);
        return quads;
    }

    private void bakeQuads(ModelBakingContext context, ModelInstance<?> model, Deque<ModelInstance<?>> modelStack, Consumer<BaseModelQuad> output){
        modelStack.push(model);

        // If the model has elements, bake them
        List<? extends BlockElement> elements = null;
        if(model.getModelType() == DefaultModelTypes.BASE || model.getModelType() == DefaultModelTypes.CONNECTING){
            elements = ((BaseModelDataImpl)model.getModelData()).elements;
        }else{
            BlockModel vanillaModel = model.getAsVanillaModel();
            if(vanillaModel != null)
                elements = vanillaModel.elements;
        }
        if(elements != null && !elements.isEmpty()){
            // Bake the faces of each element
            for(BlockElement element : elements){
                for(Direction direction : element.faces.keySet()){
                    BlockElementFace face = element.faces.get(direction);
                    TextureAtlasSprite sprite = context.getTexture(this.resolveMaterial(context, modelStack, face.texture));
                    BakedQuad quad = FACE_BAKERY.bakeQuad(element.from, element.to, face, sprite, direction, context.getTransformation(), element.rotation, element.shade, context.getModelIdentifier());
                    Direction cullDirection = face.cullForDirection != null ? Direction.rotate(context.getTransformation().getRotation().getMatrix(), face.cullForDirection) : null;
                    Integer lightEmission = element instanceof BaseModelElement ? ((BaseModelElement)element).light_emission : null;
                    output.accept(new BaseModelQuad(quad, cullDirection, lightEmission));
                }
            }
            // If the model had elements, ignore parent models' elements
            modelStack.pop();
            return;
        }

        // If the model has no elements, check for parents
        for(ResourceLocation location : model.getParentModels()){
            ModelInstance<?> dependency = context.getModel(location);
            if(dependency != null)
                this.bakeQuads(context, dependency, modelStack, output);
        }

        modelStack.pop();
    }

    protected SpriteIdentifier resolveMaterial(ModelBakingContext context, Deque<ModelInstance<?>> modelStack, String key){
        if(key.charAt(0) == '#')
            key = key.substring(1);

        // Repeatedly resolve key references until we get to a texture
        List<String> encounteredKeys = new ArrayList<>();
        encounteredKeys.add(key);
        String currentKey = key;
        while(true){
            Either<Material,String> either = null;
            for(ModelInstance<?> model : modelStack){
                BlockModel vanillaModel = model.getAsVanillaModel();
                if(vanillaModel == null)
                    continue;
                either = vanillaModel.textureMap.get(currentKey);
                if(either != null)
                    break;
            }
            // If a key could not be found, return the missing texture
            if(either == null)
                return SpriteIdentifier.missing();

            // If a texture is found return it
            if(either.left().isPresent())
                return SpriteIdentifier.of(either.left().get());

            // Check if a key has already been encountered
            currentKey = either.right().get();
            if(encounteredKeys.contains(currentKey)){
                FusionClient.LOGGER.warn("Unable to resolve texture due to circular references {}->'{}' in '{}'!", encounteredKeys.stream().map(o -> "'" + o + "'").collect(Collectors.joining("->")), currentKey, context.getModelIdentifier());
                return SpriteIdentifier.missing();
            }
            encounteredKeys.add(currentKey);
        }
    }
}
