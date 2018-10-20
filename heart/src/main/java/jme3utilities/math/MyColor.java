/*
 Copyright (c) 2014-2018, Stephen Gold
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
package jme3utilities.math;

import com.jme3.math.ColorRGBA;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jme3utilities.Validate;

/**
 * Utility methods for RGBA colors.
 *
 * @author Stephen Gold sgold@sonic.net
 */
final public class MyColor {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(MyColor.class.getName());
    /**
     * pattern for matching a color
     */
    final private static Pattern colorPattern = Pattern.compile(
            "Color\\[\\s*([^,]+),\\s*([^,]+),\\s*([^,]+),\\s*(\\S+)\\s*]");
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private MyColor() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Interpolate linearly between two colors (with no side effect).
     * <p>
     * Use instead of ColorRGBA.interpolate(), which got renamed in JME 3.1.
     *
     * @param fraction (&le;1, &ge;0)
     * @param start 1st input color (not null, unaffected)
     * @param end 2nd input color (not null, unaffected)
     * @return new color, start*(1-fraction) + end*fraction
     */
    public static ColorRGBA interpolateLinear(float fraction, ColorRGBA start,
            ColorRGBA end) {
        Validate.fraction(fraction, "fraction");
        Validate.nonNull(start, "start");
        Validate.nonNull(end, "end");

        float r = MyMath.lerp(fraction, start.r, end.r);
        float g = MyMath.lerp(fraction, start.g, end.g);
        float b = MyMath.lerp(fraction, start.b, end.b);
        float a = MyMath.lerp(fraction, start.a, end.a);
        ColorRGBA result = new ColorRGBA(r, g, b, a);

        return result;
    }

    /**
     * Parse a color from the specified text string.
     *
     * @param textString input text (not null, not empty)
     * @return a new color instance, or null if text is invalid
     */
    public static ColorRGBA parse(String textString) {
        Validate.nonEmpty(textString, "text string");

        ColorRGBA result = null;
        Matcher matcher = colorPattern.matcher(textString);
        boolean valid = matcher.matches();
        if (valid) {
            String rText = matcher.group(1);
            float r = Float.parseFloat(rText);
            String gText = matcher.group(2);
            float g = Float.parseFloat(gText);
            String bText = matcher.group(3);
            float b = Float.parseFloat(bText);
            String aText = matcher.group(4);
            float a = Float.parseFloat(aText);
            result = new ColorRGBA(r, g, b, a);
        }

        return result;
    }

    /**
     * Parse a color from the specified text string.
     *
     * @param textString input text (not null, not empty)
     * @return a new color instance, or null if text is invalid
     * @deprecated use {@link #parse(java.lang.String)}
     */
    @Deprecated
    public static ColorRGBA parseColor(String textString) {
        return parse(textString);
    }

    /**
     * Generate a brightened and saturated version of a specified color (with no
     * side effect).
     *
     * @param baseColor input color (not null, unaffected)
     * @return new color with the same hue, but full brightness and full
     * saturation
     */
    public static ColorRGBA saturate(ColorRGBA baseColor) {
        float max = MyMath.max(baseColor.r, baseColor.g, baseColor.b);
        if (max <= 0f) {
            return new ColorRGBA(1f, 1f, 1f, baseColor.a);
        }
        ColorRGBA result = baseColor.mult(1f / max);

        return result;
    }
}
