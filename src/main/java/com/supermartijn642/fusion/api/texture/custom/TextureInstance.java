package com.supermartijn642.fusion.api.texture.custom;

import com.supermartijn642.fusion.api.model.custom.quad.MutableQuad;
import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.util.PropertyStore;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * Instance of a texture whose sprites have been stitched.
 * <p>
 * Created 22/03/2026 by SuperMartijn642
 */
@ApiStatus.NonExtendable
public interface TextureInstance<X> {

    /**
     * Gets the type of this texture instance.
     */
    TextureType<?,X> getTextureType();

    /**
     * Gets the identifier of this texture.
     */
    ResourceLocation getIdentifier();

    /**
     * Gets the sprites associated with this texture.
     */
    List<SpriteInstance> getSprites();

    /**
     * Gets the default sprite for this texture.
     */
    SpriteInstance getDefaultSprite();

    /**
     * Gets the custom data from this texture.
     * @see TextureOutput#setCustomData(Object)
     */
    X getCustomData();

    /**
     * Gets the render types that may result from initializing or processing a quad with the given sprite.
     * The default render type for the block does not have to be included.
     * @param sprite sprite of the quad
     * @return the smallest set that contains all render types that may result from initializing or processing a quad with the given sprite
     */
    default Set<RenderType> getBlockStateRenderTypes(SpriteInstance sprite){
        return this.getTextureType().getBlockStateRenderTypes(sprite, this.getCustomData());
    }

    /**
     * Configures the given quad in a baked model.
     * One can return a quad processor to adjust the quad based on the render context.
     * @param quad       quad properties to configure
     * @param sprite     sprite of the quad
     * @param properties property store containing model properties as well as shared properties. May be used to store data shared between multiple quads. Any stored properties will be available to quad processors.
     * @return a processor to adjust the quad based on the render context
     */
    @Nullable
    default QuadProcessor<?> initializeModelQuad(MutableQuad quad, SpriteInstance sprite, PropertyStore properties){
        return this.getTextureType().initializeModelQuad(quad, sprite, this.getCustomData(), properties);
    }
}
