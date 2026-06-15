package com.supermartijn642.fusion.model.custom;

import com.mojang.math.MatrixUtil;
import com.mojang.math.Transformation;
import com.supermartijn642.fusion.api.model.custom.ModelTransform;
import net.minecraft.client.resources.model.ModelState;
import org.apache.commons.lang3.tuple.Triple;
import org.joml.*;

/**
 * Most of this class is just copied from {@link Transformation} as I have no clue how rotation math works.
 * <p>
 * Created 04/05/2026 by SuperMartijn642
 */
public class ModelTransformImpl implements ModelTransform {

    private static final ModelTransform IDENTITY = of(new Matrix4f().identity(), false);

    public static ModelTransform of(Matrix4fc matrix, boolean uvLock){
        return new ModelTransformImpl(new Matrix4f(matrix), uvLock);
    }

    public static ModelTransform of(Transformation transformation, boolean uvLock){
        return new ModelTransformImpl(transformation, uvLock);
    }

    public static ModelTransform of(ModelState modelState){
        return new ModelTransformImpl(modelState);
    }

    public static ModelTransform identity(){
        return IDENTITY;
    }

    public static ModelTransform compose(ModelTransform... transforms){
        Matrix4f matrix = new Matrix4f().identity();
        boolean uvLock = false;
        for(ModelTransform transform : transforms){
            matrix.mul(transform.matrix());
            uvLock |= transform.uvLock();
        }
        return new ModelTransformImpl(matrix, uvLock);
    }

    private static final float PRECISION = 1e-7f;

    private final Matrix4fc matrix;
    private final boolean uvLock;
    private Vector3fc translation;
    private Quaternionfc leftRotation;
    private Vector3fc scale;
    private Quaternionfc rightRotation;

    private Transformation transformation;
    private ModelState modelState;

    private ModelTransformImpl(Matrix4fc matrix, boolean uvLock){
        this.matrix = matrix;
        this.uvLock = uvLock;
    }

    private ModelTransformImpl(Transformation transformation, boolean uvLock){
        this(transformation.getMatrix(), uvLock);
        this.transformation = transformation;
        if(transformation.decomposed){
            this.translation = transformation.getTranslation();
            this.leftRotation = transformation.getLeftRotation();
            this.scale = transformation.getScale();
            this.rightRotation = transformation.getRightRotation();
        }
    }

    private ModelTransformImpl(ModelState modelState){
        this(modelState.getRotation(), modelState.isUvLocked());
        this.modelState = modelState;
    }

    @Override
    public boolean uvLock(){
        return this.uvLock;
    }

    @Override
    public Matrix4fc matrix(){
        return this.matrix;
    }

    @Override
    public Vector3fc translation(){
        this.decompose();
        return this.translation;
    }

    @Override
    public Quaternionfc leftRotation(){
        this.decompose();
        return this.leftRotation;
    }

    @Override
    public Vector3fc scale(){
        this.decompose();
        return this.scale;
    }

    @Override
    public Quaternionfc rightRotation(){
        this.decompose();
        return this.rightRotation;
    }

    private void decompose(){
        if(this.translation != null)
            return;
        float scaleFactor = 1 / this.matrix.m33();
        Triple<Quaternionf,Vector3f,Quaternionf> triple = MatrixUtil.svdDecompose(new Matrix3f(this.matrix).scale(scaleFactor));
        this.translation = this.matrix.getTranslation(new Vector3f()).mul(scaleFactor);
        this.leftRotation = new Quaternionf(triple.getLeft());
        this.scale = new Vector3f(triple.getMiddle());
        this.rightRotation = new Quaternionf(triple.getRight());
    }

    @Override
    public Transformation toTransformation(){
        if(this.transformation == null)
            this.transformation = new Transformation(new Matrix4f(this.matrix));
        return this.transformation;
    }

    @Override
    public ModelState toModelState(){
        if(this.modelState == null){
            // Create the model state
            this.modelState = new ModelState() {
                @Override
                public Transformation getRotation(){
                    return ModelTransformImpl.this.toTransformation();
                }

                @Override
                public boolean isUvLocked(){
                    return ModelTransformImpl.this.uvLock();
                }
            };
        }
        return this.modelState;
    }

    @Override
    public final boolean equals(Object o){
        if(!(o instanceof ModelTransformImpl that)) return false;

        return this.uvLock == that.uvLock && this.matrix.equals(that.matrix);
    }

    @Override
    public int hashCode(){
        int result = this.matrix.hashCode();
        result = 31 * result + Boolean.hashCode(this.uvLock);
        return result;
    }
}
