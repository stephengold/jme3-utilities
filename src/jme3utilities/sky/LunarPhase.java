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
package jme3utilities.sky;

import com.jme3.math.FastMath;

/**
 * Enumerate the phases of the moon.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public enum LunarPhase {
    // *************************************************************************
    // values

    FULL, WANING_CRESCENT, WANING_GIBBOUS, WAXING_CRESCENT, WAXING_GIBBOUS;
    // *************************************************************************
    // new methods exposed

    /**
     * Look up the textual description of this phase.
     */
    public String describe() {
        switch (this) {
            case FULL:
                return "full";
            case WANING_CRESCENT:
                return "waning-crescent";
            case WANING_GIBBOUS:
                return "waning-gibbous";
            case WAXING_CRESCENT:
                return "waxing-crescent";
            case WAXING_GIBBOUS:
                return "waxing-gibbous";
        }
        return "?";
    }

    /**
     * Find a phase based on its textual description.
     *
     * @param description returned by describe()
     * @return the phase, or null if the description does not match any value
     */
    public static LunarPhase fromDescription(String description) {
        for (LunarPhase phase : values()) {
            if (phase.describe().equals(description)) {
                return phase;
            }
        }
        return null;
    }

    /**
     * Look up the path to the image map for this phase.
     *
     * @return asset path (not null)
     */
    public String imagePath() {
        String description = describe();
        String assetPath =
                String.format("Textures/skies/moon/%s.png", description);
        return assetPath;
    }

    /**
     * Look up the celestial longitude difference for this phase.
     *
     * @return angle in radians
     */
    public float longitudeDifference() {
        switch (this) {
            case FULL:
                return FastMath.PI;
            case WANING_CRESCENT:
                return 1.75f * FastMath.PI;
            case WANING_GIBBOUS:
                return 1.25f * FastMath.PI;
            case WAXING_CRESCENT:
                return 0.25f * FastMath.PI;
            case WAXING_GIBBOUS:
                return 0.75f * FastMath.PI;
        }
        throw new IllegalStateException();
    }
}