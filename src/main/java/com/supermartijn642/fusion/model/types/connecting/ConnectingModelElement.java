package com.supermartijn642.fusion.model.types.connecting;

import com.supermartijn642.fusion.model.types.base.BaseModelElement;
import net.minecraft.client.renderer.block.model.BlockPartFace;
import net.minecraft.client.renderer.block.model.BlockPartRotation;
import net.minecraft.util.EnumFacing;
import org.lwjgl.util.vector.Vector3f;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;

/**
 * Created 07/09/2024 by SuperMartijn642
 */
public class ConnectingModelElement extends BaseModelElement {

    public final Map<EnumFacing,String> faceConnectionKeys;

    public ConnectingModelElement(Vector3f from, Vector3f to, Map<EnumFacing,BlockPartFace> faces, @Nullable BlockPartRotation rotation, boolean shade, Integer light_emission, Map<EnumFacing,String> faceConnectionKeys){
        super(from, to, faces, rotation, shade, light_emission);
        this.faceConnectionKeys = faceConnectionKeys == null ? Collections.emptyMap() : Collections.unmodifiableMap(faceConnectionKeys);
    }
}
