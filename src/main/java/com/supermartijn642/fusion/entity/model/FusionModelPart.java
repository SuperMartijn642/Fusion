package com.supermartijn642.fusion.entity.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.supermartijn642.fusion.entity.EntityRenderTypeHelper;
import com.supermartijn642.fusion.entity.VanillaModelLayerProperties;
import com.supermartijn642.fusion.extensions.EntityExtension;
import com.supermartijn642.fusion.extensions.EntityRenderStateExtension;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;

import java.util.function.Function;

/**
 * Created 29/09/2024 by SuperMartijn642
 */
public class FusionModelPart extends SubModelPart {

    public static final ThreadLocal<RenderContext> RENDER_CONTEXT = new ThreadLocal<>();

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
        RenderContext renderContext = RENDER_CONTEXT.get();
        if(renderContext == null)
            return;
        EntityLayerProperties.ModelChoice modelChoice = renderContext.entityState.getFusionModel(this.layerIndex);
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
            RenderContext renderContext = RENDER_CONTEXT.get();
            if(renderContext != null)
                vertexConsumer = this.adjustTexture(renderContext);
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

    private VertexConsumer adjustTexture(RenderContext renderContext){
        // Get the current render type
        RenderType renderType = renderContext.renderType;
        if(this.adjustedRenderType != null && this.adjustedRenderType == renderType)
            return renderContext.buffer.apply(renderType);

        // Get the same render type, but with the model's texture
        renderType = EntityRenderTypeHelper.getRenderTypeWithTexture(renderType, this.currentTexture);
        if(renderType == null)
            return renderContext.buffer.apply(renderContext.renderType);

        // Request a new buffer the new render type
        this.adjustedRenderType = renderType;
        return renderContext.buffer.apply(renderType);
    }

    public record RenderContext(EntityRenderStateExtension entityState, RenderType renderType, Function<RenderType,VertexConsumer> buffer) {
    }
}
