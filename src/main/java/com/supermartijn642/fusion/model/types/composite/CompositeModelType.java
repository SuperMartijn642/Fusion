package com.supermartijn642.fusion.model.types.composite;

import com.google.common.collect.ImmutableList;
import com.google.gson.*;
import com.supermartijn642.fusion.api.model.DefaultModelTypes;
import com.supermartijn642.fusion.api.model.FusionModelTypeRegistry;
import com.supermartijn642.fusion.api.model.ModelInstance;
import com.supermartijn642.fusion.api.model.ModelType;
import com.supermartijn642.fusion.api.model.custom.*;
import com.supermartijn642.fusion.api.model.custom.geometry.CuboidModelGeometry;
import com.supermartijn642.fusion.api.model.custom.geometry.ModelGeometry;
import com.supermartijn642.fusion.api.model.predicates.DefaultModelPredicates;
import com.supermartijn642.fusion.api.model.predicates.FusionModelPredicateRegistry;
import com.supermartijn642.fusion.api.model.predicates.ModelPredicate;
import com.supermartijn642.fusion.api.model.types.base.BaseModelData;
import com.supermartijn642.fusion.api.model.types.composite.CompositeModelData;
import com.supermartijn642.fusion.api.util.Either;
import com.supermartijn642.fusion.api.util.Property;
import com.supermartijn642.fusion.model.ModelTypeRegistryImpl;
import com.supermartijn642.fusion.util.IdentifierUtil;
import com.supermartijn642.fusion.util.MatrixUtil;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.model.ItemOverride;
import net.minecraft.client.renderer.model.ItemTransformVec3f;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import javax.vecmath.Matrix4f;
import java.util.*;

/**
 * Created 15/06/2026 by SuperMartijn642
 */
public class CompositeModelType implements ModelType<CompositeModelData> {

    @Override
    public Collection<ResourceLocation> getDependencies(CompositeModelData data){
        List<ResourceLocation> dependencies = new ArrayList<>();
        for(List<CompositeModelData.ModelEntry> series : data.getModels()){
            for(CompositeModelData.ModelEntry modelEntry : series){
                dependencies.addAll(modelEntry.getModel().flatMap(
                    Collections::singletonList,
                    ModelInstance::getDependencies
                ));
            }
        }
        return dependencies;
    }

    @Override
    public List<Either<ResourceLocation,UntypedModelInstance>> getParents(CompositeModelData data){
        List<Either<ResourceLocation,UntypedModelInstance>> parents = new ArrayList<>();
        for(List<CompositeModelData.ModelEntry> series : data.getModels()){
            ModelPredicate composedConditions = DefaultModelPredicates.always();
            for(CompositeModelData.ModelEntry modelEntry : series){
                ModelPredicate condition = modelEntry.getCondition();
                parents.add(Either.right(new ModelEntryModelInstance(
                    modelEntry.getModel(),
                    modelEntry.getTransform(),
                    DefaultModelPredicates.and(composedConditions, condition)
                )));
                if(condition == null)
                    composedConditions = DefaultModelPredicates.never();
                else{
                    composedConditions = DefaultModelPredicates.and(
                        composedConditions,
                        DefaultModelPredicates.not(condition)
                    );
                }
            }
        }
        return parents;
    }

    @Override
    public @Nullable Boolean getAmbientOcclusion(CompositeModelData data){
        return data.getAmbientOcclusion();
    }

    @Override
    public @Nullable Boolean getIsGui3d(CompositeModelData data){
        return data.getIsGui3d();
    }

    @Override
    public @Nullable ItemTransformVec3f getItemTransform(ItemCameraTransforms.TransformType type, CompositeModelData data){
        return data.getItemTransform(type);
    }

    @Override
    public List<ItemOverride> getItemOverrides(CompositeModelData data){
        return data.getItemOverrides();
    }

    @Override
    public Map<String,Either<String,ModelMaterial>> getMaterials(CompositeModelData data){
        return data.getMaterials();
    }

    @Override
    public @Nullable ModelGeometry getGeometry(CompositeModelData data){
        return CuboidModelGeometry.of(Collections.emptyList());
    }

