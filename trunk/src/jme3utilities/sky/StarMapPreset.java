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