/*
 Copyright (c) 2019, Stephen Gold
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
package jme3utilities.ui;

import java.util.logging.Logger;
import jme3utilities.Validate;
import jme3utilities.math.MyMath;

/**
 * An immutable set of display-size limits.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class DisplaySizeLimits {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(DisplaySizeLimits.class.getName());
    // *************************************************************************
    // fields

    /**
     * maximum display height (in pixels, &ge;minHeight)
     */
    final private int maxHeight;
    /**
     * maximum display width (in pixels, &ge;minWidth)
     */
    final private int maxWidth;
    /**
     * minimum display height (in pixels, &le;maxHeight)
     */
    final private int minHeight;
    /**
     * minimum display width (in pixels, &le;maxWidth)
     */
    final private int minWidth;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a set of limits.
     *
     * @param minWidth minimum display width (in pixels, &gt;0, &le;maxWidth)
     * @param minHeight minimum display height (in pixels, &gt;0, &le;maxHeight)
     * @param maxWidth maximum display width (in pixels, &ge;minWidth)
     * @param maxHeight maximum display height (in pixels, &ge;minHeight)
     */
    public DisplaySizeLimits(int minWidth, int minHeight, int maxWidth,
            int maxHeight) {
        Validate.inRange(minWidth, "minimum width", 1, maxWidth);
        Validate.inRange(minHeight, "minimum height", 1, maxHeight);
        Validate.inRange(maxWidth, "maximum width", minWidth,
                Integer.MAX_VALUE);
        Validate.inRange(maxHeight, "maximum height", minHeight,
                Integer.MAX_VALUE);

        this.minWidth = minWidth;
        this.minHeight = minHeight;
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Test the validity of the specified display size.
     *
     * @param width the display width (in pixels)
     * @param height the height (in pixels)
     * @return true if in range, otherwise false
     */
    public boolean isValidDisplaySize(int width, int height) {
        if (!MyMath.isBetween(minHeight, height, maxHeight)) {
            return false;

        } else if (!MyMath.isBetween(minWidth, width, maxWidth)) {
            return false;

        } else {
            return true;
        }
    }

    /**
     * Read the maximum display height.
     *
     * @return height (in pixels, &gt;0)
     */
    public int maxHeight() {
        assert maxHeight > 0 : maxHeight;
        return maxHeight;
    }

    /**
     * Read the maximum display width.
     *
     * @return width (in pixels, &gt;0)
     */
    public int maxWidth() {
        assert maxWidth > 0 : maxWidth;
        return maxWidth;
    }

    /**
     * Read the minimum display height.
     *
     * @return height (in pixels, &gt;0)
     */
    public int minHeight() {
        assert minHeight > 0 : minHeight;
        return minHeight;
    }

    /**
     * Read the minimum display width.
     *
     * @return width (in pixels, &gt;0)
     */
    public int minWidth() {
        assert minWidth > 0 : minWidth;
        return minWidth;
    }
}
