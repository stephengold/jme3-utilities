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

import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.control.AbstractControl;

/**
 * A simplified AbstractControl.
 * <p>
 * Although this is an abstract class, it defines all required methods in order
 * to simplify the development of subclasses -- unlike AbstractControl. It also
 * validates parameters of its public methods and finalizes isEnabled().
 * <p>
 * Each instance is enabled at creation.
 *
 * @author Stephen Gold sgold@sonic.net
 */
abstract public class SimpleControl extends AbstractControl {
    // *************************************************************************
    // new methods exposed

    /**
     * Toggle the enabled status of this Control.
     */
    public void toggleEnabled() {
        setEnabled(!enabled);
    }
    // *************************************************************************
    // AbstractControl methods

    /**
     * Render this Control. Invoked when the controlled spatial is about to be
     * rendered to a ViewPort.
     * <p>
     * This implementation only performs checks and is meant to be overridden.
     *
     * @param renderManager the renderer which is rendering the controlled
     * spatial (not null)
     * @param viewPort the ViewPort where the controlled spatial will be
     * rendered (not null)
     */
    @Override
    protected void controlRender(RenderManager renderManager,
            ViewPort viewPort) {
        Validate.nonNull(renderManager, "render manager");
        Validate.nonNull(viewPort, "viewport");

        if (!enabled) {
            throw new IllegalStateException("should be enabled");
        }
    }

    /**
     * Update this Control. Invoked once per frame during the logical-state
     * update, provided the Control is enabled and added to a Spatial. Should
     * not be invoked directly from user code.
     * <p>
     * This implementation only performs checks and is meant to be overridden.
     *
     * @param updateInterval time interval between frames (in seconds, &ge;0)
     */
    @Override
    protected void controlUpdate(float updateInterval) {
        Validate.nonNegative(updateInterval, "interval");

        if (!enabled) {
            throw new IllegalStateException("should be enabled");
        }
    }

    /**
     * Test whether this Control is enabled.
     *
     * @return true if enabled, otherwise false
     */
    @Override
    final public boolean isEnabled() {
        return enabled;
    }
}
