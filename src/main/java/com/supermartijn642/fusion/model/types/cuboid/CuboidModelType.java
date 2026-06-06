package com.supermartijn642.fusion.model.types.cuboid;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.DefaultModelTypes;
import com.supermartijn642.fusion.api.model.custom.BlockStateModelBakingContext;
import com.supermartijn642.fusion.api.model.custom.ModelMaterial;
import com.supermartijn642.fusion.api.model.custom.ModelStack;
import com.supermartijn642.fusion.api.model.custom.ModelTransform;
import com.supermartijn642.fusion.api.model.custom.geometry.CuboidModelGeometry;
import com.supermartijn642.fusion.api.model.custom.geometry.ModelGeometry;
import com.supermartijn642.fusion.api.model.types.base.BaseModelData;
import com.supermartijn642.fusion.model.types.UnknownModelType;
import net.minecraft.client.resources.model.cuboid.CuboidModel;
import net.minecraft.client.resources.model.cuboid.CuboidModelElement;
import net.minecraft.client.resources.model.cuboid.UnbakedCuboidGeometry;
import net.minecraft.client.resources.model.sprite.TextureSlots;

import java.util.Map;

/**
 * Created 29/04/2023 by SuperMartijn642
 */
public class CuboidModelType extends UnknownModelType<CuboidModel> {

    @Override
    protected void bakeGeometry(BlockStateModelBakingContext context, ModelStack modelStack, CuboidModel data, ModelTransform transform, ModelGeometry.MaterialResolver materialResolver, ModelGeometry.QuadConsumer quadConsumer){
        this.getGeometry(data).bake(quadConsumer, transform, materialResolver);
    }

    @Override
    public CuboidModel deserialize(JsonObject json) throws JsonParseException{
        return CuboidModel.GSON.fromJson(json, CuboidModel.class);
    }

    @Override
    public JsonObject serialize(CuboidModel model){
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
                case TextureSlots.Value value -> builder.material(key, ModelMaterial.of(value.material()));
            }
        }
        // Copy elements
        if(model.geometry() instanceof UnbakedCuboidGeometry geometry){
            for(CuboidModelElement element : geometry.elements())
                builder.elements(CuboidModelGeometry.Element.of(element));
        }
        // Serialize data
        return DefaultModelTypes.BASE.serialize(builder.build());
    }
}
