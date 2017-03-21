/*
 Copyright (c) 2017, Stephen Gold
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
package jme3utilities.debug.test;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.input.controls.ActionListener;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Spatial;
import de.lessvoid.nifty.NiftyEventSubscriber;
import de.lessvoid.nifty.controls.CheckBox;
import de.lessvoid.nifty.controls.RadioButtonStateChangedEvent;
import de.lessvoid.nifty.screen.Screen;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MySpatial;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.debug.SkeletonDebugControl;
import jme3utilities.nifty.GuiScreenController;
import static jme3utilities.nifty.GuiScreenController.showPopup;

/**
 * Controller for the HUD for the PoseDemo application.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class PoseDemoHud
        extends GuiScreenController
        implements ActionListener {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            PoseDemoHud.class.getName());
    /**
     * names of models which can be loaded
     */
    final private static String[] modelArray = {
        "Elephant", "Jaime", "Ninja", "Oto", "Sinbad"
    };
    /**
     * action prefix for the "select model" popup menu
     */
    final private static String selectMenuPrefix = "select model ";
    // *************************************************************************
    // fields

    /**
     * cached reference to the model's skeleton debug control
     */
    private SkeletonDebugControl debugControl = null;
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized, disabled display that will be enabled
     * during initialization.
     */
    public PoseDemoHud() {
        super("pose-demo", "Interface/Nifty/huds/pose.xml", true);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Callback which Nifty invokes after the user selects a radio button.
     *
     * @param buttonId Nifty element id of the radio button (not null)
     * @param event details of the event (not null)
     */
    @NiftyEventSubscriber(pattern = ".*RadioButton")
    public void onRadioButtonChanged(final String buttonId,
            final RadioButtonStateChangedEvent event) {
        Validate.nonNull(buttonId, "button id");
        Validate.nonNull(event, "event");

        if (!hasStarted()) {
            return;
        }

        switch (buttonId) {
            case "flyingRadioButton":
                PoseDemo.cameraState.setOrbitMode(false);
                return;

            case "orbitingRadioButton":
                PoseDemo.cameraState.setOrbitMode(true);
                return;
        }
        logger.log(Level.WARNING, "unknown radio button: id={0}",
                MyString.quote(buttonId));
    }
    // *************************************************************************
    // ActionListener methods

    /**
     * Process a GUI action.
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
                case "select model":
                    showPopup(selectMenuPrefix, modelArray);
                    return;
            }
            if (actionString.startsWith(selectMenuPrefix)) {
                String modelName = actionString.substring(
                        selectMenuPrefix.length());
                loadModel(modelName);
                return;
            }
        }
        /*
         * Forward any unhandled actions to the application.
         */
        guiApplication.onAction(actionString, ongoing, tpf);
    }
    // *************************************************************************
    // GuiScreenController methods

    /**
     * Initialize this controller prior to its 1st update.
     *
     * @param stateManager (not null)
     * @param application application which owns this screen (not null)
     */
    @Override
    public void initialize(AppStateManager stateManager,
            Application application) {
        if (isEnabled()) {
            throw new IllegalStateException("shouldn't be enabled yet");
        }

        setListener(this);
        super.initialize(stateManager, application);
        /*
         * Initialize check boxes and radio buttons.
         */
        setCheckBox("3DCursorCheckBox", true);
        setCheckBox("axesCheckBox", true);
        setCheckBox("skeletonDebugCheckBox", true);
    }

    /**
     * Callback to update this state prior to rendering. (Invoked once per
     * render pass.)
     *
     * @param tpf time interval between render passes (in seconds, &ge;0)
     */
    @Override
    public void update(float tpf) {
        if (PoseDemo.cameraState.isOrbitMode()) {
            setRadioButton("orbitingRadioButton");
        } else {
            setRadioButton("flyingRadioButton");
        }

        Screen screen = getScreen();
        CheckBox box = screen.findNiftyControl("3DCursorCheckBox",
                CheckBox.class);
        boolean enable = box.isChecked();
        PoseDemo.cameraState.cursorSetEnabled(enable);

        box = screen.findNiftyControl("axesCheckBox", CheckBox.class);
        enable = box.isChecked();
        PoseDemo.axes.setEnabled(enable);

        if (debugControl != null) {
            box = screen.findNiftyControl("skeletonDebugCheckBox",
                    CheckBox.class);
            enable = box.isChecked();
            debugControl.setEnabled(enable);
        }

        box = screen.findNiftyControl("shadowsCheckBox", CheckBox.class);
        enable = box.isChecked();
        PoseDemo.dlsf.setEnabled(enable);
    }

    // *************************************************************************
    // private methods
    /**
     * Unload the current model, if any, and load a new one.
     *
     * @param modelName name of mode (not null)
     */
    private void loadModel(String modelName) {
        assert modelName != null;

        String assetPath;
        switch (modelName) {
            case "Jaime":
                assetPath = "Models/Jaime/Jaime.j3o";
                break;

            case "Elephant":
            case "Ninja":
            case "Oto":
            case "Sinbad":
                assetPath = String.format("Models/%s/%s.mesh.xml",
                        modelName, modelName);
                break;

            default:
                logger.log(Level.SEVERE, "Unknown model {0}",
                        MyString.quote(modelName));
                return;

        }
        Spatial model = assetManager.loadModel(assetPath);

        Spatial oldModel = MySpatial.findChild(rootNode, "model");
        if (oldModel != null) {
            rootNode.detachChild(oldModel);
        }
        rootNode.attachChild(model);
        model.setName("model");
        model.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        setStatusText("modelStatus", modelName);
        /*
         * Make the model one world unit tall, with its base
         * resting on the XZ plane.
         */
        float maxY = MySpatial.getMaxY(model);
        float minY = MySpatial.getMinY(model);
        assert maxY > minY : maxY;
        float worldScale = 1f / (maxY - minY);
        MySpatial.setWorldScale(model, worldScale);
        Vector3f worldLocation = new Vector3f(0f, -minY * worldScale, 0f);
        MySpatial.setWorldLocation(model, worldLocation);
        /*
         * Add a skeleton debug control to the model.
         */
        debugControl = new SkeletonDebugControl(assetManager);
        model.addControl(debugControl);
    }
}
