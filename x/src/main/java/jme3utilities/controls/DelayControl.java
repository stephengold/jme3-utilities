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

import com.jme3.scene.control.Control;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyControl;
import jme3utilities.MyString;
import jme3utilities.SimpleControl;
import jme3utilities.Validate;

/**
 * Simple control to enable other "slave" controls after a pre-set simulation
 * interval.
 * <p>
 * Each instance is disabled at creation.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class DelayControl extends SimpleControl {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(DelayControl.class.getName());
    // *************************************************************************
    // fields

    /**
     * time interval (in seconds) until the timer pops
     */
    private float remainingSeconds = Float.POSITIVE_INFINITY;
    /**
     * list of slaves to be enabled
     */
    final private List<Control> slaves = new ArrayList<>(3);
    // *************************************************************************
    // constructors

    /**
     * Instantiate a disabled control without any slaves.
     */
    public DelayControl() {
        super.setEnabled(false);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Add a slave to the list.
     *
     * @param slave control to be added (not null)
     */
    public void addSlave(Control slave) {
        if (!isValidSlave(slave)) {
            throw new IllegalArgumentException("unknown control subclass");
        }
        slaves.add(slave);
    }

    /**
     * Read the amount of time remaining.
     *
     * @return time interval (in seconds) until the final action
     */
    public float getRemainingSeconds() {
        return remainingSeconds;
    }

    /**
     * Alter the amount of time remaining.
     *
     * @param newSeconds time interval (in seconds, &ge;0) until the final
     * action
     */
    public void setRemainingSeconds(float newSeconds) {
        Validate.nonNegative(newSeconds, "seconds");
        this.remainingSeconds = newSeconds;
    }
    // *************************************************************************
    // SimpleControl methods

    /**
     * Callback invoked when the spatial's geometric state is about to be
     * updated, once per frame while attached and enabled.
     *
     * @param updateInterval time interval between updates (in seconds, &ge;0)
     */
    @Override
    protected void controlUpdate(float updateInterval) {
        super.controlUpdate(updateInterval);
        if (spatial == null) {
            return;
        }

        // Update the time remaining.
        this.remainingSeconds -= updateInterval;
        if (remainingSeconds < 0f) {
            String name = spatial.getName();
            logger.log(
                    Level.INFO, "timed out, spatial={0}", MyString.quote(name));
            for (Control slave : slaves) {
                enableSlave(slave);
            }
            setEnabled(false);
            boolean success = spatial.removeControl(this);
            assert success;
        }
    }
    // *************************************************************************
    // new protected methods

    /**
     * Enable a slave.
     *
     * @param slave control to enable (not null)
     */
    protected void enableSlave(Control slave) {
        Validate.nonNull(slave, "slave control");
        MyControl.setEnabled(slave, true);
    }

    /**
     * Test whether a control is valid before adding it to the list of slaves.
     *
     * @param slave control to test
     * @return true if enableSlave() can enable the control, false otherwise
     */
    protected boolean isValidSlave(Control slave) {
        boolean result = MyControl.canDisable(slave);
        return result;
    }
}
