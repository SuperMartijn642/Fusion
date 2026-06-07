package com.supermartijn642.fusion.model.types.base;

import com.supermartijn642.fusion.api.model.custom.ModelMaterial;
import com.supermartijn642.fusion.api.model.custom.geometry.CuboidModelGeometry;
import com.supermartijn642.fusion.api.model.types.base.BaseModelData;
import com.supermartijn642.fusion.api.util.Either;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.RenderTypeGroup;
import org.jetbrains.annotations.Nullable;

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
    protected final Map<ItemTransforms.TransformType,ItemTransform> itemTransforms;
    protected final RenderTypeGroup forgeRenderTypeGroup;

    public BaseModelDataImpl(ResourceLocation parent, Map<String,Either<String,ModelMaterial>> materials, Boolean ambientOcclusion, Boolean shade, Boolean emissive, BlockModel.GuiLight guiLight, CuboidModelGeometry geometry, Map<ItemTransforms.TransformType,ItemTransform> itemTransforms, RenderTypeGroup forgeRenderTypeGroup){
        this.parent = parent;
        this.materials = Map.copyOf(materials);
        this.ambientOcclusion = ambientOcclusion;
        this.shade = shade;
        this.emissive = emissive;
        this.guiLight = guiLight;
        this.geometry = geometry;
        this.itemTransforms = Map.copyOf(itemTransforms);
        this.forgeRenderTypeGroup = forgeRenderTypeGroup;
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
    public @Nullable ItemTransform getItemTransform(ItemTransforms.TransformType type){
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
    public @Nullable RenderTypeGroup getForgeRenderTypeGroup(){
        return this.forgeRenderTypeGroup;
    }
}
