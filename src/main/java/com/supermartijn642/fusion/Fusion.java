package com.supermartijn642.fusion;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;

/**
 * Created 26/04/2023 by SuperMartijn642
 */
public class Fusion implements ModInitializer {

    public static final String MODID = "fusion";

    public static Identifier identifier(String path){
        return Identifier.fromNamespaceAndPath(MODID, path);
    }

    @Override
    public void onInitialize(){
    }
}
