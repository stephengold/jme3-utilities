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
 * A DynamicAnimControl configured specifically for the Puppet model.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class PuppetControl extends DynamicAnimControl {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final public static Logger logger4
            = Logger.getLogger(PuppetControl.class.getName());
    // *************************************************************************
    // constructors

    public PuppetControl() {
        super();
        super.setMass(torsoName, 5f);

        super.link("spine", 10f,
                new RangeOfMotion(0.5f, -1f, 0.7f, -0.7f, 0.7f, -0.7f));
        super.link("upper_chest", 1f,
                new RangeOfMotion(0.3f, -0.6f, 0.2f, -0.2f, 0.2f, -0.2f));
        super.link("neck", 1f,
                new RangeOfMotion(0.2f, -0.5f, 0.5f, -0.5f, 0.5f, -0.5f));
        super.link("head", 5f,
                new RangeOfMotion(0.5f, 0.5f, 0.5f));

        super.link("shoulder.R", 2f,
                new RangeOfMotion(0.1f, 0.2f, 0f));
        super.link("upper_arm.1.R", 3f,
                new RangeOfMotion(1.5f, -0.5f, 1.5f, -0.5f, 1f, -1f));
        super.link("forearm.1.R", 3f,
                new RangeOfMotion(0f, 0f, 2f, 0f, 1f, -1f));
        super.link("hand.R", 2f,
                new RangeOfMotion(0.8f, 0.1f, 0f));

        super.link("shoulder.L", 2f,
                new RangeOfMotion(0.1f, 0.2f, 0f));
        super.link("upper_arm.1.L", 3f,
                new RangeOfMotion(0.5f, -1.5f, 1.5f, -0.5f, 1f, -1f));
        super.link("forearm.1.L", 3f,
                new RangeOfMotion(0f, 0f, 2f, 0f, 1f, -1f));
        super.link("hand.L", 2f,
                new RangeOfMotion(0.8f, 0.1f, 0f));

        super.link("thigh.R", 5f,
                new RangeOfMotion(1f, -0.5f, 0.3f, -0.8f, 0.2f, -0.2f));
        super.link("shin.R", 5f,
                new RangeOfMotion(0f, -1.6f, 0f, 0f, 0f, 0f));
        super.link("foot.R", 1f,
                new RangeOfMotion(0.5f, 0.4f, 0.2f));
        super.link("toe.R", 1f,
                new RangeOfMotion(0.5f, 0f, 0f));

        super.link("thigh.L", 5f,
                new RangeOfMotion(1f, -0.5f, 0.8f, -0.3f, 0.2f, -0.2f));
        super.link("shin.L", 5f,
                new RangeOfMotion(0f, -1.6f, 0f, 0f, 0f, 0f));
        super.link("foot.L", 1f,
                new RangeOfMotion(0.5f, 0.4f, 0.2f));
        super.link("toe.L", 1f,
                new RangeOfMotion(0.5f, 0f, 0f));
    }
}
