package com.supermartijn642.fusion.model.types.connecting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.*;
import com.supermartijn642.fusion.api.predicate.ConnectionPredicate;
import com.supermartijn642.fusion.api.predicate.FusionPredicateRegistry;
import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.predicate.IsSameStateConnectionPredicate;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.model.TRSRTransformation;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Created 27/04/2023 by SuperMartijn642
 */
public class ConnectingModelType implements ModelType<Pair<ModelBlock,List<ConnectionPredicate>>> {

    @Override
    public Collection<ResourceLocation> getModelDependencies(Pair<ModelBlock,List<ConnectionPredicate>> data){
        return DefaultModelTypes.VANILLA.getModelDependencies(data.left());
    }

    @Override
    public Collection<SpriteIdentifier> getTextureDependencies(GatherTexturesContext context, Pair<ModelBlock,List<ConnectionPredicate>> data){
        return DefaultModelTypes.VANILLA.getTextureDependencies(context, data.left());
    }

    @Override
    @Nullable
    public ModelBlock getAsVanillaModel(Pair<ModelBlock,List<ConnectionPredicate>> data){
        return DefaultModelTypes.VANILLA.getAsVanillaModel(data.left());
    }

    @Override
    public IBakedModel bake(ModelBakingContext context, Pair<ModelBlock,List<ConnectionPredicate>> data){
        return new ConnectingBakedModel(DefaultModelTypes.VANILLA.bake(context, data.left()), context.getTransformation().apply(Optional.empty()).orElse(TRSRTransformation.identity()), data.right());
    }

    @Override
    public Pair<ModelBlock,List<ConnectionPredicate>> deserialize(JsonObject json) throws JsonParseException{
        // Deserialize the vanilla model
        ModelBlock model = DefaultModelTypes.VANILLA.deserialize(json);
        // Deserialize all the predicates from the 'connections' array
        List<ConnectionPredicate> predicates = new ArrayList<>();
        if(json.has("connections")){
            if(!json.get("connections").isJsonArray())
                throw new JsonParseException("Property 'connections' must be an array!");
            JsonArray array = json.getAsJsonArray("connections");
            for(JsonElement element : array){
                if(!element.isJsonObject())
                    throw new JsonParseException("Property 'connections' must only contain objects!");
                ConnectionPredicate predicate = FusionPredicateRegistry.deserializeConnectionPredicate(element.getAsJsonObject());
                predicates.add(predicate);
            }
        }else
            predicates.add(new IsSameStateConnectionPredicate());
        return Pair.of(model, predicates);
    }

    @Override
    public JsonObject serialize(Pair<ModelBlock,List<ConnectionPredicate>> value){
        JsonObject json = DefaultModelTypes.VANILLA.serialize(value.left());
        // Create an array with all the serialized predicates
        if(!value.right().isEmpty()){
            if(json == null)
                json = new JsonObject();
            JsonArray predicatesJson = new JsonArray();
            for(ConnectionPredicate predicate : value.right())
                predicatesJson.add(FusionPredicateRegistry.serializeConnectionPredicate(predicate));
            json.add("connections", predicatesJson);
        }
        return json;
    }
}
