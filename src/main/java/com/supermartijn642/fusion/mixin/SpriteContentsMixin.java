package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.extensions.SpriteContentsExtension;
import net.minecraft.client.renderer.texture.SpriteContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Created 15/07/2025 by SuperMartijn642
 */
@Mixin(SpriteContents.class)
public class SpriteContentsMixin implements SpriteContentsExtension {

    @Unique
    private Pair<TextureType<Object>,Object> fusionMetadata;

    @Override
    public void setFusionMetadata(Pair<TextureType<Object>,Object> metadata){
        this.fusionMetadata = metadata;
    }

    @Override
    public Pair<TextureType<Object>,Object> getFusionMetadata(){
        return this.fusionMetadata;
    }
}
