package com.supermartijn642.fusion.model.modifiers.block;

import com.google.common.collect.ImmutableSet;
import com.google.gson.*;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.api.model.predicates.blockstate.BlockStateModelPredicate;
import com.supermartijn642.fusion.api.model.predicates.blockstate.DefaultBlockStateModelPredicates;
import com.supermartijn642.fusion.api.model.predicates.blockstate.FusionBlockStateModelPredicateRegistry;
import com.supermartijn642.fusion.util.IdentifierUtil;
import com.supermartijn642.fusion.util.LoggingHelper;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.block.model.SingleVariant;
import net.minecraft.client.renderer.block.model.Variant;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Created 19/09/2024 by SuperMartijn642
 */
public class BlockModelModifierReloadListener {

    private static final Gson GSON = new GsonBuilder().setLenient().create();
    private static final String LOCATION = "fusion/model_modifiers/blocks";
    private static final FileToIdConverter ID_CONVERTER = FileToIdConverter.json(LOCATION);
    private static final int DEFAULT_PRIORITY = 100;

    public static final BlockModelModifierReloadListener INSTANCE = new BlockModelModifierReloadListener();

    private final Map<BlockState,List<Properties>> modifiers = new HashMap<>();

    private BlockModelModifierReloadListener(){
    }

    public void registerModelDependencies(ResolvableModel.Resolver resolver){
        // Collect all model identifiers
        Set<Identifier> models = new HashSet<>();
        for(List<Properties> modifiers : this.modifiers.values()){ // Note: Even models that will technically never be used due to earlier conditions still need to be resolved for user warnings
            for(Properties modifier : modifiers){
                for(ModelEntry entry : modifier.defaultModelOverrides)
                    models.add(entry.model);
                for(List<ModelEntry> conditionals : modifier.appendModels){
                    for(ModelEntry entry : conditionals){
                        models.add(entry.model);
                    }
                }
            }
        }
        // Mark the models
        models.forEach(resolver::markDependency);
    }

    public void applyModelModifiers(ModelBakery.BakingResult results, ModelBakery.ModelBakerImpl resolver){
        Map<BlockState,BlockStateModel> bakedModels = results.blockStateModels();

        // Create model resolver
        Function<Identifier,BlockStateModel> modelResolver = new Function<>() {
            final Map<Identifier,BlockStateModel> models = new HashMap<>();

            @Override
            public BlockStateModel apply(Identifier identifier){
                BlockStateModel model = this.models.get(identifier);
                if(model != null)
                    return model;
                try{
                    model = new SingleVariant.Unbaked(new Variant(identifier, Variant.SimpleModelState.DEFAULT)).bake(resolver);
                }catch(Exception e){
                    FusionClient.LOGGER.error("Caught exception baking model '{}':", identifier, e);
                    model = results.missingModels().block();
                }
                this.models.put(identifier, model);
                return model;
            }
        };

        // Create a modifier model for each target
        for(Map.Entry<BlockState,List<Properties>> entry : this.modifiers.entrySet()){
            BlockState target = entry.getKey();
            BlockStateModel targetModel = bakedModels.get(target);
            if(targetModel == null) continue;

            // Sort modifier properties by their priority
            List<Properties> modifiers = entry.getValue();
            modifiers.sort(Comparator.<Properties>comparingInt(p -> p.priority).thenComparing(p -> p.location));

            // Resolve default model overwrites
            List<BlockModelModifierBakedModel.ConditionalModel> defaultModelOverrides = createConditionModels(
                modifiers.stream().flatMap(p -> p.defaultModelOverrides.stream()).toList(),
                modelResolver,
                false
            );

            // Resolve append models
            List<List<BlockModelModifierBakedModel.ConditionalModel>> appendModels = new ArrayList<>();
            for(Properties modifier : modifiers){
                for(List<ModelEntry> conditionals : modifier.appendModels){
                    List<BlockModelModifierBakedModel.ConditionalModel> resolvedConditionals = createConditionModels(
                        conditionals,
                        modelResolver,
                        modifier.showBreakingOverlay != Boolean.FALSE
                    );
                    if(!resolvedConditionals.isEmpty())
                        appendModels.add(resolvedConditionals);
                }
            }
            appendModels = List.copyOf(appendModels);

            // Apply pane culling fix
            if(modifiers.stream().anyMatch(Properties::paneCullingFix))
                targetModel = new PaneCullingBakedModel(targetModel);

            // Create the modifier model
            BlockModelModifierBakedModel modifierModel = new BlockModelModifierBakedModel(
                targetModel,
                defaultModelOverrides,
                appendModels
            );
            bakedModels.put(target, modifierModel);
        }

        // Clear modifier data
        this.modifiers.clear();
    }

