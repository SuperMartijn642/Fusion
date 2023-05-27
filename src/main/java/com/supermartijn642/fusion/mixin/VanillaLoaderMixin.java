package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.model.FusionBlockModel;
import com.supermartijn642.fusion.model.types.vanilla.VanillaModelType;
import net.minecraft.client.renderer.block.model.ModelBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.animation.ModelBlockAnimation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

/**
 * Created 26/05/2023 by SuperMartijn642
 */
@Mixin(targets = "net.minecraftforge.client.model.ModelLoader$VanillaLoader", priority = 900, remap = false)
public class VanillaLoaderMixin {

    @Shadow(remap = false)
    private ModelLoader loader;

    @Inject(
        method = "loadModel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraftforge/client/model/ModelLoader$VanillaModelWrapper;<init>(Lnet/minecraftforge/client/model/ModelLoader;Lnet/minecraft/util/ResourceLocation;Lnet/minecraft/client/renderer/block/model/ModelBlock;ZLnet/minecraftforge/client/model/animation/ModelBlockAnimation;)V",
            shift = At.Shift.AFTER
        ),
        locals = LocalCapture.CAPTURE_FAILHARD,
        cancellable = true
    )
    private void loadModel(ResourceLocation location, CallbackInfoReturnable<IModel> ci, String modelPath, ResourceLocation armatureLocation, ModelBlockAnimation animation, ModelBlock model){
        if(model instanceof FusionBlockModel){
            ci.setReturnValue(((FusionBlockModel)model));
            VanillaModelType.modelLoader = this.loader;
        }
    }
}
