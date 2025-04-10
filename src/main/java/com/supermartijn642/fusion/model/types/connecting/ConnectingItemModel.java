package com.supermartijn642.fusion.model.types.connecting;

import com.google.common.base.Suppliers;
import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.model.types.base.BaseModelQuad;
import com.supermartijn642.fusion.texture.types.connecting.ConnectingTextureSprite;
import com.supermartijn642.fusion.texture.types.connecting.layouts.ConnectingTextureLayoutHandler;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.item.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.RenderTypeHelper;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Created 09/04/2025 by SuperMartijn642
 */
public class ConnectingItemModel implements ItemModel {

    private final List<ItemTintSource> tints;
    private final ModelRenderProperties properties;
    private final List<Pair<RenderType,List<BakedQuad>>> mesh;
    private final Supplier<Vector3f[]> extents;

    public ConnectingItemModel(List<ItemTintSource> tints, List<ConnectingModelQuad> quads, ModelRenderProperties properties, RenderType forgeRenderType){
        this.tints = tints;
        this.properties = properties;
        this.extents = Suppliers.memoize(() -> BlockModelWrapper.computeExtents(quads.stream().map(BaseModelQuad::bakedQuad).toList()));

        // Create the item mesh
        Map<RenderType,List<BakedQuad>> mesh = new HashMap<>();
        OrientedMutableQuad mutableQuad = new OrientedMutableQuad();
        for(ConnectingModelQuad quad : quads){
            // Some layouts need auxiliary quads, hence simply repeat the quad that many times
            int auxiliaryQuadCount = quad.hasConnectingTexture() ? ConnectingTextureLayoutHandler.get(quad.getLayout()).getAuxiliaryQuadCount() : 0;
            // Submit the quads
            for(int quadIndex = 0; quadIndex < auxiliaryQuadCount + 1; quadIndex++){
                mutableQuad.fillFromBakedQuad(quad.bakedQuad());
                mutableQuad.emissive(quad.emissive());

                // Add the item quad
                RenderType renderType = FusionClient.getRenderTypeMaterial(quad.renderType());
                if(renderType == FusionClient.USE_ORIGINAL_RENDER_TYPE_MARKER && forgeRenderType != null)
                    renderType = forgeRenderType;
                if(renderType != FusionClient.USE_ORIGINAL_RENDER_TYPE_MARKER)
                    renderType = RenderTypeHelper.getEntityRenderType(renderType);
                List<BakedQuad> itemQuads = mesh.computeIfAbsent(renderType, k -> new ArrayList<>());
                // Process the quad if it has a connecting texture
                // As item mesh does not depend on state, we can run the connecting texture processing immediately
                if(quad.hasConnectingTexture()){
                    mutableQuad.set(ConnectingBakedModel.TextureOrientation.NORMAL_0.vertexIndexPermutation);
                    boolean keepQuad = ConnectingTextureLayoutHandler.get(quad.getLayout()).processItemQuad(quadIndex, mutableQuad, (ConnectingTextureSprite)quad.bakedQuad().sprite());
                    mutableQuad.resetPermutation();
                    if(!keepQuad)
                        continue;
                }
                itemQuads.add(mutableQuad.toBakedQuad());
            }
        }
        this.mesh = mesh.entrySet().stream()
            .map(entry -> Pair.of(entry.getKey(), entry.getValue()))
            .toList();
    }

    @Override
    public void update(ItemStackRenderState renderState, ItemStack stack, ItemModelResolver modelResolver, ItemDisplayContext displayContext, @Nullable ClientLevel level, @Nullable LivingEntity entity, int i){
        ItemStackRenderState.FoilType foilType = stack.hasFoil() ? BlockModelWrapper.hasSpecialAnimatedTexture(stack) ? ItemStackRenderState.FoilType.SPECIAL : ItemStackRenderState.FoilType.STANDARD : null;
        int tints = this.tints.size();
        int[] tintValues = new int[tints];
        for(int j = 0; j < tints; j++)
            tintValues[j] = this.tints.get(j).calculate(stack, level, entity);
        for(Pair<RenderType,List<BakedQuad>> pair : this.mesh){
            ItemStackRenderState.LayerRenderState layer = renderState.newLayer();
            if(foilType != null)
                layer.setFoilType(foilType);
            System.arraycopy(tintValues, 0, layer.prepareTintLayers(tints), 0, tintValues.length);
            layer.setExtents(this.extents);
            this.properties.applyToLayer(layer, displayContext);
            layer.prepareQuadList().addAll(pair.right());
            RenderType renderType = pair.left();
            if(renderType == FusionClient.USE_ORIGINAL_RENDER_TYPE_MARKER)
                renderType = ItemBlockRenderTypes.getRenderType(stack);
            layer.setRenderType(renderType);
        }
    }
}
