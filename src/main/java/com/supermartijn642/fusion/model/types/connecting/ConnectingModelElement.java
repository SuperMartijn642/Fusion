package com.supermartijn642.fusion.model.types.connecting;

import com.supermartijn642.fusion.model.types.base.BaseModelElement;
import net.minecraft.client.renderer.model.BlockPartFace;
import net.minecraft.client.renderer.model.BlockPartRotation;
import net.minecraft.util.Direction;
import net.minecraft.util.math.vector.Vector3f;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;

/**
 * Created 07/09/2024 by SuperMartijn642
 */
public class ConnectingModelElement extends BaseModelElement {

    public final Map<Direction,String> faceConnectionKeys;

    public ConnectingModelElement(Vector3f from, Vector3f to, Map<Direction,BlockPartFace> faces, @Nullable BlockPartRotation rotation, boolean shade, Integer light_emission, Map<Direction,String> faceConnectionKeys){
        super(from, to, faces, rotation, shade, light_emission);
        this.faceConnectionKeys = faceConnectionKeys == null ? Collections.emptyMap() : Collections.unmodifiableMap(faceConnectionKeys);
    }
}
