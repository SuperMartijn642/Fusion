package com.supermartijn642.fusion.model.types.connecting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.BlockModelBakingContext;
import com.supermartijn642.fusion.api.model.DefaultModelTypes;
import com.supermartijn642.fusion.api.model.ItemModelBakingContext;
import com.supermartijn642.fusion.api.model.ModelType;
import com.supermartijn642.fusion.api.model.data.BaseModelData;
import com.supermartijn642.fusion.api.model.data.ConnectingModelData;
import com.supermartijn642.fusion.api.predicate.ConnectionPredicate;
import com.supermartijn642.fusion.api.predicate.DefaultConnectionPredicates;
import com.supermartijn642.fusion.api.predicate.FusionPredicateRegistry;
import com.supermartijn642.fusion.api.util.Either;
import com.supermartijn642.fusion.model.types.base.BaseModelDataImpl;
import com.supermartijn642.fusion.model.types.base.BaseModelElement;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.client.resources.model.cuboid.ItemTransforms;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Created 27/04/2023 by SuperMartijn642
 */
public class ConnectingModelType implements ModelType<ConnectingModelData> {

    public static final String DEFAULT_CONNECTION_KEY = "default";

    @Override
    public Collection<Identifier> getModelDependencies(ConnectingModelData data){
        return DefaultModelTypes.VANILLA.getModelDependencies(data.getVanillaModel());
    }

    @Override
    @Nullable
    public UnbakedModel getAsVanillaModel(ConnectingModelData data){
        return DefaultModelTypes.VANILLA.getAsVanillaModel(data.getVanillaModel());
    }

    @Override
    public List<Identifier> getParentModels(ConnectingModelData data){
        return data.getParents();
    }

    @Override
    public BlockStateModel bakeBlockModel(BlockModelBakingContext context, ConnectingModelData data){
        // Check for circular dependencies
        ((ConnectingModelDataImpl)data).validateParents(context);
        // Bake the quads
        //noinspection unchecked,rawtypes
        List<ConnectingModelQuad> quads = (List)((ConnectingModelDataImpl)data).bakeQuads(context);
        // Gather remaining model properties
        Boolean ambientOcclusion = ((BaseModelDataImpl)data).findProperty(context, UnbakedModel::ambientOcclusion, null);
        Material.Baked particleSprite = context.getMaterial(((BaseModelDataImpl)data).findParticleSprite(context));
        // Finally, create the model
        return new ConnectingBakedModel(
            quads,
            ambientOcclusion,
            particleSprite
        );
    }

    @Override
    public ItemModel bakeItemModel(ItemModelBakingContext context, ConnectingModelData data){
        // Check for circular dependencies
        ((ConnectingModelDataImpl)data).validateParents(context);
        // Bake the quads
        //noinspection unchecked,rawtypes
        List<ConnectingModelQuad> quads = (List)((ConnectingModelDataImpl)data).bakeQuads(context);
        // Gather remaining model properties
        boolean usesBlockLight = ((BaseModelDataImpl)data).findProperty(context, UnbakedModel::guiLight, UnbakedModel.GuiLight.SIDE).lightLikeBlock();
        Material.Baked particleSprite = context.getMaterial(((BaseModelDataImpl)data).findParticleSprite(context));
        //noinspection deprecation
        ItemTransforms transforms = new ItemTransforms(
            ((BaseModelDataImpl)data).findItemTransform(context, ItemDisplayContext.THIRD_PERSON_LEFT_HAND),
            ((BaseModelDataImpl)data).findItemTransform(context, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND),
            ((BaseModelDataImpl)data).findItemTransform(context, ItemDisplayContext.FIRST_PERSON_LEFT_HAND),
            ((BaseModelDataImpl)data).findItemTransform(context, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND),
            ((BaseModelDataImpl)data).findItemTransform(context, ItemDisplayContext.HEAD),
            ((BaseModelDataImpl)data).findItemTransform(context, ItemDisplayContext.GUI),
            ((BaseModelDataImpl)data).findItemTransform(context, ItemDisplayContext.GROUND),
            ((BaseModelDataImpl)data).findItemTransform(context, ItemDisplayContext.FIXED),
            ((BaseModelDataImpl)data).findItemTransform(context, ItemDisplayContext.ON_SHELF)
        );
        // Finally, create the model
        return new ConnectingItemModel(
            context.getTintSources(),
            quads,
            new ModelRenderProperties(usesBlockLight, particleSprite, transforms),
            context.getTransformation().transformation().getMatrix()
        );
    }