    @Override
    public @Nullable Boolean getShade(CompositeModelData data){
        return data.getShade();
    }

    @Override
    public @Nullable Boolean getEmissive(CompositeModelData data){
        return data.getEmissive();
    }

    @Override
    public <X, C> Optional<X> getProperty(Property<X,C> property, C context, CompositeModelData data){
        return Optional.empty();
    }

    @Override
    public @Nullable IBakedModel bakeModel(ModelBakingContext context, ModelStack modelStack, CompositeModelData data){
        // Bake model entries
        ModelTransform predicateTransform = ModelTransform.compose(context.getTransformation(), modelStack.composeTransforms());
        List<CompositeBakedModel.ConditionalList> lists = new ArrayList<>();
        IBakedModel defaultModel = null;
        for(List<CompositeModelData.ModelEntry> series : data.getModels()){
            if(series.isEmpty())
                continue;
            List<CompositeBakedModel.ModelEntry> list = new ArrayList<>(series.size());
            for(CompositeModelData.ModelEntry entry : series){
                // Bake model
                ModelEntryModelInstance model = new ModelEntryModelInstance(entry.getModel(), entry.getTransform(), null);
                IBakedModel baked = model.bakeModel(context, modelStack.push(model));
                // Simply conditions
                ModelPredicate conditions = entry.getCondition();
                if(conditions != null)
                    conditions = conditions.applyTransform(predicateTransform).simplify();
                // Add entry
                list.add(new CompositeBakedModel.ModelEntry(baked, conditions));
                // If condition is always true, ignore any following entries
                if(conditions == null || conditions.alwaysTrue()){
                    if(defaultModel == null)
                        defaultModel = baked;
                    break;
                }
            }
            lists.add(new CompositeBakedModel.ConditionalList(ImmutableList.copyOf(list)));
        }

        // Create model
        if(defaultModel == null)
            defaultModel = context.getMissingBakedModel();
        return new CompositeBakedModel(defaultModel, ImmutableList.copyOf(lists));
    }

    @Override
    public CompositeModelData deserialize(JsonObject json) throws JsonParseException{
        CompositeModelData.Builder builder = CompositeModelData.builder();

        // Use base model type to deserialize base properties
        BaseModelData baseModelData = DefaultModelTypes.BASE.deserialize(json);
        builder.isGui3d(baseModelData.getIsGui3d())
            .ambientOcclusion(baseModelData.getAmbientOcclusion())
            .shade(baseModelData.getShade())
            .emissive(baseModelData.getEmissive());
        baseModelData.getMaterials().forEach(builder::material);
        for(ItemCameraTransforms.TransformType type : ItemCameraTransforms.TransformType.values())
            builder.itemTransform(type, baseModelData.getItemTransform(type));

        // Model entries
        if(!json.has("models"))
            throw new JsonParseException("Missing 'models' property!");
        if(!json.get("models").isJsonArray())
            throw new JsonParseException("Property 'models' must be an array!");
        boolean empty = true;
        for(JsonElement element : json.getAsJsonArray("models")){
            try{
                List<CompositeModelData.ModelEntry> series = deserializeModelSeries(element);
                if(series != null){
                    empty = false;
                    builder.modelSeries(series);
                }
            }catch(JsonParseException e){
                throw new JsonParseException("Failed to deserialize 'models' entry", e);
            }
        }
        if(empty)
            throw new JsonParseException("Array property 'models' must not be empty!");
        return builder.build();
    }

    @Override
    public JsonObject serialize(CompositeModelData data){
        // Use base model data to serialize base properties
        BaseModelData.Builder<?,BaseModelData> baseModelData = BaseModelData.builder();
        baseModelData.isGui3d(data.getIsGui3d())
            .ambientOcclusion(data.getAmbientOcclusion())
            .shade(data.getShade())
            .emissive(data.getEmissive());
        data.getMaterials().forEach(baseModelData::material);
        for(ItemCameraTransforms.TransformType type : ItemCameraTransforms.TransformType.values())
            baseModelData.itemTransform(type, data.getItemTransform(type));
        JsonObject json = DefaultModelTypes.BASE.serialize(baseModelData.build());

        // Model entries
        JsonArray array = new JsonArray();
        for(List<CompositeModelData.ModelEntry> series : data.getModels())
            array.add(serializeModelSeries(series));
        json.add("models", array);
        return json;
    }

