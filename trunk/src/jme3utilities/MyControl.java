// (c) Copyright 2013 Stephen Gold <sgold@sonic.net>
// Distributed under the terms of the GNU General Public License

/*
 This file is part of the JME3 Utilities Package.

 The JME3 Utilities Package is free software: you can redistribute it and/or
 modify it under the terms of the GNU General Public License as published by the
 Free Software Foundation, either version 3 of the License, or (at your
 option) any later version.

 The JME3 Utilities Package is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 for more details.

 You should have received a copy of the GNU General Public License along with
 the JME3 Utilities Package.  If not, see <http://www.gnu.org/licenses/>.
 */
package jme3utilities;

import com.jme3.bullet.control.GhostControl;
import com.jme3.bullet.control.KinematicRagdollControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.effect.ParticleEmitter;
import com.jme3.scene.control.AbstractControl;
import java.util.logging.Logger;

/**
 * Access the "enabled" state of JME3 scene-graph controls. Aside from test
 * cases, all methods here should be public and static.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class MyControl {
    // *************************************************************************
    // constants

    /**
     * message logger for this class
     */
    final private static Logger logger =
            Logger.getLogger(MyControl.class.getName());
    // *************************************************************************
    // constructors

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private MyControl() {
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Generate a textual description of a scene-graph control.
     *
     * @param control (not null)
     */
    public static String describe(Object control) {
        String name = control.getClass().getSimpleName();
        if (name.endsWith("Control")) {
            int length = name.length() - "Control".length();
            name = name.substring(0, length);
        }
        return name;
    }

    /**
     * Test whether a scene-graph control is enabled.
     *
     * @param control which control to test (not null)
     * @return true if the control is enabled, otherwise false
     */
    public static boolean isEnabled(Object control) {
        if (control instanceof AbstractControl) {
            return ((AbstractControl) control).isEnabled();
        } else if (control instanceof GhostControl) {
            return ((GhostControl) control).isEnabled();
        } else if (control instanceof KinematicRagdollControl) {
            return ((KinematicRagdollControl) control).isEnabled();
        } else if (control instanceof RigidBodyControl) {
            return ((RigidBodyControl) control).isEnabled();
        } else if (control instanceof ParticleEmitter.ParticleEmitterControl) {
            return ((ParticleEmitter.ParticleEmitterControl) control)
                    .isEnabled();
        } else {
            assert false : control.getClass();
            return false;
        }
    }

    /**
     * Check whether a scene-graph control is compatible with isEnabled() and
     * setEnabled().
     *
     * @param control which control to validate
     */
    public static boolean isValid(Object control) {
        return control instanceof AbstractControl
                || control instanceof GhostControl
                || control instanceof KinematicRagdollControl
                || control instanceof RigidBodyControl
                || control instanceof ParticleEmitter.ParticleEmitterControl;
    }

    /**
     * Alter the enabled state of a scene-graph control.
     *
     * @param control which control to alter
     * @param newState true means enable the control, false means disable it
     */
    public static void setEnabled(Object control, boolean newState) {
        if (control instanceof AbstractControl) {
            ((AbstractControl) control).setEnabled(newState);
        } else if (control instanceof GhostControl) {
            ((GhostControl) control).setEnabled(newState);
        } else if (control instanceof KinematicRagdollControl) {
            ((KinematicRagdollControl) control).setEnabled(newState);
        } else if (control instanceof RigidBodyControl) {
            ((RigidBodyControl) control).setEnabled(newState);
        } else if (control instanceof ParticleEmitter.ParticleEmitterControl) {
            ((ParticleEmitter.ParticleEmitterControl) control)
                    .setEnabled(newState);
        } else {
            assert false : control.getClass();
        }
    }
}