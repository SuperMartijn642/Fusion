package com.supermartijn642.fusion.model.types.connecting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.custom.DefaultModelProperties;
import com.supermartijn642.fusion.api.model.custom.geometry.CuboidModelGeometry;
import com.supermartijn642.fusion.api.model.types.connecting.ConnectingModelData;
import com.supermartijn642.fusion.api.texture.types.connecting.predicates.ConnectionPredicate;
import com.supermartijn642.fusion.api.texture.types.connecting.predicates.DefaultConnectionPredicates;
import com.supermartijn642.fusion.api.texture.types.connecting.predicates.FusionConnectionPredicateRegistry;
import com.supermartijn642.fusion.api.util.Either;
import com.supermartijn642.fusion.api.util.Property;
import com.supermartijn642.fusion.model.types.base.BaseModelType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Created 27/04/2023 by SuperMartijn642
 */
public class ConnectingModelType extends BaseModelType<ConnectingModelData,ConnectingModelDataBuilderImpl> {

    @Override
    public <X, C> Optional<X> getProperty(Property<X,C> property, C context, ConnectingModelData data){
        if(property == DefaultModelProperties.CONNECTION_PREDICATES)
            //noinspection unchecked
            return Optional.of((X)data.getAllConnectionPredicates());
        if(property == DefaultModelProperties.CONNECTION_PREDICATE)
            //noinspection unchecked,SuspiciousMethodCalls
            return Optional.ofNullable((X)data.getAllConnectionPredicates().get(context));
        return super.getProperty(property, context, data);
    }

    @Override
    protected ConnectingModelDataBuilderImpl builder(){
        return (ConnectingModelDataBuilderImpl)ConnectingModelData.builder();
    }

    @Override
    protected void deserialize(JsonObject json, ConnectingModelDataBuilderImpl builder){
        // Deserialize the base model
        super.deserialize(json, builder);
        // Deserialize all the predicates from the 'connections' array
        if(json.has("connections")){
            JsonElement connectionsElement = json.get("connections");
            if(connectionsElement.isJsonArray() || (connectionsElement.isJsonObject() && connectionsElement.getAsJsonObject().has("type"))) // Legacy array
                builder.defaultConnections(loadPredicate(connectionsElement, "connections"));
            else if(connectionsElement.isJsonObject()){ // Load predicates per texture
                JsonObject object = connectionsElement.getAsJsonObject();
                if(object.size() == 0)
                    throw new JsonParseException("Property 'connections' must have a 'type' key or keys per texture!");
                for(Map.Entry<String,JsonElement> entry : object.entrySet()){
                    String key = entry.getKey();
                    if(entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isString()){
                        String reference = entry.getValue().getAsString();
                        if(reference.isEmpty() || reference.charAt(0) != '#')
                            throw new JsonParseException("Reference for connections key '" + key + "' must start with '#'!");
                        builder.connections(key, reference.substring(1));
                    }else
                        builder.connections(key, loadPredicate(object.get(key), key));
                }
            }else
                throw new JsonParseException("Property 'connections' must be an object or array!");
        }
    }

    @Override
    public JsonObject serialize(ConnectingModelData data){
        // Serialize base model
        JsonObject json = super.serialize(data);
        // Create 'connections' array with all the predicates
        Map<String,Either<String,ConnectionPredicate>> predicates = data.getAllConnectionPredicates();
        if(predicates.size() == 1 && predicates.containsKey(ConnectingModelData.DEFAULT_KEY) && predicates.get(ConnectingModelData.DEFAULT_KEY).isRight())
            json.add("connections", FusionConnectionPredicateRegistry.serializeConnectionPredicate(predicates.get(ConnectingModelData.DEFAULT_KEY).right()));
        else if(!predicates.isEmpty()){
            JsonObject connectionsJson = new JsonObject();
            predicates.forEach((key, predicate) -> {
                if(predicate.isRight())
                    connectionsJson.add(key, FusionConnectionPredicateRegistry.serializeConnectionPredicate(predicate.right()));
                else
                    connectionsJson.addProperty(key, '#' + predicate.left());
            });
            json.add("connections", connectionsJson);
        }
        return json;
    }

    public static ConnectionPredicate loadPredicate(JsonElement element, String key){
        if(element.isJsonArray()){
            JsonArray array = element.getAsJsonArray();
            List<ConnectionPredicate> subPredicates = new ArrayList<>();
            for(JsonElement predicateElements : array){
                if(!predicateElements.isJsonObject())
                    throw new JsonParseException("Predicate '" + key + "' must only contain objects!");
                ConnectionPredicate predicate = FusionConnectionPredicateRegistry.deserializeConnectionPredicate(predicateElements.getAsJsonObject());
                subPredicates.add(predicate);
            }
            return DefaultConnectionPredicates.or(subPredicates.toArray(ConnectionPredicate[]::new));
        }
        if(element.isJsonObject())
            return FusionConnectionPredicateRegistry.deserializeConnectionPredicate(element.getAsJsonObject());
        throw new JsonParseException("Predicate '" + key + "' must be an object or an array!");
    }

    @Override
    protected CuboidModelGeometry.Face.Builder deserializeFace(JsonObject json){
        CuboidModelGeometry.Face.Builder builder = super.deserializeFace(json);
        JsonElement connectionsJson = json.get("connections");
        if(connectionsJson != null){
            if(!connectionsJson.isJsonPrimitive() || !connectionsJson.getAsJsonPrimitive().isString())
                throw new JsonParseException("Element face property 'connections' must be a string!");
            String key = connectionsJson.getAsString();
            if(!key.isEmpty() && key.charAt(0) == '#')
                key = key.substring(1);
            if(key.isEmpty())
                throw new JsonParseException("Element face property 'connections' must not be empty!");
            builder.property(DefaultModelProperties.FACE_CONNECTIONS_KEY, key);
        }
        return builder;
    }

    @Override
    protected JsonObject serializeFace(CuboidModelGeometry.Face face){
        JsonObject json = super.serializeFace(face);
        Optional<String> key = face.getProperty(DefaultModelProperties.FACE_CONNECTIONS_KEY);
        key.ifPresent(s -> json.addProperty("connections", s));
        return json;
    }
}
