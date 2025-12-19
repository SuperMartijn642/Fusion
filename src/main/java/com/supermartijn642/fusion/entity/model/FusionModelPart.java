package com.supermartijn642.fusion.entity.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.supermartijn642.fusion.entity.EntityRenderTypeHelper;
import com.supermartijn642.fusion.entity.VanillaModelLayerProperties;
import com.supermartijn642.fusion.extensions.BufferSourceExtension;
import com.supermartijn642.fusion.extensions.EntityExtension;
import com.supermartijn642.fusion.extensions.EntityRenderStateExtension;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;

/**
 * Created 29/09/2024 by SuperMartijn642
 */
public class FusionModelPart extends SubModelPart {

    public static final ThreadLocal<MultiBufferSource.BufferSource> BUFFER_SOURCE_CONTEXT = new ThreadLocal<>();
    public static final ThreadLocal<EntityRenderStateExtension> RENDER_STATE_CONTEXT = new ThreadLocal<>();

    private final int layerIndex;
    private final ModelPart original;
    private VanillaModelLayerProperties vanillaProperties;
    private EntityLayerProperties properties;

    private boolean ready;
    private Identifier currentTexture;
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

    public void extractState(Entity entity, EntityRenderStateExtension state){
        if(this.properties == null)
            return;
        // Check if the cached model choice should be recomputed
        EntityLayerProperties.ModelChoice model;
        if(((EntityExtension)entity).shouldFusionRecomputeModel(this.layerIndex)){
            model = this.properties.chooseModel(entity);
            ((EntityExtension)entity).setFusionModel(this.layerIndex, model);
        }else
            model = ((EntityExtension)entity).getFusionModel(this.layerIndex);
        state.setFusionModel(this.layerIndex, model);
    }

    public void setup(){
        if(this.properties == null)
            return;

        // Get the model from render state context
        EntityRenderStateExtension state = RENDER_STATE_CONTEXT.get();
        if(state == null)
            return;
        EntityLayerProperties.ModelChoice modelChoice = state.getFusionModel(this.layerIndex);
        if(modelChoice == null)
            return;
        ModelPart currentModel = modelChoice.model();
        this.currentTexture = modelChoice.texture();
        this.currentScaling = modelChoice.scaling();

        // Reset the model to its initial state
        resetPose(currentModel);

        // Copy all properties to this model
        this.mirror(currentModel);

        this.ready = true;
    }

    public void clear(){
        if(this.properties == null)
            return;

        this.currentTexture = null;
        this.currentScaling = null;
        this.adjustedRenderType = null;
        this.ready = false;
    }

    @Override
    public void render(PoseStack poseStack, VertexConsumer vertexConsumer, int i, int j, int k){
        this.renderPart(this, poseStack, vertexConsumer, i, j, k);
    }

    public void renderPart(SubModelPart part, PoseStack poseStack, VertexConsumer vertexConsumer, int i, int j, int k){
        if(!this.visible) return;
        this.setup();
        if(!this.ready){
            if(part == this)
                this.original.render(poseStack, vertexConsumer, i, j, k);
            return;
        }

        // Get render type for current texture
        if(this.currentTexture != null){
            MultiBufferSource.BufferSource bufferSource = BUFFER_SOURCE_CONTEXT.get();
            if(bufferSource != null)
                vertexConsumer = this.adjustTexture(vertexConsumer, bufferSource);
        }

        poseStack.pushPose();
        this.vanillaProperties.transform(poseStack);
        if(this.currentScaling != null)
            poseStack.scale(this.currentScaling, this.currentScaling, this.currentScaling);
        part.renderInternal(poseStack, vertexConsumer, i, j, k);
        poseStack.popPose();
        this.clear();
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
        // Check what texture the render type uses
        RenderSetup.TextureBinding sampler0 = renderType.state.textures.get("Sampler0");
        if(sampler0 == null)
            return buffer;
        Identifier texture = sampler0.location();
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
