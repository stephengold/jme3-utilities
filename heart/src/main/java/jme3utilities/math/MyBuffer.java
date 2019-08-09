/*
 Copyright (c) 2019, Stephen Gold
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright
 notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 notice, this list of conditions and the following disclaimer in the
 documentation and/or other materials provided with the distribution.
 * Neither the name of the copyright holder nor the names of its contributors
 may be used to endorse or promote products derived from this software without
 specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package jme3utilities.math;

import com.jme3.math.Matrix3f;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import java.nio.FloatBuffer;
import java.util.logging.Logger;
import jme3utilities.Validate;

/**
 * Utility methods that operate on buffers.
 *
 * @author Stephen Gold sgold@sonic.net
 */
final public class MyBuffer {
    // *************************************************************************
    // constants and loggers

    /**
     * number of axes in a vector
     */
    final private static int numAxes = 3;
    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(MyBuffer.class.getName());
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private MyBuffer() {
    }
    // *************************************************************************
    // new methods exposed - TODO add countDistinct()

    /**
     * Calculate the sample covariance of 3-D vectors in the specified
     * FloatBuffer range.
     *
     * @param buffer the buffer that contains the vectors (not null, unaffected)
     * @param startPosition the position at which the vectors start (&ge;0,
     * &le;endPosition-6)
     * @param endPosition the position at which the vectors end
     * (&ge;startPosition+6, &le;capacity)
     * @param storeResult storage for the result (modified if not null)
     * @return the unbiased sample covariance (either storeResult or a new
     * matrix, not null)
     */
    public static Matrix3f covariance(FloatBuffer buffer, int startPosition,
            int endPosition, Matrix3f storeResult) {
        Validate.nonNull(buffer, "buffer");
        Validate.inRange(startPosition, "start position", 0,
                endPosition - 2 * numAxes);
        Validate.inRange(endPosition, "end position",
                startPosition + 2 * numAxes, buffer.capacity());
        Matrix3f result = (storeResult == null) ? new Matrix3f() : storeResult;

        int numFloats = endPosition - startPosition;
        assert (numFloats % numAxes == 0) : numFloats;
        int numVectors = numFloats / numAxes;
        Vector3f sampleMean = mean(buffer, startPosition, endPosition, null);
        /*
         * Accumulate sums in the upper triangle of the matrix.
         */
        result.zero();
        float[] aboveMean = new float[numAxes];
        for (int vectorIndex = 0; vectorIndex < numVectors; ++vectorIndex) {
            int position = startPosition + vectorIndex * numAxes;

            float x = buffer.get(position + MyVector3f.xAxis);
            float y = buffer.get(position + MyVector3f.yAxis);
            float z = buffer.get(position + MyVector3f.zAxis);
            aboveMean[0] = x - sampleMean.x;
            aboveMean[1] = y - sampleMean.y;
            aboveMean[2] = z - sampleMean.z;
            for (int rowIndex = 0; rowIndex < numAxes; ++rowIndex) {
                for (int colIndex = rowIndex; colIndex < numAxes; ++colIndex) {
                    float sum = result.get(rowIndex, colIndex);
                    sum += aboveMean[rowIndex] * aboveMean[colIndex];
                    result.set(rowIndex, colIndex, sum);
                }
            }
        }
        /*
         * Multiply sums by 1/(N-1) and fill in the lower triangle.
         */
        float nMinus1 = numVectors - 1;
        for (int rowIndex = 0; rowIndex < numAxes; ++rowIndex) {
            for (int colIndex = rowIndex; colIndex < numAxes; ++colIndex) {
                float sum = result.get(rowIndex, colIndex);
                float element = sum / nMinus1;
                result.set(rowIndex, colIndex, element);
                result.set(colIndex, rowIndex, element);
            }
        }

        return result;
    }

