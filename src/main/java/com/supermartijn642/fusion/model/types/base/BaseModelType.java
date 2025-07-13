package com.supermartijn642.fusion.model.types.base;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.DefaultModelTypes;
import com.supermartijn642.fusion.api.model.ModelBakingContext;
import com.supermartijn642.fusion.api.model.ModelType;
import com.supermartijn642.fusion.api.model.data.BaseModelData;
import com.supermartijn642.fusion.util.IdentifierUtil;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemModelGenerator;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.client.NamedRenderTypeManager;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created 06/09/2024 by SuperMartijn642
 */
public class BaseModelType implements ModelType<BaseModelData> {

    @Override
    public Collection<ResourceLocation> getModelDependencies(BaseModelData data){
        return data.getParents();
    }

    @Nullable
    @Override
    public UnbakedModel getAsVanillaModel(BaseModelData data){
        return data.getVanillaModel();
    }

    @Override
    public List<ResourceLocation> getParentModels(BaseModelData data){
        return data.getParents();
    }

    @Override
    public BakedModel bake(ModelBakingContext context, BaseModelData data){
        // Check for circular dependencies
        ((BaseModelDataImpl)data).validateParents(context);
        // Bake the quads
        List<BaseModelQuad> quads = ((BaseModelDataImpl)data).bakeQuads(context);
        // Gather remaining model properties
        boolean ambientOcclusion = ((BaseModelDataImpl)data).findProperty(context, UnbakedModel::getAmbientOcclusion, true);
        boolean isGui3d = ((BaseModelDataImpl)data).findProperty(context, model -> model instanceof ItemModelGenerator ? false : null, true);
        boolean usesBlockLight = ((BaseModelDataImpl)data).findProperty(context, UnbakedModel::getGuiLight, BlockModel.GuiLight.SIDE).lightLikeBlock();
        TextureAtlasSprite particleSprite = context.getTexture(((BaseModelDataImpl)data).findParticleSprite(context));
        //noinspection deprecation
        ItemTransforms itemTransforms = new ItemTransforms(
            ((BaseModelDataImpl)data).findItemTransform(context, ItemDisplayContext.THIRD_PERSON_LEFT_HAND),
            ((BaseModelDataImpl)data).findItemTransform(context, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND),
            ((BaseModelDataImpl)data).findItemTransform(context, ItemDisplayContext.FIRST_PERSON_LEFT_HAND),
            ((BaseModelDataImpl)data).findItemTransform(context, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND),
            ((BaseModelDataImpl)data).findItemTransform(context, ItemDisplayContext.HEAD),
            ((BaseModelDataImpl)data).findItemTransform(context, ItemDisplayContext.GUI),
            ((BaseModelDataImpl)data).findItemTransform(context, ItemDisplayContext.GROUND),
            ((BaseModelDataImpl)data).findItemTransform(context, ItemDisplayContext.FIXED)
        );
        RenderType forgeRenderType = NamedRenderTypeManager.get(data.getVanillaModel().customData.getRenderTypeHint()).block();
        // Finally, create the model
        return new BaseBakedModel(
            quads,
            ambientOcclusion,
            isGui3d,
            usesBlockLight,
            particleSprite,
            itemTransforms,
            forgeRenderType
        );
    }

    @Override
    public BaseModelData deserialize(JsonObject json) throws JsonParseException{
        // Deserialize the vanilla model attributes
        BlockModel model = DefaultModelTypes.VANILLA.deserialize(json);
        // Read parents
        if(json.has("parent") && json.has("parents"))
            throw new JsonParseException("Model can only have either 'parent' or 'parents', not both!");
        List<ResourceLocation> parents = List.of();
        if(json.has("parent")){
            if(!json.get("parent").isJsonPrimitive() || !json.get("parent").getAsJsonPrimitive().isString())
                throw new JsonParseException("Property 'parent' must be a string!");
            String parent = json.get("parent").getAsString();
            if(!IdentifierUtil.isValidIdentifier(parent))
                throw new JsonParseException("Property 'parent' must be a valid identifier!");
            parents = List.of(ResourceLocation.parse(parent));
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
                parents.add(ResourceLocation.parse(parent));
            }
            if(!parents.isEmpty())
                model.parentLocation = parents.get(0);
        }
        List<BaseModelElement> elements = new ArrayList<>(model.elements.size());
        model.elements.forEach(element -> elements.add(new BaseModelElement(element.from, element.to, element.faces, element.rotation, element.shade, element.lightEmission)));
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
