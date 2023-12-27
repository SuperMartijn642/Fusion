package com.supermartijn642.fusion;

import net.neoforged.fml.IExtensionPoint;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

/**
 * Created 26/04/2023 by SuperMartijn642
 */
@Mod("fusion")
public class Fusion {

    public Fusion(){
        // Accept any version from the server
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(() -> "", (a, b) -> true));
        // Initialize Fusion stuff if this is on the client
        if(FMLEnvironment.dist.isClient())
            FusionClient.init();
    }
}
