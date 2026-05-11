package com.supermartijn642.fusion.model.types.connecting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.ModelInstance;
import com.supermartijn642.fusion.api.model.custom.*;
import com.supermartijn642.fusion.api.model.custom.geometry.CuboidModelGeometry;
import com.supermartijn642.fusion.api.model.custom.geometry.ModelGeometry;
import com.supermartijn642.fusion.api.model.custom.quad.QuadAccess;
import com.supermartijn642.fusion.api.model.types.connecting.ConnectingModelData;
import com.supermartijn642.fusion.api.model.types.connecting.predicates.ConnectionPredicate;
import com.supermartijn642.fusion.api.model.types.connecting.predicates.DefaultConnectionPredicates;
import com.supermartijn642.fusion.api.model.types.connecting.predicates.FusionConnectionPredicateRegistry;
import com.supermartijn642.fusion.api.util.Either;
import com.supermartijn642.fusion.model.types.UnknownModelType;
import com.supermartijn642.fusion.model.types.base.BaseModelType;
import net.minecraft.client.renderer.Vector3f;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.model.ItemTransformVec3f;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created 27/04/2023 by SuperMartijn642
 */
public class ConnectingModelType extends BaseModelType<ConnectingModelData,ConnectingModelDataBuilderImpl> {

    private static final ConnectionPredicate FALLBACK_PREDICATE = DefaultConnectionPredicates.isSameState();

    @Override
    public <X, C> Optional<X> getProperty(ModelProperty<X,C> property, C context, ConnectingModelData data){
        if(property == ModelProperty.MODEL_CONNECTION_PREDICATES)
            //noinspection unchecked
            return Optional.of((X)data.getAllConnectionPredicates());
        return super.getProperty(property, context, data);
    }

