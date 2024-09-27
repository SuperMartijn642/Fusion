package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.entity.model.FusionModelPart;
import com.supermartijn642.fusion.extensions.EntityRendererExtension;
import net.minecraft.client.renderer.entity.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

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
}
