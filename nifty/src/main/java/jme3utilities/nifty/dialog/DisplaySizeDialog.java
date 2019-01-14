/*
 Copyright (c) 2018-2019, Stephen Gold
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
package jme3utilities.nifty.dialog;

import java.util.Locale;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jme3utilities.Validate;
import jme3utilities.ui.DisplaySizeLimits;

/**
 * Controller for a text-entry dialog box used to input display dimensions.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class DisplaySizeDialog extends TextEntryDialog {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(DisplaySizeDialog.class.getName());
    /**
     * pattern for matching a display dimensions
     */
    final private static Pattern dimensionsPattern
            = Pattern.compile("^\\s*(\\d+)\\s*[x,]\\s*(\\d+)\\s*");
    // *************************************************************************
    // fields

    /**
     * range of valid display sizes (not null)
     */
    final public DisplaySizeLimits limits;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a controller.
     *
     * @param description the commit-button text (not null, not empty)
     * @param dsl the range of valid display sizes (not null)
     */
    public DisplaySizeDialog(String description, DisplaySizeLimits dsl) {
        super(description);
        Validate.nonNull(dsl, "limits");
        limits = dsl;
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Parse the specified text to obtain display dimensions.
     *
     * @param text the input text (not null, not empty)
     * @return a new array containing the width and height, or null for a syntax
     * error
     */
    public static int[] parseDisplaySize(String text) {
        Validate.nonEmpty(text, "text");

        String lcText = text.toLowerCase(Locale.ROOT);
        Matcher matcher = dimensionsPattern.matcher(lcText);
        int[] result = null;
        if (matcher.find()) {
            result = new int[2];
            String widthText = matcher.group(1);
            result[0] = Integer.parseInt(widthText);
            String heightText = matcher.group(2);
            result[1] = Integer.parseInt(heightText);
        }

        return result;
    }
    // *************************************************************************
    // TextEntryDialog methods

    /**
     * Determine the feedback message for the specified input text.
     *
     * @param input the input text (not null)
     * @return the feedback message (not null)
     */
    @Override
    protected String feedback(String input) {
        Validate.nonNull(input, "input");

        String lcInput = input.toLowerCase(Locale.ROOT);

        String msg;
        if ("min".equals(lcInput) || "max".equals(lcInput)) {
            msg = "";
        } else {
            Matcher matcher = dimensionsPattern.matcher(lcInput);
            if (matcher.find()) {
                String widthText = matcher.group(1);
                int width = Integer.parseInt(widthText);
                String heightText = matcher.group(2);
                int height = Integer.parseInt(heightText);
                msg = limits.feedbackValid(width, height);
            } else {
                msg = "improperly formatted display dimensions";
            }
        }

        return msg;
    }
}
