/*
 Copyright (c) 2017-2022, Stephen Gold
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

/**
 * Controller for a text-entry dialog box used to input a boolean value.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class BooleanDialog extends TextEntryDialog {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(BooleanDialog.class.getName());
    /**
     * pattern for matching the word "false"
     */
    final private static Pattern falsePattern
            = Pattern.compile("\\s*false\\s*");
    /**
     * pattern for matching the word "true"
     */
    final private static Pattern truePattern
            = Pattern.compile("\\s*true\\s*");
    // *************************************************************************
    // fields

    /**
     * is "null" a valid input?
     */
    final private AllowNull allowNull;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a controller.
     *
     * @param description commit-button text (not null, not empty)
     * @param allowNull should "null" be a valid input? (not null)
     */
    public BooleanDialog(String description, AllowNull allowNull) {
        super(description);
        Validate.nonNull(allowNull, "allow null");
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

        String lcText = input.toLowerCase(Locale.ROOT);
        String msg = notABoolean();

        if (matchesTrue(lcText) || matchesFalse(lcText)) {
            msg = "";
        } else if (allowNull.equals(AllowNull.Yes) && matchesNull(lcText)) {
            msg = "";
        }

        return msg;
    }
    // *************************************************************************
    // private methods

    /**
     * Test whether the specified string matches falsePattern.
     *
     * @param lcText text string (not null, assumed to be in lower case)
     * @return true for match, otherwise false
     */
    private static boolean matchesFalse(String lcText) {
        assert lcText != null;

        Matcher matcher = falsePattern.matcher(lcText);
        boolean result = matcher.matches();

        return result;
    }

    /**
     * Test whether the specified string matches truePattern.
     *
     * @param lcText text string (not null, assumed to be in lower case)
     * @return true for match, otherwise false
     */
    private static boolean matchesTrue(String lcText) {
        assert lcText != null;

        Matcher matcher = truePattern.matcher(lcText);
        boolean result = matcher.matches();

        return result;
    }

    /**
     * Generate a feedback message when the text does not represent a Boolean.
     *
     * @return message text (not null, not empty)
     */
    private String notABoolean() {
        if (allowNull.equals(AllowNull.Yes)) {
            return "must be true, false, or null";
        } else {
            return "must be true or false";
        }
    }
}
