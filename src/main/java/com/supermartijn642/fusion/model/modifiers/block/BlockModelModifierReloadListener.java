package com.supermartijn642.fusion.model.modifiers.block;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.*;
import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.api.model.predicates.blockstate.BlockStateModelPredicate;
import com.supermartijn642.fusion.api.model.predicates.blockstate.DefaultBlockStateModelPredicates;
import com.supermartijn642.fusion.api.model.predicates.blockstate.FusionBlockStateModelPredicateRegistry;
import com.supermartijn642.fusion.extensions.ResourcePackExtension;
import com.supermartijn642.fusion.util.IdentifierUtil;
import com.supermartijn642.fusion.util.LoggingHelper;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.BlockStateMapper;
import net.minecraft.client.resources.*;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.RegistrySimple;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created 19/09/2024 by SuperMartijn642
 */
public class BlockModelModifierReloadListener {

    private static final Gson GSON = new GsonBuilder().setLenient().create();
    private static final String LOCATION = "fusion/model_modifiers/blocks";
    private static final int DEFAULT_PRIORITY = 100;

    public static final BlockModelModifierReloadListener INSTANCE = new BlockModelModifierReloadListener();

    private final Map<ModelResourceLocation,List<Properties>> modifiers = new HashMap<>();

    private BlockModelModifierReloadListener(){
    }

    public Collection<ModelResourceLocation> gatherModelDependencies(){
        // Collect all model identifiers
        Set<ResourceLocation> models = new HashSet<>();
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
        return models.stream()
            .map(BlockModelModifierReloadListener::variantModelLocation)
            .collect(Collectors.toList());
    }

    public void applyModelModifiers(ModelBakery modelBakery){
        RegistrySimple<ModelResourceLocation,IBakedModel> bakedModels = modelBakery.bakedRegistry;

        // Create a modifier model for each target
        for(Map.Entry<ModelResourceLocation,List<Properties>> entry : this.modifiers.entrySet()){
            ModelResourceLocation target = entry.getKey();
            IBakedModel targetModel = bakedModels.getObject(target);
            if(targetModel == null) continue;

            // Sort modifier properties by their priority
            List<Properties> modifiers = entry.getValue();
            modifiers.sort(Comparator.<Properties>comparingInt(p -> p.priority).thenComparing(p -> p.location));

            // Resolve default model overwrites
            List<BlockModelModifierBakedModel.ConditionalModel> defaultModelOverrides = createConditionModels(
                modifiers.stream().flatMap(p -> p.defaultModelOverrides.stream()).collect(Collectors.toList()),
                i -> bakedModels.getObject(variantModelLocation(i)),
                false
            );

            // Resolve append models
            List<List<BlockModelModifierBakedModel.ConditionalModel>> appendModels = new ArrayList<>();
            for(Properties modifier : modifiers){
                for(List<ModelEntry> conditionals : modifier.appendModels){
                    List<BlockModelModifierBakedModel.ConditionalModel> resolvedConditionals = createConditionModels(
                        conditionals,
                        i -> bakedModels.getObject(variantModelLocation(i)),
                        modifier.showBreakingOverlay != Boolean.FALSE
                    );
                    if(!resolvedConditionals.isEmpty())
                        appendModels.add(resolvedConditionals);
                }
            }
            appendModels = ImmutableList.copyOf(appendModels);

            // Apply pane culling fix
            if(modifiers.stream().anyMatch(p -> p.paneCullingFix))
                targetModel = new PaneCullingBakedModel(targetModel);

            // Create the modifier model
            BlockModelModifierBakedModel modifierModel = new BlockModelModifierBakedModel(
                targetModel,
                defaultModelOverrides,
                appendModels
            );
            bakedModels.putObject(target, modifierModel);
        }

        // Clear modifier data
        this.modifiers.clear();
    }

    private static List<BlockModelModifierBakedModel.ConditionalModel> createConditionModels(List<ModelEntry> modelEntries, Function<ResourceLocation,IBakedModel> modelResolver, boolean showBreakingOverlay){
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
        return ImmutableList.copyOf(conditionals);
    }

    private static ModelResourceLocation variantModelLocation(ResourceLocation modelLocation){
        return new ModelResourceLocation(modelLocation, "fusion_block_model_modifier");
    }

