package com.supermartijn642.fusion.model.types.connecting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.custom.*;
import com.supermartijn642.fusion.api.model.custom.geometry.CuboidModelGeometry;
import com.supermartijn642.fusion.api.model.custom.geometry.ModelGeometry;
import com.supermartijn642.fusion.api.model.custom.quad.MutableQuad;
import com.supermartijn642.fusion.api.model.custom.quad.QuadAccess;
import com.supermartijn642.fusion.api.model.predicates.ModelPredicate;
import com.supermartijn642.fusion.api.model.types.connecting.ConnectingModelData;
import com.supermartijn642.fusion.api.texture.SpriteHelper;
import com.supermartijn642.fusion.api.texture.custom.BlockStateQuadProcessor;
import com.supermartijn642.fusion.api.texture.custom.ItemQuadProcessor;
import com.supermartijn642.fusion.api.texture.custom.SpriteInstance;
import com.supermartijn642.fusion.api.texture.types.connecting.predicates.ConnectionPredicate;
import com.supermartijn642.fusion.api.texture.types.connecting.predicates.DefaultConnectionPredicates;
import com.supermartijn642.fusion.api.texture.types.connecting.predicates.FusionConnectionPredicateRegistry;
import com.supermartijn642.fusion.api.util.Either;
import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.api.util.Property;
import com.supermartijn642.fusion.api.util.PropertyStore;
import com.supermartijn642.fusion.model.types.base.BaseBlockStateModel;
import com.supermartijn642.fusion.model.types.base.BaseItemModel;
import com.supermartijn642.fusion.model.types.base.BaseModelType;
import com.supermartijn642.fusion.util.CullingHelper;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3fc;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created 27/04/2023 by SuperMartijn642
 */
public class ConnectingModelType extends BaseModelType<ConnectingModelData,ConnectingModelDataBuilderImpl> {

    public static final Property<ConnectionPredicate,Void> FACE_CONNECTION_PREDICATE = Property.create();

    @Override
    public <X, C> Optional<X> getProperty(Property<X,C> property, C context, ConnectingModelData data){
        if(property == DefaultModelProperties.MODEL_CONNECTION_PREDICATES)
            //noinspection unchecked
            return Optional.of((X)data.getAllConnectionPredicates());
        if(property == DefaultModelProperties.MODEL_CONNECTION_PREDICATE)
            //noinspection unchecked,SuspiciousMethodCalls
            return Optional.ofNullable((X)data.getAllConnectionPredicates().get(context));
        return super.getProperty(property, context, data);
    }

