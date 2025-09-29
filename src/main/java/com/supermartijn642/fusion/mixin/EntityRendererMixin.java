package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.entity.model.FusionModelPart;
import com.supermartijn642.fusion.extensions.EntityRenderStateExtension;
import com.supermartijn642.fusion.extensions.EntityRendererExtension;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Created 29/09/2024 by SuperMartijn642
 */
@Mixin(EntityRenderer.class)
public class EntityRendererMixin implements EntityRendererExtension {

    @Unique
    private List<FusionModelPart> parts;

    @Override
    public List<FusionModelPart> getFusionModelParts(){
        return this.parts;
    }

    @Override
    public void setFusionModelParts(List<FusionModelPart> parts){
        this.parts = parts;
    }

    @Inject(
        method = "createRenderState(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;",
        at = @At("RETURN")
    )
    private void createRenderState(Entity entity, float partialTicks, CallbackInfoReturnable<EntityRenderState> ci){
        if(this.parts == null)
            return;
        EntityRenderState state = ci.getReturnValue();
        if(state == null)
            return;
        // Extract state for each part
        for(FusionModelPart part : this.parts)
            part.extractState(entity, (EntityRenderStateExtension)state);
    }
}
