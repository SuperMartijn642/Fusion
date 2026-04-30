package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.api.texture.custom.SpriteInstance;
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
    private SpriteInstance fusionSpriteInstance;

    @Override
    public void setFusionSpriteInstance(SpriteInstance instance){
        this.fusionSpriteInstance = instance;
    }

    @Override
    public SpriteInstance getFusionSpriteInstance(){
        return this.fusionSpriteInstance;
    }
}
