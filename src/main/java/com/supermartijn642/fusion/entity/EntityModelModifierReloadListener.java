package com.supermartijn642.fusion.entity;

import com.google.gson.*;
import com.supermartijn642.fusion.api.util.Either;
import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.entity.model.predicates.DefaultEntityModelPredicates;
import com.supermartijn642.fusion.entity.model.predicates.EntityModelPredicate;
import com.supermartijn642.fusion.entity.model.predicates.EntityModelPredicateRegistryImpl;
import com.supermartijn642.fusion.util.IdentifierUtil;
import com.supermartijn642.fusion.util.LoggingHelper;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.world.entity.EntityType;

import java.util.*;
import java.util.function.Consumer;

/**
 * Created 24/09/2024 by SuperMartijn642
 */
public class EntityModelModifierReloadListener {

    private static final Gson GSON = new GsonBuilder().setLenient().create();
    private static final String LOCATION = "fusion/model_modifiers/entities";

    private static final Map<EntityType<?>,Modifier> MODIFIERS = new HashMap<>();

    public static Map<EntityType<?>,Modifier> getModifiers(){
        return MODIFIERS;
    }

    public static void getModelLocations(Consumer<ResourceLocation> output){
        for(Modifier modifier : MODIFIERS.values())
            modifier.gatherModelLocations(output);
    }

    public static void reload(ResourceManager resourceManager){
        MODIFIERS.clear();

        // Find all overlay files
        Map<ResourceLocation,JsonElement> resources = new HashMap<>();
        SimpleJsonResourceReloadListener.scanDirectory(resourceManager, LOCATION, GSON, resources);

        // Parse all the model overlay files
        for(Map.Entry<ResourceLocation,JsonElement> entry : resources.entrySet()){
            ResourceLocation location = entry.getKey();
            if(!entry.getValue().isJsonObject())
                throw new IllegalArgumentException("Entity model modifier '" + location + "' must contain a json object!");
            JsonObject json = entry.getValue().getAsJsonObject();
            try{
                parseResource(json);
            }catch(JsonParseException e){
                LoggingHelper.logUserError(e, "Failed to parse entity model modifier '%s':", location);
            }
        }
    }

    private static void parseResource(JsonObject json){
        // Get the targets
        if(!json.has("targets") || !json.get("targets").isJsonArray())
            throw new JsonParseException("Entity model modifier must have array property 'targets'!");
        JsonArray targetsJson = json.getAsJsonArray("targets");
        Set<EntityType<?>> targets = new HashSet<>();
        for(JsonElement element : targetsJson){
            if(!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString())
                throw new JsonParseException("Property 'targets' array must only contain strings!");
            if(!IdentifierUtil.isValidIdentifier(element.getAsString()))
                throw new JsonParseException("Target must be a valid identifier, not '" + element.getAsString() + "'!!");
            ResourceLocation identifier = new ResourceLocation(element.getAsString());
            EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(identifier);
            //noinspection ConstantValue
            if(entityType == null)
                throw new JsonParseException("Could not find an entity type for '" + identifier + "'!");
            targets.add(entityType);
        }

        // Parse all the layers
        if(!json.has("layers") || !json.get("layers").isJsonObject())
            throw new JsonParseException("Entity model modifier must contain layer objects!");
        JsonObject layersJson = json.get("layers").getAsJsonObject();
        Map<String,Layer> layers = new HashMap<>();
        for(String name : layersJson.keySet()){
            if(!IdentifierUtil.isValidIdentifier(name))
                throw new JsonParseException("Layer name must be a valid identifier, not '" + name + "'!");
            if(!layersJson.get(name).isJsonObject())
                throw new JsonParseException("Property 'layers' object must only contain objects, layer '" + name + "' is not an object!");
            Layer layer = parseLayer(layersJson.getAsJsonObject(name), name);
            layers.put(name, layer);
        }

        for(EntityType<?> target : targets){
            Modifier modifier = MODIFIERS.computeIfAbsent(target, Modifier::new);
            ResourceLocation entityIdentifier = BuiltInRegistries.ENTITY_TYPE.getKey(target);
            for(Map.Entry<String,Layer> entry : layers.entrySet()){
                ModelLayerLocation location = new ModelLayerLocation(entityIdentifier, entry.getKey());
                modifier.layers.put(location, entry.getValue());
            }
        }
    }

