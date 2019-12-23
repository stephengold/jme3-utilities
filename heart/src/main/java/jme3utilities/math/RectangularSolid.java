/*
 Copyright (c) 2018-2019, Stephen Gold
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

import com.jme3.bounding.BoundingBox;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.math.Eigen3f;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.shape.AbstractBox;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.Validate;

/**
 * A rectangular solid whose axes might not be axis-aligned. Immutable except
 * for {@link #read(com.jme3.export.JmeImporter)}.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class RectangularSolid implements Savable {
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
            = Logger.getLogger(RectangularSolid.class.getName());
    // *************************************************************************
    // fields

    /**
     * orientation of the local (principal) axes (default=identity)
     */
    private Quaternion localToWorld = new Quaternion();
    /**
     * maximum coordinate value for each local axis
     */
    private Vector3f maxima = new Vector3f();
    /**
     * minimum coordinate value for each local axis
     */
    private Vector3f minima = new Vector3f();
    // *************************************************************************
    // constructors

    /**
     * Instantiate a zero-size rectangular solid at the origin.
     */
    public RectangularSolid() {
    }

    /**
     * Instantiate a solid that exactly matches the specified mesh.
     *
     * @param mesh the input mesh (not null, unaffected)
     */
    public RectangularSolid(AbstractBox mesh) {
        maxima.set(mesh.center);
        maxima.addLocal(mesh.xExtent, mesh.yExtent, mesh.zExtent);

        minima.set(mesh.center);
        minima.subtractLocal(mesh.xExtent, mesh.yExtent, mesh.zExtent);
    }

    /**
     * Instantiate a solid that matches the specified axis-aligned bounding box.
     *
     * @param aabb the axis-aligned bounding box (not null, unaffected)
     */
    public RectangularSolid(BoundingBox aabb) {
        aabb.getMax(maxima);
        aabb.getMin(minima);
    }

    /**
     * Instantiate a compact solid that bounds the specified collection of
     * sample locations.
     *
     * @param sampleLocations the sample locations (not null, at least 2,
     * unaffected)
     */
    public RectangularSolid(Collection<Vector3f> sampleLocations) {
        Validate.nonEmpty(sampleLocations, "sample locations");
        int numSamples = sampleLocations.size();
        assert numSamples > 1 : numSamples;
        /*
         * Orient local axes based on the eigenvectors of the covariance matrix.
         */
        Matrix3f covariance = MyVector3f.covariance(sampleLocations, null);
        Eigen3f eigen = new Eigen3f(covariance);
        Vector3f[] basis = eigen.getEigenVectors();
        localToWorld.fromAxes(basis);
        /*
         * Calculate the min and max for each local axis.
         */
        maxima.set(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY,
                Float.NEGATIVE_INFINITY);
        minima.set(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
                Float.POSITIVE_INFINITY);
        Quaternion worldToLocal = localToWorld.inverse();
        Vector3f tempVector = new Vector3f();
        for (Vector3f world : sampleLocations) {
            worldToLocal.mult(world, tempVector);
            MyVector3f.accumulateMaxima(maxima, tempVector);
            MyVector3f.accumulateMinima(minima, tempVector);
        }
    }

    /**
     * Instantiate a compact solid that bounds the sample locations in the
     * specified float array.
     *
     * @param inputArray the sample locations (not null, at least 6 elements,
     * length a multiple of 3, unaffected)
     */
    public RectangularSolid(float[] inputArray) {
        Validate.nonEmpty(inputArray, "input array");
        int numFloats = inputArray.length;
        assert (numFloats % numAxes == 0) : numFloats;
        int numVectors = numFloats / numAxes;
        assert numVectors >= 2 : numVectors;
        /*
         * Orient local axes based on the eigenvectors of the covariance matrix.
         */
        Matrix3f covariance = MyArray.covarianceVector3f(inputArray, null);
        Eigen3f eigen = new Eigen3f(covariance);
        Vector3f[] basis = eigen.getEigenVectors();
        localToWorld.fromAxes(basis);
        /*
         * Calculate the min and max for each local axis.
         */
        maxima.set(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY,
                Float.NEGATIVE_INFINITY);
        minima.set(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
                Float.POSITIVE_INFINITY);
        Quaternion worldToLocal = localToWorld.inverse();
        Vector3f tempVector = new Vector3f();
        for (int vectorIndex = 0; vectorIndex < numVectors; ++vectorIndex) {
            tempVector.x = inputArray[vectorIndex * numAxes + MyVector3f.xAxis];
            tempVector.y = inputArray[vectorIndex * numAxes + MyVector3f.yAxis];
            tempVector.z = inputArray[vectorIndex * numAxes + MyVector3f.zAxis];

            worldToLocal.mult(tempVector, tempVector);
            MyVector3f.accumulateMaxima(maxima, tempVector);
            MyVector3f.accumulateMinima(minima, tempVector);
        }
    }

    /**
     * Instantiate a compact solid that bounds the sample locations in the
     * specified FloatBuffer range.
     *
     * @param buffer the buffer that contains the sample locations (not null,
     * unaffected)
     * @param startPosition the position at which the sample locations start
     * (&ge;0, &le;endPosition-6)
     * @param endPosition the position at which the sample locations end
     * (&ge;startPosition+6, &le;capacity)
     */
    public RectangularSolid(FloatBuffer buffer, int startPosition,
            int endPosition) {
        Validate.nonNull(buffer, "buffer");
        Validate.inRange(startPosition, "start position", 0,
                endPosition - 2 * numAxes);
        Validate.inRange(endPosition, "end position",
                startPosition + 2 * numAxes, buffer.capacity());

        int numFloats = endPosition - startPosition;
        assert (numFloats % numAxes == 0) : numFloats;
        int numVectors = numFloats / numAxes;
        assert numVectors >= 2 : numVectors;
        /*
         * Orient local axes based on the eigenvectors of the covariance matrix.
         */
        Matrix3f covariance = MyBuffer.covariance(buffer, 0, numFloats, null);
        Eigen3f eigen = new Eigen3f(covariance);
        Vector3f[] basis = eigen.getEigenVectors();
        localToWorld.fromAxes(basis);
        /*
         * Calculate the min and max for each local axis.
         */
        maxima.set(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY,
                Float.NEGATIVE_INFINITY);
        minima.set(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
                Float.POSITIVE_INFINITY);
        Quaternion worldToLocal = localToWorld.inverse();
        Vector3f tmpVector = new Vector3f();

        for (int vectorIndex = 0; vectorIndex < numVectors; ++vectorIndex) {
            int position = vectorIndex * numAxes;
            MyBuffer.get(buffer, position, tmpVector);
            worldToLocal.mult(tmpVector, tmpVector);
            MyVector3f.accumulateMaxima(maxima, tmpVector);
            MyVector3f.accumulateMinima(minima, tmpVector);
        }
    }

    /**
     * Instantiate a centered solid with the specified half extents.
     *
     * @param halfExtents half extents the axis-aligned bounding box (not null,
     * unaffected)
     */
    public RectangularSolid(Vector3f halfExtents) {
        maxima.set(halfExtents);
        halfExtents.mult(-1f, minima);
    }

    /**
     * Instantiate a solid with the specified minima, maxima, and rotation.
     *
     * @param min the minimum coordinate value for each local axis (not null,
     * unaffected)
     * @param max the maximum coordinate value for each local axis (not null,
     * unaffected)
     * @param orientation the orientation of the local axes (not null,
     * unaffected)
     */
    public RectangularSolid(Vector3f min, Vector3f max, Quaternion orientation) {
        assert min.x <= max.x;
        assert min.y <= max.y;
        assert min.z <= max.z;

        maxima.set(max);
        minima.set(min);
        localToWorld.set(orientation);
    }

    /**
     * Instantiate a rectangular solid by scaling another solid around its
     * center.
     *
     * @param otherSolid (not null, unaffected)
     * @param scaleFactors (not null, all components non-negative, unaffected)
     */
    public RectangularSolid(RectangularSolid otherSolid, Vector3f scaleFactors) {
        Validate.nonNegative(scaleFactors, "scale factors");

        Vector3f center = MyVector3f.midpoint(otherSolid.minima,
                otherSolid.maxima, null);

        otherSolid.maxima.subtract(center, maxima);
        maxima.multLocal(scaleFactors);
        maxima.addLocal(center);

        otherSolid.minima.subtract(center, minima);
        minima.multLocal(scaleFactors);
        minima.addLocal(center);

        localToWorld.set(otherSolid.localToWorld);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Calculate the half extents of the solid.
     *
     * @param storeResult storage for the result (modified if not null)
     * @return the half extent of each local axis (either storeResult or a new
     * vector, not null, all components non-negative)
     */
    public Vector3f halfExtents(Vector3f storeResult) {
        Vector3f result = (storeResult == null) ? new Vector3f() : storeResult;
        maxima.subtract(minima, result);
        result.divideLocal(2f);

        assert result.x >= 0f : result.x;
        assert result.y >= 0f : result.y;
        assert result.z >= 0f : result.z;
        return result;
    }

    /**
     * Enumerate the corner locations of the specified RectangularSolid.
     *
     * @return a new list of new vectors
     */
    public List<Vector3f> listCorners() {
        /*
         * Enumerate the local coordinates of the 8 corners of the box.
         */
        List<Vector3f> cornerLocations = new ArrayList<>(8);
        cornerLocations.add(new Vector3f(maxima.x, maxima.y, maxima.z));
        cornerLocations.add(new Vector3f(maxima.x, maxima.y, minima.z));
        cornerLocations.add(new Vector3f(maxima.x, minima.y, maxima.z));
        cornerLocations.add(new Vector3f(maxima.x, minima.y, minima.z));
        cornerLocations.add(new Vector3f(minima.x, maxima.y, maxima.z));
        cornerLocations.add(new Vector3f(minima.x, maxima.y, minima.z));
        cornerLocations.add(new Vector3f(minima.x, minima.y, maxima.z));
        cornerLocations.add(new Vector3f(minima.x, minima.y, minima.z));
        /*
         * Transform local coordinates to world coordinates.
         */
        for (Vector3f location : cornerLocations) {
            localToWorld.mult(location, location);
        }

        return cornerLocations;
    }

    /**
     * Copy the local-to-world rotation.
     *
     * @param storeResult storage for the result (modified if not null)
     * @return the maxima (either storeResult or a new vector, not null)
     */
    public Quaternion localToWorld(Quaternion storeResult) {
        Quaternion result;
        if (storeResult == null) {
            result = localToWorld.clone();
        } else {
            result = storeResult.set(localToWorld);
        }

        return result;
    }

    /**
     * Rotate from local coordinates to world coordinates.
     *
     * @param local the input coordinates (not null, unaffected)
     * @param storeResult storage for the result (modified if not null)
     * @return the corresponding world coordinates (either storeResult or a new
     * vector, not null)
     */
    public Vector3f localToWorld(Vector3f local, Vector3f storeResult) {
        Validate.finite(local, "local");
        Vector3f result = (storeResult == null) ? new Vector3f() : storeResult;

        localToWorld.mult(local, result);
        return result;
    }

    /**
     * Copy the maximum coordinate value for each local axis.
     *
     * @param storeResult storage for the result (modified if not null)
     * @return the maxima (either storeResult or a new vector, not null)
     */
    public Vector3f maxima(Vector3f storeResult) {
        Vector3f result;
        if (storeResult == null) {
            result = maxima.clone();
        } else {
            result = storeResult.set(maxima);
        }
        return result;
    }

    /**
     * Copy minimum coordinate value for each local axis.
     *
     * @param storeResult storage for the result (modified if not null)
     * @return the minima (either storeResult or a new vector, not null)
     */
    public Vector3f minima(Vector3f storeResult) {
        Vector3f result;
        if (storeResult == null) {
            result = minima.clone();
        } else {
            result = storeResult.set(minima);
        }
        return result;
    }

    /**
     * Calculate the volume of the solid.
     *
     * @return the volume (in cubic world units, &ge;0)
     */
    public float volume() {
        float dx = maxima.x - minima.x;
        float dy = maxima.y - minima.y;
        float dz = maxima.z - minima.z;
        float volume = dx * dy * dz;

        assert volume >= 0f : volume;
        return volume;
    }
    // *************************************************************************
    // Object methods

    /**
     * Represent this solid as a text string.
     *
     * @return descriptive string of text (not null, not empty)
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(80);

        builder.append("RectangularSolid[");
        builder.append(localToWorld.toString());
        builder.append(", min=");
        builder.append(minima.toString());
        builder.append(", max=");
        builder.append(maxima.toString());
        builder.append("]");

        String description = builder.toString();
        return description;
    }
    // *************************************************************************
    // Savable methods

    /**
     * De-serialize this solid, for example when loading from a J3O file.
     *
     * @param importer (not null)
     * @throws IOException from importer
     */
    @Override
    public void read(JmeImporter importer) throws IOException {
        InputCapsule capsule = importer.getCapsule(this);

        localToWorld = (Quaternion) capsule.readSavable("localToWorld", null);
        maxima = (Vector3f) capsule.readSavable("maxima", null);
        minima = (Vector3f) capsule.readSavable("minima", null);
    }

    /**
     * Serialize this solid, for example when saving to a J3O file.
     *
     * @param exporter (not null)
     * @throws IOException from exporter
     */
    @Override
    public void write(JmeExporter exporter) throws IOException {
        OutputCapsule capsule = exporter.getCapsule(this);

        capsule.write(localToWorld, "localToWorld", null);
        capsule.write(maxima, "maxima", null);
        capsule.write(minima, "minima", null);
    }
}
