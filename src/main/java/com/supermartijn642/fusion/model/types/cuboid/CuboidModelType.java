package com.supermartijn642.fusion.model.types.cuboid;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.DefaultModelTypes;
import com.supermartijn642.fusion.api.model.custom.BlockStateModelBakingContext;
import com.supermartijn642.fusion.api.model.custom.ModelStack;
import com.supermartijn642.fusion.api.model.custom.ModelTransform;
import com.supermartijn642.fusion.api.model.custom.geometry.CuboidModelGeometry;
import com.supermartijn642.fusion.api.model.custom.geometry.ModelGeometry;
import com.supermartijn642.fusion.api.model.types.base.BaseModelData;
import com.supermartijn642.fusion.model.types.UnknownModelType;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.SimpleUnbakedGeometry;
import net.minecraft.client.renderer.block.model.TextureSlots;

import java.util.Map;

/**
 * Created 29/04/2023 by SuperMartijn642
 */
public class CuboidModelType extends UnknownModelType<BlockModel> {

    @Override
    protected void bakeGeometry(BlockStateModelBakingContext context, ModelStack modelStack, BlockModel data, ModelTransform transform, ModelGeometry.MaterialKeyResolver materialResolver, ModelGeometry.QuadConsumer quadConsumer){
        this.getGeometry(data).bake(quadConsumer, transform, materialResolver);
    }

    @Override
    public BlockModel deserialize(JsonObject json) throws JsonParseException{
        return BlockModel.GSON.fromJson(json, BlockModel.class);
    }

    @Override
    public JsonObject serialize(BlockModel model){
        // Use base model type to serialize vanilla cuboid model
        BaseModelData.Builder<?,BaseModelData> builder = BaseModelData.builder();
        // Copy properties
        builder.parent(model.parent())
            .guiLight(model.guiLight())
            .ambientOcclusion(model.ambientOcclusion())
            .itemTransforms(model.transforms());
        // Copy materials
        for(Map.Entry<String,TextureSlots.SlotContents> entry : model.textureSlots().values().entrySet()){
            String key = entry.getKey();
            switch(entry.getValue()){
                case TextureSlots.Reference reference -> builder.material(key, reference.target());
                case TextureSlots.Value value -> builder.material(key, value.material().texture());
            }
        }
        // Copy elements
        if(model.geometry() instanceof SimpleUnbakedGeometry geometry){
            for(BlockElement element : geometry.elements())
                builder.elements(CuboidModelGeometry.Element.of(element));
        }
        // Serialize data
        return DefaultModelTypes.BASE.serialize(builder.build());
    }
}