    /**
     * Find the the radius of a bounding cylinder for the specified FloatBuffer
     * range.
     *
     * @param buffer the buffer that contains the vectors (not null, unaffected)
     * @param startPosition the position at which the vectors start (&ge;0,
     * &le;endPosition)
     * @param endPosition the position at which the vectors end
     * (&ge;startPosition, &le;capacity)
     * @param axisIndex the cylinder's height axis: 0&rarr;X, 1&rarr;Y, 2&rarr;Z
     * @return the radius of the minimum bounding cylinder centered at the
     * origin (&ge;0)
     */
    public static float cylinderRadius(FloatBuffer buffer, int startPosition,
            int endPosition, int axisIndex) {
        Validate.nonNull(buffer, "buffer");
        Validate.inRange(startPosition, "start position", 0, endPosition);
        Validate.inRange(endPosition, "end position", startPosition,
                buffer.capacity());
        Validate.inRange(axisIndex, "axis index", MyVector3f.xAxis,
                MyVector3f.zAxis);

        int numFloats = endPosition - startPosition;
        assert (numFloats % numAxes == 0) : numFloats;
        double maxRadiusSquared = 0.0;
        Vector3f tmpVector = new Vector3f();
        int numVectors = numFloats / numAxes;

        for (int vectorIndex = 0; vectorIndex < numVectors; ++vectorIndex) {
            int position = startPosition + vectorIndex * numAxes;

            float x = buffer.get(position + MyVector3f.xAxis);
            float y = buffer.get(position + MyVector3f.yAxis);
            float z = buffer.get(position + MyVector3f.zAxis);
            float radiusSquared;
            switch (axisIndex) {
                case MyVector3f.xAxis:
                    radiusSquared = y * y + z * z;
                    break;
                case MyVector3f.yAxis:
                    radiusSquared = x * x + z * z;
                    break;
                case MyVector3f.zAxis:
                    radiusSquared = x * x + y * y;
                    break;
                default:
                    String message = Integer.toString(axisIndex);
                    throw new RuntimeException(message);
            }

            if (radiusSquared > maxRadiusSquared) {
                maxRadiusSquared = radiusSquared;
            }
        }

        float result = (float) Math.sqrt(maxRadiusSquared);
        assert result >= 0f : result;
        return result;
    }

    /**
     * Find the maximum absolute coordinate for each axis in the specified
     * FloatBuffer range.
     *
     * @param buffer the buffer that contains the vectors (not null, unaffected)
     * @param startPosition the position at which the vectors start (&ge;0,
     * &le;endPosition)
     * @param endPosition the position at which the vectors end
     * (&ge;startPosition, &le;capacity)
     * @param storeResult storage for the result (modified if not null)
     * @return the half extent for each axis (either storeResult or a new
     * instance)
     */
    public static Vector3f maxAbs(FloatBuffer buffer, int startPosition,
            int endPosition, Vector3f storeResult) {
        Validate.nonNull(buffer, "buffer");
        Validate.inRange(startPosition, "start position", 0, endPosition);
        Validate.inRange(endPosition, "end position", startPosition,
                buffer.capacity());
        Vector3f result = (storeResult == null) ? new Vector3f() : storeResult;

        int numFloats = endPosition - startPosition;
        assert (numFloats % numAxes == 0) : numFloats;
        int numVectors = numFloats / numAxes;
        result.zero();

        for (int vectorIndex = 0; vectorIndex < numVectors; ++vectorIndex) {
            int position = startPosition + vectorIndex * numAxes;
            float x = buffer.get(position + MyVector3f.xAxis);
            float y = buffer.get(position + MyVector3f.yAxis);
            float z = buffer.get(position + MyVector3f.zAxis);
            result.x = Math.max(result.x, Math.abs(x));
            result.y = Math.max(result.y, Math.abs(y));
            result.z = Math.max(result.z, Math.abs(z));
        }

        return result;
    }

