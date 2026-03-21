package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.texture.TextureCreationHandler;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.Stitcher;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Map;

/**
 * Created 21/05/2023 by SuperMartijn642
 */
@Mixin(value = SpriteLoader.class, priority = 900)
public class SpriteLoaderMixin {

    @ModifyVariable(
        method = "stitch",
        at = @At("HEAD"),
        ordinal = 0
    )
    private List<SpriteContents> flattenSprites(List<SpriteContents> sprites){
        return TextureCreationHandler.onStitchSprites(sprites);
    }

    @Inject(
        method = "getStitchedSprites",
        at = @At("RETURN")
    )
    private void initializeTextures(Stitcher<SpriteContents> stitcher, int atlasWidth, int atlasHeight, CallbackInfoReturnable<Map<ResourceLocation,TextureAtlasSprite>> ci){
        TextureCreationHandler.afterLoadSprites(ci.getReturnValue(), atlasWidth, atlasHeight, stitcher);
    }
}
