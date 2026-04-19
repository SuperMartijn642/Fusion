package com.supermartijn642.fusion.model.types.connecting;

import com.google.common.base.Suppliers;
import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.model.types.base.BaseModelQuad;
import com.supermartijn642.fusion.texture.types.connecting.StitchedConnectingTextureData;
import com.supermartijn642.fusion.texture.types.connecting.layouts.ConnectingTextureLayoutHandler;
import it.unimi.dsi.fastutil.ints.IntList;
import net.fabricmc.fabric.api.client.renderer.v1.Renderer;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableMesh;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.*;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fc;
import org.joml.Vector3fc;

import java.util.List;
import java.util.function.Supplier;

/**
 * Created 09/04/2025 by SuperMartijn642
 */
public class ConnectingItemModel implements ItemModel {

    private final List<ItemTintSource> tints;
    private final ModelRenderProperties properties;
    private final Matrix4fc transformation;
    private final Mesh mesh;
    private final Supplier<Vector3fc[]> extents;
    private final boolean animated;

    public ConnectingItemModel(List<ItemTintSource> tints, List<ConnectingModelQuad> quads, ModelRenderProperties properties, Matrix4fc transformation){
        this.tints = tints;
        this.properties = properties;
        this.transformation = transformation;
        this.extents = Suppliers.memoize(() -> CuboidItemModelWrapper.computeExtents(quads.stream().map(BaseModelQuad::bakedQuad).toList()));

        // Create the item mesh
        MutableMesh builder = Renderer.get().mutableMesh();
        QuadEmitter emitter = builder.emitter();
        OrientedMutableQuad mutableQuad = new OrientedMutableQuad();
        for(ConnectingModelQuad quad : quads){
            // Some layouts need auxiliary quads, hence simply repeat the quad that many times
            int auxiliaryQuadCount = 0;
            ConnectingTextureLayoutHandler layoutHandler = null;
            if(quad.hasConnectingTexture()){
                layoutHandler = ConnectingTextureLayoutHandler.get(quad.getLayout());
                // Get the number of auxiliary quads needed
                auxiliaryQuadCount = layoutHandler.getAuxiliaryQuadCount();
            }
            // Process and submit the quads
            for(int quadIndex = 0; quadIndex < auxiliaryQuadCount + 1; quadIndex++){
                emitter.fromBakedQuad(quad.bakedQuad());
                emitter.cullFace(quad.cullDirection());
                FusionClient.applyMaterialProperties(emitter, null, quad.renderType(), quad.emissive());
                // Process the quad if it has a connecting texture
                // As item mesh does not depend on state, we can run the connecting texture processing immediately
                if(layoutHandler != null){
                    mutableQuad.set(emitter);
                    mutableQuad.set(ConnectingBakedModel.TextureOrientation.NORMAL_0.vertexIndexPermutation);
                    boolean keepQuad = layoutHandler.processItemQuad(quadIndex, mutableQuad, quad.spriteInstance(), (StitchedConnectingTextureData)quad.spriteInstance().getTexture().getCustomData());
                    if(!keepQuad)
                        continue;
                }
                emitter.emit();
            }
        }
        this.mesh = builder.immutableCopy();

        // Check whether the quads contain animated textures
        boolean animated = false;
        for(BaseModelQuad quad : quads){
            //noinspection resource
            if(quad.bakedQuad().materialInfo().sprite().contents().isAnimated()){
                animated = true;
                break;
            }
        }
        this.animated = animated;
    }

    @Override
    public void update(ItemStackRenderState renderState, ItemStack stack, ItemModelResolver modelResolver, ItemDisplayContext displayContext, @Nullable ClientLevel level, @Nullable ItemOwner owner, int i){
        renderState.appendModelIdentityElement(this);
        ItemStackRenderState.LayerRenderState layer = renderState.newLayer();
        if(stack.hasFoil()){
            ItemStackRenderState.FoilType foil = CuboidItemModelWrapper.hasSpecialAnimatedTexture(stack) ? ItemStackRenderState.FoilType.SPECIAL : ItemStackRenderState.FoilType.STANDARD;
            layer.setFoilType(foil);
            renderState.setAnimated();
            renderState.appendModelIdentityElement(foil);
        }
        if(!this.tints.isEmpty()){
            IntList tintValues = layer.tintLayers();
            for(ItemTintSource tinting : this.tints){
                int tint = tinting.calculate(stack, level, owner == null ? null : owner.asLivingEntity());
                tintValues.add(tint);
                renderState.appendModelIdentityElement(tint);
            }
        }
        layer.setExtents(this.extents);
        layer.setLocalTransform(this.transformation);
        this.properties.applyToLayer(layer, displayContext);
        this.mesh.outputTo(layer.emitter());
        if(this.animated)
            renderState.setAnimated();
    }
}