    /**
     * Find the magnitude of the longest 3-D vector in the specified FloatBuffer
     * range.
     *
     * @param buffer the buffer that contains the vectors (not null, unaffected)
     * @param startPosition the position at which the vectors start (&ge;0,
     * &le;endPosition)
     * @param endPosition the position at which the vectors end
     * (&ge;startPosition, &le;capacity)
     * @return the radius of the minimum bounding sphere centered at the origin
     * (&ge;0)
     */
    public static float maxLength(FloatBuffer buffer, int startPosition,
            int endPosition) {
        Validate.nonNull(buffer, "buffer");
        Validate.inRange(startPosition, "start position", 0, endPosition);
        Validate.inRange(endPosition, "end position", startPosition,
                buffer.capacity());

        int numFloats = endPosition - startPosition;
        assert (numFloats % numAxes == 0) : numFloats;
        double maxLengthSquared = 0.0;
        Vector3f tmpVector = new Vector3f();
        int numVectors = numFloats / numAxes;

        for (int vectorIndex = 0; vectorIndex < numVectors; ++vectorIndex) {
            int position = startPosition + vectorIndex * numAxes;

            tmpVector.x = buffer.get(position + MyVector3f.xAxis);
            tmpVector.y = buffer.get(position + MyVector3f.yAxis);
            tmpVector.z = buffer.get(position + MyVector3f.zAxis);

            double lengthSquared = MyVector3f.lengthSquared(tmpVector);
            if (lengthSquared > maxLengthSquared) {
                maxLengthSquared = lengthSquared;
            }
        }

        float result = (float) Math.sqrt(maxLengthSquared);
        assert result >= 0f : result;
        return result;
    }

    /**
     * Find the maximum and minimum coordinates of 3-D vectors in the specified
     * FloatBuffer range. An empty range stores infinities.
     *
     * @see com.jme3.bounding.BoundingBox#containAABB(java.nio.FloatBuffer)
     *
     * @param buffer the buffer that contains the vectors (not null, unaffected)
     * @param startPosition the position at which the vectors start (&ge;0,
     * &le;endPosition)
     * @param endPosition the position at which the vectors end
     * (&ge;startPosition, &le;capacity)
     * @param storeMaxima storage for maxima (not null, modified)
     * @param storeMinima storage for minima (not null, modified)
     */
    public static void maxMin(FloatBuffer buffer, int startPosition,
            int endPosition, Vector3f storeMaxima, Vector3f storeMinima) {
        Validate.nonNull(buffer, "buffer");
        Validate.inRange(startPosition, "start position", 0, endPosition);
        Validate.inRange(endPosition, "end position", startPosition,
                buffer.capacity());

        int numFloats = endPosition - startPosition;
        assert (numFloats % numAxes == 0) : numFloats;
        storeMaxima.set(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY,
                Float.NEGATIVE_INFINITY);
        storeMinima.set(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
                Float.POSITIVE_INFINITY);
        Vector3f tmpVector = new Vector3f();
        int numVectors = numFloats / numAxes;

        for (int vectorIndex = 0; vectorIndex < numVectors; ++vectorIndex) {
            int position = startPosition + vectorIndex * numAxes;

            tmpVector.x = buffer.get(position + MyVector3f.xAxis);
            tmpVector.y = buffer.get(position + MyVector3f.yAxis);
            tmpVector.z = buffer.get(position + MyVector3f.zAxis);

            MyVector3f.accumulateMinima(storeMinima, tmpVector);
            MyVector3f.accumulateMaxima(storeMaxima, tmpVector);
        }
    }

    /**
     * Calculate the arithmetic mean of 3-D vectors in the specified FloatBuffer
     * range.
     *
     * @param buffer the buffer that contains the vectors (not null, unaffected)
     * @param startPosition the position at which the vectors start (&ge;0,
     * &le;endPosition-3)
     * @param endPosition the position at which the vectors end
     * (&ge;startPosition+3, &le;capacity)
     * @param storeResult storage for the result (modified if not null)
     * @return the mean (either storeResult or a new vector, not null)
     */
    public static Vector3f mean(FloatBuffer buffer, int startPosition,
            int endPosition, Vector3f storeResult) {
        Validate.nonNull(buffer, "buffer");
        Validate.inRange(startPosition, "start position", 0,
                endPosition - numAxes);
        Validate.inRange(endPosition, "end position", startPosition + numAxes,
                buffer.capacity());
        Vector3f result = (storeResult == null) ? new Vector3f() : storeResult;

        int numFloats = endPosition - startPosition;
        assert (numFloats % numAxes == 0) : numFloats;
        int numVectors = numFloats / numAxes;
        result.zero();

        for (int vectorIndex = 0; vectorIndex < numVectors; ++vectorIndex) {
            int position = startPosition + vectorIndex * numAxes;

            float x = buffer.get(position + MyVector3f.xAxis);
            float y = buffer.get(position + MyVector3f.yAxis);
            float z = buffer.get(position + MyVector3f.zAxis);

            result.addLocal(x, y, z);
        }
        result.divideLocal(numVectors);

        return result;
    }