    @Override
    public IBakedModel bakeModel(ModelBakingContext context, ConnectingModelData data){
        // Bake geometry
        List<ConnectingBakedModel.Part> parts = new ArrayList<>();
        context.walkModelTree(ModelInstance.of(this, data), (modelInstance, stack) -> {
            ModelGeometry geometry = modelInstance.getGeometry();
            if(geometry == null)
                return ModelWalker.Result.proceed();
            // Resolve materials
            Set<String> missingKeys = new HashSet<>();
            ModelGeometry.MaterialResolver materialResolver = ModelGeometry.MaterialResolver.fromKeyLookup(
                key -> UnknownModelType.findPropertyInStackAndParents(context, stack, m -> m.getMaterial(key), null),
                context::getMaterial,
                missingKeys::add,
                keys -> context.pushWarning("Found circular material chain (" + keys.stream().map(k -> "'#" + k + "'").collect(Collectors.joining(" -> ")) + ") for model stack (" + stack + ")!")
            );
            // Resolve connection predicates
            Function<String,@Nullable ConnectionPredicate> connectionsResolver = createConnectionsResolver(
                context,
                stack,
                keys -> context.pushWarning("Found circular connections key chain (" + keys.stream().map(k -> "'#" + k + "'").collect(Collectors.joining(" -> ")) + ") for model stack (" + stack + ")!")
            );
            // Compose transformations
            ModelTransform transforms = stack.composeTransforms();
            transforms = ModelTransform.compose(transforms, context.getTransformation());
            // Bake the geometry
            ConnectingModelQuads.Builder quads;
            if(geometry.isCuboidGeometry())
                quads = bakeCuboidGeometry((CuboidModelGeometry)geometry, transforms, materialResolver, connectionsResolver);
            else
                quads = ConnectingModelQuads.builder().add(geometry.bake(transforms, materialResolver));
            if(!missingKeys.isEmpty())
                context.pushWarning("Found missing materials " + missingKeys.stream().map(k -> "'#" + k + "'").collect(Collectors.joining(",")) + " for model stack (" + stack + ")!");
            // Apply model properties to the quads
            Boolean shade = UnknownModelType.findPropertyInStackAndParents(context, stack, ModelInstance::getShade, null);
            Boolean emissive = UnknownModelType.findPropertyInStackAndParents(context, stack, ModelInstance::getEmissive, null);
            quads = quads.mutateQuads((side, quad) -> {
                if(shade != null)
                    quad.shade(shade);
                if(emissive != null)
                    quad.emissive(emissive);
                BaseModelType.applyTextureProperties(quad);
                return true;
            });
            // Create a new part
            parts.add(new ConnectingBakedModel.Part(
                quads.build()
            ));
            return ModelWalker.Result.endBranch();
        });

        // Find particle sprite
        ModelMaterial particleMaterial = context.walkModelTree(ModelInstance.of(this, data), (modelInstance, stack) -> {
            ModelMaterial material = stack.findMaterialRecursive(
                "particle",
                l -> {}
            );
            return material == null ? ModelWalker.Result.proceed() : ModelWalker.Result.stop(material);
        }).orElse(null);
        if(particleMaterial == null){
            context.pushWarning("Could not resolve 'particle' material!");
            particleMaterial = ModelMaterial.missing();
        }
        TextureAtlasSprite resolvedParticleMaterial = context.getMaterial(particleMaterial);
        // Find ambient occlusion
        boolean ambientOcclusion = context.walkModelTree(
            ModelInstance.of(this, data),
            (modelInstance, stack) -> {
                Boolean v = modelInstance.getAmbientOcclusion();
                return v == null ? ModelWalker.Result.proceed() : ModelWalker.Result.stop(v);
            }
        ).orElse(true);
        // Find gui 3d
        boolean isGui3d = context.walkModelTree(
            ModelInstance.of(this, data),
            (modelInstance, stack) -> {
                Boolean v = modelInstance.getIsGui3d();
                return v == null ? ModelWalker.Result.proceed() : ModelWalker.Result.stop(v);
            }
        ).orElse(true);
        // Find item transforms
        BiFunction<ItemCameraTransforms.TransformType,ItemTransformVec3f,ItemTransformVec3f> itemTransformResolver = (type, fallback) ->
            context.walkModelTree(
                ModelInstance.of(this, data),
                (modelInstance, stack) -> {
                    ItemTransformVec3f transform = modelInstance.getItemTransform(type);
                    return transform == null ? ModelWalker.Result.proceed() : ModelWalker.Result.stop(transform);
                }
            ).orElse(fallback);
        ItemCameraTransforms itemTransforms = new ItemCameraTransforms(
            itemTransformResolver.apply(ItemCameraTransforms.TransformType.THIRD_PERSON_LEFT_HAND, ItemTransformVec3f.NO_TRANSFORM),
            itemTransformResolver.apply(ItemCameraTransforms.TransformType.THIRD_PERSON_RIGHT_HAND, ItemTransformVec3f.NO_TRANSFORM),
            itemTransformResolver.apply(ItemCameraTransforms.TransformType.FIRST_PERSON_LEFT_HAND, ItemTransformVec3f.NO_TRANSFORM),
            itemTransformResolver.apply(ItemCameraTransforms.TransformType.FIRST_PERSON_RIGHT_HAND, ItemTransformVec3f.NO_TRANSFORM),
            itemTransformResolver.apply(ItemCameraTransforms.TransformType.HEAD, ItemTransformVec3f.NO_TRANSFORM),
            itemTransformResolver.apply(ItemCameraTransforms.TransformType.GUI, ItemTransformVec3f.NO_TRANSFORM),
            itemTransformResolver.apply(ItemCameraTransforms.TransformType.GROUND, ItemTransformVec3f.NO_TRANSFORM),
            itemTransformResolver.apply(ItemCameraTransforms.TransformType.FIXED, ItemTransformVec3f.NO_TRANSFORM)
        );

        // Finally, create the model
        return new ConnectingBakedModel(
            parts,
            resolvedParticleMaterial,
            ambientOcclusion,
            isGui3d,
            itemTransforms
        );
    }

