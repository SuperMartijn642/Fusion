package com.supermartijn642.fusion.model.types.cuboid;

import com.google.common.collect.ImmutableMap;
import com.supermartijn642.fusion.api.model.custom.DefaultModelProperties;
import com.supermartijn642.fusion.api.model.custom.geometry.CuboidModelGeometry;
import com.supermartijn642.fusion.api.model.types.CuboidModelDataBuilder;
import com.supermartijn642.fusion.util.ForgeNamedRenderTypeGroupHelper;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.client.model.ForgeBlockModelData;
import net.minecraftforge.client.model.ForgeFaceData;

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
        TextureSlots.Data.Builder textures = new TextureSlots.Data.Builder();
        this.materials.forEach((key, value) -> {
            if(value.isLeft())
                textures.addReference(key, value.left());
            else
                textures.addTexture(key, value.right().toMaterial());
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
                    face.uv(),
                    face.rotation(),
                    new ForgeFaceData(
                        face.getProperty(DefaultModelProperties.FORGE_GEOMETRY_COLOR).orElse(-1),
                        face.getProperty(DefaultModelProperties.FORGE_GEOMETRY_BLOCK_LIGHT).orElse(0),
                        face.getProperty(DefaultModelProperties.FORGE_GEOMETRY_SKY_LIGHT).orElse(0),
                        face.getProperty(DefaultModelProperties.FORGE_GEOMETRY_AMBIENT_OCCLUSION).orElse(true)
                    )
                ));
            }
            elements.add(new BlockElement(
                element.from(), element.to(),
                faces,
                element.rotation(),
                element.shade() == null || element.shade(),
                element.lightEmission() == null ? 0 : element.lightEmission()
            ));
        }
        SimpleUnbakedGeometry geometry = new SimpleUnbakedGeometry(elements);
        // Create item transforms
        ItemTransforms itemTransforms = null;
        if(!this.itemTransforms.isEmpty()){
            itemTransforms = new ItemTransforms(
                this.itemTransforms.getOrDefault(ItemDisplayContext.THIRD_PERSON_LEFT_HAND, ItemTransform.NO_TRANSFORM),
                this.itemTransforms.getOrDefault(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, ItemTransform.NO_TRANSFORM),
                this.itemTransforms.getOrDefault(ItemDisplayContext.FIRST_PERSON_LEFT_HAND, ItemTransform.NO_TRANSFORM),
                this.itemTransforms.getOrDefault(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, ItemTransform.NO_TRANSFORM),
                this.itemTransforms.getOrDefault(ItemDisplayContext.HEAD, ItemTransform.NO_TRANSFORM),
                this.itemTransforms.getOrDefault(ItemDisplayContext.GUI, ItemTransform.NO_TRANSFORM),
                this.itemTransforms.getOrDefault(ItemDisplayContext.GROUND, ItemTransform.NO_TRANSFORM),
                this.itemTransforms.getOrDefault(ItemDisplayContext.FIXED, ItemTransform.NO_TRANSFORM),
                ImmutableMap.copyOf(this.itemTransforms)
            );
        }
        // Forge render type
        ForgeBlockModelData forgeData = this.forgeRenderTypeGroup == null ? null : new ForgeBlockModelData(
            Optional.empty(),
            Optional.ofNullable(ForgeNamedRenderTypeGroupHelper.getIdentifier(this.forgeRenderTypeGroup)),
            Optional.empty(),
            Optional.empty()
        );
        // Create the vanilla model
        return new BlockModel(geometry, this.guiLight, this.ambientOcclusion == null || this.ambientOcclusion, itemTransforms, textures.build(), this.parent, forgeData);
    }
}
