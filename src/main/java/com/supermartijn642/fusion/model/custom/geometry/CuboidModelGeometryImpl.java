package com.supermartijn642.fusion.model.custom.geometry;

import com.google.common.collect.ImmutableList;
import com.supermartijn642.fusion.api.model.custom.CullableQuads;
import com.supermartijn642.fusion.api.model.custom.ModelMaterial;
import com.supermartijn642.fusion.api.model.custom.ModelTransform;
import com.supermartijn642.fusion.api.model.custom.geometry.CuboidModelGeometry;
import com.supermartijn642.fusion.api.model.custom.quad.MutableQuad;
import com.supermartijn642.fusion.api.model.custom.quad.QuadAccess;
import com.supermartijn642.fusion.api.util.Either;
import com.supermartijn642.fusion.api.util.PropertyGetter;
import net.minecraft.client.renderer.Matrix4f;
import net.minecraft.client.renderer.Vector3f;
import net.minecraft.client.renderer.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Direction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
        List<BlockPart> elements = model.getElements();
        if(elements == null)
            return of(Collections.emptyList());
        List<Element> converted = new ArrayList<>(elements.size());
        for(BlockPart element : elements)
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

            boolean shouldDrawFace;
            switch(side.getAxis()){
                case X:
                    shouldDrawFace = drawXFaces;
                    break;
                case Y:
                    shouldDrawFace = drawYFaces;
                    break;
                case Z:
                    shouldDrawFace = drawZFaces;
                    break;
                default:
                    throw new AssertionError();
            }
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
        String materialKey = face.material();
        if(!materialKey.isEmpty() && materialKey.charAt(0) == '#')
            materialKey = materialKey.substring(1);
        TextureAtlasSprite sprite = materialResolver.get(materialKey);
        BakedQuad bakedQuad = FACE_BAKERY.bakeQuad(
            element.from(), element.to(),
            new BlockPartFace(
                null, -1, "",
                new BlockFaceUV(
                    face.uv() == null ? calculateFaceUV(element, side) : new float[]{face.uv().minU(), face.uv().minV(), face.uv().maxU(), face.uv().maxV()},
                    face.rotation() == null ? 0 : face.rotation().angle()
                )
            ),
            sprite,
            side,
            transformation.toModelTransform(),
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

    private static float[] calculateFaceUV(Element element, Direction side){
        switch(side){
            case DOWN:
                return new float[]{element.from().x(), 16.0F - element.to().z(), element.to().x(), 16.0F - element.from().z()};
            case UP:
                return new float[]{element.from().x(), element.from().z(), element.to().x(), element.to().z()};
            case NORTH:
                return new float[]{16.0F - element.to().x(), 16.0F - element.to().y(), 16.0F - element.from().x(), 16.0F - element.from().y()};
            case SOUTH:
                return new float[]{element.from().x(), 16.0F - element.to().y(), element.to().x(), 16.0F - element.from().y()};
            case WEST:
                return new float[]{element.from().z(), 16.0F - element.to().y(), element.to().z(), 16.0F - element.from().y()};
            case EAST:
                return new float[]{16.0F - element.to().z(), 16.0F - element.to().y(), 16.0F - element.from().z(), 16.0F - element.from().y()};
        }
        throw new AssertionError();
    }

    private final List<Element> elements;

    private CuboidModelGeometryImpl(List<Element> elements){
        this.elements = ImmutableList.copyOf(elements);
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
