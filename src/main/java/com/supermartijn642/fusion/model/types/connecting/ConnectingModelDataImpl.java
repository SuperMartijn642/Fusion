package com.supermartijn642.fusion.model.types.connecting;

import com.google.common.collect.ImmutableMap;
import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.api.model.BlockModelBakingContext;
import com.supermartijn642.fusion.api.model.DefaultModelTypes;
import com.supermartijn642.fusion.api.model.ModelInstance;
import com.supermartijn642.fusion.api.model.data.ConnectingModelData;
import com.supermartijn642.fusion.api.predicate.ConnectionPredicate;
import com.supermartijn642.fusion.api.util.Either;
import com.supermartijn642.fusion.model.types.base.BaseModelDataImpl;
import com.supermartijn642.fusion.model.types.base.BaseModelQuad;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Created 23/10/2023 by SuperMartijn642
 */
public class ConnectingModelDataImpl extends BaseModelDataImpl implements ConnectingModelData {

    private final Map<String,ConnectionPredicate> predicates;
    private final Map<String,String> connectionReferences;

    public ConnectingModelDataImpl(BlockModel model, List<Identifier> parents, List<ConnectingModelElement> elements, Map<String,ConnectionPredicate> predicates, Map<String,String> references){
        //noinspection rawtypes,unchecked
        super(model, parents, (List)elements);
        this.predicates = ImmutableMap.copyOf(predicates);
        this.connectionReferences = ImmutableMap.copyOf(references);
    }

    @Override
    public List<ConnectingModelElement> getElements(){
        //noinspection rawtypes,unchecked
        return (List)super.getElements();
    }

    @Override
    public ConnectionPredicate getConnectionPredicate(String texture){
        return this.predicates.get(texture);
    }

    @Override
    public ConnectionPredicate getDefaultConnectionPredicate(){
        return this.getConnectionPredicate("default");
    }

    @Override
    public Map<String,ConnectionPredicate> getAllConnectionPredicates(){
        return this.predicates;
    }

    public Map<String,String> getConnectionReferences(){
        return this.connectionReferences;
    }

    @Override
    public List<BaseModelQuad> bakeQuads(BlockModelBakingContext context){
        List<BaseModelQuad> quads = new ArrayList<>();
        this.bakeQuads(context, ModelInstance.of(DefaultModelTypes.CONNECTING, this), new LinkedList<>(), quads::add);
        return quads;
    }

    private void bakeQuads(BlockModelBakingContext context, ModelInstance<?> model, Deque<ModelInstance<?>> modelStack, Consumer<BaseModelQuad> output){
        modelStack.addLast(model);

        // If the model has elements, bake them
        List<? extends BlockElement> elements = null;
        if(model.getModelType() == DefaultModelTypes.BASE || model.getModelType() == DefaultModelTypes.CONNECTING){
            elements = ((BaseModelDataImpl)model.getModelData()).getElements();
        }else{
            UnbakedModel vanillaModel = model.getAsVanillaModel();
            if(vanillaModel instanceof ItemModelGenerator)
                elements = this.generateItemModel(context, modelStack);
            else if(vanillaModel != null && vanillaModel.geometry() instanceof SimpleUnbakedGeometry geometry)
                elements = geometry.elements();
        }
        if(elements != null && !elements.isEmpty()){
            // Bake the faces of each element
            for(BlockElement element : elements){
                for(Direction direction : element.faces().keySet()){
                    BlockElementFace face = element.faces().get(direction);
                    TextureAtlasSprite sprite = context.getTexture(this.resolveMaterial(context, modelStack, face.texture()));
                    BakedQuad quad = FaceBakery.bakeQuad(context.getModelBaker().parts(), element.from(), element.to(), face, sprite, direction, context.getTransformation(), element.rotation(), element.shade(), element.lightEmission());
                    Direction cullDirection = face.cullForDirection() != null ? Direction.rotate(context.getTransformation().transformation().getMatrix(), face.cullForDirection()) : null;
                    String connectionsKey = element instanceof ConnectingModelElement && ((ConnectingModelElement)element).faceConnectionKeys.containsKey(direction) ? ((ConnectingModelElement)element).faceConnectionKeys.get(direction) : face.texture();
                    ConnectionPredicate predicate = this.resolveConnectionKey(context, modelStack, connectionsKey);
                    output.accept(new ConnectingModelQuad(quad, cullDirection, predicate));
                }
            }
            // If the model had elements, ignore parent models' elements
            modelStack.pop();
            return;
        }

        // If the model has no elements, check for parents
        for(Identifier location : model.getParentModels()){
            ModelInstance<?> dependency = context.getModel(location);
            if(dependency != null)
                this.bakeQuads(context, dependency, modelStack, output);
        }

        modelStack.removeLast();
    }

