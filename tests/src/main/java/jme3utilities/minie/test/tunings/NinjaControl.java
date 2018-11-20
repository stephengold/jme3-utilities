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
package jme3utilities.minie.test.tunings;

import com.jme3.bullet.animation.DynamicAnimControl;
import com.jme3.bullet.animation.RangeOfMotion;
import java.util.logging.Logger;

/**
 * A DynamicAnimControl configured specifically for the Ninja model.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class NinjaControl extends DynamicAnimControl {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final public static Logger logger4
            = Logger.getLogger(NinjaControl.class.getName());
    // *************************************************************************
    // constructors

    public NinjaControl() {
        super();
        super.setMass(torsoName, 5f);

        // chest
        super.link("Joint4", 9f,
                new RangeOfMotion(1f, -0.5f, 0.8f, -0.8f, 0.3f, -0.3f));
        super.link("Joint6", 9f,
                new RangeOfMotion(0.5f, 0.8f, 0f));

        // head
        super.link("Joint7", 4f,
                new RangeOfMotion(0.8f, -0.5f, 1f, -1f, 0.8f, -0.8f));

        // right arm and katana
        super.link("Joint9", 2f,
                new RangeOfMotion(0.3f, -1f, 1f, -1f, 0.3f, -1f));
        super.link("Joint11", 2f,
                new RangeOfMotion(0f, -1.4f, 0.8f, -0.8f, 0f, -0f));
        super.link("Joint12", 5f,
                new RangeOfMotion(0.5f, 1f, 0f));

        // left arm
        super.link("Joint14", 2f,
                new RangeOfMotion(0.3f, -1f, 1f, -1f, 1f, -0.3f));
        super.link("Joint16", 2f,
                new RangeOfMotion(0f, -1.4f, 0.8f, -0.8f, 0f, -0f));
        super.link("Joint17", 1f,
                new RangeOfMotion(0.5f, 1f, 0f));

        // right leg
        super.link("Joint18", 3f,
                new RangeOfMotion(0.3f, -1f, 0.3f, -0.3f, 0.2f, -0.5f));
        super.link("Joint19", 2f,
                new RangeOfMotion(1.2f, 0f, 0.2f, -0.2f, 0f, 0f));
        super.link("Joint21", 1f,
                new RangeOfMotion(0.5f, 0.2f, 0.1f));

        // left leg
        super.link("Joint23", 3f,
                new RangeOfMotion(0.3f, -1f, 0.3f, -0.3f, 0.5f, -0.2f));
        super.link("Joint24", 2f,
                new RangeOfMotion(1.2f, 0f, 0.2f, -0.2f, 0f, 0f));
        super.link("Joint26", 1f,
                new RangeOfMotion(0.5f, 0.2f, 0.1f));
    }
}
