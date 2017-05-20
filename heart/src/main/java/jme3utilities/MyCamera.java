/*
 Copyright (c) 2013-2017, Stephen Gold
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
package jme3utilities;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.math.MyMath;
import jme3utilities.math.MyVector3f;

/**
 * Utility methods that operate on jME3 cameras. Aside from test cases, all
 * methods should be public and static.
 *
 * @author Stephen Gold sgold@sonic.net
 */
final public class MyCamera {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            MyCamera.class.getName());
    /**
     * local copy of {@link com.jme3.math.Vector3f#UNIT_X}
     */
    final private static Vector3f xAxis = new Vector3f(1f, 0f, 0f);
    /**
     * local copy of {@link com.jme3.math.Vector3f#UNIT_Y}
     */
    final private static Vector3f yAxis = new Vector3f(0f, 1f, 0f);
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private MyCamera() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Calculate the aspect ratio of the specified camera.
     *
     * @param camera camera to measure (not null, unaffected)
     * @return width divided by height (&gt;0)
     */
    public static float aspectRatio(Camera camera) {
        float height = camera.getHeight();
        assert height > 0f : height;

        float width = camera.getWidth();
        assert width > 0f : width;

        float ratio = width / height;

        return ratio;
    }

    /**
     * Calculate the azimuth angle of the specified camera.
     *
     * @param camera camera to measure (not null, unaffected)
     * @return radians east of north
     */
    public static float azimuth(Camera camera) {
        Vector3f direction = camera.getDirection();
        float azimuth = MyVector3f.azimuth(direction);
        return azimuth;
    }

    /**
     * Calculate the vertical field-of-view angle of the specified camera.
     *
     * @param camera camera to measure (not null, unaffected)
     * @return radians from top of frustum to bottom of frustum
     */
    public static float fovY(Camera camera) {
        if (camera.isParallelProjection()) {
            return 0f;
        }

        float near = camera.getFrustumNear();
        assert near > 0f : near;

        float top = camera.getFrustumTop();
        assert top > 0f : top;

        float fovY = 2f * FastMath.atan(top / near);

        return fovY;
    }

    /**
     * Rotate a camera without changing its location, setting its "up" direction
     * automatically.
     *
     * @param camera camera to rotate (not null)
     * @param direction (length&gt;0, unaffected)
     */
    public static void look(Camera camera, Vector3f direction) {
        Validate.nonZero(direction, "new direction");

        if (direction.x == 0f && direction.z == 0f) {
            /*
             * When looking straight up or down, use +X as the up direction.
             */
            camera.lookAtDirection(direction, xAxis);
        } else {
            camera.lookAtDirection(direction, yAxis);
        }
    }

    /**
     * Alter a camera's near and far planes without affecting its aspect ratio
     * or field-of-view.
     *
     * @param camera camera to alter (not null)
     * @param newNear distance to the near clipping plane (&lt;newFar, &gt;0)
     * @param newFar distance to the far clipping plane (&gt;newNear)
     */
    public static void setNearFar(Camera camera, float newNear, float newFar) {
        Validate.positive(newNear, "near");
        if (!(newFar > newNear)) {
            logger.log(Level.SEVERE, "far={0} near={1}",
                    new Object[]{newFar, newNear});
            throw new IllegalArgumentException(
                    "far should be greater than near");
        }

        if (camera.isParallelProjection()) {
            camera.setFrustumFar(newFar);
            camera.setFrustumNear(newNear);
        } else {
            float aspectRatio = aspectRatio(camera);
            float yDegrees = yDegrees(camera);
            camera.setFrustumPerspective(yDegrees, aspectRatio, newNear,
                    newFar);
        }
    }

    /**
     * Alter a camera's field-of-view tangents.
     *
     * @param camera camera to alter (not null)
     * @param newTangent tangent of the vertical field-of-view half-angle
     * (&gt;0)
     */
    public static void setYTangent(Camera camera, float newTangent) {
        Validate.nonNull(camera, "camera");
        Validate.positive(newTangent, "tangent");

        float yTangent = yTangent(camera);
        float factor = newTangent / yTangent;
        zoom(camera, factor);
    }

    /**
     * Calculate the vertical field-of-view angle of the specified camera.
     *
     * @param camera camera to measure (not null, unaffected)
     * @return vertical angle in degrees (&gt;0)
     */
    public static float yDegrees(Camera camera) {
        Validate.nonNull(camera, "camera");
        if (camera.isParallelProjection()) {
            return 0f;
        }

        float yTangent = yTangent(camera);
        float yRadians = 2f * FastMath.atan(yTangent);
        float yDegrees = MyMath.toDegrees(yRadians);

        return yDegrees;
    }

    /**
     * Calculate the vertical field-of-view tangent of the specified camera.
     *
     * @param camera camera to measure (not null, unaffected)
     * @return tangent of the vertical field-of-view half-angle (&gt;0)
     */
    public static float yTangent(Camera camera) {
        float near = camera.getFrustumNear();
        assert near > 0f : near;

        float top = camera.getFrustumTop();
        assert top > 0f : top;

        float yTangent = top / near;

        return yTangent;
    }

    /**
     * Increase a camera's field-of-view tangents by the specified factor.
     *
     * @param camera camera to alter (not null)
     * @param factor amount to reduce both field-of-view tangents (&gt;0)
     */
    public static void zoom(Camera camera, float factor) {
        Validate.positive(factor, "factor");

        float bottom = camera.getFrustumBottom();
        camera.setFrustumBottom(bottom * factor);

        float left = camera.getFrustumLeft();
        camera.setFrustumLeft(left * factor);

        float right = camera.getFrustumRight();
        camera.setFrustumRight(right * factor);

        float top = camera.getFrustumTop();
        camera.setFrustumTop(top * factor);
    }
}
