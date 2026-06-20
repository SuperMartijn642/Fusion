package com.supermartijn642.fusion.mixin;

import com.mojang.math.Transformation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Created 21/06/2026 by SuperMartijn642
 */
@Mixin(Transformation.class)
public interface TransformationAccessor {

    @Accessor("decomposed")
    boolean fusion$getDecomposed();
}
