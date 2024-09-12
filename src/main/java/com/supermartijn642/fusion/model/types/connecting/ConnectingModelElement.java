package com.supermartijn642.fusion.model.types.connecting;

import com.mojang.math.Vector3f;
import com.supermartijn642.fusion.model.types.base.BaseModelElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockElementRotation;
import net.minecraft.core.Direction;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;

/**
 * Created 07/09/2024 by SuperMartijn642
 */
public class ConnectingModelElement extends BaseModelElement {

    public final Map<Direction,String> faceConnectionKeys;

    public ConnectingModelElement(Vector3f from, Vector3f to, Map<Direction,BlockElementFace> faces, @Nullable BlockElementRotation rotation, boolean shade, Integer light_emission, Map<Direction,String> faceConnectionKeys){
        super(from, to, faces, rotation, shade, light_emission);
        this.faceConnectionKeys = faceConnectionKeys == null ? Collections.emptyMap() : Collections.unmodifiableMap(faceConnectionKeys);
    }
}
