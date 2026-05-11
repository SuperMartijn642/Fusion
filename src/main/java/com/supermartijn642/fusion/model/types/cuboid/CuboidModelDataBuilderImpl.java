package com.supermartijn642.fusion.model.types.cuboid;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Either;
import com.supermartijn642.fusion.api.model.custom.geometry.CuboidModelGeometry;
import com.supermartijn642.fusion.api.model.types.CuboidModelDataBuilder;
import net.minecraft.client.renderer.model.*;
import net.minecraft.util.Direction;

import java.util.*;

/**
 * Created 01/05/2023 by SuperMartijn642
 */
public class CuboidModelDataBuilderImpl extends AbstractCuboidModelDataBuilder<CuboidModelDataBuilderImpl,BlockModel> {

    public static CuboidModelDataBuilder<?,BlockModel> builder(){
        return new CuboidModelDataBuilderImpl();
    }

    private CuboidModelDataBuilderImpl(){
    }

    @Override
    public BlockModel build(){
        // Create vanilla texture slots
        ImmutableMap.Builder<String,Either<RenderMaterial,String>> textures = ImmutableMap.builder();
        this.materials.forEach((key, value) -> {
            if(value.isLeft())
                textures.put(key, Either.right(value.left()));
            else
                textures.put(key, Either.left(value.right().toRenderMaterial()));
        });
        // Convert vanilla geometry
        List<BlockPart> elements = new ArrayList<>(this.elements.size());
        for(CuboidModelGeometry.Element element : this.elements){
            Map<Direction,BlockPartFace> faces = new EnumMap<>(Direction.class);
            for(Direction side : Direction.values()){
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
        ItemCameraTransforms itemTransforms = ItemCameraTransforms.NO_TRANSFORMS;
        if(!this.itemTransforms.isEmpty()){
            itemTransforms = new ItemCameraTransforms(
                this.itemTransforms.getOrDefault(ItemCameraTransforms.TransformType.THIRD_PERSON_LEFT_HAND, ItemTransformVec3f.NO_TRANSFORM),
                this.itemTransforms.getOrDefault(ItemCameraTransforms.TransformType.THIRD_PERSON_RIGHT_HAND, ItemTransformVec3f.NO_TRANSFORM),
                this.itemTransforms.getOrDefault(ItemCameraTransforms.TransformType.FIRST_PERSON_LEFT_HAND, ItemTransformVec3f.NO_TRANSFORM),
                this.itemTransforms.getOrDefault(ItemCameraTransforms.TransformType.FIRST_PERSON_RIGHT_HAND, ItemTransformVec3f.NO_TRANSFORM),
                this.itemTransforms.getOrDefault(ItemCameraTransforms.TransformType.HEAD, ItemTransformVec3f.NO_TRANSFORM),
                this.itemTransforms.getOrDefault(ItemCameraTransforms.TransformType.GUI, ItemTransformVec3f.NO_TRANSFORM),
                this.itemTransforms.getOrDefault(ItemCameraTransforms.TransformType.GROUND, ItemTransformVec3f.NO_TRANSFORM),
                this.itemTransforms.getOrDefault(ItemCameraTransforms.TransformType.FIXED, ItemTransformVec3f.NO_TRANSFORM)
            );
        }
        // Create the vanilla model
        return new BlockModel(this.parent, elements, textures.build(), this.ambientOcclusion, this.guiLight, itemTransforms, Collections.emptyList());
    }
}