    private static List<CompositeModelData.ModelEntry> deserializeModelSeries(JsonElement element){
        // Single entry
        if(element.isJsonObject() || element.isJsonPrimitive() && element.getAsJsonPrimitive().isString())
            return ImmutableList.of(deserializeModelEntry(element));
        // Array
        if(!element.isJsonArray())
            throw new JsonParseException("Entry must be an object, string, or array!");
        JsonArray array = element.getAsJsonArray();
        List<CompositeModelData.ModelEntry> series = new ArrayList<>(array.size());
        for(JsonElement entry : array){
            try{
                series.add(deserializeModelEntry(entry));
            }catch(JsonParseException e){
                throw new JsonParseException("Failed to deserialize an entry in model series", e);
            }
        }
        return series.isEmpty() ? null : ImmutableList.copyOf(series);
    }

    private static JsonElement serializeModelSeries(List<CompositeModelData.ModelEntry> series){
        if(series.size() == 1)
            return serializeModelEntry(series.get(0));
        JsonArray array = new JsonArray();
        for(CompositeModelData.ModelEntry entry : series)
            array.add(serializeModelEntry(entry));
        return array;
    }

    private static CompositeModelData.ModelEntry deserializeModelEntry(JsonElement element){
        // Simple string
        if(element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()){
            if(!IdentifierUtil.isValidIdentifier(element.getAsString()))
                throw new JsonParseException("Entry must be a valid identifier, not '" + element.getAsString() + "'!");
            ResourceLocation identifier = new ResourceLocation(element.getAsString());
            return CompositeModelData.ModelEntry.of(identifier, ModelTransform.identity(), null);
        }
        if(!element.isJsonObject())
            throw new JsonParseException("Entry must be an object or string!");
        JsonObject json = element.getAsJsonObject();

        // Model
        if(!json.has("model"))
            throw new JsonParseException("Entry must have property 'model'!");
        Either<ResourceLocation,ModelInstance<?>> model;
        if(json.get("model").isJsonPrimitive() && json.getAsJsonPrimitive("model").isString()){
            if(!IdentifierUtil.isValidIdentifier(json.get("model").getAsString()))
                throw new JsonParseException("Entry property 'model' must be a valid identifier, not '" + json.get("model").getAsString() + "'!");
            model = Either.left(new ResourceLocation(json.get("model").getAsString()));
        }else if(json.get("model").isJsonObject()){
            try{
                model = Either.right(ModelTypeRegistryImpl.deserializeModelData(json.getAsJsonObject("model")));
            }catch(JsonParseException e){
                throw new JsonParseException("Failed to deserialize property 'model'", e);
            }
        }else
            throw new JsonParseException("Entry property 'model' must be a string or object!");
        // Transform
        ModelTransform transform = ModelTransform.identity();
        if(json.has("transform")){
            if(!json.get("transform").isJsonArray())
                throw new JsonParseException("Entry property 'transform' must be an array!");
            try{
                transform = deserializeModelTransform(json.getAsJsonArray("transform"));
            }catch(JsonParseException e){
                throw new JsonParseException("Failed to deserialize entry property 'transform'", e);
            }
        }
        // Condition
        ModelPredicate condition = null;
        if(json.has("condition")){
            if(!json.get("condition").isJsonObject())
                throw new JsonParseException("Entry property 'condition' must be an object!");
            try{
                condition = FusionModelPredicateRegistry.deserializeModelPredicate(json.getAsJsonObject("condition"));
            }catch(JsonParseException e){
                throw new JsonParseException("Failed to deserialize entry property 'condition'", e);
            }
        }
        return model.isLeft() ?
            CompositeModelData.ModelEntry.of(model.left(), transform, condition) :
            CompositeModelData.ModelEntry.of(model.right(), transform, condition);
    }

