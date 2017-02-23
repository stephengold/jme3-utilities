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
package jme3utilities;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.input.FlyByCamera;
import com.jme3.input.InputManager;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An app state with protected fields analogous to those of
 * {@link com.jme3.app.SimpleApplication}. If any of these fields change, these
 * states should be notified by invoking {@link #refreshCachedFields()}.
 * <p>
 * A simple app state can influence other app states. Enabling a disabled simple
 * app state causes all the states influenced by it to get enabled. Likewise,
 * disabling an enabled simple app state disables any states it influences.
 * Influence may be mutual or one-way.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class SimpleAppState implements AppState {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            SimpleAppState.class.getName());
    // *************************************************************************
    // fields

    /**
     * app state manager: set by initialize()
     */
    protected AppStateManager stateManager;
    /**
     * asset manager: set by initialize()
     */
    protected AssetManager assetManager;
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
     * default camera for rendering: set by initialize()
     */
    protected Camera cam;
    /**
     * camera controller for demos and debugging: set by initialize()
     */
    protected FlyByCamera flyCam;
    /**
     * input manager: set by initialize()
     */
    protected InputManager inputManager;
    /**
     * app states influenced by this one (not null)
     */
    final private List<AppState> influenceList = new ArrayList<>(2);
    /**
     * generator for unique names
     */
    final private static NameGenerator nameGenerator = new NameGenerator();
    /**
     * root node of GUI scene graph: set by initialize()
     */
    protected Node guiNode;
    /**
     * root node of 3-D scene graph: set by initialize()
     */
    protected Node rootNode;
    /**
     * render manager: set by initialize()
     */
    protected RenderManager renderManager;
    /**
     * application instance: set by initialize()
     */
    protected SimpleApplication simpleApplication;
    /**
     * unique name for debugging (not null, set by constructor)
     */
    final public String appStateName;
    /**
     * viewport for GUI scene: set by initialize()
     */
    protected ViewPort guiViewPort;
    /**
     * viewport for 3-D scene: set by initialize()
     */
    protected ViewPort viewPort;
    // *************************************************************************
    // constructor

    /**
     * Instantiate an uninitialized state with a unique name.
     *
     * @param enabled true &rarr; enabled, false &rarr; disabled
     */
    public SimpleAppState(boolean enabled) {
        String className = getClass().getSimpleName();
        appStateName = nameGenerator.unique(className);
        this.enabled = enabled;
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Test whether this state influences the specified state.
     *
     * @param appState state to test
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
            logger.warning("influenced state is out-of-synch");
        }

        influenceList.add(appState);
    }

    /**
     * Update cached references to match the application.
     */
    public void refreshCachedFields() {
        stateManager = simpleApplication.getStateManager();
        assert stateManager != null;

        assetManager = simpleApplication.getAssetManager();
        assert assetManager != null;

        cam = simpleApplication.getCamera();
        assert cam != null;

        flyCam = simpleApplication.getFlyByCamera();
        assert flyCam != null;

        inputManager = simpleApplication.getInputManager();
        assert inputManager != null;

        guiNode = simpleApplication.getGuiNode();
        assert guiNode != null;

        rootNode = simpleApplication.getRootNode();
        assert rootNode != null;

        renderManager = simpleApplication.getRenderManager();
        assert renderManager != null;

        guiViewPort = simpleApplication.getGuiViewPort();
        assert guiViewPort != null;

        viewPort = simpleApplication.getViewPort();
        assert viewPort != null;
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
     * Clean up this app state on the 1st update after it gets detached.
     */
    @Override
    public void cleanup() {
        if (!isInitialized()) {
            throw new IllegalStateException("should be initialized");
        }

        initialized = false;
    }

    /**
     * Initialize this app state on the 1st update after it gets attached.
     *
     * @param sm application's state manager (not null)
     * @param app application which owns this state (not null)
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

        initialized = true;

        simpleApplication = (SimpleApplication) app;
        if (sm != simpleApplication.getStateManager()) {
            throw new IllegalArgumentException("wrong state manager");
        }
        refreshCachedFields();

        assert isInitialized();
    }

    /**
     * Test whether this app state is enabled. Declared final here to prevent
     * subclasses from overriding it.
     *
     * @return true if enabled, otherwise false
     */
    @Override
    final public boolean isEnabled() {
        return enabled;
    }

    /**
     * Test whether this app state is initialized. Declared final here to
     * prevent subclasses from overriding it.
     *
     * @return true if initialized, otherwise false
     */
    @Override
    final public boolean isInitialized() {
        return initialized;
    }

    /**
     * Callback during each render pass after all rendering is complete.
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
     * Callback to perform rendering for this state during each render pass.
     *
     * @param rm application's render manager (not null)
     */
    @Override
    public void render(RenderManager rm) {
        if (rm != renderManager) {
            throw new IllegalArgumentException("wrong render manager");
        }
        if (!isInitialized()) {
            throw new IllegalStateException("should be initialized");
        }
        if (!isEnabled()) {
            throw new IllegalStateException("should be enabled");
        }
    }

    /**
     * Enable or disable the functionality of this app state.
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
             * Exert influence over other app states.
             */
            for (AppState as : influenceList) {
                as.setEnabled(newSetting);
            }
        }
    }

    /**
     * Callback when this app state gets attached.
     *
     * @param sm application's state manager (not null)
     */
    @Override
    public void stateAttached(AppStateManager sm) {
        logger.log(Level.INFO, "attach {0}", appStateName);
        Validate.nonNull(sm, "state manager");
    }

    /**
     * Callback when this app state gets detached.
     *
     * @param sm application's state manager (not null)
     */
    @Override
    public void stateDetached(AppStateManager sm) {
        logger.log(Level.INFO, "detach {0}", appStateName);

        if (sm != stateManager) {
            throw new IllegalArgumentException("wrong state manager");
        }
    }

    /**
     * Callback to update this state prior to rendering. (Invoked once per
     * render pass.)
     *
     * @param elapsedTime time interval between render passes (in seconds,
     * &ge;0)
     */
    @Override
    public void update(float elapsedTime) {
        Validate.nonNegative(elapsedTime, "interval");
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
     * Represent this app state as a text string.
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
