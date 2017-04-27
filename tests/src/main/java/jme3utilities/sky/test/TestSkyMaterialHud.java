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
import com.jme3.asset.AssetNotFoundException;
import com.jme3.input.controls.ActionListener;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.post.filters.BloomFilter;
import de.lessvoid.nifty.NiftyEventSubscriber;
import de.lessvoid.nifty.controls.RadioButtonStateChangedEvent;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyCamera;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.math.MyMath;
import jme3utilities.nifty.GuiScreenController;
import jme3utilities.sky.LunarPhase;
import jme3utilities.sky.SkyMaterial;

/**
 * GUI screen controller for the heads-up display (HUD) of the TestSkyMaterial
 * application.
 * <p>
 * Each time the HUD is enabled, the flyby camera is disabled so that Nifty can
 * grab the mouse pointer. The flyby camera gets re-enabled when the HUD is
 * disabled.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class TestSkyMaterialHud
        extends GuiScreenController
        implements ActionListener {
    // *************************************************************************
    // constants and loggers

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
    final private static Logger logger = Logger.getLogger(
            TestSkyMaterialHud.class.getName());
    // *************************************************************************
    // fields

    /**
     * bloom filter controlled by this HUD: set by constructor
     */
    final private BloomFilter bloom;
    /**
     * phase of the moon, selected from a popup menu
     */
    private LunarPhase phase = LunarPhase.FULL;
    /**
     * material controlled by this HUD: set by setMaterial()
     */
    private SkyMaterial material = null;
    // *************************************************************************
    // constructors

    /**
     * Instantiate a disabled display which will be enabled during
     * initialization. Invoke setMaterial() before attaching it to the state
     * manager.
     */
    TestSkyMaterialHud(BloomFilter bloom) {
        super("test-sky-material", "Interface/Nifty/huds/test-sky-material.xml",
                true);
        this.bloom = bloom;
    }
    // *************************************************************************
    // new methods exposed

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
        Validate.nonNull(buttonId, "button id");

        if (!hasStarted()) {
            return;
        }

        switch (buttonId) {
            case "northRadioButton":
                /*
                 * Re-orient the camera toward the north horizon.
                 */
                MyCamera.look(cam, Vector3f.UNIT_X);
                return;

            case "zenithRadioButton":
                /*
                 * Re-orient the camera toward the zenith.
                 */
                MyCamera.look(cam, Vector3f.UNIT_Y);
                return;
        }
        logger.log(Level.WARNING, "unknown radio button: id={0}",
                MyString.quote(buttonId));
    }

    /**
     * Alter the material under test.
     *
     * @param newMaterial test material (not null)
     */
    final void setMaterial(SkyMaterial newMaterial) {
        assert newMaterial != null;

        material = newMaterial;
        if (!isInitialized()) {
            return;
        }
        /*
         * Adjust HUD controls to reflect the test material.
         */
        ColorRGBA clearColor = material.copyClearColor();
        setColorBank("clear", clearColor);
        ColorRGBA clearGlow = material.copyClearColor();
        setColorBank("clrGlo", clearGlow);

        int maxCloudLayers = material.getMaxCloudLayers();
        if (maxCloudLayers > 0) {
            ColorRGBA c0Color = material.copyCloudsColor(0);
            setColorBank("c0", c0Color);
            ColorRGBA c0Glow = material.copyCloudsGlow(0);
            setColorBank("c0g", c0Glow);

            Vector2f c0Offset = material.copyCloudsOffset(0);
            setUVBank("c0", c0Offset);

            float c0Scale = material.getCloudsScale(0);
            float logC0Scale = FastMath.log(c0Scale, 2f);
            setSlider("c0Scale", logC0Scale);
        }

        if (maxCloudLayers > 1) {
            ColorRGBA c1Color = material.copyCloudsColor(1);
            setColorBank("c1", c1Color);
            ColorRGBA c1Glow = material.copyCloudsGlow(1);
            setColorBank("c1g", c1Glow);

            Vector2f c1Offset = material.copyCloudsOffset(1);
            setUVBank("c1", c1Offset);

            float c1Scale = material.getCloudsScale(1);
            float logC1Scale = FastMath.log(c1Scale, 2f);
            setSlider("c1Scale", logC1Scale);
        }

        ColorRGBA hazeColor = material.copyHazeColor();
        setColorBank("haze", hazeColor);

        int maxObjects = material.getMaxObjects();
        if (maxObjects > moonIndex) {
            ColorRGBA moonColor = material.copyObjectColor(moonIndex);
            setColorBank("moon", moonColor);

            ColorRGBA moonGlow = material.copyObjectGlow(moonIndex);
            setColorBank("mGlo", moonGlow);

            Vector2f moonOffset = material.copyObjectOffset(moonIndex);
            setUVBank("m", moonOffset);

            float moonScale = material.getObjectScale(moonIndex);
            float logMoonScale = FastMath.log(moonScale, 10f);
            setSlider("mSca", logMoonScale);

            Vector2f moonRotation = material.copyObjectRotation(moonIndex);
            float radians = moonRotation.getAngle();
            float degrees = MyMath.toDegrees(radians);
            setSlider("mRot", degrees);
        }

        if (maxObjects > sunIndex) {
            ColorRGBA sunColor = material.copyObjectColor(sunIndex);
            setColorBank("sun", sunColor);

            ColorRGBA sunGlow = material.copyObjectGlow(sunIndex);
            setColorBank("sGlo", sunGlow);

            Vector2f sunOffset = material.copyObjectOffset(sunIndex);
            setUVBank("sun", sunOffset);

            float sunScale = material.getObjectScale(sunIndex);
            float logSunScale = FastMath.log(sunScale, 10f);
            setSlider("sunScale", logSunScale);
        }
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
                case "c0Texture":
                case "c1Texture":
                    showCloudsMenu(actionString + " ");
                    return;

                case "c0Texture clear":
                case "c0Texture cyclone":
                case "c0Texture fbm":
                case "c0Texture overcast":
                case "c1Texture clear":
                case "c1Texture cyclone":
                case "c1Texture fbm":
                case "c1Texture overcast":
                    String layer = actionString.substring(1, 2);
                    String name = actionString.substring(10);
                    String path = String.format("Textures/skies/clouds/%s.png",
                            name);
                    setCloudTexture(layer, path);
                    return;

                case "c0Texture t0neg0d":
                case "c1Texture t0neg0d":
                    layer = actionString.substring(1, 2);
                    setCloudTexture(layer, "Textures/skies/t0neg0d/Clouds_L.png");
                    return;

                case "phase":
                    showPhaseMenu();
                    return;

                case "phase full":
                case "phase waning-crescent":
                case "phase waning-gibbous":
                case "phase waxing-crescent":
                case "phase waxing-gibbous":
                    name = actionString.substring(6);
                    setPhase(name);
                    return;

                case "stars":
                    showStarsMenu();
                    return;

                case "stars 16m/northern":
                case "stars 16m/southern":
                case "stars 16m/wiltshire":
                case "stars none":
                case "stars northern":
                case "stars southern":
                case "stars wiltshire":
                    name = actionString.substring(6);
                    setStars(name);
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
    // BasicScreenController methods

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
    // SimpleAppState methods

    /**
     * Callback to update this display. (Invoked once per frame.)
     *
     * @param elapsedTime since the previous update (in seconds, &ge;0)
     */
    @Override
    public void update(float elapsedTime) {
        super.update(elapsedTime);

        ColorRGBA bgColor = updateColorBank("bg");
        viewPort.setBackgroundColor(bgColor);

        ColorRGBA clearColor = updateColorBank("clear");
        material.setClearColor(clearColor);
        ColorRGBA clearGlow = updateColorBank("clrGlo");
        material.setClearGlow(clearGlow);

        int maxCloudLayers = material.getMaxCloudLayers();
        if (maxCloudLayers > 0) {
            ColorRGBA c0Color = updateColorBank("c0");
            material.setCloudsColor(0, c0Color);
            ColorRGBA c0Glow = updateColorBank("c0g");
            material.setCloudsGlow(0, c0Glow);

            Vector2f c0Offset = updateUVBank("c0");
            material.setCloudsOffset(0, c0Offset.x, c0Offset.y);

            float c0Scale = updateLogSlider("c0Scale", 2f, "x");
            material.setCloudsScale(0, c0Scale);
        }

        if (maxCloudLayers > 1) {
            ColorRGBA c1Color = updateColorBank("c1");
            material.setCloudsColor(1, c1Color);
            ColorRGBA c1Glow = updateColorBank("c1g");
            material.setCloudsGlow(1, c1Glow);

            Vector2f c1Offset = updateUVBank("c1");
            material.setCloudsOffset(1, c1Offset.x, c1Offset.y);

            float c1Scale = updateLogSlider("c1Scale", 2f, "x");
            material.setCloudsScale(1, c1Scale);
        }

        ColorRGBA hazeColor = updateColorBank("haze");
        material.setHazeColor(hazeColor);

        int maxObjects = material.getMaxObjects();
        if (maxObjects > moonIndex) {
            ColorRGBA moonColor = updateColorBank("moon");
            material.setObjectColor(moonIndex, moonColor);
            ColorRGBA moonGlow = updateColorBank("mGlo");
            material.setObjectGlow(moonIndex, moonGlow);

            Vector2f moonOffset = updateUVBank("m");
            float moonScale = updateLogSlider("mSca", 10f, "x");
            float moonRotation = updateSlider("mRot", " deg");
            moonRotation = MyMath.toRadians(moonRotation);
            Vector2f rotate = new Vector2f(FastMath.cos(moonRotation),
                    FastMath.sin(moonRotation));
            material.setObjectTransform(moonIndex, moonOffset, moonScale,
                    rotate);

            float percentage = 100f * material.getTransmission(moonIndex);
            String transmissionStatus = String.format("%5.1f%%", percentage);
            setStatusText("moonTransmissionStatus", transmissionStatus);

            setStatusText("phaseStatus", "Lunar phase: " + phase.describe());
        }

        if (maxObjects > sunIndex) {
            ColorRGBA sunColor = updateColorBank("sun");
            material.setObjectColor(sunIndex, sunColor);
            ColorRGBA sunGlow = updateColorBank("sGlo");
            material.setObjectGlow(sunIndex, sunGlow);

            float sunScale = updateLogSlider("sunScale", 10f, "x");
            Vector2f sunOffset = updateUVBank("sun");
            material.setObjectTransform(sunIndex, sunOffset, sunScale, null);

            float percentage = 100f * material.getTransmission(sunIndex);
            String transmissionStatus = String.format("%5.1f%%", percentage);
            setStatusText("sunTransmissionStatus", transmissionStatus);
        }

        float bloomIntensity = updateSlider("bloomIntensity", "");
        bloom.setBloomIntensity(bloomIntensity);

        float blurScale = updateSlider("blurScale", "");
        bloom.setBlurScale(blurScale);

        float exposurePower = updateSlider("exposurePower", "");
        bloom.setExposurePower(exposurePower);
    }
    // *************************************************************************
    // private methods

    /**
     * Alter a cloud texture.
     *
     * @param indexString index of the cloud layer to alter ("0" or "1")
     * @param assetPath asset path to the new texture (not null)
     */
    private void setCloudTexture(String indexString, String assetPath) {
        assert indexString != null;
        assert assetPath != null;

        int maxCloudLayers = material.getMaxCloudLayers();
        switch (indexString) {
            case "0":
                if (maxCloudLayers > 0) {
                    material.addClouds(0, assetPath);
                }
                return;
            case "1":
                if (maxCloudLayers > 1) {
                    material.addClouds(1, assetPath);
                }
                return;
        }
        assert false : indexString;
    }

    /**
     * Alter the positions of 4 sliders that control a color.
     *
     * @param prefix unique id prefix of the bank (not null)
     * @param color values to use (unaffected)
     */
    private void setColorBank(String prefix, ColorRGBA color) {
        assert prefix != null;

        setSlider(prefix + "R", color.r);
        setSlider(prefix + "G", color.g);
        setSlider(prefix + "B", color.b);
        setSlider(prefix + "A", color.a);
    }

    /**
     * Alter the phase of the moon.
     *
     * @param name name of the new phase (not null)
     */
    private void setPhase(String name) {
        assert name != null;

        if (material.getMaxObjects() <= moonIndex) {
            return;
        }

        phase = LunarPhase.fromDescription(name);
        String imageAssetPath = phase.imagePath("");
        try {
            material.addObject(moonIndex, imageAssetPath);
        } catch (AssetNotFoundException exception) {
            imageAssetPath = phase.imagePath("-nonviral");
            material.addObject(moonIndex, imageAssetPath);
        }
    }

    /**
     * Alter or clear the star map.
     *
     * @param name name of the new star map or "none" (not null)
     */
    private void setStars(String name) {
        assert name != null;

        switch (name) {
            case "none":
                material.removeStars();
                break;

            default:
                String assetPath = String.format(
                        "Textures/skies/star-maps/%s.png", name);
                material.addStars(assetPath);
        }
    }

    /**
     * Alter the style of the sun.
     *
     * @param name name of the new style (not null)
     */
    private void setStyle(String name) {
        assert name != null;

        if (material.getMaxObjects() <= sunIndex) {
            return;
        }

        switch (name) {
            case "t0neg0d":
                material.addObject(sunIndex,
                        "Textures/skies/t0neg0d/Sun_L.png");
                break;

            default:
                String assetPath = String.format(
                        "Textures/skies/suns/%s.png", name);
                material.addObject(sunIndex, assetPath);
        }
    }

    /**
     * Alter the positions of a pair of sliders that control a set of texture
     * (UV) coordinates.
     *
     * @param prefix unique id prefix of the bank (not null)
     * @param uv values to use (unaffected)
     */
    private void setUVBank(String prefix, Vector2f uv) {
        assert prefix != null;

        setSlider(prefix + "U", uv.x);
        setSlider(prefix + "V", uv.y);
    }

    /**
     * Display a menu of cloud textures.
     *
     * @param actionPrefix common prefix of the menu's action strings (not null)
     */
    private void showCloudsMenu(String actionPrefix) {
        assert actionPrefix != null;

        if (material.getMaxCloudLayers() == 0) {
            return;
        }

        showPopupMenu(actionPrefix, new String[]{
            "clear", "cyclone", "fbm", "overcast", "t0neg0d"
        });
    }

    /**
     * Display a menu of lunar phases.
     */
    private void showPhaseMenu() {
        if (material.getMaxObjects() <= moonIndex) {
            return;
        }

        showPopupMenu("phase ", new String[]{
            "full", "waning-crescent", "waning-gibbous",
            "waxing-crescent", "waxing-gibbous"
        });
    }

    /**
     * Display a menu of star maps.
     */
    private void showStarsMenu() {
        showPopupMenu("stars ", new String[]{
            "16m/northern", "16m/southern", "16m/wiltshire", "none",
            "northern", "southern", "wiltshire"
        });
    }

    /**
     * Display a menu of sun styles.
     */
    private void showStyleMenu() {
        if (material.getMaxObjects() <= sunIndex) {
            return;
        }

        showPopupMenu("style ", new String[]{
            "chaotic", "disc", "hazy-disc", "rayed", "t0neg0d"
        });
    }

    /**
     * Update a bank of four sliders that control a color.
     *
     * @param prefix unique id prefix of the bank (not null)
     * @return color indicated by the sliders (new instance)
     */
    private ColorRGBA updateColorBank(String prefix) {
        assert prefix != null;

        float r = updateSlider(prefix + "R", "");
        float g = updateSlider(prefix + "G", "");
        float b = updateSlider(prefix + "B", "");
        float a = updateSlider(prefix + "A", "");
        ColorRGBA color = new ColorRGBA(r, g, b, a);

        return color;
    }

    /**
     * Update a bank of 2 sliders that control a set of texture (UV)
     * coordinates.
     *
     * @param prefix unique id prefix of the bank (not null)
     * @return coordinates indicated by the sliders (new vector)
     */
    private Vector2f updateUVBank(String prefix) {
        assert prefix != null;

        float u = updateSlider(prefix + "U", "");
        float v = updateSlider(prefix + "V", "");
        Vector2f result = new Vector2f(u, v);

        return result;
    }
}
