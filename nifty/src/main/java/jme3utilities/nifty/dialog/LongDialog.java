/*
 Copyright (c) 2017-2020, Stephen Gold
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

/**
 * Controller for a text-entry dialog box used to input a long integer value.
 * TODO accept MAX_VALUE and MIN_VALUE as inputs
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class LongDialog extends TextEntryDialog {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(LongDialog.class.getName());
    // *************************************************************************
    // fields

    /**
     * if true, "null" is an allowed value, otherwise it is disallowed
     */
    final private boolean allowNull;
    /**
     * maximum value to commit
     */
    final private long maxValue;
    /**
     * minimum value to commit
     */
    final private long minValue;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a controller.
     *
     * @param description commit-button text (not null, not empty)
     * @param min minimum value (&lt;max)
     * @param max minimum value (&gt;min)
     * @param allowNull if true, "null" will be an allowed value
     */
    public LongDialog(String description, long min, long max,
            boolean allowNull) {
        super(description);
        assert min < max : max;

        minValue = min;
        maxValue = max;
        this.allowNull = allowNull;
    }
    // *************************************************************************
    // TextEntryDialog methods

    /**
     * Determine the feedback message for the specified input text.
     *
     * @param input the input text (not null)
     * @return the message (not null)
     */
    @Override
    protected String feedback(String input) {
        Validate.nonNull(input, "input");

        String msg = "";
        try {
            long inputValue = Long.parseLong(input);
            if (inputValue < minValue) {
                msg = String.format("must not be < %d", minValue);
            } else if (inputValue > maxValue) {
                msg = String.format("must not be > %d", maxValue);
            }
        } catch (NumberFormatException e) {
            msg = notANumber();
        }

        String lcText = input.toLowerCase(Locale.ROOT);
        if (allowNull && matchesNull(lcText)) {
            msg = "";
        }

        return msg;
    }
    // *************************************************************************
    // private methods

    /**
     * Generate a feedback message when the text does not represent a number.
     *
     * @return message text (not null, not empty)
     */
    private String notANumber() {
        if (allowNull) {
            return "must be a number or null";
        } else {
            return "must be a number";
        }
    }
}
