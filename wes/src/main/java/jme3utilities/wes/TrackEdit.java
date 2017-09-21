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
package jme3utilities.wes;

import com.jme3.animation.Animation;
import com.jme3.animation.Bone;
import com.jme3.animation.BoneTrack;
import com.jme3.animation.Skeleton;
import com.jme3.animation.SpatialTrack;
import com.jme3.animation.Track;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.plugins.bvh.BoneMapping;
import com.jme3.scene.plugins.bvh.SkeletonMapping;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;
import jme3utilities.MyAnimation;
import jme3utilities.Validate;

/**
 * Utility methods for track/animation editing. All methods should be static.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class TrackEdit {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            TrackEdit.class.getName());
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private TrackEdit() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Copy a bone/spatial track, deleting everything before the specified time
     * and making that the start of the track.
     *
     * @param oldTrack input bone/spatial track (not null, unaffected)
     * @param neckTime cutoff time (in seconds, &gt;0)
     * @param neckTransform transform of bone/spatial at the neck time (not
     * null, unaffected)
     * @param oldDuration (in seconds, &ge;neckTime)
     * @return a new instance
     */
    public static Track behead(Track oldTrack, float neckTime,
            Transform neckTransform, float oldDuration) {
        assert oldTrack instanceof BoneTrack
                || oldTrack instanceof SpatialTrack;
        Validate.positive(neckTime, "neck time");

        float[] oldTimes = oldTrack.getKeyFrameTimes();
        Vector3f[] oldTranslations = MyAnimation.getTranslations(oldTrack);
        Quaternion[] oldRotations = MyAnimation.getRotations(oldTrack);
        Vector3f[] oldScales = MyAnimation.getScales(oldTrack);

        int oldCount = oldTimes.length;
        int neckIndex;
        neckIndex = MyAnimation.findPreviousKeyframeIndex(oldTrack, neckTime);
        int newCount = oldCount - neckIndex;
        assert newCount > 0 : newCount;
        /*
         * Allocate new arrays.
         */
        float[] newTimes = new float[newCount];
        newTimes[0] = 0f;
        Vector3f[] newTranslations = null;
        if (oldTranslations != null) {
            newTranslations = new Vector3f[newCount];
            newTranslations[0] = neckTransform.getTranslation().clone();
        }
        Quaternion[] newRotations = null;
        if (oldRotations != null) {
            newRotations = new Quaternion[newCount];
            newRotations[0] = neckTransform.getRotation().clone();
        }
        Vector3f[] newScales = null;
        if (oldScales != null) {
            newScales = new Vector3f[newCount];
            newScales[0] = neckTransform.getScale().clone();
        }

        for (int newIndex = 1; newIndex < newCount; newIndex++) {
            int oldIndex = newIndex + neckIndex;

            newTimes[newIndex] = oldTimes[oldIndex] - neckTime;
            if (newTranslations != null) {
                newTranslations[newIndex] = oldTranslations[oldIndex].clone();
            }
            if (newRotations != null) {
                newRotations[newIndex] = oldRotations[oldIndex].clone();
            }
            if (newScales != null) {
                newScales[newIndex] = oldScales[oldIndex].clone();
            }
        }

        Track result = newTrack(oldTrack, newTimes, newTranslations,
                newRotations, newScales);

        return result;
    }

    /**
     * Copy a bone/spatial track, deleting the indexed range of keyframes (which
     * mustn't include the 1st keyframe).
     *
     * @param oldTrack input bone/spatial track (not null, unaffected)
     * @param startIndex 1st keyframe to delete (&gt;0, &le;lastIndex)
     * @param deleteCount number of keyframes to delete (&gt;0, &lt;lastIndex)
     * @return a new instance
     */
    public static Track deleteRange(Track oldTrack, int startIndex,
            int deleteCount) {
        assert oldTrack instanceof BoneTrack
                || oldTrack instanceof SpatialTrack;
        float[] oldTimes = oldTrack.getKeyFrameTimes();
        int oldCount = oldTimes.length;
        int lastIndex = oldCount - 1;
        Validate.inRange(startIndex, "start index", 1, lastIndex);
        Validate.inRange(deleteCount, "delete count", 1, lastIndex);
        float endIndex = startIndex + deleteCount - 1;
        Validate.inRange(endIndex, "end index", 1, lastIndex);

        Vector3f[] oldTranslations = MyAnimation.getTranslations(oldTrack);
        Quaternion[] oldRotations = MyAnimation.getRotations(oldTrack);
        Vector3f[] oldScales = MyAnimation.getScales(oldTrack);

        int newCount = oldCount - deleteCount;
        /*
         * Allocate new arrays.
         */
        float[] newTimes = new float[newCount];
        Vector3f[] newTranslations = null;
        if (oldTranslations != null) {
            newTranslations = new Vector3f[newCount];
        }
        Quaternion[] newRotations = null;
        if (oldRotations != null) {
            newRotations = new Quaternion[newCount];
        }
        Vector3f[] newScales = null;
        if (oldScales != null) {
            newScales = new Vector3f[newCount];
        }

        for (int newIndex = 0; newIndex < newCount; newIndex++) {
            int oldIndex;
            if (newIndex < startIndex) {
                oldIndex = newIndex;
            } else {
                oldIndex = newIndex + deleteCount;
            }

            newTimes[newIndex] = oldTimes[oldIndex];
            if (newTranslations != null) {
                newTranslations[newIndex] = oldTranslations[oldIndex].clone();
            }
            if (newRotations != null) {
                newRotations[newIndex] = oldRotations[oldIndex].clone();
            }
            if (newScales != null) {
                newScales[newIndex] = oldScales[oldIndex].clone();
            }
        }

        Track result = newTrack(oldTrack, newTimes, newTranslations,
                newRotations, newScales);

        return result;
    }

    /**
     * Copy a bone/spatial track, inserting a keyframe at the specified time
     * (which mustn't already have a keyframe).
     *
     * @param oldTrack input bone/spatial track (not null, unaffected)
     * @param frameTime when to insert (&gt;0)
     * @param transform transform to insert (not null, unaffected)
     * @return a new instance
     */
    public static Track insertKeyframe(Track oldTrack, float frameTime,
            Transform transform) {
        assert oldTrack instanceof BoneTrack
                || oldTrack instanceof SpatialTrack;
        Validate.positive(frameTime, "keyframe time");
        assert MyAnimation.findKeyframeIndex(oldTrack, frameTime) == -1;

        float[] oldTimes = oldTrack.getKeyFrameTimes();
        Vector3f[] oldTranslations = MyAnimation.getTranslations(oldTrack);
        Quaternion[] oldRotations = MyAnimation.getRotations(oldTrack);
        Vector3f[] oldScales = MyAnimation.getScales(oldTrack);

        int oldCount = oldTimes.length;
        int newCount = oldCount + 1;
        /*
         * Allocate new arrays.
         */
        float[] newTimes = new float[newCount];
        Vector3f[] newTranslations = new Vector3f[newCount];
        Quaternion[] newRotations = new Quaternion[newCount];
        Vector3f[] newScales = new Vector3f[newCount];

        boolean added = false;
        for (int oldIndex = 0; oldIndex < oldCount; oldIndex++) {
            float time = oldTimes[oldIndex];
            int newIndex = oldIndex;
            if (time > frameTime) {
                if (!added) {
                    newTimes[newIndex] = frameTime;
                    newTranslations[newIndex] = transform.getTranslation().clone();
                    newRotations[newIndex] = transform.getRotation().clone();
                    newScales[newIndex] = transform.getScale().clone();
                    added = true;
                }
                ++newIndex;
            }

            newTimes[newIndex] = oldTimes[oldIndex];
            if (oldTranslations == null) {
                newTranslations[newIndex] = new Vector3f();
            } else {
                newTranslations[newIndex] = oldTranslations[oldIndex].clone();
            }
            if (oldRotations == null) {
                newRotations[newIndex] = new Quaternion();
            } else {
                newRotations[newIndex] = oldRotations[oldIndex].clone();
            }
            if (oldScales == null) {
                newScales[newIndex] = new Vector3f(1f, 1f, 1f);
            } else {
                newScales[newIndex] = oldScales[oldIndex].clone();
            }
        }
        if (!added) {
            newTimes[oldCount] = frameTime;
            newTranslations[oldCount] = transform.getTranslation().clone();
            newRotations[oldCount] = transform.getRotation().clone();
            newScales[oldCount] = transform.getScale().clone();
        }

        Track result = newTrack(oldTrack, newTimes, newTranslations,
                newRotations, newScales);

        return result;
    }

    /**
     * Create a new bone/spatial track.
     *
     * @param oldTrack to identify the track type and target bone/spatial (not
     * null)
     * @param times (not null, alias created)
     * @param translations (either null or same length as times)
     * @param rotations (either null or same length as times)
     * @param scales (either null or same length as times)
     * @return a new instance of the same type as oldTrack
     */
    public static Track newTrack(Track oldTrack, float[] times,
            Vector3f[] translations, Quaternion[] rotations,
            Vector3f[] scales) {
        Validate.nonNull(times, "times");
        int numKeyframes = times.length;
        assert translations == null || translations.length == numKeyframes;
        assert rotations == null || rotations.length == numKeyframes;
        assert scales == null || scales.length == numKeyframes;

        Track result;
        if (oldTrack instanceof BoneTrack) {
            BoneTrack boneTrack = (BoneTrack) oldTrack;
            int boneIndex = boneTrack.getTargetBoneIndex();
            result = MyAnimation.newBoneTrack(boneIndex, times, translations,
                    rotations, scales);

        } else if (oldTrack instanceof SpatialTrack) {
            //SpatialTrack spatialTrack = (SpatialTrack) oldTrack; // TODO JME 3.2
            //Spatial spatial = spatialTrack.getTrackSpatial();
            SpatialTrack newSpatialTrack = new SpatialTrack(times, translations,
                    rotations, scales);
            //newSpatialTrack.setTrackSpatial(spatial);
            result = newSpatialTrack;

        } else {
            throw new IllegalArgumentException();
        }

        return result;
    }

    /**
     * Copy a bone/spatial track, uniformly reducing the number of keyframes by
     * the specified factor.
     *
     * @param oldTrack input bone/spatial track (not null, unaffected)
     * @param factor reduction factor (&ge;2)
     * @return a new instance
     */
    public static Track reduce(Track oldTrack, int factor) {
        assert oldTrack instanceof BoneTrack
                || oldTrack instanceof SpatialTrack;
        Validate.inRange(factor, "factor", 2, Integer.MAX_VALUE);

        float[] oldTimes = oldTrack.getKeyFrameTimes();
        Vector3f[] oldTranslations = MyAnimation.getTranslations(oldTrack);
        Quaternion[] oldRotations = MyAnimation.getRotations(oldTrack);
        Vector3f[] oldScales = MyAnimation.getScales(oldTrack);

        int oldCount = oldTimes.length;
        assert oldCount > 0 : oldCount;
        int newCount = 1 + (oldCount - 1) / factor;
        /*
         * Allocate new arrays.
         */
        float[] newTimes = new float[newCount];
        Vector3f[] newTranslations = null;
        if (oldTranslations != null) {
            newTranslations = new Vector3f[newCount];
        }
        Quaternion[] newRotations = null;
        if (oldRotations != null) {
            newRotations = new Quaternion[newCount];
        }
        Vector3f[] newScales = null;
        if (oldScales != null) {
            newScales = new Vector3f[newCount];
        }

        for (int newIndex = 0; newIndex < newCount; newIndex++) {
            int oldIndex = newIndex * factor;

            newTimes[newIndex] = oldTimes[oldIndex];
            if (newTranslations != null) {
                newTranslations[newIndex] = oldTranslations[oldIndex].clone();
            }
            if (newRotations != null) {
                newRotations[newIndex] = oldRotations[oldIndex].clone();
            }
            if (newScales != null) {
                newScales[newIndex] = oldScales[oldIndex].clone();
            }
        }

        Track result = newTrack(oldTrack, newTimes, newTranslations,
                newRotations, newScales);

        return result;
    }

    /**
     * Remove repetitious keyframes from an animation.
     *
     * @param animation (not null, modified)
     * @return number of tracks edited
     */
    public static int removeRepeats(Animation animation) {
        int numTracksEdited = 0;
        Track[] tracks = animation.getTracks();
        for (Track track : tracks) {
            if (track instanceof BoneTrack || track instanceof SpatialTrack) {
                boolean removed = removeRepeats(track);
                if (removed) {
                    ++numTracksEdited;
                }
            } // TODO other track types
        }

        return numTracksEdited;
    }

    /**
     * Remove all repetitious keyframes from a bone/spatial track.
     *
     * @param track input bone/spatial track (not null, modified)
     * @return true if 1 or more keyframes were removed, otherwise false
     */
    public static boolean removeRepeats(Track track) {
        assert track instanceof BoneTrack || track instanceof SpatialTrack;

        float[] oldTimes = track.getKeyFrameTimes();
        /*
         * Count distinct keyframes.
         */
        float prevTime = Float.NEGATIVE_INFINITY;
        int newCount = 0;
        for (float time : oldTimes) {
            if (time != prevTime) {
                ++newCount;
            }
            prevTime = time;
        }

        int oldCount = oldTimes.length;
        if (newCount == oldCount) {
            return false;
        }

        Vector3f[] oldTranslations = MyAnimation.getTranslations(track);
        Quaternion[] oldRotations = MyAnimation.getRotations(track);
        Vector3f[] oldScales = MyAnimation.getScales(track);
        /*
         * Allocate new arrays.
         */
        float[] newTimes = new float[newCount];
        Vector3f[] newTranslations = null;
        if (oldTranslations != null) {
            newTranslations = new Vector3f[newCount];
        }
        Quaternion[] newRotations = null;
        if (oldRotations != null) {
            newRotations = new Quaternion[newCount];
        }
        Vector3f[] newScales = null;
        if (oldScales != null) {
            newScales = new Vector3f[newCount];
        }
        /*
         * Copy all non-repeated keyframes.
         */
        prevTime = Float.NEGATIVE_INFINITY;
        int newIndex = 0;
        for (int oldIndex = 0; oldIndex < oldCount; oldIndex++) {
            float time = oldTimes[oldIndex];
            if (time != prevTime) {
                newTimes[newIndex] = oldTimes[oldIndex];
                if (newTranslations != null) {
                    newTranslations[newIndex] = oldTranslations[oldIndex];
                }
                if (newRotations != null) {
                    newRotations[newIndex] = oldRotations[oldIndex];
                }
                if (newScales != null) {
                    newScales[newIndex] = oldScales[oldIndex];
                }
                ++newIndex;
            }
            prevTime = time;
        }

        setKeyframes(track, newTimes, newTranslations, newRotations, newScales);
        return true;
    }

    /**
     * Copy a bone/spatial track, setting the indexed keyframe to the specified
     * transform.
     *
     * @param oldTrack input bone/spatial track (not null, unaffected)
     * @param frameIndex which keyframe (&ge;0, &lt;numFrames)
     * @param transform transform to apply (not null, unaffected)
     * @return a new instance
     */
    public static Track replaceKeyframe(Track oldTrack, int frameIndex,
            Transform transform) {
        assert oldTrack instanceof BoneTrack
                || oldTrack instanceof SpatialTrack;
        float[] oldTimes = oldTrack.getKeyFrameTimes();
        int frameCount = oldTimes.length;
        Validate.inRange(frameIndex, "keyframe index", 0, frameCount - 1);
        Validate.nonNull(transform, "transform");

        Vector3f[] oldTranslations = MyAnimation.getTranslations(oldTrack);
        Quaternion[] oldRotations = MyAnimation.getRotations(oldTrack);
        Vector3f[] oldScales = MyAnimation.getScales(oldTrack);
        /*
         * Allocate new arrays.
         */
        float[] newTimes = new float[frameCount];
        Vector3f[] newTranslations = new Vector3f[frameCount];
        Quaternion[] newRotations = new Quaternion[frameCount];
        Vector3f[] newScales = new Vector3f[frameCount];

        for (int frameI = 0; frameI < frameCount; frameI++) {
            newTimes[frameI] = oldTimes[frameI];
            if (frameI == frameIndex) {
                newTranslations[frameI] = transform.getTranslation().clone();
                newRotations[frameI] = transform.getRotation().clone();
                newScales[frameI] = transform.getScale().clone();
            } else {
                if (oldTranslations == null) {
                    newTranslations[frameI] = new Vector3f();
                } else {
                    newTranslations[frameI] = oldTranslations[frameI].clone();
                }
                if (oldRotations == null) {
                    newRotations[frameI] = new Quaternion();
                } else {
                    newRotations[frameI] = oldRotations[frameI].clone();
                }
                if (oldScales == null) {
                    newScales[frameI] = new Vector3f(1f, 1f, 1f);
                } else {
                    newScales[frameI] = oldScales[frameI].clone();
                }
            }
        }

        Track result = newTrack(oldTrack, newTimes, newTranslations,
                newRotations, newScales);

        return result;
    }

    /**
     * Re-target the specified animation from the specified source skeleton to
     * the specified target skeleton using the specified map.
     *
     * @param sourceAnimation which animation to re-target (not null,
     * unaffected)
     * @param sourceSkeleton (not null, unaffected)
     * @param targetSkeleton (not null, unaffected)
     * @param map skeleton map to use (not null, unaffected)
     * @param techniques tweening techniques to use (not null, unaffected)
     * @param animationName name for the resulting animation (not null)
     * @return a new animation
     */
    public static Animation retargetAnimation(Animation sourceAnimation,
            Skeleton sourceSkeleton, Skeleton targetSkeleton,
            SkeletonMapping map, TweenTransforms techniques,
            String animationName) {
        Validate.nonNull(sourceSkeleton, "source skeleton");
        Validate.nonNull(map, "map");
        Validate.nonNull(techniques, "techniques");
        Validate.nonNull(animationName, "animation name");
        /*
         * Start with an empty animation.
         */
        float duration = sourceAnimation.getLength();
        Animation result = new Animation(animationName, duration);

        Map<Float, Pose> cache = new TreeMap<>();
        /*
         * Add a bone track for each target bone that's mapped.
         */
        int numTargetBones = targetSkeleton.getBoneCount();
        for (int iTarget = 0; iTarget < numTargetBones; iTarget++) {
            Bone targetBone = targetSkeleton.getBone(iTarget);
            String targetName = targetBone.getName();
            BoneMapping boneMapping = map.get(targetName);
            if (boneMapping != null) {
                String sourceName = boneMapping.getSourceName();
                int iSource = sourceSkeleton.getBoneIndex(sourceName);
                BoneTrack sourceTrack = MyAnimation.findBoneTrack(
                        sourceAnimation, iSource);
                BoneTrack track = retargetTrack(sourceAnimation, sourceTrack,
                        sourceSkeleton, targetSkeleton, iTarget, map,
                        techniques, cache);
                result.addTrack(track);
            }
        }
        /*
         * Copy any non-bone tracks.
         */
        Track[] tracks = sourceAnimation.getTracks();
        for (Track track : tracks) {
            if (!(track instanceof BoneTrack)) {
                Track clone = track.clone();
                result.addTrack(clone);
            }
        }

        return result;
    }

    /**
     * Re-target the specified bone track from the specified source skeleton to
     * the specified target skeleton using the specified map.
     *
     * @param sourceAnimation the animation to re-target, or null for bind pose
     * @param sourceTrack input bone track (not null, unaffected)
     * @param sourceSkeleton (not null, unaffected)
     * @param targetSkeleton (not null, unaffected)
     * @param targetBoneIndex index of the target bone (&ge;0)
     * @param map skeleton map to use (not null, unaffected)
     * @param techniques tweening techniques to use (not null, unaffected)
     * @param cache previously calculated poses (not null, added to)
     * @return a new bone track
     */
    public static BoneTrack retargetTrack(Animation sourceAnimation,
            BoneTrack sourceTrack, Skeleton sourceSkeleton,
            Skeleton targetSkeleton, int targetBoneIndex, SkeletonMapping map,
            TweenTransforms techniques, Map<Float, Pose> cache) {
        Validate.nonNull(sourceSkeleton, "source skeleton");
        Validate.nonNull(targetSkeleton, "target skeleton");
        Validate.nonNull(map, "map");
        Validate.nonNull(techniques, "techniques");
        Validate.nonNegative(targetBoneIndex, "target bone index");

        float[] times;
        int numKeyframes;
        if (sourceTrack == null) {
            numKeyframes = 1;
            times = new float[numKeyframes];
            times[0] = 0f;
        } else {
            times = sourceTrack.getKeyFrameTimes();
            numKeyframes = times.length;
        }
        Vector3f[] translations = new Vector3f[numKeyframes];
        Quaternion[] rotations = new Quaternion[numKeyframes];
        Vector3f[] scales = new Vector3f[numKeyframes];
        Pose sourcePose = new Pose(sourceSkeleton);

        for (int frameIndex = 0; frameIndex < numKeyframes; frameIndex++) {
            float trackTime = times[frameIndex];
            Pose targetPose = cache.get(trackTime);
            if (targetPose == null) {
                targetPose = new Pose(targetSkeleton);
                sourcePose.setToAnimation(sourceAnimation, trackTime,
                        techniques);
                targetPose.setToRetarget(sourcePose, map);
                cache.put(trackTime, targetPose);
            }
            Transform userTransform;
            userTransform = targetPose.userTransform(targetBoneIndex, null);
            translations[frameIndex] = userTransform.getTranslation();
            rotations[frameIndex] = userTransform.getRotation();
            scales[frameIndex] = userTransform.getScale();
        }

        BoneTrack result = new BoneTrack(targetBoneIndex, times, translations,
                rotations, scales);

        return result;
    }

    /**
     * Copy a track, altering its duration and adjusting all its keyframe times
     * proportionately.
     *
     * @param oldTrack input track (not null, unaffected)
     * @param newDuration new duration (in seconds, &ge;0)
     * @return a new instance
     */
    public static Track setDuration(Track oldTrack, float newDuration) {
        Validate.nonNegative(newDuration, "duration");

        float oldDuration = oldTrack.getLength();
        float[] oldTimes = oldTrack.getKeyFrameTimes();
        int numFrames = oldTimes.length;
        assert numFrames == 1 || oldDuration > 0f : numFrames;

        Track result = oldTrack.clone();
        float[] newTimes = result.getKeyFrameTimes(); // an alias

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
     * Alter the keyframes in a bone/spatial track.
     *
     * @param track (not null, modified)
     * @param times (not null, alias created)
     * @param translations (either null or same length as times)
     * @param rotations (either null or same length as times)
     * @param scales (either null or same length as times)
     */
    public static void setKeyframes(Track track, float[] times,
            Vector3f[] translations, Quaternion[] rotations,
            Vector3f[] scales) {
        Validate.nonNull(times, "times");
        int numKeyframes = times.length;
        assert translations == null || translations.length == numKeyframes;
        assert rotations == null || rotations.length == numKeyframes;
        assert scales == null || scales.length == numKeyframes;

        if (track instanceof BoneTrack) {
            BoneTrack boneTrack = (BoneTrack) track;
            if (scales == null) {
                boneTrack.setKeyframes(times, translations, rotations);
            } else {
                boneTrack.setKeyframes(times, translations, rotations, scales);
            }

        } else if (track instanceof SpatialTrack) {
            SpatialTrack spatialTrack = (SpatialTrack) track;
            spatialTrack.setKeyframes(times, translations, rotations, scales);

        } else {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Copy a bone/spatial track, smoothing it using the specified techniques.
     *
     * @param oldTrack input bone/spatial track (not null, unaffected)
     * @param width width of time window (&ge;0, &le;duration)
     * @param smoothTranslations technique for translations (not null)
     * @param smoothRotations technique for translations (not null)
     * @param smoothScales technique for scales (not null)
     * @param duration animation duration (in seconds, &ge;0)
     * @return a new instance
     */
    public static Track smooth(Track oldTrack, float width,
            SmoothVectors smoothTranslations, SmoothRotations smoothRotations,
            SmoothVectors smoothScales, float duration) {
        assert oldTrack instanceof BoneTrack
                || oldTrack instanceof SpatialTrack;
        Validate.inRange(width, "width", 0f, duration);
        Validate.nonNegative(duration, "duration");

        float[] oldTimes = oldTrack.getKeyFrameTimes();
        Vector3f[] oldTranslations = MyAnimation.getTranslations(oldTrack);
        Quaternion[] oldRotations = MyAnimation.getRotations(oldTrack);
        Vector3f[] oldScales = MyAnimation.getScales(oldTrack);

        int numFrames = oldTimes.length;
        float[] newTimes = new float[numFrames];
        for (int i = 0; i < numFrames; i++) {
            newTimes[i] = oldTimes[i];
        }

        Vector3f[] newTranslations = null;
        if (oldTranslations != null) {
            newTranslations = smoothTranslations.smooth(oldTimes, duration,
                    oldTranslations, width, null);
        }

        Quaternion[] newRotations = null;
        if (oldRotations != null) {
            newRotations = smoothRotations.smooth(oldTimes, duration,
                    oldRotations, width, null);
        }

        Vector3f[] newScales = null;
        if (oldScales != null) {
            newScales = smoothScales.smooth(oldTimes, duration, oldScales,
                    width, null);
        }

        Track result = newTrack(oldTrack, newTimes, newTranslations,
                newRotations, newScales);

        return result;
    }

    /**
     * Copy a bone/spatial track, truncating it at the specified time.
     *
     * @param oldTrack input bone/spatial track (not null, unaffected)
     * @param endTime cutoff time (&ge;0)
     * @return a new instance
     */
    public static Track truncate(Track oldTrack, float endTime) {
        assert oldTrack instanceof BoneTrack
                || oldTrack instanceof SpatialTrack;
        Validate.positive(endTime, "end time");

        float[] oldTimes = oldTrack.getKeyFrameTimes();
        Vector3f[] oldTranslations = MyAnimation.getTranslations(oldTrack);
        Quaternion[] oldRotations = MyAnimation.getRotations(oldTrack);
        Vector3f[] oldScales = MyAnimation.getScales(oldTrack);

        int newCount;
        newCount = 1 + MyAnimation.findPreviousKeyframeIndex(oldTrack, endTime);
        /*
         * Allocate new arrays.
         */
        float[] newTimes = new float[newCount];
        Vector3f[] newTranslations = null;
        if (oldTranslations != null) {
            newTranslations = new Vector3f[newCount];
        }
        Quaternion[] newRotations = null;
        if (oldRotations != null) {
            newRotations = new Quaternion[newCount];
        }
        Vector3f[] newScales = null;
        if (oldScales != null) {
            newScales = new Vector3f[newCount];
        }

        for (int frameIndex = 0; frameIndex < newCount; frameIndex++) {
            newTimes[frameIndex] = oldTimes[frameIndex];
            if (newTranslations != null) {
                newTranslations[frameIndex] = oldTranslations[frameIndex].clone();
            }
            if (newRotations != null) {
                newRotations[frameIndex] = oldRotations[frameIndex].clone();
            }
            if (newScales != null) {
                newScales[frameIndex] = oldScales[frameIndex].clone();
            }
        }

        Track result = newTrack(oldTrack, newTimes, newTranslations,
                newRotations, newScales);

        return result;
    }

    /**
     * Copy a bone/spatial track, altering its end-time keyframe to match its
     * 1st keyframe. If the track doesn't end with a keyframe, append one.
     *
     * @param oldTrack input bone/spatial track (not null, unaffected)
     * @param endTime when to insert (&gt;0)
     * @return a new instance
     */
    public static Track wrap(Track oldTrack, float endTime) {
        assert oldTrack instanceof BoneTrack
                || oldTrack instanceof SpatialTrack;
        Validate.positive(endTime, "end time");

        float[] oldTimes = oldTrack.getKeyFrameTimes();
        Vector3f[] oldTranslations = MyAnimation.getTranslations(oldTrack);
        Quaternion[] oldRotations = MyAnimation.getRotations(oldTrack);
        Vector3f[] oldScales = MyAnimation.getScales(oldTrack);

        int oldCount = oldTimes.length;
        int newCount;
        int endIndex = MyAnimation.findKeyframeIndex(oldTrack, endTime);
        if (endIndex == -1) {
            endIndex = oldCount;
            newCount = oldCount + 1;
        } else {
            newCount = oldCount;
        }
        assert endIndex == newCount - 1;
        /*
         * Allocate new arrays.
         */
        float[] newTimes = new float[newCount];
        newTimes[endIndex] = endTime;
        Vector3f[] newTranslations = null;
        if (oldTranslations != null) {
            newTranslations = new Vector3f[newCount];
            newTranslations[endIndex] = oldTranslations[0].clone();
        }
        Quaternion[] newRotations = null;
        if (oldRotations != null) {
            newRotations = new Quaternion[newCount];
            newRotations[endIndex] = oldRotations[0].clone();
        }
        Vector3f[] newScales = null;
        if (oldScales != null) {
            newScales = new Vector3f[newCount];
            newScales[endIndex] = oldScales[0].clone();
        }

        for (int frameIndex = 0; frameIndex < endIndex; frameIndex++) {
            newTimes[frameIndex] = oldTimes[frameIndex];
            if (newTranslations != null) {
                newTranslations[frameIndex] = oldTranslations[frameIndex].clone();
            }
            if (newRotations != null) {
                newRotations[frameIndex] = oldRotations[frameIndex].clone();
            }
            if (newScales != null) {
                newScales[frameIndex] = oldScales[frameIndex].clone();
            }
        }

        Track result = newTrack(oldTrack, newTimes, newTranslations,
                newRotations, newScales);

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
            float[] times = track.getKeyFrameTimes(); // an alias
            if (times[0] != 0f) {
                times[0] = 0f;
                ++numTracksEdited;
            }
        }

        return numTracksEdited;
    }
}
