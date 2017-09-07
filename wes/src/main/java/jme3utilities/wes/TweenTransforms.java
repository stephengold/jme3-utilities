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

import com.jme3.animation.BoneTrack;
import com.jme3.animation.SpatialTrack;
import com.jme3.animation.Track;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import java.util.logging.Logger;
import jme3utilities.MyAnimation;
import jme3utilities.Validate;

/**
 * Tweening techniques for time sequences of JME transforms.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class TweenTransforms implements Cloneable {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            TweenTransforms.class.getName());
    // *************************************************************************
    // fields

    /**
     * technique for rotations
     */
    private TweenRotations tweenRotations = TweenRotations.Nlerp;
    /**
     * technique for scales
     */
    private TweenVectors tweenScales = TweenVectors.Lerp;
    /**
     * technique for translations
     */
    private TweenVectors tweenTranslations = TweenVectors.Lerp;
    // *************************************************************************
    // new methods exposed

    /**
     * Calculate the transform for the specified bone/spatial track and time
     * using these techniques. TODO sort methods
     *
     * @param track input bone/spatial track (not null, unaffected)
     * @param time animation time input (in seconds)
     * @param duration (in seconds)
     * @param storeResult (modified if not null)
     * @return a transform (either storeResult or a new instance)
     */
    public Transform transform(Track track, float time, float duration,
            Transform storeResult) {
        assert track instanceof BoneTrack || track instanceof SpatialTrack;
        if (storeResult == null) {
            storeResult = new Transform();
        }

        float[] times = track.getKeyFrameTimes();
        int lastFrame = times.length - 1;
        assert lastFrame >= 0 : lastFrame;

        Vector3f[] translations = MyAnimation.getTranslations(track);
        Quaternion[] rotations = MyAnimation.getRotations(track);
        Vector3f[] scales = MyAnimation.getScales(track);

        if (time <= 0f || lastFrame == 0) {
            /*
             * Copy the transform of the first frame.
             */
            storeResult.loadIdentity();
            if (translations != null) {
                storeResult.setTranslation(translations[0]);
            }
            if (rotations != null) {
                storeResult.setRotation(rotations[0]);
            }
            if (scales != null) {
                storeResult.setScale(scales[0]);
            }

        } else {
            /*
             * Interpolate between frames.
             */
            interpolate(time, times, duration, translations, rotations, scales,
                    storeResult);
        }

        return storeResult;
    }

    /**
     * Read the technique for rotations.
     *
     * @return enum (not null)
     */
    public TweenRotations getTweenRotations() {
        return tweenRotations;
    }

    /**
     * Read the technique for scales.
     *
     * @return enum (not null)
     */
    public TweenVectors getTweenScales() {
        return tweenScales;
    }

    /**
     * Read the technique for translations.
     *
     * @return enum (not null)
     */
    public TweenVectors getTweenTranslations() {
        return tweenTranslations;
    }

    /**
     * Interpolate between keyframes in a bone/spatial track using these
     * techniques.
     *
     * @param time (in seconds, &ge;0, &le;duration)
     * @param track input bone/spatial track (not null, unaffected)
     * @param duration animation duration (in seconds, &gt;0)
     * @param storeResult (modified if not null)
     * @return transform (either storeResult or a new instance)
     */
    public Transform interpolate(float time, Track track, float duration,
            Transform storeResult) {
        Validate.inRange(time, "time", 0f, duration);
        assert track instanceof BoneTrack || track instanceof SpatialTrack;

        float[] times = track.getKeyFrameTimes();
        Vector3f[] translations = MyAnimation.getTranslations(track);
        Quaternion[] rotations = MyAnimation.getRotations(track);
        Vector3f[] scales = MyAnimation.getScales(track);

        storeResult = interpolate(time, times, duration, translations,
                rotations, scales, storeResult);

        return storeResult;
    }

    /**
     * Interpolate between keyframes using these techniques.
     *
     * @param time (in seconds, &ge;0, &le;duration)
     * @param times keyframe times (in seconds, not null, unaffected)
     * @param duration animation duration (in seconds, &gt;0)
     * @param translations (may be null, unaffected, same length as times)
     * @param rotations (may be null, unaffected, same length as times)
     * @param scales (may be null, unaffected, same length as times)
     * @param storeResult (modified if not null)
     * @return transform (either storeResult or a new instance)
     */
    public Transform interpolate(float time, float[] times, float duration,
            Vector3f[] translations, Quaternion[] rotations, Vector3f[] scales,
            Transform storeResult) {
        Validate.inRange(time, "time", 0f, duration);
        Validate.nonNull(times, "times");
        if (storeResult == null) {
            storeResult = new Transform();
        }

        storeResult.loadIdentity();
        if (translations != null) {
            tweenTranslations.interpolate(time, times, duration, translations,
                    storeResult.getTranslation());
        }
        if (rotations != null) {
            tweenRotations.interpolate(time, times, duration, rotations,
                    storeResult.getRotation());
        }
        if (scales != null) {
            tweenScales.interpolate(time, times, duration, scales,
                    storeResult.getScale());
        }

        return storeResult;
    }

    /**
     * Copy a bone/spatial track, resampling at the specified times using these
     * techniques.
     *
     * @param oldTrack input bone/spatial track (not null, unaffected)
     * @param newTimes sample times (not null, alias created)
     * @param duration animation duration (in seconds, &ge;0)
     * @return a new instance
     */
    public Track resample(Track oldTrack, float[] newTimes, float duration) {
        assert oldTrack instanceof BoneTrack
                || oldTrack instanceof SpatialTrack;
        Validate.nonNegative(duration, "duration");

        int numSamples = newTimes.length;
        Vector3f[] oldTranslations = MyAnimation.getTranslations(oldTrack);
        Quaternion[] oldRotations = MyAnimation.getRotations(oldTrack);
        Vector3f[] oldScales = MyAnimation.getScales(oldTrack);
        /*
         * Allocate new arrays.
         */
        Vector3f[] newTranslations = null;
        if (oldTranslations != null) {
            newTranslations = new Vector3f[numSamples];
        }
        Quaternion[] newRotations = null;
        if (oldRotations != null) {
            newRotations = new Quaternion[numSamples];
        }
        Vector3f[] newScales = null;
        if (oldScales != null) {
            newScales = new Vector3f[numSamples];
        }

        for (int frameIndex = 0; frameIndex < numSamples; frameIndex++) {
            float time = newTimes[frameIndex];
            Transform transform = transform(oldTrack, time, duration, null);

            if (newTranslations != null) {
                newTranslations[frameIndex] = transform.getTranslation();
            }
            if (newRotations != null) {
                newRotations[frameIndex] = transform.getRotation();
            }
            if (newScales != null) {
                newScales[frameIndex] = transform.getScale();
            }
        }

        Track result = TrackEdit.newTrack(oldTrack, newTimes, newTranslations,
                newRotations, newScales);

        return result;
    }

    /**
     * Copy a bone/spatial track, resampling it at the specified rate using
     * these techniques.
     *
     * @param oldTrack input bone/spatial track (not null, unaffected)
     * @param sampleRate sample rate (in frames per second, &gt;0)
     * @param duration animation duration (in seconds, &ge;0)
     * @return a new instance
     */
    public Track resampleAtRate(Track oldTrack, float sampleRate,
            float duration) {
        assert oldTrack instanceof BoneTrack
                || oldTrack instanceof SpatialTrack;
        Validate.positive(sampleRate, "sample rate");
        Validate.nonNegative(duration, "duration");

        int numSamples = 1 + (int) Math.floor(duration * sampleRate);
        float[] newTimes = new float[numSamples];
        for (int frameIndex = 0; frameIndex < numSamples; frameIndex++) {
            float time = frameIndex / sampleRate;
            if (time > duration) {
                time = duration;
            }
            newTimes[frameIndex] = time;
        }
        Track result = resample(oldTrack, newTimes, duration);

        return result;
    }

    /**
     * Copy a bone/spatial track, resampling to the specified number of samples
     * using these techniques.
     *
     * @param oldTrack input bone/spatial track (not null, unaffected)
     * @param numSamples number of samples (&ge;2)
     * @param duration animation duration (in seconds, &gt;0)
     * @return a new instance
     */
    public Track resampleToNumber(Track oldTrack, int numSamples,
            float duration) {
        assert oldTrack instanceof BoneTrack
                || oldTrack instanceof SpatialTrack;
        Validate.inRange(numSamples, "number of samples", 2, Integer.MAX_VALUE);
        Validate.positive(duration, "duration");

        float[] newTimes = new float[numSamples];
        for (int frameIndex = 0; frameIndex < numSamples; frameIndex++) {
            float time;
            if (frameIndex == numSamples - 1) {
                time = duration;
            } else {
                time = (frameIndex * duration) / (numSamples - 1);
            }
            newTimes[frameIndex] = time;
        }
        Track result = resample(oldTrack, newTimes, duration);

        return result;
    }

    /**
     * Alter the technique for rotations.
     *
     * @param newTechnique (not null)
     */
    public void setTweenRotations(TweenRotations newTechnique) {
        Validate.nonNull(newTechnique, "new technique");
        tweenRotations = newTechnique;
    }

    /**
     * Alter the technique for scales.
     *
     * @param newTechnique (not null)
     */
    public void setTweenScales(TweenVectors newTechnique) {
        Validate.nonNull(newTechnique, "new technique");
        tweenScales = newTechnique;
    }

    /**
     * Alter the technique for translations.
     *
     * @param newTechnique (not null)
     */
    public void setTweenTranslations(TweenVectors newTechnique) {
        Validate.nonNull(newTechnique, "new technique");
        tweenTranslations = newTechnique;
    }
    // *************************************************************************
    // Object methods

    /**
     * Create a deep copy of this object.
     *
     * @return a new object, equivalent to this one
     * @throws CloneNotSupportedException if superclass isn't cloneable
     */
    @Override
    public TweenTransforms clone() throws CloneNotSupportedException {
        TweenTransforms clone = (TweenTransforms) super.clone();
        return clone;
    }
}
