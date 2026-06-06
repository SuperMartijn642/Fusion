package com.supermartijn642.fusion.model;

import com.supermartijn642.fusion.api.model.custom.ItemModelBakingContext;
import com.supermartijn642.fusion.api.model.custom.ModelTransform;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.function.Consumer;

/**
 * Created 27/04/2023 by SuperMartijn642
 */
public class ItemModelBakingContextImpl extends BlockStateModelBakingContextImpl implements ItemModelBakingContext {

    private final List<ItemTintSource> tintSources;
    private final EntityModelSet entityModelSet;

    public ItemModelBakingContextImpl(Consumer<String> warnings, ResourceLocation identifier, ModelTransform transform, ModelBaker modelBaker, List<ItemTintSource> tintSources, EntityModelSet entityModelSet){
        super(warnings, identifier, transform, modelBaker);
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

    @Override
    public ItemModel getMissingItemModel(){
        return this.getMissingModels().item();
    }
}
