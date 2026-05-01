package com.supermartijn642.fusion.model.types.connecting;

import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.api.model.BlockModelBakingContext;
import com.supermartijn642.fusion.api.model.DefaultModelTypes;
import com.supermartijn642.fusion.api.model.ModelInstance;
import com.supermartijn642.fusion.api.model.data.ConnectingModelData;
import com.supermartijn642.fusion.api.predicate.ConnectionPredicate;
import com.supermartijn642.fusion.api.util.Either;
import com.supermartijn642.fusion.model.types.base.BaseModelDataImpl;
import com.supermartijn642.fusion.model.types.base.BaseModelElement;
import com.supermartijn642.fusion.model.types.base.BaseModelQuad;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.client.resources.model.cuboid.*;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Created 23/10/2023 by SuperMartijn642
 */
public class ConnectingModelDataImpl extends BaseModelDataImpl implements ConnectingModelData {

    private final Map<String,Either<ConnectionPredicate,String>> connections;

    public ConnectingModelDataImpl(CuboidModel model, List<Identifier> parents, List<ConnectingModelElement> elements, Map<String,Either<ConnectionPredicate,String>> connections){
        //noinspection rawtypes,unchecked
        super(model, parents, (List)elements);
        this.connections = Map.copyOf(connections);
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
    public List<BaseModelQuad> bakeQuads(BlockModelBakingContext context){
        List<BaseModelQuad> quads = new ArrayList<>();
        this.bakeQuads(context, ModelInstance.of(DefaultModelTypes.CONNECTING, this), new LinkedList<>(), quads::add);
        return quads;
    }

    private void bakeQuads(BlockModelBakingContext context, ModelInstance<?> model, Deque<ModelInstance<?>> modelStack, Consumer<BaseModelQuad> output){
        modelStack.addLast(model);

        // If the model has elements, bake them
        List<CuboidModelElement> vanillaElements = null;
        List<? extends BaseModelElement> customElements = null;
        if(model.getModelType() == DefaultModelTypes.BASE || model.getModelType() == DefaultModelTypes.CONNECTING){
            customElements = ((BaseModelDataImpl)model.getModelData()).getElements();
            vanillaElements = customElements.stream().map(e -> e.original).toList();
        }else{
            UnbakedModel vanillaModel = model.getAsVanillaModel();
            if(vanillaModel instanceof ItemModelGenerator){
                // Special case layered item models
                ConnectionPredicate predicate = this.resolveConnectionKey(context, modelStack, ConnectingModelType.DEFAULT_CONNECTION_KEY);
                QuadCollection quads = this.generateItemModel(context, modelStack);
                for(Direction cullDirection : Direction.values()){
                    for(BakedQuad quad : quads.getQuads(cullDirection)){
                        output.accept(new ConnectingModelQuad(quad, cullDirection, predicate));
                    }
                }
                for(BakedQuad quad : quads.getQuads(null))
                    output.accept(new ConnectingModelQuad(quad, null, predicate));
            }else if(vanillaModel != null && vanillaModel.geometry() instanceof UnbakedCuboidGeometry geometry)
                vanillaElements = geometry.elements();
        }
        if(vanillaElements != null && !vanillaElements.isEmpty()){
            // Bake the faces of each element
            for(int i = 0; i < vanillaElements.size(); i++){
                CuboidModelElement element = vanillaElements.get(i);
                BaseModelElement customElement = customElements != null && i < customElements.size() ? customElements.get(i) : null;
                for(Direction direction : element.faces().keySet()){
                    CuboidFace face = element.faces().get(direction);
                    Material.Baked sprite = context.getMaterial(this.resolveMaterial(context, modelStack, face.texture()));
                    BakedQuad quad = FaceBakery.bakeQuad(context.getModelBaker(), element.from(), element.to(), face, sprite, direction, context.getTransformation(), element.rotation(), element.shade(), element.lightEmission());
                    Direction cullDirection = face.cullForDirection() != null ? Direction.rotate(context.getTransformation().transformation().getMatrix(), face.cullForDirection()) : null;
                    String connectionsKey = customElement instanceof ConnectingModelElement && ((ConnectingModelElement)customElement).faceConnectionKeys.containsKey(direction) ? ((ConnectingModelElement)customElement).faceConnectionKeys.get(direction) : face.texture();
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
            if(newKey == null){
                for(ModelInstance<?> model : modelStack){
                    UnbakedModel vanillaModel = model.getAsVanillaModel();
                    if(vanillaModel == null)
                        continue;
                    TextureSlots.SlotContents material = vanillaModel.textureSlots().values().get(currentKey);
                    if(material != null){
                        newKey = material instanceof TextureSlots.Value ?
                            ((TextureSlots.Value)material).material().sprite().toString() :
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
            Either<ConnectionPredicate,String> either = ((ConnectingModelDataImpl)model.getModelData()).connections.get(key);
            if(either != null)
                return either;
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
