package com.supermartijn642.fusion.texture.types.connecting;

import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.api.texture.data.ConnectingTextureData;
import com.supermartijn642.fusion.api.texture.data.ConnectingTextureLayout;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;

/**
 * Created 30/04/2023 by SuperMartijn642
 */
public class ConnectingTextureSprite extends TextureAtlasSprite {

    private final ConnectingTextureLayout layout;
    private final ConnectingTextureData.RenderType renderType;

    protected ConnectingTextureSprite(TextureAtlasSprite original, ConnectingTextureLayout layout, ConnectingTextureData.RenderType renderType){
        super(
            original.atlas(),
            new Info(original.getName(), original.getWidth(), original.getHeight(), AnimationMetadataSection.EMPTY),
            0,
            1,
            1,
            original.getX(),
            original.getY(),
            FusionClient.getDummyImage()
        );
        this.layout = layout;
        this.renderType = renderType;
        this.mainImage = original.mainImage;
        this.animatedTexture = original.animatedTexture;
        this.u0 = original.u0;
        this.u1 = original.u1;
        this.v0 = original.v0;
        this.v1 = original.v1;
    }

    public ConnectingTextureLayout getLayout(){
        return this.layout;
    }

    public ConnectingTextureData.RenderType getRenderType(){
        return this.renderType;
    }
}
