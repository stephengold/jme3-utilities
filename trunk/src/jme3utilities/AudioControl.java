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

import com.jme3.audio.AudioNode;
import com.jme3.scene.Spatial;
import java.util.logging.Logger;

/**
 * Simple control to allow audio to be initiated from a physics thread.
 *
 * @author Stephen Gold <sgold@sonic.net>
 */
public class AudioControl
        extends SimpleControl {
    // *************************************************************************
    // constants

    /**
     * message logger for this control
     */
    final private static Logger logger =
            Logger.getLogger(AudioControl.class.getName());
    // *************************************************************************
    // fields
    /**
     * if true, play audio on next update
     */
    private boolean startFlag = false;
    // *************************************************************************
    // new methods exposed

    /**
     * Schedule the audio to be played.
     */
    public void playInstance() {
        startFlag = true;
    }
    // *************************************************************************
    // AbstractControl methods

    /**
     * Verify that the new spatial is an audio node.
     *
     * @param newSpatial (may be null)
     */
    @Override
    public void setSpatial(Spatial newSpatial) {
        if (newSpatial != null) {
            assert newSpatial instanceof AudioNode;
        }
        super.setSpatial(newSpatial);
    }

    /**
     * Update this control. (Invoked once per frame.)
     *
     * @param unused seconds since the previous update (>=0)
     */
    @Override
    public void update(float unused) {
        if (startFlag) {
            AudioNode node = (AudioNode) spatial;
            node.playInstance();
            startFlag = false;
        }
    }
}