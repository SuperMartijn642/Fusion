package com.supermartijn642.fusion;

import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;

/**
 * Created 26/04/2023 by SuperMartijn642
 */
@Mod(modid = "@mod_id@", name = "@mod_name@", version = "@mod_version@", dependencies = "required-after:forge@@forge_dependency@", clientSideOnly = true)
public class Fusion {

    public Fusion(){
        // Initialize Fusion stuff if this is on the client
        if(FMLCommonHandler.instance().getSide().isClient())
            FusionClient.init();
    }
}
