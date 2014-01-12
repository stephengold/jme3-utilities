/*
 Copyright (c) 2013-2014, Stephen Gold
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright
 notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 notice, this list of conditions and the following disclaimer in the
 documentation and/or other materials provided with the distribution.
 * Stephen Gold's name may not be used to endorse or promote products
 derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL STEPHEN GOLD BE LIABLE FOR ANY
 DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package jme3utilities.sky;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyMath;

/**
 * A custom mesh which generates a hemispherical dome of radius=1, centered at
 * the origin, with its top at y=1 and its rim in the XZ plane.
 * <p>
 * The main differences between this class and com.jme3.scene.shape.Dome are:
 * (1) the radius and center are not configurable, (2) the texture coordinates
 * are configurable, and (3) the normal vectors have the correct sign (issue
 * #615).
 * <p>
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
     * No-argument constructor for serialization purposes only. Do not use!
     */
    public DomeMesh() {
    }

    /**
     * Instantiate an inward-facing dome with a specific number of samples on
     * each axis. Use this constructor to create a dome for SkyMaterial.
     *
     * @param rimSamples number of samples around the rim (>=3)
     * @param quadrantSamples number of samples from top to rim, inclusive (>=2)
     */
    public DomeMesh(int rimSamples, int quadrantSamples) {
        this(rimSamples, quadrantSamples, Constants.topU, Constants.topV,
                Constants.uvScale, true);
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
            logger.log(Level.SEVERE, "rimSamples={0}", rimSamples);
            throw new IllegalArgumentException(
                    "need at least 3 samples on the rim");
        }
        this.rimSamples = rimSamples;

        if (quadrantSamples < 2) {
            logger.log(Level.SEVERE, "quadrantSamples={0}", quadrantSamples);
            throw new IllegalArgumentException(
                    "need at least 2 samples per quadrant");
        }
        this.quadrantSamples = quadrantSamples;

        if (topU < Constants.uvMin || topU > Constants.uvMax) {
            logger.log(Level.SEVERE, "topU={0}", topU);
            throw new IllegalArgumentException(
                    "topU should be between 0 and 1, inclusive");
        }
        this.topU = topU;

        if (topV < Constants.uvMin || topV > Constants.uvMax) {
            logger.log(Level.SEVERE, "topV={0}", topV);
            throw new IllegalArgumentException(
                    "topV should be between 0 and 1, inclusive");
        }
        this.topV = topV;

        if (uvScale <= 0f || uvScale >= 0.5f) {
            logger.log(Level.SEVERE, "uvScale={0}", uvScale);
            throw new IllegalArgumentException(
                    "uvScale should be between 0 and 0.5");
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
     * @return a new vector, or null if direction is too far below the rim
     */
    public Vector2f directionUV(Vector3f direction) {
        if (direction == null) {
            throw new NullPointerException("direction should not be null");
        }
        if (!direction.isUnitVector()) {
            logger.log(Level.SEVERE, "direction={0}", direction);
            throw new IllegalArgumentException(
                    "direction should be a unit vector");
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
        if (u < Constants.uvMin || u > Constants.uvMax || v < Constants.uvMin
                || v > Constants.uvMax) {
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
        if (u > Constants.uvMax || u < Constants.uvMin || v > Constants.uvMax
                || v < Constants.uvMin) {
            logger.log(Level.SEVERE, "u={0}, v={1}", new Object[]{u, v});
            throw new IllegalArgumentException(
                    "texture coordinates should be between 0 and 1, inclusive");
        }

        float uvDistance = MyMath.hypotenuse(u - topU, v - topV);
        float angleFromTop = uvDistance / uvScale * FastMath.HALF_PI;
        float elevationAngle = FastMath.HALF_PI - angleFromTop;

        assert elevationAngle <= FastMath.HALF_PI : elevationAngle;
        return elevationAngle;
    }
    // *************************************************************************
    // Savable methods

    /**
     * De-serialize this instance when loading.
     *
     * @param importer (not null)
     */
    @Override
    public void read(JmeImporter importer)
            throws IOException {
        super.read(importer);

        InputCapsule capsule = importer.getCapsule(this);

        inwardFacing = capsule.readBoolean("inwardFacing", true);
        quadrantSamples = capsule.readInt("quadrantSamples", 2);
        rimSamples = capsule.readInt("rimSamples", 3);
        topU = capsule.readFloat("topU", Constants.topU);
        topV = capsule.readFloat("topV", Constants.topV);
        uvScale = capsule.readFloat("uvScale", Constants.uvScale);
        /*
         * cached values
         */
        int quadsPerGore = quadrantSamples - 2;
        int trianglesPerGore = 2 * quadsPerGore + 1;
        triangleCount = trianglesPerGore * rimSamples;
        vertexCount = (quadrantSamples - 1) * rimSamples + 1;
    }

    /**
     * Serialize this instance when saving.
     *
     * @param exporter (not null)
     */
    @Override
    public void write(JmeExporter exporter)
            throws IOException {
        super.write(exporter);

        OutputCapsule capsule = exporter.getCapsule(this);

        capsule.write(inwardFacing, "inwardFacing", true);
        capsule.write(quadrantSamples, "quadrantSamples", 2);
        capsule.write(rimSamples, "rimSamples", 3);
        capsule.write(topU, "topU", Constants.topU);
        capsule.write(topV, "topV", Constants.topV);
        capsule.write(uvScale, "uvScale", Constants.uvScale);
    }
    // *************************************************************************
    // private methods

    /**
     * Rebuild this dome after a parameter change.
     */
    private void updateAll() {
        if (rimSamples < 3) {
            logger.log(Level.SEVERE, "rimSamples={0}", rimSamples);
            throw new IllegalArgumentException(
                    "need at least 3 samples on the rim");
        }
        if (quadrantSamples < 2) {
            logger.log(Level.SEVERE, "rimSamples={0}", quadrantSamples);
            throw new IllegalArgumentException("need at least "
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
                logger.log(Level.FINE, "coords {0}", vertexIndex);
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
                logger.log(Level.FINE, "index {0}", triIndex);
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
                logger.log(Level.FINE, "index {0}", triIndex);
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
            logger.log(Level.FINE, "index {0}", triIndex);
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

            logger.log(Level.FINE, "normal {0}", vertexIndex);
            normalArray[vertexIndex] = normal;
        }
        /*
         * Allocate and assign a buffer for normals.
         */
        FloatBuffer nomalBuffer = BufferUtils.createFloatBuffer(normalArray);
        setBuffer(VertexBuffer.Type.Normal, 3, nomalBuffer);
    }
}