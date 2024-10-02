package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.extensions.ModelPartExtension;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;

/**
 * Created 18/09/2024 by SuperMartijn642
 */
@Mixin(ModelPart.class)
public class ModelPartMixin implements ModelPartExtension {

    @Shadow
    private Map<String,ModelPart> children;

    @Override
    public boolean hasFusionChild(String name){
        return this.children.containsKey(name);
    }
}
