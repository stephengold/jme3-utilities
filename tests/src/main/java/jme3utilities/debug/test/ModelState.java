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
import com.jme3.animation.Track;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Spatial;
import com.jme3.scene.plugins.ogre.MeshLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyAnimation;
import jme3utilities.MySkeleton;
import jme3utilities.MySpatial;
import jme3utilities.MyString;
import jme3utilities.SimpleAppState;
import jme3utilities.Validate;
import jme3utilities.debug.SkeletonDebugControl;

/**
 * Model state for the PoseDemo application.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class ModelState extends SimpleAppState {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            ModelState.class.getName());
    // *************************************************************************
    // fields

    /**
     * animation channel (Can be null only if no model selected.
     * getAnimationName() == null means bind pose is selected.)
     */
    private AnimChannel channel = null;
    /**
     * true if the loaded model has user control enabled, otherwise false
     */
    private boolean userControlFlag = false;
    /**
     * inverse local rotation of each bone in original bind pose, indexed by
     * boneIndex
     */
    final private List<Quaternion> inverseBindPose = new ArrayList<>(30);
    /**
     * the skeleton debug control (set by #load())
     */
    private SkeletonDebugControl skeletonDebugControl = null;
    /**
     * the model's spatial (set by #load())
     */
    private Spatial spatial = null;
    /**
     * name of the loaded model (set by #load())
     */
    private String loadedModelName = null;
    /**
     * name of the selected bone or noBone (not null)
     */
    private String selectedBoneName = PoseDemoHud.noBone;
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized, enabled state.
     */
    ModelState() {
        super(true);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Add a pose animation to the loaded model and select the new animation.
     * The new animation has zero duration, a single keyframe at t=0, and all
     * the tracks are BoneTracks, set to the current pose.
     *
     * @param animationName name for the new animation (not null , not empty)
     */
    void addPoseAnimation(String animationName) {
        assert animationName != null;
        assert !animationName.isEmpty();
        /*
         * Check whether the name is in use.
         */
        AnimControl control = getAnimControl();
        Collection<String> names = control.getAnimationNames();
        if (names.contains(animationName)) {
            logger.log(Level.WARNING, "replacing existing animation {0}",
                    MyString.quote(animationName));
            Animation oldAnimation = control.getAnim(animationName);
            control.removeAnim(oldAnimation);
        }

        Animation pose = captureCurrentPose(animationName);
        control.addAnim(pose);
    }

    /**
     * Delete the loaded animation.
     */
    void deleteAnimation() {
        if (isBindPoseSelected()) {
            logger.log(Level.WARNING, "Cannot delete bind pose.");
            return;
        }
        AnimControl animControl = getAnimControl();
        String animationName = channel.getAnimationName();
        Animation animation = animControl.getAnim(animationName);
        animControl.removeAnim(animation);

        loadBindPose();
    }

    /**
     * Calculate the rotation angles of the selected bone.
     *
     * @param storeAngles (&ge;3 elements, modified)
     */
    void boneAngles(float[] storeAngles) {
        int boneIndex = MySkeleton.findBoneIndex(spatial, selectedBoneName);
        Quaternion rotation = boneRotation(boneIndex);
        rotation.toAngles(storeAngles);
    }

    /**
     * Access the loaded animation.
     *
     * @return the pre-existing instance, or null if none or in bind pose
     */
    Animation getAnimation() {
        if (isBindPoseSelected()) {
            return null;
        }
        AnimControl animControl = getAnimControl();
        String animationName = channel.getAnimationName();
        Animation animation = animControl.getAnim(animationName);

        return animation;
    }

    /**
     * Access the named animation.
     *
     * @param name (not null)
     * @return the pre-existing instance
     */
    Animation getAnimation(String name) {
        AnimControl animControl = getAnimControl();
        Animation animation = animControl.getAnim(name);

        return animation;
    }

    /**
     * Access the AnimControl of the loaded model.
     *
     * @return the pre-existing instance, or null if none
     */
    AnimControl getAnimControl() {
        AnimControl animControl = spatial.getControl(AnimControl.class);
        if (animControl == null) {
            String message = String.format(
                    "expected model %s to have an AnimControl",
                    MyString.quote(loadedModelName));
            throw new IllegalArgumentException(message);
        }

        return animControl;
    }

    /**
     * Read the name of the selected bone.
     *
     * @return the name, or noBone if none selected (not null)
     */
    String getBoneName() {
        String result = selectedBoneName;
        assert result != null;
        return result;
    }

    /**
     * Read the duration of the loaded animation.
     *
     * @return time (in seconds, &ge;0)
     */
    float getDuration() {
        Animation animation = getAnimation();
        float result = animation.getLength();

        assert result >= 0f : result;
        return result;
    }

    /**
     * Read the duration of a named animation.
     *
     * @param name
     * @return time (in seconds, &ge;0)
     */
    float getDuration(String name) {
        assert name != null;

        Animation animation = getAnimation(name);
        float result = animation.getLength();

        assert result >= 0f : result;
        return result;
    }

    /**
     * Read the name of the loaded model.
     *
     * @return name (not null)
     */
    String getName() {
        assert loadedModelName != null;
        return loadedModelName;
    }

    /**
     * Access the skeleton debug control.
     *
     * @return the pre-existing instance (not null)
     */
    SkeletonDebugControl getSkeletonDebugControl() {
        assert skeletonDebugControl != null;
        return skeletonDebugControl;
    }

    /**
     * Access the spatial which represents the model in the scene.
     *
     * @return the pre-existing instance (not null)
     */
    Spatial getSpatial() {
        assert spatial != null;
        return spatial;
    }

    /**
     * Read the animation speed.
     *
     * @return relative speed (&ge;0, 1 &rarr; normal)
     */
    float getSpeed() {
        if (isBindPoseSelected()) {
            return 0f;
        }
        float result = channel.getSpeed();

        assert result >= 0f : result;
        return result;
    }

    /**
     * Read the animation time.
     *
     * @return seconds since start (&ge;0)
     */
    float getTime() {
        float result;
        if (getDuration() == 0f) {
            result = 0f;
        } else {
            result = channel.getTime();
        }

        assert result >= 0f : result;
        return result;
    }

    /**
     * Test whether the skeleton contains the named bone.
     *
     * @param name (not null)
     * @return true if found or noBone, otherwise false
     */
    boolean hasBone(String name) {
        if (name.equals(PoseDemoHud.noBone)) {
            return true;
        }
        Bone bone = MySkeleton.getBone(spatial, name);
        if (bone == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Test whether the selected bone has a BoneTrack.
     *
     * @return true if a bone is selected and it has a track, otherwise false
     */
    boolean hasTrack() {
        BoneTrack track = getTrack();
        if (track == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Test whether an animation is running.
     *
     * @return true if an animation is running, false otherwise
     */
    boolean isAnimationRunning() {
        if (getSpeed() == 0f) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Test whether the bind pose is selected.
     *
     * @return true if it's selected, false if an animation is selected
     */
    boolean isBindPoseSelected() {
        if (channel == null) {
            return true;
        }
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
    boolean isBoneSelected() {
        if (selectedBoneName.equals(PoseDemoHud.noBone)) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Test whether user control is enabled.
     *
     * @return true if enabled, false if disabled
     */
    boolean isUserControl() {
        return userControlFlag;
    }

    /**
     * List the names of all known animations and poses for the loaded model.
     *
     * @return a new collection
     */
    Collection<String> listAnimationNames() {
        Collection<String> names = MyAnimation.listAnimations(spatial);
        names.add(PoseDemoHud.bindPoseName);

        return names;
    }

    /**
     * List the names of all known bones in the loaded model.
     *
     * @return a new list
     */
    List<String> listBoneNames() {
        List<String> boneNames = MySkeleton.listBones(spatial);
        boneNames.remove("");
        boneNames.add(PoseDemoHud.noBone);

        return boneNames;
    }

    /**
     * List all known bones in the loaded model whose names begin with the
     * specified prefix.
     *
     * @return a new list
     */
    List<String> listBoneNames(String namePrefix) {
        List<String> boneNames = listBoneNames();
        for (String name : MyString.toArray(boneNames)) {
            if (!name.startsWith(namePrefix)) {
                boneNames.remove(name);
            }
        }

        return boneNames;
    }

    /**
     * List keyframes of the selected bone, along with play options for the
     * loaded animation.
     *
     * @return a new list, or null if no options
     */
    List<String> listKeyframes() {
        if (channel == null || channel.getAnimationName() == null) {
            logger.log(Level.INFO, "No animation is selected.");
            return null;
        }
        if (!isBoneSelected()) {
            logger.log(Level.INFO, "No bone is selected.");
            return null;
        }
        if (!isTrackSelected()) {
            logger.log(Level.INFO, "No track is selected.");
            return null;
        }

        BoneTrack track = getTrack();
        float[] keyframes = track.getTimes();

        List<String> result = new ArrayList<>(20);
        result.add("play");
        result.add("slow");
        result.add("pause");
        for (float keyframe : keyframes) {
            String menuItem = String.format("%.3f", keyframe);
            result.add(menuItem);
        }

        return result;
    }

    /**
     * Unload the current model, if any, and load a new one.
     *
     * @param name name of model to load (not null)
     * @return true if successful, otherwise false
     */
    boolean load(String name) {
        assert isInitialized();
        assert name != null;
        /*
         * Temporarily hush loader warnings about vertices with >4 weights.
         */
        Logger mlLogger = Logger.getLogger(MeshLoader.class.getName());
        Level save = mlLogger.getLevel();
        mlLogger.setLevel(Level.SEVERE);

        String assetPath = String.format("Models/%s/%s.mesh.xml", name, name);
        Spatial loaded = assetManager.loadModel(assetPath);

        mlLogger.setLevel(save);
        /*
         * Detach the old spatial (if any) from the scene.
         */
        if (spatial != null) {
            rootNode.detachChild(spatial);
            channel = null;
        }

        loadedModelName = name;
        spatial = loaded;
        userControlFlag = false;
        MySkeleton.setUserControl(spatial, userControlFlag);
        spatial.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        rootNode.attachChild(spatial);
        saveInverseBindPose();
        /*
         * Scale and translate the model so its bind pose is 1 world-unit
         * tall, with its base resting on the XZ plane.
         */
        float maxY = MySpatial.getMaxY(spatial);
        float minY = MySpatial.getMinY(spatial);
        assert maxY > minY : maxY; // no 2D models!
        float worldScale = 1f / (maxY - minY);
        MySpatial.setWorldScale(spatial, worldScale);
        Vector3f worldLocation = new Vector3f(0f, -minY * worldScale, 0f);
        MySpatial.setWorldLocation(spatial, worldLocation);
        /*
         * Add a skeleton debug control to the spatial.
         */
        skeletonDebugControl = new SkeletonDebugControl(assetManager);
        spatial.addControl(skeletonDebugControl);

        loadBindPose();
        selectBone(PoseDemoHud.noBone);

        return true;
    }

    /**
     * Load an animation (not bind pose) and set the playback speed.
     *
     * @para name name of animation
     * @param speed (&ge;0)
     */
    void loadAnimation(String name, float speed) {
        assert name != null;
        assert !name.equals(PoseDemoHud.bindPoseName);
        assert speed >= 0f : speed;

        channel.setAnim(name, 0f);
        if (speed > 0f) {
            channel.setLoopMode(LoopMode.Loop);
        }
        channel.setSpeed(speed);
    }

    /**
     * Load the bind pose.
     */
    void loadBindPose() {
        if (channel == null) {
            AnimControl control = getAnimControl();
            channel = control.createChannel();
        }
        channel.reset(false);
        resetSkeleton();
    }

    /**
     * Load the selected animation pose under user control.
     */
    void poseSkeleton() {
        /*
         * Save user-control status and enable it.
         */
        boolean savedStatus = userControlFlag;
        setUserControl(true);
        /*
         * Copy bone rotations from pose to skeleton.
         */
        Animation animation = getAnimation();
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
         * Restore prior user-control status.
         */
        setUserControl(savedStatus);
    }

    /**
     * Rotate the model around +Y by the specified angle.
     *
     * @param angle in radians
     */
    void rotateY(float angle) {
        spatial.rotate(0f, angle, 0f);
    }

    /**
     * Alter which bone is selected. TODO validate
     *
     * @param name bone name or noBone
     */
    void selectBone(String name) {
        selectedBoneName = name;
    }

    /**
     * Alter which keyframe is selected in the current bone track. A bone track
     * must be selected.
     *
     * @param name name of the new selection (not null)
     */
    void selectKeyframe(String name) {
        assert isTrackSelected();
        assert name != null;

        switch (name) {
            case "play":
                channel.setSpeed(1f);
                break;
            case "slow":
                channel.setSpeed(0.25f);
                break;
            case "pause":
                channel.setSpeed(0f);
                break;
            default:
                float newTime = Float.valueOf(name);
                channel.setSpeed(0f);
                channel.setTime(newTime);
        }
    }

    /**
     * Alter the user-control setting of the loaded model.
     *
     * @param newSetting true &rarr; user transforms, false &rarr; animation
     */
    void setUserControl(boolean newSetting) {
        if (newSetting == userControlFlag) {
            return;
        }
        MySkeleton.setUserControl(spatial, newSetting);
        userControlFlag = newSetting;
    }

    /**
     * Alter the user transforms of the selected bone.
     *
     * @param translation
     * @param rotation
     * @param scale
     */
    void setUserTransforms(Vector3f translation, Quaternion rotation,
            Vector3f scale) {
        Validate.nonNull(translation, "translation");
        Validate.nonNull(rotation, "rotation");
        Validate.nonNull(scale, "scale");

        Bone bone = getBone();
        bone.setUserTransforms(translation, rotation, scale);
    }
    // *************************************************************************
    // private methods

    /**
     * Calculate the rotation of a bone (local rotation minus bind rotation).
     *
     * @param boneIndex (&ge;0)
     * @return a new instance
     */
    private Quaternion boneRotation(int boneIndex) {
        assert boneIndex >= 0 : boneIndex;

        Skeleton skeleton = getSkeleton();
        Bone bone = skeleton.getBone(boneIndex);
        Quaternion localRotation = bone.getLocalRotation().clone();

        Quaternion invBind = inverseBindPose.get(boneIndex);
        Quaternion rotation = invBind.mult(localRotation);

        return rotation;
    }

    /**
     * Capture the model's current pose as an animation. The new animation has a
     * zero duration, a single keyframe at t=0, and all its tracks are
     * BoneTracks.
     *
     * @parm animationName name for the new animation (not null)
     * @return a new instance
     */
    private Animation captureCurrentPose(String animationName) {
        assert animationName != null;
        assert !animationName.isEmpty();
        /*
         * Start with an empty animation.
         */
        float duration = 0f;
        Animation result = new Animation(animationName, duration);

        Skeleton skeleton = getSkeleton();
        skeleton.updateWorldVectors();
        /*
         * Add a BoneTrack for each bone in the skeleton.
         */
        int numBones = skeleton.getBoneCount();
        for (int boneIndex = 0; boneIndex < numBones; boneIndex++) {
            Vector3f translation = Vector3f.ZERO; // TODO
            Quaternion rotation = boneRotation(boneIndex);
            Vector3f scale = Vector3f.UNIT_XYZ; // TODO
            BoneTrack track = MyAnimation.createTrack(boneIndex, translation,
                    rotation, scale);
            result.addTrack(track);
        }

        return result;
    }

    /**
     * Find the track for the selected bone in the loaded animation.
     *
     * @return the pre-existing instance, or null if none
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

        int boneIndex = MySkeleton.findBoneIndex(spatial, selectedBoneName);
        Animation animation = getAnimation();
        BoneTrack track = MyAnimation.findTrack(animation, boneIndex);

        return track;
    }

    /**
     * Access the selected bone.
     *
     * @return the pre-existing instance, or null if none selected
     */
    private Bone getBone() {
        if (!isBoneSelected()) {
            return null;
        }
        Bone bone = MySkeleton.getBone(spatial, selectedBoneName);

        return bone;
    }

    /**
     * Access the skeleton of the loaded model.
     *
     * @return the pre-existing instance (not null)
     */
    private Skeleton getSkeleton() {
        Skeleton skeleton = MySkeleton.getSkeleton(spatial);

        assert skeleton != null;
        return skeleton;
    }

    /**
     * Access the selected BoneTrack.
     *
     * @return the pre-existing instance, or null if no track is selected
     */
    private BoneTrack getTrack() {
        if (isBoneSelected()) {
            BoneTrack result = findTrack();
            return result;
        }
        assert !isTrackSelected();
        return null;
    }

    /**
     * Test whether a track is selected.
     *
     * @return true if one is selected, false if none is selected
     */
    private boolean isTrackSelected() {
        if (isBoneSelected()) {
            if (isBindPoseSelected()) {
                return false;
            }
            Track track = findTrack();
            if (track == null) {
                return false;
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    /**
     * Reset the skeleton to its bind pose.
     */
    private void resetSkeleton() {
        Skeleton skeleton = getSkeleton();

        if (userControlFlag) {
            /*
             * Skeleton.reset() is ineffective with user mode enabled,
             * so load bind pose under user control.
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
     * Save (in memory) a copy of all inverse local rotations for the bind pose.
     */
    private void saveInverseBindPose() {
        resetSkeleton();
        inverseBindPose.clear();

        Skeleton skeleton = getSkeleton();
        int boneCount = skeleton.getBoneCount();
        for (int boneIndex = 0; boneIndex < boneCount; boneIndex++) {
            Bone bone = skeleton.getBone(boneIndex);
            /*
             * Save the bone's local orientation (relative to its parent,
             * or if it's a root bone, relative to the model).
             */
            Quaternion local = bone.getLocalRotation();
            Quaternion inverse = local.inverse();
            inverseBindPose.add(inverse);
        }
    }
}
