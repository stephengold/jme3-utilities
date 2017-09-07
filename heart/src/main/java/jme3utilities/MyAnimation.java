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

import com.jme3.animation.AnimControl;
import com.jme3.animation.Animation;
import com.jme3.animation.AudioTrack;
import com.jme3.animation.Bone;
import com.jme3.animation.BoneTrack;
import com.jme3.animation.EffectTrack;
import com.jme3.animation.Skeleton;
import com.jme3.animation.SpatialTrack;
import com.jme3.animation.Track;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import java.util.Collection;
import java.util.TreeSet;
import java.util.logging.Logger;
import jme3utilities.math.MyArray;

/**
 * Utility methods for manipulating animations and tracks. All methods should be
 * static.
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
     * Count all tracks of the specified type in the specified animation.
     *
     * @param <T> superclass of Track
     * @param animation animation to search (may be null, unaffected)
     * @param trackType superclass of Track to search for
     * @return number of tracks (&ge;0)
     */
    public static <T extends Track> int countTracks(Animation animation,
            Class<T> trackType) {
        int result = 0;

        if (animation != null) {
            Track[] tracks = animation.getTracks();
            for (Track track : tracks) {
                if (trackType.isAssignableFrom(track.getClass())) {
                    ++result;
                }
            }
        }

        assert result >= 0 : result;
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
     * Find a BoneTrack in a specified animation for a specified bone.
     *
     * @param animation which animation (not null, unaffected)
     * @param boneIndex which bone (&ge;0)
     * @return the pre-existing instance, or null if not found
     */
    public static BoneTrack findBoneTrack(Animation animation, int boneIndex) {
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
     * Access the keyframe rotations for the specified bone/spatial track.
     *
     * @param track which track (not null, unaffected)
     * @return the pre-existing instance (may be null)
     */
    public static Quaternion[] getRotations(Track track) {
        Quaternion[] result;
        if (track instanceof BoneTrack) {
            BoneTrack boneTrack = (BoneTrack) track;
            result = boneTrack.getRotations();

        } else if (track instanceof SpatialTrack) {
            SpatialTrack spatialTrack = (SpatialTrack) track;
            result = spatialTrack.getRotations();

        } else {
            throw new IllegalArgumentException();
        }

        return result;
    }

    /**
     * Access the keyframe scales for the specified bone/spatial track.
     *
     * @param track which track (not null, unaffected)
     * @return the pre-existing instance (not null)
     */
    public static Vector3f[] getScales(Track track) {
        Vector3f[] result;
        if (track instanceof BoneTrack) {
            BoneTrack boneTrack = (BoneTrack) track;
            result = boneTrack.getScales();

        } else if (track instanceof SpatialTrack) {
            SpatialTrack spatialTrack = (SpatialTrack) track;
            result = spatialTrack.getScales();

        } else {
            throw new IllegalArgumentException();
        }

        assert result != null;
        return result;
    }

    /**
     * Access the translations for the specified bone/spatial track.
     *
     * @param track which track (not null, unaffected)
     * @return the pre-existing instance (not null)
     */
    public static Vector3f[] getTranslations(Track track) {
        Vector3f[] result;
        if (track instanceof BoneTrack) {
            BoneTrack boneTrack = (BoneTrack) track;
            result = boneTrack.getTranslations();

        } else if (track instanceof SpatialTrack) {
            SpatialTrack spatialTrack = (SpatialTrack) track;
            result = spatialTrack.getTranslations();

        } else {
            throw new IllegalArgumentException();
        }

        assert result != null;
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
     * Create a BoneTrack consisting of a single keyframe at t=0.
     *
     * @param boneIndex which bone (&ge;0)
     * @param translation relative to bind pose (not null, unaffected)
     * @param rotation relative to bind pose (not null, unaffected)
     * @param scale relative to bind pose (not null, unaffected)
     * @return a new instance
     */
    public static BoneTrack newBoneTrack(int boneIndex, Vector3f translation,
            Quaternion rotation, Vector3f scale) {
        Validate.nonNegative(boneIndex, "bone index");

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
     * Create a BoneTrack in which all keyframes have the same transform.
     *
     * @param boneIndex which bone (&ge;0)
     * @param frameTimes (not null, unaffected)
     * @param transform (not null, unaffected)
     * @return a new instance
     */
    public static BoneTrack newBoneTrack(int boneIndex, float[] frameTimes,
            Transform transform) {
        Validate.nonNegative(boneIndex, "bone index");

        int numFrames = frameTimes.length;
        float[] times = new float[numFrames];
        Vector3f[] translations = new Vector3f[numFrames];
        Quaternion[] rotations = new Quaternion[numFrames];
        Vector3f[] scales = new Vector3f[numFrames];
        transform = transform.clone();

        for (int frameIndex = 0; frameIndex < numFrames; frameIndex++) {
            times[frameIndex] = frameTimes[frameIndex];
            translations[frameIndex] = transform.getTranslation();
            rotations[frameIndex] = transform.getRotation();
            scales[frameIndex] = transform.getScale();
        }
        BoneTrack result = newBoneTrack(boneIndex, times, translations,
                rotations, scales);

        return result;
    }
}