    @Override
    public ConnectingModelData deserialize(JsonObject json) throws JsonParseException{
        // Deserialize the base model
        BaseModelData base = DefaultModelTypes.BASE.deserialize(json);
        // Deserialize all the predicates from the 'connections' array
        Map<String,Either<ConnectionPredicate,String>> connections = new LinkedHashMap<>(); // This should maintain order
        connections.put("default", Either.left(DefaultConnectionPredicates.isSameState()));
        if(json.has("connections")){
            JsonElement connectionsElement = json.get("connections");
            if(connectionsElement.isJsonArray() || (connectionsElement.isJsonObject() && connectionsElement.getAsJsonObject().has("type"))) // Legacy array
                connections.put("default", Either.left(loadPredicate(connectionsElement, "connections")));
            else if(connectionsElement.isJsonObject()){ // Load predicates per texture
                JsonObject object = connectionsElement.getAsJsonObject();
                if(object.isEmpty())
                    throw new JsonParseException("Property 'connections' must have a 'type' key or keys per texture!");
                for(String key : object.keySet()){
                    if(object.get(key).isJsonPrimitive() && object.getAsJsonPrimitive(key).isString()){
                        String reference = object.get(key).getAsString();
                        if(reference.isEmpty() || reference.charAt(0) != '#')
                            throw new JsonParseException("Reference for connections key '" + key + "' must start with '#'!");
                        connections.put(key, Either.right(reference.substring(1)));
                    }else
                        connections.put(key, Either.left(loadPredicate(object.get(key), key)));
                }
            }else
                throw new JsonParseException("Property 'connections' must be an array!");
        }
        // Read the 'connections' keys for all element faces
        List<ConnectingModelElement> elements = new ArrayList<>(((BaseModelDataImpl)base).getElements().size());
        JsonArray elementsJson = json.getAsJsonArray("elements");
        for(int i = 0; i < ((BaseModelDataImpl)base).getElements().size(); i++){
            JsonObject elementFaces = elementsJson.get(i).getAsJsonObject().getAsJsonObject("faces");
            BaseModelElement baseElement = ((BaseModelDataImpl)base).getElements().get(i);
            Map<Direction,String> connectionKeys = null;
            if(elementFaces != null){
                for(Direction side : Direction.values()){
                    if(elementFaces.has(side.getName()) && elementFaces.get(side.getName()).isJsonObject() && elementFaces.getAsJsonObject(side.getName()).has("connections")){
                        JsonElement connectionsJson = elementFaces.getAsJsonObject(side.getName()).get("connections");
                        if(!connectionsJson.isJsonPrimitive() || !connectionsJson.getAsJsonPrimitive().isString())
                            throw new JsonParseException("Face property 'connections' must be a string!");
                        String key = connectionsJson.getAsString();
                        if(key.isEmpty())
                            throw new JsonParseException("Face property 'connections' must not be empty!");
                        if(connectionKeys == null)
                            connectionKeys = new EnumMap<>(Direction.class);
                        connectionKeys.put(side, key);
                    }
                }
            }
            elements.add(new ConnectingModelElement(
                baseElement.original.from(),
                baseElement.original.to(),
                baseElement.original.faces(),
                baseElement.original.rotation(),
                baseElement.original.shade(),
                baseElement.original.lightEmission(),
                connectionKeys
            ));
        }
        return new ConnectingModelDataImpl(base.getVanillaModel(), base.getParents(), elements, connections);
    }

    @Override
    public JsonObject serialize(ConnectingModelData value){
        // Serialize base model
        JsonObject json = DefaultModelTypes.BASE.serialize(value);
        // Create an array with all the serialized predicates
        Map<String,Either<ConnectionPredicate,String>> predicates = value.getAllConnectionPredicates();
        if(predicates.size() == 1 && predicates.containsKey("default") && predicates.get("default").isLeft())
            json.add("connections", FusionPredicateRegistry.serializeConnectionPredicate(predicates.get("default").left()));
        else if(!predicates.isEmpty()){
            JsonObject connectionsJson = new JsonObject();
            predicates.forEach((key, predicate) -> {
                if(predicate.isLeft())
                    connectionsJson.add(key, FusionPredicateRegistry.serializeConnectionPredicate(predicate.left()));
                else
                    connectionsJson.addProperty(key, '#' + predicate.right());
            });
            json.add("connections", connectionsJson);
        }
        // Add 'connections' property to element faces
        for(int i = 0; i < ((ConnectingModelDataImpl)value).getElements().size(); i++){
            Map<Direction,String> connectionKeys = ((ConnectingModelDataImpl)value).getElements().get(i).faceConnectionKeys;
            if(connectionKeys.isEmpty())
                continue;
            JsonObject elementFaces = json.getAsJsonArray("elements").get(i).getAsJsonObject().getAsJsonObject("faces");
            if(elementFaces == null)
                continue;
            for(Direction side : connectionKeys.keySet()){
                if(elementFaces.has(side.getName()))
                    elementFaces.getAsJsonObject(side.getName()).addProperty("connections", connectionKeys.get(side));
            }
        }
        return json;
    }

    private static ConnectionPredicate loadPredicate(JsonElement element, String key){
        if(element.isJsonArray()){
            JsonArray array = element.getAsJsonArray();
            List<ConnectionPredicate> subPredicates = new ArrayList<>();
            for(JsonElement predicateElements : array){
                if(!predicateElements.isJsonObject())
                    throw new JsonParseException("Predicate '" + key + "' must only contain objects!");
                ConnectionPredicate predicate = FusionPredicateRegistry.deserializeConnectionPredicate(predicateElements.getAsJsonObject());
                subPredicates.add(predicate);
            }
            return DefaultConnectionPredicates.or(subPredicates.toArray(ConnectionPredicate[]::new));
        }
        if(element.isJsonObject())
            return FusionPredicateRegistry.deserializeConnectionPredicate(element.getAsJsonObject());
        throw new JsonParseException("Predicate '" + key + "' must be an object or an array!");
    }
}