    private static List<BlockModelModifierBakedModel.ConditionalModel> createConditionModels(List<ModelEntry> modelEntries, Function<Identifier,BlockStateModel> modelResolver, boolean showBreakingOverlay){
        List<BlockModelModifierBakedModel.ConditionalModel> conditionals = new ArrayList<>();
        for(ModelEntry entry : modelEntries){
            if(entry.conditions != null && entry.conditions.alwaysFalse()) // Skip models for which the condition is always false
                continue;
            conditionals.add(new BlockModelModifierBakedModel.ConditionalModel(
                modelResolver.apply(entry.model),
                entry.conditions,
                entry.showBreakingOverlay == null ? showBreakingOverlay : entry.showBreakingOverlay
            ));
            if(entry.conditions == null || entry.conditions.alwaysTrue()) // If the condition is always true, any later entries will never be reached
                break;
        }
        return List.copyOf(conditionals);
    }

    public void reload(ResourceManager resourceManager){
        this.modifiers.clear();

        // Find all block model modifier files
        Map<Identifier,JsonElement> resources = new HashMap<>();
        SimpleJsonResourceReloadListener.scanDirectory(resourceManager, ID_CONVERTER, JsonOps.INSTANCE, new Codec<>() {
            @Override
            public <T> DataResult<Pair<JsonElement,T>> decode(DynamicOps<T> ops, T input){
                return DataResult.success(com.mojang.datafixers.util.Pair.of(ops.convertTo(JsonOps.INSTANCE, input), input));
            }

            @Override
            public <T> DataResult<T> encode(JsonElement input, DynamicOps<T> ops, T prefix){
                return DataResult.success(JsonOps.INSTANCE.convertTo(ops, input));
            }
        }, resources);

        // Parse all the block model modifier files
        for(Map.Entry<Identifier,JsonElement> entry : resources.entrySet()){
            Identifier location = entry.getKey();
            if(!entry.getValue().isJsonObject()){
                FusionClient.LOGGER.error("Block model modifier '{}' must contain a json object!", location);
                continue;
            }
            JsonObject json = entry.getValue().getAsJsonObject();
            try{
                this.parseResource(json, location);
            }catch(JsonParseException e){
                LoggingHelper.logUserError(e, "Failed to parse block model modifier '%s':", location);
            }
        }
    }

    private void parseResource(JsonObject json, Identifier location){
        // Get the targets
        if(!json.has("targets") || !json.get("targets").isJsonArray())
            throw new JsonParseException("Model modifier must have array property 'targets'!");
        JsonArray targetsJson = json.getAsJsonArray("targets");
        Set<BlockState> targets = new HashSet<>();
        try{
            for(JsonElement element : targetsJson){
                if(element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()){ // Handle simple strings
                    if(!IdentifierUtil.isValidIdentifier(element.getAsString()))
                        throw new JsonParseException("Target must be a valid identifier, not '" + element.getAsString() + "'!");
                    Identifier identifier = Identifier.parse(element.getAsString());
                    Optional<Block> block = BuiltInRegistries.BLOCK.getOptional(identifier);
                    if(block.isEmpty())
                        throw new JsonParseException("Could not find a block for identifier '" + identifier + "'!");
                    targets.addAll(block.get().getStateDefinition().getPossibleStates());
                }else if(element.isJsonObject()) // Handle blocks with specific state properties
                    parseTarget(element.getAsJsonObject()).forEach(targets::add);
                else
                    throw new JsonParseException("Array property 'targets' must only contain objects and strings!");
            }
        }catch(JsonParseException e){
            throw new JsonParseException("Failed to parse a 'targets' entry", e);
        }
        if(targets.isEmpty())
            return;

        // Priority
        int priority = DEFAULT_PRIORITY;
        if(json.has("priority")){
            if(!json.get("priority").isJsonPrimitive() || !json.getAsJsonPrimitive("priority").isNumber())
                throw new JsonParseException("Property 'priority' must be a number!");
            priority = json.getAsJsonPrimitive("priority").getAsInt();
        }

        // Get default model overrides
        List<ModelEntry> defaultModel = List.of();
        if(json.has("default_model_overrides")){
            try{
                defaultModel = parseModelEntry(json.get("default_model_overrides"));
            }catch(JsonParseException e){
                throw new JsonParseException("Failed to parse 'default_model_overrides'", e);
            }
        }

        // Get append models
        List<List<ModelEntry>> appendModels = new ArrayList<>();
        if(json.has("append_models")){
            if(!json.get("append_models").isJsonArray())
                throw new JsonParseException("Property 'append_models' must be an array!");
            try{
                for(JsonElement e : json.getAsJsonArray("append_models"))
                    appendModels.add(parseModelEntry(e));
            }catch(JsonParseException e){
                throw new JsonParseException("Failed to parse 'append_models' entry", e);
            }
        }

        // Read legacy 'append' entries
        if(json.has("append")){
            Set<Identifier> models = new LinkedHashSet<>(); // This should maintain order
            if(!json.get("append").isJsonArray())
                throw new JsonParseException("Property 'append' must be an array!");
            JsonArray appendJson = json.getAsJsonArray("append");
            for(JsonElement element : appendJson){
                if(!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString())
                    throw new JsonParseException("Array property 'append' must only contain strings!");
                if(!IdentifierUtil.isValidIdentifier(element.getAsString()))
                    throw new JsonParseException("Model must be a valid identifier, not '" + element.getAsString() + "'!!");
                models.add(Identifier.parse(element.getAsString()));
            }
            for(Identifier model : models)
                appendModels.add(List.of(ModelEntry.simple(model)));
        }

        // Show breaking overlay
        Boolean showBreakingOverlay = null;
        if(json.has("show_breaking_overlay")){
            if(!json.get("show_breaking_overlay").isJsonPrimitive() || !json.getAsJsonPrimitive("show_breaking_overlay").isBoolean())
                throw new JsonParseException("Property 'show_breaking_overlay' must be a boolean!");
            showBreakingOverlay = json.get("show_breaking_overlay").getAsBoolean();
        }

        // Pane culling option
        boolean paneCullingFix = false;
        if(json.has("pane_culling_fix")){
            if(!json.get("pane_culling_fix").isJsonPrimitive() || !json.getAsJsonPrimitive("pane_culling_fix").isBoolean())
                throw new JsonParseException("Property 'pane_culling_fix' must be a boolean!");
            paneCullingFix = json.get("pane_culling_fix").getAsBoolean();
        }

        // Put the properties into the map
        Properties properties = new Properties(priority, location, defaultModel, appendModels, paneCullingFix, showBreakingOverlay);
        for(BlockState target : targets)
            this.modifiers.computeIfAbsent(target, t -> new ArrayList<>(8)).add(properties);
    }

