package com.supermartijn642.fusion.model;

import com.supermartijn642.fusion.api.model.ItemModelBakingContext;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Created 27/04/2023 by SuperMartijn642
 */
public class ItemModelBakingContextImpl extends BlockModelBakingContextImpl implements ItemModelBakingContext {

    private final List<ItemTintSource> tintSources;
    private final EntityModelSet entityModelSet;

    public ItemModelBakingContextImpl(ModelBaker modelBaker, Function<Material,TextureAtlasSprite> spriteGetter, ResourceLocation modelIdentifier, Map<ResourceLocation,UnbakedModel> dependencies, Map<String,Material> topLevelTextureReferences, boolean topLevelAmbientOcclusion, boolean topLevelUseBlockLighting, ItemTransforms topLevelItemTransforms, UnbakedGeometry topLevelGeometry, IGeometryBakingContext forgeBakingContext, List<ItemTintSource> tintSources, EntityModelSet entityModelSet){
        super(modelBaker, spriteGetter, BlockModelRotation.X0_Y0, modelIdentifier, dependencies, topLevelTextureReferences, topLevelAmbientOcclusion, topLevelUseBlockLighting, topLevelItemTransforms, topLevelGeometry, forgeBakingContext);
        this.tintSources = tintSources;
        this.entityModelSet = entityModelSet;
    }

    @Override
    public List<ItemTintSource> getTintSources(){
        return this.tintSources;
    }

    @Override
    public EntityModelSet getEntityModels(){
        return this.entityModelSet;
    }
}
