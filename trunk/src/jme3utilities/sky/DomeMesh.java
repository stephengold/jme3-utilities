// (c) Copyright 2013 Stephen Gold <sgold@sonic.net>
// Distributed under the terms of the GNU General Public License

/*
 This file is part of the JME3 Utilities Package.

 The JME3 Utilities Package is free software: you can redistribute it and/or
 modify it under the terms of the GNU General Public License as published by the
 Free Software Foundation, either version 3 of the License, or (at your
 option) any later version.

 The JME3 Utilities Package is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 for more details.

 You should have received a copy of the GNU General Public License along with
 the JME3 Utilities Package.  If not, see <http://www.gnu.org/licenses/>.
 */
package jme3utilities.sky;

import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyMath;

/**
 * A custom mesh which generates a hemispherical dome of radius=1, centered at
 * the origin, with its top at y=1 and its rim in the XZ plane.
 *
 * The main differences between this class and com.jme3.scene.shape.Dome are:
 * (1) the radius and center are not configurable and (2) the texture
 * coordinates are configurable.
 *
 * The projection to texture space is an "azimuthal equidistant projection". The
 * maximum U coordinate (= topU + uvScale) occurs at X=1. Y=0, Z=0. The maximum
 * V coordinate (= topV + uvScale) occurs at X=0, Y=0, Z=-1.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class DomeMesh
        extends Mesh {
    // *************************************************************************
    // constants

    /**
     * maximum value for texture coordinates which do not wrap
     */
    final public static float uvMax = 1f;
    /**
     * minimum value for texture coordinates which do not wrap
     */
    final public static float uvMin = 0f;
    /**
     * number of vertices per triangle
     */
    final private static int vpt = 3;
    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(DomeMesh.class.getName());
    // *************************************************************************
    // fields
    /**
     * if true, vertex normals point inward; if false, they point outward
     */
    protected boolean inwardFacing;
    /**
     * U-coordinate of the top (<=1, >=0)
     */
    protected float topU;
    /**
     * V-coordinate of the top (<=1, >=0)
     */
    protected float topV;
    /**
     * UV distance from top to rim (<0.5, >0)
     */
    protected float uvScale;
    /**
     * number of samples in each longitudinal quadrant of the dome, including
     * both the top and the rim (>=2)
     */
    protected int quadrantSamples;
    /**
     * number of samples around the dome's rim (>=3)
     */
    protected int rimSamples;
    /**
     * number of triangles in the mesh
     */
    protected int triangleCount;
    /**
     * number of vertices in the mesh
     */
    protected int vertexCount;
    // *************************************************************************
    // constructors

    /**
     * Default constructor. Do not invoke!
     */
    public DomeMesh() {
        this(3, 2);
    }

    /**
     * Instantiate an inward-facing dome with a specific number of samples on
     * each axis. Use this constructor to create a dome for SkyMaterial.
     *
     * @param rimSamples number of samples around the rim (>=3)
     * @param quadrantSamples number of samples from top to rim, inclusive (>=2)
     */
    public DomeMesh(int rimSamples, int quadrantSamples) {
        this(rimSamples, quadrantSamples, SkyMaterial.topU, SkyMaterial.topV,
                SkyMaterial.uvScale, true);
    }

    /**
     * Instantiate a dome with a specific number of samples on each axis and a
     * specific texture coordinate system. This is the most general form of the
     * constructor.
     *
     * @param rimSamples number of samples around the rim (>=3)
     * @param quadrantSamples number of samples from top to rim, inclusive (>=2)
     * @param topU U-coordinate of the top (<=1, >=0)
     * @param topV V-coordinate of the top (<=1, >=0)
     * @param uvScale UV distance from top to rim (<0.5, >0)
     * @param inwardFacing if true, vertex normals point inward; if false, they
     * point outward
     */
    public DomeMesh(int rimSamples, int quadrantSamples, float topU, float topV,
            float uvScale, boolean inwardFacing) {
        if (rimSamples < 3) {
            throw new IllegalArgumentException(
                    "must have at least 3 samples on the rim");
        }
        this.rimSamples = rimSamples;

        if (rimSamples < 2) {
            throw new IllegalArgumentException(
                    "must have at least 2 samples per quadrant");
        }
        this.quadrantSamples = quadrantSamples;

        if (topU < uvMin || topU > uvMax) {
            throw new IllegalArgumentException(
                    "topU must be between 0 and 1, inclusive");
        }
        this.topU = topU;

        if (topV < uvMin || topV > uvMax) {
            throw new IllegalArgumentException(
                    "topV must be between 0 and 1, inclusive");
        }
        this.topV = topV;

        if (uvScale <= 0f || uvScale >= 0.5f) {
            throw new IllegalArgumentException(
                    "uvScale must be between 0 and 0.5");
        }
        this.uvScale = uvScale;
        this.inwardFacing = inwardFacing;
        updateAll();
        setStatic();
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Calculate the texture coordinate of a point on this mesh that's located
     * in a particular direction from its center.
     *
     * @param direction (unit vector, not altered)
     * @return a new vector, or null if direction lies too far below the rim
     */
    public Vector2f directionUV(Vector3f direction) {
        if (direction == null) {
            throw new NullPointerException("direction cannot be null");
        }
        if (!direction.isUnitVector()) {
            throw new IllegalArgumentException(
                    "direction must be a unit vector");
        }
        float angleFromTop = FastMath.acos(direction.y);
        float uvDistance = uvScale * angleFromTop / FastMath.HALF_PI;

        float x = direction.x;
        float z = direction.z;
        float xzDistance = MyMath.hypotenuse(x, z);
        if (xzDistance == 0f) {
            /*
             * Avoid division by zero at the top and base of the dome.
             */
            if (direction.y < 0f) { // base
                return null;
            } else { // top
                return new Vector2f(topU, topV);
            }
        }
        float cosLongitude = x / xzDistance;
        float sinLongitude = z / xzDistance;
        float u = topU + uvDistance * cosLongitude;
        float v = topV - uvDistance * sinLongitude;
        if (u < uvMin || u > uvMax || v < uvMin || v > uvMax) {
            return null;
        }
        return new Vector2f(u, v);
    }

    /**
     * Calculate the elevation angle of a point on this mesh, given it's texture
     * coordinates.
     *
     * @param u 1st texture coordinate (<=1, >=0)
     * @param v 2nd texture coordinate (<=1, >=0)
     * @return angle in radians (<=Pi/2)
     */
    public float elevationAngle(float u, float v) {
        if (u > uvMax || u < uvMin || v > uvMax || v < uvMin) {
            throw new IllegalArgumentException(
                    "texture coordinates must be between 0 and 1, inclusive");
        }

        float uvDistance = MyMath.hypotenuse(u - topU, v - topV);
        float angleFromTop = uvDistance / uvScale * FastMath.HALF_PI;
        float elevationAngle = FastMath.HALF_PI - angleFromTop;

        assert elevationAngle <= FastMath.HALF_PI : elevationAngle;
        return elevationAngle;
    }
    // *************************************************************************
    // private methods

    /**
     * Rebuild this dome after a parameter change.
     */
    private void updateAll() {
        if (rimSamples < 3) {
            throw new IllegalArgumentException(
                    "A dome must have at least 3 samples on its rim");
        }
        if (quadrantSamples < 2) {
            throw new IllegalArgumentException("A dome must have at least "
                    + "2 samples per longitudinal quadrant");
        }
        /*
         * Precompute some properties of the mesh.
         */
        int quadsPerGore = quadrantSamples - 2;
        int trianglesPerGore = 2 * quadsPerGore + 1;
        triangleCount = trianglesPerGore * rimSamples;
        logger.log(Level.INFO, "{0} triangles", triangleCount);
        vertexCount = (quadrantSamples - 1) * rimSamples + 1;
        logger.log(Level.INFO, "{0} vertices", vertexCount);
        assert vertexCount <= Short.MAX_VALUE : vertexCount;
        /*
         * Update each buffer.
         */
        updateCoordinates();
        updateIndices();
        updateNormals();
        /*
         * Update the bounds of the mesh.
         */
        updateBound();
    }

    /**
     * Update the buffered locations and texture coordinates of each vertex in
     * this dome.
     */
    private void updateCoordinates() {
        /*
         * Allocate an array to hold the local (XYZ) coordinates.
         */
        Vector3f[] locationArray = new Vector3f[vertexCount];
        /*
         * Allocate an array to hold the texture (U-V) coordinates.
         */
        Vector2f[] texCoordArray = new Vector2f[vertexCount];
        /*
         * Calculate the non-polar vertices first. Vertices are arranged first
         * by latitude (starting from the rim).
         */
        float quadHeight = FastMath.HALF_PI / (quadrantSamples - 1); // radians
        float quadWidth = FastMath.TWO_PI / rimSamples; // radians
        for (int parallel = 0; parallel < quadrantSamples - 1; parallel++) {
            float latitude = quadHeight * parallel;
            float y = FastMath.sin(latitude);
            float xzDistance = FastMath.cos(latitude);
            /*
             * Within each latitude, vertices are arranged by longitude
             * (starting from the +X meridian and proceeding counterclockwise
             * as seen from above).
             */
            for (int meridian = 0; meridian < rimSamples; meridian++) {
                float longitude = quadWidth * meridian;
                float sinLongitude = FastMath.sin(longitude);
                float cosLongitude = FastMath.cos(longitude);
                float x = xzDistance * cosLongitude;
                float z = xzDistance * sinLongitude;

                int vertexIndex = parallel * rimSamples + meridian;
                logger.log(Level.INFO, "coords {0}", vertexIndex);
                Vector3f location = new Vector3f(x, y, z);
                locationArray[vertexIndex] = location;

                Vector2f uv = directionUV(location);
                texCoordArray[vertexIndex] = uv;
            }
        }
        /*
         * The final vertex is at the top.
         */
        int topIndex = vertexCount - 1;
        logger.log(Level.INFO, "coords {0}", topIndex);
        locationArray[topIndex] = new Vector3f(0f, 1f, 0f);
        texCoordArray[topIndex] = new Vector2f(topU, topV);
        /*
         * Allocate and assign buffers for locations and texture coordinates.
         */
        FloatBuffer locBuffer = BufferUtils.createFloatBuffer(locationArray);
        setBuffer(VertexBuffer.Type.Position, 3, locBuffer);
        FloatBuffer tcBuffer = BufferUtils.createFloatBuffer(texCoordArray);
        setBuffer(VertexBuffer.Type.TexCoord, 2, tcBuffer);
    }

    /**
     * Update the buffered indices of each triangle in this dome.
     */
    private void updateIndices() {
        /*
         * Allocate an array to hold the three vertex indices of each triangle.
         */
        short[] indexArray = new short[vpt * triangleCount];
        /*
         * Calculate the quad triangles first. Quads are arranged first
         * by latitude (starting from the rim).
         */
        int quadsPerGore = quadrantSamples - 2;
        for (int parallel = 0; parallel < quadsPerGore; parallel++) {
            int nextParallel = parallel + 1;
            /*
             * Within each latitude, quads are arranged by longitude
             * (starting from the +X meridian and proceeding counterclockwise
             * as seen from above).
             */
            for (int meridian = 0; meridian < rimSamples; meridian++) {
                int nextMeridian = (meridian + 1) % rimSamples;
                int v0Index = parallel * rimSamples + meridian;
                int v1Index = parallel * rimSamples + nextMeridian;
                int v2Index = nextParallel * rimSamples + meridian;
                int v3Index = nextParallel * rimSamples + nextMeridian;

                int triIndex = 2 * v0Index;
                int baseIndex = vpt * triIndex;
                logger.log(Level.INFO, "index {0}", triIndex);
                indexArray[vpt * triIndex] = (short) v0Index;
                if (inwardFacing) {
                    indexArray[baseIndex + 1] = (short) v1Index;
                    indexArray[baseIndex + 2] = (short) v3Index;
                } else {
                    indexArray[baseIndex + 1] = (short) v3Index;
                    indexArray[baseIndex + 2] = (short) v1Index;
                }

                triIndex++;
                baseIndex = vpt * triIndex;
                logger.log(Level.INFO, "index {0}", triIndex);
                indexArray[baseIndex] = (short) v0Index;
                if (inwardFacing) {
                    indexArray[baseIndex + 1] = (short) v3Index;
                    indexArray[baseIndex + 2] = (short) v2Index;
                } else {
                    indexArray[baseIndex + 1] = (short) v2Index;
                    indexArray[baseIndex + 2] = (short) v3Index;
                }
            }
        }
        /*
         * The remaining (non-quad) triangles are arranged by longitude
         * (starting from the +X meridian and proceeding counterclockwise
         * as seen from above).
         */
        int parallel = quadsPerGore;
        int topIndex = vertexCount - 1;
        for (int meridian = 0; meridian < rimSamples; meridian++) {
            int nextMeridian = (meridian + 1) % rimSamples;
            int v0Index = parallel * rimSamples + meridian;
            int v1Index = parallel * rimSamples + nextMeridian;

            int triIndex = 2 * quadsPerGore * rimSamples + meridian;
            int baseIndex = vpt * triIndex;
            logger.log(Level.INFO, "index {0}", triIndex);
            indexArray[baseIndex] = (short) v0Index;
            if (inwardFacing) {
                indexArray[baseIndex + 1] = (short) v1Index;
                indexArray[baseIndex + 2] = (short) topIndex;
            } else {
                indexArray[baseIndex + 1] = (short) topIndex;
                indexArray[baseIndex + 2] = (short) v1Index;
            }
        }
        /*
         * Allocate and assign a buffer for indices.
         */
        ShortBuffer indexBuffer = BufferUtils.createShortBuffer(indexArray);
        setBuffer(VertexBuffer.Type.Index, vpt, indexBuffer);
    }

    /**
     * Update the buffered vertex normals of each vertex in this dome.
     *
     * Assumes that the coordinates have already been updated.
     */
    private void updateNormals() {
        /*
         * Allocate an array to hold the normal of each vertex.
         */
        Vector3f[] normalArray = new Vector3f[vertexCount];
        /*
         * Compute the normal at each vertex, which is simply its local
         * coordinates (for an outward facing dome) or the negative of its local
         * coordinates (for an inward facing dome).
         */
        FloatBuffer locationBuffer = getFloatBuffer(VertexBuffer.Type.Position);
        locationBuffer.rewind();
        float[] vertex = new float[3];
        for (int vertexIndex = 0; vertexIndex < vertexCount; vertexIndex++) {
            locationBuffer.get(vertex, 0, 3);
            Vector3f normal = new Vector3f(vertex[0], vertex[1], vertex[2]);
            assert normal.isUnitVector() : normal;
            if (inwardFacing) {
                normal.negateLocal();
            }

            logger.log(Level.INFO, "normal {0}", vertexIndex);
            normalArray[vertexIndex] = normal;
        }
        /*
         * Allocate and assign a buffer for normals.
         */
        FloatBuffer nomalBuffer = BufferUtils.createFloatBuffer(normalArray);
        setBuffer(VertexBuffer.Type.Normal, 3, nomalBuffer);
    }
}