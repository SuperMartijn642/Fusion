package com.supermartijn642.fusion.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.Transparency;
import com.supermartijn642.fusion.texture.types.base.BaseTextureSprite;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.model.cuboid.FaceBakery;
import net.minecraft.client.resources.model.sprite.Material;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FaceBakery.class)
public class FaceBakeryMixin {
    @WrapOperation(method = "computeMaterialTransparency", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/SpriteContents;computeTransparency(FFFF)Lcom/mojang/blaze3d/platform/Transparency;"))
    private static Transparency computeMaterialTransparency(SpriteContents instance, float u0, float v0, float u1, float v1, Operation<Transparency> original, Material.Baked material){
        if(material.sprite() instanceof BaseTextureSprite sprite){
            return sprite.computeTransparency(u0, v0, u1, v1);
        }
        return original.call(instance, u0, v0, u1, v1);
    }
}