    @Override
    public @Nullable BakedModel bakeBlockStateModel(BlockStateModelBakingContext context, ModelStack modelStack, ConnectingModelData data){
        // Bake geometry
        ModelGeometry geometry = this.getGeometry(data);
        if(geometry != null){
            // Create shared property store
            PropertyStore propertyStore = PropertyStore.create();
            // Resolve materials
            Set<String> missingKeys = new HashSet<>();
            ModelGeometry.MaterialResolver materialResolver = ModelGeometry.MaterialResolver.fromKeyLookup(
                key -> modelStack.findMaterialIncludingParents(key, context),
                context::getMaterial,
                missingKeys::add,
                keys -> context.pushWarning("Found circular material chain (" + keys.stream().map(k -> "'#" + k + "'").collect(Collectors.joining(" -> ")) + ") for model stack (" + modelStack + ")!")
            );
            // Resolve connection predicates
            Function<String,@Nullable ConnectionPredicate> connectionsResolver = createConnectionsResolver(
                context,
                modelStack,
                keys -> context.pushWarning("Found circular connections key chain (" + keys.stream().map(k -> "'#" + k + "'").collect(Collectors.joining(" -> ")) + ") for model stack (" + modelStack + ")!")
            );
            // Compose transformations
            ModelTransform transforms = modelStack.composeTransforms();
            transforms = ModelTransform.compose(transforms, context.getTransformation());
            // Combine conditions
            ModelPredicate conditions = modelStack.combineConditions();
            if(conditions != null)
                conditions = conditions.simplify();
            // Bake the geometry
            List<Pair<QuadAccess,ConnectionPredicate>>[] quads;
            if(geometry.isCuboidGeometry())
                quads = bakeCuboidGeometry((CuboidModelGeometry)geometry, transforms, materialResolver, connectionsResolver);
            else{
                //noinspection unchecked
                quads = new List[7];
                CullableQuads bakedGeometry = geometry.bake(transforms, materialResolver);
                for(Direction cullDirection : CullingHelper.cullDirections()){
                    quads[CullingHelper.cullIndex(cullDirection)] = bakedGeometry.get(cullDirection).stream()
                        .map(q -> Pair.of(q, (ConnectionPredicate)null))
                        .toList();
                }
            }
            if(!missingKeys.isEmpty())
                context.pushWarning("Found missing materials " + missingKeys.stream().map(k -> "'#" + k + "'").collect(Collectors.joining(",")) + " for model stack (" + modelStack + ")!");
            // Apply model properties to the quads
            Boolean ambientOcclusion = modelStack.findAmbientOcclusionIncludingParents(context);
            Boolean shade = modelStack.findShadeIncludingParents(context);
            Boolean emissive = modelStack.findEmissiveIncludingParents(context);
            // Initialize quads
            //noinspection unchecked
            List<BaseBlockStateModel.Quad>[] processedQuads = new List[7];
            MutableQuad mutableQuad = MutableQuad.create();
            for(Direction cullDirection : CullingHelper.cullDirections()){
                List<BaseBlockStateModel.Quad> directionQuads = new ArrayList<>(quads[CullingHelper.cullIndex(cullDirection)].size());
                for(Pair<QuadAccess,ConnectionPredicate> pair : quads[CullingHelper.cullIndex(cullDirection)]){
                    QuadAccess quad = pair.left();
                    // Get the sprite instance
                    SpriteInstance sprite = SpriteHelper.getSpriteInstance(quad.sprite());
                    // Put the face's connection predicate into the property store
                    ConnectionPredicate predicate = pair.right();
                    if(predicate != null)
                        predicate = predicate.simplify();
                    propertyStore.setProperty(FACE_CONNECTION_PREDICATE, predicate);
                    // Initialize the quad
                    BlockStateQuadProcessor<?> processor = null;
                    if(sprite != null){
                        mutableQuad.copyFrom(quad);
                        processor = sprite.getTexture().initializeBlockStateModelQuad(mutableQuad, sprite, propertyStore);
                        quad = mutableQuad.createCopy();
                        SpriteInstance newSprite = SpriteHelper.getSpriteInstance(quad.sprite());
                        if(newSprite != null)
                            sprite = newSprite;
                    }
                    // Apply model properties
                    if(ambientOcclusion != null)
                        mutableQuad.ambientOcclusion(ambientOcclusion);
                    if(shade != null)
                        mutableQuad.shade(shade);
                    if(emissive != null)
                        mutableQuad.emissive(emissive);
                    // Create quad
                    //noinspection unchecked
                    directionQuads.add(new BaseBlockStateModel.Quad(
                        quad,
                        sprite,
                        (BlockStateQuadProcessor<Object>)processor
                    ));
                }
                processedQuads[CullingHelper.cullIndex(cullDirection)] = List.copyOf(directionQuads);
            }
            propertyStore.setProperty(FACE_CONNECTION_PREDICATE, null);
            // Resolve particle material
            TextureAtlasSprite particleSprite = materialResolver.get("particle");
            if(ModelMaterial.isMissingSprite(particleSprite))
                context.pushWarning("Could not resolve 'particle' material for model stack (" + modelStack + ")!");
            // Create the model
            return new BaseBlockStateModel(
                new BaseBlockStateModel.Quads(processedQuads),
                conditions,
                particleSprite,
                propertyStore
            );
        }

        // Bake parent
        ResourceLocation parent = data.getParent();
        if(parent != null){
            UntypedModelInstance parentModel = context.getModelOrMissing(parent);
            return parentModel.bakeBlockStateModel(context, modelStack.push(parentModel, parent));
        }

        // If there's no geometry, return null
        return null;
    }

