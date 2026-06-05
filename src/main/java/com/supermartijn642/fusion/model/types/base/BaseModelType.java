package com.supermartijn642.fusion.model.types.base;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.ModelInstance;
import com.supermartijn642.fusion.api.model.ModelType;
import com.supermartijn642.fusion.api.model.custom.*;
import com.supermartijn642.fusion.api.model.custom.geometry.CuboidModelGeometry;
import com.supermartijn642.fusion.api.model.custom.geometry.ModelGeometry;
import com.supermartijn642.fusion.api.model.custom.quad.MutableQuad;
import com.supermartijn642.fusion.api.model.custom.quad.QuadAccess;
import com.supermartijn642.fusion.api.model.predicates.ModelPredicate;
import com.supermartijn642.fusion.api.model.types.base.BaseModelData;
import com.supermartijn642.fusion.api.texture.SpriteHelper;
import com.supermartijn642.fusion.api.texture.custom.BlockStateQuadProcessor;
import com.supermartijn642.fusion.api.texture.custom.ItemQuadProcessor;
import com.supermartijn642.fusion.api.texture.custom.SpriteInstance;
import com.supermartijn642.fusion.api.util.Either;
import com.supermartijn642.fusion.api.util.Property;
import com.supermartijn642.fusion.api.util.PropertyStore;
import com.supermartijn642.fusion.model.types.UnknownModelType;
import com.supermartijn642.fusion.util.CullingHelper;
import com.supermartijn642.fusion.util.IdentifierUtil;
import net.minecraft.client.renderer.block.model.BlockElementRotation;
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
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Created 06/09/2024 by SuperMartijn642
 */
public abstract class BaseModelType<T extends BaseModelData, BUILDER extends BaseModelData.Builder<BUILDER,T>> implements ModelType<T> {

    public static <T extends BaseModelData.Builder<T,BaseModelData>> ModelType<BaseModelData> create(){
        return new BaseModelType<BaseModelData,T>() {
            @Override
            protected T builder(){
                //noinspection unchecked
                return (T)BaseModelData.builder();
            }
        };
    }

    @Override
    public Collection<ResourceLocation> getDependencies(T data){
        return data.getParent() == null ? List.of() : List.of(data.getParent());
    }

    @Override
    public List<Either<ResourceLocation,UntypedModelInstance>> getParents(T data){
        return data.getParent() == null ? List.of() : List.of(Either.left(data.getParent()));
    }

    @Override
    public @Nullable Boolean getAmbientOcclusion(T data){
        return data.getAmbientOcclusion();
    }

    @Override
    public UnbakedModel.@Nullable GuiLight getGuiLight(T data){
        return data.getGuiLight();
    }

    @Override
    public @Nullable ItemTransform getItemTransform(ItemDisplayContext type, T data){
        return data.getItemTransform(type);
    }

    @Override
    public Map<String,Either<String,ModelMaterial>> getMaterials(T data){
        return data.getMaterials();
    }

    @Override
    public @Nullable ModelGeometry getGeometry(T data){
        return data.getGeometry();
    }

    @Override
    public @Nullable Boolean getShade(T data){
        return data.getShade();
    }

    @Override
    public @Nullable Boolean getEmissive(T data){
        return data.getEmissive();
    }

    @Override
    public <X, C> Optional<X> getProperty(Property<X,C> property, C context, T data){
        return Optional.empty();
    }

