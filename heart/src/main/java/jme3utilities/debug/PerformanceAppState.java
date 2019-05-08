/*
 Copyright (c) 2014-2019, Stephen Gold
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
package jme3utilities.debug;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;
import java.util.logging.Logger;
import jme3utilities.MyAsset;
import jme3utilities.SimpleAppState;
import jme3utilities.Validate;

/**
 * App state that implements a latency-oriented performance monitor for
 * jMonkeyEngine3. It displays the duration of the longest update during the
 * preceding measurement interval.
 * <p>
 * Each instance is enabled at creation.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class PerformanceAppState extends SimpleAppState {
    // *************************************************************************
    // constants and loggers

    /**
     * width of the background rectangle (in pixels)
     */
    final private static float backgroundWidth = 250f;
    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(PerformanceAppState.class.getName());
    /**
     * asset path to the default font
     */
    final private static String fontPath = "Interface/Fonts/Default.fnt";
    // *************************************************************************
    // fields

    /**
     * text object to display statistics: set by initialize()
     */
    private BitmapText text = null;
    /**
     * color of background for statistics text (50% black)
     */
    final private ColorRGBA backgroundColor = new ColorRGBA(0f, 0f, 0f, 0.5f);
    /**
     * color of statistics text (white)
     */
    final private ColorRGBA textColor = new ColorRGBA(1f, 1f, 1f, 1f);
    /**
     * time remaining in the current measurement interval (in seconds)
     */
    private double secondsRemaining = 0f;
    /**
     * largest time per frame observed during the current measurement interval
     * (in seconds)
     */
    private float maxTPF = 0f;
    /**
     * minimum duration of a measurement interval (in seconds)
     */
    private float measurementInterval = 1f;
    /*
     * background for statistics text: set by initialize()
     */
    private Geometry background;
    // *************************************************************************
    // constructor

    /**
     * Instantiate a new enabled, uninitialized state.
     */
    public PerformanceAppState() {
        super(true);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Alter the measurement interval.
     *
     * @param newInterval (in seconds, &gt;0)
     */
    public void setUpdateInterval(float newInterval) {
        Validate.positive(newInterval, "new interval");
        measurementInterval = newInterval;
    }

    /**
     * Read the measurement interval.
     *
     * @return interval (in seconds, &gt;0)
     */
    public float updateInterval() {
        assert measurementInterval > 0f : measurementInterval;
        return measurementInterval;
    }
    // *************************************************************************
    // SimpleAppState methods

    /**
     * Clean up this performance monitor after it has been detached. Should be
     * invoked only by a subclass or by the AppStateManager.
     */
    @Override
    public void cleanup() {
        super.cleanup();

        guiNode.detachChild(background);
        guiNode.detachChild(text);
    }

    /**
     * Initialize this performance monitor prior to its first update. Should be
     * invoked only by a subclass or by the AppStateManager.
     *
     * @param stateManager the manager for this state (not null)
     * @param application (not null)
     */
    @Override
    public void initialize(AppStateManager stateManager,
            Application application) {
        super.initialize(stateManager, application);
        /*
         * Create and attach a GUI text object to display statistics.
         */
        BitmapFont font = assetManager.loadFont(fontPath);
        text = new BitmapText(font);
        float lineHeight = text.getLineHeight();
        text.setColor(textColor);
        text.setCullHint(Spatial.CullHint.Never);
        text.setLocalTranslation(0f, lineHeight, 0f);
        guiNode.attachChild(text);
        /*
         * Create and attach a colored background for the display.
         */
        Material backgroudMaterial
                = MyAsset.createUnshadedMaterial(assetManager);
        backgroudMaterial.setColor("Color", backgroundColor.clone());
        RenderState renderState = backgroudMaterial.getAdditionalRenderState();
        renderState.setBlendMode(RenderState.BlendMode.Alpha);
        Quad quad = new Quad(backgroundWidth, lineHeight);
        background = new Geometry("perf stats background", quad);
        background.setCullHint(Spatial.CullHint.Never);
        background.setLocalTranslation(0f, 0f, -1f);
        background.setMaterial(backgroudMaterial);
        guiNode.attachChild(background);

        reset();
    }

    /**
     * Enable or disable this performance monitor.
     *
     * @param newSetting true &rarr; enable, false &rarr; disable
     */
    @Override
    public void setEnabled(boolean newSetting) {
        boolean oldSetting = isEnabled();
        super.setEnabled(newSetting);

        if (oldSetting != newSetting) {
            Spatial.CullHint cull;
            if (newSetting) {
                reset();
                cull = Spatial.CullHint.Never;
            } else {
                cull = Spatial.CullHint.Always;
            }
            background.setCullHint(cull);
            text.setCullHint(cull);
        }
    }

    /**
     * Update the performance statistics.
     *
     * @param timePerFrame time interval between updates (in seconds, &ge;0)
     */
    @Override
    public void update(float timePerFrame) {
        super.update(timePerFrame);

        maxTPF = Math.max(maxTPF, timePerFrame);

        secondsRemaining -= timePerFrame;
        if (secondsRemaining < 0.0) {
            updateText();
            maxTPF = 0f;
            secondsRemaining = measurementInterval;
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Reset the counters and the display after enabling this performance
     * monitor.
     */
    private void reset() {
        maxTPF = 0f;
        secondsRemaining = measurementInterval;
        text.setText("(awaiting update)");
    }

    /**
     * Update the text at the end of a measurement interval.
     */
    private void updateText() {
        float milliseconds = 1000f * maxTPF;
        String message = String.format("Max time per frame = %.1f msec",
                milliseconds);
        text.setText(message);
    }
}
