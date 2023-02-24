/*
 Copyright (c) 2013-2023 Stephen Gold
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

import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import de.lessvoid.nifty.NiftyEventSubscriber;
import de.lessvoid.nifty.controls.Button;
import de.lessvoid.nifty.controls.CheckBox;
import de.lessvoid.nifty.controls.CheckBoxStateChangedEvent;
import de.lessvoid.nifty.controls.RadioButton;
import de.lessvoid.nifty.controls.RadioButtonStateChangedEvent;
import de.lessvoid.nifty.controls.Slider;
import de.lessvoid.nifty.controls.SliderChangedEvent;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.elements.render.TextRenderer;
import de.lessvoid.nifty.screen.Screen;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.InitialState;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.nifty.dialog.DialogController;
import jme3utilities.nifty.dialog.FloatSliderDialog;

/**
 * A PopScreenController with added support for Nifty controls such as check
 * boxes, radio buttons, sliders, tool windows, and dynamic labels.
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
    // fields

    /**
     * flag that causes this controller to temporarily ignore change events from
     * GUI controls during an update
     */
    private boolean ignoreGuiChanges = false;
    /**
     * map a checkbox name to the tool that manages the checkbox
     */
    final private Map<String, Tool> checkBoxMap = new TreeMap<>();
    /**
     * map a radio-button name to the tool that manages the radio button
     */
    final private Map<String, Tool> radioButtonMap = new TreeMap<>();
    /**
     * map a slider name to the tool that manages the slider
     */
    final private Map<String, Tool> sliderMap = new TreeMap<>();
    /**
     * map a tool name to the controller for that tool
     */
    final private Map<String, Tool> toolMap = new TreeMap<>();
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

    /**
     * Instantiate a disabled controller for the specified screen id and layout
     * asset path.
     *
     * @param screenId Nifty id (not null)
     * @param xmlAssetPath path to the Nifty XML layout asset (not null)
     * @param initialState if Enabled, enable this controller during
     * initialization; if Disabled or null, leave it disabled
     */
    public GuiScreenController(
            String screenId, String xmlAssetPath, InitialState initialState) {
        super(screenId, xmlAssetPath, initialState);
        assert !isInitialized();
        assert !isEnabled();
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Disable the named checkbox. This assumes a naming convention where the
     * Nifty id of every check box ends with "CheckBox".
     *
     * @param name the name (unique id prefix) of the checkbox (not null)
     */
    public void disableCheckBox(String name) {
        Validate.nonNull(name, "check-box name");

        CheckBox checkBox = getCheckBox(name);
        checkBox.disable();
    }

    /**
     * Find the named tool.
     *
     * @param toolName the name (unique id prefix) of the tool (not null)
     * @return the pre-existing instance, or null if none
     */
    public Tool findTool(String toolName) {
        Validate.nonNull(toolName, "tool name");
        Tool tool = toolMap.get(toolName);
        return tool;
    }

    /**
     * Access the named Nifty button. This assumes a naming convention where the
     * Nifty id of every button ends with "Button".
     *
     * @param name the name (unique id prefix) of the button (not null)
     * @return the pre-existing instance (not null)
     */
    public Button getButton(String name) {
        Validate.nonNull(name, "button name");

        Screen screen = getScreen();
        String niftyId = name + "Button";
        Button button = screen.findNiftyControl(niftyId, Button.class);
        if (button == null) {
            String qnid = MyString.quote(niftyId);
            logger.log(Level.SEVERE, "missing button {0} in {1}",
                    new Object[]{
                        qnid, MyString.quote(getScreenId())
                    });
            throw new IllegalArgumentException("missing button: " + qnid);
        }

        return button;
    }

    /**
     * Access the named Nifty check box. This assumes a naming convention where
     * the Nifty id of every check box ends with "CheckBox".
     *
     * @param name the name (unique id prefix) of the checkbox (not null)
     * @return the pre-existing instance (not null)
     */
    public CheckBox getCheckBox(String name) {
        Validate.nonNull(name, "check-box name");

        Screen screen = getScreen();
        String niftyId = name + "CheckBox";
        CheckBox box = screen.findNiftyControl(niftyId, CheckBox.class);
        if (box == null) {
            String qnid = MyString.quote(niftyId);
            logger.log(Level.SEVERE, "missing check box {0} in {1}",
                    new Object[]{
                        qnid, MyString.quote(getScreenId())
                    });
            throw new IllegalArgumentException("missing check box: " + qnid);
        }

        return box;
    }

    /**
     * Access the named Nifty slider. This assumes a naming convention where the
     * Nifty id of every slider ends with "Slider".
     *
     * @param name the name (unique id prefix) of the slider (not null)
     * @return the pre-existing instance (not null)
     */
    public Slider getSlider(String name) {
        Validate.nonNull(name, "slider name");

        Screen screen = getScreen();
        String niftyId = name + "Slider";
        Slider slider = screen.findNiftyControl(niftyId, Slider.class);
        if (slider == null) {
            String qnid = MyString.quote(niftyId);
            logger.log(Level.SEVERE, "missing slider {0} in {1}", new Object[]{
                qnid, MyString.quote(getScreenId())
            });
            throw new IllegalArgumentException("missing slider: " + qnid);
        }

        return slider;
    }

    /**
     * Test whether the named checkbox is ticked. This assumes a naming
     * convention where the Nifty id of every check box ends with "CheckBox".
     *
     * @param name the name (unique id prefix) of the checkbox (not null)
     * @return true if ticked, otherwise false
     */
    public boolean isChecked(String name) {
        Validate.nonNull(name, "check-box name");

        CheckBox checkBox = getCheckBox(name);
        boolean result = checkBox.isChecked();

        return result;
    }

    /**
     * Test whether GUI changes should be ignored.
     *
     * @return true if ignored, otherwise false
     */
    public boolean isIgnoreGuiChanges() {
        return ignoreGuiChanges;
    }

    /**
     * Associate the named checkbox with the tool that manages it.
     *
     * @param checkBoxName the name (unique id prefix) of the checkbox (not
     * null)
     * @param managingTool (not null, alias created)
     */
    public void mapCheckBox(String checkBoxName, Tool managingTool) {
        Validate.nonNull(checkBoxName, "check-box name");
        Validate.nonNull(managingTool, "managing tool");

        Tool oldMapping = checkBoxMap.put(checkBoxName, managingTool);
        assert oldMapping == null : checkBoxName;
    }

    /**
     * Associate the named radio button with the tool that manages it.
     *
     * @param radioButtonName the name (unique id prefix) of the radio button
     * (not null)
     * @param managingTool (not null, alias created)
     */
    public void mapRadioButton(String radioButtonName, Tool managingTool) {
        Validate.nonNull(radioButtonName, "radio-button name");
        Validate.nonNull(managingTool, "managing tool");

        Tool oldMapping = radioButtonMap.put(radioButtonName, managingTool);
        assert oldMapping == null : radioButtonName;
    }

    /**
     * Associate the named slider with the Tool that manages it.
     *
     * @param sliderName the name (unique id prefix) of the slider (not null)
     * @param managingTool (not null, alias created)
     */
    public void mapSlider(String sliderName, Tool managingTool) {
        Validate.nonNull(sliderName, "slider name");
        Validate.nonNull(managingTool, "managing tool");

        Tool oldMapping = sliderMap.put(sliderName, managingTool);
        assert oldMapping == null : sliderName;
    }

    /**
     * Associate the named tool with its controller.
     *
     * @param toolName the name (unique id prefix) of the tool (not null)
     * @param controller the tool's controller (not null, alias created)
     */
    public void mapTool(String toolName, Tool controller) {
        Validate.nonNull(toolName, "tool name");
        Validate.nonNull(controller, "controller");

        Tool oldMapping = toolMap.put(toolName, controller);
        if (oldMapping != null) {
            String message = "Two tools with the same name: "
                    + MyString.quote(toolName);
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Callback that Nifty invokes after a checkbox changes.
     *
     * @param checkBoxId Nifty element ID of the checkbox (not null, ends with
     * "CheckBox")
     * @param event details of the event (not null)
     */
    @NiftyEventSubscriber(pattern = ".*CheckBox")
    public void onCheckBoxChanged(
            final String checkBoxId, final CheckBoxStateChangedEvent event) {
        Validate.nonNull(checkBoxId, "check box ID");
        Validate.nonNull(event, "event");
        Validate.require(
                checkBoxId.endsWith("CheckBox"), "ID ends with CheckBox");

        if (!isIgnoreGuiChanges() && hasStarted()) {
            String checkBoxName = MyString.removeSuffix(checkBoxId, "CheckBox");
            Tool manager = findCheckBoxTool(checkBoxName);
            if (manager != null) {
                boolean isChecked = event.isChecked();
                manager.onCheckBoxChanged(checkBoxName, isChecked);
            }
        }
    }

    /**
     * Callback that Nifty invokes after a radio button changes.
     *
     * @param buttonId Nifty element id of the radio button (not null,
     * "RadioButton" suffix)
     * @param event details of the event (not null)
     */
    @NiftyEventSubscriber(pattern = ".*RadioButton")
    public void onRadioButtonChanged(
            final String buttonId, final RadioButtonStateChangedEvent event) {
        Validate.nonNull(buttonId, "button ID");
        Validate.require(buttonId.endsWith("RadioButton"), "expected suffix");
        Validate.nonNull(event, "event");

        if (!isIgnoreGuiChanges() && hasStarted()) {
            String buttonName = MyString.removeSuffix(buttonId, "RadioButton");
            Tool manager = findRadioButtonTool(buttonName);
            if (manager != null) {
                manager.onRadioButtonChanged(buttonName);
            }
        }
    }

    /**
     * Callback that Nifty invokes after a screen/tool slider changes.
     *
     * @param sliderId Nifty element ID of the slider (not null, "Slider"
     * suffix)
     * @param event details of the event (not null, ignored)
     */
    @NiftyEventSubscriber(pattern = ".*Slider")
    public void onSliderChanged(
            final String sliderId, final SliderChangedEvent event) {
        Validate.nonNull(sliderId, "slider ID");
        Validate.require(sliderId.endsWith("Slider"), "expected suffix");
        Validate.nonNull(event, "event");

        if (!isIgnoreGuiChanges() && hasStarted()) {
            String sliderName = MyString.removeSuffix(sliderId, "Slider");
            Tool manager = findSliderTool(sliderName);
            if (manager != null) {
                manager.onSliderChanged(sliderName);
            }
        }
    }

    /**
     * Callback that Nifty invokes after a dialog slider changes.
     *
     * @param sliderId Nifty element ID of the slider (not null, "#dialogslider"
     * suffix)
     * @param event details of the event (not null, ignored)
     */
    @NiftyEventSubscriber(pattern = ".*#dialogslider")
    public void onSliderChangedInDialog(
            final String sliderId, final SliderChangedEvent event) {
        Validate.nonNull(sliderId, "slider ID");
        Validate.require(sliderId.endsWith("#dialogslider"), "expected suffix");
        Validate.nonNull(event, "event");

        if (hasStarted()) {
            DialogController dc = getActiveDialog();
            if (dc instanceof FloatSliderDialog) {
                Element dialogElement = getDialogElement();
                ((FloatSliderDialog) dc).onSliderChanged(dialogElement);
            }
        }
    }

    /**
     * Read a color from the named bank of 3 sliders.
     *
     * @param name the name (unique id prefix) of the bank to read (not null)
     * @param transform how to transform the raw readings (not null)
     * @param storeResult (modified if not null)
     * @return the color indicated by the sliders (either storeResult or a new
     * instance)
     */
    public ColorRGBA readColorBank(
            String name, SliderTransform transform, ColorRGBA storeResult) {
        Validate.nonNull(name, "name");
        Validate.nonNull(transform, "transform");
        ColorRGBA color = (storeResult == null) ? new ColorRGBA() : storeResult;

        float r = readSlider(name + "R", transform);
        float g = readSlider(name + "G", transform);
        float b = readSlider(name + "B", transform);
        color.set(r, g, b, 1f);

        return color;
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

        Slider slider = getSlider(name);
        float result = readSlider(slider, transform);

        return result;
    }

    /**
     * Read a vector from the named bank of 3 sliders.
     *
     * @param name the name (unique id infix) of the bank to read (not null)
     * @param transform how to transform the raw readings (not null)
     * @param storeResult (modified if not null)
     * @return vector indicated by the sliders (either storeResult or a new
     * instance)
     */
    public Vector3f readVectorBank(
            String name, SliderTransform transform, Vector3f storeResult) {
        Validate.nonNull(name, "name");
        Validate.nonNull(transform, "transform");
        Vector3f vector = (storeResult == null) ? new Vector3f() : storeResult;

        float x = readSlider("x" + name, transform);
        float y = readSlider("y" + name, transform);
        float z = readSlider("z" + name, transform);
        vector.set(x, y, z);

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

        Button button = getButton(name);
        if (newText.isEmpty()) {
            button.getElement().hide();
        } else {
            button.setText(newText);
            button.getElement().show();
        }
    }

    /**
     * Alter the ticked status of the named checkbox and enable it. This assumes
     * a naming convention where the Nifty id of every check box ends with
     * "CheckBox".
     *
     * @param name the name (unique id prefix) of the checkbox (not null)
     * @param newStatus true to tick the checkbox, false to un-tick it
     */
    public void setChecked(String name, boolean newStatus) {
        Validate.nonNull(name, "check-box name");

        CheckBox checkBox = getCheckBox(name);
        checkBox.setChecked(newStatus);
        checkBox.enable();
    }

    /**
     * Set a bank of 3 sliders that control a color and update their status
     * labels.
     *
     * @param name unique id prefix of the bank (not null)
     * @param transform how each component has been transformed (not null)
     * @param color (not null, unaffected)
     */
    public void setColorBank(
            String name, SliderTransform transform, ColorRGBA color) {
        Validate.nonNull(name, "name");
        Validate.nonNull(transform, "transform");

        setSlider(name + "R", transform, color.r);
        updateSliderStatus(name + "R", color.r, "");

        setSlider(name + "G", transform, color.g);
        updateSliderStatus(name + "G", color.g, "");

        setSlider(name + "B", transform, color.b);
        updateSliderStatus(name + "B", color.b, "");
    }

    /**
     * Alter the "ignore GUI changes" flag.
     *
     * @param newSetting true &rarr; ignore events, false &rarr; invoke callback
     * handlers for events
     */
    public void setIgnoreGuiChanges(boolean newSetting) {
        ignoreGuiChanges = newSetting;
    }

    /**
     * Select the identified Nifty radio button.
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
            logger.log(Level.WARNING, "screen {0} lacks radio button {1}",
                    new Object[]{
                        MyString.quote(getScreenId()),
                        MyString.quote(elementId)
                    });
        }
    }

    /**
     * Enable or disable the identified Nifty radio button.
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
            logger.log(Level.WARNING, "screen {0} lacks radio button {1}",
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
    public void setSlider(
            String name, SliderTransform transform, float inputValue) {
        Validate.nonNull(name, "name");
        Validate.nonNull(transform, "transform");

        Slider slider = getSlider(name);
        setSlider(slider, transform, inputValue);
    }

    /**
     * Enable or disable the named Nifty slider.
     *
     * @param name unique id prefix of the slider (not null)
     * @param newState true to enable the slider, false to disable it
     */
    public void setSliderEnabled(String name, boolean newState) {
        Validate.nonNull(name, "name");

        Slider slider = getSlider(name);
        if (newState) {
            slider.enable();
        } else {
            slider.disable();
        }
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

        Element element = getScreen().findElementById(elementId);
        if (element == null) {
            logger.log(Level.WARNING, "screen {0} lacks element {1}",
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
     * Update the status label of the named Nifty slider. This assumes a naming
     * convention where the label's Nifty id ends with "SliderStatus".
     *
     * @param name the name (unique id prefix) of the slider (not null)
     * @param value value of the slider
     * @param statusSuffix suffix to specify a unit of measurement (not null)
     */
    public void updateSliderStatus(
            String name, float value, String statusSuffix) {
        Validate.nonNull(name, "name");
        Validate.nonNull(statusSuffix, "status suffix");

        // Select output precision based on the magnitude of the value.
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
        String statusText
                = String.format("%s = %s%s", name, valueString, statusSuffix);

        String statusName = name + "SliderStatus";
        setStatusText(statusName, statusText);
    }
    // *************************************************************************
    // protected methods

    /**
     * Find the Tool that manages the named checkbox.
     *
     * @param checkBoxName the name (unique id prefix) of the checkbox (not
     * null)
     * @return the pre-existing instance, or null if none
     */
    protected Tool findCheckBoxTool(String checkBoxName) {
        Validate.nonNull(checkBoxName, "check-box name");
        Tool managingTool = checkBoxMap.get(checkBoxName);
        return managingTool;
    }

    /**
     * Find the Tool that manages the named radio button.
     *
     * @param buttonName the name (unique id prefix) of the radio button (not
     * null)
     * @return the pre-existing instance, or null if none
     */
    protected Tool findRadioButtonTool(String buttonName) {
        Validate.nonNull(buttonName, "radio-button name");
        Tool result = radioButtonMap.get(buttonName);
        return result;
    }

    /**
     * Find the tool that manages the named slider.
     *
     * @param sliderName the name (unique id prefix) of the slider (not null)
     * @return the pre-existing instance, or null if none
     */
    protected Tool findSliderTool(String sliderName) {
        Validate.nonNull(sliderName, "slider name");
        Tool managingTool = sliderMap.get(sliderName);
        return managingTool;
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
                throw new IllegalArgumentException("transform = " + transform);
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
    private static void setSlider(
            Slider slider, SliderTransform transform, float inputValue) {
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
                throw new IllegalArgumentException("transform = " + transform);
        }

        slider.setValue(raw);
    }
}
