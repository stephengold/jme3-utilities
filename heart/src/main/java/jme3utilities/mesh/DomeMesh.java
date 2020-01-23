/*
 Copyright (c) 2013-2019, Stephen Gold
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
package jme3utilities.mesh;

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
import jme3utilities.Validate;
import jme3utilities.math.MyMath;

/**
 * A 3-D, static, Triangles-mode mesh for a dome (or a pie-cut segment thereof)
 * with radius=1, centered at the origin, with its top at (0,1,0) and its
 * equator in the X-Z plane.
 * <p>
 * The key differences between this class and com.jme3.scene.shape.Dome are:<ol>
 * <li> the radius and center ARE NOT configurable,
 * <li> the texture coordinates, segment angle, and vertical angle ARE
 * configurable, and
 * <li> the normal vectors have the correct sign (JME issue #615).</ol>
 * <p>
 * The projection to texture space is an "azimuthal equidistant projection". The
 * dome's equator maps to a circle of radius uvScale centered at (topU,topV).
 * The +X direction maps to +U, and the +Z direction maps to -V.
 * <p>
 * TODO override JmeCloneable methods and test load/save/clone
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class DomeMesh extends Mesh {
    // *************************************************************************
    // constants and loggers

    /**
     * default for the first (U) texture coordinate at the top of the dome
     */
    final public static float defaultTopU = 0.5f;
    /**
     * default for the 2nd (V) texture coordinate at the top of the dome
     */
    final public static float defaultTopV = 0.5f;
    /**
     * default UV distance from top to rim
     */
    final public static float defaultUvScale = 0.44f;
    /**
     * number of axes in a vector
     */
    final private static int numAxes = 3;
    /**
     * number of vertices per triangle
     */
    final private static int vpt = 3;
    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(DomeMesh.class.getName());
    // *************************************************************************
    // fields

    /**
     * if true, generate a complete dome; if false, generate a pie-cut segment
     * of a dome
     */
    private boolean complete;
    /**
     * if true, vertex normals point inward; if false, they point outward
     */
    private boolean inwardFacing;
    /**
     * how much of the rim to generate (in radians, &le;2*Pi, &gt;0)
     */
    private float segmentAngle;
    /**
     * U-coordinate of the top (&le;1, &ge;0)
     */
    private float topU;
    /**
     * V-coordinate of the top (&le;1, &ge;0)
     */
    private float topV;
    /**
     * UV distance from top to equator (&lt;0.5, &gt;0)
     */
    private float uvScale;
    /**
     * angle from top to rim (in radians, &lt;Pi, &gt;0, Pi/2 &rarr; hemisphere)
     */
    private float verticalAngle;
    /**
     * number of samples in each longitudinal quadrant of the dome, including
     * both the top and the rim (&ge;2)
     */
    private int quadrantSamples;
    /**
     * number of samples around the dome's rim (&ge;3)
     */
    private int rimSamples;
    /**
     * number of triangles in the mesh
     */
    private int triangleCount;
    /**
     * number of vertices in the mesh
     */
    private int vertexCount;
    // *************************************************************************
    // constructors

    /**
     * No-argument constructor needed by SavableClassUtil.
     */
    protected DomeMesh() {
    }

    /**
     * Instantiate an inward-facing hemispherical dome with a specified number
     * of samples on each axis. Use this constructor to generate domes for
     * SkyMaterial.
     *
     * @param rimSamples number of samples around the rim (&ge;3)
     * @param quadrantSamples number of samples from top to rim, inclusive
     * (&ge;2)
     */
    public DomeMesh(int rimSamples, int quadrantSamples) {
        this(rimSamples, quadrantSamples, defaultTopU, defaultTopV,
                defaultUvScale, true);
    }

    /**
     * Instantiate a hemispherical dome with a specified number of samples on
     * each axis and a specified texture coordinate-system. This is the most
     * general form of the constructor.
     *
     * @param rimSamples number of samples around the rim (&ge;3)
     * @param quadrantSamples number of samples from top to rim, inclusive
     * (&ge;2)
     * @param topU U-coordinate of the top (&le;1, &ge;0)
     * @param topV V-coordinate of the top (&le;1, &ge;0)
     * @param uvScale UV distance from top to equator (&lt;0.5, &gt;0)
     * @param inwardFacing if true, vertex normals point inward; if false, they
     * point outward
     */
    public DomeMesh(int rimSamples, int quadrantSamples, float topU, float topV,
            float uvScale, boolean inwardFacing) {
        Validate.inRange(rimSamples, "rim samples", 3, Integer.MAX_VALUE);
        this.rimSamples = rimSamples;

        Validate.inRange(quadrantSamples, "quadrant samples",
                2, Integer.MAX_VALUE);
        this.quadrantSamples = quadrantSamples;

        Validate.fraction(topU, "topU");
        this.topU = topU;

        Validate.fraction(topV, "topV");
        this.topV = topV;

        if (!(uvScale > 0f && uvScale < 0.5f)) {
            logger.log(Level.SEVERE, "uvScale={0}", uvScale);
            throw new IllegalArgumentException(
                    "uvScale should be between 0 and 0.5");
        }
        this.uvScale = uvScale;

        this.inwardFacing = inwardFacing;

        segmentAngle = FastMath.TWO_PI;
        verticalAngle = FastMath.HALF_PI;

        updateAll();
        setStatic();
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Calculate the texture coordinate of a point on this mesh that's in the
     * specified direction from the center of the mesh.
     *
     * @param direction (length&gt;0, unaffected)
     * @return a new vector, or null if direction is too far below the equator
     */
    public Vector2f directionUV(Vector3f direction) {
        Validate.nonZero(direction, "direction");

        Vector3f norm = direction.normalize();
        float angleFromTop = FastMath.acos(norm.y);
        float uvDistance = uvScale * angleFromTop / FastMath.HALF_PI;

        float x = norm.x;
        float z = norm.z;
        float xzDistance = MyMath.hypotenuse(x, z);
        if (xzDistance == 0f) {
            /*
             * Avoid division by zero at the Y-axis.
             */
            if (norm.y < 0f) {
                return null;
            } else { // top
                return new Vector2f(topU, topV);
            }
        }
        float cosLongitude = x / xzDistance;
        float sinLongitude = z / xzDistance;
        float u = topU + uvDistance * cosLongitude;
        float v = topV - uvDistance * sinLongitude;
        if (u < 0f || u > 1f || v < 0f || v > 1f) {
            return null;
        }
        return new Vector2f(u, v);
    }

    /**
     * Compute the elevation angle of a point on this mesh, given its texture
     * coordinates.
     *
     * @param u the first texture coordinate (&le;1, &ge;0)
     * @param v the 2nd texture coordinate (&le;1, &ge;0)
     * @return angle in radians (&le;Pi/2)
     */
    public float elevationAngle(float u, float v) {
        Validate.fraction(u, "u");
        Validate.fraction(v, "v");

        float uvDistance = MyMath.hypotenuse(u - topU, v - topV);
        float angleFromTop = uvDistance / uvScale * FastMath.HALF_PI;
        assert angleFromTop < FastMath.PI : angleFromTop;
        float elevationAngle = FastMath.HALF_PI - angleFromTop;

        assert elevationAngle <= FastMath.HALF_PI : elevationAngle;
        return elevationAngle;
    }

    /**
     * Read the U-V scale of this dome.
     *
     * @return UV distance from top to equator (&lt;0.5, &gt;0)
     */
    public float getUVScale() {
        assert uvScale > 0f : uvScale;
        assert uvScale < 0.5f : uvScale;
        return uvScale;
    }

    /**
     * Read the vertical angle of this dome.
     *
     * @return angle (in radians, &lt;Pi, &gt;0)
     * @see #setVerticalAngle(float)
     */
    public float getVerticalAngle() {
        assert verticalAngle > 0f : verticalAngle;
        assert verticalAngle < FastMath.PI : verticalAngle;
        return verticalAngle;
    }

    /**
     * Regenerate the mesh for a new segment angle: 2*Pi produces a complete
     * dome, Pi results in a half dome, and so on.
     *
     * @param newAngle (in radians, &le;2*Pi, &gt;0)
     */
    public void setSegmentAngle(float newAngle) {
        if (!(newAngle > 0f && newAngle <= FastMath.TWO_PI)) {
            logger.log(Level.SEVERE, "angle={0}", newAngle);
            throw new IllegalArgumentException(
                    "angle should be between 0 and 2*Pi");
        }
        segmentAngle = newAngle;

        updateAll();
    }

    /**
     * Regenerate the mesh for a new vertical angle: Pi/2 produces a hemisphere
     * and so on.
     *
     * @param newAngle (in radians, &lt;Pi, &gt;0)
     */
    public void setVerticalAngle(float newAngle) {
        if (!(newAngle > 0f && newAngle < FastMath.PI)) {
            logger.log(Level.SEVERE, "angle={0}", newAngle);
            throw new IllegalArgumentException(
                    "angle should be between 0 and Pi");
        }
        verticalAngle = newAngle;

        updateAll();
    }
    // *************************************************************************
    // Savable methods

    /**
     * De-serialize this mesh, for example when loading from a J3O file.
     *
     * @param importer (not null)
     * @throws IOException from importer
     */
    @Override
    public void read(JmeImporter importer) throws IOException {
        super.read(importer);

        InputCapsule capsule = importer.getCapsule(this);

        inwardFacing = capsule.readBoolean("inwardFacing", true);
        quadrantSamples = capsule.readInt("quadrantSamples", 2);
        rimSamples = capsule.readInt("rimSamples", 3);
        segmentAngle = capsule.readFloat("segmentAngle", FastMath.TWO_PI);
        topU = capsule.readFloat("topU", defaultTopU);
        topV = capsule.readFloat("topV", defaultTopV);
        uvScale = capsule.readFloat("uvScale", defaultUvScale);
        verticalAngle = capsule.readFloat("verticalAngle", FastMath.HALF_PI);
        /*
         * Recompute the derived properties.
         */
        updateDerivedProperties();
    }

    /**
     * Serialize this mesh, for example when saving to a J3O file.
     *
     * @param exporter (not null)
     * @throws IOException from exporter
     */
    @Override
    public void write(JmeExporter exporter) throws IOException {
        super.write(exporter);

        OutputCapsule capsule = exporter.getCapsule(this);

        capsule.write(inwardFacing, "inwardFacing", true);
        capsule.write(quadrantSamples, "quadrantSamples", 2);
        capsule.write(rimSamples, "rimSamples", 3);
        capsule.write(segmentAngle, "segmentAngle", FastMath.TWO_PI);
        capsule.write(topU, "topU", defaultTopU);
        capsule.write(topV, "topV", defaultTopV);
        capsule.write(uvScale, "uvScale", defaultUvScale);
        capsule.write(verticalAngle, "verticalAngle", FastMath.HALF_PI);
    }
    // *************************************************************************
    // private methods

    /**
     * Rebuild this dome after a parameter change.
     */
    private void updateAll() {
        /*
         * Recompute the derived properties.
         */
        updateDerivedProperties();
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
         * Allocate an array to hold the mesh (XYZ) coordinates.
         */
        Vector3f[] locationArray = new Vector3f[vertexCount];
        /*
         * Allocate an array to hold the texture (UV) coordinates.
         */
        Vector2f[] texCoordArray = new Vector2f[vertexCount];
        /*
         * Compute the non-polar vertices first. Vertices are arranged first
         * by latitude (starting from the rim).
         */
        float quadHeight = verticalAngle / (quadrantSamples - 1); // radians
        float quadWidth;  // radians
        if (complete) {
            quadWidth = FastMath.TWO_PI / rimSamples;
        } else {
            quadWidth = segmentAngle / (rimSamples - 1);
        }
        for (int parallel = 0; parallel < quadrantSamples - 1; ++parallel) {
            float latitude = FastMath.HALF_PI - verticalAngle
                    + quadHeight * parallel;
            float y = FastMath.sin(latitude);
            float xzDistance = FastMath.cos(latitude);
            /*
             * Within each latitude, vertices are arranged by longitude
             * (starting from the +X meridian and proceeding counterclockwise
             * as seen from +Y).
             */
            for (int meridian = 0; meridian < rimSamples; ++meridian) {
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
        setBuffer(VertexBuffer.Type.Position, numAxes, locBuffer);
        FloatBuffer tcBuffer = BufferUtils.createFloatBuffer(texCoordArray);
        setBuffer(VertexBuffer.Type.TexCoord, 2, tcBuffer);
    }

    /**
     * Update some basic properties of the mesh: complete, triangleCount, and
     * vertexCount.
     */
    private void updateDerivedProperties() {
        if (rimSamples < 3) {
            logger.log(Level.SEVERE, "rimSamples={0}", rimSamples);
            throw new IllegalStateException(
                    "need at least 3 samples on the rim");
        }
        if (quadrantSamples < 2) {
            logger.log(Level.SEVERE, "quadrantSamples={0}", quadrantSamples);
            throw new IllegalStateException("need at least "
                    + "2 samples per longitudinal quadrant");
        }

        complete = (segmentAngle > 1.999f * FastMath.PI);

        int quadsPerGore = quadrantSamples - 2;
        int trianglesPerGore = 2 * quadsPerGore + 1;
        triangleCount = trianglesPerGore * rimSamples;
        logger.log(Level.INFO, "{0} triangles", triangleCount);

        vertexCount = (quadrantSamples - 1) * rimSamples + 1;
        logger.log(Level.INFO, "{0} vertices", vertexCount);
        assert vertexCount <= Short.MAX_VALUE : vertexCount;
    }

    /**
     * Update the buffered indices of each triangle in this dome.
     */
    private void updateIndices() {
        /*
         * Allocate an array to hold the 3 vertex indices of each triangle.
         */
        short[] indexArray = new short[vpt * triangleCount];
        /*
         * If the dome is incomplete, leave a gap between the last rim sample
         * and the first.
         */
        int numGores;
        if (complete) {
            numGores = rimSamples;
        } else {
            numGores = rimSamples - 1;
        }
        /*
         * Compute the quad triangles first. Quads are arranged first
         * and foremost by latitude, starting at the rim.
         */
        int quadsPerGore = quadrantSamples - 2;
        for (int parallel = 0; parallel < quadsPerGore; ++parallel) {
            int nextParallel = parallel + 1;
            /*
             * Within each latitude band, quads are arranged by longitude,
             * starting from the +X meridian and proceeding counterclockwise
             * as seen from +Y.
             */
            for (int meridian = 0; meridian < numGores; ++meridian) {
                int nextMeridian = (meridian + 1) % rimSamples;
                int v0Index = parallel * rimSamples + meridian;
                int v1Index = parallel * rimSamples + nextMeridian;
                int v2Index = nextParallel * rimSamples + meridian;
                int v3Index = nextParallel * rimSamples + nextMeridian;
                /*
                 * Each quad consists of 2 triangles.
                 */
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

                ++triIndex;
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
         * The remaining (non-quad) triangles near the top of the dome
         * are arranged by longitude, starting from the +X meridian and
         * proceeding counterclockwise as seen from +Y.
         */
        int parallel = quadsPerGore;
        int topIndex = vertexCount - 1;
        for (int meridian = 0; meridian < numGores; ++meridian) {
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
     * <p>
     * Assumes that the coordinates have already been updated.
     */
    private void updateNormals() {
        /*
         * Allocate an array to hold the normal of each vertex.
         */
        Vector3f[] normalArray = new Vector3f[vertexCount];
        /*
         * Compute the normal at each vertex, which is simply its local
         * coordinates (for an outward-facing dome) or the negative of its local
         * coordinates (for an inward-facing dome).
         */
        FloatBuffer locationBuffer = getFloatBuffer(VertexBuffer.Type.Position);
        locationBuffer.rewind();
        float[] vertex = new float[numAxes];
        for (int vertexIndex = 0; vertexIndex < vertexCount; ++vertexIndex) {
            locationBuffer.get(vertex, 0, numAxes);
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
        FloatBuffer normalBuffer = BufferUtils.createFloatBuffer(normalArray);
        setBuffer(VertexBuffer.Type.Normal, numAxes, normalBuffer);
    }
}