    private static Stream<BlockState> parseTarget(JsonObject json){
        // Block
        if(!json.has("block") || !json.get("block").isJsonPrimitive() || !json.getAsJsonPrimitive("block").isString())
            throw new JsonParseException("Entry must have string property 'block'!");
        if(!IdentifierUtil.isValidIdentifier(json.get("block").getAsString()))
            throw new JsonParseException("Property 'block' must be a valid identifier, not '" + json.get("block").getAsString() + "'!!");
        Identifier identifier = Identifier.parse(json.get("block").getAsString());
        Optional<Block> optional = BuiltInRegistries.BLOCK.getOptional(identifier);
        if(optional.isEmpty())
            throw new JsonParseException("Could not find a block for identifier '" + identifier + "'!");
        Block block = optional.get();

        // Properties
        Map<Property<?>,Set<?>> properties = new HashMap<>();
        if(!json.has("properties") || !json.get("properties").isJsonObject())
            throw new JsonParseException("Entry must have object property 'properties'!");
        if(json.getAsJsonObject("properties").isEmpty())
            throw new JsonParseException("At least one property must be specified!");
        for(Map.Entry<String,JsonElement> entry : json.getAsJsonObject("properties").entrySet()){
            try{
                // Parse the property
                Property<?> property = block.getStateDefinition().getProperty(entry.getKey());
                if(property == null)
                    throw new JsonParseException("Block '" + identifier + "' does not have a property named '" + entry.getKey() + "'!");
                // Parse the values
                ImmutableSet.Builder<Object> builder = ImmutableSet.builder();
                if(entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isString()){
                    Optional<?> value = property.getValue(entry.getValue().getAsString());
                    if(value.isEmpty())
                        throw new JsonParseException("Unknown value '" + entry.getValue().getAsString() + "' for property '" + property.getName() + "' in block '" + identifier + "'!");
                    builder.add(value.get());
                }else if(entry.getValue().isJsonArray()){
                    if(entry.getValue().getAsJsonArray().isEmpty())
                        throw new JsonParseException("Valid values cannot be empty!");
                    for(JsonElement element : entry.getValue().getAsJsonArray()){
                        if(!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString())
                            throw new JsonParseException("Values array must only contain strings!");
                        Optional<?> value = property.getValue(element.getAsString());
                        if(value.isEmpty())
                            throw new JsonParseException("Unknown value '" + element.getAsString() + "' for property '" + property.getName() + "' in block '" + identifier + "'!");
                        builder.add(value.get());
                    }
                }else
                    throw new JsonParseException("Value must be a string or an array of strings!");
                properties.put(property, builder.build());
            }catch(JsonParseException e){
                throw new JsonParseException("Failed to parse properties entry '" + entry.getKey() + "'", e);
            }
        }

        // Find all matching states
        Stream<BlockState> states = Stream.of(block.getStateDefinition().any());
        for(Property<?> property : block.getStateDefinition().getProperties()){
            if(properties.containsKey(property)){
                Set<?> values = properties.get(property);
                states = states.flatMap(state -> values.stream().map(value -> stateWithValue(state, property, value)));
            }else
                states = states.flatMap(state -> property.getAllValues().map(value -> stateWithValue(state, property, value.value())));
        }
        return states;
    }

