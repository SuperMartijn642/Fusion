package com.supermartijn642.fusion.model.types.base;

import com.google.common.base.Suppliers;
import com.supermartijn642.fusion.FusionClient;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableMesh;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.item.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;
import java.util.function.Supplier;

/**
 * Created 09/04/2025 by SuperMartijn642
 */
public class BaseItemModel implements ItemModel {

    private final List<ItemTintSource> tints;
    private final ModelRenderProperties properties;
    private final Mesh mesh;
    private final Supplier<Vector3f[]> extents;
    private final boolean animated;

    public BaseItemModel(List<ItemTintSource> tints, List<BaseModelQuad> quads, ModelRenderProperties properties){
        this.tints = tints;
        this.properties = properties;
        this.extents = Suppliers.memoize(() -> BlockModelWrapper.computeExtents(quads.stream().map(BaseModelQuad::bakedQuad).toList()));

        // Create the item mesh
        MutableMesh builder = Renderer.get().mutableMesh();
        QuadEmitter emitter = builder.emitter();
        for(BaseModelQuad quad : quads){
            emitter.fromBakedQuad(quad.bakedQuad());
            emitter.cullFace(quad.cullDirection());
            FusionClient.applyMaterialProperties(emitter, null, quad.renderType(), quad.emissive());
            emitter.emit();
        }
        this.mesh = builder.immutableCopy();

        // Check whether the quads contain animated textures
        boolean animated = false;
        for(BaseModelQuad quad : quads){
            if(quad.bakedQuad().sprite().isAnimated()){
                animated = true;
                break;
            }
        }
        this.animated = animated;
    }

    @Override
    public void update(ItemStackRenderState renderState, ItemStack stack, ItemModelResolver modelResolver, ItemDisplayContext displayContext, @Nullable ClientLevel level, @Nullable LivingEntity entity, int i){
        renderState.appendModelIdentityElement(this);
        ItemStackRenderState.LayerRenderState layer = renderState.newLayer();
        if(stack.hasFoil()){
            ItemStackRenderState.FoilType foil = BlockModelWrapper.hasSpecialAnimatedTexture(stack) ? ItemStackRenderState.FoilType.SPECIAL : ItemStackRenderState.FoilType.STANDARD;
            layer.setFoilType(foil);
            renderState.setAnimated();
            renderState.appendModelIdentityElement(foil);
        }
        int tints = this.tints.size();
        int[] tintValues = layer.prepareTintLayers(tints);
        for(int j = 0; j < tints; j++){
            int tint = this.tints.get(j).calculate(stack, level, entity);
            tintValues[j] = tint;
            renderState.appendModelIdentityElement(tint);
        }
        layer.setExtents(this.extents);
        this.properties.applyToLayer(layer, displayContext);
        this.mesh.outputTo(layer.emitter());
        layer.setRenderType(Sheets.translucentItemSheet());
        if(this.animated)
            renderState.setAnimated();
    }
}
