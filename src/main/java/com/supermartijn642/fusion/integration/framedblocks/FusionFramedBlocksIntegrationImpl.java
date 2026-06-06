package com.supermartijn642.fusion.integration.framedblocks;

import net.minecraftforge.client.model.data.ModelData;
import xfacthd.framedblocks.client.data.ConTexDataHandler;

/**
 * Created 06/06/2026 by SuperMartijn642
 */
public class FusionFramedBlocksIntegrationImpl {

    public static Object getCacheValue(ModelData modelData){
        return ConTexDataHandler.extractConTexData(modelData);
    }
}
