package com.supermartijn642.fusion.model.types.connecting;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.*;
import com.supermartijn642.fusion.api.model.data.ConnectingModelData;
import com.supermartijn642.fusion.api.predicate.ConnectionPredicate;
import com.supermartijn642.fusion.api.predicate.DefaultConnectionPredicates;
import com.supermartijn642.fusion.api.predicate.FusionPredicateRegistry;
import com.supermartijn642.fusion.api.util.Pair;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelBlock;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.model.TRSRTransformation;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created 27/04/2023 by SuperMartijn642
 */
public class ConnectingModelType implements ModelType<ConnectingModelData> {

    public static final ResourceLocation DEFAULT_CONNECTION_KEY = new ResourceLocation("fusion", "default");

    @Override
    public Collection<ResourceLocation> getModelDependencies(ConnectingModelData data){
        return DefaultModelTypes.VANILLA.getModelDependencies(data.getVanillaModel());
    }

    @Override
    public Collection<SpriteIdentifier> getTextureDependencies(GatherTexturesContext context, ConnectingModelData data){
        return DefaultModelTypes.VANILLA.getTextureDependencies(context, data.getVanillaModel());
    }

    @Override
    @Nullable
    public ModelBlock getAsVanillaModel(ConnectingModelData data){
        return DefaultModelTypes.VANILLA.getAsVanillaModel(data.getVanillaModel());
    }

    @Override
    public IBakedModel bake(ModelBakingContext context, ConnectingModelData data){
        IBakedModel model = DefaultModelTypes.VANILLA.bake(context, data.getVanillaModel());
        ImmutableMap.Builder<ResourceLocation,ConnectionPredicate> predicates = ImmutableMap.builder();
        predicates.putAll(
            data.getAllConnectionPredicates().entrySet().stream()
                .map(entry -> Pair.of(entry.getKey().equals("default") ? DEFAULT_CONNECTION_KEY : new ResourceLocation(data.getVanillaModel().resolveTextureName(entry.getKey())), entry.getValue()))
                .filter(pair -> !pair.left().equals(TextureMap.LOCATION_MISSING_TEXTURE))
                .collect(Collectors.toMap(
                    Pair::left,
                    Pair::right,
                    DefaultConnectionPredicates::or
                ))
        );
        return new ConnectingBakedModel(model, context.getTransformation().apply(Optional.empty()).orElse(TRSRTransformation.identity()), predicates.build());
    }

    @Override
    public ConnectingModelData deserialize(JsonObject json) throws JsonParseException{
        // Deserialize the vanilla model
        ModelBlock model = DefaultModelTypes.VANILLA.deserialize(json);
        // Deserialize all the predicates from the 'connections' array
        Map<String,ConnectionPredicate> predicates = new HashMap<>();
        predicates.put("default", DefaultConnectionPredicates.isSameState());
        if(json.has("connections")){
            JsonElement connectionsElement = json.get("connections");
            if(connectionsElement.isJsonArray() || (connectionsElement.isJsonObject() && connectionsElement.getAsJsonObject().has("type"))) // Legacy array
                predicates.put("default", loadPredicate(connectionsElement, "connections"));
            else if(connectionsElement.isJsonObject()){ // Load predicates per texture
                JsonObject object = connectionsElement.getAsJsonObject();
                if(object.size() == 0)
                    throw new JsonParseException("Property 'connections' must have a 'type' key or keys per texture!");
                for(Map.Entry<String,JsonElement> texture : object.entrySet())
                    predicates.put(texture.getKey(), loadPredicate(texture.getValue(), texture.getKey()));
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
        return json.size() == 0 ? null : json;
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
            return DefaultConnectionPredicates.or(subPredicates.toArray(new ConnectionPredicate[0]));
        }
        if(element.isJsonObject())
            return FusionPredicateRegistry.deserializeConnectionPredicate(element.getAsJsonObject());
        throw new JsonParseException("Predicate '" + key + "' must be an object or an array!");
    }
}