    public void reload(IResourceManager resourceManager){
        this.modifiers.clear();

        // Find all block model modifier files
        Map<ResourceLocation,JsonElement> resources = new HashMap<>();
        for(ResourceLocation fullLocation : listResources(resourceManager, LOCATION, s -> s.endsWith(".json"))){
            ResourceLocation location = new ResourceLocation(fullLocation.getResourceDomain(), fullLocation.getResourcePath().substring(LOCATION.length() + 1, fullLocation.getResourcePath().length() - ".json".length()));
            try(IResource resource = resourceManager.getResource(fullLocation)){
                Reader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()));
                JsonElement element = JsonUtils.fromJson(GSON, reader, JsonElement.class);
                resources.put(location, element);
            }catch(JsonParseException e){
                FusionClient.LOGGER.error("Failed to parse json from block model overlay '{}'!", fullLocation, e);
            }catch(Exception e){
                FusionClient.LOGGER.error("Encountered an exception whilst reading block model overlay at '{}'!", fullLocation, e);
            }
        }

        // Parse all the block model modifier files
        for(Map.Entry<ResourceLocation,JsonElement> entry : resources.entrySet()){
            ResourceLocation location = entry.getKey();
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

    private void parseResource(JsonObject json, ResourceLocation location){
        // Get the targets
        if(!json.has("targets") || !json.get("targets").isJsonArray())
            throw new JsonParseException("Model modifier must have array property 'targets'!");
        JsonArray targetsJson = json.getAsJsonArray("targets");
        Set<ModelResourceLocation> targets = new HashSet<>();
        BlockStateMapper blockStateMapper = Minecraft.getMinecraft().modelManager.getBlockModelShapes().getBlockStateMapper();
        try{
            for(JsonElement element : targetsJson){
                if(element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()){ // Handle simple strings
                    if(!IdentifierUtil.isValidIdentifier(element.getAsString()))
                        throw new JsonParseException("Target must be a valid identifier, not '" + element.getAsString() + "'!!");
                    ResourceLocation identifier = new ResourceLocation(element.getAsString());
                    if(!ForgeRegistries.BLOCKS.containsKey(identifier))
                        throw new JsonParseException("Could not find a block for identifier '" + identifier + "'!");
                    Block block = ForgeRegistries.BLOCKS.getValue(identifier);
                    block.getBlockState().getValidStates().stream()
                        .map(state -> blockStateMapper.getVariants(state.getBlock()).get(state))
                        .forEach(targets::add);
                }else if(element.isJsonObject()){ // Handle blocks with specific state properties
                    parseTarget(element.getAsJsonObject())
                        .map(state -> blockStateMapper.getVariants(state.getBlock()).get(state))
                        .forEach(targets::add);
                }else
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
        List<ModelEntry> defaultModel = Collections.emptyList();
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
            Set<ResourceLocation> models = new LinkedHashSet<>(); // This should maintain order
            if(!json.get("append").isJsonArray())
                throw new JsonParseException("Property 'append' must be an array!");
            JsonArray appendJson = json.getAsJsonArray("append");
            for(JsonElement element : appendJson){
                if(!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString())
                    throw new JsonParseException("Array property 'append' must only contain strings!");
                if(!IdentifierUtil.isValidIdentifier(element.getAsString()))
                    throw new JsonParseException("Model must be a valid identifier, not '" + element.getAsString() + "'!!");
                models.add(new ResourceLocation(element.getAsString()));
            }
            for(ResourceLocation model : models)
                appendModels.add(ImmutableList.of(ModelEntry.simple(model)));
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
        for(ModelResourceLocation target : targets)
            this.modifiers.computeIfAbsent(target, t -> new ArrayList<>(8)).add(properties);
    }

    private static Stream<IBlockState> parseTarget(JsonObject json){
        // Block
        if(!json.has("block") || !json.get("block").isJsonPrimitive() || !json.getAsJsonPrimitive("block").isString())
            throw new JsonParseException("Entry must have string property 'block'!");
        if(!IdentifierUtil.isValidIdentifier(json.get("block").getAsString()))
            throw new JsonParseException("Property 'block' must be a valid identifier, not '" + json.get("block").getAsString() + "'!!");
        ResourceLocation identifier = new ResourceLocation(json.get("block").getAsString());
        if(!ForgeRegistries.BLOCKS.containsKey(identifier))
            throw new JsonParseException("Could not find a block for identifier '" + identifier + "'!");
        Block block = ForgeRegistries.BLOCKS.getValue(identifier);

        // Properties
        Map<IProperty<?>,Set<?>> properties = new HashMap<>();
        if(!json.has("properties") || !json.get("properties").isJsonObject())
            throw new JsonParseException("Entry must have object property 'properties'!");
        if(json.getAsJsonObject("properties").size() == 0)
            throw new JsonParseException("At least one property must be specified!");
        for(Map.Entry<String,JsonElement> entry : json.getAsJsonObject("properties").entrySet()){
            try{
                // Parse the property
                IProperty<?> property = block.getBlockState().getProperty(entry.getKey());
                if(property == null)
                    throw new JsonParseException("Block '" + identifier + "' does not have a property named '" + entry.getKey() + "'!");
                // Parse the values
                ImmutableSet.Builder<Object> builder = ImmutableSet.builder();
                if(entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isString()){
                    Optional<?> value = property.parseValue(entry.getValue().getAsString()).toJavaUtil();
                    if(!value.isPresent())
                        throw new JsonParseException("Unknown value '" + entry.getValue().getAsString() + "' for property '" + property.getName() + "' in block '" + identifier + "'!");
                    builder.add(value.get());
                }else if(entry.getValue().isJsonArray()){
                    if(entry.getValue().getAsJsonArray().size() == 0)
                        throw new JsonParseException("Valid values cannot be empty!");
                    for(JsonElement element : entry.getValue().getAsJsonArray()){
                        if(!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString())
                            throw new JsonParseException("Values array must only contain strings!");
                        Optional<?> value = property.parseValue(element.getAsString()).toJavaUtil();
                        if(!value.isPresent())
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
        Stream<IBlockState> states = Stream.of(block.getBlockState().getBaseState());
        for(IProperty<?> property : block.getBlockState().getProperties()){
            if(properties.containsKey(property)){
                Set<?> values = properties.get(property);
                states = states.flatMap(state -> values.stream().map(value -> stateWithValue(state, property, value)));
            }else
                states = states.flatMap(state -> property.getAllowedValues().stream().map(value -> stateWithValue(state, property, value)));
        }
        return states;
    }

    private static <T extends Comparable<T>> IBlockState stateWithValue(IBlockState state, IProperty<?> property, Object value){
        //noinspection unchecked
        return state.withProperty((IProperty<T>)property, (T)value);
    }

    /**
     * Copies the listResources behaviour from 1.14+
     */
    public static Collection<ResourceLocation> listResources(IResourceManager resourceManager, String folder, Predicate<String> predicate){
        if(resourceManager instanceof SimpleReloadableResourceManager){
            Set<ResourceLocation> resources = Sets.newHashSet();

            for(FallbackResourceManager domainManager : ((SimpleReloadableResourceManager)resourceManager).domainResourceManagers.values()){
                resources.addAll(listResources(domainManager, folder, predicate));
            }

            List<ResourceLocation> list = Lists.newArrayList(resources);
            Collections.sort(list);
            return list;
        }
        if(resourceManager instanceof FallbackResourceManager){
            List<ResourceLocation> resources = Lists.newArrayList();

            for(IResourcePack pack : ((FallbackResourceManager)resourceManager).resourcePacks){
                if(pack instanceof ResourcePackExtension)
                    resources.addAll(((ResourcePackExtension)pack).fusionGetResources(folder, Integer.MAX_VALUE, predicate));
            }

            Collections.sort(resources);
            return resources;
        }
        return Collections.emptyList();
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
            return ImmutableList.of(ModelEntry.simple(new ResourceLocation(identifier)));
        }

        JsonObject object = element.getAsJsonObject();

        // Model identifier
        if(!object.has("model") || !object.get("model").isJsonPrimitive() || !object.getAsJsonPrimitive("model").isString())
            throw new JsonParseException("Object must have string property 'model'!");
        if(!IdentifierUtil.isValidIdentifier(object.get("model").getAsString()))
            throw new JsonParseException("Property 'model' must be a valid identifier, not '" + object.get("model").getAsString() + "'!");
        ResourceLocation model = new ResourceLocation(object.get("model").getAsString());

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

        return ImmutableList.of(new ModelEntry(model, conditions, showBreakingOverlay));
    }

    private static final class Properties{
        private final int priority;
        private final ResourceLocation location;
        private final List<ModelEntry> defaultModelOverrides;
        private final List<List<ModelEntry>> appendModels;
        private final boolean paneCullingFix;
        private final Boolean showBreakingOverlay;

        private Properties(int priority, ResourceLocation location, List<ModelEntry> defaultModelOverrides, List<List<ModelEntry>> appendModels, boolean paneCullingFix, Boolean showBreakingOverlay){
            this.priority = priority;
            this.location = location;
            this.defaultModelOverrides = defaultModelOverrides;
            this.appendModels = appendModels;
            this.paneCullingFix = paneCullingFix;
            this.showBreakingOverlay = showBreakingOverlay;
        }
    }

    private static final class ModelEntry{
        static ModelEntry simple (ResourceLocation model){
            return new ModelEntry(model, null, null);
        }

        private final ResourceLocation model;
        private final @Nullable BlockStateModelPredicate conditions;
        private final Boolean showBreakingOverlay;

        private ModelEntry(ResourceLocation model, @Nullable BlockStateModelPredicate conditions, Boolean showBreakingOverlay){
            this.model = model;
            this.conditions = conditions;
            this.showBreakingOverlay = showBreakingOverlay;
        }
    }
}
