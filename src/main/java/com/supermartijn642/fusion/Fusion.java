package com.supermartijn642.fusion;

import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * Created 26/04/2023 by SuperMartijn642
 */
@Mod("fusion")
public class Fusion {

    public Fusion(FMLJavaModLoadingContext context){
        // Accept any version from the server
        context.registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(() -> "", (a, b) -> true));
        // Initialize Fusion stuff if this is on the client
        if(FMLEnvironment.dist.isClient())
            FusionClient.init(context);
    }
}
