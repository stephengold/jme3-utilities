/*
 Copyright (c) 2014, Stephen Gold
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

import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;

/**
 * Constants shared among classes the jme3utilities.sky package.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
class Constants {
    // *************************************************************************
    // constants

    /**
     * maximum value for any opacity
     */
    final static float alphaMax = 1f;
    /**
     * minimum value for opacity
     */
    final static float alphaMin = 0f;
    /**
     * default observer's latitude - Wiltshire (radians north of the equator)
     */
    final static float defaultLatitude = 51.1788f * FastMath.DEG_TO_RAD;
    /**
     * 1st (U) texture coordinate of the top of a DomeMesh
     */
    final static float topU = 0.5f;
    /**
     * 2nd (V) texture coordinate of the top of a DomeMesh
     */
    final static float topV = 0.5f;
    /**
     * maximum value for texture coordinates which do not wrap
     */
    final static float uvMax = 1f;
    /**
     * minimum value for texture coordinates which do not wrap
     */
    final static float uvMin = 0f;
    /**
     * UV distance from top to rim (<0.5, >0)
     */
    final static float uvScale = 0.44f;
    /**
     * coefficient used to compute stretchFactor
     */
    final static float stretchCoefficient =
            (FastMath.HALF_PI - uvMax) / (uvScale * uvScale);
    /**
     * the duration of a full day (in hours)
     */
    final static int hoursPerDay = 24;
    /**
     * texture coordinates of the top of a DomeMesh
     */
    final static Vector2f topUV = new Vector2f(topU, topV);
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private Constants() {
    }
}