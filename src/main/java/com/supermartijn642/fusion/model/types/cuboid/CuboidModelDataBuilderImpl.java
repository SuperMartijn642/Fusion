package com.supermartijn642.fusion.model.types.cuboid;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Either;
import com.supermartijn642.fusion.api.model.custom.geometry.CuboidModelGeometry;
import com.supermartijn642.fusion.api.model.types.CuboidModelDataBuilder;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

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
        ImmutableMap.Builder<String,Either<Material,String>> textures = ImmutableMap.builderWithExpectedSize(this.materials.size());
        this.materials.forEach((key, value) -> {
            if(value.isLeft())
                textures.put(key, Either.right(value.left()));
            else
                textures.put(key, Either.left(value.right().toMaterial()));
        });
        // Convert vanilla geometry
        List<BlockElement> elements = new ArrayList<>(this.elements.size());
        for(CuboidModelGeometry.Element element : this.elements){
            Map<Direction,BlockElementFace> faces = new EnumMap<>(Direction.class);
            for(Direction side : Direction.values()){
                CuboidModelGeometry.Face face = element.face(side);
                if(face == null)
                    continue;
                String material = face.material();
                if(!material.isEmpty() && material.charAt(0) == '#')
                    material = material.substring(1);
                faces.put(side, new BlockElementFace(
                    face.cullDirection(),
                    face.tintIndex() == null ? -1 : face.tintIndex(),
                    material,
                    new BlockFaceUV(
                        face.uv() == null ? null : new float[]{face.uv().minU(), face.uv().minV(), face.uv().maxU(), face.uv().maxV()},
                        face.rotation() == null ? 0 : face.rotation().angle()
                    )
                ));
            }
            elements.add(new BlockElement(
                element.from(), element.to(),
                faces,
                element.rotation(),
                element.shade() == null || element.shade()
            ));
        }
        // Create item transforms
        ItemTransforms itemTransforms = ItemTransforms.NO_TRANSFORMS;
        if(!this.itemTransforms.isEmpty()){
            itemTransforms = new ItemTransforms(
                this.itemTransforms.getOrDefault(ItemTransforms.TransformType.THIRD_PERSON_LEFT_HAND, ItemTransform.NO_TRANSFORM),
                this.itemTransforms.getOrDefault(ItemTransforms.TransformType.THIRD_PERSON_RIGHT_HAND, ItemTransform.NO_TRANSFORM),
                this.itemTransforms.getOrDefault(ItemTransforms.TransformType.FIRST_PERSON_LEFT_HAND, ItemTransform.NO_TRANSFORM),
                this.itemTransforms.getOrDefault(ItemTransforms.TransformType.FIRST_PERSON_RIGHT_HAND, ItemTransform.NO_TRANSFORM),
                this.itemTransforms.getOrDefault(ItemTransforms.TransformType.HEAD, ItemTransform.NO_TRANSFORM),
                this.itemTransforms.getOrDefault(ItemTransforms.TransformType.GUI, ItemTransform.NO_TRANSFORM),
                this.itemTransforms.getOrDefault(ItemTransforms.TransformType.GROUND, ItemTransform.NO_TRANSFORM),
                this.itemTransforms.getOrDefault(ItemTransforms.TransformType.FIXED, ItemTransform.NO_TRANSFORM)
            );
        }
        // Create the vanilla model
        return new BlockModel(this.parent, elements, textures.build(), this.ambientOcclusion, this.guiLight, itemTransforms, List.of());
    }
}
