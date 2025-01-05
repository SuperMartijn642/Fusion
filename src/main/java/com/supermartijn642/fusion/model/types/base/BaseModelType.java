package com.supermartijn642.fusion.model.types.base;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.*;
import com.supermartijn642.fusion.api.model.data.BaseModelData;
import com.supermartijn642.fusion.util.IdentifierUtil;
import net.minecraft.client.renderer.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Created 06/09/2024 by SuperMartijn642
 */
public class BaseModelType implements ModelType<BaseModelData> {

    @Override
    public Collection<ResourceLocation> getModelDependencies(BaseModelData data){
        return data.getParents();
    }

    @Override
    public Collection<SpriteIdentifier> getTextureDependencies(GatherTexturesContext context, BaseModelData data){
        // Check for circular dependencies
        ((BaseModelDataImpl)data).validateParents(context::getModel, null);
        // Gather textures
        return ((BaseModelDataImpl)data).gatherTextures(context);
    }

    @Nullable
    @Override
    public BlockModel getAsVanillaModel(BaseModelData data){
        return data.getVanillaModel();
    }

    @Override
    public List<ResourceLocation> getParentModels(BaseModelData data){
        return data.getParents();
    }

    @SuppressWarnings("deprecation")
    @Override
    public IBakedModel bake(ModelBakingContext context, BaseModelData data){
        // Check for circular dependencies
        ((BaseModelDataImpl)data).validateParents(context::getModel, context.getModelIdentifier());
        // Bake the quads
        List<BaseModelQuad> quads = ((BaseModelDataImpl)data).bakeQuads(context);
        // Gather remaining model properties
        boolean ambientOcclusion = ((BaseModelDataImpl)data).findProperty(context, model -> model.hasAmbientOcclusion, true);
        boolean gui3d = ((BaseModelDataImpl)data).findProperty(context, model -> model.guiLight, BlockModel.GuiLight.SIDE).lightLikeBlock();
        TextureAtlasSprite particleSprite = context.getTexture(((BaseModelDataImpl)data).findParticleSprite(context));
        ItemTransformVec3f transformThirdPersonLeftHand = ((BaseModelDataImpl)data).findProperty(context, model -> model.transforms.hasTransform(ItemCameraTransforms.TransformType.THIRD_PERSON_LEFT_HAND) ? model.transforms.getTransform(ItemCameraTransforms.TransformType.THIRD_PERSON_LEFT_HAND) : null, ItemTransformVec3f.NO_TRANSFORM);
        ItemTransformVec3f transformThirdPersonRightHand = ((BaseModelDataImpl)data).findProperty(context, model -> model.transforms.hasTransform(ItemCameraTransforms.TransformType.THIRD_PERSON_RIGHT_HAND) ? model.transforms.getTransform(ItemCameraTransforms.TransformType.THIRD_PERSON_RIGHT_HAND) : null, ItemTransformVec3f.NO_TRANSFORM);
        ItemTransformVec3f transformFirstPersonLeftHand = ((BaseModelDataImpl)data).findProperty(context, model -> model.transforms.hasTransform(ItemCameraTransforms.TransformType.FIRST_PERSON_LEFT_HAND) ? model.transforms.getTransform(ItemCameraTransforms.TransformType.FIRST_PERSON_LEFT_HAND) : null, ItemTransformVec3f.NO_TRANSFORM);
        ItemTransformVec3f transformFirstPersonRightHand = ((BaseModelDataImpl)data).findProperty(context, model -> model.transforms.hasTransform(ItemCameraTransforms.TransformType.FIRST_PERSON_RIGHT_HAND) ? model.transforms.getTransform(ItemCameraTransforms.TransformType.FIRST_PERSON_RIGHT_HAND) : null, ItemTransformVec3f.NO_TRANSFORM);
        ItemTransformVec3f transformHead = ((BaseModelDataImpl)data).findProperty(context, model -> model.transforms.hasTransform(ItemCameraTransforms.TransformType.HEAD) ? model.transforms.getTransform(ItemCameraTransforms.TransformType.HEAD) : null, ItemTransformVec3f.NO_TRANSFORM);
        ItemTransformVec3f transformGui = ((BaseModelDataImpl)data).findProperty(context, model -> model.transforms.hasTransform(ItemCameraTransforms.TransformType.GUI) ? model.transforms.getTransform(ItemCameraTransforms.TransformType.GUI) : null, ItemTransformVec3f.NO_TRANSFORM);
        ItemTransformVec3f transformGround = ((BaseModelDataImpl)data).findProperty(context, model -> model.transforms.hasTransform(ItemCameraTransforms.TransformType.GROUND) ? model.transforms.getTransform(ItemCameraTransforms.TransformType.GROUND) : null, ItemTransformVec3f.NO_TRANSFORM);
        ItemTransformVec3f transformFixed = ((BaseModelDataImpl)data).findProperty(context, model -> model.transforms.hasTransform(ItemCameraTransforms.TransformType.FIXED) ? model.transforms.getTransform(ItemCameraTransforms.TransformType.FIXED) : null, ItemTransformVec3f.NO_TRANSFORM);
        ItemCameraTransforms itemTransforms = new ItemCameraTransforms(transformThirdPersonLeftHand, transformThirdPersonRightHand, transformFirstPersonLeftHand, transformFirstPersonRightHand, transformHead, transformGui, transformGround, transformFixed);
        ItemOverrideList itemOverrides = data.getVanillaModel().overrides.isEmpty() ? ItemOverrideList.EMPTY : new ItemOverrideList(context.getModelBakery(), data.getVanillaModel(), i -> context.getModel(i).getAsVanillaModel(), data.getVanillaModel().overrides);
        // Finally, create the model
        return new BaseBakedModel(
            quads,
            ambientOcclusion,
            gui3d,
            true,
            particleSprite,
            itemTransforms,
            itemOverrides
        );
    }

