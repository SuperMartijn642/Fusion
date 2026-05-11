package com.supermartijn642.fusion.api.model.custom.geometry;

import com.supermartijn642.fusion.api.model.custom.CullableQuads;
import com.supermartijn642.fusion.api.model.custom.ModelProperty;
import com.supermartijn642.fusion.api.model.custom.ModelTransform;
import com.supermartijn642.fusion.api.model.custom.quad.QuadAccess;
import com.supermartijn642.fusion.model.custom.geometry.CuboidGeometryElementImpl;
import com.supermartijn642.fusion.model.custom.geometry.CuboidGeometryFaceImpl;
import com.supermartijn642.fusion.model.custom.geometry.CuboidModelGeometryImpl;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockElementRotation;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3fc;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * An interface representing geometry consisting of cuboids.
 * <p>
 * Created 02/05/2026 by SuperMartijn642
 */
@ApiStatus.NonExtendable
public interface CuboidModelGeometry extends ModelGeometry {

    /**
     * Creates a geometry instance from the given elements.
     */
    static CuboidModelGeometry of(List<Element> elements){
        return CuboidModelGeometryImpl.of(elements);
    }

    /**
     * Converts the given {@link BlockModel} to a {@link CuboidModelGeometry} instance.
     */
    static CuboidModelGeometry of(BlockModel model){
        return CuboidModelGeometryImpl.of(model);
    }

    /**
     * Bakes the given element into a set of quads.
     * @param element          element to be baked
     * @param transformation   transformation to apply to the element
     * @param materialResolver resolver for material keys
     */
    static CullableQuads bakeElement(Element element, ModelTransform transformation, MaterialResolver materialResolver){
        return CuboidModelGeometryImpl.bakeElement(element, transformation, materialResolver);
    }

    /**
     * Bakes the given face into a quad.
     * @param face             face to be baked
     * @param element          element that the face belongs to
     * @param side             side of the element that the face is on
     * @param transformation   transformation to apply to the face
     * @param materialResolver resolver for material keys
     */
    static QuadAccess bakeFace(Face face, Element element, Direction side, ModelTransform transformation, MaterialResolver materialResolver){
        return CuboidModelGeometryImpl.bakeFace(face, element, side, transformation, materialResolver);
    }

    /**
     * A list of elements that this geometry consists of.
     * The returned list of elements may be empty.
     */
    List<Element> elements();

    /**
     * A part in a cuboid model representing a cuboid with potential faces on each side.
     */
    @ApiStatus.NonExtendable
    interface Element {

        /**
         * Converts the given {@link BlockElement} to an {@link Element}.
         */
        static Element of(BlockElement element){
            return CuboidGeometryElementImpl.of(element);
        }

        /**
         * Creates a builder for elements.
         */
        static Builder builder(){
            return CuboidGeometryElementImpl.builder();
        }

        /**
         * From-position of the element's cube shape.
         */
        Vector3fc from();

        /**
         * To-position of the element's cube shape.
         */
        Vector3fc to();

        /**
         * Rotation of the element.
         */
        @Nullable
        BlockElementRotation rotation();

        /**
         * Face for the given side of the element.
         */
        @Nullable
        Face face(Direction side);

        /**
         * Whether the element should be shaded.
         */
        @Nullable
        Boolean shade();

        /**
         * Base light-level of the element.
         */
        @Nullable
        Integer lightEmission();

        /**
         * Whether the element should be rendered with ambient occlusion.
         */
        @Nullable
        Boolean ambientOcclusion();

        /**
         * Whether the element is emissive.
         */
        @Nullable
        Boolean emissive();

        /**
         * Gets an arbitrary property of this element.
         * @see ModelProperty
         */
        <X, C> Optional<X> getProperty(ModelProperty<X,C> property, C context);

        /**
         * Gets an arbitrary property of this element.
         * @see ModelProperty
         */
        default <X> Optional<X> getProperty(ModelProperty<X,Void> property){
            return this.getProperty(property, null);
        }

        @ApiStatus.NonExtendable
        interface Builder {

            /**
             * Sets the from- and to-positions of the elements cube shape.
             * @param from from-position of the cube shape
             * @param to   to-position of the cube shape
             */
            Builder fromTo(Vector3fc from, Vector3fc to);

            /**
             * Sets the rotation of the element.
             */
            Builder rotation(@Nullable BlockElementRotation rotation);

            /**
             * Sets the face for the given side of the element.
             * @param side side of the element
             * @param face face for the given side, may be {@code null} for no face
             */
            Builder face(@Nullable Direction side, Face face);

            /**
             * Sets whether the element should be shaded.
             */
            Builder shade(@Nullable Boolean shade);

            /**
             * Sets the base light-level for the element.
             */
            Builder lightEmission(@Nullable Integer lightEmission);

            /**
             * Sets whether the element should be rendered with ambient occlusion.
             */
            Builder ambientOcclusion(@Nullable Boolean ambientOcclusion);

            /**
             * Sets whether the element is emissive.
             */
            Builder emissive(@Nullable Boolean emissive);

