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

import com.jme3.animation.AnimControl;
import com.jme3.animation.Animation;
import com.jme3.animation.AudioTrack;
import com.jme3.animation.Bone;
import com.jme3.animation.BoneTrack;
import com.jme3.animation.EffectTrack;
import com.jme3.animation.Skeleton;
import com.jme3.animation.SpatialTrack;
import com.jme3.animation.Track;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import java.util.Collection;
import java.util.TreeSet;
import java.util.logging.Logger;
import jme3utilities.math.MyArray;

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
     * Copy a bone track, deleting everything before the specified time, and
     * making that the start of the animation.
     *
     * @param oldTrack (not null, unaffected)
     * @param neckTime cutoff time (in seconds, &gt;0)
     * @param neckTransform user transform of bone at the neck time (not null,
     * unaffected)
     * @param oldDuration (in seconds, &ge;neckTime)
     * @return a new instance
     */
    public static BoneTrack behead(BoneTrack oldTrack, float neckTime,
            Transform neckTransform, float oldDuration) {
        Validate.positive(neckTime, "neck time");

        float[] oldTimes = oldTrack.getKeyFrameTimes();
        Vector3f[] oldTranslations = oldTrack.getTranslations();
        Quaternion[] oldRotations = oldTrack.getRotations();
        Vector3f[] oldScales = oldTrack.getScales();
        int oldCount = oldTimes.length;

        int neckIndex;
        neckIndex = findPreviousKeyframeIndex(oldTrack, neckTime);
        int newCount = oldCount - neckIndex;
        Vector3f[] translations = new Vector3f[newCount];
        Quaternion[] rotations = new Quaternion[newCount];
        Vector3f[] scales = null;
        if (oldScales != null) {
            scales = new Vector3f[newCount];
        }
        float[] times = new float[newCount];

        Transform user = neckTransform.clone();
        translations[0] = user.getTranslation();
        rotations[0] = user.getRotation();
        if (scales != null) {
            scales[0] = user.getScale();
        }
        times[0] = 0f;
        for (int newIndex = 1; newIndex < newCount; newIndex++) {
            int oldIndex = newIndex + neckIndex;
            translations[newIndex] = oldTranslations[oldIndex].clone();
            rotations[newIndex] = oldRotations[oldIndex].clone();
            if (scales != null) {
                scales[newIndex] = oldScales[oldIndex].clone();
            }
            times[newIndex] = oldTimes[oldIndex] - neckTime;
        }

        int boneIndex = oldTrack.getTargetBoneIndex();
        BoneTrack result = newBoneTrack(boneIndex, times, translations,
                rotations, scales);

        return result;
    }

    /**
     * Create a BoneTrack consisting of a single keyframe at t=0.
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
        BoneTrack result = newBoneTrack(boneIndex, times, translations,
                rotations, scales);

        return result;
    }

    /**
     * Copy a bone track, deleting the indexed keyframe (which mustn't be the
     * 1st).
     *
     * @param oldTrack (not null, unaffected)
     * @param frameIndex which keyframe (&gt;0, &lt;numFrames)
     * @return a new instance
     */
    public static BoneTrack deleteKeyframe(BoneTrack oldTrack, int frameIndex) {
        float[] oldTimes = oldTrack.getKeyFrameTimes();
        int oldCount = oldTimes.length;
        Validate.inRange(frameIndex, "keyframe index", 1, oldCount - 1);

        Vector3f[] oldTranslations = oldTrack.getTranslations();
        Quaternion[] oldRotations = oldTrack.getRotations();
        Vector3f[] oldScales = oldTrack.getScales();

        int newCount = oldCount - 1;
        Vector3f[] newTranslations = new Vector3f[newCount];
        Quaternion[] newRotations = new Quaternion[newCount];
        Vector3f[] newScales = null;
        if (oldScales != null) {
            newScales = new Vector3f[newCount];
        }
        float[] newTimes = new float[newCount];

        for (int newIndex = 0; newIndex < newCount; newIndex++) {
            int oldIndex = newIndex;
            if (newIndex >= frameIndex) {
                ++oldIndex;
            }
            newTranslations[newIndex] = oldTranslations[oldIndex].clone();
            newRotations[newIndex] = oldRotations[oldIndex].clone();
            if (oldScales != null) {
                newScales[newIndex] = oldScales[oldIndex].clone();
            }
            newTimes[newIndex] = oldTimes[oldIndex];
        }

        int boneIndex = oldTrack.getTargetBoneIndex();
        BoneTrack result = newBoneTrack(boneIndex, newTimes, newTranslations,
                newRotations, newScales);

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
            Skeleton skeleton = MySkeleton.findSkeleton(spatial);
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
     * Find the index of the keyframe at the specified time in the specified
     * track.
     *
     * @param track which track to search (not null, unaffected)
     * @param time track time (in seconds, &ge;0)
     * @return keyframe index (&ge;0) or -1 if keyframe not found
     */
    public static int findKeyframeIndex(Track track, float time) {
        Validate.nonNegative(time, "time");

        float[] times = track.getKeyFrameTimes();
        int result = MyArray.findPreviousIndex(time, times);
        if (result >= 0 && times[result] != time) {
            result = -1;
        }

        return result;
    }

    /**
     * Find the keyframe in the specified animation with the latest time.
     *
     * @param animation input (not null)
     * @return track time (in seconds, &ge;0)
     */
    public static float findLastKeyframe(Animation animation) {
        float maxTime = 0f;
        Track[] loadedTracks = animation.getTracks();
        for (Track track : loadedTracks) {
            float[] frameTimes = track.getKeyFrameTimes();
            for (float time : frameTimes) {
                if (time > maxTime) {
                    maxTime = time;
                }
            }
        }

        return maxTime;
    }

    /**
     * Find the index of the keyframe at or before the specified time in the
     * specified track.
     *
     * @param track which track to search (not null, unaffected)
     * @param time track time (in seconds, &ge;0)
     * @return keyframe index (&ge;0)
     */
    public static int findPreviousKeyframeIndex(Track track, float time) {
        Validate.nonNegative(time, "time");

        float[] times = track.getKeyFrameTimes();
        int result = MyArray.findPreviousIndex(time, times);

        assert result >= 0 : result;
        return result;
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
     * @return the bone's name
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
     * Test whether the specified animation includes a bone track for the
     * indexed bone.
     *
     * @param animation which animation to test (not null)
     * @param boneIndex which bone (&ge;0)
     * @return true if a track exists, otherwise false
     */
    public static boolean hasTrackForBone(Animation animation, int boneIndex) {
        Validate.nonNegative(boneIndex, "bone index");

        boolean result = false;
        Track[] tracks = animation.getTracks();
        for (Track track : tracks) {
            if (track instanceof BoneTrack) {
                BoneTrack boneTrack = (BoneTrack) track;
                int target = boneTrack.getTargetBoneIndex();
                if (target == boneIndex) {
                    result = true;
                }
            }
        }

        return result;
    }

    /**
     * Copy a bone track, inserting a keyframe at the specified time (which
     * mustn't already have a keyframe).
     *
     * @param oldTrack (not null, unaffected)
     * @param frameTime when to insert (&gt;0)
     * @param transform user transform to insert (not null, unaffected)
     * @return a new instance
     */
    public static BoneTrack insertKeyframe(BoneTrack oldTrack, float frameTime,
            Transform transform) {
        Validate.positive(frameTime, "keyframe time");
        assert findKeyframeIndex(oldTrack, frameTime) == -1;

        float[] oldTimes = oldTrack.getKeyFrameTimes();
        Vector3f[] oldTranslations = oldTrack.getTranslations();
        Quaternion[] oldRotations = oldTrack.getRotations();
        Vector3f[] oldScales = oldTrack.getScales();
        int oldCount = oldTimes.length;

        int newCount = oldCount + 1;
        Vector3f[] translations = new Vector3f[newCount];
        Quaternion[] rotations = new Quaternion[newCount];
        Vector3f[] scales = new Vector3f[newCount];
        float[] newTimes = new float[newCount];

        boolean added = false;
        for (int oldIndex = 0; oldIndex < oldCount; oldIndex++) {
            float time = oldTimes[oldIndex];
            int newIndex = oldIndex;
            if (time > frameTime) {
                if (!added) {
                    translations[newIndex] = transform.getTranslation().clone();
                    rotations[newIndex] = transform.getRotation().clone();
                    scales[newIndex] = transform.getScale().clone();
                    newTimes[newIndex] = frameTime;
                    added = true;
                }
                ++newIndex;
            }
            translations[newIndex] = oldTranslations[oldIndex].clone();
            rotations[newIndex] = oldRotations[oldIndex].clone();
            if (oldScales != null) {
                scales[newIndex] = oldScales[oldIndex].clone();
            } else {
                scales[newIndex] = new Vector3f(1f, 1f, 1f);
            }
            newTimes[newIndex] = oldTimes[oldIndex];
        }
        if (!added) {
            translations[oldCount] = transform.getTranslation().clone();
            rotations[oldCount] = transform.getRotation().clone();
            scales[oldCount] = transform.getScale().clone();
            newTimes[oldCount] = frameTime;
        }

        int boneIndex = oldTrack.getTargetBoneIndex();
        BoneTrack result = newBoneTrack(boneIndex, newTimes, translations,
                rotations, scales);

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
     * Create a new bone track, with or without scales.
     *
     * @param boneIndex (&ge;0)
     * @param times (not null, alias created)
     * @param translations (not null, same length as times)
     * @param rotations (not null, same length as times)
     * @param scales (either null or same length as times)
     * @return a new instance
     */
    public static BoneTrack newBoneTrack(int boneIndex, float[] times,
            Vector3f[] translations, Quaternion[] rotations,
            Vector3f[] scales) {
        Validate.nonNull(times, "times");
        Validate.nonNull(translations, "translations");
        Validate.nonNull(rotations, "rotations");
        int numKeyframes = times.length;
        assert translations.length == numKeyframes;
        assert rotations.length == numKeyframes;
        assert scales == null || scales.length == numKeyframes;

        BoneTrack result;
        if (scales == null) {
            result = new BoneTrack(boneIndex, times, translations, rotations);
        } else {
            result = new BoneTrack(boneIndex, times, translations, rotations,
                    scales);
        }

        return result;
    }

    /**
     * Copy a bone track, reducing the number of keyframes by the specified
     * factor.
     *
     * @param oldTrack (not null, unaffected)
     * @param factor reduction factor (&ge;2)
     * @return a new instance
     */
    public static BoneTrack reduce(BoneTrack oldTrack, int factor) {
        Validate.inRange(factor, "factor", 2, Integer.MAX_VALUE);

        Vector3f[] oldTranslations = oldTrack.getTranslations();
        Quaternion[] oldRotations = oldTrack.getRotations();
        Vector3f[] oldScales = oldTrack.getScales();
        float[] oldTimes = oldTrack.getKeyFrameTimes();
        int oldCount = oldTimes.length;
        assert oldCount > 0 : oldCount;

        int newCount = 1 + (oldCount - 1) / factor;
        Vector3f[] newTranslations = new Vector3f[newCount];
        Quaternion[] newRotations = new Quaternion[newCount];
        Vector3f[] newScales = null;
        if (oldScales != null) {
            newScales = new Vector3f[newCount];
        }
        float[] newTimes = new float[newCount];

        for (int newIndex = 0; newIndex < newCount; newIndex++) {
            int oldIndex = newIndex * factor;
            newTranslations[newIndex] = oldTranslations[oldIndex].clone();
            newRotations[newIndex] = oldRotations[oldIndex].clone();
            if (oldScales != null) {
                newScales[newIndex] = oldScales[oldIndex].clone();
            }
            newTimes[newIndex] = oldTimes[oldIndex];
        }

        int boneIndex = oldTrack.getTargetBoneIndex();
        BoneTrack result = newBoneTrack(boneIndex, newTimes, newTranslations,
                newRotations, newScales);

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
        Vector3f[] newScales = null;
        if (originalScales != null) {
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

        if (newScales == null) {
            boneTrack.setKeyframes(newTimes, newTranslations, newRotations);
        } else {
            boneTrack.setKeyframes(newTimes, newTranslations, newRotations,
                    newScales);
        }

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

    /**
     * Copy a bone track, altering its duration and adjusting all its keyframes
     * proportionately.
     *
     * @param oldTrack (not null, unaffected)
     * @param newDuration new duration (in seconds, &ge;0)
     * @return a new instance
     */
    public static BoneTrack setDuration(BoneTrack oldTrack, float newDuration) {
        Validate.nonNegative(newDuration, "duration");

        BoneTrack result = oldTrack.clone();
        float[] newTimes = result.getKeyFrameTimes(); // an alias

        float oldDuration = oldTrack.getLength();
        float[] oldTimes = oldTrack.getKeyFrameTimes();
        int numFrames = oldTimes.length;
        assert numFrames == 1 || oldDuration > 0f : numFrames;

        for (int frameIndex = 0; frameIndex < numFrames; frameIndex++) {
            float oldTime = oldTimes[frameIndex];
            assert oldTime <= oldDuration : oldTime;

            float newTime;
            if (oldDuration == 0f) {
                assert frameIndex == 0 : frameIndex;
                assert oldTime == 0f : oldTime;
                newTime = 0f;
            } else {
                newTime = newDuration * oldTime / oldDuration;
                newTime = FastMath.clamp(newTime, 0f, newDuration);
            }
            newTimes[frameIndex] = newTime;
        }

        return result;
    }

    /**
     * Copy a bone track, truncating it at the specified time.
     *
     * @param oldTrack (not null, unaffected)
     * @param endTime cutoff time (&ge;0)
     * @return a new instance
     */
    public static BoneTrack truncate(BoneTrack oldTrack, float endTime) {
        Validate.positive(endTime, "end time");

        float[] oldTimes = oldTrack.getKeyFrameTimes();
        Vector3f[] oldTranslations = oldTrack.getTranslations();
        Quaternion[] oldRotations = oldTrack.getRotations();
        Vector3f[] oldScales = oldTrack.getScales();

        int newCount = 1 + findPreviousKeyframeIndex(oldTrack, endTime);
        Vector3f[] translations = new Vector3f[newCount];
        Quaternion[] rotations = new Quaternion[newCount];
        Vector3f[] scales = null;
        if (oldScales != null) {
            scales = new Vector3f[newCount];
        }
        float[] times = new float[newCount];

        for (int frameIndex = 0; frameIndex < newCount; frameIndex++) {
            translations[frameIndex] = oldTranslations[frameIndex].clone();
            rotations[frameIndex] = oldRotations[frameIndex].clone();
            if (oldScales != null) {
                scales[frameIndex] = oldScales[frameIndex].clone();
            }
            times[frameIndex] = oldTimes[frameIndex];
        }

        int boneIndex = oldTrack.getTargetBoneIndex();
        BoneTrack result = newBoneTrack(boneIndex, times, translations,
                rotations, scales);

        return result;
    }

    /**
     * Copy a bone track, altering its end-time keyframe to match its 1st
     * keyframe. If the track doesn't end with a keyframe, append one.
     *
     * @param oldTrack (not null, unaffected)
     * @param endTime when to insert (&gt;0)
     * @return a new instance
     */
    public static BoneTrack wrap(BoneTrack oldTrack, float endTime) {
        Validate.positive(endTime, "end time");

        float[] oldTimes = oldTrack.getKeyFrameTimes();
        Vector3f[] oldTranslations = oldTrack.getTranslations();
        Quaternion[] oldRotations = oldTrack.getRotations();
        Vector3f[] oldScales = oldTrack.getScales();
        int oldCount = oldTimes.length;

        int newCount;
        int endIndex = findKeyframeIndex(oldTrack, endTime);
        if (endIndex == -1) {
            endIndex = oldCount;
            newCount = oldCount + 1;
        } else {
            newCount = oldCount;
        }
        assert endIndex == newCount - 1;
        Vector3f[] translations = new Vector3f[newCount];
        Quaternion[] rotations = new Quaternion[newCount];
        Vector3f[] scales = null;
        if (oldScales != null) {
            scales = new Vector3f[newCount];
        }
        float[] newTimes = new float[newCount];

        for (int frameIndex = 0; frameIndex < endIndex; frameIndex++) {
            translations[frameIndex] = oldTranslations[frameIndex].clone();
            rotations[frameIndex] = oldRotations[frameIndex].clone();
            if (oldScales != null) {
                scales[frameIndex] = oldScales[frameIndex].clone();
            }
            newTimes[frameIndex] = oldTimes[frameIndex];
        }

        translations[endIndex] = oldTranslations[0].clone();
        rotations[endIndex] = oldRotations[0].clone();
        if (oldScales != null) {
            scales[endIndex] = oldScales[0].clone();
        }
        newTimes[endIndex] = endTime;

        int boneIndex = oldTrack.getTargetBoneIndex();
        BoneTrack result = newBoneTrack(boneIndex, newTimes, translations,
                rotations, scales);

        return result;
    }

    /**
     * Repair all tracks in which the 1st keyframe isn't at time=0.
     *
     * @param animation (not null)
     * @return number of tracks edited (&ge;0)
     */
    public static int zeroFirst(Animation animation) {
        int numTracksEdited = 0;
        Track[] tracks = animation.getTracks();
        for (Track track : tracks) {
            float[] times = track.getKeyFrameTimes();
            if (times[0] != 0f) {
                times[0] = 0f;
                ++numTracksEdited;
            }
        }

        return numTracksEdited;
    }
}