    @Override
    public BaseModelData deserialize(JsonObject json) throws JsonParseException{
        // Deserialize the vanilla model attributes
        BlockModel model = DefaultModelTypes.VANILLA.deserialize(json);
        // Read parents
        if(json.has("parent") && json.has("parents"))
            throw new JsonParseException("Model can only have either 'parent' or 'parents', not both!");
        List<ResourceLocation> parents = Collections.emptyList();
        if(json.has("parent")){
            if(!json.get("parent").isJsonPrimitive() || !json.get("parent").getAsJsonPrimitive().isString())
                throw new JsonParseException("Property 'parent' must be a string!");
            String parent = json.get("parent").getAsString();
            if(!IdentifierUtil.isValidIdentifier(parent))
                throw new JsonParseException("Property 'parent' must be a valid identifier!");
            parents = Collections.singletonList(new ResourceLocation(parent));
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
                parents.add(new ResourceLocation(parent));
            }
            if(!parents.isEmpty())
                model.parentLocation = parents.get(0);
        }
        // Read elements (for 'light_emission' property)
        List<BaseModelElement> elements = new ArrayList<>(model.elements.size());
        JsonArray elementsJson = json.getAsJsonArray("elements");
        for(int i = 0; i < model.elements.size(); i++){
            BlockPart vanillaElement = model.elements.get(i);
            Integer lightEmission = null;
            JsonElement lightEmissionJson = elementsJson.get(i).getAsJsonObject().get("light_emission");
            if(lightEmissionJson != null){
                if(!lightEmissionJson.isJsonPrimitive() || !lightEmissionJson.getAsJsonPrimitive().isNumber())
                    throw new JsonParseException("Element property 'light_emission' must be a number!");
                lightEmission = lightEmissionJson.getAsInt();
                if(lightEmission < 0 || lightEmission > 15)
                    throw new JsonParseException("Element property 'light_emission' must be between 0 and 15!");
            }
            elements.add(new BaseModelElement(vanillaElement.from, vanillaElement.to, vanillaElement.faces, vanillaElement.rotation, vanillaElement.shade, lightEmission));
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
            JsonArray parents = new JsonArray();
            value.getParents().forEach(p -> parents.add(p.toString()));
            json.add("parents", parents);
        }
        // Add 'light_emission' property to model elements
        for(int i = 0; i < ((BaseModelDataImpl)value).getElements().size(); i++){
            Integer lightEmission = ((BaseModelDataImpl)value).getElements().get(i).light_emission;
            if(lightEmission != null)
                json.getAsJsonArray("elements").get(i).getAsJsonObject().addProperty("light_emission", lightEmission);
        }
        return json;
    }
}