    private static Layer parseLayer(JsonObject json, String layerName){
        // Parse model properties
        ModelOption defaultProperties = parseModelOption(json, layerName);
        // Parse conditional models
        List<Pair<EntityModelPredicate,ModelOption>> conditionals = new ArrayList<>();
        if(json.has("conditionals")){
            if(!json.get("conditionals").isJsonArray())
                throw new JsonParseException("Property 'conditionals' for layer '" + layerName + "' must be an array!");
            for(JsonElement element : json.getAsJsonArray("conditionals")){
                if(!element.isJsonObject())
                    throw new JsonParseException("Array property 'conditionals' for layer '" + layerName + "' must only contain objects!");
                conditionals.add(parseConditional(element.getAsJsonObject()));
            }
        }
        return new Layer(defaultProperties, conditionals);
    }

    private static Pair<EntityModelPredicate,ModelOption> parseConditional(JsonObject json){
        if(!json.has("model") && !json.has("texture"))
            throw new JsonParseException("Conditional model entry must have at least one of 'model' or 'texture'!");
        // Parse model properties
        ModelOption options = parseModelOption(json, "model");
        // Parse conditions
        if(!json.has("conditions") || !json.get("conditions").isJsonArray())
            throw new JsonParseException("Conditional model entry must array property 'conditions'!");
        JsonArray conditionsJson = json.getAsJsonArray("conditions");
        if(conditionsJson.isEmpty())
            throw new JsonParseException("Property 'conditions' must not be empty!");
        List<EntityModelPredicate> conditions = new ArrayList<>();
        for(JsonElement element : conditionsJson){
            if(!element.isJsonObject())
                throw new JsonParseException("Array property 'conditions' must only contain objects!");
            EntityModelPredicate predicate = EntityModelPredicateRegistryImpl.deserializePredicate(element.getAsJsonObject());
            conditions.add(predicate);
        }
        EntityModelPredicate combined = conditions.size() == 1 ? conditions.get(0) : DefaultEntityModelPredicates.and(conditions.toArray(new EntityModelPredicate[0]));
        combined = combined.simplify();
        return Pair.of(combined, options);
    }

    private static List<ModelOption> parseModelOptions(JsonElement json, String propertyName){
        if((json.isJsonPrimitive() && json.getAsJsonPrimitive().isString()) || json.isJsonObject())
            return List.of(parseModelOption(json, propertyName));
        if(!json.isJsonArray())
            throw new JsonParseException("Property '" + propertyName + "' must be a string, object, or array!");
        List<ModelOption> options = new ArrayList<>(json.getAsJsonArray().size());
        for(JsonElement element : json.getAsJsonArray())
            options.add(parseModelOption(element, propertyName));
        return options;
    }

    private static ModelOption parseModelOption(JsonElement element, String propertyName){
        if(element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()){
            if(!IdentifierUtil.isValidIdentifier(element.getAsString()))
                throw new JsonParseException("Property '" + propertyName + "' must be a valid identifier, not '" + element.getAsString() + "'!");
            return new ModelOption(Either.left(new ResourceLocation(element.getAsString())), null, null, null, null, null, null, null, null, 1);
        }
        if(element.isJsonObject()){
            JsonObject json = element.getAsJsonObject();
            // Model
            Either<ResourceLocation,List<ModelOption>> model = null;
            if(json.has("model"))
                model = Either.right(parseModelOptions(json.get("model"), propertyName));
            // Texture
            List<ResourceLocation> textures;
            if(json.has("texture"))
                textures = parseTextures(json.get("texture"), propertyName);
            else
                textures = null;
            // Flipping
            Boolean flipX = null;
            if(json.has("flip_x")){
                if(!json.get("flip_x").isJsonPrimitive() || !json.getAsJsonPrimitive("flip_x").isBoolean())
                    throw new JsonParseException("Property 'flip_x' must be a boolean!");
                flipX = json.get("flip_x").getAsBoolean();
            }
            Boolean flipY = null;
            if(json.has("flip_y")){
                if(!json.get("flip_y").isJsonPrimitive() || !json.getAsJsonPrimitive("flip_y").isBoolean())
                    throw new JsonParseException("Property 'flip_y' must be a boolean!");
                flipY = json.get("flip_y").getAsBoolean();
            }
            Boolean flipZ = null;
            if(json.has("flip_z")){
                if(!json.get("flip_z").isJsonPrimitive() || !json.getAsJsonPrimitive("flip_z").isBoolean())
                    throw new JsonParseException("Property 'flip_z' must be a boolean!");
                flipZ = json.get("flip_z").getAsBoolean();
            }
            // Offset
            Float offsetX = null;
            if(json.has("offset_x")){
                if(!json.get("offset_x").isJsonPrimitive() || !json.getAsJsonPrimitive("offset_x").isNumber())
                    throw new JsonParseException("Property 'offset_x' must be a number!");
                offsetX = json.get("offset_x").getAsFloat();
            }
            Float offsetY = null;
            if(json.has("offset_y")){
                if(!json.get("offset_y").isJsonPrimitive() || !json.getAsJsonPrimitive("offset_y").isNumber())
                    throw new JsonParseException("Property 'offset_y' must be a number!");
                offsetY = json.get("offset_y").getAsFloat();
            }
            Float offsetZ = null;
            if(json.has("offset_z")){
                if(!json.get("offset_z").isJsonPrimitive() || !json.getAsJsonPrimitive("offset_z").isNumber())
                    throw new JsonParseException("Property 'offset_z' must be a number!");
                offsetZ = json.get("offset_z").getAsFloat();
            }
            // Scaling
            Float scale = null;
            if(json.has("scale")){
                if(!json.get("scale").isJsonPrimitive() || !json.getAsJsonPrimitive("scale").isNumber())
                    throw new JsonParseException("Property 'scale' must be a number!");
                scale = json.get("scale").getAsFloat();
                if(scale <= 0)
                    throw new JsonParseException("Property 'scale' must be greater than zero!");
            }
            // Weight
            float weight = 1;
            if(json.has("weight")){
                if(!json.get("weight").isJsonPrimitive() || !json.getAsJsonPrimitive("weight").isNumber())
                    throw new JsonParseException("Property 'weight' must be a number!");
                weight = json.get("weight").getAsFloat();
                if(weight <= 0)
                    throw new JsonParseException("Property 'weight' must be greater than zero!");
            }
            return new ModelOption(model, textures, flipX, flipY, flipZ, offsetX, offsetY, offsetZ, scale, weight);
        }
        throw new JsonParseException("Property '" + propertyName + "' must be a string or an object!");
    }

