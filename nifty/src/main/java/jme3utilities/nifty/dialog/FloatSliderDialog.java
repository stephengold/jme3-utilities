/*
 Copyright (c) 2020-2023, Stephen Gold
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

import com.jme3.math.FastMath;
import de.lessvoid.nifty.controls.Button;
import de.lessvoid.nifty.controls.Slider;
import de.lessvoid.nifty.controls.TextField;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.elements.render.TextRenderer;
import java.util.logging.Logger;
import jme3utilities.Validate;
import jme3utilities.math.MyMath;

/**
 * Controller for a text-and-slider dialog box used to input a single-precision
 * floating-point value.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class FloatSliderDialog implements DialogController {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(FloatSliderDialog.class.getName());
    // *************************************************************************
    // fields

    /**
     * flag that causes this controller to temporarily ignore change events from
     * GUI controls during an update
     */
    private boolean ignoreChanges = false;
    /**
     * maximum value to commit
     */
    final private float maxValue;
    /**
     * minimum value to commit
     */
    final private float minValue;
    /**
     * description of the commit action (not null, not empty, should fit the
     * button -- about 8 or 9 characters)
     */
    final private String commitDescription;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a controller.
     *
     * @param description commit-button text (not null, not empty)
     * @param min minimum value (&lt;max)
     * @param max minimum value (&gt;min)
     */
    public FloatSliderDialog(String description, float min, float max) {
        Validate.nonEmpty(description, "description");
        Validate.require(min < max, "min < max");

        this.commitDescription = description;
        this.minValue = min;
        this.maxValue = max;
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Update the value based on the slider.
     *
     * @param dialogElement (not null)
     */
    public void onSliderChanged(Element dialogElement) {
        Validate.nonNull(dialogElement, "dialog element");
        if (ignoreChanges) {
            return;
        }

        Slider slider
                = dialogElement.findNiftyControl("#dialogslider", Slider.class);
        float raw = slider.getValue();
        float newValue = MyMath.lerp(raw, minValue, maxValue);

        TextField textField = dialogElement.findNiftyControl(
                "#textfield", TextField.class);
        String newText = Float.toString(newValue);
        textField.setText(newText);
    }
    // *************************************************************************
    // new protected methods

    /**
     * Determine the feedback message for the specified input text.
     *
     * @param inputText (not null)
     * @return the message (not null)
     */
    protected String feedback(String inputText) {
        String msg = "";
        try {
            float inputValue = Float.parseFloat(inputText);
            if (inputValue < minValue) {
                String minText = Float.toString(minValue);
                msg = String.format("must not be < %s", minText);
            } else if (inputValue > maxValue) {
                String maxText = Float.toString(maxValue);
                msg = String.format("must not be > %s", maxText);
            } else if (Float.isNaN(inputValue)) {
                msg = "must be a number";
            }
        } catch (NumberFormatException exception) {
            msg = "must be a number";
        }

        return msg;
    }

    /**
     * Read the text field.
     *
     * @param dialogElement (not null)
     * @return a text string (not null)
     */
    protected String text(Element dialogElement) {
        TextField textField
                = dialogElement.findNiftyControl("#textfield", TextField.class);
        String text = textField.getRealText();

        assert text != null;
        return text;
    }
    // *************************************************************************
    // DialogController methods

    /**
     * Test whether "commit" actions are allowed.
     *
     * @param dialogElement (not null)
     * @return true if allowed, otherwise false
     */
    @Override
    public boolean allowCommit(Element dialogElement) {
        Validate.nonNull(dialogElement, "dialog element");

        String text = text(dialogElement);
        String feedback = feedback(text);
        boolean allow = feedback.isEmpty();

        return allow;
    }

    /**
     * Construct the action-string suffix for a commit.
     *
     * @param dialogElement (not null)
     * @return the suffix (not null)
     */
    @Override
    public String commitSuffix(Element dialogElement) {
        Validate.nonNull(dialogElement, "dialog element");
        String suffix = text(dialogElement);
        return suffix;
    }

    /**
     * Update this dialog box prior to rendering. (Invoked once per frame.)
     *
     * @param dialogElement (not null)
     * @param unused time interval between frames (in seconds, &ge;0)
     */
    @Override
    public void update(Element dialogElement, float unused) {
        Validate.nonNull(dialogElement, "dialog element");

        this.ignoreChanges = true;

        String text = text(dialogElement);
        String feedback = feedback(text);
        Button commitButton
                = dialogElement.findNiftyControl("#commit", Button.class);

        Element feedbackElement = dialogElement.findElementById("#feedback");
        TextRenderer renderer = feedbackElement.getRenderer(TextRenderer.class);
        renderer.setText(feedback);

        if (feedback.isEmpty()) {
            commitButton.setText(commitDescription);
            commitButton.getElement().show();

            Slider slider = dialogElement.findNiftyControl(
                    "#dialogslider", Slider.class);
            float value = Float.parseFloat(text);
            float raw = FastMath.unInterpolateLinear(value, minValue, maxValue);
            slider.setValue(raw);

        } else {
            commitButton.setText("");
            commitButton.getElement().hide();
        }

        this.ignoreChanges = false;
    }
}
