package com.supermartijn642.fusion.model.types.base;

import com.google.common.collect.ImmutableList;
import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.api.model.BlockModelBakingContext;
import com.supermartijn642.fusion.api.model.DefaultModelTypes;
import com.supermartijn642.fusion.api.model.ModelInstance;
import com.supermartijn642.fusion.api.model.SpriteIdentifier;
import com.supermartijn642.fusion.api.model.data.BaseModelData;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.QuadCollection;
import net.minecraft.client.resources.model.UnbakedGeometry;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;

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

    public void validateParents(BlockModelBakingContext context){
        List<ResourceLocation> encounteredModels = new ArrayList<>();
        for(ResourceLocation parent : this.parents)
            this.validateParents(context, parent, encounteredModels);
    }

    private void validateParents(BlockModelBakingContext context, ResourceLocation modelLocation, List<ResourceLocation> encounteredModels){
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

    public <T> T findProperty(BlockModelBakingContext context, Function<UnbakedModel,T> property, T defaultValue){
        T value = this.findProperty(context, ModelInstance.of(DefaultModelTypes.BASE, this), property);
        return value == null ? defaultValue : value;
    }

    private <T> T findProperty(BlockModelBakingContext context, ModelInstance<?> model, Function<UnbakedModel,T> property){
        UnbakedModel vanillaModel = model.getAsVanillaModel();
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

    public ItemTransform findItemTransform(BlockModelBakingContext context, ItemDisplayContext transformType){
        return this.findProperty(
            context,
            model -> {
                ItemTransforms transforms = model.transforms();
                if(transforms == null || transforms.getTransform(transformType) == ItemTransform.NO_TRANSFORM)
                    return null;
                return transforms.getTransform(transformType);
            },
            ItemTransform.NO_TRANSFORM
        );
    }

    public SpriteIdentifier findParticleSprite(BlockModelBakingContext context){
        ModelInstance<?> model = ModelInstance.of(DefaultModelTypes.BASE, this);

        // Repeatedly resolve key references until we get to a texture
        List<String> encounteredKeys = new ArrayList<>();
        encounteredKeys.add("particle");
        String currentKey = "particle";
        while(true){
            String finalCurrentKey = currentKey;
            TextureSlots.SlotContents contents = this.findProperty(context, model, m -> m.textureSlots().values().get(finalCurrentKey));
            // If a key could not be found, return the missing texture
            if(contents == null)
                return SpriteIdentifier.missing();

            // If a texture is found return it
            if(contents instanceof TextureSlots.Value)
                return SpriteIdentifier.of(((TextureSlots.Value)contents).material());

            // Check if a key has already been encountered
            currentKey = ((TextureSlots.Reference)contents).target();
            if(encounteredKeys.contains(currentKey)){
                FusionClient.LOGGER.warn("Unable to resolve texture due to circular references {}->'{}' in '{}'!", encounteredKeys.stream().map(o -> "'" + o + "'").collect(Collectors.joining("->")), currentKey, context.getModelIdentifier());
                return SpriteIdentifier.missing();
            }
            encounteredKeys.add(currentKey);
        }
    }

    public List<BaseModelQuad> bakeQuads(BlockModelBakingContext context){
        List<BaseModelQuad> quads = new ArrayList<>();
        this.bakeQuads(context, ModelInstance.of(DefaultModelTypes.BASE, this), context.getModelIdentifier(), new LinkedList<>(), quads::add);
        return quads;
    }

    private void bakeQuads(BlockModelBakingContext context, ModelInstance<?> model, ResourceLocation modelLocation, Deque<ModelInstance<?>> modelStack, Consumer<BaseModelQuad> output){
        modelStack.addLast(model);

        // If the model has elements, bake them
        if(model.getModelType() == DefaultModelTypes.BASE || model.getModelType() == DefaultModelTypes.CONNECTING){
            List<? extends BaseModelElement> elements = ((BaseModelDataImpl)model.getModelData()).elements;
            if(elements != null && !elements.isEmpty()){
                // Bake the faces of each element
                for(BaseModelElement element : elements){
                    for(Direction direction : element.original.faces().keySet()){
                        BlockElementFace face = element.original.faces().get(direction);
                        TextureAtlasSprite sprite = context.getTexture(this.resolveMaterial(context, modelStack, face.texture()));
                        BakedQuad quad = FaceBakery.bakeQuad(element.original.from(), element.original.to(), face, sprite, direction, context.getTransformation(), element.original.rotation(), element.original.shade(), element.original.lightEmission());
                        Direction cullDirection = face.cullForDirection() != null ? Direction.rotate(context.getTransformation().transformation().getMatrix(), face.cullForDirection()) : null;
                        output.accept(new BaseModelQuad(quad, cullDirection));
                    }
                }
                // If the model had elements, ignore parent models' elements
                modelStack.pop();
                return;
            }
        }

        // Try and bake the model as a vanilla model
        UnbakedModel vanillaModel = model.getAsVanillaModel();
        if(vanillaModel != null){
            UnbakedGeometry geometry = vanillaModel.geometry();
            if(geometry != null){
                QuadCollection quads = geometry.bake(new TextureSlots(context.getTopLevelTextureReferences()), context.getModelBaker(), context.getTransformation(), modelLocation::toString);
                quads.getQuads(null).forEach(quad -> output.accept(new BaseModelQuad(quad, null)));
                for(Direction cullDirection : Direction.values())
                    quads.getQuads(cullDirection).forEach(quad -> output.accept(new BaseModelQuad(quad, cullDirection)));
                modelStack.pop();
                return;
            }
        }

        // If the model has no elements, check for parents
        for(ResourceLocation location : model.getParentModels()){
            ModelInstance<?> dependency = context.getModel(location);
            if(dependency != null)
                this.bakeQuads(context, dependency, location, modelStack, output);
        }

        modelStack.removeLast();
    }

    protected SpriteIdentifier resolveMaterial(BlockModelBakingContext context, Deque<ModelInstance<?>> modelStack, String key){
        if(key.charAt(0) == '#')
            key = key.substring(1);

        // Repeatedly resolve key references until we get to a texture
        List<String> encounteredKeys = new ArrayList<>();
        encounteredKeys.add(key);
        String currentKey = key;
        while(true){
            TextureSlots.SlotContents contents = null;
            // Check models in the model stack
            for(ModelInstance<?> model : modelStack){
                UnbakedModel vanillaModel = model.getAsVanillaModel();
                if(vanillaModel == null)
                    continue;
                contents = vanillaModel.textureSlots().values().get(currentKey);
                if(contents != null)
                    break;
            }
            // If no value is found, check the parents of the last model
            if(contents == null){
                String finalCurrentKey = currentKey;
                contents = this.findProperty(context, modelStack.getLast(), model -> model.textureSlots().values().get(finalCurrentKey));
            }
            // If a key could not be found, return the missing texture
            if(contents == null)
                return SpriteIdentifier.missing();

            // If a texture is found return it
            if(contents instanceof TextureSlots.Value)
                return SpriteIdentifier.of(((TextureSlots.Value)contents).material());

            // Check if a key has already been encountered
            currentKey = ((TextureSlots.Reference)contents).target();
            if(encounteredKeys.contains(currentKey)){
                FusionClient.LOGGER.warn("Unable to resolve texture due to circular references {}->'{}' in '{}'!", encounteredKeys.stream().map(o -> "'" + o + "'").collect(Collectors.joining("->")), currentKey, context.getModelIdentifier());
                return SpriteIdentifier.missing();
            }
            encounteredKeys.add(currentKey);
        }
    }

    protected List<BlockElement> generateItemModel(BlockModelBakingContext context, Deque<ModelInstance<?>> modelStack){
        List<BlockElement> elements = new ArrayList<>();
        for(int layer = 0; layer < ItemModelGenerator.LAYERS.size(); layer++){
            String layerName = ItemModelGenerator.LAYERS.get(layer);
            SpriteIdentifier sprite = this.resolveMaterial(context, modelStack, layerName);
            if(SpriteIdentifier.missing().equals(sprite))
                break;

            SpriteContents spriteContents = context.getTexture(sprite).contents();
            elements.addAll(ItemModelGenerator.processFrames(layer, layerName, spriteContents));
        }
        return elements;
    }
}
