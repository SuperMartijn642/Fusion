package com.supermartijn642.fusion.texture.custom;

import com.supermartijn642.fusion.api.texture.custom.AllocatedSprite;
import net.minecraft.resources.ResourceLocation;

/**
 * Created 20/03/2026 by SuperMartijn642
 */
public record AllocatedSpriteImpl(ResourceLocation identifier,
                                  int x, int y, int width, int height,
                                  float u0, float u1, float v0, float v1) implements AllocatedSprite {
}