            /**
             * Sets an arbitrary property for the element.
             * @param property property to be set
             * @param value    value for the property
             * @see ModelProperty
             */
            <X> Builder property(ModelProperty<X,?> property, X value);

            /**
             * Sets an arbitrary property for the element.
             * @param property property to be set
             * @param value    value supplier for the property
             * @see ModelProperty
             */
            <X> Builder property(ModelProperty<X,?> property, Supplier<X> value);

            /**
             * Sets an arbitrary property for the element.
             * @param property property to be set
             * @param value    value resolver for the property
             * @see ModelProperty
             */
            <X, C> Builder property(ModelProperty<X,C> property, Function<C,X> value);

            /**
             * Builds the element.
             */
            Element build();
        }
    }

    /**
     * A face of a cuboid element.
     */
    interface Face {

        /**
         * Converts the given {@link BlockElementFace} to a {@link Face}.
         */
        static Face of(BlockElementFace face){
            return CuboidGeometryFaceImpl.of(face);
        }

        /**
         * Creates a builder for faces.
         */
        static Builder builder(){
            return CuboidGeometryFaceImpl.builder();
        }

        /**
         * The material key of the face.
         */
        String material();

        /**
         * The texture uv-coordinates of the face.
         * If {@code null}, the coordinates should be calculated from the shape of the element that the face belongs to.
         */
        @Nullable
        UV uv();

        /**
         * The texture rotation of the face.
         */
        @Nullable
        Rotation rotation();

        /**
         * The cull direction of the face.
         */
        @Nullable
        Direction cullDirection();

        /**
         * The tint index of the face.
         */
        @Nullable
        Integer tintIndex();

        /**
         * Whether the face should be shaded.
         */
        @Nullable
        Boolean shade();

        /**
         * Base light-level of the face.
         */
        @Nullable
        Integer lightEmission();

        /**
         * Whether the face should be rendered with ambient occlusion.
         */
        @Nullable
        Boolean ambientOcclusion();

        /**
         * Whether the face is emissive.
         */
        @Nullable
        Boolean emissive();

        /**
         * Gets an arbitrary property of this face.
         * @see ModelProperty
         */
        <X, C> Optional<X> getProperty(ModelProperty<X,C> property, C context);

        /**
         * Gets an arbitrary property of this face.
         * @see ModelProperty
         */
        default <X> Optional<X> getProperty(ModelProperty<X,Void> property){
            return this.getProperty(property, null);
        }

        record UV(float minU, float minV, float maxU, float maxV) {
        }

        enum Rotation {
            R0, R90, R180, R270;

            public static Rotation byAngle(int angle){
                return values()[(((angle % 360) + 360) % 360) / 90];
            }

            public int angle(){
                return switch(this){
                    case R0 -> 0;
                    case R90 -> 90;
                    case R180 -> 180;
                    case R270 -> 270;
                };
            }
        }

        @ApiStatus.NonExtendable
        interface Builder {

            /**
             * Sets the material key for the face.
             */
            Builder material(String key);

            /**
             * Sets the texture uv-coordinates of the face.
             * If not set, the uv-coordinates are calculated from the shape of the element that the face belongs to.
             */
            Builder uv(@Nullable UV uv);

            /**
             * Sets the texture uv-coordinates of the face.
             * If not set, the uv-coordinates are calculated from the shape of the element that the face belongs to.
             */
            default Builder uv(float minU, float minV, float maxU, float maxV){
                return this.uv(new UV(minU, minV, maxU, maxV));
            }

            /**
             * Sets the texture rotation of the face.
             */
            Builder rotation(@Nullable Rotation quadrant);

            /**
             * Sets the cull direction of the face.
             */
            Builder cullDirection(@Nullable Direction direction);

            /**
             * Sets the tint index of the face.
             */
            Builder tintIndex(@Nullable Integer tintIndex);

            /**
             * Sets whether the face should be shaded.
             */
            Builder shade(@Nullable Boolean shade);

            /**
             * Sets the base light-level of the face.
             */
            Builder lightEmission(@Nullable Integer lightEmission);

            /**
             * Sets whether the face should be rendered with ambient occlusion.
             */
            Builder ambientOcclusion(@Nullable Boolean ambientOcclusion);

            /**
             * Sets whether the face is emissive.
             */
            Builder emissive(@Nullable Boolean emissive);

            /**
             * Sets an arbitrary property for the face.
             * @param property property to be set
             * @param value    value for the property
             * @see ModelProperty
             */
            <X> Builder property(ModelProperty<X,?> property, X value);

            /**
             * Sets an arbitrary property for the face.
             * @param property property to be set
             * @param value    value supplier for the property
             * @see ModelProperty
             */
            <X> Builder property(ModelProperty<X,?> property, Supplier<X> value);

            /**
             * Sets an arbitrary property for the face.
             * @param property property to be set
             * @param value    value resolver for the property
             * @see ModelProperty
             */
            <X, C> Builder property(ModelProperty<X,C> property, Function<C,X> value);

            /**
             * Builds the face.
             */
            Face build();
        }
    }
}
