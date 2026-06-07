package com.supermartijn642.fusion.model.types.cuboid;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.DefaultModelTypes;
import com.supermartijn642.fusion.api.model.custom.*;
import com.supermartijn642.fusion.api.model.custom.geometry.CuboidModelGeometry;
import com.supermartijn642.fusion.api.model.custom.geometry.ModelGeometry;
import com.supermartijn642.fusion.api.model.types.base.BaseModelData;
import com.supermartijn642.fusion.api.util.Either;
import com.supermartijn642.fusion.api.util.Property;
import com.supermartijn642.fusion.model.SimpleModelType;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.neoforge.client.model.NeoForgeModelProperties;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Created 29/04/2023 by SuperMartijn642
 */
public class CuboidModelType extends SimpleModelType<BlockModel> {

    @Override
    public Collection<ResourceLocation> getDependencies(BlockModel data){
        ResourceLocation parent = data.getParentLocation();
        return parent == null ? List.of() : List.of(parent);
    }

    @Override
    public List<Either<ResourceLocation,UntypedModelInstance>> getParents(BlockModel data){
        ResourceLocation parent = data.getParentLocation();
        return parent == null ? List.of() : List.of(Either.left(parent));
    }

    @Override
    public Boolean getAmbientOcclusion(BlockModel data){
        return data.getAmbientOcclusion();
    }

    @Override
    public UnbakedModel.GuiLight getGuiLight(BlockModel data){
        return data.getGuiLight();
    }

    @Override
    public ModelGeometry getGeometry(BlockModel data){
        List<BlockElement> elements = data.getElements();
        return elements == null || elements.isEmpty() ? null : CuboidModelGeometry.of(data);
    }

    @Override
    public ItemTransform getItemTransform(ItemDisplayContext type, BlockModel data){
        return DefaultModelTypes.UNKNOWN.getItemTransform(type, data);
    }

    @Override
    public Map<String,Either<String,ModelMaterial>> getMaterials(BlockModel data){
        return DefaultModelTypes.UNKNOWN.getMaterials(data);
    }

    @Override
    public @Nullable Boolean getShade(BlockModel data){
        return null;
    }

    @Override
    public @Nullable Boolean getEmissive(BlockModel data){
        return null;
    }

    @Override
    public <X, C> Optional<X> getProperty(Property<X,C> property, C context, BlockModel data){
        return DefaultModelTypes.UNKNOWN.getProperty(property, context, data);
    }

    @Override
    protected @Nullable ResourceLocation getParent(BlockModel data){
        return data.getParentLocation();
    }

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
        builder.parent(model.getParentLocation())
            .guiLight(model.getGuiLight())
            .ambientOcclusion(model.getAmbientOcclusion())
            .itemTransforms(model.getTransforms());
        // Copy materials
        for(Map.Entry<String,TextureSlots.SlotContents> entry : model.getTextureSlots().values().entrySet()){
            String key = entry.getKey();
            switch(entry.getValue()){
                case TextureSlots.Reference reference -> builder.material(key, reference.target());
                case TextureSlots.Value value -> builder.material(key, value.material().texture());
            }
        }
        // Copy elements
        List<BlockElement> elements = model.getElements();
        if(elements != null){
            for(BlockElement element : elements)
                builder.elements(CuboidModelGeometry.Element.of(element));
        }
        // NeoForge render type
        ContextMap.Builder contextMap = new ContextMap.Builder();
        model.fillAdditionalProperties(contextMap);
        builder.neoRenderTypeGroup(contextMap.getOptionalParameter(NeoForgeModelProperties.RENDER_TYPE));
        // Serialize data
        return DefaultModelTypes.BASE.serialize(builder.build());
    }
}