    private ConnectionPredicate resolveConnectionKey(BlockModelBakingContext context, Deque<ModelInstance<?>> modelStack, String key){
        if(key.charAt(0) == '#')
            key = key.substring(1);

        // Repeatedly resolve key references until we get to predicates
        List<String> encounteredKeys = new ArrayList<>();
        encounteredKeys.add(key);
        String currentKey = key;
        while(true){
            String newKey = null;
            // First check for the connections map
            for(ModelInstance<?> model : modelStack){
                if(model.getModelType() != DefaultModelTypes.CONNECTING)
                    continue;
                ConnectionPredicate predicate = ((ConnectingModelDataImpl)model.getModelData()).predicates.get(currentKey);
                // If a predicate is found return it
                if(predicate != null)
                    return predicate;
                String reference = ((ConnectingModelDataImpl)model.getModelData()).connectionReferences.get(currentKey);
                if(reference != null){
                    newKey = reference;
                    break;
                }
            }
            // If no connections map contains the key, use the texture references
            if(newKey == null){
                for(ModelInstance<?> model : modelStack){
                    UnbakedModel vanillaModel = model.getAsVanillaModel();
                    if(vanillaModel == null)
                        continue;
                    TextureSlots.SlotContents material = vanillaModel.textureSlots().values().get(currentKey);
                    if(material != null){
                        newKey = material instanceof TextureSlots.Value ?
                            ((TextureSlots.Value)material).material().texture().toString() :
                            ((TextureSlots.Reference)material).target();
                        break;
                    }
                }
            }
            // Check parent models for connection keys
            if(newKey == null){
                Either<ConnectionPredicate,String> entry = findConnectionsEntry(context, modelStack.getLast(), currentKey);
                if(entry != null && entry.isLeft())
                    return entry.left();
                else if(entry != null && entry.isRight())
                    newKey = entry.right();
            }
            // If a key could not be found, try the default key
            if(newKey == null && !currentKey.equals(ConnectingModelType.DEFAULT_CONNECTION_KEY))
                newKey = ConnectingModelType.DEFAULT_CONNECTION_KEY;
            // If the default key also cannot be found, return null
            if(newKey == null)
                return null;

            // Check if a key has already been encountered
            currentKey = newKey;
            if(currentKey.charAt(0) == '#')
                currentKey = currentKey.substring(1);
            if(encounteredKeys.contains(currentKey)){
                FusionClient.LOGGER.warn("Unable to resolve connections due to circular references {}->'{}' in '{}'!", encounteredKeys.stream().map(o -> "'" + o + "'").collect(Collectors.joining("->")), currentKey, context.getModelIdentifier());
                return null;
            }
            encounteredKeys.add(currentKey);
        }
    }

    private static Either<ConnectionPredicate,String> findConnectionsEntry(BlockModelBakingContext context, ModelInstance<?> model, String key){
        // Check the model itself
        if(model.getModelType() == DefaultModelTypes.CONNECTING){
            ConnectionPredicate predicate = ((ConnectingModelDataImpl)model.getModelData()).predicates.get(key);
            if(predicate != null)
                return Either.left(predicate);
            String reference = ((ConnectingModelDataImpl)model.getModelData()).connectionReferences.get(key);
            if(reference != null)
                return Either.right(reference);
        }
        // Check parent models
        for(Identifier location : model.getParentModels()){
            ModelInstance<?> parent = context.getModel(location);
            if(parent != null){
                Either<ConnectionPredicate,String> entry = findConnectionsEntry(context, parent, key);
                if(entry != null)
                    return entry;
            }
        }
        return null;
    }
}
