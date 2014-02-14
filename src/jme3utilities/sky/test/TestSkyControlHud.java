/*
 Copyright (c) 2013-2014, Stephen Gold
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
import jme3utilities.MyString;
import jme3utilities.TimeOfDay;
import jme3utilities.math.MyMath;
import jme3utilities.sky.LunarPhase;
import jme3utilities.sky.SkyMaterial;
import jme3utilities.ui.GuiScreenController;

/**
 * A GUI screen controller for the heads-up display (HUD) of the TestSkyControl
 * application.
 * <p>
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
     * maximum opacity for clouds (&le;1, &ge;0)
     */
    private float cloudiness = 0f;
    /**
     * rate of motion of clouds (relative to standard rate)
     */
    private float cloudRate = 0f;
    /**
     * vertical offset of the clouds-only dome (fraction of dome height, &lt;1,
     * &ge;0)
     */
    private float cloudYOffset = 0f;
    /**
     * observer's latitude (radians north of equator, &le;Pi/2, &ge;-Pi/2)
     */
    private float latitude = 0f;
    /**
     * angular diameter of the moon (radians, &lt;Pi, &gt;0)
     */
    private float lunarDiameter = 0.031f;
    /**
     * vertical relief for terrain (Y-coordinate of peak)
     */
    private float relief = 0f;
    /**
     * phase angle for custom moon (radians east of the sun, &le;2*Pi, &ge;0)
     */
    private float phaseAngle = FastMath.PI;
    /**
     * angular diameter of the sun (radians, &lt;Pi, &gt;0)
     */
    private float solarDiameter = 0.031f;
    /**
     * sun's longitude (radians east of vernal equinox, &le;2*Pi, &ge;0)
     */
    private float solarLongitude = 0f;
    /**
     * vertical angle for top dome (radians from top to rim, &lt;Pi, &gt;0)
     */
    private float topVerticalAngle = FastMath.HALF_PI;
    /**
     * clock direction (&le;1, &ge;-1)
     */
    private int clockDirection = +1;
    /**
     * phase of the moon, selected from a popup menu
     */
    private LunarPhase phase = LunarPhase.FULL;
    /**
     * sun color map asset path, selected from a popup menu
     */
    private String sunAssetPath = SkyMaterial.sunMapPath;
    /**
     * the time of day: initially 4:45 a.m.
     */
    final private TimeOfDay timeOfDay = new TimeOfDay(4.75f);
    // *************************************************************************
    // constructors

    /**
     * Instantiate a disabled display which will be enabled during
     * initialization.
     */
    TestSkyControlHud() {
        super("test-sky-control", "Interface/Nifty/huds/test-sky-control.xml",
                true);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Read the current opacity of the clouds.
     *
     * @return maximum opacity (&le;1, &ge;0)
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
     * Read the current vertical offset of the clouds-only dome.
     *
     * @return offset as a fraction of dome height (&lt;1, &ge;0)
     */
    float getCloudYOffset() {
        assert cloudYOffset >= 0f : cloudYOffset;
        assert cloudYOffset < 1f : cloudYOffset;
        return cloudYOffset;
    }

    /**
     * Read the status of the "enable floor" check box.
     *
     * @return true if the box is checked, otherwise false
     */
    boolean getFloorFlag() {
        CheckBox box =
                getScreen().findNiftyControl("floorCheckBox", CheckBox.class);
        boolean result = box.isChecked();
        return result;
    }

    /**
     * Read the current solar time.
     *
     * @return hours since midnight (&lt;24, &ge;0)
     */
    float getHour() {
        float hour = timeOfDay.getHour();

        assert hour >= 0f : hour;
        assert hour < 24f : hour;
        return hour;
    }

    /**
     * Read the status of the "enable landscape" check box.
     *
     * @return true if the box is checked, otherwise false
     */
    boolean getLandscapeFlag() {
        CheckBox box = getScreen().findNiftyControl("landscapeCheckBox",
                CheckBox.class);
        boolean result = box.isChecked();
        return result;
    }

    /**
     * Read the observer's latitude.
     *
     * @return angle (radians north of equator, &le;Pi/2, &ge;-Pi/2)
     */
    float getLatitude() {
        assert latitude >= -FastMath.HALF_PI : latitude;
        assert latitude <= FastMath.HALF_PI : latitude;
        return latitude;
    }

    /**
     * Read the angular diameter of the moon.
     *
     * @return diameter (in radians, &lt;Pi, &gt;0)
     */
    float getLunarDiameter() {
        assert lunarDiameter > 0f : lunarDiameter;
        assert lunarDiameter < FastMath.PI : lunarDiameter;
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
     * Read the phase angle for custom moon.
     *
     * @return angle (radians east of the sun, &le;2*Pi, &ge;0)
     */
    float getPhaseAngle() {
        assert phaseAngle >= 0f : phaseAngle;
        assert phaseAngle <= FastMath.TWO_PI : phaseAngle;
        return phaseAngle;
    }

    /**
     * Read the status of the "enable sky" check box.
     *
     * @return true if the box is checked, otherwise false
     */
    boolean getSkyFlag() {
        CheckBox box =
                getScreen().findNiftyControl("skyCheckBox", CheckBox.class);
        boolean result = box.isChecked();
        return result;
    }

    /**
     * Read the diameter of the sun.
     *
     * @return angular diameter (in radians, &lt;Pi, &gt;0)
     */
    float getSolarDiameter() {
        assert solarDiameter > 0f : solarDiameter;
        assert solarDiameter < FastMath.PI : solarDiameter;
        return solarDiameter;
    }

    /**
     * Read the current solar longitude.
     *
     * @return angle (radians east of vernal equinox, &le;2*Pi, &ge;0)
     */
    float getSolarLongitude() {
        assert solarLongitude >= 0f : solarLongitude;
        assert solarLongitude <= FastMath.TWO_PI : solarLongitude;
        return solarLongitude;
    }

    /**
     * Read the sun's color map.
     *
     * @return asset path (or null for no sun)
     */
    String getSunStyle() {
        return sunAssetPath;
    }

    /**
     * Read the current vertical angle for the top dome.
     *
     * @return angle (radians from top to rim, &lt;Pi, &gt;0)
     */
    float getTopVerticalAngle() {
        assert topVerticalAngle > 0f : topVerticalAngle;
        assert topVerticalAngle < FastMath.PI : topVerticalAngle;
        return topVerticalAngle;
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
        if (!hasStarted()) {
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
     * @param elapsedTime since the previous update (in seconds, &ge;0)
     */
    @Override
    public void update(float elapsedTime) {
        if (!(elapsedTime >= 0f)) {
            logger.log(Level.SEVERE, "time={0}", elapsedTime);
            throw new IllegalArgumentException("time should not be negative");
        }
        super.update(elapsedTime);

        if (!isEnabled()) {
            return;
        }

        cloudiness = updateSlider("cloudiness", "");

        CheckBox checkBox = getScreen().findNiftyControl("modulationCheckBox",
                CheckBox.class);
        cloudModulation = checkBox.isChecked();

        cloudRate = updateSlider("cloudRate", "x");
        cloudYOffset = updateSlider("cloudYOffset", "");

        float lunarDiameterDegrees =
                updateLogSlider("lunarDiameter", 10f, " deg");
        lunarDiameter = lunarDiameterDegrees * FastMath.DEG_TO_RAD;

        float solarDiameterDegrees =
                updateLogSlider("solarDiameter", 10f, " deg");
        solarDiameter = solarDiameterDegrees * FastMath.DEG_TO_RAD;

        float phaseDegrees = updateSlider("customLunarPhase", " deg");
        phaseAngle = phaseDegrees * FastMath.DEG_TO_RAD;

        float solarLongitudeDegrees = updateSlider("solarLongitude", " deg");
        solarLongitude = solarLongitudeDegrees * FastMath.DEG_TO_RAD;

        float latitudeDegrees = updateSlider("latitude", " deg");
        latitude = latitudeDegrees * FastMath.DEG_TO_RAD;

        float tvaDegrees = updateSlider("topVerticalAngle", " deg");
        topVerticalAngle = tvaDegrees * FastMath.DEG_TO_RAD;

        relief = updateSlider("relief", " wu");

        float speed = updateLogSlider("speed", 10f, "x");
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

            case "phase custom":
            case "phase full":
            case "phase waning-crescent":
            case "phase waning-gibbous":
            case "phase waxing-crescent":
            case "phase waxing-gibbous":
                String name = actionString.substring(6);
                phase = LunarPhase.fromDescription(name);
                break;

            case "style":
                showStyleMenu();
                break;

            case "style chaotic":
            case "style disc":
            case "style hazy-disc":
            case "style rayed":
            case "style t0neg0d":
                name = actionString.substring(6);
                setStyle(name);
                break;

            default:
                logger.log(Level.WARNING, "Action {0} was not handled.",
                        MyString.quote(actionString));
                break;
        }
    }
    // *************************************************************************
    // GuiScreenController methods

    /**
     * Initialize and enable this display.
     *
     * @param stateManager (not null)
     * @param application (not null)
     */
    @Override
    public void initialize(AppStateManager stateManager,
            Application application) {
        if (isInitialized()) {
            throw new IllegalStateException("already initialized");
        }
        if (isEnabled()) {
            throw new IllegalStateException("shouldn't be enabled yet");
        }

        setListener(this);
        super.initialize(stateManager, application);

        setCheckBox("floorCheckBox", false);
        setCheckBox("landscapeCheckBox", true);
        setCheckBox("skyCheckBox", true);
        setRadioButton("forwardRadioButton");
    }

    /**
     * Enable or disable this display. Enabling the display disables flyCam, and
     * disabling the display re-enables flyCam.
     *
     * @param newState true to enable the display, false to disable it
     */
    @Override
    public void setEnabled(boolean newState) {
        super.setEnabled(newState);

        SimpleApplication app = getApplication();
        FlyByCamera fbc = app.getFlyByCamera();
        fbc.setEnabled(!newState);
    }
    // *************************************************************************
    // private methods

    /**
     * Alter the style of the sun.
     *
     * @param name of new style (not null)
     */
    private void setStyle(String name) {
        assert name != null;

        switch (name) {
            case "t0neg0d":
                sunAssetPath = SkyMaterial.sunMapPath;
                break;

            default:
                sunAssetPath =
                        String.format("Textures/skies/suns/%s.png", name);
        }
    }

    /**
     * Display a menu of lunar phases.
     */
    private void showPhaseMenu() {
        showPopup("phase ", new String[]{
            "custom", "full", "none", "waning", "waxing"
        });
    }

    /**
     * Display a menu of sun styles.
     */
    private void showStyleMenu() {
        showPopup("style ", new String[]{
            "chaotic", "disc", "hazy-disc", "rayed", "t0neg0d"
        });
    }
}