package com.supermartijn642.fusion.model.custom.geometry;

import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import com.supermartijn642.fusion.api.model.custom.CullableQuads;
import com.supermartijn642.fusion.api.model.custom.ModelMaterial;
import com.supermartijn642.fusion.api.model.custom.ModelTransform;
import com.supermartijn642.fusion.api.model.custom.geometry.CuboidModelGeometry;
import com.supermartijn642.fusion.api.model.custom.quad.MutableQuad;
import com.supermartijn642.fusion.api.model.custom.quad.QuadAccess;
import com.supermartijn642.fusion.api.util.Either;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created 03/05/2026 by SuperMartijn642
 */
public class CuboidModelGeometryImpl implements CuboidModelGeometry {

    public static CuboidModelGeometry of(List<Element> elements){
        return new CuboidModelGeometryImpl(elements);
    }

    public static CuboidModelGeometry of(BlockModel model){
        List<BlockElement> elements = model.getElements();
        if(elements == null)
            return of(List.of());
        List<Element> converted = new ArrayList<>(elements.size());
        for(BlockElement element : elements)
            converted.add(Element.of(element));
        return of(converted);
    }

    public static CullableQuads bakeElement(Element element, ModelTransform transformation, MaterialResolver materialResolver){
        // Create quads the same way as vanilla
        // Check whether the size is 0 for any axis
        Vector3f from = element.from();
        Vector3f to = element.to();
        boolean drawXFaces = from.y() != to.y() && from.z() != to.z();
        boolean drawYFaces = from.x() != to.x() && from.z() != to.z();
        boolean drawZFaces = from.x() != to.x() && from.y() != to.y();
        if(!drawXFaces && !drawYFaces && !drawZFaces)
            return CullableQuads.empty();

        // Create the quads for each side
        CullableQuads.Builder quads = CullableQuads.builder();
        for(Direction side : Direction.values()){
            Face face = element.face(side);
            if(face == null)
                continue;

            boolean shouldDrawFace = switch(side.getAxis()){
                case X -> drawXFaces;
                case Y -> drawYFaces;
                case Z -> drawZFaces;
            };
            if(!shouldDrawFace)
                continue;

            // Bake the face
            QuadAccess quad = CuboidModelGeometry.bakeFace(face, element, side, transformation, materialResolver);
            // Add the quad
            Direction cullDirection = face.cullDirection() == null ? null :
                Direction.rotate(new Matrix4f(transformation.matrix()), face.cullDirection());
            quads.add(cullDirection, quad);
        }
        return quads.build();
    }

    private static final FaceBakery FACE_BAKERY = new FaceBakery();

    public static QuadAccess bakeFace(Face face, Element element, Direction side, ModelTransform transformation, MaterialResolver materialResolver){
        // Create a dummy baked quad
        String materialKey = face.material();
        if(!materialKey.isEmpty() && materialKey.charAt(0) == '#')
            materialKey = materialKey.substring(1);
        TextureAtlasSprite sprite = materialResolver.get(materialKey);
        BakedQuad bakedQuad = FACE_BAKERY.bakeQuad(
            element.from(), element.to(),
            new BlockElementFace(
                null, -1, "",
                new BlockFaceUV(
                    face.uv() == null ? calculateFaceUV(element, side) : new float[]{face.uv().minU(), face.uv().minV(), face.uv().maxU(), face.uv().maxV()},
                    face.rotation() == null ? 0 : face.rotation().angle()
                )
            ),
            sprite,
            side,
            transformation.toModelState(),
            element.rotation(),
            true,
            null
        );
        // Create the proper quad
        MutableQuad quad = MutableQuad.create();
        quad.copyBakedQuad(bakedQuad);
        if(face.tintIndex() != null)
            quad.tintIndex(face.tintIndex());
        if(face.shade() != null || element.shade() != null)
            quad.shade(face.shade() == null ? element.shade() : face.shade());
        if(face.lightEmission() != null || element.lightEmission() != null)
            quad.lightEmission(face.lightEmission() == null ? element.lightEmission() : face.lightEmission());
        if(face.ambientOcclusion() != null || element.ambientOcclusion() != null)
            quad.ambientOcclusion(face.ambientOcclusion() == null ? element.ambientOcclusion() : face.ambientOcclusion());
        if(face.emissive() != null || element.emissive() != null)
            quad.emissive(face.emissive() == null ? element.emissive() : face.emissive());
        return quad;
    }

    private static float[] calculateFaceUV(Element element, Direction side){
        return switch(side){
            case DOWN -> new float[]{element.from().x(), 16.0F - element.to().z(), element.to().x(), 16.0F - element.from().z()};
            case UP -> new float[]{element.from().x(), element.from().z(), element.to().x(), element.to().z()};
            case NORTH -> new float[]{16.0F - element.to().x(), 16.0F - element.to().y(), 16.0F - element.from().x(), 16.0F - element.from().y()};
            case SOUTH -> new float[]{element.from().x(), 16.0F - element.to().y(), element.to().x(), 16.0F - element.from().y()};
            case WEST -> new float[]{element.from().z(), 16.0F - element.to().y(), element.to().z(), 16.0F - element.from().y()};
            case EAST -> new float[]{16.0F - element.to().z(), 16.0F - element.to().y(), 16.0F - element.from().z(), 16.0F - element.from().y()};
        };
    }

    private final List<Element> elements;

    private CuboidModelGeometryImpl(List<Element> elements){
        this.elements = List.copyOf(elements);
    }

    @Override
    public List<Element> elements(){
        return this.elements;
    }

    @Override
    public Collection<Either<String,ModelMaterial>> getRequiredMaterials(){
        List<Either<String,ModelMaterial>> materials = new ArrayList<>();
        for(Element element : this.elements){
            for(Direction side : Direction.values()){
                Face face = element.face(side);
                if(face != null)
                    materials.add(Either.left(face.material()));
            }
        }
        return materials;
    }

    @Override
    public CullableQuads bake(ModelTransform transformation, MaterialResolver materialResolver){
        CullableQuads.Builder quads = CullableQuads.builder();
        for(Element element : this.elements)
            quads.add(CuboidModelGeometry.bakeElement(element, transformation, materialResolver));
        return quads.build();
    }
}
