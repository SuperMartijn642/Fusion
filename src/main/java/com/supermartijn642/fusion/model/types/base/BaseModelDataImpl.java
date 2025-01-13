package com.supermartijn642.fusion.model.types.base;

import com.google.common.collect.ImmutableList;
import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.api.model.*;
import com.supermartijn642.fusion.api.model.data.BaseModelData;
import com.supermartijn642.fusion.util.IdentifierUtil;
import com.supermartijn642.fusion.util.TextureAtlases;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.model.TRSRTransformation;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created 06/09/2024 by SuperMartijn642
 */
public class BaseModelDataImpl implements BaseModelData {

    protected static final FaceBakery FACE_BAKERY = new FaceBakery();

    protected final ModelBlock model;
    protected final List<ResourceLocation> parents;
    protected final List<BaseModelElement> elements;

    public BaseModelDataImpl(ModelBlock model, List<ResourceLocation> parents, List<BaseModelElement> elements){
        this.model = model;
        this.parents = ImmutableList.copyOf(parents);
        this.elements = ImmutableList.copyOf(elements);
    }

    @Override
    public ModelBlock getVanillaModel(){
        return this.model;
    }

    @Override
    public List<ResourceLocation> getParents(){
        return this.parents;
    }

    public List<? extends BaseModelElement> getElements(){
        return this.elements;
    }

    public void validateParents(Function<ResourceLocation,ModelInstance<?>> modelResolver, ResourceLocation rootModel){
        List<ResourceLocation> encounteredModels = new ArrayList<>();
        for(ResourceLocation parent : this.parents)
            this.validateParents(modelResolver, parent, encounteredModels, rootModel);
    }

    private void validateParents(Function<ResourceLocation,ModelInstance<?>> modelResolver, ResourceLocation modelLocation, List<ResourceLocation> encounteredModels, ResourceLocation rootModel){
        if(encounteredModels.contains(modelLocation))
            throw new IllegalStateException("Unable to bake model '" + rootModel + "' due to circular dependency " + encounteredModels.stream().map(o -> "'" + o + "'").collect(Collectors.joining("->")) + "->'" + modelLocation + "'!");
        encounteredModels.add(modelLocation);

        ModelInstance<?> model = modelResolver.apply(modelLocation);
        if(model != null){
            for(ResourceLocation dependency : model.getParentModels())
                this.validateParents(modelResolver, dependency, encounteredModels, rootModel);
        }

        encounteredModels.remove(encounteredModels.size() - 1);
    }

    public <T> T findProperty(ModelBakingContext context, Function<ModelBlock,T> property, T defaultValue){
        T value = this.findProperty(context::getModel, ModelInstance.of(DefaultModelTypes.BASE, this), property);
        return value == null ? defaultValue : value;
    }

