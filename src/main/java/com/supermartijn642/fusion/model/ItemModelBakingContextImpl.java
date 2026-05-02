package com.supermartijn642.fusion.model;

import com.supermartijn642.fusion.api.model.ItemModelBakingContext;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.resources.model.*;
import net.minecraft.client.resources.model.cuboid.ItemTransforms;
import net.minecraft.client.resources.model.geometry.UnbakedGeometry;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextMap;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Created 27/04/2023 by SuperMartijn642
 */
public class ItemModelBakingContextImpl extends BlockModelBakingContextImpl implements ItemModelBakingContext {

    private final List<ItemTintSource> tintSources;
    private final EntityModelSet entityModelSet;

    public ItemModelBakingContextImpl(ModelBaker modelBaker, Function<Material,Material.Baked> materialBaker, Identifier modelIdentifier, Map<Identifier,UnbakedModel> dependencies, Map<String,Material> topLevelTextureReferences, boolean topLevelAmbientOcclusion, boolean topLevelUseBlockLighting, ItemTransforms topLevelItemTransforms, UnbakedGeometry topLevelGeometry, ContextMap neoforgeAdditionalProperties, List<ItemTintSource> tintSources, EntityModelSet entityModelSet){
        super(modelBaker, materialBaker, BlockModelRotation.IDENTITY, modelIdentifier, dependencies, topLevelTextureReferences, topLevelAmbientOcclusion, topLevelUseBlockLighting, topLevelItemTransforms, topLevelGeometry, neoforgeAdditionalProperties);
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
