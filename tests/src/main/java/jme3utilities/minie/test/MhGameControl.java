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
import com.jme3.bullet.animation.RangeOfMotion;
import java.util.logging.Logger;

/**
 * A DynamicAnimControl configured specifically for the MhGame model.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class MhGameControl extends DynamicAnimControl {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final public static Logger logger4
            = Logger.getLogger(MhGameControl.class.getName());
    // *************************************************************************
    // constructors

    public MhGameControl() {
        super();
        super.setMass(torsoName, 5f);

        super.link("spine_01", 4f,
                new RangeOfMotion(0.5f, 0.3f, 0.5f));
        super.link("spine_02", 1f,
                new RangeOfMotion(0.5f, 0.3f, 0.5f));
        super.link("spine_03", 1f,
                new RangeOfMotion(0.4f, 0.2f, 0f));
        super.link("neck_01", 1f,
                new RangeOfMotion(0.5f, 0.5f, 0.3f));
        super.link("head", 1f,
                new RangeOfMotion(0.4f, 0.6f, 0.2f));

        super.link("clavicle_r", 1f,
                new RangeOfMotion(0.4f, 0f, 0.2f));
        super.link("upperarm_r", 1f,
                new RangeOfMotion(0.2f, -1f, 0.8f, -0.8f, 1f, -1f));
        super.link("lowerarm_r", 1f,
                new RangeOfMotion(0f, -2f, 0f, 0f, 0f, 0f));
        super.link("hand_r", 1f,
                new RangeOfMotion(0.7f, 0f, 0.2f));

        super.link("clavicle_l", 1f,
                new RangeOfMotion(0.4f, 0f, 0.2f));
        super.link("upperarm_l", 1f,
                new RangeOfMotion(0.2f, -1f, 0.8f, -0.8f, 1f, -1f));
        super.link("lowerarm_l", 1f,
                new RangeOfMotion(0f, -2f, 0f, 0f, 0f, 0f));
        super.link("hand_l", 1f,
                new RangeOfMotion(0.7f, 0f, 0.2f));

        super.link("thigh_r", 1f,
                new RangeOfMotion(1f, -0.2f, 0.2f, -0.2f, 0.2f, -0.5f));
        super.link("calf_r", 1f,
                new RangeOfMotion(0f, -1.8f, 0f, 0f, 0f, 0f));
        super.link("foot_r", 1f,
                new RangeOfMotion(0.8f, 0.3f, 0.5f));

        super.link("thigh_l", 1f,
                new RangeOfMotion(1f, -0.2f, 0.2f, -0.2f, 0.5f, -0.2f));
        super.link("calf_l", 1f,
                new RangeOfMotion(0f, -1.8f, 0f, 0f, 0f, 0f));
        super.link("foot_l", 1f,
                new RangeOfMotion(0.5f, 0.3f, 0.5f));
    }
}