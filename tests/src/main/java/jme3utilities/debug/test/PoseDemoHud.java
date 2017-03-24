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

import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.Animation;
import com.jme3.animation.Bone;
import com.jme3.animation.BoneTrack;
import com.jme3.animation.LoopMode;
import com.jme3.animation.Skeleton;
import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.input.controls.ActionListener;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Spatial;
import de.lessvoid.nifty.NiftyEventSubscriber;
import de.lessvoid.nifty.controls.CheckBox;
import de.lessvoid.nifty.controls.RadioButtonStateChangedEvent;
import de.lessvoid.nifty.controls.Slider;
import de.lessvoid.nifty.screen.Screen;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyAnimation;
import jme3utilities.MySkeleton;
import jme3utilities.MySpatial;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.debug.SkeletonDebugControl;
import jme3utilities.nifty.GuiScreenController;

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
     * names of the coordinate axes
     */
    final private static String[] axisNames = {"x", "y", "z"};
    /**
     * names of loadable models in the jme3-testdata library
     * <p>
     * Jaime excluded because its skeleton is backward.
     */
    final private static String[] modelNames = {
        "Elephant", "Ninja", "Oto", "Sinbad"
    };
    /**
     * action prefix for the "select animation" popup menu
     */
    final private static String animationMenuPrefix = "select animation ";
    /**
     * dummy animation name used in menus and statuses (but never stored in
     * channel!) to indicate bind pose, that is, no animation selected
     */
    final private static String bindPoseName = "( bind pose )";
    /**
     * action prefix for the "select bone" popup menu
     */
    final private static String boneMenuPrefix = "select bone ";
    /**
     * action prefix for the "select model" popup menu
     */
    final private static String modelMenuPrefix = "select model ";
    /**
     * dummy bone name used in menus and statuses to indicate no bone selection
     * (not null)
     */
    final private static String noBone = "( no bone )";
    // *************************************************************************
    // fields

    /**
     * animation channel (Can be null only if no model selected.
     * getAnimationName() == null means bind pose is selected.)
     */
    private AnimChannel channel = null;
    /**
     * When true, all three bone-angle sliders should be enabled, along with the
     * user-control flags of all bones in the model's skeleton.
     */
    private boolean boneAngleSlidersEnabledFlag = false;
    /**
     * inverse local rotation of each bone in original bind pose, indexed by
     * boneIndex
     */
    final private List<Quaternion> inverseBindPose = new ArrayList<>(30);
    /**
     * reference to the model's skeleton debug control
     */
    private SkeletonDebugControl debugControl = null;
    /**
     * name of the bone which is selected, or noBone (not null)
     */
    private String selectedBoneName = noBone;
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
     * Access the loaded model.
     *
     * @return the pre-existing instance, or null if not yet loaded
     */
    Spatial getModel() {
        Spatial model = MySpatial.findChild(rootNode, "model");
        return model;
    }

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
                case "select animation":
                    showPopup(animationMenuPrefix, listAnimationNames());
                    return;

                case "select bone":
                    showPopup(boneMenuPrefix, listBoneNames());
                    return;

                case "select model":
                    showPopup(modelMenuPrefix, modelNames);
                    return;
            }
            if (actionString.startsWith(animationMenuPrefix)) {
                int namePos = animationMenuPrefix.length();
                String name = actionString.substring(namePos);
                selectAnimation(name);
                return;

            } else if (actionString.startsWith(boneMenuPrefix)) {
                int namePos = boneMenuPrefix.length();
                String name = actionString.substring(namePos);
                selectBone(name);
                return;

            } else if (actionString.startsWith(modelMenuPrefix)) {
                int namePos = modelMenuPrefix.length();
                String name = actionString.substring(namePos);
                loadModel(name);
                return;
            }
        }
        /*
         * Forward unhandled action to the application.
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
         * Initialize check boxes and labels.
         */
        setCheckBox("3DCursorCheckBox", true);
        setCheckBox("axesCheckBox", true);
        setCheckBox("skeletonDebugCheckBox", true);

        loadModel("Elephant");
    }

    /**
     * Callback to update this state prior to rendering. (Invoked once per
     * render pass.)
     *
     * @param tpf time interval between render passes (in seconds, &ge;0)
     */
    @Override
    public void update(float tpf) {
        /*
         * Camera mode
         */
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
        /*
         * Animation and bone
         */
        String status;
        if (findTrack() != null) {
            status = String.format("+ %s", selectedBoneName); // with a track
        } else {
            status = selectedBoneName;
        }
        setStatusText("boneStatus", status);
        /*
         * AxesControl
         */
        box = screen.findNiftyControl("axesCheckBox", CheckBox.class);
        enable = box.isChecked();
        PoseDemo.axes.setEnabled(enable);
        float lineWidth = updateSlider("aLineWidth", " pix");
        PoseDemo.axes.setLineWidth(lineWidth);
        float length = updateSlider("aLength", " wu");
        PoseDemo.axes.setAxisLength(length);
        /*
         * SkeletonDebugControl
         */
        box = screen.findNiftyControl("skeletonDebugCheckBox", CheckBox.class);
        enable = box.isChecked();
        debugControl.setEnabled(enable);
        ColorRGBA wireColor = updateColorBank("wire");
        debugControl.setColor(wireColor);
        lineWidth = updateSlider("sLineWidth", " pix");
        debugControl.setLineWidth(lineWidth);
        float pointSize = updateSlider("pointSize", " pix");
        if (debugControl.supportsPointSize()) {
            debugControl.setPointSize(pointSize);
        }
        /*
         * Lighting options
         */
        box = screen.findNiftyControl("shadowsCheckBox", CheckBox.class);
        enable = box.isChecked();
        PoseDemo.dlsf.setEnabled(enable);
    }
    // *************************************************************************
    // private methods

    /**
     * Disable the 3 bone-angle sliders (and user control).
     */
    private void disableBoneAngleSliders() {
        logger.log(Level.INFO, "Disable the bone-angle sliders.");
        /*
         * Disable the sliders.
         */
        for (String axisName : axisNames) {
            Slider slider = getSlider(axisName);
            slider.disable();
        }
        /*
         * Give control of the skeleton back to the animations.
         */
        Spatial model = getModel();
        MySkeleton.setUserControl(model, false);
        /*
         * Update the status.
         */
        boneAngleSlidersEnabledFlag = false;
    }

    /**
     * Enable the 3 bone-angle sliders (and user control).
     */
    private void enableBoneAngleSliders() {
        logger.log(Level.INFO, "Enable the bone-angle sliders.");
        /*
         * Enable the sliders.
         */
        for (String axisName : axisNames) {
            Slider slider = getSlider(axisName);
            slider.enable();
        }
        /*
         * Take control of the skeleton away from any animations.
         */
        Spatial model = getModel();
        MySkeleton.setUserControl(model, true);
        /*
         * Update the status.
         */
        boneAngleSlidersEnabledFlag = true;
    }

    /**
     * Find a named BoneTrack in the selected animation.
     *
     * @parm boneName the name of a bone (not null or noBone)
     * @return a pre-existing instance, or null if no track is selected
     */
    private BoneTrack findTrack() {
        if (!isBoneSelected()) {
            return null;
        }
        if (channel == null) {
            return null;
        }
        if (isBindPoseSelected()) {
            return null;
        }

        Spatial model = getModel();
        Animation animation = getAnimation();
        int boneIndex = MySkeleton.findBoneIndex(model, selectedBoneName);
        BoneTrack track = MyAnimation.findTrack(animation, boneIndex);

        return track;
    }

    /**
     * Access the selected animation.
     *
     * @return a pre-existing instance, or null if none or in bind pose
     */
    private Animation getAnimation() {
        if (isBindPoseSelected()) {
            return null;
        }
        AnimControl animControl = getAnimControl();
        if (animControl == null) {
            return null;
        }
        String animationName = channel.getAnimationName();
        Animation animation = animControl.getAnim(animationName);

        return animation;
    }

    /**
     * Access the AnimControl of the loaded model.
     *
     * @return the pre-existing instance, or null if none
     */
    private AnimControl getAnimControl() {
        Spatial model = getModel();
        AnimControl animControl = model.getControl(AnimControl.class);
        if (animControl == null) {
            throw new IllegalArgumentException(
                    "expected the model to have an AnimControl");
        }

        return animControl;
    }

    /**
     * Access the skeleton of the loaded model.
     *
     * @return the pre-existing instance (not null)
     */
    private Skeleton getSkeleton() {
        Spatial model = getModel();
        assert model != null;
        Skeleton skeleton = MySkeleton.getSkeleton(model);

        assert skeleton != null;
        return skeleton;
    }

    /**
     * Test whether an animation is running.
     *
     * @return true if an animation is running, false otherwise
     */
    private boolean isAnimationRunning() {
        if (!isBindPoseSelected() && channel.getSpeed() != 0f) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Test whether the bind pose is selected.
     *
     * @return true if it's selected, false if an animation is selected or if no
     * model is loaded
     */
    private boolean isBindPoseSelected() {
        String animationName = channel.getAnimationName();
        if (animationName == null) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Test whether a bone is selected.
     *
     * @return true if one is selected, false if none is selected
     */
    private boolean isBoneSelected() {
        if (selectedBoneName.equals(noBone)) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * List the names of all known animations and poses for the loaded model.
     *
     * @return a new collection
     */
    private Collection<String> listAnimationNames() {
        Spatial model = getModel();
        Collection<String> names = MyAnimation.listAnimations(model);
        names.add(bindPoseName);

        return names;
    }

    /**
     * List the names of all known bones in the loaded model.
     *
     * @return a new collection
     */
    private Collection<String> listBoneNames() {
        Spatial model = getModel();
        Collection<String> boneNames = MySkeleton.listBones(model);
        boneNames.add(noBone);
        boneNames.remove("");
        if (boneNames.size() > 30) {
            /*
             * If the list is long, remove some minor bones.
             */
            String[] allNames = MyString.toArray(boneNames);
            for (String name : allNames) {
                if (name.endsWith("Lip")
                        || name.startsWith("Brow")
                        || name.startsWith("Cheek")
                        || name.startsWith("IndexFinger")
                        || name.startsWith("Jaw")
                        || name.startsWith("MiddleFinger")
                        || name.startsWith("Pinky")
                        || name.startsWith("RingFinger")
                        || name.startsWith("Thumb")
                        || name.startsWith("Tongue")) {
                    boneNames.remove(name);
                }
            }
        }

        return boneNames;
    }

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
        /*
         * Detach the old model from the scene.
         */
        Spatial oldModel = getModel();
        if (oldModel != null) {
            rootNode.detachChild(oldModel);
            channel = null;
        }

        rootNode.attachChild(model);
        model.setName("model");
        model.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        setStatusText("modelStatus", modelName);
        selectAnimation(bindPoseName);
        selectBone(noBone);
        /*
         * Scale and translate the model to make it one world unit tall,
         * with its base resting on the XZ plane.
         */
        float maxY = MySpatial.getMaxY(model);
        float minY = MySpatial.getMinY(model);
        assert maxY > minY : maxY; // no 2D models!
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

    /**
     * Reset the skeleton of the loaded model to its bind pose.
     */
    private void resetSkeleton() {
        Skeleton skeleton = getSkeleton();

        if (boneAngleSlidersEnabledFlag) {
            /*
             * Skeleton.reset() is ineffective with user control enabled.
             */
            int boneCount = skeleton.getBoneCount();
            for (int boneIndex = 0; boneIndex < boneCount; boneIndex++) {
                Bone bone = skeleton.getBone(boneIndex);
                Vector3f translation = new Vector3f(0f, 0f, 0f);
                Quaternion rotation = new Quaternion();
                Vector3f scale = new Vector3f(1f, 1f, 1f);

                bone.setUserTransforms(translation, rotation, scale);
            }

        } else {
            skeleton.reset();
        }
    }

    /**
     * Load a single-pose animation under user control.
     */
    private void poseSkeleton() {
        Animation animation = getAnimation();
        /*
         * Make sure user control is enabled.
         */
        boolean savedStatus = boneAngleSlidersEnabledFlag;
        if (!savedStatus) {
            enableBoneAngleSliders();
        }
        /*
         * Copy bone rotations from pose to skeleton.
         */
        Skeleton skeleton = getSkeleton();
        int boneCount = skeleton.getBoneCount();
        for (int boneIndex = 0; boneIndex < boneCount; boneIndex++) {
            BoneTrack track = MyAnimation.findTrack(animation, boneIndex);

            Vector3f translation = new Vector3f(0f, 0f, 0f);
            Quaternion rotation;
            if (track != null) {
                Quaternion[] rotations = track.getRotations();
                assert rotations.length == 1;
                rotation = rotations[0];
            } else {
                rotation = new Quaternion();
            }
            Vector3f scale = new Vector3f(1f, 1f, 1f);

            Bone bone = skeleton.getBone(boneIndex);
            bone.setUserTransforms(translation, rotation, scale);
        }
        /*
         * Restore prior user control status.
         */
        if (!savedStatus) {
            disableBoneAngleSliders();
        }
    }

    /**
     * Select the named animation or pose.
     *
     * @param name (not null)
     */
    private void selectAnimation(String name) {
        setStatusText("animationStatus", name);
        if (name.equals(bindPoseName)) {
            /*
             * Select bind pose.
             */
            if (channel == null) {
                AnimControl control = getAnimControl();
                channel = control.createChannel();
            }
            channel.reset(false);
            resetSkeleton();
            updateSlidersEnabled();

        } else {
            channel.setAnim(name, 0f);
            if (channel.getAnimMaxTime() == 0f) {
                /*
                 * The new animation consists of a single pose:
                 * Set speed to zero and load the pose under user control.
                 */
                channel.setSpeed(0f);
                updateSlidersEnabled();
                poseSkeleton();

            } else {
                /*
                 * Start the animation looping at normal speed.
                 */
                channel.setLoopMode(LoopMode.Loop);
                channel.setSpeed(1f);
                updateSlidersEnabled();
            }
        }
    }

    /**
     * Select the named bone or noBone.
     *
     * @param name (not null)
     */
    private void selectBone(String name) {
        assert name != null;
        selectedBoneName = name;
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
     * Update the enabled/disabled status of the bone-angle sliders (and the
     * skeleton's user-control flags). Invoke this methods after changes are
     * made to the unit model, animation, bone, or speed.
     */
    private void updateSlidersEnabled() {
        boolean wasEnabled = boneAngleSlidersEnabledFlag;
        boolean enableFlag = isBoneSelected() && !isAnimationRunning();

        if (enableFlag && !wasEnabled) {
            enableBoneAngleSliders();
        } else if (wasEnabled && !enableFlag) {
            disableBoneAngleSliders();
        }

        assert boneAngleSlidersEnabledFlag == enableFlag;
    }
}
