package com.supermartijn642.fusion.util;

import com.supermartijn642.fusion.model.types.base.BaseBakedModel;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.client.resources.model.WeightedBakedModel;
import net.minecraftforge.client.model.data.ModelData;

/** Conservative eligibility check for skipping geometry which a renderer will discard. */
public final class HiddenFaceSkipping {

    private HiddenFaceSkipping(){}

    public interface WeightedVariants {
        boolean fusion$canSkipHiddenFaces(ModelData modelData);
    }

    public static boolean canSkip(BakedModel model, ModelData modelData, boolean allowWeighted){
        if(model == null || modelData == null)
            return false;
        if(model.getClass() == SimpleBakedModel.class)
            return true;
        if(model.getClass() == BaseBakedModel.class)
            return ((BaseBakedModel)model).canSkipHiddenFaces(modelData);
        return allowWeighted && model.getClass() == WeightedBakedModel.class
            && model instanceof WeightedVariants variants && variants.fusion$canSkipHiddenFaces(modelData);
    }
}
