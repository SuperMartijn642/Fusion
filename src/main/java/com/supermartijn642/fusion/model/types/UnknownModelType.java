package com.supermartijn642.fusion.model.types;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.BlockModelBakingContext;
import com.supermartijn642.fusion.api.model.ItemModelBakingContext;
import com.supermartijn642.fusion.api.model.ModelType;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.block.model.SimpleModelWrapper;
import net.minecraft.client.renderer.block.model.SingleVariant;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.renderer.item.BlockModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.QuadCollection;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * Created 30/04/2023 by SuperMartijn642
 */
public class UnknownModelType<T extends UnbakedModel> implements ModelType<T> {

    @Override
    public T deserialize(JsonObject json) throws JsonParseException{
        throw new UnsupportedOperationException("Cannot deserialize unknown model type!");
    }

    @Override
    public JsonObject serialize(T value){
        throw new UnsupportedOperationException("Cannot serialize unknown model type!");
    }

    @Override
    public Collection<ResourceLocation> getModelDependencies(T data){
        ResourceLocation parent = data.parent();
        return parent == null ? List.of() : List.of(parent);
    }

    @Override
    public BlockStateModel bakeBlockModel(BlockModelBakingContext context, T data){
        TextureSlots textures = new TextureSlots(context.getTopLevelTextureReferences());
        boolean ambientOcclusion = context.getTopLevelAmbientOcclusion();
        TextureAtlasSprite particle = context.getModelBaker().sprites().resolveSlot(textures, "particle", () -> context.getModelIdentifier().toString());
        QuadCollection quads = context.getTopLevelGeometry().bake(textures, context.getModelBaker(), context.getTransformation(), () -> context.getModelIdentifier().toString(), context.getForgeContext());
        return new SingleVariant(new SimpleModelWrapper(quads, ambientOcclusion, particle));
    }

    @Override
    public ItemModel bakeItemModel(ItemModelBakingContext context, T data){
        TextureSlots textures = new TextureSlots(context.getTopLevelTextureReferences());
        TextureAtlasSprite particle = context.getModelBaker().sprites().resolveSlot(textures, "particle", () -> context.getModelIdentifier().toString());
        QuadCollection quads = context.getTopLevelGeometry().bake(textures, context.getModelBaker(), BlockModelRotation.X0_Y0, () -> context.getModelIdentifier().toString());
        return new BlockModelWrapper(context.getTintSources(), quads.getAll(), new ModelRenderProperties(
            context.getTopLevelUseBlockLighting(),
            particle,
            context.getTopLevelItemTransforms()
        ));
    }

    @Override
    public @Nullable UnbakedModel getAsVanillaModel(T data){
        return data;
    }

    @Override
    public List<ResourceLocation> getParentModels(T data){
        ResourceLocation parent = data.parent();
        return parent == null ? List.of() : List.of(parent);
    }
}
