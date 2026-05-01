package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.api.model.ModelInstance;
import com.supermartijn642.fusion.extensions.CuboidModelExtension;
import com.supermartijn642.fusion.model.FusionBlockModel;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.client.resources.model.cuboid.CuboidModel;
import net.minecraft.client.resources.model.cuboid.ItemTransforms;
import net.minecraft.client.resources.model.geometry.UnbakedGeometry;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Created 30/04/2023 by SuperMartijn642
 */
@Mixin(value = CuboidModel.class, priority = 900)
public class CuboidModelMixin implements CuboidModelExtension {

    @Unique
    private ModelInstance<?> fusionModel;
    @Unique
    private FusionBlockModel fusionBlockModelData;

    @Override
    public ModelInstance<?> getFusionModel(){
        return this.fusionModel;
    }

    @Override
    public void setFusionModel(ModelInstance<?> fusionModel){
        this.fusionModel = fusionModel;
    }

    @Override
    public FusionBlockModel getFusionBlockModelData(){
        return this.fusionBlockModelData;
    }

    @Override
    public void setFusionBlockModelData(FusionBlockModel data){
        this.fusionBlockModelData = data;
    }

    @Inject(
        method = "geometry",
        at = @At("HEAD"),
        cancellable = true
    )
    public void geometry(CallbackInfoReturnable<UnbakedGeometry> ci){
        if(this.fusionBlockModelData != null)
            ci.setReturnValue(this.fusionBlockModelData.geometry());
    }

    @Inject(
        method = "guiLight",
        at = @At("HEAD"),
        cancellable = true
    )
    public void guiLight(CallbackInfoReturnable<UnbakedModel.GuiLight> ci){
        if(this.fusionBlockModelData != null)
            ci.setReturnValue(this.fusionBlockModelData.guiLight());
    }

    @Inject(
        method = "ambientOcclusion",
        at = @At("HEAD"),
        cancellable = true
    )
    public void ambientOcclusion(CallbackInfoReturnable<Boolean> ci){
        if(this.fusionBlockModelData != null)
            ci.setReturnValue(this.fusionBlockModelData.ambientOcclusion());
    }

    @Inject(
        method = "transforms",
        at = @At("HEAD"),
        cancellable = true
    )
    public void transforms(CallbackInfoReturnable<ItemTransforms> ci){
        if(this.fusionBlockModelData != null)
            ci.setReturnValue(this.fusionBlockModelData.transforms());
    }

    @Inject(
        method = "textureSlots",
        at = @At("HEAD"),
        cancellable = true
    )
    public void textureSlots(CallbackInfoReturnable<TextureSlots.Data> ci){
        if(this.fusionBlockModelData != null)
            ci.setReturnValue(this.fusionBlockModelData.textureSlots());
    }

    @Inject(
        method = "parent",
        at = @At("HEAD"),
        cancellable = true
    )
    public void parent(CallbackInfoReturnable<Identifier> ci){
        if(this.fusionBlockModelData != null)
            ci.setReturnValue(this.fusionBlockModelData.parent());
    }
}
