package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.api.texture.SpriteHelper;
import net.minecraft.client.renderer.texture.Stitcher;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Created 25/10/2024 by SuperMartijn642
 */
@Mixin(Stitcher.Holder.class)
public class StitcherHolderMixin {

    @Unique
    private boolean preventRotating;

    @Inject(
        method = "<init>",
        at = @At("TAIL")
    )
    private void init(TextureAtlasSprite sprite, int mipmapLevel, CallbackInfo ci){
        this.preventRotating = !SpriteHelper.getTextureType(sprite).allowRotation();
    }

    @Inject(
        method = "rotate",
        at = @At("HEAD"),
        cancellable = true
    )
    private void rotate(CallbackInfo ci){
        if(this.preventRotating)
            ci.cancel();
    }
}
