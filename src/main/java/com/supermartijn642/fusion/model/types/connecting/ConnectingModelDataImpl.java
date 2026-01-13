package com.supermartijn642.fusion.model.types.connecting;

import com.google.common.collect.ImmutableMap;
import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.api.model.DefaultModelTypes;
import com.supermartijn642.fusion.api.model.ModelBakingContext;
import com.supermartijn642.fusion.api.model.ModelInstance;
import com.supermartijn642.fusion.api.model.data.ConnectingModelData;
import com.supermartijn642.fusion.api.predicate.ConnectionPredicate;
import com.supermartijn642.fusion.api.util.Either;
import com.supermartijn642.fusion.model.types.base.BaseModelDataImpl;
import com.supermartijn642.fusion.model.types.base.BaseModelElement;
import com.supermartijn642.fusion.model.types.base.BaseModelQuad;
import net.minecraft.client.renderer.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.BasicState;
import net.minecraftforge.common.model.TRSRTransformation;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Created 23/10/2023 by SuperMartijn642
 */
public class ConnectingModelDataImpl extends BaseModelDataImpl implements ConnectingModelData {

    private final Map<String,Either<ConnectionPredicate,String>> connections;

    public ConnectingModelDataImpl(BlockModel model, List<ResourceLocation> parents, List<ConnectingModelElement> elements, Map<String,Either<ConnectionPredicate,String>> connections){
        //noinspection rawtypes,unchecked
        super(model, parents, (List)elements);
        this.connections = ImmutableMap.copyOf(connections);
    }

    @Override
    public List<ConnectingModelElement> getElements(){
        //noinspection rawtypes,unchecked
        return (List)super.getElements();
    }

    @Override
    public ConnectionPredicate getConnectionPredicate(String texture){
        return this.connections.get(texture).leftOrElse(null);
    }

    @Override
    public ConnectionPredicate getDefaultConnectionPredicate(){
        return this.getConnectionPredicate("default");
    }

    @Override
    public Map<String,Either<ConnectionPredicate,String>> getAllConnectionPredicates(){
        return this.connections;
    }

    @Override
    public List<BaseModelQuad> bakeQuads(ModelBakingContext context){
        List<BaseModelQuad> quads = new ArrayList<>();
        this.bakeQuads(context, ModelInstance.of(DefaultModelTypes.CONNECTING, this), new LinkedList<>(), quads::add);
        return quads;
    }

    private void bakeQuads(ModelBakingContext context, ModelInstance<?> model, Deque<ModelInstance<?>> modelStack, Consumer<BaseModelQuad> output){
        modelStack.addLast(model);

        // If the model has elements, bake them
        List<? extends BlockPart> elements = null;
        if(model.getModelType() == DefaultModelTypes.BASE || model.getModelType() == DefaultModelTypes.CONNECTING){
            elements = ((BaseModelDataImpl)model.getModelData()).getElements();
        }else{
            BlockModel vanillaModel = model.getAsVanillaModel();
            if(vanillaModel != null){
                if(vanillaModel == ModelBakery.GENERATION_MARKER)
                    elements = this.generateItemModel(context, modelStack);
                else
                    elements = vanillaModel.elements;
            }
        }
        if(elements != null && !elements.isEmpty()){
            // Bake the faces of each element
            for(BlockPart element : elements){
                for(Direction direction : element.faces.keySet()){
                    BlockPartFace face = element.faces.get(direction);
                    TextureAtlasSprite sprite = context.getTexture(this.resolveMaterial(context::getModel, modelStack, face.texture, context.getModelIdentifier()));
                    BakedQuad quad = FACE_BAKERY.bakeQuad(element.from, element.to, face, sprite, direction, new BasicState(context.getTransformation().getState().apply(Optional.empty()).orElse(TRSRTransformation.identity()), context.getTransformation().isUvLocked()), element.rotation, element.shade);
                    Direction cullDirection = face.cullForDirection == null ? null : context.getTransformation().getState().apply(Optional.empty()).map(t -> t.rotateTransform(face.cullForDirection)).orElse(face.cullForDirection);
                    Integer lightEmission = element instanceof BaseModelElement ? ((BaseModelElement)element).light_emission : null;
                    String connectionsKey = element instanceof ConnectingModelElement && ((ConnectingModelElement)element).faceConnectionKeys.containsKey(direction) ? ((ConnectingModelElement)element).faceConnectionKeys.get(direction) : face.texture;
                    ConnectionPredicate predicate = this.resolveConnectionKey(context, modelStack, connectionsKey);
                    output.accept(new ConnectingModelQuad(quad, cullDirection, lightEmission, predicate));
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

    private ConnectionPredicate resolveConnectionKey(ModelBakingContext context, Deque<ModelInstance<?>> modelStack, String key){
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
                Either<ConnectionPredicate,String> either = this.connections.get(currentKey);
                if(either == null)
                    continue;
                // If a predicate is found return it
                if(either.isLeft())
                    return either.left();
                // Update the current key
                newKey = either.right();
                break;
            }
            // If no connections map contains the key, use the texture references
            if(newKey != null){
                for(ModelInstance<?> model : modelStack){
                    BlockModel vanillaModel = model.getAsVanillaModel();
                    if(vanillaModel == null)
                        continue;
                    String value = vanillaModel.textureMap.get(currentKey);
                    if(value != null){
                        newKey = value;
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

    private static Either<ConnectionPredicate,String> findConnectionsEntry(ModelBakingContext context, ModelInstance<?> model, String key){
        // Check the model itself
        if(model.getModelType() == DefaultModelTypes.CONNECTING){
            Either<ConnectionPredicate,String> either = ((ConnectingModelDataImpl)model.getModelData()).connections.get(key);
            if(either != null)
                return either;
        }
        // Check parent models
        for(ResourceLocation location : model.getParentModels()){
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
