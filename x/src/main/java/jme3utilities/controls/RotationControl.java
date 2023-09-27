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
package jme3utilities.controls;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import java.util.logging.Logger;
import jme3utilities.SimpleControl;
import jme3utilities.Validate;

/**
 * Simple control to rotate a spatial at a constant rate.
 * <p>
 * Each instance is enabled at creation.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class RotationControl extends SimpleControl {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(RotationControl.class.getName());
    // *************************************************************************
    // fields

    /**
     * axis of rotation (in local coordinates, length=1): set by constructor
     */
    final private Vector3f axis;
    /**
     * rate of rotation (radians per second): set by constructor
     */
    final private float rate;
    // *************************************************************************
    // constructors

    /**
     * Instantiate an enabled control with the specified vector.
     *
     * @param rate rate of rotation (in radians per second)
     * @param axis axis of rotation (in local coordinates, length&gt;0,
     * unaffected)
     */
    public RotationControl(float rate, Vector3f axis) {
        Validate.nonZero(axis, "axis");

        this.axis = axis.normalize();
        this.rate = rate;

        assert isEnabled();
    }
    // *************************************************************************
    // SimpleControl methods

    /**
     * Update the spatial's orientation. Invoked when the spatial's geometric
     * state is about to be updated, once per frame while this control attached
     * and enabled.
     *
     * @param updateInterval time interval between updates (in seconds, &ge;0)
     */
    @Override
    protected void controlUpdate(float updateInterval) {
        super.controlUpdate(updateInterval);
        if (spatial == null) {
            return;
        }

        Quaternion rotation = new Quaternion();
        float angle = rate * updateInterval;
        rotation.fromAngleNormalAxis(angle, axis);
        spatial.rotate(rotation);
    }
}
