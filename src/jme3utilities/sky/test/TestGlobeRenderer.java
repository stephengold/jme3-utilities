/*
 Copyright (c) 2014-2017, Stephen Gold
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

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.ScreenshotAppState;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.FastMath;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Quad;
import com.jme3.system.AppSettings;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Misc;
import jme3utilities.MyAsset;
import jme3utilities.MyString;
import jme3utilities.sky.GlobeRenderer;
import jme3utilities.sky.LunarPhase;

/**
 * Simple application to test the GlobeRenderer class. The application's main
 * entry point is here.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class TestGlobeRenderer
        extends SimpleApplication
        implements ActionListener {
    // *************************************************************************
    // constants

    /**
     * number of samples around the globe's middle
     */
    final private static int equatorSamples = 12;
    /**
     * number of samples from pole to pole
     */
    final private static int meridianSamples = 24;
    /**
     * size of the dynamic texture (pixels per side)
     */
    final private static int moonRendererResolution = 512;
    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(TestGlobeRenderer.class.getName());
    /**
     * application name for the title bar and usage message
     */
    final private static String applicationName = "TestGlobeRenderer";
    // *************************************************************************
    // fields
    /**
     * true means just display the usage message; false means run the
     * application
     */
    @Parameter(names = {"-h", "-u", "--help", "--usage"}, help = true,
            description = "display this usage message")
    private static boolean usageOnly = false;
    /**
     * globe renderer for the moon: set in simpleInitApp()
     */
    private GlobeRenderer moonRenderer = null;
    /**
     * current phase of the moon: set in simpleInitApp()
     */
    private LunarPhase phase = null;
    /**
     * material with dynamic color map
     */
    private Material dynamicMaterial = null;
    /**
     * material with color map loaded from asset
     */
    private Material loadedMaterial = null;
    /**
     * name of initial phase (default is "full")
     */
    @Parameter(names = {"-p", "--phase"}, description = "specify initial phase")
    private static String phaseName = "full";
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the TestGlobeRenderer application.
     *
     * @param arguments array of command-line arguments (not null)
     */
    public static void main(String[] arguments) {
        /*
         * Mute the chatty loggers found in some imported packages.
         */
        Misc.setLoggingLevels(Level.WARNING);
        /*
         * Set the logging level for this class.
         */
        logger.setLevel(Level.INFO);
        Logger.getLogger("com.jme3.audio.openal.ALAudioRenderer")
                .setLevel(Level.SEVERE);
        /*
         * Instantiate the application.
         */
        TestGlobeRenderer application = new TestGlobeRenderer();
        /*
         * Parse the command-line arguments.
         */
        JCommander jCommander = new JCommander(application, arguments);
        jCommander.setProgramName(applicationName);
        if (usageOnly) {
            jCommander.usage();
            return;
        }
        /*
         * Don't pause on lost focus.  This simplifies debugging and
         * permits the application to keep running while minimized.
         */
        application.setPauseOnLostFocus(false);
        /*
         * Customize the window's resolution and title bar.
         */
        AppSettings settings = new AppSettings(true);
        settings.setResolution(640, 480);
        settings.setTitle(applicationName);
        application.setSettings(settings);
        /*
         * Skip the "Display Settings" dialog during startup.
         */
        application.setShowSettings(false);

        application.start();
        /*
         * ... and onward to TestGlobeRenderer.simpleInitApp()!
         */
    }
    // *************************************************************************
    // ActionListener methods

    /**
     * Process a keystroke or mouse click.
     *
     * @param actionString textual description of the action (not null)
     * @param ongoing true if the action is ongoing, otherwise false
     * @param ignored time per frame (in seconds)
     */
    @Override
    public void onAction(String actionString, boolean ongoing, float ignored) {
        if (ongoing && "next".equals(actionString)) {
            /*
             * switch to the next phase
             */
            int ordinal = phase.ordinal() + 1;
            if (ordinal == LunarPhase.CUSTOM.ordinal()) {
                ++ordinal;
            }
            if (ordinal >= LunarPhase.values().length) {
                ordinal = 0;
            }
            phase = LunarPhase.values()[ordinal];
            updateScene();
        }
    }
    // *************************************************************************
    // SimpleApplication methods

    /**
     * Initialize this application.
     */
    @Override
    public void simpleInitApp() {
        /*
         * Log the jME3-utilities version string.
         */
        logger.log(Level.INFO, "jME3-utilities version is {0}",
                MyString.quote(Misc.getVersionShort()));

        flyCam.setEnabled(false);
        initializeUserInterface();
        /*
         * Add a globe renderer for the moon.
         */
        Texture moonTexture = MyAsset.loadTexture(assetManager,
                "Textures/skies/moon/clementine.png");
        Material moonMaterial = MyAsset.createShadedMaterial(assetManager,
                moonTexture);
        moonRenderer = new GlobeRenderer(moonMaterial,
                Image.Format.Luminance8Alpha8, equatorSamples, meridianSamples,
                moonRendererResolution);
        stateManager.attach(moonRenderer);
        /*
         * Create an unshaded material for each texture.
         */
        Texture dynamicTexture = moonRenderer.getTexture();
        dynamicMaterial =
                MyAsset.createUnshadedMaterial(assetManager, dynamicTexture);
        RenderState additional = dynamicMaterial.getAdditionalRenderState();
        additional.setBlendMode(RenderState.BlendMode.Alpha);
        additional.setDepthWrite(false);

        loadedMaterial = MyAsset.createUnshadedMaterial(assetManager);
        additional = loadedMaterial.getAdditionalRenderState();
        additional.setBlendMode(RenderState.BlendMode.Alpha);
        additional.setDepthWrite(false);
        /*
         * Add twin quads to the scene and apply a different material to each.
         */
        float quadSize = 3f; // world units
        boolean flip = true;
        Quad quad = new Quad(quadSize, quadSize, flip);

        Geometry left = new Geometry("left", quad);
        rootNode.attachChild(left);
        float offset = quadSize / 2f;
        left.setLocalTranslation(-2f - offset, -offset, 0f);
        left.setMaterial(dynamicMaterial);

        Geometry right = new Geometry("right", quad);
        rootNode.attachChild(right);
        right.setLocalTranslation(2f - offset, -offset, 0f);
        right.setMaterial(loadedMaterial);

        phase = LunarPhase.fromDescription(phaseName);
        updateScene();
    }
    // *************************************************************************
    // private methods

    /**
     * Initialize the user interface.
     */
    private void initializeUserInterface() {
        /*
         * Capture a screenshot each time the KEY_SYSRQ hotkey is pressed.
         */
        ScreenshotAppState screenShotState = new ScreenshotAppState();
        boolean success = stateManager.attach(screenShotState);
        assert success;
        /*
         * Disable jME3 stat view.
         */
        setDisplayStatView(false);
        /*
         * Press spacebar or left-click to advance to the next phase.
         */
        inputManager.addMapping("next", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("next",
                new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addListener(this, "next");
    }

    /**
     * Update the scene after changing the phase.
     */
    private void updateScene() {
        float phaseAngle = phase.longitudeDifference();
        float intensity = 2f + FastMath.abs(phaseAngle - FastMath.PI);
        moonRenderer.setLightIntensity(intensity);
        moonRenderer.setPhase(phaseAngle);
        /*
         * Load the corresponding static texture.
         */
        String loadPath = phase.imagePath();
        Texture loadedTexture = MyAsset.loadTexture(assetManager, loadPath);
        loadedMaterial.setTexture("ColorMap", loadedTexture);
    }
}
