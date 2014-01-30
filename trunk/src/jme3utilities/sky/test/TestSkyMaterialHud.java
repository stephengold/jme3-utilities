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
import com.jme3.app.state.AppStateManager;
import com.jme3.input.controls.ActionListener;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import de.lessvoid.nifty.NiftyEventSubscriber;
import de.lessvoid.nifty.controls.RadioButtonStateChangedEvent;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.ui.GuiScreenController;
import jme3utilities.sky.LunarPhase;
import jme3utilities.sky.SkyMaterial;

/**
 * A GUI screen controller for the heads-up display (HUD) of the TestSkyMaterial
 * application.
 *
 * Each time the HUD is enabled, the flyby camera is disabled so that Nifty can
 * grab the mouse pointer. The flyby camera gets re-enabled when the HUD is
 * disabled.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class TestSkyMaterialHud
        extends GuiScreenController
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
     * phase of the moon, selected from a popup menu
     */
    private LunarPhase phase = LunarPhase.FULL;
    /**
     * material controlled by this HUD: set by constructor
     */
    private SkyMaterial material = null;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a disabled display which will be enabled during
     * initialization. Invoke setMaterial() before attaching it to the state
     * manager.
     */
    TestSkyMaterialHud() {
        super("test-sky-material", "Interface/Nifty/huds/test-sky-material.xml",
                true);
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
        if (!hasStarted()) {
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
     * @param newMaterial (not null)
     */
    final void setMaterial(SkyMaterial newMaterial) {
        assert newMaterial != null;

        this.material = newMaterial;
    }
    // *************************************************************************
    // AbstractAppState methods

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
    // ActionListener methods

    /**
     * Process an action from the GUI.
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
            case "c0Texture":
            case "c1Texture":
                showCloudsMenu(actionString + " ");
                break;

            case "c0Texture clear":
            case "c0Texture cyclone":
            case "c0Texture overcast":
            case "c1Texture clear":
            case "c1Texture cyclone":
            case "c1Texture overcast":
                String layer = actionString.substring(1, 2);
                String name = actionString.substring(10);
                String path = String.format("Textures/skies/clouds/%s.png",
                        name);
                setCloudTexture(layer, path);
                break;

            case "c0Texture t0neg0d":
            case "c1Texture t0neg0d":
                layer = actionString.substring(1, 2);
                setCloudTexture(layer, "Textures/skies/t0neg0d/Clouds_L.png");
                break;

            case "phase":
                showPhaseMenu();
                break;

            case "phase full":
            case "phase waning-crescent":
            case "phase waning-gibbous":
            case "phase waxing-crescent":
            case "phase waxing-gibbous":
                name = actionString.substring(6);
                setPhase(name);
                break;

            case "style":
                showStyleMenu();
                break;

            case "style disc":
            case "style rayed":
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
        setRadioButton("zenithRadioButton");
    }
    // *************************************************************************
    // private methods

    /**
     * Alter a cloud texture.
     *
     * @param indexString which cloud layer ("0" or "1")
     * @param assetPath to the new texture (not null)
     */
    private void setCloudTexture(String indexString, String assetPath) {
        assert indexString != null;
        assert assetPath != null;

        switch (indexString) {
            case "0":
                material.addClouds(0, assetPath);
                return;
            case "1":
                material.addClouds(1, assetPath);
                return;
        }
        assert false : indexString;
    }

    /**
     * Alter the phase of the moon.
     *
     * @param name name of the new phase (not null)
     */
    private void setPhase(String name) {
        assert name != null;

        phase = LunarPhase.fromDescription(name);
        String imageAssetPath = phase.imagePath();
        material.addObject(moonIndex, imageAssetPath);
    }

    /**
     * Alter the style of the sun.
     *
     * @param name name of the new style (not null)
     */
    private void setStyle(String name) {
        assert name != null;

        switch (name) {
            case "disc":
                material.addObject(sunIndex, "Textures/skies/sun/disc.png");
                return;

            case "rayed":
                material.addObject(sunIndex, SkyMaterial.sunMapPath);
        }
    }

    /**
     * Display a cloud texture menu.
     *
     * @param actionPrefix common prefix of the menu's action strings (not null)
     */
    private void showCloudsMenu(String actionPrefix) {
        assert actionPrefix != null;

        showPopup(actionPrefix, new String[]{
            "clear", "overcast", "cyclone", "t0neg0d"
        });
    }

    /**
     * Display a phase-of-the-moon menu.
     */
    private void showPhaseMenu() {
        showPopup("phase ", new String[]{
            "full", "waning-crescent", "waning-gibbous",
            "waxing-crescent", "waxing-gibbous"
        });
    }

    /**
     * Display a style menu for the sun.
     */
    private void showStyleMenu() {
        showPopup("style ", new String[]{"disc", "rayed"});
    }

    /**
     * Update a bank of four sliders which control a color.
     *
     * @param prefix unique id prefix of the bank (not null)
     * @return the color indicated by the sliders (a new instance)
     */
    private ColorRGBA updateColorBank(String prefix) {
        assert prefix != null;

        float r = updateSlider(prefix + "R");
        float g = updateSlider(prefix + "G");
        float b = updateSlider(prefix + "B");
        float a = updateSlider(prefix + "A");
        ColorRGBA color = new ColorRGBA(r, g, b, a);

        return color;
    }

    /**
     * Update a bank of two sliders which control a set of texture (UV)
     * coordinates.
     *
     * @param prefix unique id prefix of the bank (not null)
     * @return the coordinates indicated by the sliders (a new instance)
     */
    private Vector2f updateUVBank(String prefix) {
        assert prefix != null;

        float u = updateSlider(prefix + "U");
        float v = updateSlider(prefix + "V");
        Vector2f result = new Vector2f(u, v);

        return result;
    }
}