    @Override
    public BakedModel bakeBlockStateModel(BlockStateModelBakingContext context, T data){
        // Create shared property store
        PropertyStore propertyStore = PropertyStore.create();

        // Bake geometry
        List<BaseBlockStateModel.Part> parts = new ArrayList<>();
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
            // Compose transformations
            ModelTransform transforms = stack.composeTransforms();
            transforms = ModelTransform.compose(transforms, context.getTransformation());
            // Combine conditions
            ModelPredicate conditions = stack.combineConditions();
            if(conditions != null)
                conditions = conditions.simplify();
            // Bake the geometry
            CullableQuads quads = geometry.bake(transforms, materialResolver);
            if(!missingKeys.isEmpty())
                context.pushWarning("Found missing materials " + missingKeys.stream().map(k -> "'#" + k + "'").collect(Collectors.joining(",")) + " for model stack (" + stack + ")!");
            // Apply model properties to the quads
            Boolean ambientOcclusion = UnknownModelType.findPropertyInStackAndParents(context, stack, UntypedModelInstance::getAmbientOcclusion, null);
            Boolean shade = UnknownModelType.findPropertyInStackAndParents(context, stack, UntypedModelInstance::getShade, null);
            Boolean emissive = UnknownModelType.findPropertyInStackAndParents(context, stack, UntypedModelInstance::getEmissive, null);
            // Initialize special texture quads
            //noinspection unchecked
            List<BaseBlockStateModel.Quad>[] processedQuads = new List[7];
            MutableQuad mutableQuad = MutableQuad.create();
            for(Direction cullDirection : CullingHelper.cullDirections()){
                List<BaseBlockStateModel.Quad> directionQuads = new ArrayList<>(quads.get(cullDirection).size());
                for(QuadAccess quad : quads.get(cullDirection)){
                    // Get the sprite instance
                    SpriteInstance sprite = SpriteHelper.getSpriteInstance(quad.sprite());
                    if(sprite == null){
                        directionQuads.add(new BaseBlockStateModel.Quad(
                            quad,
                            null,
                            null
                        ));
                        continue;
                    }
                    // Initialize the quad
                    mutableQuad.copyFrom(quad);
                    BlockStateQuadProcessor<?> processor = sprite.getTexture().initializeBlockStateModelQuad(mutableQuad, sprite, propertyStore);
                    SpriteInstance newSprite = SpriteHelper.getSpriteInstance(mutableQuad.sprite());
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
                        mutableQuad.createCopy(),
                        newSprite == null ? sprite : newSprite,
                        (BlockStateQuadProcessor<Object>)processor
                    ));
                }
                processedQuads[CullingHelper.cullIndex(cullDirection)] = List.copyOf(directionQuads);
            }
            // Create a new part
            parts.add(new BaseBlockStateModel.Part(
                new BaseBlockStateModel.Quads(processedQuads),
                conditions
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
            particleMaterial = ModelMaterial.missingBlockAtlas();
        }
        TextureAtlasSprite resolvedParticleMaterial = context.getMaterial(particleMaterial);

        // Finally, create the model
        return new BaseBlockStateModel(
            parts,
            resolvedParticleMaterial,
            propertyStore
        );
    }

    @Override
    public ItemModel bakeItemModel(ItemModelBakingContext context, T data){
        // Create shared property store
        PropertyStore propertyStore = PropertyStore.create();

        // Create a submodel for each geometry
        List<BaseItemModel.Part> parts = new ArrayList<>();
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
            // Compose transformations
            ModelTransform transforms = stack.composeTransforms();
            // Combine conditions
            ModelPredicate conditions = stack.combineConditions();
            if(conditions != null)
                conditions = conditions.simplify();
            // Bake the geometry
            CullableQuads quads = geometry.bake(transforms, materialResolver);
            if(!missingKeys.isEmpty())
                context.pushWarning("Found missing materials " + missingKeys.stream().map(k -> "'#" + k + "'").collect(Collectors.joining(",")) + " for model stack (" + stack + ")!");
            // Apply model properties to the quads
            Boolean ambientOcclusion = UnknownModelType.findPropertyInStackAndParents(context, stack, UntypedModelInstance::getAmbientOcclusion, null);
            Boolean shade = UnknownModelType.findPropertyInStackAndParents(context, stack, UntypedModelInstance::getShade, null);
            Boolean emissive = UnknownModelType.findPropertyInStackAndParents(context, stack, UntypedModelInstance::getEmissive, null);
            // Initialize special texture quads
            List<BaseItemModel.Quad> processedQuads = new ArrayList<>(quads.all().size());
            MutableQuad mutableQuad = MutableQuad.create();
            for(QuadAccess quad : quads.all()){
                // Get the sprite instance
                SpriteInstance sprite = SpriteHelper.getSpriteInstance(quad.sprite());
                if(sprite == null){
                    processedQuads.add(new BaseItemModel.Quad(
                        quad,
                        null,
                        null
                    ));
                    continue;
                }
                // Initialize the quad
                mutableQuad.copyFrom(quad);
                ItemQuadProcessor<?> processor = sprite.getTexture().initializeItemModelQuad(mutableQuad, sprite, propertyStore);
                SpriteInstance newSprite = SpriteHelper.getSpriteInstance(mutableQuad.sprite());
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
                    mutableQuad.createCopy(),
                    newSprite == null ? sprite : newSprite,
                    (ItemQuadProcessor<Object>)processor
                ));
            }
            // Resolve particle material
            TextureAtlasSprite particleSprite = materialResolver.get("particle");
            if(particleSprite == null)
                context.pushWarning("Could not resolve 'particle' material for model stack (" + stack + ")!");
            // Resolve gui light
            UnbakedModel.GuiLight guiLight = UnknownModelType.findPropertyInStackAndParents(context, stack, UntypedModelInstance::getGuiLight, UnbakedModel.GuiLight.SIDE);
            // Resolve item transforms
            BiFunction<ItemDisplayContext,ItemTransform,ItemTransform> itemTransformResolver = (type, fallback) ->
                UnknownModelType.findPropertyInStackAndParents(context, stack, m -> m.getItemTransform(type), fallback);
            ImmutableMap.Builder<ItemDisplayContext,ItemTransform> moddedTransforms = ImmutableMap.builder();
            for(ItemDisplayContext type : ItemDisplayContext.values()){
                if(type.isModded()){
                    ItemTransform transform = itemTransformResolver.apply(type, null);
                    if(transform != null)
                        moddedTransforms.put(type, transform);
                }
            }
            ItemTransforms itemTransforms = new ItemTransforms(
                itemTransformResolver.apply(ItemDisplayContext.THIRD_PERSON_LEFT_HAND, ItemTransform.NO_TRANSFORM),
                itemTransformResolver.apply(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, ItemTransform.NO_TRANSFORM),
                itemTransformResolver.apply(ItemDisplayContext.FIRST_PERSON_LEFT_HAND, ItemTransform.NO_TRANSFORM),
                itemTransformResolver.apply(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, ItemTransform.NO_TRANSFORM),
                itemTransformResolver.apply(ItemDisplayContext.HEAD, ItemTransform.NO_TRANSFORM),
                itemTransformResolver.apply(ItemDisplayContext.GUI, ItemTransform.NO_TRANSFORM),
                itemTransformResolver.apply(ItemDisplayContext.GROUND, ItemTransform.NO_TRANSFORM),
                itemTransformResolver.apply(ItemDisplayContext.FIXED, ItemTransform.NO_TRANSFORM),
                moddedTransforms.build()
            );
            // Create the item model
            parts.add(new BaseItemModel.Part(
                List.copyOf(processedQuads),
                conditions,
                guiLight,
                particleSprite,
                itemTransforms
            ));
            return ModelWalker.Result.endBranch();
        });

        // Create one composite model of all the sub-models
        return new BaseItemModel(parts, context.getTintSources(), propertyStore);
    }

    protected abstract BUILDER builder();

    @Override
    public T deserialize(JsonObject json) throws JsonParseException{
        BUILDER builder = this.builder();
        this.deserialize(json, builder);
        return builder.build();
    }

    protected void deserialize(JsonObject json, BUILDER builder){
        // Parent
        if(json.has("parent")){
            if(!json.get("parent").isJsonPrimitive() || !json.getAsJsonPrimitive("parent").isString())
                throw new JsonParseException("Property 'parent' must be a string!");
            String value = json.get("parent").getAsString();
            if(!IdentifierUtil.isValidIdentifier(value))
                throw new JsonParseException("Property 'parent' must be a valid identifier!");
            builder.parent(ResourceLocation.parse(value));
        }
        // Materials
        if(json.has("textures")){
            if(!json.get("textures").isJsonObject())
                throw new JsonParseException("Property 'textures' must be an object!");
            this.deserializeMaterials(json.getAsJsonObject("textures")).forEach(builder::material);
        }
        // Gui light
        if(json.has("gui_light")){
            if(!json.get("gui_light").isJsonPrimitive() || !json.getAsJsonPrimitive("gui_light").isString())
                throw new JsonParseException("Property 'gui_light' must be a string!");
            String value = json.get("gui_light").getAsString();
            try{
                builder.guiLight(UnbakedModel.GuiLight.getByName(value.toLowerCase(Locale.ROOT)));
            }catch(Exception e){
                String allowedValues = Arrays.stream(UnbakedModel.GuiLight.values()).map(o -> "'" + o.getSerializedName() + "'").collect(Collectors.joining(","));
                throw new JsonParseException("Property 'gui_light' must be one of " + allowedValues + ", not '" + value + "'!");
            }
        }
        // Ambient occlusion
        if(json.has("ambient_occlusion")){
            if(!json.get("ambient_occlusion").isJsonPrimitive() || !json.getAsJsonPrimitive("ambient_occlusion").isBoolean())
                throw new JsonParseException("Property 'ambient_occlusion' must be a boolean!");
            builder.ambientOcclusion(json.get("ambient_occlusion").getAsBoolean());
        }
        // Shade
        if(json.has("shade")){
            if(!json.get("shade").isJsonPrimitive() || !json.getAsJsonPrimitive("shade").isBoolean())
                throw new JsonParseException("Property 'shade' must be a boolean!");
            builder.shade(json.get("shade").getAsBoolean());
        }
        // Emissive
        if(json.has("emissive")){
            if(!json.get("emissive").isJsonPrimitive() || !json.getAsJsonPrimitive("emissive").isBoolean())
                throw new JsonParseException("Property 'emissive' must be a boolean!");
            builder.emissive(json.get("emissive").getAsBoolean());
        }
        // Elements
        if(json.has("elements")){
            if(!json.get("elements").isJsonArray())
                throw new JsonParseException("Property 'elements' must be an array!");
            for(JsonElement element : json.getAsJsonArray("elements")){
                if(!element.isJsonObject())
                    throw new JsonParseException("Array 'elements' must only contain objects!");
                builder.elements(this.deserializeElement(element.getAsJsonObject()).build());
            }
        }
        // Item transforms
        if(json.has("display")){
            if(!json.get("display").isJsonObject())
                throw new JsonParseException("Property 'display' must be an object!");
            JsonObject displayJson = json.getAsJsonObject("display");
            ItemTransform thirdPersonRightHand = this.deserializeItemTransform(displayJson, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND);
            ItemTransform thirdPersonLeftHand = this.deserializeItemTransform(displayJson, ItemDisplayContext.THIRD_PERSON_LEFT_HAND);
            if(thirdPersonLeftHand == ItemTransform.NO_TRANSFORM)
                thirdPersonLeftHand = thirdPersonRightHand;
            ItemTransform firstPersonRightHand = this.deserializeItemTransform(displayJson, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND);
            ItemTransform firstPersonLeftHand = this.deserializeItemTransform(displayJson, ItemDisplayContext.FIRST_PERSON_LEFT_HAND);
            if(firstPersonLeftHand == ItemTransform.NO_TRANSFORM)
                firstPersonLeftHand = firstPersonRightHand;
            ItemTransform head = this.deserializeItemTransform(displayJson, ItemDisplayContext.HEAD);
            ItemTransform gui = this.deserializeItemTransform(displayJson, ItemDisplayContext.GUI);
            ItemTransform ground = this.deserializeItemTransform(displayJson, ItemDisplayContext.GROUND);
            ItemTransform fixed = this.deserializeItemTransform(displayJson, ItemDisplayContext.FIXED);
            // Modded transforms
            ImmutableMap.Builder<ItemDisplayContext,ItemTransform> moddedTransforms = ImmutableMap.builder();
            for(ItemDisplayContext type : ItemDisplayContext.values()){
                if(!type.isModded())
                    continue;
                ItemTransform transform = this.deserializeItemTransform(displayJson, type);
                if(transform != ItemTransform.NO_TRANSFORM)
                    moddedTransforms.put(type, transform);
            }
            builder.itemTransforms(new ItemTransforms(
                thirdPersonLeftHand,
                thirdPersonRightHand,
                firstPersonLeftHand,
                firstPersonRightHand,
                head,
                gui,
                ground,
                fixed,
                moddedTransforms.build()
            ));
        }
    }

    @Override
    public JsonObject serialize(T data){
        JsonObject json = new JsonObject();
        if(data.getParent() != null)
            json.addProperty("parent", data.getParent().toString());
        if(!data.getMaterials().isEmpty())
            json.add("textures", this.serializeMaterials(data.getMaterials()));
        if(data.getGuiLight() != null)
            json.addProperty("gui_light", data.getGuiLight().getSerializedName());
        if(data.getAmbientOcclusion() != null)
            json.addProperty("ambient_occlusion", data.getAmbientOcclusion());
        if(data.getShade() != null)
            json.addProperty("shade", data.getShade());
        if(data.getEmissive() != null)
            json.addProperty("emissive", data.getEmissive());
        if(data.getGeometry() != null && !data.getGeometry().elements().isEmpty()){
            JsonArray elementsJson = new JsonArray();
            for(CuboidModelGeometry.Element element : data.getGeometry().elements())
                elementsJson.add(this.serializeElement(element));
            json.add("elements", elementsJson);
        }
        JsonObject itemTransformsJson = new JsonObject();
        for(ItemDisplayContext type : ItemDisplayContext.values()){
            ItemTransform transform = data.getItemTransform(type);
            if(transform != null)
                itemTransformsJson.add(type.getSerializedName(), this.serializeItemTransform(transform));
        }
        if(!itemTransformsJson.isEmpty())
            json.add("display", itemTransformsJson);
        return json;
    }

    protected Map<String,Either<String,ModelMaterial>> deserializeMaterials(JsonObject json){
        if(json.isEmpty())
            return Map.of();
        Map<String,Either<String,ModelMaterial>> materials = new HashMap<>();
        for(String key : json.keySet()){
            if(json.get(key).isJsonPrimitive() && json.getAsJsonPrimitive(key).isString()){
                String value = json.get(key).getAsString();
                if(!value.isEmpty() && value.charAt(0) == '#')
                    materials.put(key, Either.left(value.substring(1)));
                else if(IdentifierUtil.isValidIdentifier(value))
                    materials.put(key, Either.right(ModelMaterial.blockAtlas(ResourceLocation.parse(value))));
                else
                    throw new JsonParseException("Invalid identifier for texture key '" + key + "': '" + value + "'!");
            }
        }
        return materials;
    }

    protected JsonObject serializeMaterials(Map<String,Either<String,ModelMaterial>> materials){
        JsonObject materialsJson = new JsonObject();
        for(Map.Entry<String,Either<String,ModelMaterial>> entry : materials.entrySet()){
            if(entry.getValue().isLeft())
                materialsJson.addProperty(entry.getKey(), entry.getValue().left());
            else
                materialsJson.addProperty(entry.getKey(), entry.getValue().right().texture().toString());
        }
        return materialsJson;
    }

    protected CuboidModelGeometry.Element.Builder deserializeElement(JsonObject json){
        CuboidModelGeometry.Element.Builder builder = CuboidModelGeometry.Element.builder();
        // Vanilla properties
        if(!json.has("from"))
            throw new JsonParseException("Element must have property 'from'!");
        Vector3fc from = this.deserializeVector3f(json.get("from"), () -> "Element property 'from'");
        if(!json.has("to"))
            throw new JsonParseException("Element must have property 'to'!");
        Vector3fc to = this.deserializeVector3f(json.get("to"), () -> "Element property 'to'");
        builder.fromTo(from, to);
        if(json.has("rotation")){
            if(!json.get("rotation").isJsonObject())
                throw new JsonParseException("Element property 'rotation' must be an object!");
            builder.rotation(this.deserializeRotation(json.getAsJsonObject("rotation")));
        }
        if(!json.has("faces"))
            throw new JsonParseException("Element must have property 'faces'!");
        if(!json.get("faces").isJsonObject())
            throw new JsonParseException("Element property 'faces' must be an object!");
        JsonObject facesJson = json.getAsJsonObject("faces");
        for(String key : facesJson.keySet()){
            Direction side = Direction.byName(key.toLowerCase(Locale.ROOT));
            if(side == null){
                String allowedValues = Arrays.stream(Direction.values()).map(o -> "'" + o.getSerializedName() + "'").collect(Collectors.joining(","));
                throw new JsonParseException("Element 'faces' key must be one of " + allowedValues + ", not '" + key + "'!");
            }
            if(!facesJson.get(key).isJsonObject())
                throw new JsonParseException("Element 'faces' property '" + key + "' must be an object!");
            builder.face(side, this.deserializeFace(facesJson.getAsJsonObject(key)).build());
        }
        if(json.has("shade")){
            if(!json.get("shade").isJsonPrimitive() || !json.getAsJsonPrimitive("shade").isBoolean())
                throw new JsonParseException("Element property 'shade' must be a boolean!");
            builder.shade(json.get("shade").getAsBoolean());
        }
        if(json.has("light_emission")){
            if(!json.get("light_emission").isJsonPrimitive() || !json.getAsJsonPrimitive("light_emission").isNumber())
                throw new JsonParseException("Element property 'light_emission' must be a number!");
            int lightEmission = json.get("light_emission").getAsInt();
            if(lightEmission < 0 || lightEmission > 15)
                throw new JsonParseException("Element property 'light_emission' must be between 0 and 15, not '" + json.get("light_emission").getAsNumber() + "'!");
            builder.lightEmission(lightEmission);
        }
        // Fusion properties
        if(json.has("ambient_occlusion")){
            if(!json.get("ambient_occlusion").isJsonPrimitive() || !json.getAsJsonPrimitive("ambient_occlusion").isBoolean())
                throw new JsonParseException("Element property 'ambient_occlusion' must be a boolean!");
            builder.ambientOcclusion(json.get("ambient_occlusion").getAsBoolean());
        }
        if(json.has("emissive")){
            if(!json.get("emissive").isJsonPrimitive() || !json.getAsJsonPrimitive("emissive").isBoolean())
                throw new JsonParseException("Element property 'emissive' must be a boolean!");
            builder.emissive(json.get("emissive").getAsBoolean());
        }
        return builder;
    }

    protected JsonObject serializeElement(CuboidModelGeometry.Element element){
        JsonObject elementJson = new JsonObject();
        // Vanilla properties
        elementJson.add("from", this.serializeVector3f(element.from()));
        elementJson.add("to", this.serializeVector3f(element.to()));
        if(element.rotation() != null)
            elementJson.add("rotation", this.serializeRotation(element.rotation()));
        JsonObject facesJson = new JsonObject();
        for(Direction side : Direction.values()){
            CuboidModelGeometry.Face face = element.face(side);
            if(face != null)
                facesJson.add(side.getSerializedName(), this.serializeFace(face));
        }
        elementJson.add("faces", facesJson);
        if(element.shade() != null)
            elementJson.addProperty("shade", element.shade());
        if(element.lightEmission() != null)
            elementJson.addProperty("light_emission", element.lightEmission());
        // Fusion properties
        if(element.ambientOcclusion() != null)
            elementJson.addProperty("ambient_occlusion", element.ambientOcclusion());
        if(element.emissive() != null)
            elementJson.addProperty("emissive", element.emissive());
        return elementJson;
    }

    protected BlockElementRotation deserializeRotation(JsonObject json){
        if(!json.has("origin"))
            throw new JsonParseException("Element rotation must have array property 'origin'!");
        Vector3f origin = this.deserializeVector3f(json.get("origin"), () -> "Element rotation property 'origin'").div(16);
        if((json.has("axis") || json.has("angle"))
            && (json.has("x") || json.has("y") || json.has("z")))
            throw new JsonParseException("Element rotation must have either 'axis' and 'angle', or individual xyz-components, not both!");
        if(json.has("x") || json.has("y") || json.has("z"))
            throw new JsonParseException("Individual 'x', 'y', 'z' properties are not supported for rotation pre-1.21.10!");
        if(!json.has("axis") || !json.has("angle"))
            throw new JsonParseException("Element rotation must have 'axis' and 'angle' properties!");
        if(!json.get("axis").isJsonPrimitive() || !json.getAsJsonPrimitive("axis").isString())
            throw new JsonParseException("Element rotation property 'axis' must be a string!");
        Direction.Axis axis = Direction.Axis.byName(json.get("axis").getAsString().toLowerCase(Locale.ROOT));
        if(axis == null){
            String allowedValues = Arrays.stream(Direction.Axis.values()).map(o -> "'" + o.getSerializedName() + "'").collect(Collectors.joining(","));
            throw new JsonParseException("Element rotation property 'axis' must be one of " + allowedValues + ", not '" + json.get("axis").getAsString() + "'!");
        }
        if(!json.get("angle").isJsonPrimitive() || !json.getAsJsonPrimitive("angle").isNumber())
            throw new JsonParseException("Element rotation property 'angle' must be a number!");
        float angle = json.get("angle").getAsFloat();
        boolean rescale = false;
        if(json.has("rescale")){
            if(!json.get("rescale").isJsonPrimitive() || !json.getAsJsonPrimitive("rescale").isBoolean())
                throw new JsonParseException("Element rotation property 'rescale' must be a boolean!");
            rescale = json.get("rescale").getAsBoolean();
        }
        return new BlockElementRotation(origin, axis, angle, rescale);
    }

    protected JsonObject serializeRotation(BlockElementRotation rotation){
        JsonObject rotationJson = new JsonObject();
        rotationJson.add("origin", this.serializeVector3f(new Vector3f(rotation.origin()).mul(16)));
        rotationJson.addProperty("axis", rotation.axis().name().toLowerCase(Locale.ROOT));
        rotationJson.addProperty("angle", rotation.angle());
        if(rotation.rescale())
            rotationJson.addProperty("rescale", true);
        return rotationJson;
    }

    protected CuboidModelGeometry.Face.Builder deserializeFace(JsonObject json){
        CuboidModelGeometry.Face.Builder builder = CuboidModelGeometry.Face.builder();
        // Vanilla properties
        if(json.has("texture")){
            if(!json.get("texture").isJsonPrimitive() || !json.getAsJsonPrimitive("texture").isString())
                throw new JsonParseException("Element face property 'texture' must be a string!");
            String texture = json.get("texture").getAsString();
            if(!texture.isEmpty() && texture.charAt(0) == '#')
                texture = texture.substring(1);
            if(texture.isEmpty())
                throw new JsonParseException("Element face property 'texture' must not be empty!");
            builder.material(texture);
        }
        if(json.has("rotation")){
            if(!json.get("rotation").isJsonPrimitive() || !json.getAsJsonPrimitive("rotation").isNumber())
                throw new JsonParseException("Element face property 'rotation' must be a number!");
            int rotation = json.get("rotation").getAsInt();
            if(rotation % 90 != 0)
                throw new JsonParseException("Element face property 'rotation' must be a multiple of 90 degrees, not '" + rotation + "'!");
            builder.rotation(CuboidModelGeometry.Face.Rotation.byAngle(rotation));
        }
        if(json.has("cullface")){
            if(!json.get("cullface").isJsonPrimitive() || !json.getAsJsonPrimitive("cullface").isString())
                throw new JsonParseException("Element face property 'cullface' must be a string!");
            Direction cullface = Direction.byName(json.get("cullface").getAsString().toLowerCase(Locale.ROOT));
            if(cullface == null){
                String allowedValues = Arrays.stream(Direction.values()).map(o -> "'" + o.getSerializedName() + "'").collect(Collectors.joining(","));
                throw new JsonParseException("Element face property 'cullface' must be one of " + allowedValues + ", not '" + json.get("cullface").getAsString() + "'!");
            }
            builder.cullDirection(cullface);
        }
        if(json.has("tintindex")){
            if(!json.get("tintindex").isJsonPrimitive() || !json.getAsJsonPrimitive("tintindex").isNumber())
                throw new JsonParseException("Element face property 'tintindex' must be a number!");
            builder.tintIndex(json.get("tintindex").getAsInt());
        }
        // Fusion properties
        if(json.has("shade")){
            if(!json.get("shade").isJsonPrimitive() || !json.getAsJsonPrimitive("shade").isBoolean())
                throw new JsonParseException("Element face property 'shade' must be a boolean!");
            builder.shade(json.get("shade").getAsBoolean());
        }
        if(json.has("light_emission")){
            if(!json.get("light_emission").isJsonPrimitive() || !json.getAsJsonPrimitive("light_emission").isNumber())
                throw new JsonParseException("Element face property 'light_emission' must be a number!");
            int lightEmission = json.get("light_emission").getAsInt();
            if(lightEmission < 0 || lightEmission > 15)
                throw new JsonParseException("Element face property 'light_emission' must be between 0 and 15, not '" + json.get("light_emission").getAsNumber() + "'!");
            builder.lightEmission(lightEmission);
        }
        if(json.has("ambient_occlusion")){
            if(!json.get("ambient_occlusion").isJsonPrimitive() || !json.getAsJsonPrimitive("ambient_occlusion").isBoolean())
                throw new JsonParseException("Element face property 'ambient_occlusion' must be a boolean!");
            builder.ambientOcclusion(json.get("ambient_occlusion").getAsBoolean());
        }
        if(json.has("emissive")){
            if(!json.get("emissive").isJsonPrimitive() || !json.getAsJsonPrimitive("emissive").isBoolean())
                throw new JsonParseException("Element face property 'emissive' must be a boolean!");
            builder.emissive(json.get("emissive").getAsBoolean());
        }
        return builder;
    }

    protected JsonObject serializeFace(CuboidModelGeometry.Face face){
        JsonObject faceJson = new JsonObject();
        // Vanilla properties
        String material = face.material();
        if(material.isEmpty() || material.charAt(0) != '#')
            material = '#' + material;
        faceJson.addProperty("texture", material);
        CuboidModelGeometry.Face.UV uv = face.uv();
        if(uv != null){
            JsonArray uvJson = new JsonArray(4);
            uvJson.add(uv.minU());
            uvJson.add(uv.minV());
            uvJson.add(uv.maxU());
            uvJson.add(uv.maxV());
        }
        if(face.rotation() != null)
            faceJson.addProperty("rotation", face.rotation().angle());
        if(face.cullDirection() != null)
            faceJson.addProperty("cullface", face.cullDirection().getSerializedName());
        if(face.tintIndex() != null)
            faceJson.addProperty("tintindex", face.tintIndex());
        // Fusion properties
        if(face.shade() != null)
            faceJson.addProperty("shade", face.shade());
        if(face.lightEmission() != null)
            faceJson.addProperty("light_emission", face.lightEmission());
        if(face.ambientOcclusion() != null)
            faceJson.addProperty("ambient_occlusion", face.ambientOcclusion());
        if(face.emissive() != null)
            faceJson.addProperty("emissive", face.emissive());
        return faceJson;
    }

    protected ItemTransform deserializeItemTransform(JsonObject json, ItemDisplayContext type){
        if(!json.has(type.getSerializedName()))
            return ItemTransform.NO_TRANSFORM;
        if(!json.get(type.getSerializedName()).isJsonObject())
            throw new JsonParseException("Display entry '" + type.getSerializedName() + "' must be an object!");
        JsonObject object = json.getAsJsonObject(type.getSerializedName());
        return new ItemTransform(
            this.deserializeVector3f(object.get("rotation"), () -> "Display '" + type.getSerializedName() + "' property 'rotation'"),
            this.deserializeVector3f(object.get("translation"), () -> "Display '" + type.getSerializedName() + "' property 'translation'"),
            this.deserializeVector3f(object.get("scale"), () -> "Display '" + type.getSerializedName() + "' property 'scale'")
        );
    }

    protected JsonObject serializeItemTransform(ItemTransform transform){
        JsonObject transformJson = new JsonObject();
        transformJson.add("translation", this.serializeVector3f(transform.translation));
        transformJson.add("scale", this.serializeVector3f(transform.scale));
        transformJson.add("rotation", this.serializeVector3f(transform.rotation));
        return transformJson;
    }

    protected Vector3f deserializeVector3f(JsonElement element, Supplier<String> errorMessageHead){
        if(!element.isJsonArray())
            throw new JsonParseException(errorMessageHead + " must be an array!");
        JsonArray array = element.getAsJsonArray();
        if(array.size() != 3)
            throw new JsonParseException(errorMessageHead + " must contain exactly 3 elements!");
        float[] values = new float[3];
        for(int i = 0; i < 3; i++){
            JsonElement value = array.get(i);
            if(!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber())
                throw new JsonParseException(errorMessageHead + " must only contain numbers!");
            values[i] = value.getAsFloat();
        }
        return new Vector3f(values[0], values[1], values[2]);
    }

    protected JsonArray serializeVector3f(Vector3fc position){
        JsonArray array = new JsonArray();
        array.add(position.x());
        array.add(position.y());
        array.add(position.z());
        return array;
    }
}