    private static List<Pair<QuadAccess,ConnectionPredicate>>[] bakeCuboidGeometry(CuboidModelGeometry geometry,
                                                                                   ModelTransform transformation,
                                                                                   ModelGeometry.MaterialResolver materialResolver,
                                                                                   Function<String,@Nullable ConnectionPredicate> connectionsResolver){
        //noinspection unchecked
        List<Pair<QuadAccess,ConnectionPredicate>>[] quads = new List[7];
        for(int cullIndex = 0; cullIndex < 7; cullIndex++)
            quads[cullIndex] = new ArrayList<>();
        for(CuboidModelGeometry.Element element : geometry.elements()){
            // Create quads the same way as vanilla
            // Check whether the size is 0 for any axis
            Vector3fc from = element.from();
            Vector3fc to = element.to();
            boolean drawXFaces = from.y() != to.y() && from.z() != to.z();
            boolean drawYFaces = from.x() != to.x() && from.z() != to.z();
            boolean drawZFaces = from.x() != to.x() && from.y() != to.y();
            if(!drawXFaces && !drawYFaces && !drawZFaces)
                continue;

            // Create the quads for each side
            for(Direction side : Direction.values()){
                CuboidModelGeometry.Face face = element.face(side);
                if(face == null)
                    continue;

                boolean shouldDrawFace = switch(side.getAxis()){
                    case X -> drawXFaces;
                    case Y -> drawYFaces;
                    case Z -> drawZFaces;
                };
                if(!shouldDrawFace)
                    continue;

                // Bake the face
                QuadAccess quad = CuboidModelGeometry.bakeFace(face, element, side, transformation, materialResolver);
                // Resolve connections key
                Optional<String> key = face.getProperty(DefaultModelProperties.FACE_CONNECTIONS_KEY);
                ConnectionPredicate connectionPredicate = connectionsResolver.apply(key.orElse(face.material()));
                // Add the quad
                Direction cullDirection = face.cullDirection() == null ? null :
                    Direction.rotate(new Matrix4f(transformation.matrix()), face.cullDirection());
                quads[CullingHelper.cullIndex(cullDirection)].add(Pair.of(quad, connectionPredicate));
            }
        }
        return quads;
    }

    private static Function<String,@Nullable ConnectionPredicate> createConnectionsResolver(BlockStateModelBakingContext context, ModelStack stack, Consumer<List<String>> reportCircular){
        // Create function to resolve specific key
        Map<String,ConnectionPredicate> resolvedConnections = new HashMap<>();
        return key -> {
            // Check if the key has already been resolved
            if(resolvedConnections.containsKey(key))
                return resolvedConnections.get(key);
            // Resolve the key
            List<String> encounteredKeys = new ArrayList<>();
            while(true){
                encounteredKeys.add(key);
                final String finalKey = key;
                Either<String,ConnectionPredicate> next = stack.findPropertyIncludingParents(DefaultModelProperties.MODEL_CONNECTION_PREDICATE, finalKey, context).orElse(null);
                if(next != null){
                    if(next.isRight()){
                        ConnectionPredicate predicate = next.right();
                        for(String encounteredKey : encounteredKeys)
                            resolvedConnections.put(encounteredKey, predicate);
                        return predicate;
                    }
                    key = next.left();
                }else{ // Check materials map
                    Either<String,ModelMaterial> material = stack.findMaterialIncludingParents(finalKey, context);
                    if(material == null){
                        if(key.equals(ConnectingModelData.DEFAULT_KEY))
                            break;
                        key = ConnectingModelData.DEFAULT_KEY;
                    }else
                        key = material.flatMap(Function.identity(), m -> m.texture().toString());
                }
                if(encounteredKeys.contains(key)){
                    encounteredKeys.add(key);
                    reportCircular.accept(Collections.unmodifiableList(encounteredKeys));
                    break;
                }
                if(resolvedConnections.containsKey(key)){
                    ConnectionPredicate previouslyResolved = resolvedConnections.get(key);
                    for(String encounteredKey : encounteredKeys)
                        resolvedConnections.put(encounteredKey, previouslyResolved);
                    return previouslyResolved;
                }
            }
            for(String encounteredKey : encounteredKeys)
                resolvedConnections.put(encounteredKey, null);
            return null;
        };
    }

