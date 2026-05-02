package com.supermartijn642.fusion.model.types.base;

import net.minecraft.client.resources.model.cuboid.CuboidFace;
import net.minecraft.client.resources.model.cuboid.CuboidModelElement;
import net.minecraft.client.resources.model.cuboid.CuboidRotation;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3fc;

import java.util.Map;

/**
 * Created 06/09/2024 by SuperMartijn642
 */
public class BaseModelElement {

    public final CuboidModelElement original;

    public BaseModelElement(Vector3fc from, Vector3fc to, Map<Direction, CuboidFace> faces, @Nullable CuboidRotation rotation, boolean shade, int lightEmission){
        this.original = new CuboidModelElement(from, to, faces, rotation, shade, lightEmission);
    }
}
