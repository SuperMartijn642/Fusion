package com.supermartijn642.fusion.model.types.base;

import com.google.common.collect.ImmutableList;
import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.api.model.BlockModelBakingContext;
import com.supermartijn642.fusion.api.model.DefaultModelTypes;
import com.supermartijn642.fusion.api.model.ModelInstance;
import com.supermartijn642.fusion.api.model.data.BaseModelData;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.client.resources.model.cuboid.*;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.geometry.UnbakedGeometry;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import org.jetbrains.annotations.Nullable;

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

    protected final CuboidModel model;
    protected final List<Identifier> parents;
    protected final List<BaseModelElement> elements;

    public BaseModelDataImpl(CuboidModel model, List<Identifier> parents, List<BaseModelElement> elements){
        this.model = model;
        this.parents = ImmutableList.copyOf(parents);
        this.elements = ImmutableList.copyOf(elements);
    }

    @Override
    public CuboidModel getVanillaModel(){
        return this.model;
    }

    @Override
    public List<Identifier> getParents(){
        return this.parents;
    }

    public List<? extends BaseModelElement> getElements(){
        return this.elements;
    }

    public void validateParents(BlockModelBakingContext context){
        List<Identifier> encounteredModels = new ArrayList<>();
        for(Identifier parent : this.parents)
            this.validateParents(context, parent, encounteredModels);
    }

    private void validateParents(BlockModelBakingContext context, Identifier modelLocation, List<Identifier> encounteredModels){
        if(encounteredModels.contains(modelLocation))
            throw new IllegalStateException("Unable to bake model '" + context.getModelIdentifier() + "' due to circular dependency " + encounteredModels.stream().map(o -> "'" + o + "'").collect(Collectors.joining("->")) + "->'" + modelLocation + "'!");
        encounteredModels.add(modelLocation);

        ModelInstance<?> model = context.getModel(modelLocation);
        if(model != null){
            for(Identifier dependency : model.getParentModels())
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

        for(Identifier location : model.getParentModels()){
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

    public Material findParticleMaterial(BlockModelBakingContext context){
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
                return context.missingMaterial();

            // If a texture is found return it
            if(contents instanceof TextureSlots.Value(Material material))
                return material;

            // Check if a key has already been encountered
            currentKey = ((TextureSlots.Reference)contents).target();
            if(encounteredKeys.contains(currentKey)){
                FusionClient.LOGGER.warn("Unable to resolve texture due to circular references {}->'{}' in '{}'!", encounteredKeys.stream().map(o -> "'" + o + "'").collect(Collectors.joining("->")), currentKey, context.getModelIdentifier());
                return context.missingMaterial();
            }
            encounteredKeys.add(currentKey);
        }
    }

    public List<BaseModelQuad> bakeQuads(BlockModelBakingContext context){
        List<BaseModelQuad> quads = new ArrayList<>();
        this.bakeQuads(context, ModelInstance.of(DefaultModelTypes.BASE, this), context.getModelIdentifier(), new LinkedList<>(), quads::add);
        return quads;
    }

    private void bakeQuads(BlockModelBakingContext context, ModelInstance<?> model, Identifier modelLocation, Deque<ModelInstance<?>> modelStack, Consumer<BaseModelQuad> output){
        modelStack.addLast(model);

        // If the model has elements, bake them
        if(model.getModelType() == DefaultModelTypes.BASE || model.getModelType() == DefaultModelTypes.CONNECTING){
            List<? extends BaseModelElement> elements = ((BaseModelDataImpl)model.getModelData()).elements;
            if(elements != null && !elements.isEmpty()){
                // Bake the faces of each element
                for(BaseModelElement element : elements){
                    for(Direction direction : element.original.faces().keySet()){
                        CuboidFace face = element.original.faces().get(direction);
                        Material unbakedMaterial = this.resolveMaterial(context, modelStack, face.texture());
                        Material.Baked material = unbakedMaterial != null ? context.bakeMaterial(unbakedMaterial) : context.bakeMaterial(context.missingMaterial());
                        BakedQuad quad = FaceBakery.bakeQuad(context.getModelBaker(), element.original.from(), element.original.to(), face, material, direction, context.getTransformation(), element.original.rotation(), element.original.shade(), element.original.lightEmission());
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
        for(Identifier location : model.getParentModels()){
            ModelInstance<?> dependency = context.getModel(location);
            if(dependency != null)
                this.bakeQuads(context, dependency, location, modelStack, output);
        }

        modelStack.removeLast();
    }

    protected @Nullable Material resolveMaterial(BlockModelBakingContext context, Deque<ModelInstance<?>> modelStack, String key){
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
                return null;

            // If a texture is found return it
            if(contents instanceof TextureSlots.Value(Material material))
                return material;

            // Check if a key has already been encountered
            currentKey = ((TextureSlots.Reference)contents).target();
            if(encounteredKeys.contains(currentKey)){
                FusionClient.LOGGER.warn("Unable to resolve texture due to circular references {}->'{}' in '{}'!", encounteredKeys.stream().map(o -> "'" + o + "'").collect(Collectors.joining("->")), currentKey, context.getModelIdentifier());
                return null;
            }
            encounteredKeys.add(currentKey);
        }
    }

    protected QuadCollection bakeItemModel(BlockModelBakingContext context, Deque<ModelInstance<?>> modelStack){
        QuadCollection.Builder quads = new QuadCollection.Builder();
        for(int layer = 0; layer < ItemModelGenerator.LAYERS.size(); layer++){
            String layerName = ItemModelGenerator.LAYERS.get(layer);
            Material unbakedMaterial = this.resolveMaterial(context, modelStack, layerName);
            if(unbakedMaterial == null)
                break;
            Material.Baked material = context.bakeMaterial(unbakedMaterial);
            quads.addAll(context.getModelBaker().compute(new ItemModelGenerator.ItemLayerKey(material, context.getTransformation(), layer)));
        }
        return quads.build();
    }
}