    @Override
    public @Nullable ItemModel bakeItemModel(ItemModelBakingContext context, ModelStack modelStack, ConnectingModelData data){
        // Bake geometry
        ModelGeometry geometry = this.getGeometry(data);
        if(geometry != null){
            // Create shared property store
            PropertyStore propertyStore = PropertyStore.create();
            // Resolve materials
            Set<String> missingKeys = new HashSet<>();
            ModelGeometry.MaterialResolver materialResolver = ModelGeometry.MaterialResolver.fromKeyLookup(
                key -> modelStack.findMaterialIncludingParents(key, context),
                context::getMaterial,
                missingKeys::add,
                keys -> context.pushWarning("Found circular material chain (" + keys.stream().map(k -> "'#" + k + "'").collect(Collectors.joining(" -> ")) + ") for model stack (" + modelStack + ")!")
            );
            // Resolve connection predicates
            Function<String,@Nullable ConnectionPredicate> connectionsResolver = createConnectionsResolver(
                context,
                modelStack,
                keys -> context.pushWarning("Found circular connections key chain (" + keys.stream().map(k -> "'#" + k + "'").collect(Collectors.joining(" -> ")) + ") for model stack (" + modelStack + ")!")
            );
            // Compose transformations
            ModelTransform transforms = modelStack.composeTransforms();
            // Combine conditions
            ModelPredicate conditions = modelStack.combineConditions();
            if(conditions != null)
                conditions = conditions.simplify();
            // Bake the geometry
            List<Pair<QuadAccess,ConnectionPredicate>>[] quads;
            if(geometry.isCuboidGeometry())
                quads = bakeCuboidGeometry((CuboidModelGeometry)geometry, transforms, materialResolver, connectionsResolver);
            else{
                //noinspection unchecked
                quads = new List[7];
                CullableQuads bakedGeometry = geometry.bake(transforms, materialResolver);
                for(Direction cullDirection : CullingHelper.cullDirections()){
                    quads[CullingHelper.cullIndex(cullDirection)] = bakedGeometry.get(cullDirection).stream()
                        .map(q -> Pair.of(q, (ConnectionPredicate)null))
                        .toList();
                }
            }
            if(!missingKeys.isEmpty())
                context.pushWarning("Found missing materials " + missingKeys.stream().map(k -> "'#" + k + "'").collect(Collectors.joining(",")) + " for model stack (" + modelStack + ")!");
            // Apply model properties to the quads
            Boolean ambientOcclusion = modelStack.findAmbientOcclusionIncludingParents(context);
            Boolean shade = modelStack.findShadeIncludingParents(context);
            Boolean emissive = modelStack.findEmissiveIncludingParents(context);
            // Initialize quads
            List<BaseItemModel.Quad> processedQuads = new ArrayList<>();
            MutableQuad mutableQuad = MutableQuad.create();
            for(Direction cullDirection : CullingHelper.cullDirections()){
                for(Pair<QuadAccess,ConnectionPredicate> pair : quads[CullingHelper.cullIndex(cullDirection)]){
                    QuadAccess quad = pair.left();
                    // Get the sprite instance
                    SpriteInstance sprite = SpriteHelper.getSpriteInstance(quad.sprite());
                    // Put the face's connection predicate into the property store
                    ConnectionPredicate predicate = pair.right();
                    if(predicate != null)
                        predicate = predicate.simplify();
                    propertyStore.setProperty(FACE_CONNECTION_PREDICATE, predicate);
                    // Initialize the quad
                    ItemQuadProcessor<?> processor = null;
                    if(sprite != null){
                        mutableQuad.copyFrom(quad);
                        processor = sprite.getTexture().initializeItemModelQuad(mutableQuad, sprite, propertyStore);
                        quad = mutableQuad.createCopy();
                        SpriteInstance newSprite = SpriteHelper.getSpriteInstance(quad.sprite());
                        if(newSprite != null)
                            sprite = newSprite;
                    }
                    // Apply model properties
                    if(ambientOcclusion != null)
                        mutableQuad.ambientOcclusion(ambientOcclusion);
                    if(shade != null)
                        mutableQuad.shade(shade);
                    if(emissive != null)
                        mutableQuad.emissive(emissive);
                    // Create quad
                    //noinspection unchecked
                    processedQuads.add(new BaseItemModel.Quad(
                        quad,
                        sprite,
                        (ItemQuadProcessor<Object>)processor
                    ));
                }
            }
            propertyStore.setProperty(FACE_CONNECTION_PREDICATE, null);
            // Resolve particle material
            TextureAtlasSprite particleSprite = materialResolver.get("particle");
            if(ModelMaterial.isMissingSprite(particleSprite))
                context.pushWarning("Could not resolve 'particle' material for model stack (" + modelStack + ")!");
            // Resolve gui light
            UnbakedModel.GuiLight guiLight = modelStack.findGuiLightIncludingParents(context);
            if(guiLight == null)
                guiLight = UnbakedModel.GuiLight.SIDE;
            // Resolve item transforms
            BiFunction<ItemDisplayContext,ItemTransform,ItemTransform> itemTransformResolver = (type, fallback) -> {
                ItemTransform transform = modelStack.findItemTransformIncludingParents(type, context);
                return transform == null ? fallback : transform;
            };
            ItemTransforms itemTransforms = new ItemTransforms(
                itemTransformResolver.apply(ItemDisplayContext.THIRD_PERSON_LEFT_HAND, ItemTransform.NO_TRANSFORM),
                itemTransformResolver.apply(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, ItemTransform.NO_TRANSFORM),
                itemTransformResolver.apply(ItemDisplayContext.FIRST_PERSON_LEFT_HAND, ItemTransform.NO_TRANSFORM),
                itemTransformResolver.apply(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, ItemTransform.NO_TRANSFORM),
                itemTransformResolver.apply(ItemDisplayContext.HEAD, ItemTransform.NO_TRANSFORM),
                itemTransformResolver.apply(ItemDisplayContext.GUI, ItemTransform.NO_TRANSFORM),
                itemTransformResolver.apply(ItemDisplayContext.GROUND, ItemTransform.NO_TRANSFORM),
                itemTransformResolver.apply(ItemDisplayContext.FIXED, ItemTransform.NO_TRANSFORM)
            );
            // Create the model
            return new BaseItemModel(
                processedQuads,
                conditions,
                propertyStore,
                guiLight,
                particleSprite,
                itemTransforms,
                context.getTintSources()
            );
        }

        // Bake parent
        ResourceLocation parent = data.getParent();
        if(parent != null){
            UntypedModelInstance parentModel = context.getModelOrMissing(parent);
            return parentModel.bakeItemModel(context, modelStack.push(parentModel, parent));
        }

        // If there's no geometry, return null
        return null;
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
                if(object.isEmpty())
                    throw new JsonParseException("Property 'connections' must have a 'type' key or keys per texture!");
                for(String key : object.keySet()){
                    if(object.get(key).isJsonPrimitive() && object.getAsJsonPrimitive(key).isString()){
                        String reference = object.get(key).getAsString();
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
