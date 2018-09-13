/*
 Copyright (c) 2017-2018, Stephen Gold
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

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jme3utilities.Validate;
import jme3utilities.math.MyVector3f;

/**
 * Controller for a text-entry dialog box used to input an boolean value.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class VectorDialog extends TextEntryDialog {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(VectorDialog.class.getName());
    /**
     * pattern for matching a vector element
     */
    final private static Pattern elementPattern
            = Pattern.compile("([^)(,\\s]+)");
    /**
     * pattern for matching the word null
     */
    final private static Pattern nullPattern
            = Pattern.compile("\\s*null\\s*");
    // *************************************************************************
    // fields

    /**
     * if true, "null" is an allowed value, otherwise it is disallowed
     */
    final private boolean allowNull;
    /**
     * number of elements in the vector (&ge;2, &le;4)
     */
    final private int numElements;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a controller.
     *
     * @param description commit-button text (not null, not empty)
     * @param numElements number of elements in the vector (&ge;2, &le;4)
     * @param allowNull if true, "null" will be an allowed value
     */
    public VectorDialog(String description, int numElements,
            boolean allowNull) {
        super(description);
        Validate.inRange(numElements, "number of elements", 2, 4);

        this.numElements = numElements;
        this.allowNull = allowNull;
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Parse the specified text to obtain a vector.
     *
     * @param text (not null, not empty)
     * @return a new vector (not null)
     */
    public static Object parseVector(String text) {
        Validate.nonEmpty(text, "text");

        String lcText = text.toLowerCase(Locale.ROOT);
        Matcher matcher = elementPattern.matcher(lcText);
        List<Float> elements = new ArrayList<>(4);
        while (matcher.find()) {
            String group = matcher.group(1);
            float element = Float.parseFloat(group);
            elements.add(element);
        }
        Object result;
        int numElements = elements.size();
        float x = elements.get(MyVector3f.xAxis);
        float y = elements.get(MyVector3f.yAxis);
        float z, w;
        switch (numElements) {
            case 2:
                result = new Vector2f(x, y);
                break;

            case 3:
                z = elements.get(MyVector3f.zAxis);
                result = new Vector3f(x, y, z);
                break;

            case 4:
                z = elements.get(MyVector3f.zAxis);
                w = elements.get(3);
                result = new Vector4f(x, y, z, w);
                break;

            default:
                throw new IllegalArgumentException();
        }

        return result;
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
        String msg = "";

        Matcher matcher = elementPattern.matcher(lcText);
        int elementCount = 0;
        while (msg.isEmpty() && matcher.find()) {
            String element = matcher.group(1);
            try {
                float inputValue = Float.parseFloat(element);
                if (Float.isNaN(inputValue)) {
                    msg = notAVector();
                }
                ++elementCount;
            } catch (NumberFormatException e) {
                msg = notAVector();
            }
        }
        if (elementCount != numElements) {
            msg = notAVector();
        }

        if (!msg.isEmpty() && allowNull && matchesNull(lcText)) {
            msg = "";
        }

        return msg;
    }
    // *************************************************************************
    // private methods

    /**
     * Test whether the specified string matches nullPattern.
     *
     * @param lcText text string (converted to lower case)
     * @return true for match, otherwise false
     */
    private boolean matchesNull(String lcText) {
        assert lcText != null;

        Matcher matcher = nullPattern.matcher(lcText);
        boolean result = matcher.matches();

        return result;
    }

    /**
     * Generate a feedback message when the text does not represent a vector.
     *
     * @return message text (not null, not empty)
     */
    private String notAVector() {
        String msg = String.format("must be a %d-element vector", numElements);
        if (allowNull) {
            msg += " or null";
        }

        return msg;
    }
}
