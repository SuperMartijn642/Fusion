package com.supermartijn642.fusion.model.types.composite;

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
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

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
                    List::of,
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
    public UnbakedModel.@Nullable GuiLight getGuiLight(CompositeModelData data){
        return data.getGuiLight();
    }

    @Override
    public @Nullable ItemTransform getItemTransform(ItemDisplayContext type, CompositeModelData data){
        return data.getItemTransform(type);
    }

    @Override
    public Map<String,Either<String,ModelMaterial>> getMaterials(CompositeModelData data){
        return data.getMaterials();
    }

    @Override
    public @Nullable ModelGeometry getGeometry(CompositeModelData data){
        return CuboidModelGeometry.of(List.of());
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
    public @Nullable BakedModel bakeBlockStateModel(BlockStateModelBakingContext context, ModelStack modelStack, CompositeModelData data){
        // Bake model entries
        List<CompositeBlockStateModel.ConditionalList> lists = new ArrayList<>();
        BakedModel defaultModel = null;
        for(List<CompositeModelData.ModelEntry> series : data.getModels()){
            if(series.isEmpty())
                continue;
            List<CompositeBlockStateModel.ModelEntry> list = new ArrayList<>(series.size());
            BakedModel itemModel = null;
            for(CompositeModelData.ModelEntry entry : series){
                // Bake model
                ModelEntryModelInstance model = new ModelEntryModelInstance(entry.getModel(), entry.getTransform(), null);
                BakedModel baked = model.bakeBlockStateModel(context, modelStack.push(model));
                // Simply conditions
                ModelPredicate conditions = entry.getCondition();
                if(conditions != null)
                    conditions = conditions.simplify();
                // Check item condition
                if(itemModel == null && (conditions == null || conditions.testForItem(ItemStack.EMPTY)))
                    itemModel = baked;
                // Add entry
                CompositeBlockStateModel.ModelEntry modelEntry = new CompositeBlockStateModel.ModelEntry(baked, conditions);
                list.add(modelEntry);
                // If condition is always true, ignore any following entries
                if(conditions == null || conditions.alwaysTrue()){
                    if(defaultModel == null)
                        defaultModel = baked;
                    break;
                }
            }
            lists.add(new CompositeBlockStateModel.ConditionalList(List.copyOf(list), itemModel));
        }

        // Create model
        if(defaultModel == null)
            defaultModel = context.getMissingBakedModel();
        return new CompositeBlockStateModel(defaultModel, List.copyOf(lists));
    }

    @Override
    public @Nullable ItemModel bakeItemModel(ItemModelBakingContext context, ModelStack modelStack, CompositeModelData data){
        // Bake model entries
        List<CompositeItemModel.ConditionalList> lists = new ArrayList<>();
        for(List<CompositeModelData.ModelEntry> series : data.getModels()){
            if(series.isEmpty())
                continue;
            List<CompositeItemModel.ModelEntry> list = new ArrayList<>(series.size());
            for(CompositeModelData.ModelEntry entry : series){
                // Bake model
                ModelEntryModelInstance model = new ModelEntryModelInstance(entry.getModel(), entry.getTransform(), null);
                ItemModel baked = model.bakeItemModel(context, modelStack.push(model));
                // Simply conditions
                ModelPredicate conditions = entry.getCondition();
                if(conditions != null)
                    conditions = conditions.simplify();
                // Add entry
                list.add(new CompositeItemModel.ModelEntry(baked, conditions));
                // If condition is always true, ignore any following entries
                if(conditions == null || conditions.alwaysTrue())
                    break;
            }
            lists.add(new CompositeItemModel.ConditionalList(List.copyOf(list)));
        }

        // Create model
        return new CompositeItemModel(List.copyOf(lists));
    }

    @Override
    public CompositeModelData deserialize(JsonObject json) throws JsonParseException{
        CompositeModelData.Builder builder = CompositeModelData.builder();

        // Use base model type to deserialize base properties
        BaseModelData baseModelData = DefaultModelTypes.BASE.deserialize(json);
        builder.guiLight(baseModelData.getGuiLight())
            .ambientOcclusion(baseModelData.getAmbientOcclusion())
            .shade(baseModelData.getShade())
            .emissive(baseModelData.getEmissive());
        baseModelData.getMaterials().forEach(builder::material);
        for(ItemDisplayContext type : ItemDisplayContext.values())
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
        baseModelData.guiLight(data.getGuiLight())
            .ambientOcclusion(data.getAmbientOcclusion())
            .shade(data.getShade())
            .emissive(data.getEmissive());
        data.getMaterials().forEach(baseModelData::material);
        for(ItemDisplayContext type : ItemDisplayContext.values())
            baseModelData.itemTransform(type, data.getItemTransform(type));
        JsonObject json = DefaultModelTypes.BASE.serialize(baseModelData.build());

        // Model entries
        JsonArray array = new JsonArray(data.getModels().size());
        for(List<CompositeModelData.ModelEntry> series : data.getModels())
            array.add(serializeModelSeries(series));
        json.add("models", array);
        return json;
    }

    private static List<CompositeModelData.ModelEntry> deserializeModelSeries(JsonElement element){
        // Single entry
        if(element.isJsonObject() || element.isJsonPrimitive() && element.getAsJsonPrimitive().isString())
            return List.of(deserializeModelEntry(element));
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
        return series.isEmpty() ? null : List.copyOf(series);
    }

    private static JsonElement serializeModelSeries(List<CompositeModelData.ModelEntry> series){
        if(series.size() == 1)
            return serializeModelEntry(series.get(0));
        JsonArray array = new JsonArray(series.size());
        for(CompositeModelData.ModelEntry entry : series)
            array.add(serializeModelEntry(entry));
        return array;
    }

    private static CompositeModelData.ModelEntry deserializeModelEntry(JsonElement element){
        // Simple string
        if(element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()){
            if(!IdentifierUtil.isValidIdentifier(element.getAsString()))
                throw new JsonParseException("Entry must be a valid identifier, not '" + element.getAsString() + "'!");
            ResourceLocation identifier = ResourceLocation.parse(element.getAsString());
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
            model = Either.left(ResourceLocation.parse(json.get("model").getAsString()));
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
        Matrix4f matrix = new Matrix4f().identity();
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

    private static Matrix4fc deserializeModelTransformEntry(JsonObject json){
        if(!json.has("type"))
            throw new JsonParseException("Transform entry must have property 'type'!");
        if(!json.get("type").isJsonPrimitive() && json.getAsJsonPrimitive("type").isString())
            throw new JsonParseException("Transform entry property 'type' must be a string!");
        String type = json.getAsJsonPrimitive("type").getAsString();

        switch(type){
            case "translate" -> {
                if(!json.has("translation"))
                    throw new JsonParseException("Translation transform entry must have property 'translation'!");
                if(!json.get("translation").isJsonArray() || json.getAsJsonArray("translation").size() != 3)
                    throw new JsonParseException("Translation transform entry property 'translation' must be an array of 3 numbers!");
                for(JsonElement element : json.getAsJsonArray("translation")){
                    if(!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber())
                        throw new JsonParseException("Translation transform entry property 'translation' must be an array of 3 numbers!");
                }
                double[] translation = json.getAsJsonArray("translation").asList().stream().map(JsonElement::getAsNumber).mapToDouble(Number::doubleValue).toArray();
                return new Matrix4f().identity().translate((float)translation[0] / 16, (float)translation[1] / 16, (float)translation[2] / 16);
            }

            case "scale" -> {
                if(!json.has("scaling"))
                    throw new JsonParseException("Scaling transform entry must have property 'scaling'!");
                double[] scaling = new double[3];
                if(json.get("scaling").isJsonPrimitive() && json.getAsJsonPrimitive("scaling").isNumber())
                    scaling[0] = scaling[1] = scaling[2] = json.get("scaling").getAsDouble();
                else if(json.get("scaling").isJsonArray()){
                    if(json.getAsJsonArray("scaling").size() != 3)
                        throw new JsonParseException("Scaling transform entry property 'scaling' must be an array of 3 numbers!");
                    for(JsonElement element : json.getAsJsonArray("scaling")){
                        if(!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber())
                            throw new JsonParseException("Scaling transform entry property 'scaling' must be an array of 3 numbers!");
                    }
                    scaling = json.getAsJsonArray("scaling").asList().stream().map(JsonElement::getAsNumber).mapToDouble(Number::doubleValue).toArray();
                }else
                    throw new JsonParseException("Scaling transform entry property 'scaling' must be a number or array of 3 numbers!");
                double[] origin = {0.5, 0.5, 0.5};
                if(json.has("origin")){
                    if(json.getAsJsonArray("origin").size() != 3)
                        throw new JsonParseException("Scaling transform entry property 'origin' must be an array of 3 numbers!");
                    for(JsonElement element : json.getAsJsonArray("origin")){
                        if(!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber())
                            throw new JsonParseException("Scaling transform entry property 'origin' must be an array of 3 numbers!");
                    }
                    origin = json.getAsJsonArray("origin").asList().stream()
                        .map(JsonElement::getAsNumber)
                        .mapToDouble(Number::doubleValue)
                        .map(d -> d / 16)
                        .toArray();
                }
                return new Matrix4f().identity().scaleAround(
                    (float)scaling[0], (float)scaling[1], (float)scaling[2],
                    (float)origin[0], (float)origin[1], (float)origin[2]
                );
            }

            case "rotate" -> {
                if(!json.has("rotation") && !json.has("axis") && !json.has("angle"))
                    throw new JsonParseException("Rotation transform entry must have property 'rotation', or 'axis' and 'angle'!");
                if(json.has("rotation") && (json.has("axis") || json.has("angle")))
                    throw new JsonParseException("Rotation transform entry must have property 'rotation', or 'axis' and 'angle', not both!");
                Matrix4f matrix;
                if(json.has("rotation")){
                    if(!json.get("rotation").isJsonArray() || json.getAsJsonArray("rotation").size() != 3)
                        throw new JsonParseException("Rotation transform entry property 'rotation' must be an array of 3 numbers!");
                    for(JsonElement element : json.getAsJsonArray("rotation")){
                        if(!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber())
                            throw new JsonParseException("Rotation transform entry property 'rotation' must be an array of 3 numbers!");
                    }
                    double[] rotation = json.getAsJsonArray("rotation").asList().stream().map(JsonElement::getAsNumber).mapToDouble(Number::doubleValue).toArray();
                    matrix = new Matrix4f().identity().rotationXYZ((float)(rotation[0] / 180 * Math.PI), (float)(rotation[1] / 180 * Math.PI), (float)(rotation[2] / 180 * Math.PI));
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
                        case "x" -> matrix = new Matrix4f().identity().rotateX(angle);
                        case "y" -> matrix = new Matrix4f().identity().rotateY(angle);
                        case "z" -> matrix = new Matrix4f().identity().rotateZ(angle);
                        default -> throw new JsonParseException("Rotation transform entry property 'axis' must be one of 'x', 'y', or 'z', not '" + axis + "'!");
                    }
                }
                double[] origin = {0.5, 0.5, 0.5};
                if(json.has("origin")){
                    if(json.getAsJsonArray("origin").size() != 3)
                        throw new JsonParseException("Rotation transform entry property 'origin' must be an array of 3 numbers!");
                    for(JsonElement element : json.getAsJsonArray("origin")){
                        if(!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber())
                            throw new JsonParseException("Rotation transform entry property 'origin' must be an array of 3 numbers!");
                    }
                    origin = json.getAsJsonArray("origin").asList().stream()
                        .map(JsonElement::getAsNumber)
                        .mapToDouble(Number::doubleValue)
                        .map(d -> d / 16)
                        .toArray();
                }
                return new Matrix4f().identity()
                    .translate((float)-origin[0], (float)-origin[1], (float)-origin[2])
                    .mul(matrix)
                    .translate((float)origin[0], (float)origin[1], (float)origin[2]);
            }
        }

        throw new JsonParseException("Transform entry property 'type' must be one of 'translate', 'scale', or 'rotate', not '" + type + "'!");
    }
}
