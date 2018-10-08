/*
 Copyright (c) 2018, Stephen Gold
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
package jme3utilities.minie.test;

import com.jme3.bullet.control.KinematicRagdollControl;
import com.jme3.bullet.control.ragdoll.JointPreset;
import java.util.logging.Logger;

/**
 * A KinematicRagdollControl configured specifically for Sinbad.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class SinbadControl extends KinematicRagdollControl {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final public static Logger logger
            = Logger.getLogger(SinbadControl.class.getName());
    // *************************************************************************
    // constructors

    public SinbadControl() {
        super();

        super.addBone("Waist",
                new JointPreset(0.4f, -0.4f, 0.4f, -0.4f, 0.4f, -0.4f));
        super.addBoneName("Stomach");
        super.addBone("Chest",
                new JointPreset(0.4f, -0.4f, 0f, 0f, 0.4f, -0.4f));
        super.addBone("Neck",
                new JointPreset(1f, -1f, 1f, -1f, 1f, -1f));

        super.addBoneName("Clavicle.R");
        super.addBone("Humerus.R",
                new JointPreset(1.6f, -0.8f, 0f, 0f, 1.6f, -0.8f));
        super.addBone("Ulna.R",
                new JointPreset(1.6f, 0f, 0f, 0f, 0f, 0f));
        super.addBone("Hand.R",
                new JointPreset(0.8f, -0.8f, 0.8f, -0.8f, 0.8f, -0.8f));

        super.addBoneName("Clavicle.L");
        super.addBone("Humerus.L",
                new JointPreset(1.6f, -0.8f, 0f, 0f, 1.6f, -0.8f));
        super.addBone("Ulna.L",
                new JointPreset(1.6f, 0f, 0f, 0f, 0f, 0f));
        super.addBone("Hand.L",
                new JointPreset(0.8f, -0.8f, 0.8f, -0.8f, 0.8f, -0.8f));

        super.addBone("Thigh.R",
                new JointPreset(2f, -0.8f, 0.4f, -0.4f, 0.8f, -0.8f));
        super.addBone("Calf.R",
                new JointPreset(0f, -2f, 0f, 0f, 0f, 0f));
        super.addBone("Foot.R",
                new JointPreset(0f, -0.8f, 0.8f, -0.8f, 0.8f, -0.8f));

        super.addBone("Thigh.L",
                new JointPreset(2f, -0.8f, 0.4f, -0.4f, 0.8f, -0.8f));
        super.addBone("Calf.L",
                new JointPreset(0f, -2f, 0f, 0f, 0f, 0f));
        super.addBone("Foot.L",
                new JointPreset(0f, -0.8f, 0.8f, -0.8f, 0.8f, -0.8f));
    }
}
