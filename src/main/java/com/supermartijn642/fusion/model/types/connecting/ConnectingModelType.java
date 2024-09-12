package com.supermartijn642.fusion.model.types.connecting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.DefaultModelTypes;
import com.supermartijn642.fusion.api.model.ModelBakingContext;
import com.supermartijn642.fusion.api.model.ModelType;
import com.supermartijn642.fusion.api.model.data.BaseModelData;
import com.supermartijn642.fusion.api.model.data.ConnectingModelData;
import com.supermartijn642.fusion.api.predicate.ConnectionPredicate;
import com.supermartijn642.fusion.api.predicate.DefaultConnectionPredicates;
import com.supermartijn642.fusion.api.predicate.FusionPredicateRegistry;
import com.supermartijn642.fusion.model.types.base.BaseModelDataImpl;
import com.supermartijn642.fusion.model.types.base.BaseModelElement;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Created 27/04/2023 by SuperMartijn642
 */
public class ConnectingModelType implements ModelType<ConnectingModelData> {

    public static final String DEFAULT_CONNECTION_KEY = "default";

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
    public List<ResourceLocation> getParentModels(ConnectingModelData data){
        return data.getParents();
    }

    @Override
    public BakedModel bake(ModelBakingContext context, ConnectingModelData data){
        // Check for circular dependencies
        ((ConnectingModelDataImpl)data).validateParents(context);
        // Bake the quads
        //noinspection unchecked,rawtypes
        List<ConnectingModelQuad> quads = (List)((ConnectingModelDataImpl)data).bakeQuads(context);
        // Gather remaining model properties
        boolean ambientOcclusion = ((ConnectingModelDataImpl)data).findProperty(context, model -> model.hasAmbientOcclusion, true);
        boolean gui3d = ((ConnectingModelDataImpl)data).findProperty(context, model -> model.guiLight, BlockModel.GuiLight.SIDE).lightLikeBlock();
        TextureAtlasSprite particleSprite = context.getTexture(((ConnectingModelDataImpl)data).findParticleSprite(context));
        ItemTransform transformThirdPersonLeftHand = ((ConnectingModelDataImpl)data).findProperty(context, model -> model.transforms.hasTransform(ItemDisplayContext.THIRD_PERSON_LEFT_HAND) ? model.transforms.getTransform(ItemDisplayContext.THIRD_PERSON_LEFT_HAND) : null, ItemTransform.NO_TRANSFORM);
        ItemTransform transformThirdPersonRightHand = ((ConnectingModelDataImpl)data).findProperty(context, model -> model.transforms.hasTransform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) ? model.transforms.getTransform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) : null, ItemTransform.NO_TRANSFORM);
        ItemTransform transformFirstPersonLeftHand = ((ConnectingModelDataImpl)data).findProperty(context, model -> model.transforms.hasTransform(ItemDisplayContext.FIRST_PERSON_LEFT_HAND) ? model.transforms.getTransform(ItemDisplayContext.FIRST_PERSON_LEFT_HAND) : null, ItemTransform.NO_TRANSFORM);
        ItemTransform transformFirstPersonRightHand = ((ConnectingModelDataImpl)data).findProperty(context, model -> model.transforms.hasTransform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) ? model.transforms.getTransform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) : null, ItemTransform.NO_TRANSFORM);
        ItemTransform transformHead = ((ConnectingModelDataImpl)data).findProperty(context, model -> model.transforms.hasTransform(ItemDisplayContext.HEAD) ? model.transforms.getTransform(ItemDisplayContext.HEAD) : null, ItemTransform.NO_TRANSFORM);
        ItemTransform transformGui = ((ConnectingModelDataImpl)data).findProperty(context, model -> model.transforms.hasTransform(ItemDisplayContext.GUI) ? model.transforms.getTransform(ItemDisplayContext.GUI) : null, ItemTransform.NO_TRANSFORM);
        ItemTransform transformGround = ((ConnectingModelDataImpl)data).findProperty(context, model -> model.transforms.hasTransform(ItemDisplayContext.GROUND) ? model.transforms.getTransform(ItemDisplayContext.GROUND) : null, ItemTransform.NO_TRANSFORM);
        ItemTransform transformFixed = ((ConnectingModelDataImpl)data).findProperty(context, model -> model.transforms.hasTransform(ItemDisplayContext.FIXED) ? model.transforms.getTransform(ItemDisplayContext.FIXED) : null, ItemTransform.NO_TRANSFORM);
        ItemTransforms itemTransforms = new ItemTransforms(transformThirdPersonLeftHand, transformThirdPersonRightHand, transformFirstPersonLeftHand, transformFirstPersonRightHand, transformHead, transformGui, transformGround, transformFixed);
        ItemOverrides itemOverrides = data.getVanillaModel().overrides.isEmpty() ? ItemOverrides.EMPTY : new ItemOverrides(context.getModelBaker(), data.getVanillaModel(), data.getVanillaModel().overrides);
        // Finally, create the model
        return new ConnectingBakedModel(
            quads,
            ambientOcclusion,
            gui3d,
            true,
            particleSprite,
            itemTransforms,
            itemOverrides
        );
    }

    @Override
    public ConnectingModelData deserialize(JsonObject json) throws JsonParseException{
        // Deserialize the base model
        BaseModelData base = DefaultModelTypes.BASE.deserialize(json);
        // Deserialize all the predicates from the 'connections' array
        Map<String,ConnectionPredicate> predicates = new HashMap<>();
        Map<String,String> connectionReferences = new HashMap<>();
        predicates.put("default", DefaultConnectionPredicates.isSameState());
        if(json.has("connections")){
            JsonElement connectionsElement = json.get("connections");
            if(connectionsElement.isJsonArray() || (connectionsElement.isJsonObject() && connectionsElement.getAsJsonObject().has("type"))) // Legacy array
                predicates.put("default", loadPredicate(connectionsElement, "connections"));
            else if(connectionsElement.isJsonObject()){ // Load predicates per texture
                JsonObject object = connectionsElement.getAsJsonObject();
                if(object.isEmpty())
                    throw new JsonParseException("Property 'connections' must have a 'type' key or keys per texture!");
                for(String texture : object.keySet()){
                    if(object.get(texture).isJsonPrimitive() && object.getAsJsonPrimitive(texture).isString())
                        connectionReferences.put(texture, object.get(texture).getAsString());
                    else
                        predicates.put(texture, loadPredicate(object.get(texture), texture));
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
                baseElement.from,
                baseElement.to,
                baseElement.faces,
                baseElement.rotation,
                baseElement.shade,
                baseElement.light_emission,
                connectionKeys
            ));
        }
        return new ConnectingModelDataImpl(base.getVanillaModel(), base.getParents(), elements, predicates, connectionReferences);
    }

    @Override
    public JsonObject serialize(ConnectingModelData value){
        // Serialize base model
        JsonObject json = DefaultModelTypes.BASE.serialize(value);
        // Create an array with all the serialized predicates
        Map<String,ConnectionPredicate> predicates = value.getAllConnectionPredicates();
        if(predicates.size() == 1 && predicates.containsKey("default"))
            json.add("connections", FusionPredicateRegistry.serializeConnectionPredicate(predicates.get("default")));
        else if(!predicates.isEmpty()){
            JsonObject connectionsJson = new JsonObject();
            predicates.forEach((texture, predicate) -> connectionsJson.add(texture, FusionPredicateRegistry.serializeConnectionPredicate(predicate)));
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
