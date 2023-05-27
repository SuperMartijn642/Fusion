package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.extensions.BlockModelExtension;
import net.minecraft.client.renderer.block.model.ModelBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.animation.ModelBlockAnimation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Created 24/05/2023 by SuperMartijn642
 */
@Mixin(targets = "net.minecraftforge.client.model.ModelLoader$VanillaModelWrapper", priority = 900, remap = false)
public class VanillaModelWrapperMixin {

    @Inject(
        method = "<init>(Lnet/minecraftforge/client/model/ModelLoader;Lnet/minecraft/util/ResourceLocation;Lnet/minecraft/client/renderer/block/model/ModelBlock;ZLnet/minecraftforge/client/model/animation/ModelBlockAnimation;)V",
        at = @At("RETURN")
    )
    private void init(ModelLoader loader, ResourceLocation location, ModelBlock model, boolean uvlock, ModelBlockAnimation animation, CallbackInfo ci){
        ((BlockModelExtension)model).setWrapper((IModel)this);
    }
}
