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
import com.jme3.app.state.AppStateManager;
import com.jme3.input.controls.ActionListener;
import com.jme3.math.FastMath;
import de.lessvoid.nifty.NiftyEventSubscriber;
import de.lessvoid.nifty.controls.RadioButtonStateChangedEvent;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyCamera;
import jme3utilities.MyString;
import jme3utilities.TimeOfDay;
import jme3utilities.debug.LandscapeControl;
import jme3utilities.math.MyMath;
import jme3utilities.nifty.GuiScreenController;
import jme3utilities.sky.LunarPhase;
import jme3utilities.sky.SkyControl;
import jme3utilities.sky.SunAndStars;
import jme3utilities.sky.Updater;

/**
 * GUI screen controller for the heads-up display (HUD) of the TestSkyControl
 * application.
 * <p>
 * Each time this HUD is enabled, the flyby camera is disabled so that Nifty can
 * grab mouse events. If enableFlyby is true, the flyby camera will get
 * re-enabled when this HUD is disabled.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class TestSkyControlHud
        extends GuiScreenController
        implements ActionListener {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            TestSkyControlHud.class.getName());
    // *************************************************************************
    // fields

    /**
     * if true, clouds will modulate the main light; if false, the main light
     * will be steady
     */
    private boolean cloudModulation = true;
    /**
     * multiplier for ambient light (&ge;0)
     */
    private float ambientMultiplier = 1f;
    /**
     * rate of motion of clouds (relative to standard rate)
     */
    private float cloudRate = 0f;
    /**
     * maximum opacity for clouds (&le;1, &ge;0)
     */
    private float cloudiness = 0f;
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
     * longitude difference for custom moon (radians east of the sun, &le;2*Pi,
     * &ge;0)
     */
    private float longitudeDifference = FastMath.PI;
    /**
     * angular diameter of the moon (radians, &lt;Pi, &gt;0)
     */
    private float lunarDiameter = 0.031f;
    /**
     * celestial latitude for custom moon (radians north of the ecliptic,
     * &le;Pi/2, &ge;-Pi/2)
     */
    private float lunarLatitude = 0f;
    /**
     * multiplier for main light (&ge;0)
     */
    private float mainMultiplier = 1f;
    /**
     * vertical relief for terrain (Y-coordinate of peak, &gt;0)
     */
    private float relief = 50f;
    /**
     * angular diameter of the sun (radians, &lt;Pi, &gt;0)
     */
    private float solarDiameter = 0.031f;
    /**
     * sun's longitude (radians east of the March equinox, &le;2*Pi, &ge;0)
     */
    private float solarLongitude = 0f;
    /**
     * vertical angle for top dome (radians from top to rim, &lt;Pi, &gt;0)
     */
    private float topVerticalAngle = FastMath.HALF_PI;
    /**
     * clock direction (&le;1, &ge;-1, 0 &rarr; paused)
     */
    private int clockDirection = +1;
    /**
     * phase of the moon, selected from a popup menu
     */
    private LunarPhase phase = LunarPhase.FULL;
    /**
     * star map name, selected from a popup menu
     */
    private String starMapName = "16m";
    /**
     * sun color map asset path, selected from a popup menu
     */
    private String sunAssetPath = "Textures/skies/suns/hazy-disc.png";
    /**
     * scene management app state (set by initialize)
     */
    private TestSkyControlRun run;
    /**
     * the time of day: initially 4:45 a.m.
     */
    final private TimeOfDay timeOfDay = new TimeOfDay(4.75f);
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized, disabled display that will be enabled
     * during initialization.
     */
    TestSkyControlHud() {
        super("test-sky-control", "Interface/Nifty/huds/test-sky-control.xml",
                true);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Read the status of the "enable ambient" check box.
     *
     * @return true if the box is checked, otherwise false
     */
    boolean getAmbientFlag() {
        boolean result = isChecked("ambient");
        return result;
    }

    /**
     * Read the ambient light multiplier.
     */
    float getAmbientMultiplier() {
        return ambientMultiplier;
    }

    /**
     * Read the status of the "enable bloom" check box.
     *
     * @return true if the box is checked, otherwise false
     */
    boolean getBloomFlag() {
        boolean result = isChecked("bloom");
        return result;
    }

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
        boolean result = isChecked("floor");
        return result;
    }

    /**
     * Read the current solar time.
     *
     * @return hours since midnight (&lt;24, &ge;0)
     */
    float getHour() {
        float hour = timeOfDay.hour();

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
        boolean result = isChecked("landscape");
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
     * Read the celestial latitude for a custom moon.
     *
     * @return radians north of the ecliptic (&le;Pi/2, &ge;-Pi/2)
     */
    float getLunarLatitude() {
        assert lunarLatitude >= -FastMath.HALF_PI : lunarLatitude;
        assert lunarLatitude <= FastMath.HALF_PI : lunarLatitude;
        return lunarLatitude;
    }

    /**
     * Read the longitude difference for a custom moon.
     *
     * @return radians east of the sun (&le;2*Pi, &ge;0)
     */
    float getLongitudeDifference() {
        assert longitudeDifference >= 0f : longitudeDifference;
        assert longitudeDifference <= FastMath.TWO_PI : longitudeDifference;
        return longitudeDifference;
    }

    /**
     * Read the phase of the moon.
     *
     * @return preset value, or null for hidden moon
     */
    LunarPhase getLunarPhase() {
        return phase;
    }

    /**
     * Read the status of the "enable main light" check box.
     *
     * @return true if the box is checked, otherwise false
     */
    boolean getMainLightFlag() {
        boolean result = isChecked("mainLight");
        return result;
    }

    /**
     * Read the main light multiplier.
     */
    float getMainMultiplier() {
        return mainMultiplier;
    }

    /**
     * Read the current vertical relief of the terrain.
     *
     * @return Y-coordinate of peak (&gt;0)
     */
    float getRelief() {
        assert relief > 0f : relief;
        return relief;
    }

    /**
     * Read the status of the "enable shadow filters" check box.
     *
     * @return true if the box is checked, otherwise false
     */
    boolean getShadowFiltersFlag() {
        boolean result = isChecked("shadowFilters");
        return result;
    }

    /**
     * Read the status of the "enable sky" check box.
     *
     * @return true if the box is checked, otherwise false
     */
    boolean getSkyFlag() {
        boolean result = isChecked("sky");
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
     * @return radians east of March equinox (&le;2*Pi, &ge;0)
     */
    float getSolarLongitude() {
        assert solarLongitude >= 0f : solarLongitude;
        assert solarLongitude <= FastMath.TWO_PI : solarLongitude;
        return solarLongitude;
    }

    /**
     * Read the star map name.
     *
     * @return name (not null)
     */
    String getStarMapName() {
        assert starMapName != null;
        return starMapName;
    }

    /**
     * Read the sun's color map.
     *
     * @return asset path
     */
    String getSunStyle() {
        return sunAssetPath;
    }

    /**
     * Read the current vertical angle for the top dome.
     *
     * @return radians from top to rim (&lt;Pi, &gt;0)
     */
    float getTopVerticalAngle() {
        assert topVerticalAngle > 0f : topVerticalAngle;
        assert topVerticalAngle < FastMath.PI : topVerticalAngle;
        return topVerticalAngle;
    }

    /**
     * Alter the HUD controls to match the specified scene controls.
     *
     * @param sky sky control (not null)
     * @param land landscape control (not null)
     */
    void matchScene(SkyControl sky, LandscapeControl land) {
        SunAndStars sunAndStars = sky.getSunAndStars();
        Updater updater = sky.getUpdater();

        ambientMultiplier = updater.getAmbientMultiplier();
        setSlider("ambient", ambientMultiplier);

        cloudModulation = sky.getCloudModulation();
        setCheckBox("modulationCheckBox", cloudModulation);
        cloudRate = sky.getCloudsRate();
        setSlider("cloudRate", cloudRate);
        cloudYOffset = sky.getCloudsYOffset();
        setSlider("cloudYOffset", cloudYOffset);
        cloudiness = sky.getCloudLayer(0).getOpacity();
        setSlider("cloudiness", cloudiness);

        latitude = sunAndStars.getObserverLatitude();
        float degrees = MyMath.toDegrees(latitude);
        setSlider("latitude", degrees);

        lunarDiameter = sky.lunarDiameter();
        degrees = MyMath.toDegrees(lunarDiameter);
        float logDegrees = FastMath.log(degrees, 10f);
        setSlider("lunarDiameter", logDegrees);

        lunarLatitude = sky.getLunarLatitude();
        degrees = MyMath.toDegrees(lunarLatitude);
        setSlider("lunarLatitude", degrees);

        mainMultiplier = updater.getMainMultiplier();
        setSlider("main", mainMultiplier);

        phase = sky.getPhase();
        longitudeDifference = sky.getLongitudeDifference();
        degrees = MyMath.toDegrees(longitudeDifference);
        setSlider("longitudeDifference", degrees);

        relief = land.peakY();
        setSlider("relief", relief);

        solarDiameter = sky.solarDiameter();
        degrees = MyMath.toDegrees(solarDiameter);
        logDegrees = FastMath.log(degrees, 10f);
        setSlider("solarDiameter", logDegrees);

        solarLongitude = sunAndStars.getSolarLongitude();
        degrees = MyMath.toDegrees(solarLongitude);
        setSlider("solarLongitude", degrees);

        float hour = sunAndStars.getHour();
        timeOfDay.setHour(hour);

        topVerticalAngle = sky.getTopVerticalAngle();
        degrees = MyMath.toDegrees(topVerticalAngle);
        setSlider("topVerticalAngle", degrees);
    }

    /**
     * Callback that Nifty invokes after the user selects a radio button.
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
    // ActionListener methods

    /**
     * Process an action from the GUI.
     *
     * @param actionString textual description of the action (not null)
     * @param ongoing true if the action is ongoing, otherwise false
     * @param tpf time interval between render passes (in seconds, &ge;0)
     */
    @Override
    public void onAction(String actionString, boolean ongoing, float tpf) {
        logger.log(Level.INFO, "Got action {0}", MyString.quote(actionString));

        if (ongoing) {
            switch (actionString) {
                case "look moon":
                    run.lookAtTheMoon();
                    return;

                case "look sun":
                    run.lookAtTheSun();
                    return;

                case "phase":
                    showPhaseMenu();
                    return;

                case "phase none":
                    phase = null;
                    return;

                case "phase waning":
                case "phase waxing":
                    showPopup(actionString + "-",
                            new String[]{"crescent", "gibbous"});
                    return;

                case "phase custom":
                case "phase full":
                case "phase waning-crescent":
                case "phase waning-gibbous":
                case "phase waxing-crescent":
                case "phase waxing-gibbous":
                    String name = actionString.substring(6);
                    phase = LunarPhase.fromDescription(name);
                    return;

                case "star-map":
                    showStarMapMenu();
                    return;

                case "star-map 4m":
                case "star-map 16m":
                case "star-map nebula":
                    name = actionString.substring(9);
                    setStarMapName(name);
                    return;

                case "style":
                    showStyleMenu();
                    return;

                case "style chaotic":
                case "style disc":
                case "style hazy-disc":
                case "style rayed":
                case "style t0neg0d":
                    name = actionString.substring(6);
                    setStyle(name);
                    return;
            }
        }
        /*
         * The action is not handled: forward it to the application class.
         */
        guiApplication.onAction(actionString, ongoing, tpf);
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
        if (isEnabled()) {
            throw new IllegalStateException("shouldn't be enabled yet");
        }

        setListener(this);
        super.initialize(stateManager, application);

        run = TestSkyControl.run;
        assert run != null;
        /*
         * Initialize check boxes and radio buttons.
         */
        setCheckBox("ambientCheckBox", true);
        setCheckBox("bloomCheckBox", true);
        setCheckBox("floorCheckBox", false);
        setCheckBox("landscapeCheckBox", true);
        setCheckBox("mainLightCheckBox", true);
        setCheckBox("shadowFiltersCheckBox", true);
        setCheckBox("skyCheckBox", true);
        setRadioButton("forwardRadioButton");
    }
    // *************************************************************************
    // SimpleAppState methods

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
     * @param elapsedTime time interval between render passes (in seconds,
     * &ge;0)
     */
    @Override
    public void update(float elapsedTime) {
        super.update(elapsedTime);

        if (!isEnabled()) {
            return;
        }

        cloudiness = updateSlider("cloudiness", "");
        cloudModulation = isChecked("modulation");
        mainMultiplier = updateSlider("main", "x");
        ambientMultiplier = updateSlider("ambient", "x");
        cloudRate = updateSlider("cloudRate", "x");
        cloudYOffset = updateSlider("cloudYOffset", "");

        float lunarDiameterDegrees = updateLogSlider(
                "lunarDiameter", 10f, " deg");
        lunarDiameter = MyMath.toRadians(lunarDiameterDegrees);

        float solarDiameterDegrees = updateLogSlider(
                "solarDiameter", 10f, " deg");
        solarDiameter = MyMath.toRadians(solarDiameterDegrees);

        float longitudeDifferenceDegrees = updateSlider(
                "longitudeDifference", " deg");
        longitudeDifference = MyMath.toRadians(longitudeDifferenceDegrees);
        float lunarLatitudeDegrees = updateSlider("lunarLatitude", " deg");
        lunarLatitude = MyMath.toRadians(lunarLatitudeDegrees);

        float solarLongitudeDegrees = updateSlider("solarLongitude", " deg");
        solarLongitude = MyMath.toRadians(solarLongitudeDegrees);

        float latitudeDegrees = updateSlider("latitude", " deg");
        latitude = MyMath.toRadians(latitudeDegrees);

        float tvaDegrees = updateSlider("topVerticalAngle", " deg");
        topVerticalAngle = MyMath.toRadians(tvaDegrees);

        relief = updateSlider("relief", " wu");

        float speed = updateLogSlider("speed", 10f, "x");
        timeOfDay.setRate(clockDirection * speed);
        /*
         * Update the labels which show status.
         */
        String timeString = timeOfDay.toString();
        setStatusText("time", timeString);

        float azimuth = MyCamera.azimuth(cam);
        float azimuthDegrees = MyMath.toDegrees(azimuth);
        azimuthDegrees = MyMath.modulo(azimuthDegrees, 360f);
        String azimuthStatus = String.format("%03.0f", azimuthDegrees);
        setStatusText("azimuthStatus", azimuthStatus);

        float fovY = MyCamera.fovY(cam);
        float fovYDegrees = MyMath.toDegrees(fovY);
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
    // private methods

    /**
     * Alter the star map name.
     *
     * @param name name of desired star map (not null)
     */
    private void setStarMapName(String name) {
        assert name != null;
        starMapName = name;
    }

    /**
     * Alter the style of the sun.
     *
     * @param name name of new style (not null)
     */
    private void setStyle(String name) {
        assert name != null;

        switch (name) {
            case "t0neg0d":
                sunAssetPath = "Textures/skies/t0neg0d/Sun_L.png";
                break;

            default:
                sunAssetPath = String.format(
                        "Textures/skies/suns/%s.png", name);
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
     * Display a menu of star maps.
     */
    private void showStarMapMenu() {
        showPopup("star-map ", new String[]{
            "16m", "4m", "nebula"
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
