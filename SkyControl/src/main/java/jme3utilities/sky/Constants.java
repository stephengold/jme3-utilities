/*
 Copyright (c) 2014-2020, Stephen Gold
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
package jme3utilities.sky;

import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import jme3utilities.math.MyMath;

/**
 * Constants shared among classes the jme3utilities.sky package.
 *
 * @author Stephen Gold sgold@sonic.net
 */
final public class Constants {
    // *************************************************************************
    // constants and loggers

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
    final static float defaultLatitude = MyMath.toRadians(51.1788f);
    /**
     * UV diameter of a sun's disc: in order to leave room for rays, haloes, and
     * haze, the disc is only 1/4 as wide as the color map.
     */
    final public static float discDiameter = 0.25f;
    /**
     * first (U) texture coordinate of the top of a DomeMesh
     */
    final static float topU = 0.5f;
    /**
     * 2nd (V) texture coordinate of the top of a DomeMesh
     */
    final static float topV = 0.5f;
    /**
     * maximum value for texture coordinates that do not wrap
     */
    final public static float uvMax = 1f;
    /**
     * minimum value for texture coordinates that do not wrap
     */
    final public static float uvMin = 0f;
    /**
     * UV distance from top to rim (&lt;0.5, &gt;0)
     */
    final static float uvScale = 0.44f;
    /**
     * coefficient used to compute the stretchFactor for objects projected onto
     * a DomeMesh
     */
    final public static float stretchCoefficient
            = (FastMath.HALF_PI - uvMax) / (uvScale * uvScale);
    /**
     * the duration of a full day (in hours)
     */
    final public static int hoursPerDay = 24;
    /**
     * texture coordinates of the top of a DomeMesh
     */
    final public static Vector2f topUV = new Vector2f(topU, topV);
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private Constants() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Read the terse version string for the SkyControl library.
     *
     * @return branch and revision (not null, not empty)
     */
    public static String versionShort() {
        return "for_jME3.2 0.9.24for32+1";
    }
}
