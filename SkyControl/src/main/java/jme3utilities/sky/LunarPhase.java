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
package jme3utilities.sky;

import com.jme3.math.FastMath;
import jme3utilities.Validate;

/**
 * Enumerate some phases of the moon.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public enum LunarPhase {
    // *************************************************************************
    // values

    /**
     * full moon: phase angle = 180 degrees
     */
    FULL,
    /**
     * 65% past full: phase angle = 297 degrees
     */
    WANING_CRESCENT,
    /**
     * 1/4 past full: phase angle = 225 degrees
     */
    WANING_GIBBOUS,
    /**
     * 35% past new: phase angle = 63 degrees
     */
    WAXING_CRESCENT,
    /**
     * 3/4 past new: phase angle = 135 degrees
     */
    WAXING_GIBBOUS,
    /**
     * custom phase: phase angle not defined
     */
    CUSTOM;
    // *************************************************************************
    // new methods exposed

    /**
     * Look up the textual description of this phase. Unlike toString(), the
     * description is in lower case.
     *
     * @return descriptive string of text (not null)
     */
    public String describe() {
        String result;
        switch (this) {
            case CUSTOM:
                result = "custom";
                break;
            case FULL:
                result = "full";
                break;
            case WANING_CRESCENT:
                result = "waning-crescent";
                break;
            case WANING_GIBBOUS:
                result = "waning-gibbous";
                break;
            case WAXING_CRESCENT:
                result = "waxing-crescent";
                break;
            case WAXING_GIBBOUS:
                result = "waxing-gibbous";
                break;
            default:
                result = String.format("ordinal=%d", ordinal());
        }
        return result;
    }

    /**
     * Find a phase based on its textual description.
     *
     * @param description returned by describe() (not null, not empty)
     * @return phase, or null if the description does not match any value
     */
    public static LunarPhase fromDescription(String description) {
        Validate.nonEmpty(description, "description");

        for (LunarPhase phase : values()) {
            if (phase.describe().equals(description)) {
                return phase;
            }
        }
        return null;
    }

    /**
     * Look up the path to the color map for this phase.
     *
     * @return asset path (not null)
     */
    public String imagePath() {
        if (this == CUSTOM) {
            throw new IllegalStateException("custom phase has no color map");
        }
        String description = describe();
        String assetPath = String.format(
                "Textures/skies/moon/%s.png", description);

        return assetPath;
    }

    /**
     * Look up the celestial longitude difference for this phase.
     *
     * @return radians east of the sun (&lt;2*Pi. &ge;0)
     */
    public float longitudeDifference() {
        switch (this) {
            case FULL:
                return FastMath.PI;
            case WANING_CRESCENT:
                return 1.65f * FastMath.PI;
            case WANING_GIBBOUS:
                return 1.25f * FastMath.PI;
            case WAXING_CRESCENT:
                return 0.35f * FastMath.PI;
            case WAXING_GIBBOUS:
                return 0.75f * FastMath.PI;
        }
        throw new IllegalStateException(this.describe());
    }
}
