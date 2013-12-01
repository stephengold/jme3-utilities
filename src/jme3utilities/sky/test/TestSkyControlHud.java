// (c) Copyright 2013 Stephen Gold <sgold@sonic.net>
// Distributed under the terms of the GNU General Public License

/*
 This file is part of the JME3 Utilities Package.

 The JME3 Utilities Package is free software: you can redistribute it and/or
 modify it under the terms of the GNU General Public License as published by the
 Free Software Foundation, either version 3 of the License, or (at your
 option) any later version.

 The JME3 Utilities Package is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 for more details.

 You should have received a copy of the GNU General Public License along with
 the JME3 Utilities Package.  If not, see <http://www.gnu.org/licenses/>.
 */
package jme3utilities.sky.test;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AppStateManager;
import com.jme3.input.FlyByCamera;
import com.jme3.input.controls.ActionListener;
import com.jme3.math.FastMath;
import com.jme3.niftygui.NiftyJmeDisplay;
import com.jme3.renderer.Camera;
import de.lessvoid.nifty.controls.CheckBox;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyCamera;
import jme3utilities.MyMath;
import jme3utilities.MyString;
import jme3utilities.SimpleScreenController;
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
class TestSkyControlHud
        extends SimpleScreenController
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
     * if true, the flyby camera will get re-enabled each time this HUD is
     * disabled
     */
    final private boolean reenableFlyby;
    /**
     * if true, clouds will modulate the main light; if false, the main light
     * will be steady
     */
    private boolean cloudModulation = true;
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
     * the time of day
     */
    final private TimeOfDay timeOfDay = new TimeOfDay(4.75f);
    // *************************************************************************
    // constructors

    /**
     * Instantiate the display.
     *
     * @param display (not null)
     * @param screenId Nifty screen id of the HUD (not null)
     * @param reenableFlyby if true, the flyby camera will get re-enabled each
     * time this HUD is disabled
     */
    TestSkyControlHud(NiftyJmeDisplay display, String screenId,
            boolean reenableFlyby) {
        super(display, screenId);
        this.reenableFlyby = reenableFlyby;
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Read the current opacity of the clouds.
     *
     * @return maximum opacity (>=0, <=1)
     */
    public float getCloudiness() {
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
    public boolean getCloudModulation() {
        return cloudModulation;
    }

    /**
     * Read the current rate of motion of clouds.
     *
     * @return rate relative to the standard rate (negative means reverse)
     */
    public float getCloudRate() {
        return cloudRate;
    }

    /**
     * Read the current solar time.
     *
     * @return hours since midnight (<24. >=0)
     */
    public float getHour() {
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
    public float getLatitude() {
        assert latitude >= -FastMath.HALF_PI : latitude;
        assert latitude <= FastMath.HALF_PI : latitude;
        return latitude;
    }

    /**
     * Read the angular diameter of the moon.
     *
     * @return in radians (<Pi, >0)
     */
    public float getLunarDiameter() {
        return lunarDiameter;
    }

    /**
     * Read the phase of the moon.
     *
     * @return value, or null for no moon
     */
    public LunarPhase getLunarPhase() {
        return phase;
    }

    /**
     * Read the current vertical relief of the terrain.
     *
     * @return Y-coordinate of peak
     */
    public float getRelief() {
        return relief;
    }

    /**
     * Read the current solar longitude.
     *
     * @return angle (radians east of vernal equinox, <=2*Pi, >=0)
     */
    public float getSolarLongitude() {
        return solarLongitude;
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
     * @param tpf real seconds since the previous update (>=0)
     */
    @Override
    public void update(float tpf) {
        assert tpf >= 0f : tpf;
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

        String phaseDescription;
        if (phase == null) {
            phaseDescription = "none";
        } else {
            phaseDescription = phase.describe();
        }
        setStatusText("phaseStatus", "Lunar phase: " + phaseDescription);

        float latitudeDegrees = updateSlider("latitude");
        latitude = latitudeDegrees * FastMath.DEG_TO_RAD;

        relief = updateSlider("relief");

        float speed = updateLogSlider("speed", 10f);
        timeOfDay.setRate(clockDirection * speed);
        String timeString = timeOfDay.toString();
        setStatusText("time", timeString);

        Camera camera = application.getCamera();
        float azimuthDegrees = MyCamera.azimuth(camera) * FastMath.RAD_TO_DEG;
        azimuthDegrees = MyMath.modulo(azimuthDegrees, 360f);
        String azimuthStatus = String.format("%03.0f", azimuthDegrees);
        setStatusText("azimuthStatus", azimuthStatus);

        float fovYDegrees = MyCamera.fovY(camera) * FastMath.RAD_TO_DEG;
        String fovStatus = String.format("%.0f", fovYDegrees);
        setStatusText("fovStatus", fovStatus);
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
            case "backward":
                clockDirection = -1;
                break;

            case "direction":
                String[] actionPrefixWords = {};
                String[] items = {"forward", "stop", "backward"};
                clockDirection = 0;
                showPopup(actionPrefixWords, items);
                break;

            case "forward":
                clockDirection = +1;
                break;

            case "phase":
                String[] phasePrefixWords = {"phase"};
                String[] phaseItems = {
                    "full", "none", "waning-crescent", "waning-gibbous",
                    "waxing-crescent", "waxing-gibbous"
                };
                showPopup(phasePrefixWords, phaseItems);
                break;

            case "phase none":
                phase = null;
                break;

            case "phase full":
            case "phase waning-crescent":
            case "phase waning-gibbous":
            case "phase waxing-crescent":
            case "phase waxing-gibbous":
                String name = actionString.substring(6);
                phase = LunarPhase.fromDescription(name);
                break;

            case "stop":
                clockDirection = 0;
                break;

            case "toggle":
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
        /*
         * Read the interface description from an XML asset.
         */
        String interfaceAssetPath = String.format("Interface/Nifty/huds/%s.xml",
                screenId);
        getNifty().addXml(interfaceAssetPath);
        setEnabled(true);
    }

    /**
     * Enable or disable this display.
     *
     * @param newState true to enable, false to disable
     */
    @Override
    public void setEnabled(boolean newState) {
        FlyByCamera fbc = ((SimpleApplication) application).getFlyByCamera();
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
}