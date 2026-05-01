package com.supermartijn642.fusion.extensions;

import net.minecraft.client.resources.model.geometry.BakedQuad;

/**
 * Created 01/05/2026 by SuperMartijn642
 */
public interface MaterialInfoExtension {

    static boolean getAmbientOcclusion(BakedQuad.MaterialInfo materialInfo) {
        //noinspection DataFlowIssue
        return ((MaterialInfoExtension)(Object)materialInfo).getFusionAmbientOcclusion();
    }

    static void setAmbientOcclusion(BakedQuad.MaterialInfo materialInfo, boolean ambientOcclusion) {
        //noinspection DataFlowIssue
        ((MaterialInfoExtension)(Object)materialInfo).setFusionAmbientOcclusion(ambientOcclusion);
    }

    void setFusionAmbientOcclusion(boolean ambientOcclusion);

    boolean getFusionAmbientOcclusion();
}
