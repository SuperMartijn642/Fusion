package com.supermartijn642.fusion.mixin.forge;

import com.supermartijn642.fusion.api.model.custom.UntypedModelInstance;
import com.supermartijn642.fusion.model.FusionBlockModelData;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelBlock;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.Attributes;
import net.minecraftforge.common.model.IModelState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Function;

/**
 * Created 12/06/2026 by SuperMartijn642
 */
@Mixin(targets = "net.minecraftforge.client.model.ModelLoader$VanillaModelWrapper", remap = false)
public class VanillaModelWrapperMixin {

    @Final
    @Shadow
    private ResourceLocation location;
    @Final
    @Shadow
    private ModelBlock model;

    @Inject(
        method = "bakeImpl",
        at = @At("HEAD"),
        cancellable = true
    )
    private void bakeImpl(IModelState modelState, VertexFormat vertexFormat, Function<ResourceLocation,TextureAtlasSprite> spriteGetter, CallbackInfoReturnable<IBakedModel> ci){
        if(!Attributes.moreSpecific(vertexFormat, Attributes.DEFAULT_BAKED_FORMAT) || this.model == null)
            return;

        // Handle Fusion models and models with Fusion textures
        if(FusionBlockModelData.containsFusionModelsOrTextures(this.model, spriteGetter)){
            FusionBlockModelData fusionData = FusionBlockModelData.get(this.model);
            if(fusionData == null){
                UntypedModelInstance model = FusionBlockModelData.getModelInstance(this.model);
                fusionData = new FusionBlockModelData(this.location, model);
                fusionData.resolve();
            }
            ci.setReturnValue(fusionData.bake(modelState, vertexFormat, spriteGetter));
        }
    }
}
