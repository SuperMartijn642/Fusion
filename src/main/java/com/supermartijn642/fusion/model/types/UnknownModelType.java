package com.supermartijn642.fusion.model.types;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.BlockModelBakingContext;
import com.supermartijn642.fusion.api.model.ItemModelBakingContext;
import com.supermartijn642.fusion.api.model.ModelType;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.SingleVariant;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fc;

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
    public Collection<Identifier> getModelDependencies(T data){
        Identifier parent = data.parent();
        return parent == null ? List.of() : List.of(parent);
    }

    @Override
    public BlockStateModel bakeBlockModel(BlockModelBakingContext context, T data){
        TextureSlots textures = new TextureSlots(context.getTopLevelTextureReferences());
        boolean ambientOcclusion = context.getTopLevelAmbientOcclusion();
        Material.Baked particle = context.getModelBaker().materials().resolveSlot(textures, "particle", () -> context.getModelIdentifier().toString());
        QuadCollection quads = context.getTopLevelGeometry().bake(textures, context.getModelBaker(), context.getTransformation(), () -> context.getModelIdentifier().toString(), context.getNeoForgeAdditionalProperties());
        return new SingleVariant(new SimpleModelWrapper(quads, ambientOcclusion, particle));
    }

    @Override
    public ItemModel bakeItemModel(ItemModelBakingContext context, T data, Matrix4fc transformation){
        TextureSlots textures = new TextureSlots(context.getTopLevelTextureReferences());
        Material.Baked particle = context.getModelBaker().materials().resolveSlot(textures, "particle", () -> context.getModelIdentifier().toString());
        QuadCollection quads = context.getTopLevelGeometry().bake(textures, context.getModelBaker(), BlockModelRotation.IDENTITY, () -> context.getModelIdentifier().toString());
        return new CuboidItemModelWrapper(context.getTintSources(), quads, new ModelRenderProperties(
            context.getTopLevelUseBlockLighting(),
            particle,
            context.getTopLevelItemTransforms()
        ), transformation);
    }

    @Override
    public @Nullable UnbakedModel getAsVanillaModel(T data){
        return data;
    }

    @Override
    public List<Identifier> getParentModels(T data){
        Identifier parent = data.parent();
        return parent == null ? List.of() : List.of(parent);
    }
}
