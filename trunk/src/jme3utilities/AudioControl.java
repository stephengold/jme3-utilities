/*
 Copyright (c) 2013-2014, Stephen Gold
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
     * Callback to update this control. (Invoked once per frame.)
     *
     * @param unused seconds since the previous update (&ge;0)
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