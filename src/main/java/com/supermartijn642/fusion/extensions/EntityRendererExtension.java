package com.supermartijn642.fusion.extensions;

import com.supermartijn642.fusion.entity.model.FusionModelPart;

import java.util.List;

/**
 * Created 29/09/2024 by SuperMartijn642
 */
public interface EntityRendererExtension {

    List<FusionModelPart> getFusionModelParts();

    void setFusionModelParts(List<FusionModelPart> parts);
}
