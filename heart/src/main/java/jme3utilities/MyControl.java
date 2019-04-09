/*
 Copyright (c) 2013-2019, Stephen Gold
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
import com.jme3.animation.Skeleton;
import com.jme3.animation.SkeletonControl;
import com.jme3.app.StatsView;
import com.jme3.cinematic.events.MotionEvent;
import com.jme3.effect.ParticleEmitter;
import com.jme3.input.ChaseCamera;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.control.Control;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

/**
 * Utility methods that operate on jME3 scene-graph controls in general. If
 * physics controls might be present, use the corresponding methods in the
 * MyControlP class instead.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class MyControl {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(MyControl.class.getName());
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private MyControl() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Check whether a scene-graph control implements isEnabled() and
     * setEnabled().
     *
     * @param sgc control to test (may be null, unaffected)
     * @return true if it's implemented, otherwise false
     */
    public static boolean canDisable(Control sgc) {
        boolean result = sgc instanceof AbstractControl
                || sgc instanceof ChaseCamera
                || sgc instanceof MotionEvent
                || sgc instanceof ParticleEmitter.ParticleEmitterControl
                || sgc instanceof StatsView;

        return result;
    }

    /**
     * Generate a textual description of a scene-graph control.
     *
     * @param control instance to describe (not null, unaffected)
     * @return description (not null, not empty)
     */
    public static String describe(Control control) {
        String result = describeType(control);

        if (control instanceof AnimControl) {
            AnimControl animControl = (AnimControl) control;
            Collection<String> nameCollection = animControl.getAnimationNames();
            int numAnimations = nameCollection.size();
            if (numAnimations > 2) {
                result += String.format("[%d]", numAnimations);
            } else {
                List<String> descs = new ArrayList<>(numAnimations);
                for (String animationName : nameCollection) {
                    Animation animation = animControl.getAnim(animationName);
                    String desc = MyAnimation.describe(animation, animControl);
                    descs.add(desc);
                }
                String names = MyString.join(descs);
                result += String.format("[%s]", names);
            }

        } else if (control instanceof SkeletonControl) {
            SkeletonControl skeletonControl = (SkeletonControl) control;
            int boneCount = skeletonControl.getSkeleton().getBoneCount();
            boolean useHw = skeletonControl.isHardwareSkinningUsed();
            result += String.format("[%d %s]", boneCount, useHw ? "hw" : "sw");
        }

        return result;
    }

    /**
     * Describe the type of a scene-graph control.
     *
     * @param control instance to describe (not null, unaffected)
     * @return description (not null)
     */
    public static String describeType(Control control) {
        String description = control.getClass().getSimpleName();
        if (description.endsWith("Control")) {
            description = MyString.removeSuffix(description, "Control");
        }

        return description;
    }

    /**
     * Find the index of the specified scene-graph control in the specified
     * spatial.
     *
     * @param sgc scene-graph control to find (not null, unaffected)
     * @param spatial where the control was added (not null, unaffected)
     * @return index (&ge;0) or -1 if not found
     */
    public static int findIndex(Control sgc, Spatial spatial) {
        Validate.nonNull(sgc, "control");

        int result = -1;
        int numControls = spatial.getNumControls();
        for (int index = 0; index < numControls; ++index) {
            Control control = spatial.getControl(index);
            if (control == sgc) {
                result = index;
            }
        }

        return result;
    }

    /**
     * Access the skeleton (if any) in the specified scene-graph control.
     *
     * @param sgc which scene-graph control (may be null, unaffected)
     * @return the pre-existing instance, or null if none found
     */
    public static Skeleton findSkeleton(Control sgc) {
        Skeleton result = null;
        if (sgc instanceof AnimControl) {
            AnimControl animControl = (AnimControl) sgc;
            result = animControl.getSkeleton();

        } else if (sgc instanceof SkeletonControl) {
            SkeletonControl skeletonControl = (SkeletonControl) sgc;
            result = skeletonControl.getSkeleton();
        }

        return result;
    }

    /**
     * Test whether the specified scene-graph control is enabled.
     *
     * @param sgc the control to test (not null, unaffected)
     * @return true if the control is enabled, otherwise false
     */
    public static boolean isEnabled(Control sgc) {
        Validate.nonNull(sgc, "control");

        boolean result;
        if (sgc instanceof AbstractControl) {
            AbstractControl abstractControl = (AbstractControl) sgc;
            result = abstractControl.isEnabled();

        } else if (sgc instanceof ChaseCamera) {
            ChaseCamera chaseCamera = (ChaseCamera) sgc;
            result = chaseCamera.isEnabled();

        } else if (sgc instanceof MotionEvent) {
            MotionEvent motionEvent = (MotionEvent) sgc;
            result = motionEvent.isEnabled();

        } else if (sgc instanceof ParticleEmitter.ParticleEmitterControl) {
            ParticleEmitter.ParticleEmitterControl pec
                    = (ParticleEmitter.ParticleEmitterControl) sgc;
            result = pec.isEnabled();

        } else if (sgc instanceof StatsView) {
            StatsView statsView = (StatsView) sgc;
            result = statsView.isEnabled();

        } else {
            String message = sgc.getClass().getName();
            throw new IllegalArgumentException(message);
        }

        return result;
    }

    /**
     * Alter the enabled state of a scene-graph control.
     *
     * @param sgc control to alter (not null)
     * @param newState true means enable the control, false means disable it
     */
    public static void setEnabled(Control sgc, boolean newState) {
        if (sgc instanceof AbstractControl) {
            AbstractControl abstractControl = (AbstractControl) sgc;
            abstractControl.setEnabled(newState);

        } else if (sgc instanceof ChaseCamera) {
            ChaseCamera chaseCamera = (ChaseCamera) sgc;
            chaseCamera.setEnabled(newState);

        } else if (sgc instanceof MotionEvent) {
            MotionEvent motionEvent = (MotionEvent) sgc;
            motionEvent.setEnabled(newState);

        } else if (sgc instanceof ParticleEmitter.ParticleEmitterControl) {
            ParticleEmitter.ParticleEmitterControl pec
                    = (ParticleEmitter.ParticleEmitterControl) sgc;
            pec.setEnabled(newState);

        } else if (sgc instanceof StatsView) {
            StatsView statsView = (StatsView) sgc;
            statsView.setEnabled(newState);

        } else {
            String message = sgc.getClass().getName();
            throw new IllegalArgumentException(message);
        }
    }
}
