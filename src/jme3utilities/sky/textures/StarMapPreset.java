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
package jme3utilities.sky.textures;

import com.jme3.math.FastMath;

/**
 * Enumerate the pre-set conditions for MakeStarMaps.
 *
 * @author Stephen Gold sgold@sonic.net
 */
enum StarMapPreset {
    // *************************************************************************
    // values

    /**
     * stars of the Northern Hemisphere at 2048x2048 resolution
     */
    NORTH_4M,
    /**
     * stars of the Northern Hemisphere at 4096x4096 resolution
     */
    NORTH_16M,
    /**
     * stars of the Southern Hemisphere at 2048x2048 resolution
     */
    SOUTH_4M,
    /**
     * stars of the Southern Hemisphere at 4096x4096 resolution
     */
    SOUTH_16M,
    /**
     * stars of Wiltshire 10h33m local sidereal time at 2048x2048 resolution
     */
    WILTSHIRE_4M,
    /**
     * stars of Wiltshire 10h33m local sidereal time at 4096x4096 resolution
     */
    WILTSHIRE_16M;
    // *************************************************************************
    // new methods exposed

    /**
     * Look up the textual description of this preset.
     */
    String describe() {
        switch (this) {
            case NORTH_4M:
                return "north";
            case NORTH_16M:
                return "north_16m";
            case SOUTH_4M:
                return "south";
            case SOUTH_16M:
                return "south_16m";
            case WILTSHIRE_4M:
                return "wiltshire";
            case WILTSHIRE_16M:
                return "wiltshire_16m";
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
     * Compute the sidereal time for this preset.
     *
     * @return number of hours since midnight (&le;24, &ge;0)
     */
    float hour() {
        switch (this) {
            case NORTH_4M:
            case NORTH_16M:
            case SOUTH_4M:
            case SOUTH_16M:
                return 0f;
            case WILTSHIRE_4M:
            case WILTSHIRE_16M:
                /*
                 * At 10h33m, Orion is about to set in the west and the
                 * Pointers of the Big Dipper are near the meridian.
                 */
                return 10.55f;
        }
        throw new IllegalStateException();
    }

    /**
     * Look up the observer's latitude for this preset.
     *
     * @return radians north of the equator (&le;Pi/2, &ge;-Pi/2)
     */
    float latitude() {
        switch (this) {
            case NORTH_4M:
            case NORTH_16M:
                return FastMath.HALF_PI;
            case SOUTH_4M:
            case SOUTH_16M:
                return -FastMath.HALF_PI;
            case WILTSHIRE_4M:
            case WILTSHIRE_16M:
                /*
                 * Stonehenge
                 */
                return 51.1788f * FastMath.DEG_TO_RAD;
        }
        throw new IllegalStateException();
    }

    /**
     * Look up the name of the texture file corresponding to this preset.
     */
    String textureFileName() {
        switch (this) {
            case NORTH_4M:
                return "northern";
            case NORTH_16M:
                return "16m/northern";
            case SOUTH_4M:
                return "southern";
            case SOUTH_16M:
                return "16m/southern";
            case WILTSHIRE_4M:
                return "wiltshire";
            case WILTSHIRE_16M:
                return "16m/wiltshire";
        }
        throw new IllegalStateException();
    }

    /**
     * Look up the texture resolution for this preset.
     *
     * @return size of the texture map (pixels per side)
     */
    int textureSize() {
        switch (this) {
            case NORTH_4M:
            case SOUTH_4M:
            case WILTSHIRE_4M:
                return 2_048;
            case NORTH_16M:
            case SOUTH_16M:
            case WILTSHIRE_16M:
                return 4_096;
        }
        throw new IllegalStateException();
    }
}