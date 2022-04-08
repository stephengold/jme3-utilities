/*
 Copyright (c) 2020-2022, Stephen Gold
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
package jme3utilities.nifty.test;

import com.jme3.system.AppSettings;
import java.util.logging.Logger;
import jme3utilities.math.RectSizeLimits;
import jme3utilities.ui.ActionApplication;
import jme3utilities.ui.DisplaySettings;

/**
 * A simplified interface to a Nifty application's display settings.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class TbsDisplaySettings extends DisplaySettings {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(TbsDisplaySettings.class.getName());
    // *************************************************************************
    // constructors

    /**
     * Instantiate settings for the specified ActionApplication.
     *
     * @param app the current application instance (not null, alias created)
     * @param windowTitle (not null)
     * @param sizeLimits display-size limits (not null)
     */
    public TbsDisplaySettings(ActionApplication app, String windowTitle,
            RectSizeLimits sizeLimits) {
        super(app, windowTitle, sizeLimits);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Test whether the cached settings can be applied immediately.
     *
     * @return true if they can be applied, otherwise false
     */
    @Override
    public boolean canApply() {
        boolean result = super.canApply();
        if (result) {
            AppSettings current = getApplication().getSettings();

            boolean currentFull = current.isFullscreen();
            boolean goFull = !currentFull && isFullscreen();

            int currentHeight = current.getHeight();
            boolean heightChange = currentHeight != height();

            int currentWidth = current.getWidth();
            boolean widthChange = currentWidth != width();

            if (goFull || heightChange || widthChange) {
                result = false;
            }
        }

        return result;
    }

    /**
     * Explain why these settings cannot be applied.
     *
     * @return message text (not null)
     */
    @Override
    public String feedbackApplicable() {
        String result = super.feedbackApplicable();
        if (result.isEmpty()) {
            AppSettings current = getApplication().getSettings();

            boolean currentFull = current.isFullscreen();
            boolean goFull = !currentFull && isFullscreen();
            if (goFull) {
                return "Can't go from windowed to fullscreen.";
            }

            int currentHeight = current.getHeight();
            boolean heightChange = currentHeight != height();
            if (heightChange) {
                return "Can't apply height change.";
            }

            int currentWidth = current.getWidth();
            boolean widthChange = currentWidth != width();
            if (widthChange) {
                return "Can't apply width change.";
            }
        }

        return result;
    }
}
