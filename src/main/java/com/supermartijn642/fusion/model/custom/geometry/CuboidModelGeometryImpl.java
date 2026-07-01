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
import com.supermartijn642.fusion.api.util.PropertyGetter;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

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

    public static CullableQuads bakeElement(Element element, ModelTransform transformation, MaterialKeyResolver materialResolver){
        CullableQuads.Builder quads = CullableQuads.builder();
        bakeElement((q, d, p) -> quads.add(d, q), element, transformation, materialResolver);
        return quads.build();
    }

    public static void bakeElement(QuadConsumer consumer, Element element, ModelTransform transformation, MaterialKeyResolver materialResolver){
        // Create quads the same way as vanilla
        // Check whether the size is 0 for any axis
        Vector3f from = element.from();
        Vector3f to = element.to();
        boolean drawXFaces = from.y() != to.y() && from.z() != to.z();
        boolean drawYFaces = from.x() != to.x() && from.z() != to.z();
        boolean drawZFaces = from.x() != to.x() && from.y() != to.y();
        if(!drawXFaces && !drawYFaces && !drawZFaces)
            return;

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
            CuboidModelGeometry.bakeFace(consumer, face, element, side, transformation, materialResolver);
        }
    }

    private static final FaceBakery FACE_BAKERY = new FaceBakery();

    public static QuadAccess bakeFace(Face face, Element element, Direction side, ModelTransform transformation, MaterialKeyResolver materialResolver){
        AtomicReference<QuadAccess> quad = new AtomicReference<>();
        bakeFace((q, d, p) -> quad.set(q), face, element, side, transformation, materialResolver);
        return quad.get();
    }

    public static void bakeFace(QuadConsumer consumer, Face face, Element element, Direction side, ModelTransform transformation, MaterialKeyResolver materialResolver){
        // Create a dummy baked quad
        Face.UV uv = face.uv() == null ?
            calculateFaceUV(element, side) :
            face.uv();
        String materialKey = face.material();
        if(!materialKey.isEmpty() && materialKey.charAt(0) == '#')
            materialKey = materialKey.substring(1);
        TextureAtlasSprite sprite = materialResolver.get(materialKey);
        BakedQuad bakedQuad = FACE_BAKERY.bakeQuad(
            element.from(), element.to(),
            new BlockElementFace(
                null, -1, "",
                new BlockFaceUV(
                    new float[]{uv.minU(), uv.minV(), uv.maxU(), uv.maxV()},
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
        if(face.emissive() != null || element.emissive() != null)
            quad.emissive(face.emissive() == null ? element.emissive() : face.emissive());
        // Rotate cull direction
        Direction cullDirection = face.cullDirection() == null ? null :
            Direction.rotate(new Matrix4f(transformation.matrix()), face.cullDirection());
        // Emit quad
        consumer.consume(quad, cullDirection, PropertyGetter.compose(face, element));
    }

    private static Face.UV calculateFaceUV(Element element, Direction side){
        float fromX = element.from().x(), fromY = element.from().y(), fromZ = element.from().z();
        float toX = element.to().x(), toY = element.to().y(), toZ = element.to().z();
        if(side.getAxis() == Direction.Axis.X)
            fromX = toX = 0;
        else if(side.getAxis() == Direction.Axis.Y)
            fromY = toY = 0;
        else if(side.getAxis() == Direction.Axis.Z)
            fromZ = toZ = 0;
        // Check if from and to are within the same block space
        boolean sameBlockSpace = sameBlockSpace(fromX, toX) && sameBlockSpace(fromY, toY) && sameBlockSpace(fromZ, toZ);
        // Use vanilla uv calculation
        Face.UV uv = switch(side){
            case DOWN -> new Face.UV(element.from().x(), 16.0F - element.to().z(), element.to().x(), 16.0F - element.from().z());
            case UP -> new Face.UV(element.from().x(), element.from().z(), element.to().x(), element.to().z());
            case NORTH -> new Face.UV(16.0F - element.to().x(), 16.0F - element.to().y(), 16.0F - element.from().x(), 16.0F - element.from().y());
            case SOUTH -> new Face.UV(element.from().x(), 16.0F - element.to().y(), element.to().x(), 16.0F - element.from().y());
            case WEST -> new Face.UV(element.from().z(), 16.0F - element.to().y(), element.to().z(), 16.0F - element.from().y());
            case EAST -> new Face.UV(16.0F - element.to().z(), 16.0F - element.to().y(), 16.0F - element.from().z(), 16.0F - element.from().y());
        };
        // If same block space, use modulo 16
        if(sameBlockSpace){
            float blockSpaceU = (float)Math.floor(Math.min(uv.minU(), uv.maxU()) / 16) * 16;
            float blockSpaceV = (float)Math.floor(Math.min(uv.minV(), uv.maxV()) / 16) * 16;
            uv = new Face.UV(uv.minU() - blockSpaceU, uv.minV() - blockSpaceV, uv.maxU() - blockSpaceU, uv.maxV() - blockSpaceV);
        }
        return uv;
    }

    private static boolean sameBlockSpace(float a, float b){
        a /= 16;
        b /= 16;
        if(a % 1 == 0)
            return Math.abs(Math.floor(a) - b) <= 1;
        float blockIndex = (float)Math.floor(a);
        return b >= blockIndex && b <= blockIndex + 1;
    }

    private final List<Element> elements;

    private CuboidModelGeometryImpl(List<Element> elements){
        this.elements = List.copyOf(elements);
    }

    @Override
    public boolean isCuboidGeometry(){
        return true;
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
    public void bake(QuadConsumer consumer, ModelTransform transformation, MaterialKeyResolver materialResolver){
        for(Element element : this.elements)
            bakeElement(consumer, element, transformation, materialResolver);
    }
}
