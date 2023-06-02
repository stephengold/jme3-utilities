/*
 Copyright (c) 2014-2023, Stephen Gold
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

import com.jme3.light.DirectionalLight;
import com.jme3.light.Light;
import com.jme3.light.PointLight;
import com.jme3.light.SpotLight;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import java.util.logging.Logger;
import jme3utilities.SimpleControl;
import jme3utilities.Validate;
import jme3utilities.math.MyQuaternion;

/**
 * Simple control which manages a light source attached to a Spatial.
 * <p>
 * The key differences between this class and
 * com.jme3.scene.control.LightControl are:<ol>
 * <li> the spatial can affect the light source but not vice versa, and
 * <li> the offset and direction of the light relative to the are
 * configurable.</ol>
 * <p>
 * Each instance is enabled at creation.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class LightControl extends SimpleControl {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(LightControl.class.getName());
    // *************************************************************************
    // fields

    /**
     * light managed by this control: set by constructor
     */
    final private Light light;
    /**
     * light's direction in local coordinates: set by constructor
     */
    final private Vector3f direction;
    /**
     * light's offset in local coordinates: set by constructor
     */
    final private Vector3f offset;
    // *************************************************************************
    // constructors

    /**
     * Instantiate an enabled control to manage the specified light.
     *
     * @param light to manage (not null, spot/point/directional light)
     * @param offset camera's offset in local coordinates (not null, unaffected)
     * @param direction camera's view direction in local coordinates (not null,
     * unaffected)
     */
    public LightControl(Light light, Vector3f offset, Vector3f direction) {
        if (!(light instanceof DirectionalLight
                || light instanceof PointLight
                || light instanceof SpotLight)) {
            throw new IllegalArgumentException(
                    "light type should be directional, point, or spot");
        }

        this.light = light;
        this.offset = offset.clone();
        this.direction = direction.clone();

        assert isEnabled();
    }
    // *************************************************************************
    // new methods exported

    /**
     * Alter the offset of the light relative to the controlled spatial.
     *
     * @param newOffset camera's offset in local coordinates (not null)
     */
    public void setOffset(Vector3f newOffset) {
        Validate.nonNull(newOffset, "offset");
        offset.set(newOffset);
    }
    // *************************************************************************
    // SimpleControl methods

    /**
     * Update the light's location and direction. Invoked when the spatial's
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
        Vector3f worldLocation = spatial.localToWorld(offset, null);
        Quaternion rotation = spatial.getWorldRotation();
        Vector3f worldDirection
                = MyQuaternion.rotate(rotation, direction, null);

        if (light instanceof DirectionalLight) {
            DirectionalLight directionalLight = (DirectionalLight) light;
            directionalLight.setDirection(worldDirection);

        } else if (light instanceof PointLight) {
            PointLight pointLight = (PointLight) light;
            pointLight.setPosition(worldLocation);

        } else if (light instanceof SpotLight) {
            SpotLight spotLight = (SpotLight) light;
            spotLight.setDirection(worldDirection);
            spotLight.setPosition(worldLocation);

        } else {
            assert false : light.getClass();
        }
    }
}
