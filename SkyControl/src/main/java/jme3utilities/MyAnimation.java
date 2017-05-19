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

import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.Animation;
import com.jme3.animation.AudioTrack;
import com.jme3.animation.Bone;
import com.jme3.animation.BoneTrack;
import com.jme3.animation.EffectTrack;
import com.jme3.animation.LoopMode;
import com.jme3.animation.Skeleton;
import com.jme3.animation.SpatialTrack;
import com.jme3.animation.Track;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import java.util.Collection;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import static jme3utilities.Misc.blendTime;

/**
 * Utility methods for manipulating animations, channels, and tracks. All
 * methods should be static.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class MyAnimation {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            MyAnimation.class.getName());
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private MyAnimation() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Smoothly transition an animation channel to the named animation.
     *
     * @param channel animation channel (not null, modified)
     * @param animationName name of animation (or null to reset the channel)
     */
    public static void blendTo(AnimChannel channel, String animationName) {
        if (animationName == null) {
            channel.reset(true);
            return;
        }
        String oldAnimationName = channel.getAnimationName();
        if (animationName.equals(oldAnimationName)) {
            return;
        }
        /*
         * new animation
         */
        logger.log(Level.INFO, "new animation={0}", animationName);
        channel.setAnim(animationName, blendTime);
        channel.setLoopMode(LoopMode.Loop);
    }

    /**
     * Create a BoneTrack consisting of a single bone with a fixed transform.
     *
     * @param boneIndex which bone (&ge;0)
     * @param translation relative to bind pose (not null, unaffected)
     * @param rotation relative to bind pose (not null, unaffected)
     * @param scale relative to bind pose (not null, unaffected)
     * @return a new instance
     */
    public static BoneTrack createTrack(int boneIndex, Vector3f translation,
            Quaternion rotation, Vector3f scale) {
        Validate.nonNegative(boneIndex, "bone index");
        Validate.nonNull(translation, "translation");
        Validate.nonNull(rotation, "rotation");
        Validate.nonNull(scale, "scale");

        Vector3f copyTranslation = translation.clone();
        Quaternion copyRotation = rotation.clone();
        Vector3f copyScale = scale.clone();

        float[] times = {0f};
        Vector3f[] translations = {copyTranslation};
        Quaternion[] rotations = {copyRotation};
        Vector3f[] scales = {copyScale};
        BoneTrack result = new BoneTrack(boneIndex, times, translations,
                rotations, scales);

        return result;
    }

    /**
     * Describe an animation.
     *
     * @param animation animation to describe (not null)
     * @param spatial animated spatial (not null)
     * @return textual description (not null)
     */
    public static String describe(Animation animation, Spatial spatial) {
        Validate.nonNull(spatial, "spatial");

        Track[] tracks = animation.getTracks();
        int numTracks = tracks.length;
        String name = animation.getName();
        if (numTracks > 2) {
            String result = String.format("%s[%d]", name, numTracks);
            return result;
        }

        String[] trackDescriptions = new String[numTracks];
        for (int iTrack = 0; iTrack < numTracks; iTrack++) {
            Track track = tracks[iTrack];
            trackDescriptions[iTrack] = describe(track, spatial);
        }
        String joined = MyString.join(trackDescriptions);
        String result = String.format("%s(%s)", name, joined);

        return result;
    }

    /**
     * Describe an animation track.
     *
     * @param track (not null)
     * @param spatial animated spatial (not null)
     * @return textual description (not null)
     */
    public static String describe(Track track, Spatial spatial) {
        Validate.nonNull(track, "track");
        Validate.nonNull(spatial, "spatial");

        char typeChar = describeTrack(track);
        float length = track.getLength();
        String result;
        if (track instanceof BoneTrack) {
            BoneTrack boneTrack = (BoneTrack) track;
            int boneIndex = boneTrack.getTargetBoneIndex();
            Skeleton skeleton = MySkeleton.getSkeleton(spatial);
            Bone bone = skeleton.getBone(boneIndex);
            result = String.format("%c:%s", typeChar, bone.getName());
        } else {
            result = String.format("%c[%.1f]", typeChar, length);
        }

        return result;
    }

    /**
     * Describe a track.
     *
     * @param track track to describe
     * @return mnemonic character
     */
    public static char describeTrack(Track track) {
        if (track instanceof AudioTrack) {
            return 'a';
        } else if (track instanceof BoneTrack) {
            return 'b';
        } else if (track instanceof EffectTrack) {
            return 'e';
        } else if (track instanceof SpatialTrack) {
            return 's';
        }
        return '?';
    }

    /**
     * Find a BoneTrack in a specified animation for a specified bone.
     *
     * @param animation which animation (not null, unaffected)
     * @param boneIndex which bone (&ge;0)
     * @return the pre-existing instance, or null if not found
     */
    public static BoneTrack findTrack(Animation animation, int boneIndex) {
        Validate.nonNull(animation, "animation");
        Validate.nonNegative(boneIndex, "bone index");

        Track[] tracks = animation.getTracks();
        for (Track track : tracks) {
            if (track instanceof BoneTrack) {
                BoneTrack boneTrack = (BoneTrack) track;
                int trackBoneIndex = boneTrack.getTargetBoneIndex();
                if (boneIndex == trackBoneIndex) {
                    return boneTrack;
                }
            }
        }

        return null;
    }

    /**
     * Read the name of the target bone of the specified bone track in the
     * specified animation control.
     *
     * @param boneTrack which bone track (not null, unaffected)
     * @param animControl the animation control containing that track (not null,
     * unaffected)
     * @return the name
     */
    public static String getTargetName(BoneTrack boneTrack,
            AnimControl animControl) {
        int boneIndex = boneTrack.getTargetBoneIndex();
        Skeleton skeleton = animControl.getSkeleton();
        Bone bone = skeleton.getBone(boneIndex);
        String result = bone.getName();

        return result;
    }

    /**
     * List all animations in an animated spatial.
     *
     * @param spatial (not null)
     * @return new collection in lexicographic order
     */
    public static Collection<String> listAnimations(Spatial spatial) {
        AnimControl control = spatial.getControl(AnimControl.class);
        Collection<String> result = new TreeSet<>();
        if (control == null) {
            return result;
        }
        Collection<String> animationNames = control.getAnimationNames();
        result.addAll(animationNames);

        return result;
    }

    /**
     * Remove all repetitious keyframes from a bone track.
     *
     * @param boneTrack (not null)
     * @return true if 1 or more keyframes were removed, otherwise false
     */
    public static boolean removeRepeats(BoneTrack boneTrack) {
        float[] originalTimes = boneTrack.getKeyFrameTimes();
        /*
         * Count distinct keyframes.
         */
        float prevTime = Float.NEGATIVE_INFINITY;
        int numDistinct = 0;
        for (float time : originalTimes) {
            if (time != prevTime) {
                ++numDistinct;
            }
            prevTime = time;
        }

        int originalCount = originalTimes.length;
        if (numDistinct == originalCount) {
            return false;
        }
        Vector3f[] originalTranslations = boneTrack.getTranslations();
        Quaternion[] originalRotations = boneTrack.getRotations();
        Vector3f[] originalScales = boneTrack.getScales();
        /*
         * Allocate new arrays.
         */
        float[] newTimes = new float[numDistinct];
        Vector3f[] newTranslations = new Vector3f[numDistinct];
        Quaternion[] newRotations = new Quaternion[numDistinct];
        Vector3f[] newScales;
        if (originalScales == null) {
            newScales = null;
        } else {
            newScales = new Vector3f[numDistinct];
        }
        /*
         * Copy all non-repeated keyframes.
         */
        prevTime = Float.NEGATIVE_INFINITY;
        int newIndex = 0;
        for (int oldIndex = 0; oldIndex < originalCount; oldIndex++) {
            float time = originalTimes[oldIndex];
            if (time != prevTime) {
                newTimes[newIndex] = originalTimes[oldIndex];
                newTranslations[newIndex] = originalTranslations[oldIndex];
                newRotations[newIndex] = originalRotations[oldIndex];
                if (newScales != null) {
                    newScales[newIndex] = originalScales[oldIndex];
                }
                ++newIndex;
            }
            prevTime = time;
        }

        boneTrack.setKeyframes(newTimes, newTranslations, newRotations,
                newScales);
        return true;
    }

    /**
     * Remove repetitious keyframes from an animation.
     *
     * @param animation (not null)
     * @return number of tracks edited
     */
    public static int removeRepeats(Animation animation) {
        int numTracksEdited = 0;
        Track[] tracks = animation.getTracks();
        for (Track track : tracks) {
            if (track instanceof BoneTrack) {
                BoneTrack boneTrack = (BoneTrack) track;
                boolean removed = removeRepeats(boneTrack);
                if (removed) {
                    ++numTracksEdited;
                }
            } // TODO other track types
        }

        return numTracksEdited;
    }
}
