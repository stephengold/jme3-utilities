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
package jme3utilities;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import java.util.logging.Logger;

/**
 * Camera utility methods. Aside from test cases, all methods should be public
 * and static.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class MyCamera {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(MyCamera.class.getName());
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
     * Calculate a camera's azimuth angle.
     *
     * @param camera which camera (not null)
     * @return radians east of north
     */
    public static float azimuth(Camera camera) {
        Vector3f direction = camera.getDirection();
        VectorXZ mapDirection = new VectorXZ(direction);
        float azimuth = mapDirection.azimuth();
        return azimuth;
    }

    /**
     * Calculate a camera's vertical field-of-view angle.
     *
     * @param camera which camera (not null)
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
     * Decrease a camera's field-of-view tangent by a given factor.
     *
     * @param camera which camera (not null)
     * @param factor amount to reduce the FOV tangent (>0)
     */
    public static void zoom(Camera camera, float factor) {
        if (factor <= 0f) {
            throw new IllegalArgumentException(
                    "factor must be positive");
        }

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