    private static List<ResourceLocation> parseTextures(JsonElement json, String propertyName){
        // Single texture string
        if(json.isJsonPrimitive() && json.getAsJsonPrimitive().isString()){
            if(!IdentifierUtil.isValidIdentifier(json.getAsString()))
                throw new JsonParseException("Texture must be a valid location, not '" + json.getAsString() + "'!");
            return List.of(new ResourceLocation("textures/" + json.getAsString() + ".png"));
        }
        // Array of strings
        if(json.isJsonArray()){
            JsonArray array = json.getAsJsonArray();
            List<ResourceLocation> textures = new ArrayList<>(array.size());
            for(JsonElement element : array){
                if(!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString())
                    throw new JsonParseException("Array property '" + propertyName + "' must only contain strings!");
                if(!IdentifierUtil.isValidIdentifier(element.getAsString()))
                    throw new JsonParseException("Texture must be a valid identifier, not '" + element.getAsString() + "'!!");
                textures.add(new ResourceLocation("textures/" + element.getAsString() + ".png"));
            }
            return textures;
        }
        throw new JsonParseException("Property '" + propertyName + "' must be either a string or an array of strings!");
    }

    public static class Modifier {

        public final EntityType<?> entityType;
        public final Map<ModelLayerLocation,Layer> layers = new LinkedHashMap<>(); // This should maintain order

        private Modifier(EntityType<?> entityType){
            this.entityType = entityType;
        }

        void gatherModelLocations(Consumer<ResourceLocation> output){
            this.layers.values().forEach(layer -> layer.gatherModelLocations(output));
        }
    }

    public static class Layer {

        public final ModelOption defaultModel;
        public final List<Pair<EntityModelPredicate,ModelOption>> conditionals;

        private Layer(ModelOption defaultModel, List<Pair<EntityModelPredicate,ModelOption>> conditionals){
            this.defaultModel = defaultModel;
            this.conditionals = conditionals;
        }

        void gatherModelLocations(Consumer<ResourceLocation> output){
            this.defaultModel.gatherModelLocations(output);
            for(Pair<EntityModelPredicate,ModelOption> conditional : this.conditionals)
                conditional.right().gatherModelLocations(output);
        }
    }

    public static class ModelOption {
        public final Either<ResourceLocation,List<ModelOption>> model;
        public final List<ResourceLocation> textures;
        public final Boolean flipX, flipY, flipZ;
        public final Float offsetX, offsetY, offsetZ;
        public final Float scale;
        public final double weight;

        public ModelOption(Either<ResourceLocation,List<ModelOption>> model, List<ResourceLocation> textures, Boolean flipX, Boolean flipY, Boolean flipZ, Float offsetX, Float offsetY, Float offsetZ, Float scale, float weight){
            this.model = model;
            this.textures = textures;
            this.flipX = flipX;
            this.flipY = flipY;
            this.flipZ = flipZ;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.offsetZ = offsetZ;
            this.scale = scale;
            this.weight = weight;
        }

        void gatherModelLocations(Consumer<ResourceLocation> output){
            if(this.model != null){
                if(this.model.isLeft())
                    output.accept(this.model.left());
                else
                    this.model.right().forEach(o -> o.gatherModelLocations(output));
            }
        }
    }
}
