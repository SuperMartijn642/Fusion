package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.api.model.ModelInstance;
import com.supermartijn642.fusion.extensions.BlockModelExtension;
import com.supermartijn642.fusion.model.FusionBlockModelData;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.resources.model.UnbakedGeometry;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Created 30/04/2023 by SuperMartijn642
 */
@Mixin(value = BlockModel.class, priority = 900)
public class BlockModelMixin implements BlockModelExtension {

    @Unique
    private ModelInstance<?> fusionModel;
    @Unique
    private FusionBlockModelData fusionData;

    @Override
    public ModelInstance<?> getFusionModel(){
        return this.fusionModel;
    }

    @Override
    public void setFusionModel(ModelInstance<?> fusionModel){
        this.fusionModel = fusionModel;
    }

    @Override
    public FusionBlockModelData getFusionData(){
        return this.fusionData;
    }

    @Override
    public void setFusionData(FusionBlockModelData data){
        this.fusionData = data;
    }

    @Inject(
        method = "geometry",
        at = @At("HEAD"),
        cancellable = true
    )
    public void geometry(CallbackInfoReturnable<UnbakedGeometry> ci){
        if(this.fusionData != null)
            ci.setReturnValue(this.fusionData.geometry());
    }

    @Inject(
        method = "guiLight",
        at = @At("HEAD"),
        cancellable = true
    )
    public void guiLight(CallbackInfoReturnable<UnbakedModel.GuiLight> ci){
        if(this.fusionData != null)
            ci.setReturnValue(this.fusionData.guiLight());
    }

    @Inject(
        method = "ambientOcclusion",
        at = @At("HEAD"),
        cancellable = true
    )
    public void ambientOcclusion(CallbackInfoReturnable<Boolean> ci){
        if(this.fusionData != null)
            ci.setReturnValue(this.fusionData.ambientOcclusion());
    }

    @Inject(
        method = "transforms",
        at = @At("HEAD"),
        cancellable = true
    )
    public void transforms(CallbackInfoReturnable<ItemTransforms> ci){
        if(this.fusionData != null)
            ci.setReturnValue(this.fusionData.transforms());
    }

    @Inject(
        method = "textureSlots",
        at = @At("HEAD"),
        cancellable = true
    )
    public void textureSlots(CallbackInfoReturnable<TextureSlots.Data> ci){
        if(this.fusionData != null)
            ci.setReturnValue(this.fusionData.textureSlots());
    }

    @Inject(
        method = "parent",
        at = @At("HEAD"),
        cancellable = true
    )
    public void parent(CallbackInfoReturnable<ResourceLocation> ci){
        if(this.fusionData != null)
            ci.setReturnValue(this.fusionData.parent());
    }
}
