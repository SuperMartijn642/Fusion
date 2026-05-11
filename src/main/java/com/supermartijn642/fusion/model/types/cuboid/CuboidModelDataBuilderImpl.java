package com.supermartijn642.fusion.model.types.cuboid;

import com.google.common.collect.ImmutableMap;
import com.supermartijn642.fusion.api.model.custom.geometry.CuboidModelGeometry;
import com.supermartijn642.fusion.api.model.types.CuboidModelDataBuilder;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.util.EnumFacing;

import java.util.*;

/**
 * Created 01/05/2023 by SuperMartijn642
 */
public class CuboidModelDataBuilderImpl extends AbstractCuboidModelDataBuilder<CuboidModelDataBuilderImpl,ModelBlock> {

    public static CuboidModelDataBuilder<?,ModelBlock> builder(){
        return new CuboidModelDataBuilderImpl();
    }

    private CuboidModelDataBuilderImpl(){
    }

    @Override
    public ModelBlock build(){
        // Create vanilla texture slots
        ImmutableMap.Builder<String,String> textures = ImmutableMap.builder();
        this.materials.forEach((key, value) -> {
            if(value.isLeft())
                textures.put(key, value.left().isEmpty() || value.left().charAt(0) != '#' ? '#' + value.left() : value.left());
            else
                textures.put(key, value.right().texture().toString());
        });
        // Convert vanilla geometry
        List<BlockPart> elements = new ArrayList<>(this.elements.size());
        for(CuboidModelGeometry.Element element : this.elements){
            Map<EnumFacing,BlockPartFace> faces = new EnumMap<>(EnumFacing.class);
            for(EnumFacing side : EnumFacing.values()){
                CuboidModelGeometry.Face face = element.face(side);
                if(face == null)
                    continue;
                String material = face.material();
                if(!material.isEmpty() && material.charAt(0) == '#')
                    material = material.substring(1);
                faces.put(side, new BlockPartFace(
                    face.cullDirection(),
                    face.tintIndex() == null ? -1 : face.tintIndex(),
                    material,
                    new BlockFaceUV(
                        face.uv() == null ? null : new float[]{face.uv().minU(), face.uv().minV(), face.uv().maxU(), face.uv().maxV()},
                        face.rotation() == null ? 0 : face.rotation().angle()
                    )
                ));
            }
            elements.add(new BlockPart(
                element.from(), element.to(),
                faces,
                element.rotation(),
                element.shade() == null || element.shade()
            ));
        }
        // Create item transforms
        ItemCameraTransforms itemTransforms = ItemCameraTransforms.DEFAULT;
        if(!this.itemTransforms.isEmpty()){
            itemTransforms = new ItemCameraTransforms(
                this.itemTransforms.getOrDefault(ItemCameraTransforms.TransformType.THIRD_PERSON_LEFT_HAND, ItemTransformVec3f.DEFAULT),
                this.itemTransforms.getOrDefault(ItemCameraTransforms.TransformType.THIRD_PERSON_RIGHT_HAND, ItemTransformVec3f.DEFAULT),
                this.itemTransforms.getOrDefault(ItemCameraTransforms.TransformType.FIRST_PERSON_LEFT_HAND, ItemTransformVec3f.DEFAULT),
                this.itemTransforms.getOrDefault(ItemCameraTransforms.TransformType.FIRST_PERSON_RIGHT_HAND, ItemTransformVec3f.DEFAULT),
                this.itemTransforms.getOrDefault(ItemCameraTransforms.TransformType.HEAD, ItemTransformVec3f.DEFAULT),
                this.itemTransforms.getOrDefault(ItemCameraTransforms.TransformType.GUI, ItemTransformVec3f.DEFAULT),
                this.itemTransforms.getOrDefault(ItemCameraTransforms.TransformType.GROUND, ItemTransformVec3f.DEFAULT),
                this.itemTransforms.getOrDefault(ItemCameraTransforms.TransformType.FIXED, ItemTransformVec3f.DEFAULT)
            );
        }
        // Create the vanilla model
        return new ModelBlock(this.parent, elements, textures.build(), this.ambientOcclusion, this.isGui3d, itemTransforms, Collections.emptyList());
    }
}
