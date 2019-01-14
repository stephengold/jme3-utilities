/*
 Copyright (c) 2017-2019, Stephen Gold
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
package jme3utilities.ui;

import com.jme3.system.AppSettings;
import com.jme3.system.JmeSystem;
import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import jme3utilities.Validate;

/**
 * A simplified interface to an application's display settings.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class DisplaySettings {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(DisplaySettings.class.getName());
    // *************************************************************************
    // fields

    /**
     * application instance (not null)
     */
    final private ActionApplication application;
    /**
     * cached settings that can be applied (to the application context) or saved
     * (written to persistent storage)
     */
    final private AppSettings cachedSettings = new AppSettings(true);
    /**
     * true&rarr;settings have been applied since their last modification,
     * otherwise false
     */
    private boolean areApplied = true;
    /**
     * true&rarr;settings have been saved since their last modification,
     * otherwise false
     */
    private boolean areSaved = false;
    /**
     * true&rarr;force startup to show the settings dialog, false&rarr; show the
     * dialog only if persistent settings are missing
     */
    private boolean forceDialog = false;
    /**
     * display-size limits (not null)
     */
    final public DisplaySizeLimits sizeLimits;
    /**
     * application name for the window's title bar, which is also the key for
     * loading/saving app settings from Java's user preferences (not null)
     */
    final private String title;
    // *************************************************************************
    // constructors

    /**
     * Instantiate settings for the specified ActionApplication.
     *
     * @param app the current application instance (not null, alias created)
     * @param windowTitle (not null)
     * @param dsl display-size limits (not null)
     */
    public DisplaySettings(ActionApplication app, String windowTitle,
            DisplaySizeLimits dsl) {
        Validate.nonNull(app, "application");
        Validate.nonNull(windowTitle, "window title");
        Validate.nonNull(dsl, "display-size limits");

        application = app;
        title = windowTitle;
        sizeLimits = dsl;
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Apply the cached settings to the application context and restart the
     * context to put them into effect.
     */
    public void applyToDisplay() {
        assert canApply();
        assert !areApplied;

        AppSettings clone = new AppSettings(false);
        clone.copyFrom(cachedSettings);

        application.setSettings(clone);
        application.restart();
        areApplied = true;
    }

    /**
     * Test whether the cached settings have been applied since their last
     * modification.
     *
     * @return true if clean, otherwise false
     */
    public boolean areApplied() {
        return areApplied;
    }

    /**
     * Test whether the cached settings have been saved (written to persistent
     * storage) since their last modification.
     *
     * @return true if clean, otherwise false
     */
    public boolean areSaved() {
        return areSaved;
    }

    /**
     * Test the validity of the cached settings prior to a save.
     *
     * @return true if good enough, otherwise false
     */
    public boolean areValid() {
        int height = cachedSettings.getHeight();
        int width = cachedSettings.getWidth();
        if (!sizeLimits.isValidDisplaySize(width, height)) {
            return false;
        }

        if (cachedSettings.isFullscreen()) {
            GraphicsEnvironment environment
                    = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice device = environment.getDefaultScreenDevice();
            if (!device.isFullScreenSupported()) {
                return false;
            }

            boolean foundMatch = false;
            DisplayMode[] modes = device.getDisplayModes();
            for (DisplayMode mode : modes) {
                int bitDepth = mode.getBitDepth();
                int frequency = mode.getRefreshRate();
                // TODO see algorithm in LwjglDisplay.getFullscreenDisplayMode()
                if (bitDepth == DisplayMode.BIT_DEPTH_MULTI
                        || bitDepth == cachedSettings.getBitsPerPixel()) {
                    if (mode.getWidth() == cachedSettings.getWidth()
                            && mode.getHeight() == cachedSettings.getHeight()
                            && frequency == cachedSettings.getFrequency()) {
                        foundMatch = true;
                    }
                }
            }
            return foundMatch;

        } else { // The cached settings specify a windowed display.
            int bpp = cachedSettings.getBitsPerPixel();
            if (bpp == 24 || bpp == 32) {
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * Test whether the cached settings can be applied immediately.
     *
     * @return true if can be applied, otherwise false
     */
    public boolean canApply() {
        AppSettings current = application.getSettings();

        int currentBpp = current.getBitsPerPixel();
        boolean bppChange = currentBpp != colorDepth();

        boolean currentGamma = current.isGammaCorrection();
        boolean gammaChange = currentGamma != isGammaCorrection();

        int currentMsaa = current.getSamples();
        boolean msaaChange = currentMsaa != msaaFactor();

        boolean result;
        if (bppChange || gammaChange || msaaChange) {
            result = false; // work around JME issue #801 and related issues
        } else {
            result = areValid();
        }

        return result;
    }

    /**
     * Read the color depth.
     *
     * @return depth (in bits per pixel, &gt;0)
     */
    public int colorDepth() {
        int result = cachedSettings.getBitsPerPixel();
        assert result > 0 : result;
        return result;
    }

    /**
     * Explain why these settings cannot be applied.
     *
     * @return message text (not null)
     */
    public String feedbackApplicable() {
        AppSettings current = application.getSettings();

        int currentBpp = current.getBitsPerPixel();
        if (currentBpp != colorDepth()) {
            return "Can't apply BPP change.";
        }

        boolean currentGamma = current.isGammaCorrection();
        if (currentGamma != isGammaCorrection()) {
            return "Can't apply gamma change.";
        }

        int currentMsaa = current.getSamples();
        if (currentMsaa != msaaFactor()) {
            return "Can't apply MSAA change.";
        }

        return "";
    }

    /**
     * Explain why the cached settings are invalid.
     *
     * @return message text (not null)
     */
    public String feedbackValid() {
        int height = cachedSettings.getHeight();
        int width = cachedSettings.getWidth();
        if (!sizeLimits.isValidDisplaySize(width, height)) {
            return sizeLimits.feedbackValid(width, height);
        }

        if (cachedSettings.isFullscreen()) {
            GraphicsEnvironment environment
                    = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice device = environment.getDefaultScreenDevice();
            if (!device.isFullScreenSupported()) {
                return "Device does not support full screen.";
            }

            boolean foundMatch = false;
            DisplayMode[] modes = device.getDisplayModes();
            for (DisplayMode mode : modes) {
                int bitDepth = mode.getBitDepth();
                int frequency = mode.getRefreshRate();
                // TODO see algorithm in LwjglDisplay.getFullscreenDisplayMode()
                if (bitDepth == DisplayMode.BIT_DEPTH_MULTI
                        || bitDepth == cachedSettings.getBitsPerPixel()) {
                    if (mode.getWidth() == cachedSettings.getWidth()
                            && mode.getHeight() == cachedSettings.getHeight()
                            && frequency == cachedSettings.getFrequency()) {
                        foundMatch = true;
                    }
                }
            }
            if (!foundMatch) {
                return "No matching mode for device.";
            }

        } else { // The cached settings specify a windowed display.
            int bpp = cachedSettings.getBitsPerPixel();
            if (bpp != 24 && bpp != 32) {
                return "Window BPP must be 24 or 32.";
            }
        }

        return "";
    }

    /**
     * Read the display height.
     *
     * @return height (in pixels, &gt;0)
     */
    public int height() {
        int result = cachedSettings.getHeight();
        assert result > 0 : result;
        return result;
    }

    /**
     * Initialize the settings before the application starts.
     *
     * @return a new instance, or null if user clicked on the "Cancel" button
     */
    public AppSettings initialize() {
        /*
         * Attempt to load settings from user preferences (persistent storage).
         */
        boolean loadedFromStore = false;
        try {
            if (Preferences.userRoot().nodeExists(title)) {
                cachedSettings.load(title);
                loadedFromStore = true;
            }
        } catch (BackingStoreException e) {
        }
        /*
         * Apply overrides to the loaded settings.
         */
        applyOverrides(cachedSettings);

        if (!loadedFromStore || forceDialog) {
            /*
             * Show JME's settings dialog.
             */
            boolean loadFlag = false;
            boolean proceed
                    = JmeSystem.showSettingsDialog(cachedSettings, loadFlag);
            if (!proceed) {
                /*
                 * The user clicked on the "Cancel" button.
                 */
                return null;
            }
        }

        if (areValid()) {
            save();
        }

        AppSettings clone = new AppSettings(false);
        clone.copyFrom(cachedSettings);

        return clone;
    }

    /**
     * Test whether full-screen mode is enabled.
     *
     * @return true if full-screen, otherwise false
     */
    public boolean isFullscreen() {
        boolean result = cachedSettings.isFullscreen();
        return result;
    }

    /**
     * Test whether gamma correction is enabled.
     *
     * @return true if enabled, otherwise false
     */
    public boolean isGammaCorrection() {
        boolean result = cachedSettings.isGammaCorrection();
        return result;
    }

    /**
     * Test whether VSync is enabled.
     *
     * @return true if enabled, otherwise false
     */
    public boolean isVSync() {
        boolean result = cachedSettings.isVSync();
        return result;
    }

    /**
     * Read the sampling factor for multi-sample anti-aliasing (MSAA).
     *
     * @return sampling factor (in samples per pixel, &ge;0)
     */
    public int msaaFactor() {
        int result = cachedSettings.getSamples();
        assert result >= 0 : result;
        return result;
    }

    /**
     * Read the display's refresh rate, which is relevant only to full-screen
     * displays.
     *
     * @return frequency (in Hertz, &ge;1) or -1 for unknown
     */
    public int refreshRate() {
        int result = cachedSettings.getFrequency();
        assert result >= 1 || result == -1 : result;
        return result;
    }

    /**
     * Write the cached settings to persistent storage so they will take effect
     * the next time the application is launched.
     */
    public void save() {
        try {
            cachedSettings.save(title);
            areSaved = true;
        } catch (BackingStoreException e) {
            String message = "Display settings were not saved.";
            logger.warning(message);
        }
    }

    /**
     * Alter the color depth.
     *
     * @param newBpp color depth (in bits per pixel, &ge;1, &le;32)
     */
    public void setColorDepth(int newBpp) {
        Validate.inRange(newBpp, "new depth", 1, 32);

        int oldBpp = colorDepth();
        if (newBpp != oldBpp) {
            cachedSettings.setBitsPerPixel(newBpp);
            areApplied = false;
            areSaved = false;
        }
    }

    /**
     * Alter the display dimensions.
     *
     * @param newWidth width (in pixels, &ge;minWidth, &le;maxWidth)
     * @param newHeight height (in pixels, &ge;minHeight, &le;maxHeight)
     */
    public void setDimensions(int newWidth, int newHeight) {
        assert sizeLimits.isValidDisplaySize(newWidth, newHeight);

        int oldWidth = width();
        if (newWidth != oldWidth) {
            cachedSettings.setWidth(newWidth);
            areApplied = false;
            areSaved = false;
        }
        int oldHeight = height();
        if (newHeight != oldHeight) {
            cachedSettings.setHeight(newHeight);
            areApplied = false;
            areSaved = false;
        }
    }

    /**
     * Alter whether startup must show the settings dialog.
     *
     * @param newSetting true&rarr;force startup to show it, false&rarr; show it
     * only if persistent settings are missing
     */
    public void setForceDialog(boolean newSetting) {
        forceDialog = newSetting;
    }

    /**
     * Enable or disable full-screen mode.
     *
     * @param newSetting true&rarr;full screen, false&rarr; windowed
     */
    public void setFullscreen(boolean newSetting) {
        boolean oldSetting = isFullscreen();
        if (newSetting != oldSetting) {
            cachedSettings.setFullscreen(newSetting);
            areApplied = false;
            areSaved = false;
        }
    }

    /**
     * Enable or disable gamma-correction mode.
     *
     * @param newSetting true&rarr;enable correction, false&rarr; disable it
     */
    public void setGammaCorrection(boolean newSetting) {
        boolean oldSetting = isGammaCorrection();
        if (newSetting != oldSetting) {
            cachedSettings.setGammaCorrection(newSetting);
            areApplied = false;
            areSaved = false;
        }
    }

    /**
     * Alter the sampling factor for multi-sample anti-aliasing (MSAA).
     *
     * @param newFactor number of samples per pixel (&ge;1, &le;16)
     */
    public void setMsaaFactor(int newFactor) {
        Validate.inRange(newFactor, "new factor", 1, 16);

        int oldFactor = msaaFactor();
        if (newFactor != oldFactor) {
            cachedSettings.setSamples(newFactor);
            areApplied = false;
            areSaved = false;
        }
    }

    /**
     * Alter the refresh rate.
     *
     * @param newRate frequency (in Hertz, &gt;0)
     */
    public void setRefreshRate(int newRate) {
        Validate.positive(newRate, "new rate");

        int oldRate = refreshRate();
        if (newRate != oldRate) {
            cachedSettings.setFrequency(newRate);
            areApplied = false;
            areSaved = false;
        }
    }

    /**
     * Enable or disable VSync mode.
     *
     * @param newSetting true&rarr;synchronize, false&rarr; don't synchronize
     */
    public void setVSync(boolean newSetting) {
        boolean oldSetting = isVSync();
        if (newSetting != oldSetting) {
            cachedSettings.setVSync(newSetting);
            areApplied = false;
            areSaved = false;
        }
    }

    /**
     * Read the display width.
     *
     * @return width (in pixels, &gt;0)
     */
    public int width() {
        int result = cachedSettings.getWidth();
        assert result > 0 : result;
        return result;
    }
    // *************************************************************************
    // new protected methods

    /**
     * Apply overrides to the specified settings.
     * <p>
     * This implementation is meant to be overridden by an application-specific
     * version.
     *
     * @param settings which settings to modify (not null)
     */
    protected void applyOverrides(AppSettings settings) {
        int minHeight = sizeLimits.minHeight;
        settings.setMinHeight(minHeight);

        int minWidth = sizeLimits.minWidth;
        settings.setMinWidth(minWidth);

        settings.setResizable(false);
        settings.setTitle(title);
    }
}
