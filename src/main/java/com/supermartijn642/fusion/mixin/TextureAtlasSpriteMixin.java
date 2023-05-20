package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.extensions.TextureAtlasSpriteExtension;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Created 26/04/2023 by SuperMartijn642
 */
@Mixin(TextureAtlasSprite.class)
public class TextureAtlasSpriteMixin implements TextureAtlasSpriteExtension {

    @Unique
    private TextureType<?> fusionType;

    @Override
    public void setFusionTextureType(TextureType<?> type){
        this.fusionType = type;
    }

    @Override
    public TextureType<?> getFusionTextureType(){
        return this.fusionType;
    }
}
