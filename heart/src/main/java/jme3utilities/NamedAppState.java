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
package jme3utilities;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.renderer.RenderManager;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An AppState with a unique name, able to influence other AppStates.
 * <p>
 * Enabling a disabled state causes all the states influenced by it to get
 * enabled. Likewise, disabling an enabled state disables any states it
 * influences. Influence may be mutual or one-way.
 *
 * @author Stephen Gold sgold@sonic.net
 * @see com.jme3.app.state.AbstractAppState
 */
public class NamedAppState implements AppState {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(NamedAppState.class.getName());
    // *************************************************************************
    // fields

    /**
     * true &rarr; enabled, false &rarr; disabled (set by constructor and
     * {@link #setEnabled(boolean)})
     */
    private boolean enabled;
    /**
     * true &rarr; initialized, false &rarr; uninitialized
     */
    private boolean initialized = false;
    /**
     * AppStates influenced by this one (not null)
     */
    final private List<AppState> influenceList = new ArrayList<>(2);
    /**
     * generator for unique names
     */
    final private static NameGenerator nameGenerator = new NameGenerator();
    /**
     * unique name for debugging (not null, set by constructor)
     */
    final public String appStateName;
    // *************************************************************************
    // constructor

    /**
     * Instantiate an uninitialized state with a unique name.
     *
     * @param enabled true &rarr; enabled, false &rarr; disabled
     */
    public NamedAppState(boolean enabled) {
        String className = getClass().getSimpleName();
        appStateName = nameGenerator.unique(className);
        this.enabled = enabled;
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Test whether this state influences the specified state.
     *
     * @param appState the state to test
     * @return true if influenced, false if not influenced
     */
    public boolean hasInfluenceOver(AppState appState) {
        boolean result = influenceList.contains(appState);
        return result;
    }

    /**
     * Cause this state to influence the specified state.
     *
     * @param appState (not null, not this)
     */
    final public void influence(AppState appState) {
        Validate.nonNull(appState, "app state");
        if (appState == this) {
            throw new IllegalArgumentException("self-influence not allowed");
        }
        if (appState.isEnabled() != isEnabled()) {
            logger.log(Level.WARNING,
                    "inflenced state {0} out-of-synch with {1}", new Object[]{
                        appState, this
                    });
        }

        influenceList.add(appState);
    }

    /**
     * Remove any influence this state has over the specified state.
     *
     * @param appState which state to stop influencing
     */
    public void stopInfluencing(AppState appState) {
        influenceList.remove(appState);
    }
    // *************************************************************************
    // AppState methods

    /**
     * Clean up this state during the first update after it gets detached.
     * Should be invoked only by a subclass or by the AppStateManager.
     */
    @Override
    public void cleanup() {
        if (!isInitialized()) {
            throw new IllegalStateException("should be initialized");
        }

        initialized = false;
    }

    /**
     * Initialize this state during the first update after it gets attached.
     * Should be invoked only by a subclass or by the AppStateManager.
     *
     * @param sm the application's state manager (not null)
     * @param app the application which owns this state (not null)
     */
    @Override
    public void initialize(AppStateManager sm, Application app) {
        logger.log(Level.INFO, "initialize {0}", appStateName);
        if (isInitialized()) {
            throw new IllegalStateException("already initialized");
        }
        Validate.nonNull(sm, "state manager");
        if (!(app instanceof SimpleApplication)) {
            throw new IllegalArgumentException(
                    "application should be a SimpleApplication");
        }
        if (sm != app.getStateManager()) {
            throw new IllegalArgumentException("wrong state manager");
        }

        initialized = true;
    }

    /**
     * Test whether this state is enabled. Declared final here to prevent
     * subclasses from overriding it.
     *
     * @return true if enabled, otherwise false
     */
    @Override
    final public boolean isEnabled() {
        return enabled;
    }

    /**
     * Test whether this state is initialized. Declared final here to prevent
     * subclasses from overriding it.
     *
     * @return true if initialized, otherwise false
     */
    @Override
    final public boolean isInitialized() {
        return initialized;
    }

    /**
     * Callback during each frame after all rendering is complete.
     */
    @Override
    public void postRender() {
        if (!isInitialized()) {
            throw new IllegalStateException("should be initialized");
        }
        if (!isEnabled()) {
            throw new IllegalStateException("should be enabled");
        }
    }

    /**
     * Callback to perform rendering for this state during each frame.
     *
     * @param rm the application's render manager (not null)
     */
    @Override
    public void render(RenderManager rm) {
        Validate.nonNull(rm, "render manager");
        if (!isInitialized()) {
            throw new IllegalStateException("should be initialized");
        }
        if (!isEnabled()) {
            throw new IllegalStateException("should be enabled");
        }
    }

    /**
     * Enable or disable the functionality of this state.
     *
     * @param newSetting true &rarr; enable, false &rarr; disable
     */
    @Override
    public void setEnabled(boolean newSetting) {
        boolean oldSetting = isEnabled();
        enabled = newSetting;

        if (oldSetting != newSetting) {
            if (newSetting) {
                logger.log(Level.INFO, "enable {0}", appStateName);
            } else {
                logger.log(Level.INFO, "disable {0}", appStateName);

            }
            /*
             * Exert influence over other AppStates.
             */
            for (AppState as : influenceList) {
                as.setEnabled(newSetting);
            }
        }
    }

    /**
     * Callback when this state gets attached.
     *
     * @param sm the application's state manager (not null)
     */
    @Override
    public void stateAttached(AppStateManager sm) {
        logger.log(Level.INFO, "attach {0}", appStateName);
        Validate.nonNull(sm, "state manager");
    }

    /**
     * Callback when this state gets detached.
     *
     * @param sm the application's state manager (not null)
     */
    @Override
    public void stateDetached(AppStateManager sm) {
        logger.log(Level.INFO, "detach {0}", appStateName);
        Validate.nonNull(sm, "state manager");
    }

    /**
     * Callback to update this state prior to rendering. (Invoked once per frame
     * while the state is attached and enabled.)
     *
     * @param elapsedTime the time interval between frames (in seconds, &ge;0)
     */
    @Override
    public void update(float elapsedTime) {
        Validate.nonNegative(elapsedTime, "elapsed time");
        if (!isInitialized()) {
            throw new IllegalStateException("should be initialized");
        }
        if (!isEnabled()) {
            throw new IllegalStateException("should be enabled");
        }
    }
    // *************************************************************************
    // Object methods

    /**
     * Represent this state as a text string.
     *
     * @return descriptive string of text (not null)
     */
    @Override
    public String toString() {
        String result = String.format("%s (%sinitialized, %senabled)",
                appStateName, initialized ? "" : "un", enabled ? "" : "not ");
        return result;
    }
}
