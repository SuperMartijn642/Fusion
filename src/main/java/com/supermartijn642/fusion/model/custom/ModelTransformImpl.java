package com.supermartijn642.fusion.model.custom;

import com.supermartijn642.fusion.api.model.custom.ModelTransform;
import net.minecraft.client.renderer.Quaternion;
import net.minecraft.client.renderer.Vector3f;
import net.minecraft.client.renderer.texture.ISprite;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.common.model.ITransformation;
import net.minecraftforge.common.model.TRSRTransformation;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import javax.vecmath.Matrix3f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Quat4f;

/**
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

    public static ModelTransform of(TRSRTransformation transformation, boolean uvLock){
        return new ModelTransformImpl(transformation, uvLock);
    }

    public static ModelTransform of(ISprite modelState){
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
            matrix.mul(transform.matrix());
            uvLock |= transform.uvLock();
        }
        return new ModelTransformImpl(matrix, uvLock);
    }

    private final Matrix4f matrix;
    private final boolean uvLock;
    private Vector3f translation;
    private Quaternion leftRotation;
    private Vector3f scale;
    private Quaternion rightRotation;

    private TRSRTransformation transformation;
    private ISprite modelState;

    private ModelTransformImpl(Matrix4f matrix, boolean uvLock){
        this.matrix = matrix;
        this.uvLock = uvLock;
    }

    private ModelTransformImpl(ITransformation transformation, boolean uvLock){
        this(transformation.getMatrixVec(), uvLock);
        if(transformation instanceof TRSRTransformation)
            this.transformation = (TRSRTransformation)transformation;
    }

    private ModelTransformImpl(ISprite modelState){
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
        Pair<Matrix3f,javax.vecmath.Vector3f> pair = TRSRTransformation.toAffine(this.matrix);
        Triple<Quat4f,javax.vecmath.Vector3f,Quat4f> triple = TRSRTransformation.svdDecompose(pair.getLeft());
        this.translation = new Vector3f(pair.getRight().x, pair.getRight().y, pair.getRight().z);
        this.leftRotation = new Quaternion(triple.getLeft().x, triple.getLeft().y, triple.getLeft().z, triple.getLeft().w);
        this.scale = new Vector3f(triple.getMiddle().x, triple.getMiddle().y, triple.getMiddle().z);
        this.rightRotation = new Quaternion(triple.getRight().x, triple.getRight().y, triple.getRight().z, triple.getRight().w);
    }

    @Override
    public TRSRTransformation toTransformation(){
        if(this.transformation == null)
            this.transformation = new TRSRTransformation(new Matrix4f(this.matrix));
        return this.transformation;
    }

    @Override
    public ISprite toModelTransform(){
        if(this.modelState == null){
            // Create the model state
            this.modelState = new ISprite() {
                @Override
                public IModelState getState(){
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
}
