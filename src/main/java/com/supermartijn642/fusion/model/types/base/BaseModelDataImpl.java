package com.supermartijn642.fusion.model.types.base;

import com.supermartijn642.fusion.api.model.custom.ModelMaterial;
import com.supermartijn642.fusion.api.model.custom.geometry.CuboidModelGeometry;
import com.supermartijn642.fusion.api.model.types.base.BaseModelData;
import com.supermartijn642.fusion.api.util.Either;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverride;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.neoforge.client.RenderTypeGroup;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Created 06/09/2024 by SuperMartijn642
 */
public class BaseModelDataImpl implements BaseModelData {

    protected final ResourceLocation parent;
    protected final Map<String,Either<String,ModelMaterial>> materials;
    protected final Boolean ambientOcclusion, shade, emissive;
    protected final BlockModel.GuiLight guiLight;
    protected final CuboidModelGeometry geometry;
    protected final Map<ItemDisplayContext,ItemTransform> itemTransforms;
    protected final List<ItemOverride> itemOverrides;
    protected final RenderTypeGroup neoRenderTypeGroup;

    public BaseModelDataImpl(ResourceLocation parent, Map<String,Either<String,ModelMaterial>> materials, Boolean ambientOcclusion, Boolean shade, Boolean emissive, BlockModel.GuiLight guiLight, CuboidModelGeometry geometry, Map<ItemDisplayContext,ItemTransform> itemTransforms, List<ItemOverride> itemOverrides, RenderTypeGroup neoRenderTypeGroup){
        this.parent = parent;
        this.materials = Map.copyOf(materials);
        this.ambientOcclusion = ambientOcclusion;
        this.shade = shade;
        this.emissive = emissive;
        this.guiLight = guiLight;
        this.geometry = geometry;
        this.itemTransforms = Map.copyOf(itemTransforms);
        this.itemOverrides = itemOverrides;
        this.neoRenderTypeGroup = neoRenderTypeGroup;
    }

    @Override
    public @Nullable ResourceLocation getParent(){
        return this.parent;
    }

    @Override
    public Map<String,Either<String,ModelMaterial>> getMaterials(){
        return this.materials;
    }

    @Override
    public CuboidModelGeometry getGeometry(){
        return this.geometry;
    }

    @Override
    public @Nullable Boolean getAmbientOcclusion(){
        return this.ambientOcclusion;
    }

    @Override
    public @Nullable BlockModel.GuiLight getGuiLight(){
        return this.guiLight;
    }

    @Override
    public @Nullable ItemTransform getItemTransform(ItemDisplayContext type){
        return this.itemTransforms.get(type);
    }

    @Override
    public @Nullable Boolean getShade(){
        return this.shade;
    }

    @Override
    public @Nullable Boolean getEmissive(){
        return this.emissive;
    }

    @Override
    public List<ItemOverride> getItemOverrides(){
        return this.itemOverrides;
    }

    @Override
    public @Nullable RenderTypeGroup getNeoRenderTypeGroup(){
        return this.neoRenderTypeGroup;
    }
}
