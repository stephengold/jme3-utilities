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
 * A DynamicAnimControl configured specifically for the Jaime model.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class JaimeControl extends DynamicAnimControl {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final public static Logger logger4
            = Logger.getLogger(JaimeControl.class.getName());
    // *************************************************************************
    // constructors

    public JaimeControl() {
        super();
        super.setMass(torsoName, 5f);

        super.link("spine", 5f,
                new JointPreset(1f, -1f, 1f, -1f, 1f, -1f));
        super.link("ribs", 5f,
                new JointPreset(0.6f, -0.6f, 0.4f, -0.4f, 0.4f, -0.4f));
        super.link("neck", 1f,
                new JointPreset(1f, -0.5f, 0.5f, -0.5f, 0.5f, -0.5f));
        super.link("head", 5f,
                new JointPreset(1f, -0.5f, 0.5f, -0.5f, 0.5f, -0.5f));

        super.link("tail.001", 1f,
                new JointPreset(0.5f, -0.5f, 0.2f, -0.2f, 0.5f, -0.5f));
        super.link("tail.002", 0.5f,
                new JointPreset(0.5f, -0.5f, 0.2f, -0.2f, 0.5f, -0.5f));
        super.link("tail.003", 0.5f,
                new JointPreset(0.5f, -0.5f, 0.2f, -0.2f, 0.5f, -0.5f));
        super.link("tail.004", 0.5f,
                new JointPreset(0.5f, -0.5f, 0.2f, -0.2f, 0.5f, -0.5f));
        super.link("tail.005", 0.5f,
                new JointPreset(0.5f, -0.5f, 0.2f, -0.2f, 0.5f, -0.5f));
        super.link("tail.007", 0.5f,
                new JointPreset(0.5f, -0.5f, 0.2f, -0.2f, 0.5f, -0.5f));
        super.link("tail.009", 0.5f,
                new JointPreset(0.5f, -0.5f, 0.2f, -0.2f, 0.5f, -0.5f));

        super.link("shoulder.R", 3f,
                new JointPreset(0.8f, -1.6f, 0f, 0f, 0.3f, -0.6f));
        super.link("upper_arm.R", 1f,
                new JointPreset(0.8f, -1.6f, 1f, -1f, 1.6f, -1.8f));
        super.link("forearm.R", 1f,
                new JointPreset(0f, 0f, 1f, -1f, 0f, -2f));
        super.link("hand.R", 2f,
                new JointPreset(0.8f, -0.8f, 0f, 0f, 0.2f, -0.2f));

        super.link("shoulder.L", 3f,
                new JointPreset(1.6f, -0.8f, 0f, 0f, 0.6f, -0.3f));
        super.link("upper_arm.L", 1f,
                new JointPreset(0.8f, -1.6f, 1f, -1f, 1.6f, -1.8f));
        super.link("forearm.L", 1f,
                new JointPreset(0f, 0f, 1f, -1f, 2f, 0f));
        super.link("hand.L", 2f,
                new JointPreset(0.8f, -0.8f, 0f, 0f, 0.2f, -0.2f));

        super.link("thigh.R", 2f,
                new JointPreset(1f, -0.4f, 0.4f, -0.4f, 0.5f, -0.5f));
        super.link("shin.R", 1f,
                new JointPreset(0f, 0f, 0.4f, -0.4f, 2f, 0f));
        super.link("foot.R", 2f,
                new JointPreset(0.6f, -0.6f, 0.2f, -0.2f, 0f, 0f));

        super.link("thigh.L", 2f,
                new JointPreset(1f, -0.4f, 0.4f, -0.4f, 0.5f, -0.5f));
        super.link("shin.L", 1f,
                new JointPreset(0f, 0f, 0.4f, -0.4f, 0f, -2f));
        super.link("foot.L", 2f,
                new JointPreset(0.6f, -0.6f, 0.2f, -0.2f, 0f, 0f));
    }
}
