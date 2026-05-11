package com.supermartijn642.fusion.util;

/**
 * Created 03/06/2026 by SuperMartijn642
 */
public class PackedLightingHelper {

    public static int pack(int block, int sky){
        return block << 4 | sky << 20;
    }

    public static int unpackBlock(int lighting){
        return (lighting & 0xFFFF) >> 4;
    }

    public static int unpackSky(int lighting){
        return lighting >> 20 & 0xFFFF;
    }
}
