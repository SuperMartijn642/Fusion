package com.supermartijn642.fusion.model.types.base;

import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockElementRotation;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Map;

/**
 * Created 06/09/2024 by SuperMartijn642
 */
public class BaseModelElement extends BlockElement {

    public Integer light_emission;

    public BaseModelElement(Vector3f from, Vector3f to, Map<Direction,BlockElementFace> faces, @Nullable BlockElementRotation rotation, boolean shade, Integer light_emission){
        super(from, to, faces, rotation, shade);
        this.light_emission = light_emission;
    }
}
