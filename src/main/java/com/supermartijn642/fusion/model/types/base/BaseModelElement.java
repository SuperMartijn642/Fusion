package com.supermartijn642.fusion.model.types.base;

import net.minecraft.client.renderer.block.model.BlockPart;
import net.minecraft.client.renderer.block.model.BlockPartFace;
import net.minecraft.client.renderer.block.model.BlockPartRotation;
import net.minecraft.util.EnumFacing;
import org.lwjgl.util.vector.Vector3f;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Created 06/09/2024 by SuperMartijn642
 */
public class BaseModelElement extends BlockPart {

    public Integer light_emission;

    public BaseModelElement(Vector3f from, Vector3f to, Map<EnumFacing,BlockPartFace> faces, @Nullable BlockPartRotation rotation, boolean shade, Integer light_emission){
        super(from, to, faces, rotation, shade);
        this.light_emission = light_emission;
    }
}