    private static <T extends Comparable<T>> BlockState stateWithValue(BlockState state, Property<?> property, Object value){
        //noinspection unchecked
        return state.setValue((Property<T>)property, (T)value);
    }

    private static List<ModelEntry> parseModelEntry(JsonElement element){
        if(!element.isJsonArray() || !element.isJsonObject() && (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()))
            throw new JsonParseException("Must be an array, object, or string!");

        // Handle arrays
        if(element.isJsonArray()){
            List<ModelEntry> entries = new ArrayList<>(element.getAsJsonArray().size());
            try{
                for(JsonElement e : element.getAsJsonArray())
                    entries.addAll(parseModelEntry(e));
            }catch(JsonParseException e){
                throw new JsonParseException("Failed to parse entry", e);
            }
            return entries;
        }

        // Handle simple strings
        if(element.isJsonPrimitive()){
            String identifier = element.getAsString();
            if(!IdentifierUtil.isValidIdentifier(identifier))
                throw new JsonParseException("String must be a valid identifier, not '" + identifier + "'!");
            return List.of(ModelEntry.simple(Identifier.parse(identifier)));
        }

        JsonObject object = element.getAsJsonObject();

        // Model identifier
        if(!object.has("model") || !object.get("model").isJsonPrimitive() || !object.getAsJsonPrimitive("model").isString())
            throw new JsonParseException("Object must have string property 'model'!");
        if(!IdentifierUtil.isValidIdentifier(object.get("model").getAsString()))
            throw new JsonParseException("Property 'model' must be a valid identifier, not '" + object.get("model").getAsString() + "'!");
        Identifier model = Identifier.parse(object.get("model").getAsString());

        // Conditions
        BlockStateModelPredicate conditions = null;
        if(object.has("conditions")){
            try{
                if(object.get("conditions").isJsonObject())
                    conditions = FusionBlockStateModelPredicateRegistry.deserializeBlockStateModelPredicate(object.get("conditions").getAsJsonObject());
                else if(object.get("conditions").isJsonArray()){
                    List<BlockStateModelPredicate> predicates = new ArrayList<>();
                    for(JsonElement entry : object.get("conditions").getAsJsonArray()){
                        if(!entry.isJsonObject())
                            throw new JsonParseException("Array entry must be an object!");
                        predicates.add(FusionBlockStateModelPredicateRegistry.deserializeBlockStateModelPredicate(entry.getAsJsonObject()));
                    }
                    conditions = DefaultBlockStateModelPredicates.and(predicates.toArray(new BlockStateModelPredicate[0]));
                }else
                    throw new JsonParseException("Value must an object or array of objects!");
            }catch(JsonParseException e){
                throw new JsonParseException("Failed to parse conditions for model '" + model + "'", e);
            }
            conditions = conditions.simplify();
        }

        // Breaking overlay
        Boolean showBreakingOverlay = null;
        if(object.has("show_breaking_overlay")){
            if(!object.get("show_breaking_overlay").isJsonPrimitive() || !object.getAsJsonPrimitive("show_breaking_overlay").isBoolean())
                throw new JsonParseException("Property 'show_breaking_overlay' must be a boolean!");
            showBreakingOverlay = object.get("show_breaking_overlay").getAsBoolean();
        }

        return List.of(new ModelEntry(model, conditions, showBreakingOverlay));
    }

    private record Properties(int priority, Identifier location, List<ModelEntry> defaultModelOverrides, List<List<ModelEntry>> appendModels, boolean paneCullingFix, Boolean showBreakingOverlay) {
    }

    private record ModelEntry(Identifier model, @Nullable BlockStateModelPredicate conditions, Boolean showBreakingOverlay) {
        static ModelEntry simple(Identifier model){
            return new ModelEntry(model, null, null);
        }
    }
}
