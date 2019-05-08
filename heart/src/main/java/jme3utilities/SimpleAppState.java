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
package jme3utilities;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.input.FlyByCamera;
import com.jme3.input.InputManager;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import java.util.logging.Logger;

/**
 * A NamedAppState with protected fields analogous to those of
 * {@link com.jme3.app.SimpleApplication}. If any of these fields change, notify
 * these states by invoking {@link #refreshCachedFields()}.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class SimpleAppState extends NamedAppState {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(SimpleAppState.class.getName());
    // *************************************************************************
    // fields

    /**
     * AppState manager: set by initialize()
     */
    protected AppStateManager stateManager;
    /**
     * asset manager: set by initialize()
     */
    protected AssetManager assetManager;
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
     * root node of GUI scene graph: set by initialize()
     */
    protected Node guiNode;
    /**
     * root node of main scene graph: set by initialize()
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
     * Instantiate an uninitialized state.
     *
     * @param enabled true &rarr; enabled, false &rarr; disabled
     */
    public SimpleAppState(boolean enabled) {
        super(enabled);
    }
    // *************************************************************************
    // new methods exposed

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
    // *************************************************************************
    // NamedAppState methods

    /**
     * Initialize this state during the first update after it gets attached.
     * Should be invoked only by a subclass or by the AppStateManager.
     *
     * @param sm the manager for this state (not null)
     * @param app the application which owns this state (not null)
     */
    @Override
    public void initialize(AppStateManager sm, Application app) {
        if (!(app instanceof SimpleApplication)) {
            throw new IllegalArgumentException(
                    "application should be a SimpleApplication");
        }
        super.initialize(sm, app);

        simpleApplication = (SimpleApplication) app;
        refreshCachedFields();

        assert isInitialized();
    }

    /**
     * Callback to perform rendering for this state during each frame. Should be
     * invoked only by a subclass or by the AppStateManager.
     *
     * @param rm the application's render manager (not null)
     */
    @Override
    public void render(RenderManager rm) {
        if (rm != renderManager) {
            throw new IllegalArgumentException("wrong render manager");
        }
        super.render(rm);
    }

    /**
     * Immediate callback when this state gets detached. Should be invoked only
     * by a subclass or by the AppStateManager.
     * <p>
     * Without knowing which thread invoked detatch(), it is unsafe to modify
     * the scene graph in this method. Instead, scene-graph modifications should
     * occur in {@link #cleanup()}.
     *
     * @param sm the application's state manager (not null)
     */
    @Override
    public void stateDetached(AppStateManager sm) {
        if (sm != stateManager) {
            throw new IllegalArgumentException("wrong state manager");
        }
        super.stateDetached(sm);
    }
}
