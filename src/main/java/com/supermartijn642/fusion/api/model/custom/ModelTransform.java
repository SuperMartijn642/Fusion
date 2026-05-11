package com.supermartijn642.fusion.api.model.custom;

import com.mojang.math.Matrix4f;
import com.mojang.math.Quaternion;
import com.mojang.math.Transformation;
import com.mojang.math.Vector3f;
import com.supermartijn642.fusion.model.custom.ModelTransformImpl;
import net.minecraft.client.resources.model.ModelState;
import org.jetbrains.annotations.ApiStatus;

/**
 * A transformation for baking a model.
 * <p>
 * Created 04/05/2026 by SuperMartijn642
 */
@ApiStatus.NonExtendable
public interface ModelTransform {

    /**
     * Creates a transformation for the given matrix.
     * @param matrix transformations for the model
     * @param uvLock whether the textures on faces should keep their original orientation
     */
    static ModelTransform of(Matrix4f matrix, boolean uvLock){
        return ModelTransformImpl.of(matrix, uvLock);
    }

    /**
     * Creates a transformation for the given matrix.
     * @param transformation transformations for the model
     * @param uvLock         whether the textures on faces should keep their original orientation
     */
    static ModelTransform of(Transformation transformation, boolean uvLock){
        return ModelTransformImpl.of(transformation, uvLock);
    }

    /**
     * Converts the given {@link ModelState} to a {@link ModelTransform} instance.
     */
    static ModelTransform of(ModelState modelState){
        return ModelTransformImpl.of(modelState);
    }

    /**
     * Transformation that has no effect on the model.
     */
    static ModelTransform identity(){
        return ModelTransformImpl.identity();
    }

    /**
     * Composes the given transformations.
     * The resulting transformation will have the uv-lock property set if at least one of the given transformations had it set.
     */
    static ModelTransform compose(ModelTransform... transforms){
        return ModelTransformImpl.compose(transforms);
    }

    /**
     * Whether the textures on faces should keep their original orientation.
     */
    boolean uvLock();

    /**
     * Matrix for the transformations.
     */
    Matrix4f matrix();

    /**
     * The decomposed translation part of the transformations.
     */
    Vector3f translation();

    /**
     * The decomposed left-rotation part of the transformations.
     */
    Quaternion leftRotation();

    /**
     * The decomposed scaling part of the transformations.
     */
    Vector3f scale();

    /**
     * The decomposed right-rotation part of the transformations.
     */
    Quaternion rightRotation();

    /**
     * Creates a vanilla transformation instance.
     * The uv-lock property is not represented by the returned instance.
     */
    Transformation toTransformation();

    /**
     * Creates a vanilla model state instance.
     */
    ModelState toModelState();
}
