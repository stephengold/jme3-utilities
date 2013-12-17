/*
 Copyright (c) 2013, Stephen Gold
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright
 notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 notice, this list of conditions and the following disclaimer in the
 documentation and/or other materials provided with the distribution.
 * Stephen Gold's name may not be used to endorse or promote products
 derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL STEPHEN GOLD BE LIABLE FOR ANY
 DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package jme3utilities.sky.test;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AppStateManager;
import com.jme3.input.FlyByCamera;
import com.jme3.input.controls.ActionListener;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.niftygui.NiftyJmeDisplay;
import de.lessvoid.nifty.NiftyEventSubscriber;
import de.lessvoid.nifty.controls.RadioButtonStateChangedEvent;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.ui.SimpleScreenController;
import jme3utilities.sky.LunarPhase;
import jme3utilities.sky.SkyMaterial;

/**
 * A simple screen controller for the heads-up display (HUD) of the
 * TestSkyMaterial application.
 *
 * Each time the HUD is enabled, the flyby camera is disabled so that Nifty can
 * grab the mouse pointer. If enableFlyby is true, the flyby camera will get
 * re-enabled when the HUD is disabled.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class TestSkyMaterialHud
        extends SimpleScreenController
        implements ActionListener {
    // *************************************************************************
    // constants

    /**
     * object index for the moon
     */
    final static int moonIndex = 1;
    /**
     * object index for the sun
     */
    final static int sunIndex = 0;
    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(TestSkyMaterialHud.class.getName());
    // *************************************************************************
    // fields
    /**
     * if true, the flyby camera will get re-enabled each time this HUD is
     * disabled: set by constructor
     */
    final private boolean reenableFlyby;
    /**
     * the phase of the moon, selected from a popup menu
     */
    private LunarPhase phase = LunarPhase.FULL;
    /**
     * material controlled by this HUD
     */
    private SkyMaterial material = null;
    // *************************************************************************
    // constructors

    /**
     * Instantiate the display.
     *
     * @param display (not null)
     * @param reenableFlyby if true, the flyby camera will get re-enabled each
     * time this HUD is disabled
     */
    TestSkyMaterialHud(NiftyJmeDisplay display, boolean reenableFlyby) {
        super(display, "test-sky-material");
        this.reenableFlyby = reenableFlyby;
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Callback which Nifty invokes after the user selects a radio button.
     * Invoked by means of reflection, so both the class and method must be
     * public.
     *
     * @param buttonId Nifty element id of the radio button (not null)
     * @param event details of the event (not null)
     */
    @NiftyEventSubscriber(pattern = ".*RadioButton")
    public void onRadioButtonChanged(final String buttonId,
            final RadioButtonStateChangedEvent event) {
        assert buttonId != null;
        if (!screenHasStarted) {
            return;
        }

        switch (buttonId) {
            case "zenithRadioButton":
                /*
                 * Re-orient the camera toward the zenith.
                 */
                Quaternion zenith = new Quaternion();
                zenith.lookAt(Vector3f.UNIT_Y, Vector3f.UNIT_X);
                getApplication().getCamera().setRotation(zenith);
                return;

            case "northRadioButton":
                /*
                 * Re-orient the camera toward the north horizon.
                 */
                Quaternion north = new Quaternion();
                north.lookAt(Vector3f.UNIT_X, Vector3f.UNIT_Y);
                getApplication().getCamera().setRotation(north);
                return;
        }
        logger.log(Level.WARNING, "unknown radio button: id={0}",
                MyString.quote(buttonId));
    }

    /**
     * Alter the material under test.
     *
     * @param material (not null)
     */
    void setMaterial(SkyMaterial material) {
        if (material == null) {
            throw new NullPointerException("material should not be null");
        }

        this.material = material;
    }
    // *************************************************************************
    // ActionListener methods

    /**
     * Process an action from the GUI or keyboard.
     *
     * @param actionString textual description of the action (not null)
     * @param ongoing true if the action is ongoing, otherwise false
     * @param ignored
     */
    @Override
    public void onAction(String actionString, boolean ongoing, float ignored) {
        /*
         * Ignore actions which are not ongoing.
         */
        if (!ongoing) {
            return;
        }
        logger.log(Level.INFO, "Got action {0}", MyString.quote(actionString));
        switch (actionString) {
            case "phase":
                showPhaseMenu();
                break;

            case "phase full":
            case "phase waning-crescent":
            case "phase waning-gibbous":
            case "phase waxing-crescent":
            case "phase waxing-gibbous":
                String name = actionString.substring(6);
                setPhase(name);
                break;

            case "toggle":
                /*
                 * Toggle the visibility of this display.
                 */
                boolean newState = !isEnabled();
                setEnabled(newState);
                break;

            default:
                logger.log(Level.WARNING, "Action {0} was not handled.",
                        MyString.quote(actionString));
                break;
        }
    }
    // *************************************************************************
    // SimpleScreenController methods

    /**
     * Initialize and enable this display.
     *
     * @param stateManager (not null)
     * @param application (not null)
     */
    @Override
    public void initialize(AppStateManager stateManager,
            Application application) {
        assert !initialized;
        assert !isEnabled();
        super.initialize(stateManager, application);

        validateAndLoadHud();
        setRadioButton("zenithRadioButton");
        setEnabled(true);
    }

    /**
     * Enable or disable this display.
     *
     * @param newState true to enable, false to disable
     */
    @Override
    public void setEnabled(boolean newState) {
        SimpleApplication app = (SimpleApplication) getApplication();
        FlyByCamera fbc = app.getFlyByCamera();
        if (newState) {
            enable(this);
            fbc.setEnabled(false);
        } else {
            disable();
            if (reenableFlyby) {
                fbc.setEnabled(true);
            }
        }
    }

    /**
     * Callback to update this display. (Invoked once per frame.)
     *
     * @param tpf seconds since the previous update (>=0)
     */
    @Override
    public void update(float tpf) {
        if (tpf < 0f) {
            logger.log(Level.SEVERE, "tpf={0}", tpf);
            throw new IllegalArgumentException("tpf should not be negative");
        }
        super.update(tpf);

        if (!isEnabled()) {
            return;
        }

        ColorRGBA bgColor = updateColorBank("bg");
        getApplication().getViewPort().setBackgroundColor(bgColor);

        ColorRGBA clearColor = updateColorBank("clear");
        material.setClearColor(clearColor);

        ColorRGBA c0Color = updateColorBank("c0");
        material.setCloudsColor(0, c0Color);

        Vector2f c0Offset = updateUVBank("c0");
        material.setCloudsOffset(0, c0Offset.x, c0Offset.y);

        float c0Scale = updateLogSlider("c0Scale", 2f);
        material.setCloudsScale(0, c0Scale);

        ColorRGBA c1Color = updateColorBank("c1");
        material.setCloudsColor(1, c1Color);

        Vector2f c1Offset = updateUVBank("c1");
        material.setCloudsOffset(1, c1Offset.x, c1Offset.y);

        float c1Scale = updateLogSlider("c1Scale", 2f);
        material.setCloudsScale(1, c1Scale);

        ColorRGBA hazeColor = updateColorBank("haze");
        material.setHazeColor(hazeColor);

        ColorRGBA moonColor = updateColorBank("moon");
        material.setObjectColor(moonIndex, moonColor);

        Vector2f moonOffset = updateUVBank("m");
        float moonScale = updateLogSlider("mSca", 10f);
        float moonRotation = updateSlider("mRot");
        moonRotation *= FastMath.DEG_TO_RAD;
        Vector2f rotate = new Vector2f(FastMath.cos(moonRotation),
                FastMath.sin(moonRotation));
        material.setObjectTransform(moonIndex, moonOffset, moonScale, rotate);

        ColorRGBA sunColor = updateColorBank("sun");
        material.setObjectColor(sunIndex, sunColor);

        float sunScale = updateLogSlider("sunScale", 10f);
        Vector2f sunOffset = updateUVBank("sun");
        material.setObjectTransform(sunIndex, sunOffset, sunScale, null);

        setStatusText("phaseStatus", "Lunar phase: " + phase.describe());

        float percentage = 100f * material.getTransmission(moonIndex);
        String transmissionStatus = String.format("%5.1f%%", percentage);
        setStatusText("moonTransmissionStatus", transmissionStatus);

        percentage = 100f * material.getTransmission(sunIndex);
        transmissionStatus = String.format("%5.1f%%", percentage);
        setStatusText("sunTransmissionStatus", transmissionStatus);
    }
    // *************************************************************************
    // private methods

    /**
     * Alter the phase of the moon.
     *
     * @param name name of the new phase (not null)
     */
    private void setPhase(String name) {
        assert name != null;

        phase = LunarPhase.fromDescription(name);
        String assetPath = phase.imagePath();
        material.addObject(moonIndex, assetPath);
    }

    /**
     * Display a phase-of-the-moon menu.
     */
    private void showPhaseMenu() {
        String[] phaseItems = {
            "full", "waning-crescent", "waning-gibbous",
            "waxing-crescent", "waxing-gibbous"
        };
        showPopup("phase ", phaseItems);
    }

    /**
     * Update a bank of four sliders which control a color.
     *
     * @param namePrefix unique name prefix of the bank (not null)
     * @return the color indicated by the sliders (a new instance)
     */
    private ColorRGBA updateColorBank(String namePrefix) {
        assert namePrefix != null;

        float r = updateSlider(namePrefix + "R");
        float g = updateSlider(namePrefix + "G");
        float b = updateSlider(namePrefix + "B");
        float a = updateSlider(namePrefix + "A");
        ColorRGBA color = new ColorRGBA(r, g, b, a);

        return color;
    }

    /**
     * Update a bank of two sliders which control a set of texture (UV)
     * coordinates.
     *
     * @param namePrefix unique name prefix of the bank (not null)
     * @return the coordinates indicated by the sliders (a new instance)
     */
    private Vector2f updateUVBank(String namePrefix) {
        assert namePrefix != null;

        float u = updateSlider(namePrefix + "U");
        float v = updateSlider(namePrefix + "V");
        Vector2f result = new Vector2f(u, v);

        return result;
    }
}