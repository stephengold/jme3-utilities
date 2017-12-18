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
package jme3utilities.debug.test;

import com.jme3.animation.Bone;
import com.jme3.animation.Skeleton;
import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.input.controls.ActionListener;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import de.lessvoid.nifty.NiftyEventSubscriber;
import de.lessvoid.nifty.controls.RadioButtonStateChangedEvent;
import de.lessvoid.nifty.controls.Slider;
import de.lessvoid.nifty.controls.SliderChangedEvent;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyMesh;
import jme3utilities.MySkeleton;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.debug.AxesVisualizer;
import jme3utilities.debug.Dumper;
import jme3utilities.debug.SkeletonVisualizer;
import jme3utilities.nifty.GuiScreenController;
import jme3utilities.nifty.SliderTransform;

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
     * number of coordinate axes
     */
    final private static int numAxes = 3;
    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(PoseDemoHud.class.getName());
    /**
     * names of the coordinate axes
     */
    final private static String[] axisNames = {"x", "y", "z"};
    /**
     * names of loadable models in the jme3-testdata library
     */
    final private static String[] modelNames = {
        "Elephant", "Jaime", "Ninja", "Oto", "Sinbad"
    };
    /**
     * action prefix for the "rename animation" dialog
     */
    final private static String animationDialogPrefix = "rename animation ";
    /**
     * action prefix for the "load animation" popup menu
     */
    final private static String animationMenuPrefix = "load animation ";
    /**
     * dummy animation name used in menus and statuses (but never stored in
     * channel!) to indicate bind pose, that is, no animation loaded
     */
    final static String bindPoseName = "( bind pose )";
    /*
     * action prefix for the "rename bone" dialog
     */
    final private static String boneDialogPrefix = "rename bone ";
    /**
     * action prefix for the "select bone" popup menu
     */
    final private static String boneMenuPrefix = "select bone ";
    /**
     * action prefix for the "select keyframe" popup menu
     */
    final private static String keyframeMenuPrefix = "select keyframe ";
    /**
     * action prefix for the "load model" popup menu
     */
    final private static String modelMenuPrefix = "load model ";
    /**
     * dummy bone name used in menus and statuses to indicate no bone selected
     * (not null)
     */
    final static String noBone = "( no bone )";
    // *************************************************************************
    // fields

    /**
     * When true, bone controls should be enabled, along with the user-control
     * flags of all bones in the model's skeleton.
     */
    private boolean boneControlsEnabledFlag = false;
    /**
     * references to the bone-angle sliders, set by
     * {@link #initialize(com.jme3.app.state.AppStateManager, com.jme3.app.Application)}
     */
    final private Slider baSliders[] = new Slider[numAxes];
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized, disabled display that will be enabled
     * during initialization.
     */
    public PoseDemoHud() {
        super("pose-demo", "Interface/Nifty/huds/pose-demo.xml", true);
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
        logger.log(Level.WARNING, "unknown radio button with id={0}",
                MyString.quote(buttonId));
    }

    /**
     * Callback which Nifty invokes after the user moves a slider.
     *
     * @param sliderId Nifty element id of the slider (not null)
     * @param event details of the event (not null, ignored)
     */
    @NiftyEventSubscriber(pattern = ".*Slider")
    public void onSliderChanged(final String sliderId,
            final SliderChangedEvent event) {
        Validate.nonNull(sliderId, "slider id");
        Validate.nonNull(event, "event");

        switch (sliderId) {
            case "xSlider":
            case "ySlider":
            case "zSlider":
                baSliderChanged();
            default:
        }
    }

    /**
     * Update the 3 bone-angle sliders to match the selected bone. If no bone is
     * selected, zero them.
     *
     * @param storeAngles array to store new angles (length=3 if not null)
     */
    void updateBaSlidersToBone(float[] storeAngles) {
        if (storeAngles == null) {
            storeAngles = new float[numAxes];
        }
        assert storeAngles.length == numAxes : storeAngles.length;

        if (PoseDemo.modelState.isBoneSelected()) {
            PoseDemo.modelState.boneAngles(storeAngles);
            for (int iAxis = 0; iAxis < numAxes; iAxis++) {
                baSliders[iAxis].setValue(storeAngles[iAxis]);
            }

        } else {
            /*
             * No bone selected: zero the sliders.
             */
            for (int iAxis = 0; iAxis < numAxes; iAxis++) {
                baSliders[iAxis].setValue(0f);
                storeAngles[iAxis] = 0f;
            }
        }
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
                case "add pose":
                    PoseDemo.modelState.addPoseAnimation("new-animation");
                    loadAnimation("new-animation");
                    return;

                case "delete animation":
                    PoseDemo.modelState.deleteAnimation();
                    loadAnimation(bindPoseName);
                    return;

                case "load animation":
                    List<String> animationNames
                            = PoseDemo.modelState.listAnimationNames();
                    showPopupMenu(animationMenuPrefix, animationNames);
                    return;

                case "load model":
                    showPopupMenu(modelMenuPrefix, modelNames);
                    return;

                case "rename animation":
                    String oldAnimName = PoseDemo.modelState.getAnimationName();
                    showTextEntryDialog("Enter new name for animation:",
                            oldAnimName, "Rename", animationDialogPrefix, null);
                    return;

                case "rename bone":
                    String oldBoneName = PoseDemo.modelState.getBoneName();
                    showTextEntryDialog("Enter new name for bone:", oldBoneName,
                            "Rename", boneDialogPrefix, null);
                    return;

                case "reset bone":
                    resetBone();
                    return;

                case "select bone":
                    List<String> boneNames
                            = PoseDemo.modelState.listBoneNames();
                    MyString.reduce(boneNames, 20);
                    Collections.sort(boneNames);
                    showPopupMenu(boneMenuPrefix, boneNames);
                    return;

                case "select keyframe":
                    List<String> options = PoseDemo.modelState.listKeyframes();
                    if (options != null) {
                        MyString.reduce(options, 20);
                        showPopupMenu(keyframeMenuPrefix, options);
                    }
                    return;
            }
            if (actionString.startsWith(animationDialogPrefix)) {
                int namePos = animationDialogPrefix.length();
                String newName = actionString.substring(namePos);
                boolean success = PoseDemo.modelState.renameAnimation(newName);
                if (success) {
                    loadAnimation(newName);
                }
                return;

            } else if (actionString.startsWith(animationMenuPrefix)) {
                int namePos = animationMenuPrefix.length();
                String name = actionString.substring(namePos);
                loadAnimation(name);
                return;

            } else if (actionString.startsWith(boneDialogPrefix)) {
                int namePos = boneDialogPrefix.length();
                String newName = actionString.substring(namePos);
                boolean success = PoseDemo.modelState.renameBone(newName);
                if (success) {
                    selectBone(newName);
                }
                return;

            } else if (actionString.startsWith(boneMenuPrefix)) {
                int namePos = boneMenuPrefix.length();
                String name = actionString.substring(namePos);

                if (!PoseDemo.modelState.hasBone(name)) {
                    List<String> boneNames
                            = PoseDemo.modelState.listBoneNames(name);
                    MyString.reduce(boneNames, 20);
                    Collections.sort(boneNames);
                    showPopupMenu(boneMenuPrefix, boneNames);
                    return;
                }

                selectBone(name);
                return;

            } else if (actionString.startsWith(keyframeMenuPrefix)) {
                int namePos = keyframeMenuPrefix.length();
                String name = actionString.substring(namePos);

                List<String> options = PoseDemo.modelState.listKeyframes();
                if (!options.contains(name)) {
                    MyString.matchPrefix(options, name);
                    MyString.reduce(options, 20);
                    showPopupMenu(keyframeMenuPrefix, options);
                    return;
                }

                PoseDemo.modelState.selectKeyframe(name);
                updateBaSlidersToBone(null); // ?
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
    // AppState methods

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

        for (int iAxis = 0; iAxis < numAxes; iAxis++) {
            String axisName = axisNames[iAxis];
            Slider slider = getSlider(axisName);
            baSliders[iAxis] = getSlider(axisName);
            slider.enable();
        }

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
        boolean enable = isChecked("3DCursor");
        PoseDemo.cameraState.cursorSetEnabled(enable);
        /*
         * Model status
         */
        String status = PoseDemo.modelState.getName();
        setStatusText("modelStatus", status);
        /*
         * Animation rename and delete buttons
         */
        if (PoseDemo.modelState.isBindPoseSelected()) {
            setButtonText("renameAnim", "");
            setButtonText("delete", "");
        } else {
            setButtonText("renameAnim", "Rename this animation");
            setButtonText("delete", "Delete this animation");
        }
        /*
         * Track time
         */
        if (PoseDemo.modelState.isBindPoseSelected()) {
            status = "n/a";
        } else {
            float trackTime = PoseDemo.modelState.getTime();
            float duration = PoseDemo.modelState.getDuration();
            status = String.format("%.3f / %.3f", trackTime, duration);
        }
        setStatusText("trackTime", status);
        /*
         * Animation speed slider and status
         */
        float speed = updateSlider("speed", "x");
        PoseDemo.modelState.setSpeed(speed);
        /*
         * Select keyframe button
         */
        if (PoseDemo.modelState.hasTrack()) {
            setButtonText("keyframe", "Select keyframe");
        } else {
            setButtonText("keyframe", "");
        }
        /*
         * Bone select and rename
         */
        if (PoseDemo.modelState.isBoneSelected()) {
            setButtonText("selectBone", "Select another bone");
            setButtonText("renameBone", "Rename this bone");
        } else {
            setButtonText("selectBone", "Select a bone");
            setButtonText("renameBone", "");
        }
        /*
         * Bone-angle sliders, status, and reset button
         */
        updateBaSliders();
        /*
         * Debugging aids
         */
        updateDebugAids();
        /*
         * Lighting options
         */
        enable = isChecked("shadows");
        PoseDemo.dlsf.setEnabled(enable);
    }
    // *************************************************************************
    // private methods

    /**
     * React to user adjusting a bone-angle slider.
     */
    private void baSliderChanged() {
        if (!boneControlsEnabledFlag) {
            /*
             * Ignore slider change events when the bone-angle sliders
             * are disabled.
             */
            return;
        }

        float angles[] = new float[numAxes];
        for (int iAxis = 0; iAxis < numAxes; iAxis++) {
            angles[iAxis] = baSliders[iAxis].getValue();
        }

        Vector3f translation = new Vector3f(0f, 0f, 0f);
        Quaternion rotation = new Quaternion();
        rotation.fromAngles(angles);
        Vector3f scale = new Vector3f(1f, 1f, 1f);
        PoseDemo.modelState.setUserTransforms(translation, rotation, scale);
    }

    /**
     * Disable the 3 bone-angle sliders, reset button, and user control.
     */
    private void disableBoneControls() {
        PoseDemo.modelState.setUserControl(false);
        if (!boneControlsEnabledFlag) {
            return;
        }
        logger.log(Level.INFO, "Disable the bone controls.");
        /*
         * Disable the sliders and reset button.
         */
        for (Slider slider : baSliders) {
            slider.disable();
        }
        setButtonText("reset", "");
        /*
         * Update the status.
         */
        boneControlsEnabledFlag = false;
    }

    /**
     * Enable the 3 bone-angle sliders, reset button, and user control.
     */
    private void enableBoneControls() {
        PoseDemo.modelState.setUserControl(true);
        if (boneControlsEnabledFlag) {
            return;
        }
        logger.log(Level.INFO, "Enable the bone-angle sliders.");
        /*
         * Enable the sliders.
         */
        for (Slider slider : baSliders) {
            slider.enable();
        }
        setButtonText("reset", "Reset bone");
        /*
         * Update the status.
         */
        boneControlsEnabledFlag = true;
    }

    /**
     * Load the named pose or animation.
     *
     * @param name (not null)
     */
    private void loadAnimation(String name) {
        assert name != null;

        setStatusText("animationStatus", name);
        if (name.equals(bindPoseName)) {
            /*
             * Load bind pose.
             */
            PoseDemo.modelState.loadBindPose();
            updateBaSlidersToBone(null);

        } else {
            if (PoseDemo.modelState.getDuration(name) == 0f) {
                /*
                 * The animation consists of a single pose:
                 * Set speed to zero and load the pose under user control.
                 */
                PoseDemo.modelState.loadAnimation(name, 0f);
                PoseDemo.modelState.poseSkeleton();
                updateBaSlidersToBone(null);

            } else {
                /*
                 * Start the animation looping at normal speed.
                 */
                PoseDemo.modelState.loadAnimation(name, 1f);
            }
        }
        String boneName = PoseDemo.modelState.getBoneName();
        selectBone(boneName);
    }

    /**
     * Load the named model.
     *
     * @param name (not null)
     */
    private void loadModel(String name) {
        assert name != null;

        PoseDemo.modelState.load(name);
        loadAnimation(bindPoseName);
        selectBone(noBone);
    }

    /**
     * Reset the selected bone.
     */
    private void resetBone() {
        if (!boneControlsEnabledFlag) {
            /*
             * Ignore reset events when the bone controls
             * are disabled.
             */
            return;
        }

        for (int iAxis = 0; iAxis < numAxes; iAxis++) {
            baSliders[iAxis].setValue(0f);
        }
    }

    /**
     * Select the named bone or noBone.
     *
     * @param name (not null)
     */
    private void selectBone(String name) {
        PoseDemo.modelState.selectBone(name);

        String status;
        if (PoseDemo.modelState.hasTrack()) {
            status = String.format("+ %s", name); // bone with track
        } else {
            status = name;
        }
        setStatusText("boneStatus", status);

        String status2, status3, status4;
        Skeleton skeleton = PoseDemo.modelState.getSkeleton();
        int numBones = skeleton.getBoneCount();
        if (PoseDemo.modelState.isBoneSelected()) {
            Bone bone = PoseDemo.modelState.getBone();

            Bone parent = bone.getParent();
            if (parent == null) {
                status2 = "a root bone";
            } else {
                String parentName = parent.getName();
                status2 = String.format("a child of %s",
                        MyString.quote(parentName));
            }

            List<Bone> children = bone.getChildren();
            int numChildren = children.size();
            if (numChildren > 1) {
                status3 = String.format("with %d children", numChildren);
            } else if (numChildren == 1) {
                String childName = children.get(0).getName();
                status3 = String.format("the parent of %s",
                        MyString.quote(childName));
            } else {
                status3 = "with no children";
            }

            int boneIndex = skeleton.getBoneIndex(bone.getName());
            int numInfluenced = 0;
            for (Mesh mesh : PoseDemo.modelState.listMeshes()) {
                numInfluenced += MyMesh.numInfluenced(mesh, boneIndex);
            }
            status4 = String.format("#%d of %d; vertices = %d",
                    boneIndex + 1, numBones, numInfluenced);

        } else {
            status2 = String.format("total bones = %d", numBones);
            int numRootBones = MySkeleton.numRootBones(skeleton);
            status3 = String.format("root bones = %d", numRootBones);
            int numLeafBones = MySkeleton.numLeafBones(skeleton);
            status4 = String.format("leaf bones = %d", numLeafBones);
        }
        setStatusText("boneStatus2", status2);
        setStatusText("boneStatus3", status3);
        setStatusText("boneStatus4", status4);

        updateBaSlidersToBone(null);
    }

    /**
     * Update the 3 bone-angle sliders, reset button, and status labels.
     */
    private void updateBaSliders() {
        if (!PoseDemo.modelState.isBoneSelected()) {
            /*
             * No bone selected:
             * Zero sliders, clear status labels, and disable controls.
             */
            for (int iAxis = 0; iAxis < numAxes; iAxis++) {
                baSliders[iAxis].setValue(0f);

                String axisName = axisNames[iAxis];
                String statusName = axisName + "SliderStatus";
                setStatusText(statusName, "");
            }
            disableBoneControls();

        } else {
            float angles[] = new float[numAxes];
            if (PoseDemo.modelState.isAnimationRunning()) {
                /*
                 * An animation is running:
                 * Update the sliders to match bone, disable controls.
                 */
                updateBaSlidersToBone(angles);
                disableBoneControls();

            } else {
                /*
                 * No animation is running:
                 * Read the sliders, enable controls.
                 */
                for (int iAxis = 0; iAxis < numAxes; iAxis++) {
                    angles[iAxis] = baSliders[iAxis].getValue();
                }
                enableBoneControls();
            }
            /*
             * Update the status labels to match the sliders.
             */
            for (int iAxis = 0; iAxis < numAxes; iAxis++) {
                String axisName = axisNames[iAxis];
                String statusName = axisName + "SliderStatus";
                String statusText
                        = String.format("%s = %+4.2f", axisName, angles[iAxis]);
                setStatusText(statusName, statusText);
            }
        }
    }

    /**
     * Update a bank of 3 sliders that control a color.
     *
     * @param prefix unique id prefix of the bank (not null)
     * @return color indicated by the sliders (new instance)
     */
    private ColorRGBA updateColorBank(String prefix) {
        assert prefix != null;

        float r = updateSlider(prefix + "R", "");
        float g = updateSlider(prefix + "G", "");
        float b = updateSlider(prefix + "B", "");
        ColorRGBA color = new ColorRGBA(r, g, b, 1f);

        return color;
    }

    /**
     * Update the GUI controls for the debugging aids.
     */
    private void updateDebugAids() {
        /*
         * Bone axes visualizer
         */
        AxesVisualizer axes = PoseDemo.modelState.getBoneAxesVisualizer();
        float lineWidth = updateSlider("bacLineWidth", " pixels");
        float length = updateLogSlider("bacLength", 10f, " bone units");
        if (axes != null) {
            axes.setLineWidth(lineWidth);
            axes.setAxisLength(length);

            boolean enable = isChecked("bacEnable");
            axes.setEnabled(enable);
            enable = isChecked("bacDepthTest");
            axes.setDepthTest(enable);
        }
        /*
         * Global axes visualizer
         */
        axes = rootNode.getControl(AxesVisualizer.class);
        boolean enable = isChecked("gacEnable");
        axes.setEnabled(enable);
        enable = isChecked("gacDepthTest");
        axes.setDepthTest(enable);
        lineWidth = updateSlider("gacLineWidth", " pixels");
        axes.setLineWidth(lineWidth);
        length = updateSlider("gacLength", " world units");
        axes.setAxisLength(length);
        /*
         * skeleton visualizer
         */
        SkeletonVisualizer visualizer
                = PoseDemo.modelState.getSkeletonVisualizer();
        enable = isChecked("skeletonDebug");
        visualizer.setEnabled(enable);
        ColorRGBA wireColor = updateColorBank("wire");
        visualizer.setLineColor(wireColor);
        ColorRGBA pointColor = updateColorBank("point");
        visualizer.setPointColor(pointColor);
        lineWidth = updateSlider("sdcLineWidth", " pixels");
        visualizer.setLineWidth(lineWidth);
        float pointSize = updateSlider("pointSize", " pixels");
        visualizer.setPointSize(pointSize);
        /*
         * Dumper
         */
        Dumper dumper = PoseDemo.getDumper();
        enable = isChecked("printTransform");
        dumper.setDumpTransform(enable);
        enable = isChecked("printUser");
        dumper.setDumpUser(enable);
        enable = isChecked("printBucket");
        dumper.setDumpBucket(enable);
        enable = isChecked("printShadow");
        dumper.setDumpShadow(enable);
        enable = isChecked("printCull");
        dumper.setDumpCull(enable);
    }

    /**
     * Read the value of a logarithmic Nifty slider and update its status label.
     * This assumes a naming convention where (a) the slider's Nifty id ends in
     * "Slider" and (b) the Nifty id of the corresponding label consists of the
     * same prefix followed by "SliderStatus".
     *
     * @param namePrefix unique id prefix of the slider (not null)
     * @param logBase logarithm base of the slider (2 or 10)
     * @param statusSuffix to specify a unit of measurement (not null)
     * @return scaled value of the slider
     */
    private float updateLogSlider(String namePrefix, float logBase,
            String statusSuffix) {
        float scaledValue;
        if (logBase == 10f) {
            scaledValue = readSlider(namePrefix, SliderTransform.Log10);
        } else if (logBase == 2f) {
            scaledValue = readSlider(namePrefix, SliderTransform.Log2);
        } else {
            throw new IllegalArgumentException();
        }
        updateSliderStatus(namePrefix, scaledValue, statusSuffix);

        return scaledValue;
    }

    /**
     * Read the value of a linear Nifty slider and update its status label. This
     * assumes a naming convention where (a) the slider's Nifty id ends in
     * "Slider" and (b) the Nifty id of the corresponding label consists of the
     * same prefix followed by "SliderStatus".
     *
     * @param name unique id prefix of the slider (not null)
     * @param statusSuffix suffix to specify a unit of measurement (not null)
     * @return value of the slider
     */
    private float updateSlider(String name, String statusSuffix) {
        float value = readSlider(name, SliderTransform.None);
        updateSliderStatus(name, value, statusSuffix);

        return value;
    }
}
