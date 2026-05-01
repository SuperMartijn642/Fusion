package com.supermartijn642.fusion.model.types.connecting;

import com.supermartijn642.fusion.model.types.base.BaseModelElement;
import net.minecraft.client.resources.model.cuboid.CuboidFace;
import net.minecraft.client.resources.model.cuboid.CuboidRotation;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3fc;

import java.util.Collections;
import java.util.Map;

/**
 * Created 07/09/2024 by SuperMartijn642
 */
public class ConnectingModelElement extends BaseModelElement {

    public final Map<Direction,String> faceConnectionKeys;

    public ConnectingModelElement(Vector3fc from, Vector3fc to, Map<Direction,CuboidFace> faces, @Nullable CuboidRotation rotation, boolean shade, int lightEmission, Map<Direction,String> faceConnectionKeys){
        super(from, to, faces, rotation, shade, lightEmission);
        this.faceConnectionKeys = faceConnectionKeys == null ? Collections.emptyMap() : Collections.unmodifiableMap(faceConnectionKeys);
    }
}
