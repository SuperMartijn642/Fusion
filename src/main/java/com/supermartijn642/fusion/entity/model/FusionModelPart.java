package com.supermartijn642.fusion.entity.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.supermartijn642.fusion.entity.EntityRenderTypeHelper;
import com.supermartijn642.fusion.entity.VanillaModelLayerProperties;
import com.supermartijn642.fusion.extensions.BufferSourceExtension;
import com.supermartijn642.fusion.extensions.EntityExtension;
import com.supermartijn642.fusion.util.Triple;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/**
 * Created 29/09/2024 by SuperMartijn642
 */
public class FusionModelPart extends SubModelPart {

    private final int layerIndex;
    private final ModelPart original;
    private VanillaModelLayerProperties vanillaProperties;
    private EntityLayerProperties properties;

    private boolean ready;
    private MultiBufferSource bufferSource;
    private ResourceLocation currentTexture;
    private Float currentScaling;

    private RenderType adjustedRenderType;

    public FusionModelPart(int layerIndex, ModelPart original){
        super(null);
        this.mainPart = this;
        this.layerIndex = layerIndex;
        this.original = original;
    }

    public void setProperties(EntityLayerProperties properties, VanillaModelLayerProperties vanillaProperties){
        this.properties = properties;
        this.vanillaProperties = vanillaProperties;
        // If no properties, use the original model
        if(properties == null)
            this.mirror(this.original);
    }

    public void setup(Entity entity, MultiBufferSource bufferSource){
        if(this.properties == null)
            return;
        this.bufferSource = bufferSource;

        // Check if the cached model choice should be recomputed
        if(((EntityExtension)entity).shouldFusionRecomputeModel(this.layerIndex)){
            Triple<ModelPart,ResourceLocation,Float> model = this.properties.chooseModel(entity);
            ((EntityExtension)entity).setFusionModel(this.layerIndex, model);
        }

        // Get the cached model choice
        Triple<ModelPart,ResourceLocation,Float> modelChoice = ((EntityExtension)entity).getFusionModel(this.layerIndex);
        ModelPart currentModel = modelChoice.left();
        this.currentTexture = modelChoice.middle();
        this.currentScaling = modelChoice.right();

        // Reset the model to its initial state
        resetPose(currentModel);

        // Copy all properties to this model
        this.mirror(currentModel);

        this.ready = true;
    }

    public void clear(){
        if(this.properties == null)
            return;

        this.bufferSource = null;
        this.currentTexture = null;
        this.currentScaling = null;
        this.adjustedRenderType = null;
    }

    @Override
    public void render(PoseStack poseStack, VertexConsumer vertexConsumer, int i, int j, float f, float g, float h, float k){
        this.renderPart(this, poseStack, vertexConsumer, i, j, f, g, h, k);
    }

    public void renderPart(SubModelPart part, PoseStack poseStack, VertexConsumer vertexConsumer, int i, int j, float f, float g, float h, float k){
        if(!this.visible) return;
        if(!this.ready){
            if(part == this)
                this.original.render(poseStack, vertexConsumer, i, j, f, g, h, k);
            return;
        }

        // Get render type for current texture
        if(this.bufferSource != null && this.currentTexture != null)
            vertexConsumer = this.adjustTexture(vertexConsumer, this.bufferSource);

        poseStack.pushPose();
        this.vanillaProperties.transform(poseStack);
        if(this.currentScaling != null)
            poseStack.scale(this.currentScaling, this.currentScaling, this.currentScaling);
        part.renderInternal(poseStack, vertexConsumer, i, j, f, g, h, k);
        poseStack.popPose();
    }

    private static void resetPose(ModelPart part){
        part.resetPose();
        part.children.values().forEach(FusionModelPart::resetPose);
    }

    private VertexConsumer adjustTexture(VertexConsumer buffer, MultiBufferSource bufferSource){
        // Obtain the current render type
        if(!(bufferSource instanceof BufferSourceExtension))
            return buffer;
        RenderType renderType = ((BufferSourceExtension)bufferSource).fusionGetLastRenderType();
        if(this.adjustedRenderType != null && this.adjustedRenderType == renderType)
            return bufferSource.getBuffer(renderType);
        if(!(renderType instanceof RenderType.CompositeRenderType))
            return buffer;

        // Check what texture the render type uses
        RenderStateShard.EmptyTextureStateShard textureState = ((RenderType.CompositeRenderType)renderType).state.textureState;
        if(!(textureState instanceof RenderStateShard.TextureStateShard))
            return buffer;
        ResourceLocation texture = ((RenderStateShard.TextureStateShard)textureState).texture.orElse(null);
        if(this.currentTexture.equals(texture)) // If the texture already matches the model's texture, just use the original buffer
            return buffer;

        // Get the same render type, but with the model's texture
        renderType = EntityRenderTypeHelper.getRenderTypeWithTexture(renderType, this.currentTexture);
        if(renderType == null)
            return buffer;

        // Request a new buffer the new render type
        this.adjustedRenderType = renderType;
        return bufferSource.getBuffer(renderType);
    }
}
