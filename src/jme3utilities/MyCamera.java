/*
 Copyright (c) 2013, Stephen Gold
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
        float azimuth = MyVector3f.azimuth(direction);
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
     * Set a camera's field-of-view tangent.
     *
     * @param camera which camera (not null)
     * @param newTangent value for the FOV tangent (>0)
     */
    public static void setYTangent(Camera camera, float newTangent) {
        if (camera == null) {
            throw new NullPointerException("camera should not be null");
        }
        if (newTangent <= 0f) {
            logger.log(Level.SEVERE, "newTangent={0}", newTangent);
            throw new IllegalArgumentException("tangent should be positive");
        }

        float yTangent = yTangent(camera);
        float factor = newTangent / yTangent;
        zoom(camera, factor);
    }

    /**
     * Calculate a camera's field-of-view tangent.
     *
     * @param camera which camera (not null)
     * @return top/near (>0)
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
     * Increase a camera's field-of-view tangent by a given factor.
     *
     * @param camera which camera (not null)
     * @param factor amount to reduce the FOV tangent (>0)
     */
    public static void zoom(Camera camera, float factor) {
        if (factor <= 0f) {
            logger.log(Level.SEVERE, "factor={0}", factor);
            throw new IllegalArgumentException("factor should be positive");
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