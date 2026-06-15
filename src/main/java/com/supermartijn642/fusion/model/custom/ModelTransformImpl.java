package com.supermartijn642.fusion.model.custom;

import com.mojang.math.MatrixUtil;
import com.mojang.math.Transformation;
import com.supermartijn642.fusion.api.model.custom.ModelTransform;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.BlockMath;
import net.minecraft.core.Direction;
import org.apache.commons.lang3.tuple.Triple;
import org.joml.*;
import org.joml.Math;

import java.util.EnumMap;
import java.util.Map;

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
        this(modelState.transformation(), modelState instanceof BlockModelRotation.WithUvLock);
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
            this.transformation = new Transformation(this.matrix);
        return this.transformation;
    }

    @Override
    public ModelState toModelState(){
        if(this.modelState == null){
            // Check whether this transform's rotations are all at multiples of 90 degrees
            boolean isAxisAlignedPreserving = true;
            Vector3f eulerRotation = this.leftRotation().getEulerAnglesXYZ(new Vector3f());
            if(!(Math.abs(eulerRotation.x) < PRECISION || Math.abs(eulerRotation.x - Math.PI_OVER_2_f) < PRECISION || Math.abs(eulerRotation.x - Math.PI_f) < PRECISION || Math.abs(eulerRotation.x - Math.PI_f - Math.PI_OVER_2_f) < PRECISION)
                || !(Math.abs(eulerRotation.y) < PRECISION || Math.abs(eulerRotation.y - Math.PI_OVER_2_f) < PRECISION || Math.abs(eulerRotation.y - Math.PI_f) < PRECISION || Math.abs(eulerRotation.y - Math.PI_f - Math.PI_OVER_2_f) < PRECISION)
                || !(Math.abs(eulerRotation.z) < PRECISION || Math.abs(eulerRotation.z - Math.PI_OVER_2_f) < PRECISION || Math.abs(eulerRotation.z - Math.PI_f) < PRECISION || Math.abs(eulerRotation.z - Math.PI_f - Math.PI_OVER_2_f) < PRECISION))
                isAxisAlignedPreserving = false;
            this.rightRotation.getEulerAnglesXYZ(eulerRotation);
            if(!(Math.abs(eulerRotation.x) < PRECISION || Math.abs(eulerRotation.x - Math.PI_OVER_2_f) < PRECISION || Math.abs(eulerRotation.x - Math.PI_f) < PRECISION || Math.abs(eulerRotation.x - Math.PI_f - Math.PI_OVER_2_f) < PRECISION)
                || !(Math.abs(eulerRotation.y) < PRECISION || Math.abs(eulerRotation.y - Math.PI_OVER_2_f) < PRECISION || Math.abs(eulerRotation.y - Math.PI_f) < PRECISION || Math.abs(eulerRotation.y - Math.PI_f - Math.PI_OVER_2_f) < PRECISION)
                || !(Math.abs(eulerRotation.z) < PRECISION || Math.abs(eulerRotation.z - Math.PI_OVER_2_f) < PRECISION || Math.abs(eulerRotation.z - Math.PI_f) < PRECISION || Math.abs(eulerRotation.z - Math.PI_f - Math.PI_OVER_2_f) < PRECISION))
                isAxisAlignedPreserving = false;
            boolean finalIsAxisAlignedPreserving = isAxisAlignedPreserving;
            // Create the model state
            if(!this.uvLock()){
                this.modelState = new ModelState() {
                    @Override
                    public Transformation transformation(){
                        return ModelTransformImpl.this.toTransformation();
                    }

                    @Override
                    public boolean mayApplyArbitraryRotation(){
                        return finalIsAxisAlignedPreserving;
                    }
                };
            }else{
                this.modelState = new ModelState() {
                    private final Map<Direction,Matrix4fc> faceTransformation = new EnumMap<>(Direction.class);
                    private final Map<Direction,Matrix4fc> inverseFaceTransformation = new EnumMap<>(Direction.class);

                    private void calculateFace(Direction face){
                        Matrix4fc faceTransform = BlockMath.getFaceTransformation(this.transformation(), face).getMatrix();
                        this.faceTransformation.put(face, faceTransform);
                        this.inverseFaceTransformation.put(face, faceTransform.invertAffine(new Matrix4f()));
                    }

                    @Override
                    public Transformation transformation(){
                        return ModelTransformImpl.this.toTransformation();
                    }

                    @Override
                    public Matrix4fc faceTransformation(Direction face){
                        Matrix4fc matrix = this.faceTransformation.get(face);
                        if(matrix == null){
                            this.calculateFace(face);
                            matrix = this.faceTransformation.get(face);
                        }
                        return matrix;
                    }

                    @Override
                    public Matrix4fc inverseFaceTransformation(Direction face){
                        Matrix4fc matrix = this.inverseFaceTransformation.get(face);
                        if(matrix == null){
                            this.calculateFace(face);
                            matrix = this.inverseFaceTransformation.get(face);
                        }
                        return matrix;
                    }

                    @Override
                    public boolean mayApplyArbitraryRotation(){
                        return finalIsAxisAlignedPreserving;
                    }
                };
            }
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