    private static JsonElement serializeModelEntry(CompositeModelData.ModelEntry entry){
        // Simple string
        if(entry.getModel().isLeft() && entry.getTransform().equals(ModelTransform.identity()) && entry.getCondition() == null)
            return new JsonPrimitive(entry.getModel().left().toString());

        JsonObject json = new JsonObject();
        if(entry.getModel().isLeft())
            json.addProperty("model", entry.getModel().left().toString());
        else
            json.add("model", FusionModelTypeRegistry.serializeModelData(entry.getModel().right()));
        if(!entry.getTransform().equals(ModelTransform.identity()))
            json.add("transform", serializeModelTransform(entry.getTransform()));
        if(entry.getCondition() != null)
            json.add("condition", FusionModelPredicateRegistry.serializeModelPredicate(entry.getCondition()));
        return json;
    }

    private static ModelTransform deserializeModelTransform(JsonArray array){
        Matrix4f matrix = new Matrix4f();
        matrix.setIdentity();
        for(JsonElement entry : array){
            if(!entry.isJsonObject())
                throw new JsonParseException("Transform entry must be an object!");
            matrix.mul(deserializeModelTransformEntry(entry.getAsJsonObject()));
        }
        return ModelTransform.of(matrix, false);
    }

    private static JsonElement serializeModelTransform(ModelTransform transform){
        // TODO implement model transform serialization
        return new JsonArray();
    }