    /**
     * Apply the specified coordinate transform to 3-D vectors in the specified
     * FloatBuffer range.
     *
     * @param buffer the buffer that contains the vectors (not null, MODIFIED)
     * @param startPosition the position at which the vectors start (&ge;0,
     * &le;endPosition)
     * @param endPosition the position at which the vectors end
     * (&ge;startPosition, &le;capacity)
     * @param transform the transform to apply (not null, unaffected)
     */
    public static void transform(FloatBuffer buffer, int startPosition,
            int endPosition, Transform transform) {
        Validate.nonNull(buffer, "buffer");
        Validate.nonNull(transform, "transform");
        Validate.inRange(startPosition, "start position", 0, endPosition);
        Validate.inRange(endPosition, "end position", startPosition,
                buffer.capacity());
        int numFloats = endPosition - startPosition;
        assert (numFloats % numAxes == 0) : numFloats;

        Vector3f tmpVector = new Vector3f();
        int numVectors = numFloats / numAxes;

        for (int vectorIndex = 0; vectorIndex < numVectors; ++vectorIndex) {
            int position = startPosition + vectorIndex * numAxes;

            tmpVector.x = buffer.get(position + MyVector3f.xAxis);
            tmpVector.y = buffer.get(position + MyVector3f.yAxis);
            tmpVector.z = buffer.get(position + MyVector3f.zAxis);

            transform.transformVector(tmpVector, tmpVector);

            buffer.put(position + MyVector3f.xAxis, tmpVector.x);
            buffer.put(position + MyVector3f.yAxis, tmpVector.y);
            buffer.put(position + MyVector3f.zAxis, tmpVector.z);
        }
    }

    /**
     * Add the specified offset to 3-D vectors in the specified FloatBuffer
     * range.
     *
     * @param buffer the buffer that contains the vectors (not null, MODIFIED)
     * @param startPosition the position at which the vectors start (&ge;0,
     * &le;endPosition)
     * @param endPosition the position at which the vectors end
     * (&ge;startPosition, &le;capacity)
     * @param offsetVector the vector to add (not null, unaffected)
     */
    public static void translate(FloatBuffer buffer, int startPosition,
            int endPosition, Vector3f offsetVector) {
        Validate.nonNull(buffer, "buffer");
        Validate.inRange(startPosition, "start position", 0, endPosition);
        Validate.inRange(endPosition, "end position", startPosition,
                buffer.capacity());
        Validate.finite(offsetVector, "offset vector");

        int numFloats = endPosition - startPosition;
        assert (numFloats % numAxes == 0) : numFloats;
        int numVectors = numFloats / numAxes;

        for (int vectorIndex = 0; vectorIndex < numVectors; ++vectorIndex) {
            int position = startPosition + vectorIndex * numAxes;

            float x = buffer.get(position + MyVector3f.xAxis);
            x += offsetVector.x;
            buffer.put(position + MyVector3f.xAxis, x);

            float y = buffer.get(position + MyVector3f.yAxis);
            y += offsetVector.y;
            buffer.put(position + MyVector3f.yAxis, y);

            float z = buffer.get(position + MyVector3f.zAxis);
            z += offsetVector.z;
            buffer.put(position + MyVector3f.zAxis, z);
        }
    }
}
