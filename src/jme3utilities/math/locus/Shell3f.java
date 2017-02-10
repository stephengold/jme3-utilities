/*
 Copyright (c) 2017, Stephen Gold
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
package jme3utilities.math.locus;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Validate;
import jme3utilities.math.MyMath;
import jme3utilities.math.noise.Noise;
import jme3utilities.math.polygon.SimplePolygon3f;
import jme3utilities.math.spline.LinearSpline3f;
import jme3utilities.math.spline.Spline3f;

/**
 * Represents a metric-based shell in 3-D space, which includes all points whose
 * metric (relative to the center) lie in a certain range. If the maximum metric
 * is zero, a single point results. If the minimum metric is zero, a solid shape
 * results. If the maximum metric is positive, a hole (negative shape) results.
 * <p>
 * The choice of metric determines the general shape of the region. An
 * unweighted 3-D Euclidian metric produces a spherical shape. An unweighted
 * Chebyshev metric produces a cubic shape. An unweighted Manhattan metric
 * produces an octahedral shape.
 * <p>
 * Nonuniform axis weights can produce an ellipsoid or a rectangular solid.
 * Applying zero weight to one or two axes can produce an infinite slab or
 * infinite cylinder.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class Shell3f implements Locus3f {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            SimplePolygon3f.class.getName());
    /**
     * pseudo-random number generator for this class
     */
    final private static Random generator = new Random();
    /**
     * weights for an infinite cylinder
     */
    final private static Vector3f cylinderWeights = new Vector3f(0f, 1f, 1f);
    /**
     * weights for an infinite slab
     */
    final private static Vector3f slabWeights = new Vector3f(1f, 0f, 0f);
    // *************************************************************************
    // fields
    
    /**
     * metric employed by this shell (not null)
     */
    private Metric metric;
    /**
     * minimum metric value for points in the shell (&ge;0, &le;outerRadius, may
     * be {@link Float#POSITIVE_INFINITY})
     */
    private float innerRadius;
    /**
     * the square of innerRadius (&ge;0, &le;outerRSquared, may be
     * {@link Double#POSITIVE_INFINITY})
     */
    private double innerRSquared;
    /**
     * optimal squared metric value for points in the shell (&ge;0, may be
     * {@link Double#POSITIVE_INFINITY})
     */
    private double optimalRSquared;
    /**
     * maximum metric value for points in the shell (&ge;innerRadius, may be
     * {@link Float#POSITIVE_INFINITY})
     */
    private float outerRadius;
    /**
     * the square of outerRadius (&ge;innerRSquared, may be
     * {@link Double#POSITIVE_INFINITY})
     */
    private double outerRSquared;
    /**
     * left-multiply to convert XYZ (world) coordinates to UVW (local)
     * coordinates or null to skip rotation
     */
    private Quaternion inverseRotation;
    /**
     * left-multiply to convert UVW (local) coordinates to XYZ (world)
     * coordinates or null to skip rotation
     */
    private Quaternion orientation;
    /**
     * coordinates of the shell's center (not null)
     */
    private Vector3f center;
    /**
     * axis weights (scale factor) applied to coordinates (all components &ge;0)
     * or null to skip weighting
     */
    private Vector3f weights;
    // *************************************************************************
    // constructors    

    /**
     * Instantiate a sphere.
     *
     * @param center coordinates of the shell's center (not null, unaffected)
     * @param radius (in world units, &gt;0, finite)
     */
    public Shell3f(Vector3f center, float radius) {
        this(Metric.EUCLID, center, radius);
    }

    /**
     * Instantiate an axis-aligned cube or octahedron.
     *
     * @param metric {@link Metric#CHEBYSHEV} &rarr; cube,
     * {@link Metric#MANHATTAN} &rarr; octahedron (not null)
     * @param center coordinates of the shell's center (not null, unaffected)
     * @param radius (in world units, &gt;0, finite)
     */
    public Shell3f(Metric metric, Vector3f center, float radius) {
        this(metric, center, radius, radius, radius);
    }

    /**
     * Instantiate an axis-aligned ellipsoid or rectangular solid.
     *
     * @param metric {@link Metric#EUCLID} &rarr; ellipsoid,
     * {@link Metric#CHEBYSHEV} &rarr; rectangular solid (not null)
     * @param center coordinates of the shell's center (not null, unaffected)
     * @param xRadius (in world units, &gt;0, finite)
     * @param yRadius (in world units, &gt;0, finite)
     * @param zRadius (in world units, &gt;0, finite)
     */
    public Shell3f(Metric metric, Vector3f center, float xRadius,
            float yRadius, float zRadius) {
        this(metric, center, null, xRadius, yRadius, zRadius);
    }

    /**
     * Instantiate a finite solid shape.
     *
     * @param metric metric employed by this shell (not null)
     * @param center coordinates of the shell's center (not null, unaffected)
     * @param orient left-multiply to convert UVW (local) coordinates to XYZ
     * (world) coordinates (unaffected) or null to skip rotation
     * @param uRadius (in world units, &gt;0, finite)
     * @param vRadius (in world units, &gt;0, finite)
     * @param wRadius (in world units, &gt;0, finite)
     */
    public Shell3f(Metric metric, Vector3f center, Quaternion orient,
            float uRadius, float vRadius, float wRadius) {
        Validate.nonNull(metric, "metric");
        Validate.nonNull(center, "center");
        Validate.finite(uRadius, "U radius");
        Validate.positive(uRadius, "U radius");
        Validate.finite(vRadius, "V radius");
        Validate.positive(vRadius, "V radius");
        Validate.finite(wRadius, "W radius");
        Validate.positive(wRadius, "W radius");

        this.metric = metric;
        this.center = center.clone();
        setOrientation(orient);
        if (uRadius == vRadius && vRadius == wRadius) {
            weights = null;
            outerRadius = uRadius;
        } else {
            float maxRadius = MyMath.max(uRadius, vRadius, wRadius);
            assert maxRadius > 0f : maxRadius;
            float uWeight = maxRadius / uRadius;
            float vWeight = maxRadius / vRadius;
            float wWeight = maxRadius / wRadius;
            weights = new Vector3f(uWeight, vWeight, wWeight);
            outerRadius = maxRadius;
        }
        innerRadius = 0f;
        innerRSquared = 0.0;
        optimalRSquared = 0.0;
        outerRSquared = outerRadius * outerRadius;
    }

    /**
     * Instantiate an infinite cylinder or infinite slab.
     *
     * @param center coordinates of the shell's center (not null, unaffected)
     * @param axis (not null, in world coordinates, not zero, unaffected)
     * @param radius (in world units, &gt;0, finite)
     * @param slabFlag (true for a slab, false for a cylinder)
     */
    public Shell3f(Vector3f center, Vector3f axis, float radius,
            boolean slabFlag) {
        Validate.nonNull(center, "center");
        Validate.nonZero(axis, "axis");
        Validate.positive(radius, "radius");
        Validate.finite(radius, "radius");

        metric = Metric.EUCLID;
        this.center = center.clone();
        Vector3f uAxis = axis.normalize();
        Vector3f vAxis = Noise.ortho(uAxis, generator);
        Vector3f wAxis = uAxis.cross(vAxis);
        orientation = new Quaternion();
        orientation.fromAxes(uAxis, vAxis, wAxis);
        inverseRotation = orientation.inverse();
        if (slabFlag) {
            weights = slabWeights;
        } else {
            weights = cylinderWeights;
        }
        innerRadius = 0f;
        innerRSquared = 0.0;
        optimalRSquared = 0.0;
        outerRadius = radius;
        outerRSquared = radius * radius;
    }

    /**
     * Instantiate a spherical shell or hole.
     *
     * @param center coordinates of the shell's center (not null, unaffected)
     * @param innerRadius radius of the negative space (in world units, &ge;0,
     * finite, &le;outerRadius, may be {@link Float#POSITIVE_INFINITY}))
     * @param outerRadius radius of the positive space (in world units,
     * &ge;innerRadius, may be {@link Float#POSITIVE_INFINITY})
     */
    public Shell3f(Vector3f center, float innerRadius, float outerRadius) {
        this(Metric.EUCLID, center, null, null, innerRadius, outerRadius);
    }

    /**
     * Instantiate a generic shell or hole.
     *
     * @param metric metric employed by this shell (not null)
     * @param center coordinates of the shell's center (not null, unaffected)
     * @param orient left-multiply to convert UVW (local) coordinates to XYZ
     * (world) coordinates (unaffected) or null to skip rotation
     * @param weights axis weights (all components &ge;0) or null to skip
     * weighting
     * @param innerRadius radius of the negative space (in world units, &ge;0,
     * &le;outerRadius, may be {@link Float#POSITIVE_INFINITY})
     * @param outerRadius radius of the positive space (in world units,
     * &ge;innerRadius, may be {@link Float#POSITIVE_INFINITY})
     */
    public Shell3f(Metric metric, Vector3f center, Quaternion orient,
            Vector3f weights, float innerRadius, float outerRadius) {
        Validate.nonNull(metric, "metric");
        Validate.nonNull(center, "center");
        Validate.nonNegative(innerRadius, "inner radius");
        if (outerRadius < innerRadius) {
            logger.log(Level.SEVERE, "inner radius={0}, outerRadius={1}",
                    new Object[]{innerRadius, outerRadius});
            throw new IllegalArgumentException(
                    "Inner radius must not exceed outer radius.");
        }

        this.metric = metric;
        this.center = center.clone();
        setOrientation(orient);

        if (weights == null) {
            this.weights = null;
        } else {
            Validate.positive(weights.x, "U-axis weight");
            Validate.positive(weights.y, "V-axis weight");
            Validate.positive(weights.z, "W-axis weight");
            this.weights = weights.clone();
        }
        this.innerRadius = innerRadius;
        innerRSquared = innerRadius * innerRadius;
        if (Float.isInfinite(outerRadius)) {
            optimalRSquared = Double.POSITIVE_INFINITY;
        } else {
            assert !Float.isInfinite(innerRadius);
            double optimalRadius = 0.5 * (double) (innerRadius + outerRadius);
            optimalRSquared = optimalRadius * optimalRadius;
        }
        this.outerRadius = outerRadius;
        outerRSquared = outerRadius * outerRadius;
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Test whether this shell is convex.
     *
     * @return true if convex, false if there is a hole
     */
    public boolean isConvex() {
        if (innerRSquared == 0.0) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Relocate this shell.
     *
     * @param newCenter new coordinates for center (not null, unaffected)
     */
    final public void setCenter(Vector3f newCenter) {
        Validate.nonNull(newCenter, "new center");

        center.set(newCenter);
    }

    /**
     * Reorient this shell.
     *
     * @param newOrientation left-multiply to convert UVW (local) coordinates to
     * XYZ (world) coordinates (unaffected) or null to skip rotation
     */
    final public void setOrientation(Quaternion newOrientation) {
        if (newOrientation == null) {
            orientation = null;
            inverseRotation = null;
        } else {
            orientation = newOrientation.clone();
            inverseRotation = newOrientation.inverse();
        }
    }
    // *************************************************************************
    // Locus3f methods    

    /**
     * Test whether this region can be merged with another.
     *
     * @param otherLocus (not null, unaffected)
     * @return true if can merge, otherwise false
     */
    @Override
    public boolean canMerge(Locus3f otherLocus) {
        return false;
    }

    /**
     * Calculate the centroid of this region. The centroid need not be contained
     * in the region, but it should be relatively near all locations that are.
     *
     * @return a new coordinate vector
     */
    @Override
    public Vector3f centroid() {
        return center.clone();
    }

    /**
     * Test whether this region contains a specified location.
     *
     * @param location coordinates of location to test (not null, unaffected)
     * @return true if location is in this region, false otherwise
     */
    @Override
    public boolean contains(Vector3f location) {
        Vector3f offset = location.subtract(center);
        if (inverseRotation != null) {
            offset = inverseRotation.mult(offset);
        }
        if (weights != null) {
            offset.multLocal(weights);
        }
        double squaredValue = metric.squaredValue(offset);
        if (squaredValue >= innerRSquared && squaredValue <= outerRSquared) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Find the location in this region nearest to a specified location.
     *
     * @param location coordinates of the input (not null, unaffected)
     * @return a new vector, or null it none found
     */
    @Override
    public Vector3f findLocation(Vector3f location) {
        Vector3f result = location.clone();

        Vector3f offset = location.subtract(center);
        if (inverseRotation != null) {
            offset = inverseRotation.mult(offset);
        }
        Vector3f unweighted = offset.clone();
        if (weights != null) {
            offset.multLocal(weights);
        }
        double squaredValue = metric.squaredValue(offset);

        if (squaredValue == 0.0 && innerRadius > 0.0) {
            /*
             * Location is the center.  Pick an offset on the inner surface.
             */
            if (weights == null) {
                unweighted.set(innerRadius, 0f, 0f);
                offset.set(unweighted);
            } else {
                float max = MyMath.max(weights.x, weights.y, weights.z);
                assert max > 0f : weights;
                if (weights.x == max) {
                    unweighted.set(innerRadius / weights.x, 0f, 0f);
                } else if (weights.y == max) {
                    unweighted.set(0f, innerRadius / weights.y, 0f);
                } else {
                    assert weights.z == max : weights;
                    unweighted.set(0f, 0f, innerRadius / weights.z);
                }
                offset.set(unweighted).multLocal(weights);
            }
            squaredValue = innerRSquared;
            assert squaredValue == metric.squaredValue(offset) : squaredValue;
        }

        double scaleFactor;
        if (squaredValue < innerRSquared) {
            assert squaredValue > 0.0 : squaredValue;
            scaleFactor = Math.sqrt(innerRSquared / squaredValue);
        } else if (squaredValue > outerRSquared) {
            assert squaredValue > 0.0 : squaredValue;
            scaleFactor = Math.sqrt(outerRSquared / squaredValue);
        } else {
            /*
             * The shell contains the original location.
             */
            return result;
        }
        assert scaleFactor > 0.0 : scaleFactor;
        /*
         * Scale the offset to the surface of the shell.  For non-spherical 
         * shells, this won't usually produce the nearest point, but it 
         * provides a starting point for optimization.
         */
        if (weights != null) {
            /*
             * Undo axis weighting.
             */
            if (weights.x != 0f) {
                offset.x /= weights.x;
            } else {
                offset.x = unweighted.x;
            }
            if (weights.y != 0f) {
                offset.y /= weights.y;
            } else {
                offset.y = unweighted.y;
            }
            if (weights.z != 0f) {
                offset.z /= weights.z;
            } else {
                offset.z = unweighted.z;
            }
        }

        if (orientation == null) {
            result = offset;
        } else {
            /*
             * Undo rotation.
             */
            result = orientation.mult(offset);
        }
        result.addLocal(center);
        /*
         * TODO if shell is non-spherical, optimize the result 
         */
        assert contains(result) : result;
        return result;
    }

    /**
     * Merge this region with another.
     *
     * @param otherLocus (not null, unaffected)
     * @return a new region representing the union of the two regions
     */
    @Override
    public Locus3f merge(Locus3f otherLocus) {
        throw new IllegalArgumentException("unable to merge");
    }
    
    /**
     * Calculate a representative location (or rep) for this region. The rep
     * must be contained in the region.
     *
     * @return a new coordinate vector, or null if none found
     */
    @Override
    public Vector3f rep() {
        Vector3f result = new Vector3f();

        if (isConvex()) {
            result.set(center);
            assert contains(result) : result;
            return result;
        }
        /*
         * Pick an offset on the inner surface.
         */
        if (weights == null) {
            if (metric == Metric.MANHATTAN) {
                float coord = innerRadius / 3f;
                result.set(coord, coord, coord);
            } else {
                result.set(innerRadius, 0f, 0f);
            }

        } else if (metric == Metric.MANHATTAN) {
                float sum = weights.x + weights.y + weights.z;
                result.set(weights.x, weights.y, weights.z);
                result.multLocal(innerRadius / sum);
            } else {
                float max = MyMath.max(weights.x, weights.y, weights.z);
                assert max > 0f : weights;
                if (weights.x == max) {
                    result.set(innerRadius, 0f, 0f);
                } else if (weights.y == max) {
                    result.set(0f, innerRadius, 0f);
                } else {
                    assert weights.z == max : weights;
                    result.set(0f, 0f, innerRadius);
            }
        }
        if (orientation != null) {
            /*
             * Undo rotation.
             */
            result = orientation.mult(result);
        }

        result.addLocal(center);
        assert contains(result) : result;
        return result;
    }

    /**
     * Score a location based on how well it "fits" with this region.
     *
     * @param location coordinates of the input (not null, unaffected)
     * @return score value (more positive &rarr; better)
     */
    @Override
    public double score(Vector3f location) {
        Vector3f offset = location.subtract(center);
        if (inverseRotation != null) {
            offset = inverseRotation.mult(offset);
        }
        if (weights != null) {
            offset.multLocal(weights);
        }
        double squaredValue = metric.squaredValue(offset);
        if (Double.isInfinite(optimalRSquared)) {
            return squaredValue;
        } else if (squaredValue >= optimalRSquared) {
            return optimalRSquared - squaredValue;
        } else {
            double result = Math.abs(optimalRSquared - squaredValue);
            return result;
        }
    }

    /**
     * Find a path between two locations in this region without leaving the
     * region. Short paths are preferred over long ones.
     *
     * @param startLocation coordinates (contained in region, unaffected)
     * @param goalLocation coordinates (contained in region, unaffected)
     * @return a new path spline, or null if none found
     */
    @Override
    public Spline3f shortestPath(Vector3f startLocation,
            Vector3f goalLocation) {
        assert contains(startLocation) : startLocation;
        assert contains(goalLocation) : goalLocation;

        if (!isConvex()) {
            throw new UnsupportedOperationException(); // TODO
        }
        Vector3f[] joints = {startLocation, goalLocation};
        Spline3f result = new LinearSpline3f(joints);

        return result;
    }

    /**
     * Calculate the distance from the specified starting point to the first
     * point of support (if any) directly below it in this region.
     *
     * @param location coordinates of starting point(not null, unaffected)
     * @param cosineTolerance cosine of maximum slope for support (&gt;0, &lt;1)
     * @return the shortest support distance (&ge;0) or
     * {@link Float#POSITIVE_INFINITY} if no support
     */
    @Override
    public float supportDistance(Vector3f location, float cosineTolerance) {
        Validate.nonNull(location, "location");
        Validate.fraction(cosineTolerance, "cosine tolerance");
        throw new UnsupportedOperationException(); // TODO
    }
}
