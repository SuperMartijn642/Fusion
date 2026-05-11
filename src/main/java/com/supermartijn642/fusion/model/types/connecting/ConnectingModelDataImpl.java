package com.supermartijn642.fusion.model.types.connecting;

import com.supermartijn642.fusion.api.model.custom.ModelMaterial;
import com.supermartijn642.fusion.api.model.custom.geometry.CuboidModelGeometry;
import com.supermartijn642.fusion.api.model.types.connecting.ConnectingModelData;
import com.supermartijn642.fusion.api.model.types.connecting.predicates.ConnectionPredicate;
import com.supermartijn642.fusion.api.util.Either;
import com.supermartijn642.fusion.model.types.base.BaseModelDataImpl;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;

import java.util.Map;

/**
 * Created 23/10/2023 by SuperMartijn642
 */
public class ConnectingModelDataImpl extends BaseModelDataImpl implements ConnectingModelData {

    private final Map<String,Either<String,ConnectionPredicate>> connections;

    public ConnectingModelDataImpl(Identifier parent, Map<String,Either<String,ModelMaterial>> materials, Boolean ambientOcclusion, Boolean shade, Boolean emissive, UnbakedModel.GuiLight guiLight, CuboidModelGeometry geometry, Map<ItemDisplayContext,ItemTransform> itemTransforms, Map<String,Either<String,ConnectionPredicate>> connections){
        super(parent, materials, ambientOcclusion, shade, emissive, guiLight, geometry, itemTransforms);
        this.connections = Map.copyOf(connections);
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
