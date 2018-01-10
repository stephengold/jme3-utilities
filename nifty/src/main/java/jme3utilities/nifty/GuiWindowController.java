/*
 Copyright (c) 2018, Stephen Gold
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

import com.jme3.math.Vector3f;
import java.util.logging.Logger;
import jme3utilities.Validate;

/**
 * A window controller with added support for Nifty controls such as check
 * boxes, radio buttons, sliders, and dynamic labels.
 * <p>
 * Each instance is enabled at creation, with an option for automatic disabling
 * during initialization.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class GuiWindowController extends WindowController {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(GuiWindowController.class.getName());
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized controller.
     *
     * @param screenController the controller of the screen containing the
     * window (not null, alias created)
     * @param controlId the Nifty id of the window control
     * @param startEnabled if false, disable this controller during
     * initialization; if true, leave it enabled
     */
    public GuiWindowController(GuiScreenController screenController,
            String controlId, boolean startEnabled) {
        super(screenController, controlId, startEnabled);
        assert !isInitialized();
        assert isEnabled();
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Disable the named check box. This assumes a naming convention where the
     * Nifty id of every check box ends with "CheckBox".
     *
     * @param name the name (unique id prefix) of the check box (not null)
     */
    public void disableCheckBox(String name) {
        Validate.nonNull(name, "check-box name");
        getScreenController().disableCheckBox(name);
    }

    /**
     * Read the transformed value of the named Nifty slider. This assumes a
     * naming convention where the Nifty id of every slider ends with "Slider".
     *
     * @param name the name (unique id prefix) of the slider to read (not null)
     * @param transform how to transform the raw reading (not null)
     * @return transformed reading
     */
    public float readSlider(String name, SliderTransform transform) {
        Validate.nonNull(name, "slider name");
        Validate.nonNull(transform, "transform");

        float result = getScreenController().readSlider(name, transform);
        return result;
    }

    /**
     * Read the named bank of 3 sliders to produce a vector.
     *
     * @param bankName the name (unique id infix) of the bank to read (not null)
     * @param transform how to transform the raw readings (not null)
     * @return vector indicated by the sliders (new instance)
     */
    public Vector3f readVectorBank(String bankName, SliderTransform transform) {
        Validate.nonNull(bankName, "bank name");
        Validate.nonNull(transform, "transform");

        Vector3f vector
                = getScreenController().readVectorBank(bankName, transform);
        return vector;
    }

    /**
     * Alter the text of the named Nifty button. Setting the text to "" hides
     * the button. This assumes a naming convention where the Nifty id of every
     * button ends with "Button".
     *
     * @param name unique id prefix of the button (not null)
     * @param newText new text for the label (not null)
     */
    public void setButtonText(String name, String newText) {
        Validate.nonNull(name, "button name");
        Validate.nonNull(newText, "new text");

        getScreenController().setButtonText(name, newText);
    }

    /**
     * Alter the ticked status of the named check box and enable it. This
     * assumes a naming convention where the Nifty id of every check box ends
     * with "CheckBox".
     *
     * @param name the name (unique id prefix) of the check box (not null)
     * @param newStatus true to tick the check box, false to un-tick it
     */
    public void setChecked(String name, boolean newStatus) {
        Validate.nonNull(name, "check-box name");
        getScreenController().setChecked(name, newStatus);
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

        getScreenController().setSlider(name, transform, inputValue);
    }

    /**
     * Enable or disable the named Nifty slider.
     *
     * @param name unique id prefix of the slider (not null)
     * @param newState true to enable the slider, false to disable it
     */
    public void setSliderEnabled(String name, boolean newState) {
        Validate.nonNull(name, "name");
        getScreenController().setSliderEnabled(name, newState);
    }

    /**
     * Alter the text of the identified Nifty element (such as a label) with a
     * text renderer.
     *
     * @param elementId id of the element (not null)
     * @param newText (not null)
     */
    public void setStatusText(String elementId, String newText) {
        Validate.nonNull(elementId, "element id");
        Validate.nonNull(newText, "text");

        getScreenController().setStatusText(elementId, newText);
    }

    /**
     * Update the status label of the named Nifty slider. This assumes a naming
     * convention where the label's Nifty id ends with "SliderStatus".
     *
     * @param name the name (unique id prefix) of the slider (not null)
     * @param value value of the slider
     * @param statusSuffix suffix to specify a unit of measurement (not null)
     */
    public void updateSliderStatus(String name, float value,
            String statusSuffix) {
        Validate.nonNull(name, "name");
        Validate.nonNull(statusSuffix, "status suffix");

        getScreenController().updateSliderStatus(name, value, statusSuffix);
    }
    // *************************************************************************
    // WindowController methods

    @Override
    public GuiScreenController getScreenController() {
        BasicScreenController bsc = super.getScreenController();
        GuiScreenController result = (GuiScreenController) bsc;

        assert result != null;
        return result;
    }
}
