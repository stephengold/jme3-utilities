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
 * Enumerate the pre-set conditions for MakeStarMaps.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
enum StarMapPreset {
    // *************************************************************************
    // values

    NORTH, SOUTH, WILTSHIRE;
    // *************************************************************************
    // new methods exposed

    /**
     * Look up the observer's latitude for this preset.
     *
     * @return radians north of the equator (>=-Pi/2, <=Pi/2)
     */
    float latitude() {
        switch (this) {
            case NORTH:
                return FastMath.HALF_PI;
            case SOUTH:
                return -FastMath.HALF_PI;
            case WILTSHIRE:
                /*
                 * Stonehenge
                 */
                return 51.1788f * FastMath.DEG_TO_RAD;
        }
        throw new IllegalStateException();
    }

    /**
     * Look up the textual description of this preset.
     */
    String describe() {
        switch (this) {
            case NORTH:
                return "north";
            case SOUTH:
                return "south";
            case WILTSHIRE:
                return "wiltshire";
        }
        return "?";
    }

    /**
     * Find a preset value based on its textual description.
     *
     * @param description
     */
    static StarMapPreset fromDescription(String description) {
        for (StarMapPreset preset : StarMapPreset.values()) {
            if (preset.describe().equals(description)) {
                return preset;
            }
        }
        return null;
    }

    /**
     * Get the sidereal time for this preset.
     *
     * @return number of hours since midnight (>=0, <=24)
     */
    float hour() {
        switch (this) {
            case NORTH:
            case SOUTH:
                return 0f;
            case WILTSHIRE:
                /*
                 * At 10h33m, Orion is about to set in the west and the
                 * Pointers of the Big Dipper are near the meridian.
                 */
                return 10.55f;
        }
        throw new IllegalStateException();
    }

    /**
     * Look up the name of the texture file corresponding to this preset.
     */
    String textureFileName() {
        switch (this) {
            case NORTH:
                return "northern";
            case SOUTH:
                return "southern";
            case WILTSHIRE:
                return "wiltshire";
        }
        throw new IllegalStateException();
    }
}