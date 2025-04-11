package com.supermartijn642.fusion.model.types.base;

import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockElementRotation;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3fc;

import java.util.Map;

/**
 * Created 06/09/2024 by SuperMartijn642
 */
public class BaseModelElement {

    public final BlockElement original;

    public BaseModelElement(Vector3fc from, Vector3fc to, Map<Direction,BlockElementFace> faces, @Nullable BlockElementRotation rotation, boolean shade, int lightEmission){
        this.original = new BlockElement(from, to, faces, rotation, shade, lightEmission);
    }
}
