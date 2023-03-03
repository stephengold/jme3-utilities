/*
 Copyright (c) 2018-2023, Stephen Gold
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
import jme3utilities.Validate;
import jme3utilities.math.RectSizeLimits;
import jme3utilities.ui.DsUtils;

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
    // *************************************************************************
    // fields

    /**
     * range of valid display sizes (not null)
     */
    final public RectSizeLimits sizeLimits;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a controller.
     *
     * @param description the commit-button text (not null, not empty)
     * @param limits the range of valid display sizes (not null)
     */
    public DisplaySizeDialog(String description, RectSizeLimits limits) {
        super(description);
        Validate.nonNull(limits, "limits");
        this.sizeLimits = limits;
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
            int[] size = DsUtils.parseDimensions(lcInput);
            if (size == null) {
                msg = "improperly formatted display dimensions";
            } else {
                int width = size[0];
                int height = size[1];
                msg = sizeLimits.feedbackInRange(width, height);
            }
        }

        return msg;
    }
}
