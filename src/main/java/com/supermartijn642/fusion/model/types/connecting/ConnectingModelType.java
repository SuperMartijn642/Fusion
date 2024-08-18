package com.supermartijn642.fusion.model.types.connecting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.DefaultModelTypes;
import com.supermartijn642.fusion.api.model.ModelBakingContext;
import com.supermartijn642.fusion.api.model.ModelType;
import com.supermartijn642.fusion.api.model.data.ConnectingModelData;
import com.supermartijn642.fusion.api.predicate.ConnectionPredicate;
import com.supermartijn642.fusion.api.predicate.DefaultConnectionPredicates;
import com.supermartijn642.fusion.api.predicate.FusionPredicateRegistry;
import com.supermartijn642.fusion.api.util.Pair;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created 27/04/2023 by SuperMartijn642
 */
public class ConnectingModelType implements ModelType<ConnectingModelData> {

    public static final ResourceLocation DEFAULT_CONNECTION_KEY = ResourceLocation.fromNamespaceAndPath("fusion", "default");

    @Override
    public Collection<ResourceLocation> getModelDependencies(ConnectingModelData data){
        return DefaultModelTypes.VANILLA.getModelDependencies(data.getVanillaModel());
    }

    @Override
    @Nullable
    public BlockModel getAsVanillaModel(ConnectingModelData data){
        return DefaultModelTypes.VANILLA.getAsVanillaModel(data.getVanillaModel());
    }

    @Override
    public BakedModel bake(ModelBakingContext context, ConnectingModelData data){
        BakedModel model = DefaultModelTypes.VANILLA.bake(context, data.getVanillaModel());
        Map<ResourceLocation,ConnectionPredicate> predicates = data.getAllConnectionPredicates().entrySet().stream()
            .map(entry -> Pair.of(entry.getKey().equals("default") ? DEFAULT_CONNECTION_KEY : data.getVanillaModel().getMaterial(entry.getKey()).texture(), entry.getValue()))
            .filter(pair -> !pair.left().equals(MissingTextureAtlasSprite.getLocation()))
            .collect(Collectors.toUnmodifiableMap(
                Pair::left,
                Pair::right,
                DefaultConnectionPredicates::or
            ));
        return new ConnectingBakedModel(model, context.getTransformation().getRotation(), predicates);
    }

    @Override
    public ConnectingModelData deserialize(JsonObject json) throws JsonParseException{
        // Deserialize the vanilla model
        BlockModel model = DefaultModelTypes.VANILLA.deserialize(json);
        // Deserialize all the predicates from the 'connections' array
        Map<String,ConnectionPredicate> predicates = new HashMap<>();
        predicates.put("default", DefaultConnectionPredicates.isSameState());
        if(json.has("connections")){
            JsonElement connectionsElement = json.get("connections");
            if(connectionsElement.isJsonArray() || (connectionsElement.isJsonObject() && connectionsElement.getAsJsonObject().has("type"))) // Legacy array
                predicates.put("default", loadPredicate(connectionsElement, "connections"));
            else if(connectionsElement.isJsonObject()){ // Load predicates per texture
                JsonObject object = connectionsElement.getAsJsonObject();
                if(object.isEmpty())
                    throw new JsonParseException("Property 'connections' must have a 'type' key or keys per texture!");
                for(String texture : object.keySet())
                    predicates.put(texture, loadPredicate(object.get(texture), texture));
            }else
                throw new JsonParseException("Property 'connections' must be an array!");
        }

        return new ConnectingModelDataImpl(model, predicates);
    }

    @Override
    public JsonObject serialize(ConnectingModelData value){
        JsonObject json = DefaultModelTypes.VANILLA.serialize(value.getVanillaModel());
        // Create an array with all the serialized predicates
        Map<String,ConnectionPredicate> predicates = value.getAllConnectionPredicates();
        if(predicates.size() == 1 && predicates.containsKey("default"))
            json.add("connections", FusionPredicateRegistry.serializeConnectionPredicate(predicates.get("default")));
        else if(!predicates.isEmpty()){
            JsonObject connectionsJson = new JsonObject();
            predicates.forEach((texture, predicate) -> connectionsJson.add(texture, FusionPredicateRegistry.serializeConnectionPredicate(predicate)));
            json.add("connections", connectionsJson);
        }
        return json.isEmpty() ? null : json;
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
