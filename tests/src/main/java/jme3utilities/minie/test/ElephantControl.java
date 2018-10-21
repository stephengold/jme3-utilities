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

import com.jme3.bullet.animation.DynamicAnimControl;
import com.jme3.bullet.animation.JointPreset;
import java.util.logging.Logger;

/**
 * A DynamicAnimControl configured specifically for the Elephant model.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class ElephantControl extends DynamicAnimControl {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final public static Logger logger4
            = Logger.getLogger(ElephantControl.class.getName());
    // *************************************************************************
    // constructors

    public ElephantControl() {
        super();
        super.setMass(torsoName, 300f);

        // head
        super.link("joint5", 200f,
                new JointPreset(1f, -1f, 1f, -1f, 0.6f, -0.3f));
        
        super.link("Ear_L", 2f,
                new JointPreset(0.2f, -0.2f, 0.4f, -1f, 0f, 0f));
        super.link("Ear_B1_L", 1f,
                new JointPreset(0.2f, -0.2f, 1f, -1f, 1f, -1f));
        super.link("Ear_M1_L", 1f,
                new JointPreset(0.2f, -0.2f, 1f, -1f, 1f, -1f));
        super.link("Ear_T1_L", 1f,
                new JointPreset(0.2f, -0.2f, 1f, -1f, 1f, -1f));
        super.link("Ear_B2_L", 1f,
                new JointPreset(0.2f, -0.2f, 1f, -1f, 1f, -1f));
        super.link("Ear_M4_L", 1f,
                new JointPreset(0.2f, -0.2f, 1f, -1f, 1f, -1f));
        super.link("Ear_T2_L", 1f,
                new JointPreset(0.2f, -0.2f, 1f, -1f, 1f, -1f));
        
        super.link("Ear_R", 5f,
                new JointPreset(0.2f, -0.2f, 0.4f, -1f, 0f, 0f));
        super.link("Ear_B1_R", 1f,
                new JointPreset(0.2f, -0.2f, 1f, -1f, 1f, -1f));
        super.link("Ear_M1_R", 1f,
                new JointPreset(0.2f, -0.2f, 1f, -1f, 1f, -1f));
        super.link("Ear_T1_R", 1f,
                new JointPreset(0.2f, -0.2f, 1f, -1f, 1f, -1f));
        super.link("Ear_B2_R", 1f,
                new JointPreset(0.2f, -0.2f, 1f, -1f, 1f, -1f));
        super.link("Ear_M4_R", 1f,
                new JointPreset(0.2f, -0.2f, 1f, -1f, 1f, -1f));
        super.link("Ear_T2_R", 1f,
                new JointPreset(0.2f, -0.2f, 1f, -1f, 1f, -1f));

        // trunk
        super.link("joint11", 5f,
                new JointPreset(1f, -1f, 1f, -1f, 0.5f, -1f));
        super.link("joint12", 5f,
                new JointPreset(1f, -1f, 1f, -1f, 0.5f, -1f));
        super.link("joint14", 3f,
                new JointPreset(1f, -1f, 1.5f, -1.5f, 1f, -1.5f));

        super.link("Tail", 1f,
                new JointPreset(0.2f, -0.2f, 1f, -1f, 0.1f, -0.1f));
        super.link("joint19", 1f,
                new JointPreset(0.2f, -0.2f, 1f, -1f, 0.1f, -0.1f));

        super.link("Oberschenkel_F_R", 100f,
                new JointPreset(0f, 0f, 0.2f, -0.2f, 0.5f, -0.5f));
        super.link("Knee_F_R", 40f,
                new JointPreset(0.2f, -0.2f, 0.2f, -0.2f, 0f, -0.5f));
        super.link("Foot_F_R", 10f,
                new JointPreset(0f, 0f, 0.2f, -0.2f, 0.2f, -0.2f));

        super.link("Oberschenkel_F_L", 100f,
                new JointPreset(0f, 0f, 0.2f, -0.2f, 0.5f, -0.5f));
        super.link("Knee_F_L", 40f,
                new JointPreset(0.2f, -0.2f, 0.2f, -0.2f, 0f, -0.5f));
        super.link("Foot_F_L", 10f,
                new JointPreset(0f, 0f, 0.2f, -0.2f, 0.2f, -0.2f));

        super.link("Oberschenkel_B_R", 60f,
                new JointPreset(0f, 0f, 0.5f, -0.5f, 0.5f, -0.5f));
        super.link("Knee_B_R", 40f,
                new JointPreset(0.2f, -0.2f, 0.2f, -0.2f, 0f, -0.5f));
        super.link("Foot_B_R", 10f,
                new JointPreset(0f, 0f, 0.2f, -0.2f, 0.2f, -0.2f));

        super.link("Oberschenkel_B_L", 60f,
                new JointPreset(0f, 0f, 0.5f, -0.5f, 0.5f, -0.5f));
        super.link("Knee_B_L", 40f,
                new JointPreset(0.2f, -0.2f, 0.2f, -0.2f, 0f, -0.5f));
        super.link("Foot_B_L", 10f,
                new JointPreset(0f, 0f, 0.2f, -0.2f, 0.2f, -0.2f));
    }
}
