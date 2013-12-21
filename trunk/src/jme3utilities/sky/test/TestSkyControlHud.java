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
import com.jme3.math.FastMath;
import com.jme3.renderer.Camera;
import de.lessvoid.nifty.NiftyEventSubscriber;
import de.lessvoid.nifty.controls.CheckBox;
import de.lessvoid.nifty.controls.RadioButtonStateChangedEvent;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyCamera;
import jme3utilities.MyMath;
import jme3utilities.MyString;
import jme3utilities.ui.GuiScreenController;
import jme3utilities.TimeOfDay;
import jme3utilities.sky.LunarPhase;

/**
 * A simple screen controller for the heads-up display (HUD) of the
 * TestSkyControl application.
 *
 * Each time this HUD is enabled, the flyby camera is disabled so that Nifty can
 * grab mouse events. If enableFlyby is true, the flyby camera will get
 * re-enabled when this HUD is disabled.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class TestSkyControlHud
        extends GuiScreenController
        implements ActionListener {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(TestSkyControlHud.class.getName());
    // *************************************************************************
    // fields
    /**
     * if true, clouds will modulate the main light; if false, the main light
     * will be steady
     */
    private boolean cloudModulation = true;
    /**
     * if true, the flyby camera will get re-enabled each time this HUD is
     * disabled
     */
    final private boolean reenableFlyby;
    /**
     * maximum opacity for clouds (<=1, >=0)
     */
    private float cloudiness = 0f;
    /**
     * rate of motion of clouds (relative to standard rate)
     */
    private float cloudRate = 0f;
    /**
     * observer's latitude (radians north of equator, <=Pi/2, >=-Pi/2)
     */
    private float latitude = 0f;
    /**
     * angular diameter of the moon (radians, <Pi, >0)
     */
    private float lunarDiameter = 0.0092f;
    /**
     * vertical relief for terrain (Y-coordinate of peak)
     */
    private float relief = 0f;
    /**
     * sun's longitude (radians east of vernal equinox, <=2*Pi, >=0)
     */
    private float solarLongitude = 0f;
    /**
     * clock direction (<=1, >=-1)
     */
    private int clockDirection = +1;
    /**
     * phase of the moon, selected from a popup menu
     */
    private LunarPhase phase = LunarPhase.FULL;
    /**
     * the time of day: initially 4:45 a.m.
     */
    final private TimeOfDay timeOfDay = new TimeOfDay(4.75f);
    // *************************************************************************
    // constructors

    /**
     * Instantiate the display.
     *
     * @param reenableFlyby if true, the flyby camera will get re-enabled each
     * time this HUD is disabled
     */
    TestSkyControlHud(boolean reenableFlyby) {
        super("test-sky-control");
        this.reenableFlyby = reenableFlyby;
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Read the current opacity of the clouds.
     *
     * @return maximum opacity (>=0, <=1)
     */
    float getCloudiness() {
        assert cloudiness >= 0f : cloudiness;
        assert cloudiness <= 1f : cloudiness;
        return cloudiness;
    }

    /**
     * Read the current state of the cloud modulation flag.
     *
     * @return true if clouds should modulate the main light, false for steady
     * light
     */
    boolean getCloudModulation() {
        return cloudModulation;
    }

    /**
     * Read the current rate of motion of clouds.
     *
     * @return rate relative to the standard rate (negative means reverse)
     */
    float getCloudRate() {
        return cloudRate;
    }

    /**
     * Read the current solar time.
     *
     * @return hours since midnight (<24. >=0)
     */
    float getHour() {
        float hour = timeOfDay.getHour();

        assert hour >= 0f : hour;
        assert hour < 24f : hour;
        return hour;
    }

    /**
     * Read the observer's latitude.
     *
     * @return angle (radians north of equator, <=Pi/2, >=-Pi/2)
     */
    float getLatitude() {
        assert latitude >= -FastMath.HALF_PI : latitude;
        assert latitude <= FastMath.HALF_PI : latitude;
        return latitude;
    }

    /**
     * Read the angular diameter of the moon.
     *
     * @return in radians (<Pi, >0)
     */
    float getLunarDiameter() {
        return lunarDiameter;
    }

    /**
     * Read the phase of the moon.
     *
     * @return value, or null for no moon
     */
    LunarPhase getLunarPhase() {
        return phase;
    }

    /**
     * Read the current vertical relief of the terrain.
     *
     * @return Y-coordinate of peak
     */
    float getRelief() {
        return relief;
    }

    /**
     * Read the current solar longitude.
     *
     * @return angle (radians east of vernal equinox, <=2*Pi, >=0)
     */
    float getSolarLongitude() {
        return solarLongitude;
    }

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
            case "forwardRadioButton":
                clockDirection = +1;
                return;

            case "pauseRadioButton":
                clockDirection = 0;
                return;

            case "reverseRadioButton":
                clockDirection = -1;
                return;
        }
        logger.log(Level.WARNING, "unknown radio button: id={0}",
                MyString.quote(buttonId));
    }
    // *************************************************************************
    // AbstractAppState methods

    /**
     * Callback invoked when this display gets attached.
     *
     * @param stateManager (not null)
     */
    @Override
    public void stateAttached(AppStateManager stateManager) {
        super.stateAttached(stateManager);

        stateManager.attach(timeOfDay);
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

        cloudiness = updateSlider("cloudiness");

        CheckBox checkBox = getScreen().findNiftyControl("modulationCheckBox",
                CheckBox.class);
        cloudModulation = checkBox.isChecked();

        cloudRate = updateSlider("cloudRate");

        lunarDiameter = updateLogSlider("diameter", 10f);

        float solarLongitudeDegrees = updateSlider("solarLong");
        solarLongitude = solarLongitudeDegrees * FastMath.DEG_TO_RAD;

        float latitudeDegrees = updateSlider("latitude");
        latitude = latitudeDegrees * FastMath.DEG_TO_RAD;

        relief = updateSlider("relief");

        float speed = updateLogSlider("speed", 10f);
        timeOfDay.setRate(clockDirection * speed);
        /*
         * Update the labels which show status.
         */
        String timeString = timeOfDay.toString();
        setStatusText("time", timeString);

        Camera camera = getApplication().getCamera();
        float azimuthDegrees = MyCamera.azimuth(camera) * FastMath.RAD_TO_DEG;
        azimuthDegrees = MyMath.modulo(azimuthDegrees, 360f);
        String azimuthStatus = String.format("%03.0f", azimuthDegrees);
        setStatusText("azimuthStatus", azimuthStatus);

        float fovYDegrees = MyCamera.fovY(camera) * FastMath.RAD_TO_DEG;
        String fovStatus = String.format("%.0f", fovYDegrees);
        setStatusText("fovStatus", fovStatus);

        String phaseDescription;
        if (phase == null) {
            phaseDescription = "none";
        } else {
            phaseDescription = phase.describe();
        }
        setStatusText("phaseStatus", "Lunar phase: " + phaseDescription);
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

            case "phase none":
                phase = null;
                break;

            case "phase waning":
            case "phase waxing":
                showPopup(actionString + "-",
                        new String[]{"crescent", "gibbous"});
                break;

            case "phase full":
            case "phase waning-crescent":
            case "phase waning-gibbous":
            case "phase waxing-crescent":
            case "phase waxing-gibbous":
                String name = actionString.substring(6);
                phase = LunarPhase.fromDescription(name);
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
        setRadioButton("forwardRadioButton");
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
    // *************************************************************************
    // private methods

    /**
     * Display a phase-of-the-moon menu.
     */
    private void showPhaseMenu() {
        showPopup("phase ", new String[]{"full", "none", "waning", "waxing"});
    }
}