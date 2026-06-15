package com.supermartijn642.fusion.model.custom;

import com.mojang.datafixers.util.Pair;
import com.supermartijn642.fusion.api.model.custom.ModelTransform;
import net.minecraft.client.renderer.model.IModelTransform;
import net.minecraft.util.math.vector.*;
import org.apache.commons.lang3.tuple.Triple;

/**
 * Most of this class is just copied from {@link TransformationMatrix} as I have no clue how rotation math works.
 * <p>
 * Created 04/05/2026 by SuperMartijn642
 */
public class ModelTransformImpl implements ModelTransform {

    private static final ModelTransform IDENTITY;

    static{
        Matrix4f identity = new Matrix4f();
        identity.setIdentity();
        IDENTITY = of(identity, false);
    }

    public static ModelTransform of(Matrix4f matrix, boolean uvLock){
        return new ModelTransformImpl(new Matrix4f(matrix), uvLock);
    }

    public static ModelTransform of(TransformationMatrix transformation, boolean uvLock){
        return new ModelTransformImpl(transformation, uvLock);
    }

    public static ModelTransform of(IModelTransform modelState){
        return new ModelTransformImpl(modelState);
    }

    public static ModelTransform identity(){
        return IDENTITY;
    }

    public static ModelTransform compose(ModelTransform... transforms){
        Matrix4f matrix = new Matrix4f();
        matrix.setIdentity();
        boolean uvLock = false;
        for(ModelTransform transform : transforms){
            matrix.multiply(transform.matrix());
            uvLock |= transform.uvLock();
        }
        return new ModelTransformImpl(matrix, uvLock);
    }

    private static final float PRECISION = 1e-7f;

    private final Matrix4f matrix;
    private final boolean uvLock;
    private Vector3f translation;
    private Quaternion leftRotation;
    private Vector3f scale;
    private Quaternion rightRotation;

    private TransformationMatrix transformation;
    private IModelTransform modelState;

    private ModelTransformImpl(Matrix4f matrix, boolean uvLock){
        this.matrix = matrix;
        this.uvLock = uvLock;
    }

    private ModelTransformImpl(TransformationMatrix transformation, boolean uvLock){
        this(transformation.getMatrix(), uvLock);
        this.transformation = transformation;
        if(transformation.decomposed){
            this.translation = transformation.getTranslation();
            this.leftRotation = transformation.getLeftRotation();
            this.scale = transformation.getScale();
            this.rightRotation = transformation.getRightRot();
        }
    }

    private ModelTransformImpl(IModelTransform modelState){
        this(modelState.getRotation(), modelState.isUvLocked());
        this.modelState = modelState;
    }

    @Override
    public boolean uvLock(){
        return this.uvLock;
    }

    @Override
    public Matrix4f matrix(){
        return this.matrix;
    }

    @Override
    public Vector3f translation(){
        this.decompose();
        return this.translation;
    }

    @Override
    public Quaternion leftRotation(){
        this.decompose();
        return this.leftRotation;
    }

    @Override
    public Vector3f scale(){
        this.decompose();
        return this.scale;
    }

    @Override
    public Quaternion rightRotation(){
        this.decompose();
        return this.rightRotation;
    }

    private void decompose(){
        if(this.translation != null)
            return;
        Pair<Matrix3f,Vector3f> pair = TransformationMatrix.toAffine(this.matrix);
        Triple<Quaternion,Vector3f,Quaternion> triple = pair.getFirst().svdDecompose();
        this.translation = pair.getSecond();
        this.leftRotation = triple.getLeft();
        this.scale = triple.getMiddle();
        this.rightRotation = triple.getRight();
    }

    @Override
    public TransformationMatrix toTransformationMatrix(){
        if(this.transformation == null)
            this.transformation = new TransformationMatrix(new Matrix4f(this.matrix));
        return this.transformation;
    }

    @Override
    public IModelTransform toModelTransform(){
        if(this.modelState == null){
            // Create the model state
            this.modelState = new IModelTransform() {
                @Override
                public TransformationMatrix getRotation(){
                    return ModelTransformImpl.this.toTransformationMatrix();
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
        if(!(o instanceof ModelTransformImpl)) return false;

        ModelTransformImpl that = (ModelTransformImpl)o;
        return this.uvLock == that.uvLock && this.matrix.equals(that.matrix);
    }

    @Override
    public int hashCode(){
        int result = this.matrix.hashCode();
        result = 31 * result + Boolean.hashCode(this.uvLock);
        return result;
    }
}
