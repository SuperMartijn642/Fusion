package com.supermartijn642.fusion.model.types.connecting;

import com.google.common.collect.ImmutableMap;
import com.supermartijn642.fusion.api.model.custom.ModelMaterial;
import com.supermartijn642.fusion.api.model.custom.geometry.CuboidModelGeometry;
import com.supermartijn642.fusion.api.model.types.connecting.ConnectingModelData;
import com.supermartijn642.fusion.api.texture.types.connecting.predicates.ConnectionPredicate;
import com.supermartijn642.fusion.api.util.Either;
import com.supermartijn642.fusion.model.types.base.BaseModelDataImpl;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemTransformVec3f;
import net.minecraft.util.ResourceLocation;

import java.util.Map;

/**
 * Created 23/10/2023 by SuperMartijn642
 */
public class ConnectingModelDataImpl extends BaseModelDataImpl implements ConnectingModelData {

    private final Map<String,Either<String,ConnectionPredicate>> connections;

    public ConnectingModelDataImpl(ResourceLocation parent, Map<String,Either<String,ModelMaterial>> materials, Boolean ambientOcclusion, Boolean shade, Boolean emissive, Boolean isGui3d, CuboidModelGeometry geometry, Map<ItemCameraTransforms.TransformType,ItemTransformVec3f> ItemCameraTransforms, Map<String,Either<String,ConnectionPredicate>> connections){
        super(parent, materials, ambientOcclusion, shade, emissive, isGui3d, geometry, ItemCameraTransforms);
        this.connections = ImmutableMap.copyOf(connections);
    }

    @Override
    public ConnectionPredicate getConnectionPredicate(String key){
        return this.connections.get(key).rightOrElse(null);
    }

    @Override
    public Map<String,Either<String,ConnectionPredicate>> getAllConnectionPredicates(){
        return this.connections;
    }
}
