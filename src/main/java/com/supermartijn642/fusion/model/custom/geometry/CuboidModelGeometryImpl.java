package com.supermartijn642.fusion.model.custom.geometry;

import com.mojang.math.Quadrant;
import com.supermartijn642.fusion.api.model.custom.CullableQuads;
import com.supermartijn642.fusion.api.model.custom.ModelTransform;
import com.supermartijn642.fusion.api.model.custom.geometry.CuboidModelGeometry;
import com.supermartijn642.fusion.api.model.custom.quad.MutableQuad;
import com.supermartijn642.fusion.api.model.custom.quad.QuadAccess;
import com.supermartijn642.fusion.api.util.PropertyGetter;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created 03/05/2026 by SuperMartijn642
 */
public class CuboidModelGeometryImpl implements CuboidModelGeometry {

    public static CuboidModelGeometry of(List<Element> elements){
        return new CuboidModelGeometryImpl(elements);
    }

    public static CuboidModelGeometry of(SimpleUnbakedGeometry geometry){
        List<Element> elements = new ArrayList<>(geometry.elements().size());
        for(BlockElement element : geometry.elements())
            elements.add(Element.of(element));
        return of(elements);
    }

    public static CullableQuads bakeElement(Element element, ModelTransform transformation, MaterialKeyResolver materialResolver){
        CullableQuads.Builder quads = CullableQuads.builder();
        bakeElement((q, d, p) -> quads.add(d, q), element, transformation, materialResolver);
        return quads.build();
    }

    public static void bakeElement(QuadConsumer consumer, Element element, ModelTransform transformation, MaterialKeyResolver materialResolver){
        // Create quads the same way as vanilla
        // Check whether the size is 0 for any axis
        Vector3fc from = element.from();
        Vector3fc to = element.to();
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

    public static QuadAccess bakeFace(Face face, Element element, Direction side, ModelTransform transformation, MaterialKeyResolver materialResolver){
        AtomicReference<QuadAccess> quad = new AtomicReference<>();
        bakeFace((q, d, p) -> quad.set(q), face, element, side, transformation, materialResolver);
        return quad.get();
    }

    public static void bakeFace(QuadConsumer consumer, Face face, Element element, Direction side, ModelTransform transformation, MaterialKeyResolver materialResolver){
        // Create a dummy baked quad
        BlockElementFace.UVs uv = face.uv() == null ?
            FaceBakery.defaultFaceUV(element.from(), element.to(), side) :
            face.uv();
        String materialKey = face.material();
        if(!materialKey.isEmpty() && materialKey.charAt(0) == '#')
            materialKey = materialKey.substring(1);
        TextureAtlasSprite sprite = materialResolver.get(materialKey);
        BakedQuad bakedQuad = FaceBakery.bakeQuad(
            ModelGeometryImpl.DUMMY_PART_CACHE,
            element.from(), element.to(),
            new BlockElementFace(
                null, -1, "",
                uv,
                face.rotation() == null ? Quadrant.R0 : face.rotation()
            ),
            sprite,
            side,
            transformation.toModelState(),
            element.rotation(),
            true,
            0
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
        // Rotate cull direction
        Direction cullDirection = face.cullDirection() == null ? null :
            Direction.rotate(transformation.matrix(), face.cullDirection());
        // Emit quad
        consumer.consume(quad, cullDirection, PropertyGetter.compose(face, element));
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
    public void bake(QuadConsumer consumer, ModelTransform transformation, MaterialKeyResolver materialResolver){
        for(Element element : this.elements)
            bakeElement(consumer, element, transformation, materialResolver);
    }
}
