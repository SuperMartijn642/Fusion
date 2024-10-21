package com.supermartijn642.fusion.extensions;

import com.supermartijn642.fusion.resources.FusionPackMetadata;
import org.jetbrains.annotations.Nullable;

/**
 * Created 21/10/2024 by SuperMartijn642
 */
public interface PackExtension {

    @Nullable FusionPackMetadata getFusionMetadata();
}
