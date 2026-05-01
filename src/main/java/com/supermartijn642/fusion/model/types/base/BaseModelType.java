package com.supermartijn642.fusion.model.types.base;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.BlockModelBakingContext;
import com.supermartijn642.fusion.api.model.DefaultModelTypes;
import com.supermartijn642.fusion.api.model.ItemModelBakingContext;
import com.supermartijn642.fusion.api.model.ModelType;
import com.supermartijn642.fusion.api.model.data.BaseModelData;
import com.supermartijn642.fusion.util.IdentifierUtil;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.client.resources.model.cuboid.CuboidModel;
import net.minecraft.client.resources.model.cuboid.CuboidModelElement;
import net.minecraft.client.resources.model.cuboid.ItemTransforms;
import net.minecraft.client.resources.model.cuboid.UnbakedCuboidGeometry;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created 06/09/2024 by SuperMartijn642
 */
public class BaseModelType implements ModelType<BaseModelData> {

    @Override
    public Collection<Identifier> getModelDependencies(BaseModelData data){
        return data.getParents();
    }

    @Nullable
    @Override
    public UnbakedModel getAsVanillaModel(BaseModelData data){
        return data.getVanillaModel();
    }

    @Override
    public List<Identifier> getParentModels(BaseModelData data){
        return data.getParents();
    }

    @Override
    public BlockStateModel bakeBlockModel(BlockModelBakingContext context, BaseModelData data){
        // Check for circular dependencies
        ((BaseModelDataImpl)data).validateParents(context);
        // Bake the quads
        List<BaseModelQuad> quads = ((BaseModelDataImpl)data).bakeQuads(context);
        // Gather remaining model properties
        Boolean ambientOcclusion = ((BaseModelDataImpl)data).findProperty(context, UnbakedModel::ambientOcclusion, null);
        Material.Baked particleSprite = context.getMaterial(((BaseModelDataImpl)data).findParticleSprite(context));
        // Finally, create the model
        return new BaseBakedModel(
            quads,
            ambientOcclusion,
            particleSprite
        );
    }

    @Override
    public ItemModel bakeItemModel(ItemModelBakingContext context, BaseModelData data){
        // Check for circular dependencies
        ((BaseModelDataImpl)data).validateParents(context);
        // Bake the quads
        List<BaseModelQuad> quads = ((BaseModelDataImpl)data).bakeQuads(context);
        // Gather remaining model properties
        boolean usesBlockLight = ((BaseModelDataImpl)data).findProperty(context, UnbakedModel::guiLight, UnbakedModel.GuiLight.SIDE).lightLikeBlock();
        Material.Baked particleSprite = context.getMaterial(((BaseModelDataImpl)data).findParticleSprite(context));
        //noinspection deprecation
        ItemTransforms transforms = new ItemTransforms(
            ((BaseModelDataImpl)data).findItemTransform(context, ItemDisplayContext.THIRD_PERSON_LEFT_HAND),
            ((BaseModelDataImpl)data).findItemTransform(context, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND),
            ((BaseModelDataImpl)data).findItemTransform(context, ItemDisplayContext.FIRST_PERSON_LEFT_HAND),
            ((BaseModelDataImpl)data).findItemTransform(context, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND),
            ((BaseModelDataImpl)data).findItemTransform(context, ItemDisplayContext.HEAD),
            ((BaseModelDataImpl)data).findItemTransform(context, ItemDisplayContext.GUI),
            ((BaseModelDataImpl)data).findItemTransform(context, ItemDisplayContext.GROUND),
            ((BaseModelDataImpl)data).findItemTransform(context, ItemDisplayContext.FIXED),
            ((BaseModelDataImpl)data).findItemTransform(context, ItemDisplayContext.ON_SHELF)
        );
        // Finally, create the model
        return new BaseItemModel(
            context.getTintSources(),
            quads,
            new ModelRenderProperties(usesBlockLight, particleSprite, transforms),
            context.getTransformation().transformation().getMatrix()
        );
    }

    @Override
    public BaseModelData deserialize(JsonObject json) throws JsonParseException{
        // Deserialize the vanilla model attributes
        CuboidModel model = DefaultModelTypes.VANILLA.deserialize(json);
        // Read parents
        if(json.has("parent") && json.has("parents"))
            throw new JsonParseException("Model can only have either 'parent' or 'parents', not both!");
        List<Identifier> parents = List.of();
        if(json.has("parent")){
            if(!json.get("parent").isJsonPrimitive() || !json.get("parent").getAsJsonPrimitive().isString())
                throw new JsonParseException("Property 'parent' must be a string!");
            String parent = json.get("parent").getAsString();
            if(!IdentifierUtil.isValidIdentifier(parent))
                throw new JsonParseException("Property 'parent' must be a valid identifier!");
            parents = List.of(Identifier.parse(parent));
        }else if(json.has("parents")){
            if(!json.get("parents").isJsonArray())
                throw new JsonParseException("Property 'parents' must be an array!");
            JsonArray parentArray = json.getAsJsonArray("parents");
            parents = new ArrayList<>(parentArray.size());
            for(JsonElement element : parentArray){
                if(!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString())
                    throw new JsonParseException("Array 'parents' must only contain strings!");
                String parent = element.getAsString();
                if(!IdentifierUtil.isValidIdentifier(parent))
                    throw new JsonParseException("Array 'parents' must only contain valid identifiers, not '" + parent + "'!");
                parents.add(Identifier.parse(parent));
            }
            if(!parents.isEmpty())
                model = new CuboidModel(model.geometry(), model.guiLight(), model.ambientOcclusion(), model.transforms(), model.textureSlots(), parents.get(0));
        }
        List<BaseModelElement> elements = List.of();
        if(model.geometry() instanceof UnbakedCuboidGeometry(
            List<CuboidModelElement> vanillaElements
        )){
            elements = new ArrayList<>(vanillaElements.size());
            for(CuboidModelElement element : vanillaElements)
                elements.add(new BaseModelElement(element.from(), element.to(), element.faces(), element.rotation(), element.shade(), element.lightEmission()));
        }
        return new BaseModelDataImpl(model, parents, elements);
    }

    @Override
    public JsonObject serialize(BaseModelData value){
        // Vanilla properties
        JsonObject json = DefaultModelTypes.VANILLA.serialize(value.getVanillaModel());
        // Add 'parents'
        if(value.getParents().size() > 1){
            json.remove("parent");
            JsonArray parents = new JsonArray(value.getParents().size());
            value.getParents().forEach(p -> parents.add(p.toString()));
            json.add("parents", parents);
        }
        return json;
    }
}
