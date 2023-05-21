package com.supermartijn642.fusion;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;

/**
 * Created 26/04/2023 by SuperMartijn642
 */
@Mod("fusion")
public class Fusion {

    public Fusion(){
        // Accept any version from the server
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(() -> "", (a,b) -> true));
        // Initialize Fusion stuff if this is on the client
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> FusionClient::init);
    }
}
