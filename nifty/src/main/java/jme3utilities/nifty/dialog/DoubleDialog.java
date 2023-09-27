/*
 Copyright (c) 2017-2023, Stephen Gold
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
 * Controller for a text-entry dialog box used to input a double-precision
 * floating-point value. TODO accept MAX_VALUE and MIN_VALUE as inputs
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class DoubleDialog extends TextEntryDialog {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(DoubleDialog.class.getName());
    // *************************************************************************
    // fields

    /**
     * is "null" a valid input?
     */
    final private AllowNull allowNull;
    /**
     * maximum value to commit
     */
    final private double maxValue;
    /**
     * minimum value to commit
     */
    final private double minValue;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a controller.
     *
     * @param description commit-button text (not null, not empty)
     * @param min minimum value (&lt;max)
     * @param max minimum value (&gt;min)
     * @param allowNull should "null" be a valid input? (not null)
     */
    public DoubleDialog(
            String description, double min, double max, AllowNull allowNull) {
        super(description);
        Validate.require(min < max, "min < max");
        Validate.nonNull(allowNull, "allow null");

        this.minValue = min;
        this.maxValue = max;
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
            double inputValue = Double.parseDouble(input);
            if (inputValue < minValue) {
                String minText = Double.toString(minValue);
                msg = String.format("must not be < %s", minText);
            } else if (inputValue > maxValue) {
                String maxText = Double.toString(maxValue);
                msg = String.format("must not be > %s", maxText);
            } else if (Double.isNaN(inputValue)) {
                msg = notANumber();
            }
        } catch (NumberFormatException exception) {
            msg = notANumber();
        }

        String lcText = input.toLowerCase(Locale.ROOT);
        if (allowNull.equals(AllowNull.Yes) && matchesNull(lcText)) {
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
        if (allowNull.equals(AllowNull.Yes)) {
            return "must be a number or null";
        } else {
            return "must be a number";
        }
    }
}
