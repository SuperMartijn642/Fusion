package com.supermartijn642.fusion;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

/**
 * Created 26/04/2023 by SuperMartijn642
 */
@Mod("fusion")
public class Fusion {

    public static final String MODID = "fusion";

    public static ResourceLocation identifier(String path){
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    public Fusion(){
        // Initialize Fusion stuff if this is on the client
        if(FMLEnvironment.dist.isClient())
            FusionClient.init();
    }
}