    private <T> T findProperty(Function<ResourceLocation,ModelInstance<?>> modelResolver, ModelInstance<?> model, Function<ModelBlock,T> property){
        ModelBlock vanillaModel = model.getAsVanillaModel();
        if(vanillaModel != null){
            T value = property.apply(vanillaModel);
            if(value != null)
                return value;
        }

        for(ResourceLocation location : model.getParentModels()){
            ModelInstance<?> dependency = modelResolver.apply(location);
            if(dependency != null){
                T childValue = this.findProperty(modelResolver, dependency, property);
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
            String value = this.findProperty(context::getModel, model, m -> m.textures.get(finalCurrentKey));
            // If a key could not be found, return the missing texture
            if(value == null)
                return SpriteIdentifier.missing();

            // If a texture is found return it
            if(!value.isEmpty() && value.charAt(0) != '#' && IdentifierUtil.isValidIdentifier(value))
                return SpriteIdentifier.of(TextureAtlases.getBlocks(), new ResourceLocation(value));

            // Check if a key has already been encountered
            currentKey = value.charAt(0) == '#' ? value.substring(1) : value;
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
        modelStack.addLast(model);

        // If the model has elements, bake them
        List<? extends BlockPart> elements = null;
        if(model.getModelType() == DefaultModelTypes.BASE || model.getModelType() == DefaultModelTypes.CONNECTING){
            elements = ((BaseModelDataImpl)model.getModelData()).elements;
        }else{
            ModelBlock vanillaModel = model.getAsVanillaModel();
            if(vanillaModel != null)
                elements = vanillaModel.elements;
        }
        if(elements != null && !elements.isEmpty()){
            // Bake the faces of each element
            for(BlockPart element : elements){
                for(EnumFacing direction : element.mapFaces.keySet()){
                    BlockPartFace face = element.mapFaces.get(direction);
                    TextureAtlasSprite sprite = context.getTexture(this.resolveMaterial(context::getModel, modelStack, face.texture, context.getModelIdentifier()));
                    BakedQuad quad = FACE_BAKERY.makeBakedQuad(element.positionFrom, element.positionTo, face, sprite, direction, context.getTransformation().apply(Optional.empty()).orElse(TRSRTransformation.identity()), element.partRotation, false, element.shade);
                    EnumFacing cullDirection = face.cullFace == null ? null : context.getTransformation().apply(Optional.empty()).map(t -> t.rotate(face.cullFace)).orElse(face.cullFace);
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

        modelStack.removeLast();
    }

    public Set<SpriteIdentifier> gatherTextures(GatherTexturesContext context){
        Set<SpriteIdentifier> textures = new HashSet<>();
        this.gatherTextures(context, ModelInstance.of(DefaultModelTypes.BASE, this), new LinkedList<>(), textures::add);
        return textures;
    }

    private void gatherTextures(GatherTexturesContext context, ModelInstance<?> model, Deque<ModelInstance<?>> modelStack, Consumer<SpriteIdentifier> output){
        modelStack.addLast(model);

        // If the model has elements, get their textures
        List<? extends BlockPart> elements = null;
        if(model.getModelType() == DefaultModelTypes.BASE || model.getModelType() == DefaultModelTypes.CONNECTING){
            elements = ((BaseModelDataImpl)model.getModelData()).elements;
        }else{
            ModelBlock vanillaModel = model.getAsVanillaModel();
            if(vanillaModel != null)
                elements = vanillaModel.elements;
        }
        if(elements != null && !elements.isEmpty()){
            // Bake the faces of each element
            for(BlockPart element : elements){
                for(EnumFacing direction : element.mapFaces.keySet()){
                    BlockPartFace face = element.mapFaces.get(direction);
                    SpriteIdentifier sprite = this.resolveMaterial(context::getModel, modelStack, face.texture, null);
                    if(!sprite.equals(SpriteIdentifier.missing()))
                        output.accept(sprite);
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
                this.gatherTextures(context, dependency, modelStack, output);
        }

        modelStack.removeLast();
    }

    protected SpriteIdentifier resolveMaterial(Function<ResourceLocation,ModelInstance<?>> modelResolver, Deque<ModelInstance<?>> modelStack, String key, ResourceLocation rootModel){
        if(key.charAt(0) == '#')
            key = key.substring(1);

        // Repeatedly resolve key references until we get to a texture
        List<String> encounteredKeys = new ArrayList<>();
        encounteredKeys.add(key);
        String currentKey = key;
        while(true){
            String value = null;
            // Check models in the model stack
            for(ModelInstance<?> model : modelStack){
                ModelBlock vanillaModel = model.getAsVanillaModel();
                if(vanillaModel == null)
                    continue;
                value = vanillaModel.textures.get(currentKey);
                if(value != null)
                    break;
            }
            // If no value is found, check the parents of the last model
            if(value == null){
                String finalCurrentKey = currentKey;
                value = this.findProperty(modelResolver, modelStack.getLast(), model -> model.textures.get(finalCurrentKey));
            }
            // If a key could not be found, return the missing texture
            if(value == null)
                return SpriteIdentifier.missing();

            // If a texture is found return it
            if(!value.isEmpty() && value.charAt(0) != '#' && IdentifierUtil.isValidIdentifier(value))
                return SpriteIdentifier.of(TextureAtlases.getBlocks(), new ResourceLocation(value));

            // Check if a key has already been encountered
            currentKey = value.charAt(0) == '#' ? value.substring(1) : value;
            if(encounteredKeys.contains(currentKey)){
                FusionClient.LOGGER.warn("Unable to resolve texture due to circular references {}->'{}' in '{}'!", encounteredKeys.stream().map(o -> "'" + o + "'").collect(Collectors.joining("->")), currentKey, rootModel);
                return SpriteIdentifier.missing();
            }
            encounteredKeys.add(currentKey);
        }
    }
}
