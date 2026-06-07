package com.supermartijn642.fusion.model.types.base;

import com.supermartijn642.fusion.api.model.custom.ModelMaterial;
import com.supermartijn642.fusion.api.model.custom.geometry.CuboidModelGeometry;
import com.supermartijn642.fusion.api.model.types.base.BaseModelData;
import com.supermartijn642.fusion.api.util.Either;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.client.RenderTypeGroup;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Created 06/09/2024 by SuperMartijn642
 */
public class BaseModelDataImpl implements BaseModelData {

    protected final Identifier parent;
    protected final Map<String,Either<String,ModelMaterial>> materials;
    protected final Boolean ambientOcclusion, shade, emissive;
    protected final UnbakedModel.GuiLight guiLight;
    protected final CuboidModelGeometry geometry;
    protected final Map<ItemDisplayContext,ItemTransform> itemTransforms;
    protected final RenderTypeGroup forgeRenderTypeGroup;

    public BaseModelDataImpl(Identifier parent, Map<String,Either<String,ModelMaterial>> materials, Boolean ambientOcclusion, Boolean shade, Boolean emissive, UnbakedModel.GuiLight guiLight, CuboidModelGeometry geometry, Map<ItemDisplayContext,ItemTransform> itemTransforms, RenderTypeGroup forgeRenderTypeGroup){
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
    public @Nullable Identifier getParent(){
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
    public @Nullable UnbakedModel.GuiLight getGuiLight(){
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
    public @Nullable RenderTypeGroup getForgeRenderTypeGroup(){
        return this.forgeRenderTypeGroup;
    }
}
