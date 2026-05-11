package com.supermartijn642.fusion.model.types.cuboid;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.ModelInstance;
import com.supermartijn642.fusion.api.model.custom.geometry.CuboidModelGeometry;
import com.supermartijn642.fusion.api.model.custom.geometry.ModelGeometry;
import com.supermartijn642.fusion.api.util.Either;
import com.supermartijn642.fusion.model.types.UnknownModelType;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.List;

/**
 * Created 29/04/2023 by SuperMartijn642
 */
public class CuboidModelType extends UnknownModelType<BlockModel> {

    @Override
    public Collection<ResourceLocation> getDependencies(BlockModel data){
        ResourceLocation parent = data.getParentLocation();
        return parent == null ? List.of() : List.of(parent);
    }

    @Override
    public List<Either<ResourceLocation,ModelInstance<?>>> getParents(BlockModel data){
        ResourceLocation parent = data.getParentLocation();
        return parent == null ? List.of() : List.of(Either.left(parent));
    }

    @Override
    public ModelGeometry getGeometry(BlockModel data){
        List<BlockElement> elements = data.getElements();
        return elements == null || elements.isEmpty() ? null : CuboidModelGeometry.of(data);
    }

    @Override
    public BlockModel deserialize(JsonObject json) throws JsonParseException{
        return BlockModel.GSON.fromJson(json, BlockModel.class);
    }

    @Override
    public JsonObject serialize(BlockModel value){
        return (JsonObject)CuboidModelSerializer.GSON.toJsonTree(value);
    }
}
