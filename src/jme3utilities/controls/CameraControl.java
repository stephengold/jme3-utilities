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
 * Stephen Gold's name may not be used to endorse or promote products
 derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL STEPHEN GOLD BE LIABLE FOR ANY
 DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package jme3utilities.controls;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import java.util.logging.Logger;
import jme3utilities.SimpleControl;
import jme3utilities.Validate;

/**
 * Simple control which manages the position and orientation of a camera in the
 * local coordinate space of a spatial.
 * <p>
 * Each instance is enabled at creation.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class CameraControl
        extends SimpleControl {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger = Logger.getLogger(
            CameraControl.class.getName());
    // *************************************************************************
    // fields
    
    /**
     * camera managed by this control: set by constructor
     */
    final private Camera camera;
    /**
     * camera's offset in local coordinates: set by constructor
     */
    final private Vector3f offset;
    /**
     * camera's up direction in local coordinates: set by constructor
     */
    final private Vector3f upDirection;
    /**
     * camera's look direction in local coordinates: set by constructor
     */
    final private Vector3f lookDirection;
    // *************************************************************************
    // constructors

    /**
     * Instantiate an enabled control to manage the specified camera.
     *
     * @param camera camera to manage (not null)
     * @param offset camera's offset in local coordinates (not null, unaffected)
     * @param lookDirection camera's look direction in local coordinates
     * (length&gt;0, unaffected)
     * @param upDirection camera's up direction in local coordinates
     * (length&gt;0, unaffected)
     */
    public CameraControl(Camera camera, Vector3f offset, Vector3f lookDirection,
            Vector3f upDirection) {
        Validate.nonNull(camera, "camera");
        Validate.nonZero(lookDirection, "look direction");
        Validate.nonZero(upDirection, "up direction");

        this.camera = camera;
        this.offset = offset.clone();
        this.lookDirection = lookDirection.clone();
        this.upDirection = upDirection.clone();

        assert isEnabled();
    }
    // *************************************************************************
    // new public methods

    /**
     * Alter the camera's look direction.
     *
     * @param newDirection direction in local coordinates (length&gt;0, unaffected)
     */
    public void setLookDirection(Vector3f newDirection) {
        Validate.nonZero(newDirection, "direction");

        lookDirection.set(newDirection);
    }
    // *************************************************************************
    // SimpleControl methods

    /**
     * Update the camera's location and orientation. Invoked when the spatial's
     * geometric state is about to be updated, once per frame while this control
     * attached and enabled.
     *
     * @param updateInterval time interval between updates (in seconds, &ge;0)
     */
    @Override
    protected void controlUpdate(float updateInterval) {
        super.controlUpdate(updateInterval);

        if (spatial == null) {
            return;
        }
        /*
         * Update the camera location.
         */
        Vector3f worldLocation = spatial.localToWorld(offset, null);
        camera.setLocation(worldLocation);
        /*
         * Update the camera orientation.
         */
        Quaternion rotation = spatial.getWorldRotation();
        Vector3f worldLookDirection = rotation.mult(lookDirection);
        Vector3f worldUpDirection = rotation.mult(upDirection);
        camera.lookAtDirection(worldLookDirection, worldUpDirection);
    }
}
