/*
 Copyright (c) 2013-2017, Stephen Gold
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
package jme3utilities.nifty;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import de.lessvoid.nifty.controls.Button;
import de.lessvoid.nifty.controls.CheckBox;
import de.lessvoid.nifty.controls.RadioButton;
import de.lessvoid.nifty.controls.Slider;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.elements.render.TextRenderer;
import de.lessvoid.nifty.screen.Screen;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.Validate;

/**
 * A pop screen controller with added support for Nifty controls such as check
 * boxes, radio buttons, sliders, and dynamic labels.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class GuiScreenController extends PopScreenController {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(GuiScreenController.class.getName());
    // *************************************************************************
    // constructors

    /**
     * Instantiate a disabled controller for the specified screen id and layout
     * asset path.
     *
     * @param screenId Nifty id (not null)
     * @param xmlAssetPath path to the Nifty XML layout asset (not null)
     * @param enableDuringInitialization if true, enable this screen controller
     * during initialization; if false, leave it disabled
     */
    public GuiScreenController(String screenId, String xmlAssetPath,
            boolean enableDuringInitialization) {
        super(screenId, xmlAssetPath, enableDuringInitialization);
        assert !isInitialized();
        assert !isEnabled();
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Access a Nifty check box. This assumes a naming convention where the
     * Nifty id of every check box ends with "CheckBox".
     *
     * @param idPrefix unique id prefix of the check box (not null)
     * @return the pre-existing instance (not null)
     */
    public CheckBox getCheckBox(String idPrefix) {
        Validate.nonNull(idPrefix, "check box id prefix");

        Screen screen = getScreen();
        String niftyId = idPrefix + "CheckBox";
        CheckBox box = screen.findNiftyControl(niftyId, CheckBox.class);
        if (box == null) {
            logger.log(Level.SEVERE, "missing check box {0} in {1}",
                    new Object[]{
                        MyString.quote(niftyId), MyString.quote(getScreenId())
                    });
            throw new RuntimeException();
        }

        return box;
    }

    /**
     * Access a Nifty slider. This assumes a naming convention where the Nifty
     * id of every slider ends with "Slider".
     *
     * @param idPrefix unique id prefix of the slider (not null)
     * @return the pre-existing instance (not null)
     */
    public Slider getSlider(String idPrefix) {
        Validate.nonNull(idPrefix, "slider id prefix");

        Screen screen = getScreen();
        String niftyId = idPrefix + "Slider";
        Slider slider = screen.findNiftyControl(niftyId, Slider.class);
        if (slider == null) {
            logger.log(Level.SEVERE, "missing slider {0} in {1}", new Object[]{
                MyString.quote(niftyId), MyString.quote(getScreenId())
            });
            throw new RuntimeException();
        }

        return slider;
    }

    /**
     * Test whether the identified check box is ticked. This assumes a naming
     * convention where the Nifty id of every check box ends with "CheckBox".
     *
     * @param idPrefix unique id prefix of the check box (not null)
     * @return true if ticked, otherwise false
     */
    public boolean isChecked(String idPrefix) {
        Validate.nonNull(idPrefix, "id prefix");

        CheckBox checkBox = getCheckBox(idPrefix);
        boolean result = checkBox.isChecked();

        return result;
    }

    /**
     * Read the transformed value of the named Nifty slider.
     *
     * @param name unique id prefix of the slider to read (not null)
     * @param transform how to transform the raw reading (not null)
     * @return transformed reading
     */
    public float readSlider(String name, SliderTransform transform) {
        Validate.nonNull(name, "name");
        Validate.nonNull(transform, "transform");

        Slider slider = getSlider(name);
        float result = readSlider(slider, transform);

        return result;
    }

    /**
     * Read a bank of 3 sliders that control a vector.
     *
     * @param name unique id infix of the bank to read (not null)
     * @param transform how to transform the raw readings (not null)
     * @return vector indicated by the sliders (new instance)
     */
    public Vector3f readVectorBank(String name, SliderTransform transform) {
        Validate.nonNull(name, "name");
        Validate.nonNull(transform, "transform");

        float x = readSlider("x" + name, transform);
        float y = readSlider("y" + name, transform);
        float z = readSlider("z" + name, transform);
        Vector3f vector = new Vector3f(x, y, z);

        return vector;
    }

    /**
     * Alter the label of a Nifty button.
     *
     * @param elementId Nifty element id of the button (not null)
     * @param newText new text for the label (not null)
     */
    public void setButtonLabel(String elementId, String newText) {
        Validate.nonNull(elementId, "element id");
        Validate.nonNull(newText, "text");

        Button button = getScreen().findNiftyControl(elementId, Button.class);
        try {
            button.setText(newText);
        } catch (NullPointerException exception) {
            logger.log(Level.INFO, "screen {0} lacks button {1}", new Object[]{
                MyString.quote(getScreenId()),
                MyString.quote(elementId)
            });
        }
    }

    /**
     * Alter the ticked status of the identified check box. This assumes a
     * naming convention where the Nifty id of every check box ends with
     * "CheckBox".
     *
     * @param idPrefix unique id prefix of the check box (not null)
     * @param newStatus true to tick the check box, false to un-tick it
     */
    public void setChecked(String idPrefix, boolean newStatus) {
        Validate.nonNull(idPrefix, "check box id prefix");

        CheckBox checkBox = getCheckBox(idPrefix);
        checkBox.setChecked(newStatus);
    }

    /**
     * Select a Nifty radio button.
     *
     * @param elementId Nifty element id of the radio button (not null)
     */
    public void setRadioButton(String elementId) {
        Validate.nonNull(elementId, "element id");

        RadioButton button
                = getScreen().findNiftyControl(elementId, RadioButton.class);
        try {
            button.select();
        } catch (NullPointerException exception) {
            logger.log(Level.INFO, "screen {0} lacks radio button {1}",
                    new Object[]{
                        MyString.quote(getScreenId()),
                        MyString.quote(elementId)
                    });
        }
    }

    /**
     * Enable or disable a Nifty radio button.
     *
     * @param elementId Nifty element id of the radio button (not null)
     * @param newState true to enable the button, false to disable it
     */
    public void setRadioButtonEnabled(String elementId, boolean newState) {
        Validate.nonNull(elementId, "element id");

        RadioButton button
                = getScreen().findNiftyControl(elementId, RadioButton.class);
        try {
            if (newState) {
                button.enable();
            } else {
                button.disable();
            }
        } catch (NullPointerException exception) {
            logger.log(Level.INFO, "screen {0} lacks radio button {1}",
                    new Object[]{
                        MyString.quote(getScreenId()),
                        MyString.quote(elementId)
                    });
        }
    }

    /**
     * Set the named Nifty slider based on a transformed value.
     *
     * @param name unique id prefix of the slider to set (not null)
     * @param transform how the value has been transformed (not null)
     * @param inputValue input value
     */
    public void setSlider(String name, SliderTransform transform,
            float inputValue) {
        Validate.nonNull(name, "name");
        Validate.nonNull(transform, "transform");

        Slider slider = getSlider(name);
        setSlider(slider, transform, inputValue);
    }

    /**
     * Alter the text of a Nifty element (such as a label) that has a text
     * renderer.
     *
     * @param elementId id of the element (not null)
     * @param newText (not null)
     */
    public void setStatusText(String elementId, String newText) {
        Validate.nonNull(elementId, "element id");
        Validate.nonNull(newText, "text");

        Element element = getScreen().findElementById(elementId);
        if (element == null) {
            logger.log(Level.INFO, "screen {0} lacks element {1}",
                    new Object[]{
                        MyString.quote(getScreenId()),
                        MyString.quote(elementId)
                    });
            return;
        }
        TextRenderer textRenderer = element.getRenderer(TextRenderer.class);
        if (textRenderer != null) {
            textRenderer.setText(newText);
            return;
        }
        logger.log(Level.WARNING, "Nifty element {0} lacks a text renderer",
                MyString.quote(elementId));
    }

    /**
     * Update the status label of a Nifty slider. This assumes a naming
     * convention where the label's Nifty id ends with "SliderStatus".
     *
     * @param namePrefix unique id prefix of the slider (not null)
     * @param value value of the slider
     * @param statusSuffix suffix to specify a unit of measurement (not null)
     */
    public void updateSliderStatus(String namePrefix, float value,
            String statusSuffix) {
        Validate.nonNull(namePrefix, "prefix");
        Validate.nonNull(statusSuffix, "suffix");
        /*
         * Select output precision based on the magnitude of the value.
         */
        String format;
        if (FastMath.abs(value) >= 5f) {
            format = "%.1f";
        } else if (FastMath.abs(value) >= 0.5f) {
            format = "%.2f";
        } else if (FastMath.abs(value) >= 0.05f) {
            format = "%.3f";
        } else {
            format = "%.4f";
        }
        String valueString = String.format(format, value);
        valueString = MyString.trimFloat(valueString);
        String statusText = String.format("%s = %s%s",
                namePrefix, valueString, statusSuffix);

        String statusName = namePrefix + "SliderStatus";
        setStatusText(statusName, statusText);
    }
    // *************************************************************************
    // private methods

    /**
     * Read the transformed value of a Nifty slider.
     *
     * @param slider slider to read (not null, unaffected)
     * @param transform how to transform the raw reading (not null)
     * @return transformed reading
     */
    private static float readSlider(Slider slider, SliderTransform transform) {
        Validate.nonNull(transform, "transform");

        float max = slider.getMax();
        float min = slider.getMin();
        float raw = slider.getValue();
        float reversed = min + max - raw;

        float transformed;
        switch (transform) {
            case None:
                transformed = raw;
                break;
            case Reversed:
                transformed = reversed;
                break;
            case Log10:
                transformed = FastMath.pow(10f, raw);
                break;
            case Log2:
                transformed = FastMath.pow(2f, raw);
                break;
            case ReversedLog10:
                transformed = FastMath.pow(10f, reversed);
                break;
            case ReversedLog2:
                transformed = FastMath.pow(2f, reversed);
                break;
            default:
                throw new IllegalArgumentException();
        }

        return transformed;
    }

    /**
     * Set a Nifty slider based on a transformed value.
     *
     * @param slider slider to set (not null)
     * @param transform how the value has been transformed (not null)
     * @param inputValue input value
     */
    private static void setSlider(Slider slider, SliderTransform transform,
            float inputValue) {
        Validate.nonNull(transform, "transform");

        float max = slider.getMax();
        float min = slider.getMin();

        float raw;
        switch (transform) {
            case None:
                raw = inputValue;
                break;
            case Reversed:
                raw = min + max - inputValue;
                break;
            case Log10:
                raw = FastMath.log(inputValue, 10f);
                break;
            case Log2:
                raw = FastMath.log(inputValue, 2f);
                break;
            case ReversedLog10:
                raw = min + max - FastMath.log(inputValue, 10f);
                break;
            case ReversedLog2:
                raw = min + max - FastMath.log(inputValue, 2f);
                break;
            default:
                throw new IllegalArgumentException();
        }

        slider.setValue(raw);
    }
}
