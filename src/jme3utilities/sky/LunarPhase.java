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