    private static ConnectingModelQuads.Builder bakeCuboidGeometry(CuboidModelGeometry geometry,
                                                                   ModelTransform transformation,
                                                                   ModelGeometry.MaterialResolver materialResolver,
                                                                   Function<String,@Nullable ConnectionPredicate> connectionsResolver){
        ConnectingModelQuads.Builder quads = ConnectingModelQuads.builder();
        for(CuboidModelGeometry.Element element : geometry.elements()){
            // Create quads the same way as vanilla
            // Check whether the size is 0 for any axis
            Vector3f from = element.from();
            Vector3f to = element.to();
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

                boolean shouldDrawFace;
                switch(side.getAxis()){
                    case X:
                        shouldDrawFace = drawXFaces;
                        break;
                    case Y:
                        shouldDrawFace = drawYFaces;
                        break;
                    case Z:
                        shouldDrawFace = drawZFaces;
                        break;
                    default:
                        throw new AssertionError();
                }
                if(!shouldDrawFace)
                    continue;

                // Bake the face
                QuadAccess quad = CuboidModelGeometry.bakeFace(face, element, side, transformation, materialResolver);
                // Resolve connections key
                Optional<String> key = face.getProperty(ModelProperty.FACE_CONNECTIONS_KEY);
                ConnectionPredicate connectionPredicate = connectionsResolver.apply(key.orElse(ConnectingModelData.DEFAULT_KEY));
                // Add the quad
                Direction cullDirection = face.cullDirection() == null ? null :
                    transformation.toTransformation().rotateTransform(face.cullDirection());
                quads.add(cullDirection, quad, connectionPredicate);
            }
        }
        return quads;
    }

    private static Function<String,@Nullable ConnectionPredicate> createConnectionsResolver(ModelBakingContext context, ModelWalker.ModelStack stack, Consumer<List<String>> reportCircular){
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
                Either<String,ConnectionPredicate> next = UnknownModelType.findPropertyInStackAndParents(context, stack, m -> m.getProperty(ModelProperty.MODEL_CONNECTION_PREDICATES).map(connections -> connections.get(finalKey)).orElse(null), null);
                if(next != null){
                    if(next.isRight()){
                        ConnectionPredicate predicate = next.right();
                        for(String encounteredKey : encounteredKeys)
                            resolvedConnections.put(encounteredKey, predicate);
                        return predicate;
                    }
                    key = next.left();
                }else{ // Check materials map
                    Either<String,ModelMaterial> material = UnknownModelType.findPropertyInStackAndParents(context, stack, m -> m.getMaterial(finalKey), null);
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
                ConnectionPredicate previouslyResolved = resolvedConnections.get(key);
                if(previouslyResolved != null){
                    for(String encounteredKey : encounteredKeys)
                        resolvedConnections.put(encounteredKey, previouslyResolved);
                    return previouslyResolved;
                }
            }
            for(String encounteredKey : encounteredKeys)
                resolvedConnections.put(encounteredKey, FALLBACK_PREDICATE);
            return FALLBACK_PREDICATE;
        };
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
                throw new JsonParseException("Property 'connections' must be an array!");
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

    private static ConnectionPredicate loadPredicate(JsonElement element, String key){
        if(element.isJsonArray()){
            JsonArray array = element.getAsJsonArray();
            List<ConnectionPredicate> subPredicates = new ArrayList<>();
            for(JsonElement predicateElements : array){
                if(!predicateElements.isJsonObject())
                    throw new JsonParseException("Predicate '" + key + "' must only contain objects!");
                ConnectionPredicate predicate = FusionConnectionPredicateRegistry.deserializeConnectionPredicate(predicateElements.getAsJsonObject());
                subPredicates.add(predicate);
            }
            return DefaultConnectionPredicates.or(subPredicates.toArray(new ConnectionPredicate[0]));
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
            builder.property(ModelProperty.FACE_CONNECTIONS_KEY, key);
        }
        return builder;
    }

    @Override
    protected JsonObject serializeFace(CuboidModelGeometry.Face face){
        JsonObject json = super.serializeFace(face);
        Optional<String> key = face.getProperty(ModelProperty.FACE_CONNECTIONS_KEY);
        key.ifPresent(s -> json.addProperty("connections", s));
        return json;
    }
}
