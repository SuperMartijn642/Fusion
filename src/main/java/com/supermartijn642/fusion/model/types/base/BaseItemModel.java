package com.supermartijn642.fusion.model.types.base;

import com.google.common.base.Suppliers;
import com.supermartijn642.fusion.model.MutableQuad;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.*;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fc;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Created 09/04/2025 by SuperMartijn642
 */
public class BaseItemModel implements ItemModel {

    private final List<ItemTintSource> tints;
    private final ModelRenderProperties properties;
    private final List<BakedQuad> mesh = new ArrayList<>();
    private final Supplier<Vector3fc[]> extents;
    private final boolean animated;
    private final Matrix4fc transformation;

    public BaseItemModel(List<ItemTintSource> tints, List<BaseModelQuad> quads, ModelRenderProperties properties, Matrix4fc transformation, ModelBaker.Interner interner){
        this.tints = tints;
        this.properties = properties;
        this.extents = Suppliers.memoize(() -> CuboidItemModelWrapper.computeExtents(quads.stream().map(BaseModelQuad::bakedQuad).toList()));

        // Create the item mesh
        @BakedQuad.MaterialFlags int flags = 0;
        MutableQuad mutableQuad = new MutableQuad();
        for(BaseModelQuad quad : quads){
            quad.fill(mutableQuad);
            this.mesh.add(mutableQuad.toBakedQuad(interner));
            flags |= quad.bakedQuad().materialInfo().flags();
        }

        // Check whether the quads contain animated textures
        this.animated = (flags & BakedQuad.FLAG_ANIMATED) != 0;

        this.transformation = transformation;
    }

    @Override
    public void update(ItemStackRenderState renderState, ItemStack stack, ItemModelResolver modelResolver, ItemDisplayContext displayContext, @Nullable ClientLevel level, @Nullable ItemOwner owner, int i){
        renderState.appendModelIdentityElement(this);
        if(this.animated)
            renderState.setAnimated();
        ItemStackRenderState.FoilType foilType = stack.hasFoil() ? CuboidItemModelWrapper.hasSpecialAnimatedTexture(stack) ? ItemStackRenderState.FoilType.SPECIAL : ItemStackRenderState.FoilType.STANDARD : null;
        if(foilType != null){
            renderState.setAnimated();
            renderState.appendModelIdentityElement(foilType);
        }
        ItemStackRenderState.LayerRenderState layer = renderState.newLayer();
        if(foilType != null)
            layer.setFoilType(foilType);
        if(!this.tints.isEmpty()){
            IntList tintLayers = layer.tintLayers();
            for(ItemTintSource tintSource : this.tints){
                int tint = tintSource.calculate(stack, level, owner == null ? null : owner.asLivingEntity());
                tintLayers.add(tint);
                renderState.appendModelIdentityElement(tint);
            }
        }
        layer.setExtents(this.extents);
        layer.setLocalTransform(this.transformation);
        this.properties.applyToLayer(layer, displayContext);
        layer.prepareQuadList().addAll(this.mesh);
    }
}