    private static Matrix4f deserializeModelTransformEntry(JsonObject json){
        if(!json.has("type"))
            throw new JsonParseException("Transform entry must have property 'type'!");
        if(!json.get("type").isJsonPrimitive() && json.getAsJsonPrimitive("type").isString())
            throw new JsonParseException("Transform entry property 'type' must be a string!");
        String type = json.getAsJsonPrimitive("type").getAsString();

        if(type.equals("translate")){
            if(!json.has("translation"))
                throw new JsonParseException("Translation transform entry must have property 'translation'!");
            if(!json.get("translation").isJsonArray() || json.getAsJsonArray("translation").size() != 3)
                throw new JsonParseException("Translation transform entry property 'translation' must be an array of 3 numbers!");
            float[] translation = new float[3];
            JsonArray array = json.getAsJsonArray("translation");
            for(int i = 0; i < array.size(); i++){
                JsonElement element = array.get(i);
                if(!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber())
                    throw new JsonParseException("Translation transform entry property 'translation' must be an array of 3 numbers!");
                translation[i] = element.getAsFloat();
            }
            return MatrixUtil.createTranslationMatrix(translation[0] / 16, translation[1] / 16, translation[2] / 16);
        }

        if(type.equals("scale")){
            if(!json.has("scaling"))
                throw new JsonParseException("Scaling transform entry must have property 'scaling'!");
            float[] scaling = new float[3];
            if(json.get("scaling").isJsonPrimitive() && json.getAsJsonPrimitive("scaling").isNumber())
                scaling[0] = scaling[1] = scaling[2] = json.get("scaling").getAsFloat();
            else if(json.get("scaling").isJsonArray()){
                if(json.getAsJsonArray("scaling").size() != 3)
                    throw new JsonParseException("Scaling transform entry property 'scaling' must be an array of 3 numbers!");
                JsonArray array = json.getAsJsonArray("scaling");
                for(int i = 0; i < array.size(); i++){
                    JsonElement element = array.get(i);
                    if(!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber())
                        throw new JsonParseException("Scaling transform entry property 'scaling' must be an array of 3 numbers!");
                    scaling[i] = element.getAsFloat();
                }
            }else
                throw new JsonParseException("Scaling transform entry property 'scaling' must be a number or array of 3 numbers!");
            float[] origin = {0.5f, 0.5f, 0.5f};
            if(json.has("origin")){
                if(json.getAsJsonArray("origin").size() != 3)
                    throw new JsonParseException("Scaling transform entry property 'origin' must be an array of 3 numbers!");
                origin = new float[3];
                JsonArray array = json.getAsJsonArray("origin");
                for(int i = 0; i < array.size(); i++){
                    JsonElement element = array.get(i);
                    if(!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber())
                        throw new JsonParseException("Scaling transform entry property 'origin' must be an array of 3 numbers!");
                    origin[i] = element.getAsFloat() / 16;
                }
            }
            Matrix4f matrix = MatrixUtil.createTranslationMatrix(-origin[0], -origin[1], -origin[2]);
            matrix.mul(MatrixUtil.createScalingMatrix(scaling[0], scaling[1], scaling[2]));
            matrix.mul(MatrixUtil.createTranslationMatrix(origin[0] * scaling[0], origin[1] * scaling[1], origin[2] * scaling[2]));
            return matrix;
        }

        if(type.equals("rotate")){
            if(!json.has("rotation") && !json.has("axis") && !json.has("angle"))
                throw new JsonParseException("Rotation transform entry must have property 'rotation', or 'axis' and 'angle'!");
            if(json.has("rotation") && (json.has("axis") || json.has("angle")))
                throw new JsonParseException("Rotation transform entry must have property 'rotation', or 'axis' and 'angle', not both!");
            Matrix4f matrix;
            if(json.has("rotation")){
                if(!json.get("rotation").isJsonArray() || json.getAsJsonArray("rotation").size() != 3)
                    throw new JsonParseException("Rotation transform entry property 'rotation' must be an array of 3 numbers!");
                float[] rotation = new float[3];
                JsonArray array = json.getAsJsonArray("rotation");
                for(int i = 0; i < array.size(); i++){
                    JsonElement element = array.get(i);
                    if(!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber())
                        throw new JsonParseException("Rotation transform entry property 'rotation' must be an array of 3 numbers!");
                    rotation[i] = element.getAsFloat();
                }
                matrix = MatrixUtil.createZYXRotationMatrix((float)(rotation[0] / 180 * Math.PI), (float)(rotation[1] / 180 * Math.PI), (float)(rotation[2] / 180 * Math.PI));
            }else{
                if(!json.has("axis") || !json.has("angle"))
                    throw new JsonParseException("Rotation transform entry must have both properties 'axis' and 'angle'!");
                if(!json.get("angle").isJsonPrimitive() || !json.getAsJsonPrimitive("angle").isNumber())
                    throw new JsonParseException("Rotation transform entry property 'angle' must be a number!");
                float angle = (float)(json.get("angle").getAsFloat() / 180 * Math.PI);
                if(!json.get("axis").isJsonPrimitive() || !json.getAsJsonPrimitive("axis").isString())
                    throw new JsonParseException("Rotation transform entry property 'axis' must be a string!");
                String axis = json.get("axis").getAsString();
                switch(axis){
                    case "x":
                        matrix = MatrixUtil.createXRotationMatrix(angle);
                        break;
                    case "y":
                        matrix = MatrixUtil.createYRotationMatrix(angle);
                        break;
                    case "z":
                        matrix = MatrixUtil.createZRotationMatrix(angle);
                        break;
                    default:
                        throw new JsonParseException("Rotation transform entry property 'axis' must be one of 'x', 'y', or 'z', not '" + axis + "'!");
                }
            }
            float[] origin = {0.5f, 0.5f, 0.5f};
            if(json.has("origin")){
                if(json.getAsJsonArray("origin").size() != 3)
                    throw new JsonParseException("Rotation transform entry property 'origin' must be an array of 3 numbers!");
                origin = new float[3];
                JsonArray array = json.getAsJsonArray("origin");
                for(int i = 0; i < array.size(); i++){
                    JsonElement element = array.get(i);
                    if(!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber())
                        throw new JsonParseException("Rotation transform entry property 'origin' must be an array of 3 numbers!");
                    origin[i] = element.getAsFloat() / 16;
                }
            }
            Matrix4f m = MatrixUtil.createTranslationMatrix(-origin[0], -origin[1], -origin[2]);
            m.mul(matrix);
            m.mul(MatrixUtil.createTranslationMatrix(origin[0], origin[1], origin[2]));
            return m;
        }

        throw new JsonParseException("Transform entry property 'type' must be one of 'translate', 'scale', or 'rotate', not '" + type + "'!");
    }
}
